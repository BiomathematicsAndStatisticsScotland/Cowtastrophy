package uk.ac.bioss.cowtastrophe;

import java.io.Serializable;

/**
 * A class for implementing a control strategy.
 */
public abstract class ControlStrategy implements Serializable {
	public static final int CULL_NOT = 0;
	public static final int CULL_ON_SUS = 1;
	public static final int CULL_ON_CON = 2;
	public static final int VAC_NOT = 0;
	public static final int VAC_ON_SUS = 1;
	public static final int VAC_ON_CON = 2;
	public static final int MOVE_NOT = 0;
	public static final int MOVE_ON_SUS = 1;
	public static final int MOVE_ON_CON = 2;
	
    /**
     * Run the control strategy at the current time.
     * @param simulation the simulation on which the control will be applied.
     */
    public abstract void run(final Simulation simulation);
    
    /**
     * A public identifier (name) of the strategy. Note this will need to be final 
     * in the implementing classes as we will be using it in a switch statement.
     */
    public static String name;

    /** The serialVersionUID. */
    private static final long serialVersionUID = 1259289399845274527L;
}
