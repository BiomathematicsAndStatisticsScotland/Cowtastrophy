
import uk.ac.bioss.cowtastrophe.controls.ControlStrategyFactory;
import broadwick.BroadwickException;
import broadwick.LoggingFacade;
import ch.qos.logback.classic.Level;
import com.google.common.base.Throwables;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import uk.ac.bioss.cowtastrophe.ControlStrategy;
import uk.ac.bioss.cowtastrophe.Simulation;

/**
 * Application class that drives the simulation. The methods from here will eventually be incorporated in the servlet.
 */
public class App {

    /**
     * Create the application, setting up the logging.
     * @param args the command line arguments.
     */
    public App(final String[] args) {

        final LoggingFacade logFacade = new LoggingFacade();
        log = logFacade.getRootLogger();
        try {
            // The valid levels are INFO, DEBUG, TRACE, ERROR, ALL
            logFacade.addFileLogger("uk.ac.bioss.cowtastrophe.log", "TRACE", logFormatThreadMsg,
                                    true);
            logFacade.addConsoleLogger("INFO", logFormatThreadMsg);

            logFacade.getRootLogger().setLevel(Level.TRACE);
        } catch (BroadwickException ex) {
            log.error("{}\nSomething went wrong starting project. See the error messages.",
                      ex.getLocalizedMessage());
            //log.trace(Throwables.getStackTraceAsString(ex));
        }
    }

    /**
     * Initialise the application.
     */
    public final void init() {
        log.info("Initialising project");
    }

    /**
     * Finalise the application.
     */
    public final void finalise() {
        log.info("Closing project : {}", simulation.getSessionId());

        // empty the contents of the pid file
        simulation.finalise();
    }

    /**
     * Read the give properties file, create the simulation and run it in the required mode.
     * @param controlStrategy the control strategy to be used.
     */
    private void runFromOptionsFile(final ControlStrategy controlStrategy) {

        final String path = new File("").getAbsolutePath();
        final String dir = new File(path, "resources").getAbsolutePath();
        log.info("Base directory = {}", dir);
        final String params = new File(dir, "cowtastrophe.properties").getAbsolutePath();

        simulation = new Simulation(dir, params);
        simulation.setControlStrategy(controlStrategy);
        run();

    }

    /**
     * Restore the simulation from the serialised session file and run the simulation.
     * @param sessionIdFile   the name of the file (including the path) of the serialised
     * @param controlStrategy the control strategy to be used. file to be restored.
     */
    private void runFromSessionIdFile(final String sessionIdFile,
                                      final ControlStrategy controlStrategy) {
        log.info("Recreating project from session file {}", sessionIdFile);

        try (ObjectInputStream ois
                               = new ObjectInputStream(new FileInputStream(sessionIdFile))) {
            simulation = (Simulation) ois.readObject();
            simulation.setControlStrategy(controlStrategy);
            log.trace("Running with settings {}", simulation.getParameters().toString());
            run();

        } catch (Exception ex) {
            log.error("Error loading session; see exception for details");
            log.error(Throwables.getStackTraceAsString(ex));
        }
    }

    /**
     * Run the simulation, either to completion or for one day (the default).
     */
    private void run() {
        switch (mode) {
            case FAST_FORWARD:
                simulation.run();
                break;
            case SINGLE_DAY:
                simulation.run24Hours();
                break;
        }
    }

    /**
     * Invocation point.
     * <p>
     * @param args the command line arguments passed to Broadwick.
     */
    public static void main(final String[] args) {
        final App app = new App(args);
        app.init();

        try {
            final CommandLineOptions cli = new CommandLineOptions(args);

            app.setMode(cli.getMode());

            String sessionIdFileName = cli.getsessionId();

            ControlStrategy control = ControlStrategyFactory.create(cli.getcontrolStrategy());
            app.getLog().trace("command line options: \n control = {}", cli.getcontrolStrategy());

            if (!(sessionIdFileName.isEmpty())) {
                app.runFromSessionIdFile(sessionIdFileName, control);
            } else {
                app.runFromOptionsFile(control);
            }

            app.finalise();

        } catch (BroadwickException ex) {
            app.getLog().error("{}\nSomething went wrong starting project. See the error message.",
                               Throwables.getStackTraceAsString(ex));
        }
    }

    @Getter
    private Logger log;
    private Simulation simulation;
    private String logFormatThreadMsg = "[%thread] %-5level %msg %n";
    @Setter
    private Mode mode;
}
