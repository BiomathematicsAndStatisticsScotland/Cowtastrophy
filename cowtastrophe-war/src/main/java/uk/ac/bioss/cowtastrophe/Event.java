package uk.ac.bioss.cowtastrophe;

import broadwick.stochastic.SimulationEvent;
import broadwick.stochastic.SimulationState;
import java.io.Serializable;
import lombok.Getter;

/**
 * Encapsulates an event (movement, disease transmission etc) in the simulation.
 */
public class Event extends SimulationEvent implements Serializable {

    /**
     * An event in the simulation is encapsulated by this event class. An event could be a
     * transmission from one farm to another, a movement of animals or a farm is culled.
     * @param initialState the initial state (farm).
     * @param finalState   the final state (farm).
     * @param eventType         the event type.
     */
    public Event(final SimulationState initialState, final SimulationState finalState,
                                                     final Type eventType) {
        super(initialState, finalState);
        this.type = eventType;
    }

    /**
     * Get a description of the farm that is suitable do describe its state.
     * @return a [state] description of the farm.
     */
    public final String description() {
        return String.format("%s [%s]", type, toString());
    }

    /**
     * The type of event, infection, movement or cull.
     */
    public enum Type {
        /** an infection event. */
        INFECTION,
        /** a herd test event. */
        TEST,
    };

    @Getter
    private final Type type;
    /**
     * The serialVersionUID.
     */
    private static final long serialVersionUID = 8249179028329467991L;
}
