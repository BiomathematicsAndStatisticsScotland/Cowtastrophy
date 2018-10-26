package uk.ac.bioss.cowtastrophe;

/**
 * The possible disease states in the simulation.
 */
public enum DiseaseState {
    /** Susceptible state. */
    SUSCEPTIBLE,
    /** Infectious state. */
    INFECTIOUS_NOT_SUSPECTED,
    /** Suspected state. */
    SUSPECTED,
    /** Confirmed case. */
    CONFIRMED,
    /** Culled state. */
    CULLED,
    /** Vaccinated state. */
    VACCINATED
}
