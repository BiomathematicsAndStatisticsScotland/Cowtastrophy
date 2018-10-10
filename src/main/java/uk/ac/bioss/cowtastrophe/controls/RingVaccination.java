package uk.ac.bioss.cowtastrophe.controls;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
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
public class RingVaccination extends ControlStrategy implements Serializable {

    static class Params {

        public Params() {
        }

        public int farmId;
        public double radius;
    }

    /**
     * Create the control strategy using the json formatted string.
     * @param jsonNode the parameters for the control.
     */
    public RingVaccination(final JsonNode jsonNode) {
        log.info("Using Ring Vaccination Strategy");

        ring = new HashMap<>();
        try {
            ObjectMapper mapper = new ObjectMapper();
            Params[] params = mapper.treeToValue(jsonNode.get("parameters"), Params[].class);
            for (Params param : params) {
                ring.put(param.farmId, param.radius);
            }

        } catch (JsonProcessingException ex) {
            log.error("Could not process Ring Vaccination parameters: \n{}",
                      Throwables.getStackTraceAsString(ex));
        }
    }

    @Override
    public final void run(final Simulation simulation) {
        ArrayList<Farm> toBeVaccinated = new ArrayList<>(simulation.getConfirmedFarms());
        for (Map.Entry<Integer, Double> entry : ring.entrySet()) {
            final int farmId = entry.getKey();
            final Farm farm = simulation.getHelper().getFarmById(farmId);

            toBeVaccinated.add(farm);
            simulation.getHelper().getAllFarmsWithindistance(farm, entry.getValue()).forEach((farm2) -> {
                toBeVaccinated.add(farm2);
            });
        }
        
        log.trace("Farms to be vaccinated {}", toBeVaccinated.stream()
                  .map(Farm::getId)
                  .sorted()
                  .collect(Collectors.toList()));

        for (Farm farm : toBeVaccinated) {
            farm.setDayVaccinated(simulation.getDay());
            farm.setStatus(DiseaseState.VACCINATED);
            simulation.getStatistics().addCost(simulation.getDay(),
                                               farm.getHerdSize() * simulation.getParameters()
                                               .getCostOfVaccinatingAnimal());
        }
    }

    /**
     * A public identifier (name) of the strategy.
     */
    public static final String name = "Ring_vaccination";

    private final Map<Integer, Double> ring;

    /**
     * The serialVersionUID.
     */
    private static final long serialVersionUID = 1182389317430294766L;
}
