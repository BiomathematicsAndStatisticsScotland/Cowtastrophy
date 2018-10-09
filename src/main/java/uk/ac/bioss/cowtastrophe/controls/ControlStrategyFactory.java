package uk.ac.bioss.cowtastrophe.controls;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import uk.ac.bioss.cowtastrophe.ControlStrategy;

/**
 * Creational Pattern for creating the appropriate control strategy.
 */
public final class ControlStrategyFactory {

    /**
     * Hidden private utility class constructor.
     */
    private ControlStrategyFactory() {
    }

    /**
     * Create the appropriate control strategy given its name and a json formatted string of parameters.
     * @param strategyJson a json formatted string with the parameters for the strategy.
     * @return the control strategy object.
     */
    public static ControlStrategy create(final String strategyJson) {

        // TODO: need to allow for several control strategies to be created and returned in an array.
        if (!strategyJson.isEmpty()) {
            try {
                ObjectMapper mapper = new ObjectMapper();
                final JsonNode strategyNode = mapper.readTree(strategyJson);

                if (strategyNode.get("strategy") == null) {
                    return new NullStrategy(strategyNode);
                }

                switch (strategyNode.get("strategy").asText()) {
                    case CullOnConfirmation.name:
                        return new CullOnConfirmation(strategyNode);
                    case CullOnSuspicion.name:
                        return new CullOnSuspicion(strategyNode);
                    case MovementRestriction.name:
                        return new MovementRestriction(strategyNode);
                    case GlobalMovementRestriction.name:
                        return new GlobalMovementRestriction(strategyNode);
                    case CullOnConfirmationWithRing.name:
                        return new CullOnConfirmationWithRing(strategyNode);
                    case VaccinateOnConfirmation.name:
                        return new VaccinateOnConfirmation(strategyNode);
                    case VaccinateOnSuspicion.name:
                        return new VaccinateOnSuspicion(strategyNode);
                    case RingVaccination.name:
                        return new RingVaccination(strategyNode);
                    default:
                        return new NullStrategy();
                }

        }catch (IOException ex) {
                // TODO: 
                System.out.println("Exception : " + ex.getLocalizedMessage());
            }
    }

    return new NullStrategy();
}

/**
 * Create the appropriate control strategy given its name and a json formatted string of parameters.
 * @param strategyJson a json formatted string with the parameters for the strategy.
 * @return the control strategy object.
 */
public static ControlStrategy create(final int culling, final int vaccinate, final double vacradius) {

        return new CullVacCombi(culling, vaccinate, vacradius);
    }
}
