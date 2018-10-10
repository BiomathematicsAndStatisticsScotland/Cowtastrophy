package uk.ac.bioss.cowtastrophe.controls;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import uk.ac.bioss.cowtastrophe.ControlStrategy;
import uk.ac.bioss.cowtastrophe.DiseaseState;
import uk.ac.bioss.cowtastrophe.Farm;
import uk.ac.bioss.cowtastrophe.Simulation;

/**
 * A control strategy that culls a farm once it is confirmed to be infected.
 */
@Slf4j
public class CullOnConfirmationWithRing extends ControlStrategy implements Serializable {

    /**
     * Create the control strategy using the json formatted string.
     * @param params the parameters for the control.
     */
    public CullOnConfirmationWithRing(final JsonNode params) {
        log.info("Using Cull On Confirmation with a ring Strategy");
        radius = params.get("radius").asDouble(1.0);
    }

    @Override
    public final void run(final Simulation simulation) {
        ArrayList<Farm> toBeCulled = new ArrayList<>(simulation.getConfirmedFarms());
        simulation.getConfirmedFarms().forEach((farm) -> {
            toBeCulled.addAll(simulation.getHelper().getAllFarmsWithindistance(farm, radius));
        });
        
        log.trace("Confirmed farms to be culled {}", toBeCulled.stream()
                 .map(Farm::getId)
                 .sorted()
                 .collect(Collectors.toList()));
                
        for (Farm farm : toBeCulled) {
            // Note: we are vaccinating farms within a ring so there may be suspected and susceptible
            // farms that we are vaccinating
            farm.setDayCulled(simulation.getDay());
            farm.setStatus(DiseaseState.CULLED);
            simulation.getStatistics().addCost(simulation.getDay(),
                                               farm.getHerdSize() * simulation.getParameters().getCostOfCullingAnimal()
                                               + simulation.getParameters().getCostOfFarmVisit());
        }
    }
    
    /**
     * A public identifier (name) of the strategy.
     */
    public static final String name = "Cull_on_confirmation_with_ring";

    private final double radius;
    /**
     * The serialVersionUID.
     */
    private static final long serialVersionUID = -3204938317835274527L;
}
