package uk.ac.bioss.cowtastrophe.controls;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import uk.ac.bioss.cowtastrophe.ControlStrategy;
import uk.ac.bioss.cowtastrophe.DiseaseState;
import uk.ac.bioss.cowtastrophe.Farm;
import uk.ac.bioss.cowtastrophe.Simulation;

/**
 * A control strategy that culls a farm once it is suspected and vaccinates suspected farms within a ring.
 */
@Slf4j
public class CullAndVaccinateOnSuspiscion extends ControlStrategy implements Serializable {

    /**
     * Create the control strategy using the json formatted string.
     * @param jsonNode the parameters for the control.
     */
    public CullAndVaccinateOnSuspiscion(final JsonNode jsonNode) {
        log.info("Using Cull On Confirmation with a ring Strategy");
        radius = jsonNode.get("radius").asDouble(1.0);

    }

    @Override
    public void run(Simulation simulation) {
        ArrayList<Farm> toBeCulled = new ArrayList<>(simulation.getSuspectedFarms());

        log.trace("Suspected farms to be culled {}", toBeCulled.stream()
                  .map(Farm::getId)
                  .sorted()
                  .collect(Collectors.toList()));

        for (Farm farm : toBeCulled) {
            farm.setDayCulled(simulation.getDay());
            farm.setStatus(DiseaseState.CULLED);
            simulation.getStatistics().addCost(simulation.getDay(),
                                               farm.getHerdSize() * simulation.getParameters().getCostOfCullingAnimal()
                                               + simulation.getParameters().getCostOfFarmVisit());

            // now vaccinate those (suspected) farms within the radius.
            List<Farm> toBeVaccinated = simulation.getHelper().getAllFarmsWithindistance(farm, radius).stream()
                    .filter((fm) -> (fm.getStatus() == DiseaseState.SUSPECTED))
                    .collect(Collectors.toList());
            for (Farm vacc : toBeVaccinated) {
                vacc.setDayVaccinated(simulation.getDay());
                vacc.setStatus(DiseaseState.VACCINATED);
                simulation.getStatistics().addCost(simulation.getDay(),
                                                   vacc.getHerdSize() * simulation.getParameters().getCostOfVaccinatingAnimal()
                                                   + simulation.getParameters().getCostOfFarmVisit());
            }
            
        }

        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * A public identifier (name) of the strategy.
     */
    public static final String name = "Cull_and_vaccinate_on_suspiscion";

    private final double radius;

    /**
     * The serialVersionUID.
     */
    private static final long serialVersionUID = 8361299864383674527L;
}
