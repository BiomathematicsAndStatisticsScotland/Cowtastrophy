import broadwick.BroadwickException;
import broadwick.LoggingFacade;
import ch.qos.logback.classic.Level;
import com.google.common.base.Throwables;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import lombok.Getter;
import org.slf4j.Logger;
import uk.ac.bioss.cowtastrophe.ControlStrategy;
import uk.ac.bioss.cowtastrophe.Simulation;
import uk.ac.bioss.cowtastrophe.controls.ControlStrategyFactory;


/**
 *  A basic app servlet for responding to cowtastrophy client requests.
 */
public class AppServlet extends HttpServlet {

    private Simulation simulation;
    @Getter
    private Logger log;
    private final String logFormatThreadMsg = "[%thread] %-5level %msg %n";

    @Override
    public final void init() {
        final LoggingFacade logFacade = new LoggingFacade();
        log = logFacade.getRootLogger();
        try {
            // The valid levels are INFO, DEBUG, TRACE, ERROR, ALL
            Path logFilePath = Paths.get(this.getServletConfig().getInitParameter("BaseDirectory"),
                                  "/uk.ac.bioss.cowtastrophe.log");
            logFacade.addFileLogger(logFilePath.toString(), "TRACE", logFormatThreadMsg, true);
            logFacade.addConsoleLogger("INFO", logFormatThreadMsg);

            logFacade.getRootLogger().setLevel(Level.TRACE);
        } catch (BroadwickException ex) {
            log.error("{}\nSomething went wrong starting project. See the error messages.",
                      ex.getLocalizedMessage());
            //log.trace(Throwables.getStackTraceAsString(ex));
        }
    }

    /**
     * Handles the HTTP <code>GET</code> method.
     * @param request  servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected final void doGet(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {

        // This is an example of what must be done to set the simulation code up
        if (simulation == null) {
            init();
            for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
                String[] get = entry.getValue();
                for (String str : get) {
                    log.info("{} = {} ", entry.getKey(), str);
                }
            }
            simulation = new Simulation(this.getServletConfig().getInitParameter("BaseDirectory"),
                                        this.getServletConfig().getInitParameter("SettingsFile"));
        }

        if (!request.getParameter("session").isEmpty()) {
            try (ObjectInputStream ois
                  = new ObjectInputStream(new FileInputStream(request.getParameter("session")))) {
                simulation = (Simulation) ois.readObject();
                log.trace("Running with settings {}", simulation.getParameters().toString());
            } catch (Exception ex) {
                log.error("Error loading session; see exception for details");
                log.error(Throwables.getStackTraceAsString(ex));
            }
        }

        if (request.getParameter("controlStrategy") != null) {
            ControlStrategy strategy =
                    ControlStrategyFactory.create(request.getParameter("controlStrategy"));
            simulation.setControlStrategy(strategy);
        }

        log.info("Running");
        if ("runToNextEvent".equals(request.getParameter("mode"))) {
            log.info("Running Simulation to Next Event");
            simulation.run24Hours();
        } else if ("run".equals(request.getParameter("mode"))) {
            simulation.run();
        //} else if ("true".equals(request.getParameter("getStatistics"))) {
            //simulation.getStatistics().asJson(); // need to write this method (ise toString()).
        }

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(simulation.asJson());
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     * @param request  servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected final void
        doPost(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {
        if (simulation == null) {
            log.info("doPost - init()");
            init();
            for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
                String[] get = entry.getValue();
                for (String str : get) {
                    log.info("{} = {} ", entry.getKey(), str);
                }
            }
            //simulation = new Simulation(this.getServletConfig().getInitParameter("BaseDirectory"),
            //                            this.getServletConfig().getInitParameter("SettingsFile"));
        }
        String ses = request.getParameter("session_id");
        if (ses == null || "".equals(ses)) {
            log.info("empty session..");
            simulation = new Simulation(this.getServletConfig().getInitParameter("BaseDirectory"),
                                        this.getServletConfig().getInitParameter("SettingsFile"));
        } else {
            String tf = request.getParameter("timeframe");
            int tfi = Integer.parseInt(tf) - 1;
            String basedir = this.getServletConfig().getInitParameter("BaseDirectory");
            String sessionDir = new File(basedir, ses).getAbsolutePath();
            String sessionFile = new File(sessionDir, ses + "_" + tfi + ".ser").getAbsolutePath();
            log.info("file: " + sessionFile);
            try (ObjectInputStream ois
                                   = new ObjectInputStream(new FileInputStream(sessionFile))) {
                simulation = (Simulation) ois.readObject();
                log.trace("Running with settings {}", simulation.getParameters().toString());
            } catch (Exception ex) {
                log.error("Error loading session; see exception for details");
                log.error(Throwables.getStackTraceAsString(ex));
            }
        }
        String culling = "";
        if (request.getParameter("culling") != null) {
            culling = request.getParameter("culling");
        }
        String vaccinate = "";
        if (request.getParameter("vaccinate") != null) {
            vaccinate = request.getParameter("vaccinate");
        }
        String vacradius = "0";
        if (request.getParameter("vacradius") != null) {
            vacradius = request.getParameter("vacradius");
        }
        int cullInt = ControlStrategy.CULL_NOT;
        if (culling.equals("1")) {
            cullInt = ControlStrategy.CULL_ON_SUS;
        }
        if (culling.equals("2")) {
            cullInt = ControlStrategy.CULL_ON_CON;
        }
        int vaccInt = ControlStrategy.VAC_NOT;
        if (vaccinate.equals("1")) {
            vaccInt = ControlStrategy.VAC_ON_SUS;
        }
        if (vaccinate.equals("2")) {
            vaccInt = ControlStrategy.VAC_ON_CON;
        }
        double vacrad = -1;
        if (vacradius != null && !"".equals(vacradius)) {
            vacrad = Double.parseDouble(vacradius);
        }
        String move = "";
        if (request.getParameter("move") != null) {
            move = request.getParameter("move");
        }
        String moveradius = "0";
        if (request.getParameter("moveradius") != null) {
            moveradius = request.getParameter("moveradius");
        }
        int moveInt = ControlStrategy.MOVE_NOT;
        if (move.equals("1")) {
            moveInt = ControlStrategy.MOVE_ON_SUS;
        }
        if (move.equals("2")) {
            moveInt = ControlStrategy.MOVE_ON_CON;
        }
        double moverad = -1;
        if(moveradius != null && !"".equals(moveradius)) {
            moverad = Double.parseDouble(moveradius);
        }
        ControlStrategy strategy = ControlStrategyFactory.create(cullInt, vaccInt, vacrad, moveInt, moverad);
        simulation.setControlStrategy(strategy);

        if ("24Hours".equals(request.getParameter("mode"))) {
            simulation.run24Hours();
        } else if ("run".equals(request.getParameter("mode"))) {
            simulation.run();
        //} else if ("true".equals(request.getParameter("getStatistics"))) {
            //simulation.getStatistics().asJson(); // need to write this method (ise toString()).
        }


        PrintWriter out = response.getWriter();
        try {
            out.write(simulation.asJson());
        } finally {
            out.close();
        }
    }

    /**
     * Returns a short description of the servlet.
     * @return a String containing servlet description
     */
    @Override
    public final String getServletInfo() {
        return "Short description";
    }

}
