package uk.ac.bioss.cowtastrophe.controls;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import uk.ac.bioss.cowtastrophe.ControlStrategy;
import uk.ac.bioss.cowtastrophe.DiseaseState;
import uk.ac.bioss.cowtastrophe.Farm;
import uk.ac.bioss.cowtastrophe.Simulation;

/**
 * A cull / vaccinate / movement combination
 */
@Slf4j
public class CullVacMoveCombi extends ControlStrategy implements Serializable {

    /**
     * Create the control strategy using the JSON formatted string.
     * @param cull
     * @param vacc
     * @param vacrad
     * @param move
     * @param moveRad
     */
    public CullVacMoveCombi(final int cull, final int vacc, final double vacrad, final int move, final double moveRad) {
        log.info("Using CullVacMove Combi Strategy");
        vacradius = vacrad;
        culling = cull;
        vaccinate = vacc;
	movement = move;
	movement_rad = moveRad;
    }

    @Override
    public final void run(final Simulation simulation) {
		doMovement(simulation);
		doMovementCost(simulation);
        doVac(simulation);
        doCull(simulation);
    }
	
	public final void doMovementCost(final Simulation simulation) {
		simulation.getStatistics().addCost(simulation.getDay(), 
			simulation.getParameters().getCostOfMvmtBanPerDay() * simulation.getRestrictedFarms().size());
	}
	
	public final void doMovement(final Simulation simulation) {
		if(movement == ControlStrategy.MOVE_NOT) {
			return;
		}
		Set<Farm> MoveSourceList;
		if(movement == ControlStrategy.MOVE_ON_CON) {
			MoveSourceList = simulation.getConfirmedFarms();
		}else {
			MoveSourceList = simulation.getSuspectedOrConfirmedFarms();
		}
		for (Farm farm : MoveSourceList) {
			
            for (Farm farm2 : simulation.getHelper().getAllFarmsWithindistance(farm, movement_rad)) {
                simulation.getRestrictedFarms().add(farm2.getId());
            }
        }
	}

	
    public final void doCull(final Simulation simulation) {
        if (culling == ControlStrategy.CULL_NOT) {
            return;
        }
        
        Set<Farm> toBeCulled;
        if (culling == ControlStrategy.CULL_ON_CON) {
            toBeCulled = simulation.getConfirmedFarms();
        } else {
            toBeCulled = simulation.getSuspectedOrConfirmedFarms();
        }

        log.trace("Farms to be culled {}", toBeCulled.stream()
                  .map(Farm::getId)
                  .sorted()
                  .collect(Collectors.toList()));

        for (Farm farm : toBeCulled) {
            farm.setDayCulled(simulation.getDay());
            farm.setStatus(DiseaseState.CULLED);
            simulation.getStatistics().addCost(simulation.getDay(),
                                               farm.getHerdSize() * simulation.getParameters().getCostOfCullingAnimal()
                                               + simulation.getParameters().getCostOfFarmVisit());
        }
    }

    public final void doVac(final Simulation simulation) {
        if (vaccinate == ControlStrategy.VAC_NOT) {
            return;
        }
        
        Set<Farm> toBeVaccinated = new HashSet<>();
		
        if (vaccinate == ControlStrategy.VAC_ON_CON) {
            for (Farm farm : simulation.getConfirmedFarms()) {
                toBeVaccinated.addAll(simulation.getHelper().getAllFarmsWithindistance(farm, vacradius));
            }
        } else {
            for (Farm farm : simulation.getSuspectedOrConfirmedFarms()) {
                toBeVaccinated.addAll(simulation.getHelper().getAllFarmsWithindistance(farm, vacradius));
            }
        }
        
        log.trace("Farms to be vaccinated {}", toBeVaccinated.stream()
                  .map(Farm::getId)
                  .sorted()
                  .collect(Collectors.toList()));

        for (Farm farm : toBeVaccinated) {
			if(farm.getStatus() == DiseaseState.SUSCEPTIBLE || farm.getStatus() == DiseaseState.INFECTIOUS_NOT_SUSPECTED) {
				farm.setDayVaccinated(simulation.getDay());
				farm.setStatus(DiseaseState.VACCINATED);
				simulation.getStatistics().addCost(simulation.getDay(),
                                               farm.getHerdSize() * simulation.getParameters()
                                               .getCostOfVaccinatingAnimal());
			}
        }
    }

    /**
     * A public identifier (name) of the strategy.
     */
    public static final String name = "Cull_and_vaccinate_and_movement_restrict";

    private final double vacradius;
    private final int culling;
    private final int vaccinate;
	private final int movement;
	private final double movement_rad;
	

    /**
     * The serialVersionUID.
     */
    private static final long serialVersionUID = 8762536431743027427L;
}
