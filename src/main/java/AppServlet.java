
import broadwick.BroadwickException;
import broadwick.LoggingFacade;
import ch.qos.logback.classic.Level;
import com.google.common.base.Throwables;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
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
 *
 */
public class AppServlet extends HttpServlet {

    private Simulation simulation;
    @Getter
    private Logger log;
    private final String logFormatThreadMsg = "[%thread] %-5level %msg %n";

    @Override
    public void init() {
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

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     * @param request  servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
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
            ControlStrategy strategy = ControlStrategyFactory.create(request.getParameter("controlStrategy"));
            simulation.setControlStrategy(strategy);
        }

        if ("24Hours".equals(request.getParameter("mode"))) {
            simulation.run24Hours();
        } else if ("run".equals(request.getParameter("mode"))) {
            simulation.run();
        } else if ("true".equals(request.getParameter("getStatistics"))) {
            // TODO: perhaps run() can return statistics???
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
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");

        PrintWriter out = response.getWriter();
        try {
            out.println("<h2>POST: Welcome to my first servlet app.</h2>");
            out.println("<p>");
            out.println(request.toString());
            out.println("<p>");
            for (Map.Entry<String, String[]> entry : request.getParameterMap().entrySet()) {
                String[] get = entry.getValue();
                for (String str : get) {
                    out.println("<h5> " + entry.getKey() + " = " + str + "</h5>");
                }
            }
        } finally {
            out.close();
        }
    }

    /**
     * Returns a short description of the servlet.
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
