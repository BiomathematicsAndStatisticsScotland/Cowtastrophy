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
                
                String strategy = strategyNode.get("strategy").asText();
                if ("Cull_on_confirmation".equalsIgnoreCase(strategy)) {
                    return new CullOnConfirmation(strategyNode);
                } else if ("Cull_on_suspicion".equalsIgnoreCase(strategy)) {
                    return new CullOnSuspicion(strategyNode);
                } else if ("Movement_restriction".equalsIgnoreCase(strategy)) {
                    return new MovementRestriction(strategyNode);
                } else if ("Global_movement_restriction".equalsIgnoreCase(strategy)) {
                    return new GlobalMovementRestriction(strategyNode);
                } else if ("Cull_on_confirmation_with_Ring".equalsIgnoreCase(strategy)) {
                    return new CullOnConfirmationWithRing(strategyNode);
                } else if ("Vaccinate_on_confirmation".equalsIgnoreCase(strategy)) {
                    return new VaccinateOnConfirmation(strategyNode);
                } else if ("Vaccinate_on_suspicion".equalsIgnoreCase(strategy)) {
                    return new VaccinateOnSuspicion(strategyNode);
                } else if ("Ring_vaccination".equalsIgnoreCase(strategy)) {
                    return new RingVaccination(strategyNode);
                } else {
                    return new NullStrategy(strategyNode);
                }
            } catch (IOException ex) {
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
