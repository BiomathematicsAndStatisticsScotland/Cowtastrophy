package uk.ac.bioss.cowtastrophe.controls;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import uk.ac.bioss.cowtastrophe.ControlStrategy;
import uk.ac.bioss.cowtastrophe.Farm;
import uk.ac.bioss.cowtastrophe.Simulation;

/**
 * A control strategy that imposes a movement restriction.
 */
@Slf4j
public class MovementRestriction extends ControlStrategy implements Serializable {

    static class Params implements Serializable {

        public Params() {
        }

        public int farmId;
        public double radius;
        public int days = 1000;
        private static final long serialVersionUID = 10924432869572879L;
    }

    /**
     * Create the control strategy using the JSON formatted string.
     * @param jsonNode the parameters for the control.
     */
    public MovementRestriction(final JsonNode jsonNode) {
        log.info("Using Movement Restriction Strategy");
        restrictions = new HashMap<>();
        try {
            ObjectMapper mapper = new ObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            MovementRestriction.Params[] params = mapper.treeToValue(jsonNode.get("parameters"),
                                                                     MovementRestriction.Params[].class);
            for (MovementRestriction.Params param : params) {
                restrictions.put(param.farmId, param);
            }

        } catch (JsonProcessingException ex) {
            log.error("Could not process Ring Vaccination parameters: \n{}",
                      Throwables.getStackTraceAsString(ex));
        }
    }
// TODO: the number of days should be VERY long be default - the idea is that there is no lifting of restriction.
    @Override
    public void run(Simulation simulation) {
        for (Map.Entry<Integer, MovementRestriction.Params> entry : restrictions.entrySet()) {
            final int farmId = entry.getKey();
            final double radius = entry.getValue().radius;
            final int days = entry.getValue().days;
            final Farm thisFarm = simulation.getHelper().getFarmById(farmId);

            List<Farm> allFarms = simulation.getHelper().getAllFarmsWithindistance(thisFarm, radius);

            log.trace("Farms to be placed under restriction {}", allFarms.stream()
                      .map(Farm::getId)
                      .sorted()
                      .collect(Collectors.toList()));

            for (Farm farm : allFarms) {
                simulation.getRestrictedFarms().add(farm.getId());
                simulation.getStatistics().addCost(simulation.getDay(),
                                                   simulation.getParameters()
                                                           .getCostOfMvmtBanPerDay() * days);

                // now schedule when the movement ban should be lifted.
                Collection<Integer> list = simulation.getEaseMvmtRestriction()
                        .getOrDefault(simulation.getDay() + days, new ArrayList<>());
                list.add(farm.getId());
                simulation.getEaseMvmtRestriction().put(simulation.getDay() + days, list);
            }
        }
    }

    /**
     * A public identifier (name) of the strategy.
     */
    public static final String name = "Movement_restriction";

    private final Map<Integer, MovementRestriction.Params> restrictions;

    /**
     * The serialVersionUID.
     */
    private static final long serialVersionUID = 1008489311840574586L;
}
