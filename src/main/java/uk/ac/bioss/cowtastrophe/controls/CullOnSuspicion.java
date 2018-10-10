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
public class CullOnSuspicion extends ControlStrategy implements Serializable {

    /**
     * Create the control strategy using the json formatted string.
     * @param params the parameters for the control.
     */
    public CullOnSuspicion(final JsonNode params) {
        log.info("Using Cull On Suspicion Strategy");
    }

    @Override
    public final void run(final Simulation simulation) {
        ArrayList<Farm> toBeCulled = new ArrayList<>(simulation.getSuspectedFarms());
        for (Farm farm : toBeCulled) {
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
    public static final String name = "Cull_on_suspicion";
    
    /**
     * The serialVersionUID.
     */
    private static final long serialVersionUID = 2742893009483674527L;
}
