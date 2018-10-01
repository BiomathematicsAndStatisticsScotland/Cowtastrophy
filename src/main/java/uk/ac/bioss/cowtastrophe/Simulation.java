package uk.ac.bioss.cowtastrophe;

import broadwick.rng.RNG;
import uk.ac.bioss.cowtastrophe.controls.NullStrategy;
import broadwick.stochastic.SimulationController;
import broadwick.stochastic.StochasticSimulator;
import broadwick.stochastic.TransitionKernel;
import broadwick.stochastic.algorithms.GillespieSimple;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

/**
 * A class that is responsible for simulating the infection process.
 */
@Slf4j
public class Simulation implements Serializable {

    /**
     * Create a simulation from a set of parameters.
     * @param directory the directory where the output is to be stored.
     * @param paramsFileName the name of the file containing the parameters with which to run the simulation.
     */
    public Simulation(final String directory, final String paramsFileName) {
        // TODO: simulation should take a string that gives the location of
        // the parameters file
        this.parameters = new Parameters(paramsFileName);
        this.parameters.setDirectory(directory);
        this.farms = new HashSet<>(this.parameters.getFarms());
        this.suspectedFarms = new HashSet<>();
        this.confirmedFarms = new HashSet<>();
        this.culledFarms = new HashSet<>();
        this.susceptibleFarms = new HashSet<>();
        this.vaccinatedFarms = new HashSet<>();
        this.restrictedFarms = new HashSet<>();
        this.SuspisciousFarmTests = new HashMap<>();
        this.easeMvmtRestriction = new HashMap<>();
        this.helper = new SimulationHelper(this);
        this.day = 0;
        this.statistics = new Statistics();
        this.controlStrategy = new NullStrategy();
        this.rngSeed = new RNG(RNG.Generator.Well19937c).getInteger(0, Integer.MAX_VALUE - 1);

        // Find all the farms whose id is in the list of seedIds and set them to be INFECTIOUS.
        List<Integer> seedIds = this.parameters.getSeedFarms().stream().map(Farm::getId)
                .collect(Collectors.toList());
        for (Farm f : farms) {
            if (seedIds.contains(f.getId())) {
                f.setStatus(DiseaseState.SUSPECTED);
            }
        }

        // Now we have the farms we will update the caches.
        susceptibleFarms.addAll(farms.stream()
                .filter(f -> f.getStatus() == DiseaseState.SUSCEPTIBLE)
                .collect(Collectors.toList()));
        suspectedFarms.addAll(farms.stream()
                .filter(f -> f.getStatus() == DiseaseState.SUSPECTED)
                .collect(Collectors.toList()));
        confirmedFarms.addAll(farms.stream()
                .filter(f -> f.getStatus() == DiseaseState.CONFIRMED)
                .collect(Collectors.toList()));

        // Create the sessionId for the session and create the directory to hold it.
        sessionId = helper.generateSessionId();
        log.info("Generated session id " + sessionId);
        File settingsDir = new File(directory + sessionId);
        if (!settingsDir.exists()) {
            log.info("Creating directory " + directory);
            settingsDir.mkdir();
            // If you require it to make the entire directory path including parents,
            // use directory.mkdirs(); here instead.
        }

        // Create the kernel, simulator, observers and controller
        manager = new PopulationManager(this);
        kernel = new TransitionKernel();
        this.simulator = new GillespieSimple(manager, kernel);
        this.simulator.setRngSeed(rngSeed);
        this.simulator.setStartTime(day);

        // register all suspected farms on day 0 to be checked.
        for (Farm farm : suspectedFarms) {
            Collection<Event> scheduledTests = this.getSuspisciousFarmTests()
                    .getOrDefault(day + 1, new ArrayList<>());
            scheduledTests.add(new Event(farm, farm, Event.Type.TEST));
            this.getSuspisciousFarmTests().put(day + parameters.getSuspectedTestDelay(),
                                               scheduledTests);
        }

        // save the initial setup.
        helper.saveSession(sessionId, day);
        helper.savePid(sessionId);
    }

