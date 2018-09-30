package uk.ac.bioss.cowtastrophe;

import broadwick.BroadwickException;
import broadwick.stochastic.SimulationState;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import java.io.Serializable;
import java.util.concurrent.ThreadLocalRandom;
import lombok.Getter;
import lombok.Setter;
import lombok.EqualsAndHashCode;

/**
 * A Farm in the simulation contains animals that can move and transmit disease. Since they contain a [disease] state
 * they implement a SimulationState.
 */
@EqualsAndHashCode
public class Farm implements SimulationState, Serializable {

    /**
     * Create a default farm object without setting any internal attributes.
     */
    public Farm() {
    }

    /**
     * Create the farm at the specified location with the given attributes.
     * @param name      the id of the farm.
     * @param latitude  the x coordinate of the farm.
     * @param longitude the y coordinate of the farm.
     * @param size      the radius of the farm.
     */
    public Farm(final int name, final double latitude, final double longitude, final int size) {
        this.id = name;
        this.x = latitude;
        this.y = longitude;
        this.status = DiseaseState.SUSCEPTIBLE;
        this.herdSize = ThreadLocalRandom.current().nextInt(MIN_SIZE, MAX_SIZE);
        this.radius = size;
        this.infectionSource = -1;
        this.dayInfected = -1;
        this.dayCulled = -1;
        this.dayVaccinated = -1;
    }

    /**
     * Get the name/id of the farm.
     * @return the states name.
     */
    @Override
    public final String getStateName() {
        return String.valueOf(id);
    }

    /**
     * Get a description of the farm that is suitable to describe its state.
     * @return a [state] description of the farm.
     */
    @Override
    public final String toString() {
        return String.format("%d [%s]", id, status);
    }

    /**
     * Get a description of the farm that is suitable do describe its state in Json format.
     * @return a [json] description of the farm.
     */
    public final String asJson() {
        String jsonStr = "";

        try {
            ObjectMapper mapper = new ObjectMapper();
            jsonStr = mapper.writer().writeValueAsString(this);
        } catch (JsonProcessingException jpe) {
            throw new BroadwickException("Error saving arm as json; "
                                         + Throwables.getStackTraceAsString(jpe));
        }
        return jsonStr;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Farm)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        return this.getId() == ((Farm) obj).getId();
    }

    @Override
    public int hashCode() {
        return id;
    }

    /**
     * The id of the farm.
     */
    @Getter
    @Setter
    private int id;
    /**
     * The x (latitude) coordinate of the farm.
     */
    @Getter
    @Setter
    private double x;
    /**
     * The y (longitude) coordinate of the farm.
     */
    @Getter
    @Setter
    private double y;
    /**
     * The disease state of the farm.
     */
    @Getter
    @Setter
    private DiseaseState status;
    /**
     * The number of animals on the farm.
     */
    @Getter
    @Setter
    private int herdSize;
    /**
     * The radius of the farm.
     */
    @Getter
    @Setter
    private int radius;
    /**
     * The day the farm was infected.
     */
    @Getter
    @Setter
    private int dayInfected;
    /**
     * The day the farm was culled.
     */
    @Getter
    @Setter
    private int dayCulled;
    /**
     * The day the farm was vaccinated.
     */
    @Getter
    @Setter
    private int dayVaccinated;
    /**
     * The id of the farm infecting this one.
     */
    @Getter
    @Setter
    private int infectionSource;
    /**
     * The serialVersionUID.
     */
    private static final long serialVersionUID = 3453098701390754586L;
    /**
     * The minimum herd size.
     */
    private static final int MIN_SIZE = 50;
    /**
     * The maximum herd size.
     */
    private static final int MAX_SIZE = 150;

}
