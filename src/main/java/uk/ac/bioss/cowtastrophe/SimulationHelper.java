package uk.ac.bioss.cowtastrophe;

import broadwick.io.FileOutput;
import com.google.common.base.Throwables;
import static com.google.common.collect.MoreCollectors.onlyElement;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.Writer;
import java.lang.management.ManagementFactory;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * A class that holds methods that help the simulation (such as saving state etc.) that don't necessarily belong in the
 * simulation class.
 */
@Slf4j
public class SimulationHelper implements Serializable {

    /**
     * Create a helper object for the simulation.
     * @param sim the simulation to help.
     */
    SimulationHelper(final Simulation sim) {
        this.simulation = sim;
        this.parameters = simulation.getParameters();
    }

    /**
     * Get the distance between two farms.
     * @param f1 a farm.
     * @param f2 another farm.
     * @return the distance between f1 and f2.
     */
    public final double getFarmDistance(final Farm f1, final Farm f2) {
        return Math.sqrt(Math.pow(f1.getX() - f2.getX(), 2)
                         + Math.pow(f1.getY() - f2.getY(), 2));
    }

    /**
     * Get a farm from the list of farms by its id.
     * @param farmId the id of the farm.
     * @return the farm.
     */
    public final Farm getFarmById(final int farmId) {
        return simulation.getFarms().stream()
                .filter(f -> f.getId() == farmId)
                .collect(onlyElement());
    }

    /**
     * Get a list of all farms that are within a given radius of a reference farm.
     * @param f1       the reference farm
     * @param distance the specified radius.
     * @return a list of farms within distance from f1.
     */
    public final List<Farm> getAllFarmsWithindistance(final Farm f1,
                                                      final double distance) {
        return simulation.getFarms().stream().filter(f -> getFarmDistance(f1, f) <= distance)
                .collect(Collectors.toList());
    }

    /**
     * Serialise the simulation to a file.
     * @param sessionId the session id for this process (will be used as the filename).
     * @param time      the simulation time of the session (will be used as the filename).
     * @return the file that the simulation was serialised to.
     */
    public final String saveSession(final String sessionId, final double time) {
        String sessionFile = parameters.getDirectory() + sessionId + File.separator
                             + sessionId + "_" + time + ".ser";
        String jsonFile = parameters.getDirectory() + sessionId + File.separator
                          + sessionId + "_" + time + ".json";

        try (ObjectOutputStream serFile
                                = new ObjectOutputStream(new FileOutputStream(sessionFile));
             FileOutput jsFile = new FileOutput(jsonFile)) {
            // we need to save the simulation with the cleanup flag set to true so that
            // when we run the simulation from a session file, subsequent session files from
            // a previous run are deleted, we do this by cloning the simulation and editing 
            // the cloned object.
            Simulation cloned = (Simulation) org.apache.commons.lang.SerializationUtils.clone(this.simulation);
            cloned.setCleanupRequired(true);
            serFile.writeObject(cloned);
            jsFile.write(cloned.asJson());
        } catch (Exception ex) {
            log.error("Error saving session; see exception for details");
            log.error(Throwables.getStackTraceAsString(ex));
        }

        // save it to a file called [sessionId]_[time].ser
        log.info("Saved session to " + sessionFile);
        return sessionFile;
    }

    /**
     * Save the process id of the current process.
     * @param sessionId the sessin id for this process (will be used as the filename).
     * @return the pid
     */
    public final int savePid(final String sessionId) {
        // from Java 9 do this
        //int pid = ProcessHandle.current().pid();
        int pid = Integer.parseInt(ManagementFactory.getRuntimeMXBean().getName().split("@")[0]);

        savePid(sessionId, String.valueOf(pid));

        return pid;
    }

    /**
     * Save the process id of the current process.
     * @param sessionId the session id for this process (will be used as the filename).
     * @param pid       the process id to save.
     */
    public final void savePid(final String sessionId, final String pid) {
        // save it to a file called [sessionId].pid
        String sessionFile = parameters.getDirectory() + sessionId + File.separator
                             + sessionId + ".pid";

        try (Writer wr = new FileWriter(sessionFile)) {
            wr.write(pid);
        } catch (Exception ex) {
            log.error("Error saving session pid; see exception for details");
            log.error(Throwables.getStackTraceAsString(ex));
        }
    }

