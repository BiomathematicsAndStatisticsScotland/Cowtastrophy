package uk.ac.bioss.cowtastrophe;

import java.io.Serializable;

/**
 * A class for implementing a control strategy.
 */
public abstract class ControlStrategy implements Serializable {

    /**
     * Run the control strategy at the current time.
     */
    public abstract void run(final Simulation simulation);

    /** The serialVersionUID. */
    private static final long serialVersionUID = 1259289399845274527L;
}
