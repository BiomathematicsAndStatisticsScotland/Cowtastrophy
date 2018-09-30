/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
  * A control strategy that vaccinates a farm once it is suspected as being infected.
 */
@Slf4j
public class VaccinateOnSuspicion  extends ControlStrategy implements Serializable {

    /**
     * Create the control strategy using the json formatted string.
     * @param jsonNode the parameters for the control.
     */
    public VaccinateOnSuspicion(final JsonNode jsonNode) {
        log.info("Using Vaccinate on Confirmation Strategy");

        radius = jsonNode.get("radius").asDouble(1.0);
    }
    
    @Override
    public void run(Simulation simulation) {
        ArrayList<Farm> toBeVaccinated = new ArrayList<>(simulation.getSuspectedFarms());
        for (Farm farm : simulation.getSuspectedFarms()) {
            toBeVaccinated.addAll(simulation.getHelper().getAllFarmsWithindistance(farm, radius));
        }
        
        for (Farm farm : toBeVaccinated) {
            // Note: we are vaccinating farms within a ring so there may be suspected and susceptible
            // farms that we are vaccinating, so we must update these lists also.
            simulation.getSusceptibleFarms().remove(farm);
            simulation.getSuspectedFarms().remove(farm);
            simulation.getConfirmedFarms().remove(farm);
            simulation.getVaccinatedFarms().add(farm);
            farm.setDayVaccinated(simulation.getDay());
            farm.setStatus(DiseaseState.VACCINATED);
            simulation.getStatistics().addCost(simulation.getDay(),
                                               farm.getHerdSize() * simulation.getParameters().getCostOfCullingAnimal()
                                               + simulation.getParameters().getCostOfFarmVisit());
        }
    }
    
    private final double radius;

    /**
     * The serialVersionUID.
     */
    private static final long serialVersionUID = 7482289333756294766L;
    
}
