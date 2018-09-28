package uk.ac.bioss.cowtastrophe;

import broadwick.BroadwickException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Throwables;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import java.util.Properties;
import lombok.ToString;

/**
 * All the parameters for the simulation are encapsulated in this class.
 */
@Slf4j
@ToString
public class Parameters implements Serializable {

    /**
     * No args constructor. Do not use, it is added so that parameters can be constructed
     * from a JSON object.
     */
    public Parameters() {

    }

    /**
     * Construct the parameters object from a JSON file.
     * @param jsonFile the name (including path) of the JSON file.
     */
    public Parameters(final String jsonFile) {
        
        log.info("Loading simulation data from {}", jsonFile);

        ObjectMapper mapper = new ObjectMapper();
        try (InputStream input = new FileInputStream(jsonFile)) {
            JsonNode json = mapper.readTree(input);
            Parameters params = mapper.treeToValue(json.get("parameters"), Parameters.class);
            
            this.directory = params.getDirectory();
            this.beta = params.getBeta();
            this.endTime = params.getEndTime();
            this.costOfFarmVisit = params.getCostOfFarmVisit();
            this.costOfTestPerAnimal = params.getCostOfTestPerAnimal();
            this.costOfCullingAnimal = params.getCostOfCullingAnimal();
            this.costOfVaccinatingAnimal = params.getCostOfVaccinatingAnimal();
            this.suspectedTestDelay = params.getSuspectedTestDelay();
            this.kernelOffset = params.getKernelOffset();
            this.kernelPower = params.getKernelPower();
            this.restrictedKernelPower = params.getRestrictedKernelPower();
            this.costOfMvmtBanPerDay = params.getCostOfMvmtBanPerDay();

            farms.clear();
            farms.addAll(Arrays.asList(mapper.treeToValue(json.get("farms"), Farm[].class)));

            seedFarms.clear();
            seedFarms.addAll(Arrays.asList(mapper.treeToValue(json.get("seedFarms"), Farm[].class)));

            log.debug("Loaded parameters {}", this.toString());

        } catch (IOException ex) {
            throw new BroadwickException("Error loading simulation info; "
                                         + Throwables.getStackTraceAsString(ex));
        }            
    }

    /**
     * Check that the properties that have been read are valid.
     * @param prop the properties to check.
     */
    private void validateProperties(final Properties prop) {
        // The properties file must have either
        // 1. a "presets" line giving the name of a json file containing the
        //    configured parameters.
        // or
        // 2. all the parameters (map, numFarms, numSeeds, transmissionTerm).

        boolean containsPresetsKey = prop.containsKey("presets")
                                     && prop.containsKey("directory");
        boolean containsParamKeys = prop.containsKey("map")
                                    && prop.containsKey("numFarms")
                                    && prop.containsKey("numSeeds")
                                    && prop.containsKey("transmissionParameter")
                                    && prop.containsKey("endTime")
                                    && prop.containsKey("costOfTest")
                                    && prop.containsKey("costOfCullingAnimal")
                                    && prop.containsKey("directory");

        if (!(containsPresetsKey || containsParamKeys)) {
            throw new BroadwickException("Could not find required properties.");
        }
    }

    /** The base directory for all the configs/data files. */
    @Getter
    @Setter
    private String directory;
    /** The disease transmission parameter. */
    @Getter
    @Setter
    private double beta;
    /** The final time of the simulation. */
    @Getter
    @Setter
    private int endTime;
    /** The time delay (in days) between a farm being suspected to being confirmed. */
    @Getter
    @Setter
    private int suspectedTestDelay;
    /** The cost of visiting a farm. */
    @Getter
    @Setter
    private double costOfFarmVisit;
    /** The cost (per animal) of performing a test. */
    @Getter
    @Setter
    private double costOfTestPerAnimal;
    /** The cost (per animal) of culling an animal. */
    @Getter
    @Setter
    private double costOfCullingAnimal;
    @Getter
    @Setter
    private double kernelPower;
    @Getter
    @Setter
    private double restrictedKernelPower;
    @Getter
    @Setter
    private double kernelOffset;
    /** The cost (per animal) of vaccinating an animal. */
    @Getter
    @Setter
    private double costOfVaccinatingAnimal;
    /** The cost (per day) of being under movement restriction. */
    @Getter
    @Setter
    private double costOfMvmtBanPerDay;
    @Getter
    private final List<Farm> farms = new ArrayList<>();
        @Getter
    private final List<Farm> seedFarms = new ArrayList<>();
    /** The serialVersionUID. */
    private static final long serialVersionUID = 1859289310330274528L;
}
