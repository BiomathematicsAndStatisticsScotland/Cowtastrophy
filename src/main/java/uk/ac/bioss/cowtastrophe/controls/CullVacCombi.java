package uk.ac.bioss.cowtastrophe.controls;

import java.io.Serializable;
import java.util.ArrayList;
import lombok.extern.slf4j.Slf4j;
import uk.ac.bioss.cowtastrophe.ControlStrategy;
import uk.ac.bioss.cowtastrophe.DiseaseState;
import uk.ac.bioss.cowtastrophe.Farm;
import uk.ac.bioss.cowtastrophe.Simulation;

/**
 * A cull / vaccinate combination
 */
@Slf4j
public class CullVacCombi extends ControlStrategy implements Serializable {

    /**
     * Create the control strategy using the JSON formatted string.
     * @param params the parameters for the control.
     */
    public CullVacCombi(final int cull, final int vacc, final double vacrad) {
        log.info("Using CullVac Combi Strategy");
        vacradius = vacrad;
        culling = cull;
        vaccinate = vacc;
    }

    @Override
    public final void run(final Simulation simulation) {
        doCull(simulation);
        doVac(simulation);
    }

    public final void doCull(final Simulation simulation) {
        if (culling == ControlStrategy.CULL_NOT) {
            return;
        }
        ArrayList<Farm> toBeCulled;
        if (culling == ControlStrategy.CULL_ON_CON) {
            toBeCulled = new ArrayList<>(simulation.getConfirmedFarms());
        } else {
            toBeCulled = new ArrayList<>(simulation.getSuspectedFarms());
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

    public final void doVac(final Simulation simulation) {
        if (vaccinate == ControlStrategy.VAC_NOT) {
            return;
        }
        ArrayList<Farm> toBeVaccinated = new ArrayList<>();
        if (vaccinate == ControlStrategy.VAC_ON_CON) {
            for (Farm farm : simulation.getConfirmedFarms()) {
                toBeVaccinated.addAll(simulation.getHelper().getAllFarmsWithindistance(farm, vacradius));
            }
        } else {
            for (Farm farm : simulation.getSuspectedFarms()) {
                toBeVaccinated.addAll(simulation.getHelper().getAllFarmsWithindistance(farm, vacradius));
            }
        }
        log.info("doVac - " + toBeVaccinated.size());
        for (Farm farm : toBeVaccinated) {
            farm.setDayVaccinated(simulation.getDay());
            farm.setStatus(DiseaseState.VACCINATED);
            simulation.getConfirmedFarms().remove(farm);
            simulation.getSuspectedFarms().remove(farm);
            simulation.getSusceptibleFarms().remove(farm);
            simulation.getVaccinatedFarms().add(farm);
            simulation.getStatistics().addCost(simulation.getDay(),
                                               farm.getHerdSize() * simulation.getParameters()
                                               .getCostOfVaccinatingAnimal());
        }
    }
    
    /**
     * A public identifier (name) of the strategy.
     */
    public static final String name = "Ring_vaccination";

    private final double vacradius;
    private final int culling;
    private final int vaccinate;

    /**
     * The serialVersionUID.
     */
    private static final long serialVersionUID = 8762536431743027427L;
}
