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
 * A control strategy that vaccinates a farm once it is confirmed as being infected.
 */
@Slf4j
public class VaccinateOnConfirmation extends ControlStrategy implements Serializable {

    /**
     * Create the control strategy using the json formatted string.
     * @param jsonNode the parameters for the control.
     */
    public VaccinateOnConfirmation(final JsonNode jsonNode) {
        log.info("Using Vaccinate on Confirmation Strategy");
        radius = jsonNode.get("radius").asDouble(1.0);
    }

    @Override
    public void run(Simulation simulation) {
        ArrayList<Farm> toBeVaccinated = new ArrayList<>(simulation.getConfirmedFarms());
        for (Farm farm : simulation.getConfirmedFarms()) {
            toBeVaccinated.addAll(simulation.getHelper().getAllFarmsWithindistance(farm, radius));
        }

        for (Farm farm : toBeVaccinated) {
            farm.setDayVaccinated(simulation.getDay());
            farm.setStatus(DiseaseState.VACCINATED);
            simulation.getConfirmedFarms().remove(farm);
            simulation.getVaccinatedFarms().add(farm);
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