    /**
     * Run the simulation for the next 24 hours.
     */
    public final void run24Hours() {
        // TODO: add member daysSinceLastSuspectedCase;
        // run the simulation for 24 hours.
        log.info("Running simulation for day {}", day);

        simulator.setStartTime(this.simulator.getCurrentTime());

        // Add a controller (netbeans converted anpnymous inner class to this lambda)
        final SimulationController controller = (StochasticSimulator sim) -> {
            log.trace("Checking if simulation time {} < time {}", sim.getCurrentTime(), day);
            return sim.getCurrentTime() <= day;
        };
        this.simulator.setController(controller);

		
        // finally create the transition kernel
        this.kernel = updateKernel();
        this.simulator.setTransitionKernel(kernel);
        this.simulator.run();
        // TODO: controlStrategy will be an array in the future and we will iterate over it
        // calling run on each control.
        controlStrategy.run(this);
        doDailyChecks();

        statistics.setSusceptibleFarms(day, susceptibleFarms.size());
        statistics.addSuspectedFarms(day, suspectedFarms.size());
        statistics.addConfirmedFarms(day, confirmedFarms.size());
        statistics.addCulledFarms(day, culledFarms.size());
        statistics.addVaccinatedFarms(day, vaccinatedFarms.size());
        //statistics.addCost(time, 0.0); // TODO - update this when the controls have been encoded.
        
        log.info("Suspected Farms = {} ", new TreeSet(suspectedFarms.stream()
            .map(Farm::getId)
            .collect(Collectors.toList())));
        log.info("Confirmed Farms = {} ", new TreeSet(confirmedFarms.stream()
            .map(Farm::getId)
            .collect(Collectors.toList())));
        log.info("Culled Farms = {} ", new TreeSet(culledFarms.stream()
            .map(Farm::getId)
            .collect(Collectors.toList())));
        log.info("Vaccinated Farms = {} ", new TreeSet(vaccinatedFarms.stream()
            .map(Farm::getId)
            .collect(Collectors.toList())));

        log.info("Finished simulation for day {} [next infection event at = {}]",
                 day,
                 this.simulator.getCurrentTime());
        log.trace("Scheduled tests: {}", SuspisciousFarmTests.toString());
        day += 1; // update the time by one day...
        helper.saveSession(sessionId, day);
    }

    /**
     * Run the tests that are scheduled for the current day. These tests check suspected farms and mark them as
     * confirmed.
     */
    private void doDailyChecks() {
        //First: check all suspected farms and mark them as confirmed.
        if (SuspisciousFarmTests.get(this.getDay()) != null) {
            Collection<Event> todaysTests = new HashSet(SuspisciousFarmTests.get(this.getDay()));
            if (todaysTests != null) {
                log.info("Testing {} suspected farms on day {} (next infection event at = {})",
                         todaysTests.size(), day, simulator.getCurrentTime());
                for (final Object eventObj : todaysTests) {
                    Event event = (Event) eventObj;

                    Farm suspectedFarm = (Farm) event.getInitialState();
                    final double cost = parameters.getCostOfFarmVisit()
                                        + parameters.getCostOfTestPerAnimal() * suspectedFarm.getHerdSize();
                    statistics.addCost(day, cost);
                    log.trace("Testing farm {} at time {} [cost = {}]", suspectedFarm, day, cost);
                    if (DiseaseState.SUSPECTED == suspectedFarm.getStatus()) {
                        // this should always be the case, but checking to be sure!
                        suspectedFarms.remove(suspectedFarm);
                        suspectedFarm.setStatus(DiseaseState.CONFIRMED);
                        confirmedFarms.add(suspectedFarm);
                        log.info("Farm {} confirmed.", suspectedFarm.getId());
                    }
                }
                // cleaning up - removing these tests from the map!
                SuspisciousFarmTests.remove(day);
            }
        }

        //Second: check if any farms that have a movement ban can have the ban lifted.
        Collection<Integer> todaysLifts = easeMvmtRestriction.get(this.getDay());
        if (todaysLifts != null) {
            for (int farmId : todaysLifts) {
                restrictedFarms.remove(farmId);
            }
        }
    }

