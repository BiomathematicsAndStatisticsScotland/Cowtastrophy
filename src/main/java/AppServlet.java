
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

import javax.servlet.http.Cookie;
import java.util.Enumeration;
import org.apache.commons.io.IOUtils;


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

        log.info("Running");
        if ("runToNextEvent".equals(request.getParameter("mode"))) {
            log.info("Running Simulation to Next Event");
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
		printRequest(request);
		// This is an example of what must be done to set the simulation code up
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
		if(ses == null || "".equals(ses)){
			log.info("empty session..");
			simulation = new Simulation(this.getServletConfig().getInitParameter("BaseDirectory"),
                                        this.getServletConfig().getInitParameter("SettingsFile"));
		} else {
			String tf = request.getParameter("timeframe");
			String basedir = this.getServletConfig().getInitParameter("BaseDirectory");
			String sessionFile = basedir + File.separator + ses + File.separator +  ses + "_" + tf + ".ser";
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
		if(request.getParameter("culling") != null) culling = request.getParameter("culling");
		String vaccinate = "";
		if(request.getParameter("vaccinate") != null) vaccinate = request.getParameter("vaccinate");
		String vacradius = "0";
		if(request.getParameter("vacradius") != null) vacradius = request.getParameter("vacradius");
		int cullInt = ControlStrategy.CULL_NOT;
		if(culling.equals("1")) cullInt = ControlStrategy.CULL_ON_SUS;
		if(culling.equals("2")) cullInt = ControlStrategy.CULL_ON_CON;
		int vaccInt = ControlStrategy.VAC_NOT;
		if(vaccinate.equals("1")) vaccInt = ControlStrategy.VAC_ON_SUS;
		if(vaccinate.equals("2")) vaccInt = ControlStrategy.VAC_ON_CON;
		double vacrad = -1;
		if(vacradius != null && !"".equals(vacradius)) {
			vacrad = Double.parseDouble(vacradius);
		}
		ControlStrategy strategy = ControlStrategyFactory.create(cullInt, vaccInt, vacrad);
        simulation.setControlStrategy(strategy);

        if ("24Hours".equals(request.getParameter("mode"))) {
            simulation.run24Hours();
        } else if ("run".equals(request.getParameter("mode"))) {
            simulation.run();
        } else if ("true".equals(request.getParameter("getStatistics"))) {
            // TODO: perhaps run() can return statistics???
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
	 * Prints the request.
	 *
	 * @param httpServletRequest the http servlet request
	 */
	private void printRequest(final HttpServletRequest httpServletRequest) {
		if (httpServletRequest == null) {
			return;
		}
		log.info("----------------------------------------");
		log.info("W4 HttpServletRequest");
		log.info("\tRequestURL : {}", httpServletRequest.getRequestURL());
		log.info("\tRequestURI : {}", httpServletRequest.getRequestURI());
		log.info("\tScheme : {}", httpServletRequest.getScheme());
		log.info("\tAuthType : {}", httpServletRequest.getAuthType());
		log.info("\tEncoding : {}", httpServletRequest.getCharacterEncoding());
		log.info("\tContentLength : {}", httpServletRequest.getContentLength());
		log.info("\tContentType : {}", httpServletRequest.getContentType());
		log.info("\tContextPath : {}", httpServletRequest.getContextPath());
		log.info("\tMethod : {}", httpServletRequest.getMethod());
		log.info("\tPathInfo : {}", httpServletRequest.getPathInfo());
		log.info("\tProtocol : {}", httpServletRequest.getProtocol());
		log.info("\tQuery : {}", httpServletRequest.getQueryString());
		log.info("\tRemoteAddr : {}", httpServletRequest.getRemoteAddr());
		log.info("\tRemoteHost : {}", httpServletRequest.getRemoteHost());
		log.info("\tRemotePort : {}", httpServletRequest.getRemotePort());
		log.info("\tRemoteUser : {}", httpServletRequest.getRemoteUser());
		log.info("\tSessionID : {}", httpServletRequest.getRequestedSessionId());
		log.info("\tServerName : {}", httpServletRequest.getServerName());
		log.info("\tServerPort : {}", httpServletRequest.getServerPort());
		log.info("\tServletPath : {}", httpServletRequest.getServletPath());

		/*log.info("");
		log.info("\tCookies");
		int i = 0;
		for (final Cookie cookie : httpServletRequest.getCookies()) {
			log.info("\tCookie[{}].name={}", i, cookie.getName());
			log.info("\tCookie[{}].comment={}", i, cookie.getComment());
			log.info("\tCookie[{}].domain={}", i, cookie.getDomain());
			log.info("\tCookie[{}].maxAge={}", i, cookie.getMaxAge());
			log.info("\tCookie[{}].path={}", i, cookie.getPath());
			log.info("\tCookie[{}].secured={}", i, cookie.getSecure());
			log.info("\tCookie[{}].value={}", i, cookie.getValue());
			log.info("\tCookie[{}].version={}", i, cookie.getVersion());
			i++;
		}*/
		log.info("\tDispatcherType : {}", httpServletRequest.getDispatcherType());
		log.info("");

		log.info("\tHeaders");
		int j = 0;
		final Enumeration<String> headerNames = httpServletRequest.getHeaderNames();
		while (headerNames.hasMoreElements()) {
			final String headerName = headerNames.nextElement();
			final String header = httpServletRequest.getHeader(headerName);
			log.info("\tHeader[{}].name={}", j, headerName);
			log.info("\tHeader[{}].value={}", j, header);
			j++;
		}

		log.info("\tLocalAddr : {}", httpServletRequest.getLocalAddr());
		log.info("\tLocale : {}", httpServletRequest.getLocale());
		log.info("\tLocalPort : {}", httpServletRequest.getLocalPort());

		log.info("");
		log.info("\tParameters");
		int k = 0;
		final Enumeration<String> parameterNames = httpServletRequest.getParameterNames();
		while (parameterNames.hasMoreElements()) {
			final String paramName = parameterNames.nextElement();
			final String paramValue = httpServletRequest.getParameter(paramName);
			log.info("\tParam[{}].name={}", k, paramName);
			log.info("\tParam[{}].value={}", k, paramValue);
			k++;
		}

		log.info("");
		log.info("\tParts");
		int l = 0;
		try {
			for (final Object part : httpServletRequest.getParts()) {
				log.info("\tParts[{}].class={}", l, part != null ? part.getClass() : "");
				log.info("\tParts[{}].value={}", l, part != null ? part.toString() : "");
				l++;
			}
		} catch (final Exception e) {
			log.error("NO MULTIPART");
		}
	
		try {
			log.info("Request Body : {}",
					IOUtils.toString(httpServletRequest.getInputStream(), httpServletRequest.getCharacterEncoding()));
			log.info("Request Object : {}", new ObjectInputStream(httpServletRequest.getInputStream()).readObject());
		} catch (final Exception e) {
			log.debug("Exception e", e);
		}
		log.info("----------------------------------------");
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
