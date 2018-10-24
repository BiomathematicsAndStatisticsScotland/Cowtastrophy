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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
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
     * @param directory      the directory where the output is to be stored.
     * @param paramsFileName the name of the file containing the parameters with which to run the simulation.
     */
    public Simulation(final String directory, final String paramsFileName) {
        // TODO: simulation should take a string that gives the location of
        // the parameters file
        this.parameters = new Parameters(paramsFileName);
        this.parameters.setDirectory(directory);
        this.farms = new HashSet<>(this.parameters.getFarms());
        this.restrictedFarms = new HashSet<>();
        this.SuspisciousFarmTests = new HashMap<>();
        this.easeMvmtRestriction = new HashMap<>();
        this.helper = new SimulationHelper(this);
        this.day = 0;
        this.statistics = new Statistics();
        this.controlStrategy = new NullStrategy();
        this.rngSeed = new RNG(RNG.Generator.Well19937c).getInteger(0, Integer.MAX_VALUE - 1);
        this.cleanupRequired = true;

        // Find all the farms whose id is in the list of seedIds and set them to be INFECTIOUS.
        List<Integer> seedIds = this.parameters.getSeedFarms().stream().map(Farm::getId)
                .collect(Collectors.toList());
        for (Farm f : farms) {
            if (seedIds.contains(f.getId())) {
                f.setStatus(DiseaseState.SUSPECTED);
            }
        }

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
        for (Farm farm : getSuspectedFarms()) {
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
        log.info("Running simulation for day {}", day);
        cleanupDataFiles();

        dayWithEvents = false;

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

        doDailyChecks();

        statistics.setSusceptibleFarms(day, ((int)countFarms(DiseaseState.SUSCEPTIBLE)));
        statistics.addSuspectedFarms(day, ((int)countFarms(DiseaseState.SUSPECTED)));
        statistics.addConfirmedFarms(day, ((int)countFarms(DiseaseState.CONFIRMED)));
        statistics.addCulledFarms(day, ((int)countFarms(DiseaseState.CULLED)));
        statistics.addVaccinatedFarms(day, ((int)countFarms(DiseaseState.VACCINATED)));
        statistics.addRestrictedFarms(day, this.restrictedFarms.size());
        statistics.addCost(day, 0.0); // TODO - update this when the controls have been encoded.

        doDailyChecks();

        log.info("Susceptible Farms = {} ", farms.stream()
                 .filter((farm) -> (farm.getStatus() == DiseaseState.SUSCEPTIBLE))
                 .map(Farm::getId)
                 .sorted()
                 .collect(Collectors.toList()));
        log.info("Suspected Farms = {} ", farms.stream()
                 .filter((farm) -> (farm.getStatus() == DiseaseState.SUSPECTED))
                 .map(Farm::getId)
                 .sorted()
                 .collect(Collectors.toList()));
        log.info("Confirmed Farms = {} ", farms.stream()
                 .filter((farm) -> (farm.getStatus() == DiseaseState.CONFIRMED))
                 .map(Farm::getId)
                 .sorted()
                 .collect(Collectors.toList()));
        log.info("Culled Farms = {} ", farms.stream()
                 .filter((farm) -> (farm.getStatus() == DiseaseState.CULLED))
                 .map(Farm::getId)
                 .sorted()
                 .collect(Collectors.toList()));
        log.info("Vaccinated Farms = {} ", farms.stream()
                 .filter((farm) -> (farm.getStatus() == DiseaseState.VACCINATED))
                 .map(Farm::getId)
                 .sorted()
                 .collect(Collectors.toList()));

        // TODO: we should now ask the statistics to update itself......
        log.info("Finished simulation for day {} [next infection event at = {}]",
                 day,
                 this.simulator.getCurrentTime());

        log.trace("Scheduled tests: {}", SuspisciousFarmTests.toString());
        day += 1; // update the time by one day...

        helper.saveSession(sessionId, day);

        // Keep running until we have a day with events (in case there are situations where there
        // are no events for several days)
        if (!dayWithEvents) {
            // we've  had no events today so try tomorrow and keep going until we have had a day with events.
            run24Hours();
        }
    }

    /**
     * Run the simulation in a separate thread until the end. The thread is a blocking thread but this will need to
     * change when connected to the servlet as we don't want the application to hang.
     */
    public final void run() {
        cleanupDataFiles();

        // todo: remove all the .ser nd .json files that have a day > today.
        log.info("Parameters = {}", parameters.toString());
        helper.savePid(sessionId);

        ExecutorService executorService = Executors.newSingleThreadExecutor();
        Future<?> result = executorService.submit(() -> {
            threadRunning = true;
            while (this.simulator.getTransitionKernel().getTransitionEvents().size() > 0
                    && day < MAX_ENDDATE) {
                // i.e. there are more events
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
     * Run the tests that are scheduled for the current day. These tests check suspected farms and mark them as
     * confirmed.
     */
    private void doDailyChecks() {

        // running the control strategy BEFORE any testing will ensure that suspected farms are
        // detected and dealt with, measures on confirmed cases will be missed until the next day
        // (unless we move the control strategy after the tests, in which case the suspected farms 
        // will not be controlled but confirmed cases will, OR we run the control again AFTER the test
        // block).
        controlStrategy.run(this);

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
                        suspectedFarm.setStatus(DiseaseState.CONFIRMED);
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
        
        // Finally: Each infected farm carries a cost as does being under movement restriction
        // update the statistics.
        Collection<DiseaseState> infectedStates = Arrays.asList(DiseaseState.CONFIRMED,
                                                                DiseaseState.INFECTIOUS);
        Long numInfectedFarms = farms.stream()
                .filter((farm) -> (infectedStates.contains(farm.getStatus())))
                .collect(Collectors.counting());
        this.statistics.addCost(day, numInfectedFarms*parameters.getCostOfInfectedFarmPerDay());
        
        this.statistics.addCost(day, restrictedFarms.size()*parameters.getCostOfMvmtBanPerDay());
    }

    /**
     * Remove all the .ser nd .json files that have a day > today.
     */
    private void cleanupDataFiles() {
        if (cleanupRequired) {
            Path path = Paths.get(parameters.getDirectory()).resolve(this.sessionId);
            File[] files = path.toFile().listFiles((d, name) -> name.startsWith(sessionId)
                                                                && name.endsWith(".ser"));

            for (File file : files) {
                String dataDay = file.getName().replace(sessionId+"_", "").replace(".0.ser", "");
                int fileDay = Integer.parseInt(dataDay);
                if (fileDay > day) {
                    log.info("Deleting previous file {} and associated json file", file);
                    file.delete();
                    (new File(file.toString().replace(".0.ser", ".0.json"))).delete();
                }
            }

            cleanupRequired = false; // do not cleanup files for every run.
        }
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

        List<Farm> infectedFarms = farms.stream()
                .filter((farm) -> (farm.getStatus() == DiseaseState.SUSPECTED)
                                  || (farm.getStatus() == DiseaseState.CONFIRMED))
                .collect(Collectors.toList());

        List<Farm> susceptibleFarms = farms.stream()
                .filter((farm) -> (farm.getStatus() == DiseaseState.SUSCEPTIBLE))
                .collect(Collectors.toList());

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
        jsFile.append("\"timeframe\": \"").append(day + 1).append("\", ");
		jsFile.append("\"next_event\": \"").append(this.simulator.getCurrentTime()).append("\", ");
		jsFile.append("\"susceptible\": \"").append(countFarms(DiseaseState.SUSCEPTIBLE)).append("\", ");
		jsFile.append("\"suspected\": \"").append(countFarms(DiseaseState.SUSPECTED)).append("\", ");
		jsFile.append("\"confirmed\": \"").append(countFarms(DiseaseState.CONFIRMED)).append("\", ");
		jsFile.append("\"culled\": \"").append(countFarms(DiseaseState.CULLED)).append("\", ");
		jsFile.append("\"vaccinated\": \"").append(countFarms(DiseaseState.VACCINATED)).append("\", ");
		jsFile.append("\"cost\": \"").append(statistics.getCost(day-1)).append("\", ");
        jsFile.append("\"farms\": [");

        jsFile.append(farms.stream().map(Farm::asJson)
                .collect(Collectors.joining(", ")));
        jsFile.append("]");
        jsFile.append("}");

        return jsFile.toString();
    }

    /**
     * Get a collection of farms (a java.util.Set) which are labelled as SUSPECTED.
     * @return a a java.util.Set of SUSPECTED farms.
     */
    public final Set<Farm> getSuspectedFarms() {
        return farms.stream()
                .filter((farm) -> (farm.getStatus() == DiseaseState.SUSPECTED))
                .collect(Collectors.toSet());
    }

    /**
     * Get a collection of farms (a java.util.Set) which are labelled as CONFIRMED.
     * @return a a java.util.Set of CONFIRMED farms.
     */
    public final Set<Farm> getConfirmedFarms() {
        return farms.stream()
                .filter((farm) -> (farm.getStatus() == DiseaseState.CONFIRMED))
                .collect(Collectors.toSet());
    }
	
	/**
     * Get a collection of farms (a java.util.Set) which are labelled as CONFIRMED or SUSPECTED.
     * @return a a java.util.Set of CONFIRMED or SUSPECTED farms.
     */
    public final Set<Farm> getSuspectedOrConfirmedFarms() {
        return farms.stream()
                .filter((farm) -> (farm.getStatus() == DiseaseState.CONFIRMED || farm.getStatus() == DiseaseState.SUSPECTED))
                .collect(Collectors.toSet());
    }

    /**
     * Get a collection of farms (a java.util.Set) which are labelled as VACCINATED.
     * @return a a java.util.Set of VACCINATED farms.
     */
    public final Set<Farm> getVaccinatedFarms() {
        return farms.stream()
                .filter((farm) -> (farm.getStatus() == DiseaseState.VACCINATED))
                .collect(Collectors.toSet());
    }

    /**
     * COUNT a collection of farms (a java.util.Set) which are labelled as stat.
     * @return a LONG
     */
    public final long countFarms(DiseaseState stat) {
        return farms.stream()
                .filter((farm) -> (farm.getStatus() == stat))
                .collect(Collectors.counting());
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
    @Getter
    private boolean threadRunning;
    private final int rngSeed;
    @JsonIgnore
    @Setter
    @Getter
    private boolean cleanupRequired;
    @JsonIgnore
    @Setter
    private boolean dayWithEvents;
    @JsonIgnore
    private final int MAX_ENDDATE = 1000;
    private static final long serialVersionUID = 32345527169572879L;
}