    /**
     * Run the simulation in a separate thread until the end. The thread is a blocking thread but this will need to
     * change when connected to the servlet as we don't want the application to hang.
     */
    public final void run() {
        final int end = this.parameters.getEndTime();
        log.info("Parameters = {}", parameters.toString());
        helper.savePid(sessionId);

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Future<?> result = executorService.submit(() -> {
            isthreadRunning = true;
            while (day < end) {
                run24Hours();
            }
        });

        // blocking case:
        try {
            // Wait until the thread is finished running (get the result of the future) and
            // shutdown the executor service.
            result.get();
            executorService.shutdownNow();
        } catch (InterruptedException | ExecutionException ex) {
            log.error("{}\nSomething went wrong running simulation. See the error messages.",
                      ex.getLocalizedMessage());
        }

        // non-blocking case:
//        try {
//            if (!executorService.awaitTermination(800, TimeUnit.MILLISECONDS)) {
//                executorService.shutdownNow();
//            }
//        } catch (InterruptedException e) {
//            executorService.shutdownNow();
//        }
    }

    /**
     * Finalise the application.
     */
    public final void finalise() {
        log.info("\n{}", statistics.toString());

        // empty the contents of the pid file
        helper.savePid(sessionId, "");
    }

    /**
     * Update the transition kernel. All suspected and confirmed farms can infect susceptible ones.
     * @return the updated Kernel.
     */
    public final TransitionKernel updateKernel() {
        TransitionKernel kern = simulator.getTransitionKernel();

        kern.clear();
        Stream<Farm> infectedFarms = Stream.concat(suspectedFarms.stream(),
                                                   confirmedFarms.stream());

        infectedFarms.forEach((infected) -> {
            susceptibleFarms.forEach((susceptible) -> {
                final double sep = helper.getFarmDistance(infected, susceptible);

                double power = parameters.getKernelPower();
                if (restrictedFarms.contains(infected.getId())
                    || restrictedFarms.contains(susceptible.getId())) {
                    power = parameters.getRestrictedKernelPower();
                }

                final double prob = Math.pow(1 + (sep / parameters.getKernelOffset()), -power)
                                    * infected.getHerdSize() * susceptible.getHerdSize()
                                    * parameters.getBeta();

                kern.addToKernel(new Event(infected, susceptible, Event.Type.INFECTION), prob);
            });
        });

        return kern;
    }

    /**
     * Get the simulation data as JSON.
     * @return A JSON string.
     */
    public final String asJson() {
        StringBuilder jsFile = new StringBuilder();

        jsFile.append("{");
        jsFile.append("\"session_id\": \"").append(sessionId).append("\", ");
        jsFile.append("\"timeframe\": \"").append(day+1).append("\", ");
        jsFile.append("\"farms\": [");

        jsFile.append(farms.stream().map(Farm::asJson)
                .collect(Collectors.joining(", ")));
        jsFile.append("]");
        jsFile.append("}");
        
        return jsFile.toString();
    }

    @JsonIgnore
    @Getter
    private final SimulationHelper helper;
    @Getter
    private final Parameters parameters;
    @JsonIgnore
    @Getter
    private final String sessionId;
    @JsonIgnore
    @Getter
    private int day;
    @JsonIgnore
    @Getter
    private final Set<Farm> farms;
    @Getter
    private final Set<Farm> suspectedFarms;
    @Getter
    private final Set<Farm> confirmedFarms;
    @Getter
    private final Set<Farm> culledFarms;
    @Getter
    private final Set<Farm> susceptibleFarms;
    @Getter
    private final Set<Farm> vaccinatedFarms;
    @Getter
    private final Set<Integer> restrictedFarms;
    private final PopulationManager manager;
    @Getter
    @Setter
    private ControlStrategy controlStrategy;
    @Getter
    private final Statistics statistics;
    @Getter
    private final StochasticSimulator simulator;
    @Getter
    private final Map<Integer, Collection<Event>> SuspisciousFarmTests;
    @Getter
    private final Map<Integer, Collection<Integer>> easeMvmtRestriction;
    private TransitionKernel kernel;
    private boolean isthreadRunning;
    private final int rngSeed;

    private static final long serialVersionUID = 32345527169572885L;
}
