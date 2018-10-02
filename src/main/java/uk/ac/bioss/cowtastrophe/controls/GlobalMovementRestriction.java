/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package uk.ac.bioss.cowtastrophe.controls;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.Serializable;
import lombok.extern.slf4j.Slf4j;
import uk.ac.bioss.cowtastrophe.ControlStrategy;
import uk.ac.bioss.cowtastrophe.Farm;
import uk.ac.bioss.cowtastrophe.Simulation;

/**
 * A control strategy that imposes a movement restriction on every farm until the end of the simulation.
 */
@Slf4j
public class GlobalMovementRestriction extends ControlStrategy implements Serializable {

    GlobalMovementRestriction(JsonNode strategyNode) {
    }

    @Override
    public void run(Simulation simulation) {
        final int days = simulation.getParameters().getEndTime() - simulation.getDay();
        for (Farm farm : simulation.getFarms()) {
            simulation.getRestrictedFarms().add(farm.getId());
            simulation.getStatistics().addCost(simulation.getDay(),
                                               simulation.getParameters()
                                                       .getCostOfMvmtBanPerDay() * days);

            // do not schedule when the movement ban should be lifted.
        }
    }

}
