package uk.ac.bioss.cowtastrophe;

import broadwick.stochastic.AmountManager;
import broadwick.stochastic.SimulationEvent;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import lombok.extern.slf4j.Slf4j;

/**
 * Create the manager that tracks the infection states.
 */
@Slf4j
public class PopulationManager implements AmountManager, Serializable {

    /**
     * Create the manager object for the simulation.
     * @param sim the simulation to manage.
     */
    public PopulationManager(final Simulation sim) {
        this.simulation = sim;
    }

    /**
     * Reflects a (multiple) firing of a reaction by adjusting the populations of the states. If a population becomes
     * negative, a <code> RuntimeException</code> is thrown.
     * @param event the index of the reaction fired
     * @param times the number of firings
     */
    @Override
    public final void performEvent(final SimulationEvent event, final int times) {

        Event ev = (Event) event;
        int day = (int) Math.floor(simulation.getSimulator().getCurrentTime());
        log.info("Day {} (time {}): event {}",
                 day, simulation.getSimulator().getCurrentTime(), ev.description());

        Parameters parameters = simulation.getParameters();
        if (ev.getType() == Event.Type.INFECTION) {
            // this is the only event type it could be (in this version).

            Farm infectedFarm = (Farm) event.getFinalState();

            // Set the farm as being infectious and update the caches
            infectedFarm.setStatus(DiseaseState.SUSPECTED);

            // We have a suspected case now on <infectedFarm> schedule a visit to confirm the
            // outbreak on day+1 (the day after it was suspected).
            Collection<Event> scheduledTests = simulation.getSuspisciousFarmTests()
                    .getOrDefault(day + parameters.getSuspectedTestDelay(), new ArrayList<>());
            scheduledTests.add(new Event(infectedFarm, infectedFarm, Event.Type.TEST));
            simulation.getSuspisciousFarmTests().put(day + parameters.getSuspectedTestDelay(),
                                                     scheduledTests);

            simulation.getStatistics().addNewInfection(((Farm) ev.getInitialState()), infectedFarm);
            infectedFarm.setDayInfected(day);
            infectedFarm.setInfectionSource(((Farm) event.getInitialState()).getId());
        }
        
        // We have processed some events for this day so mark the simulation.
        this.simulation.setDayWithEvents(true);

        // the possible events are:
        //    movement
        //    change disease state
        //    control measure ?
        // if a farm becomes infected/culled update the relevant lists in the
        // simulation and update the kernel (possibly too difficult but would save a lot
        // of time).
    }

    /**
     * Get a detailed description of the states and their sizes (potentially for debugging).
     * @return a detailed description of the states in the manager.
     */
    @Override
    public final String toVerboseString() {
        throw new UnsupportedOperationException("Unused method.");
    }

    /**
     * Resets the amount of each species to the initial amount retrieved by the networks. {@link AnnotationManager}.
     * This is called whenever a {@link Simulator} is started.
     */
    @Override
    public final void resetAmount() {
        throw new UnsupportedOperationException("Unused method.");
    }

    /**
     * Makes a copy of the amount array.
     */
    @Override
    public final void save() {
        throw new UnsupportedOperationException("Unused method.");
    }

    /**
     * Restore the amount array from the recently saved one.
     */
    @Override
    public final void rollback() {
        throw new UnsupportedOperationException("Unused method.");
    }

    /**
     * The simulation object that is being managed.
     */
    private final Simulation simulation;
    /**
     * The serialVersionUID.
     */
    private static final long serialVersionUID = 7853489311840574586L;
}
