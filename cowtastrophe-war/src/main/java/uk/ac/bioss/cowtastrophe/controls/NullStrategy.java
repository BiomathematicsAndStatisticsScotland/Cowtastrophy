package uk.ac.bioss.cowtastrophe.controls;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.Serializable;
import lombok.extern.slf4j.Slf4j;
import uk.ac.bioss.cowtastrophe.ControlStrategy;
import uk.ac.bioss.cowtastrophe.Simulation;

/**
 * A control strategy that culls a farm once it is confirmed to be infected.
 */
@Slf4j
public class NullStrategy extends ControlStrategy implements Serializable {

    /**
     * Create the control strategy using the json formatted string.
     * @param params the parameters for the control.
     */
    public NullStrategy(final JsonNode params) {
        log.info("Using Null Strategy");
    }

    /**
     * Create the control strategy using the json formatted string.
     */
    public NullStrategy() {
        log.info("Using Null Strategy");
    }

    @Override
    public final void run(final Simulation simulation) {
        // there is no strategy to implement!
    }

    /**
     * The serialVersionUID.
     */
    private static final long serialVersionUID = 2259289317430274527L;
}