    /**
     * Save the parameters to a file (specified in the parameters object) in json format.
     */
//    public final void saveParametersAsJson() {
//        String mapFile = Files.getNameWithoutExtension(parameters.getMap());
//        File file = new File(parameters.getDirectory() + "presets/" + mapFile + ".json");
//
//        ObjectMapper mapper = new ObjectMapper();
//        mapper.enable(SerializationFeature.INDENT_OUTPUT);
//
//        try (JsonGenerator g = mapper.getFactory().createGenerator(new FileOutputStream(file))) {
//            mapper.writeValue(g, simulation);
//            log.info("Saved parameters as {}", file.getAbsoluteFile());
//        } catch (IOException ex) {
//            throw new BroadwickException("Error saving parameters as json; "
//                                         + Throwables.getStackTraceAsString(ex));
//        }
//    }
    /**
     * Generate a unique id for this session.
     * @return the session id.
     */
    public final String generateSessionId() {
        final int length = 16;
        SecureRandom random = new SecureRandom();
        BigInteger bigInteger = new BigInteger(130, random);

        //return String.valueOf(java.util.Calendar.getInstance().getTime().hashCode()
        //                      + bigInteger.toString(length));
        return String.valueOf(bigInteger.toString(length));
    }

//    /**
//     * Generate the farms, sampling from the map to determine the farm location.
//     */
//    public final void generateFarms() {
//        try {
//            log.info("generating farms for map {}",
//                     parameters.getDirectory() + parameters.getMap());
//            File mapFile = new File(parameters.getDirectory() + parameters.getMap());
//            BufferedImage img = ImageIO.read(mapFile);
//            final int imgMaxX = img.getWidth();
//            final int imgMaxY = img.getHeight();
//
//            Graphics graphics = img.createGraphics();
//            graphics.setColor(StateColour.SUSCEPTIBLE_FARM.getColour());
//
//            int farmsPlaced = 0;
//            while (farmsPlaced < parameters.getNumFarms()) {
//                int randX = ThreadLocalRandom.current().nextInt(0, imgMaxX);
//                int randY = ThreadLocalRandom.current().nextInt(0, imgMaxY);
//                java.awt.Color c = new java.awt.Color(img.getRGB(randX, randY));
//
//                final int farmSize = ThreadLocalRandom.current().nextInt(2, 5);
//                // only place a farm if  the pixel and all the pixels within a
//                // radius of farmSize
//                //       are white (255,255,255)
//                //       are (254,254,254) with probability = 10%
//                //       are (253,253,253) with probability = 2%
//                // Farms are coloured black (0,0,0) the sea/rivers are coloured blue
//                // (0,0,255) and cities are coloured (250,250,250)
//                if (StateColour.LOW_DENSITY.isEqual(c)) {
//                    if (placeFarmAt(img, graphics, farmsPlaced + 1, randX, randY, farmSize,
//                                    0.02)) {
//                        farmsPlaced++;
//                    }
//                } else if (StateColour.MED_DENSITY.isEqual(c)) {
//                    if (placeFarmAt(img, graphics, farmsPlaced + 1, randX, randY, farmSize, 0.1)) {
//                        farmsPlaced++;
//                    }
//                } else if (StateColour.BACKGROUND.isEqual(c)) {
//                    if (placeFarmAt(img, graphics, farmsPlaced + 1, randX, randY, farmSize, 1.0)) {
//                        farmsPlaced++;
//                    }
//                }
//            }
//
//            // Save the farms while we have the map.
//            ImageIO.write(img, "png", new File(parameters.getDirectory() + "savedMap.png"));
//
//            // now set the seed farms.
//            while (simulation.getSuspectedFarms().size() < parameters.getNumSeeds()) {
//                final ThreadLocalRandom current = ThreadLocalRandom.current();
//                Farm inftd = Iterables.get(simulation.getFarms(),
//                                           current.nextInt(0, simulation.getFarms().size()));
//                inftd.setStatus(DiseaseState.SUSPECTED);
//                simulation.getSuspectedFarms().add(inftd);
//            }
//        } catch (IOException ex) {
//            throw new BroadwickException("Error generating farms; see exception for details. "
//                                         + Throwables.getStackTraceAsString(ex));
//        }
//    }
//
//    /**
//     * Attempt to create and place a farm at a given location on a given map.
//     * @param img       the BufferedImage of the map on which the farms will be created.
//     * @param graphics  the graphics object for the img file.
//     * @param id        the id of the farm that will be created/placed.
//     * @param latitude  the latitude (x) coordinate to place the farm.
//     * @param longitude the longitude (y) coordinate to place the farm.
//     * @param size      the size of the farm (a farm will be a square centered on the coordinates
//     *                  whose lengths are 2*side+1)
//     * @param p         the probability of placing a farm at the given coordinates.
//     * @return true if the farm was placed at the coordinates.
//     */
//    private boolean placeFarmAt(final BufferedImage img, final Graphics graphics, final int id,
//                                final int latitude, final int longitude,
//                                final int size, final double p) {
//
//        if (ThreadLocalRandom.current().nextDouble() > p) {
//            return false;
//        }
//
//        boolean isLocationValid = true;
//
//        // Here all farms are square with side 2xsize+1
//        for (int x = latitude - size; x <= latitude + size; x++) {
//            for (int y = longitude - size; y <= longitude + size; y++) {
//                java.awt.Color c = new java.awt.Color(img.getRGB(x, y));
//
//                isLocationValid = isLocationValid & StateColour.BACKGROUND.isEqual(c);
//
//                if (!isLocationValid) {
//                    break;
//                }
//            }
//        }
//
//        if (isLocationValid) {
//            graphics.fillRect(latitude - size, longitude - size, 2 * size + 1, 2 * size + 1);
//            Farm farm = new Farm(id, latitude, longitude, size);
//            simulation.getFarms().add(farm);
//        }
//        return isLocationValid;
//    }

