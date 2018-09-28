package uk.ac.bioss.cowtastrophe.controls;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.Serializable;
import java.util.ArrayList;
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
        for (Farm farm : simulation.getConfirmedFarms()) {
            toBeCulled.addAll(simulation.getHelper().getAllFarmsWithindistance(farm, radius));
        }

        for (Farm farm : toBeCulled) {
            farm.setDayCulled(simulation.getDay());
            farm.setStatus(DiseaseState.CULLED);
            simulation.getConfirmedFarms().remove(farm);
            simulation.getCulledFarms().add(farm);
            simulation.getStatistics().addCost(simulation.getDay(),
                                               farm.getHerdSize() * simulation.getParameters().getCostOfCullingAnimal()
                                               + simulation.getParameters().getCostOfFarmVisit());
        }
    }

    private final double radius;
    /**
     * The serialVersionUID.
     */
    private static final long serialVersionUID = -3204938317430274527L;
}