    /*
    public void cleanUpMap() {
        // not to be used in production - only used to tidy up the pixels in a map.
        try {
            log.info("cleaning map {}", parameters.getDirectory() + parameters.getMap());
            File mapFile = new File(parameters.getDirectory() + parameters.getMap());
            BufferedImage img = ImageIO.read(mapFile);
            final int imgMaxX = img.getWidth();
            final int imgMaxY = img.getHeight();

            Graphics graphics = img.createGraphics();
            graphics.setColor(Color.black);

            for (int x = 0; x < imgMaxX; x++) {
                for (int y = 0; y < imgMaxY; y++) {
                    java.awt.Color c = new java.awt.Color(img.getRGB(x, y));

                    // if the colour is mostly blue paint it blue and if it's mostly green
                    // paint it white.
                    if (c.getBlue() > 80 && c.getGreen() < 120) {
                        graphics.setColor(Color.blue);
                        graphics.fillRect(x, y, 1, 1);
                    } else if (c.getBlue() < 200 && c.getGreen() > 100) {
                        graphics.setColor(Color.white);
                        graphics.fillRect(x, y, 1, 1);
                    } else if (c.getRed() > 60 && c.getGreen() > 60) {
                        graphics.setColor(Color.white);
                        graphics.fillRect(x, y, 1, 1);
                    }

                    if ((c.getRed() == 0 && c.getGreen() == 0 && c.getBlue() == 254)) {
                        graphics.setColor(Color.blue);
                        graphics.fillRect(x, y, 1, 1);
                    }

                    if ((c.getRed() == 1 && c.getGreen() == 1 && c.getBlue() == 255)) {
                        graphics.setColor(Color.blue);
                        graphics.fillRect(x, y, 1, 1);
                    }

                    if ((c.getRed() == 254 && c.getGreen() == 254 && c.getBlue() == 254)
                        || (c.getRed() == 253 && c.getGreen() == 253 && c.getBlue() == 254)) {
                        graphics.setColor(Color.white);
                        graphics.fillRect(x, y, 1, 1);
                    }

                    // Report on any pixels that are neither blue nor white
                    if (!((c.getRed() == 0 && c.getGreen() == 0 && c.getBlue() == 255)
                          || (c.getRed() == 255 && c.getGreen() == 255 && c.getBlue() == 255))) {
                        log.error("Pixel (" + x + ", " + y + ") is an invalid colour ("
                                            + c.getRed() + ", " + c.getGreen() + ", "
                                            + c.getBlue() + ")");
                    }
                }
            }

            ImageIO.write(img, "png", new File(parameters.getDirectory() + "savedMap.png"));

        } catch (IOException ex) {
            throw new BroadwickException("Error cleaning map; see exception for details. "
                                         + Throwables.getStackTraceAsString(ex));
        }
    }
     */
    /**
     * The parameters of the simulation.
     */
    private final Parameters parameters;
    /**
     * The simulation object that is being helped.
     */
    private final Simulation simulation;
    /**
     * The serialVersionUID.
     */
    private static final long serialVersionUID = 495884972383424504L;
}
