/*
 * Copyright 2006-2018. California Institute of Technology.
 * ALL RIGHTS RESERVED.
 * U.S. Government sponsorship acknowledged.
 *
 * This software is subject to U. S. export control laws and
 * regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 * extent that the software is subject to U.S. export control laws
 * and regulations, the recipient has the responsibility to obtain
 * export licenses or other export authority as may be required
 * before exporting such information to foreign countries or
 * providing access to foreign nationals.
 */
package jpl.gds.shared.sys;

import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jpl.gds.shared.cli.app.AbstractCommandLineApp;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.config.ReleaseProperties;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Markers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.spring.BeanUtil;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.util.HostPortUtility;

/**
 * Handler class that deals with SIGINT and SIGTERM signals
 *
 */
public class QuitSignalHandler implements Runnable {
    /** ICommandLine */
    protected ICommandLine           cmdline;
    
    private final IQuitSignalHandler handler;
    private final IAccurateDateTime  start;
    private final String 			 appName;

    /**
     * @param handler
     *            The application IQuitSignalHandler
     * 
     */
    public QuitSignalHandler(final IQuitSignalHandler handler) {
        this.handler = handler;
        start = new AccurateDateTime();
        appName = ApplicationConfiguration.getApplicationName();
    }
    

    @Override
    public void run() {
    		// First check for help and version opt, do nothing if present
    		if (AbstractCommandLineApp.helpDisplayed.get() || AbstractCommandLineApp.versionDisplayed.get()) { 
    			return;
    		}
    		
    		// Instantiate logger locally - not global. Aviods unnecessarily starting the logging context 
        final Tracer log = TraceManager.getDefaultTracer();
        log.info(Markers.SYS, appName," received a shutdown request. Attempting to shut down gracefully...");
        
        try {
            if (cmdline != null) {
                logCommandlineSummary(log);
            }
        } catch (final JsonProcessingException e) {
            log.error(Markers.SYS, "Error creating JSON command line summary: ", ExceptionTools.getMessage(e), e);
        }

        logApplicationSummary(log);

        try {
            handler.exitCleanly();
        }
        catch (final Exception e) {
            log.error(Markers.SYS, "Error shutting down ", appName, ": ", ExceptionTools.getMessage(e), e);
        }
        finally {
            // LogManager.getContext() will start the logging service if it was not initialized
            // There is currently no api to check if logging has started, without initializing it
            // Added checks for help and version opt to prevent unnecessarily starting the logging context
            if (LogManager.getContext() instanceof LoggerContext) {
                TraceManager.shutdown(LoggerContext.getContext());
            }
            else {
                log.debug("Logger context was not log4j2");
            }
        }
    }
    
    /**
     * Logs an "Application Summary" message 
     * 
     * @param log Tracer to log with
     */
    private void logApplicationSummary(final Tracer log) { 
    		final IAccurateDateTime end = new AccurateDateTime();
        log.info(Markers.SYS, GdsSystemProperties.getSystemUserName(), "@", HostPortUtility.getLocalHostName(),
                " Shutting down the ", appName, " application.",
                " StartTime=", start.getFormattedErt(true), 
                ", EndTime=", end.getFormattedErt(true), 
                ", ElapsedTime=", (end.getTime() - start.getTime()), 
                ", PID=",GdsSystemProperties.getPid(), 
                ", Product=", ReleaseProperties.getProductLine(), " ", ReleaseProperties.getVersion());
    }


    /**
     * Creates a command line summary for the application execution.
     * An application "end time" is created at the time this method is called
     * 
     * @param  log Tracer to log with
     * @throws JsonProcessingException
     *             if an error occurs parsing JSON
     */
    private void logCommandlineSummary(final Tracer log) throws JsonProcessingException {
        final IAccurateDateTime end = new AccurateDateTime();
        // top level nodes
        final ObjectMapper mapper = new ObjectMapper();
        final ArrayNode summary = mapper.createArrayNode();

        // POJO nodes
        final ObjectNode command = mapper.createObjectNode();
        final ObjectNode configuration = mapper.createObjectNode();
        final ObjectNode execution = mapper.createObjectNode();


        final ObjectNode configDirs = mapper.createObjectNode();
        configDirs.put("SYSTEM", GdsSystemProperties.getGdsDirectory());

        try { 
            configDirs.put("MISSION", GdsSystemProperties.getProjectConfigDir(BeanUtil.getBean(SseContextFlag.class).isApplicationSse()));
        } catch(final Exception e) { 
            configDirs.put("MISSION", GdsSystemProperties.getProjectConfigDir());
        }
        configDirs.put("USER", GdsSystemProperties.getUserConfigDir());
        configuration.putPOJO("CONFIGURATION", configDirs);

        final ObjectNode environment = mapper.createObjectNode();
        environment.put("CHILL_GDS", GdsSystemProperties.getGdsDirectory());
        environment.put("GDS_JAVA_OPTS", System.getenv("GDS_JAVA_OPTS"));
        environment.put("ACTIVEMQ_HOME", System.getenv("ACTIVEMQ_HOME"));
        environment.put("TMP", System.getenv("TMPDIR"));
        configuration.putPOJO("ENVIRONMENT", environment);

        final ObjectNode stats = mapper.createObjectNode();
        stats.put("USER", GdsSystemProperties.getSystemUserName());
        stats.put("HOST", HostPortUtility.getLocalHostName());
        stats.put("PID", GdsSystemProperties.getPid());
        stats.put("PRODUCT", ReleaseProperties.getProductLine());
        stats.put("SUBSYSTEM", "MPCS");
        stats.put("START", start.getFormattedErt(true));
        stats.put("END", end.getFormattedErt(true));
        stats.put("DELTA_MS", end.getTime() - start.getTime());
        execution.putPOJO("EXECUTION", stats);

        final ObjectNode optionNode = mapper.createObjectNode();
        optionNode.put("APP", ApplicationConfiguration.getApplicationName());
        if (cmdline != null && !cmdline.getAllOptions().isEmpty()) {
            for (final Entry<String, String> e : cmdline.getAllOptions().entrySet()) {
                optionNode.put(e.getKey(), e.getValue());
            }
        }
        command.putPOJO("COMMAND", optionNode);

        // add user and option nodes to summary node array
        summary.add(command);
        summary.add(configuration);
        summary.add(execution);

        log.info(Markers.SUPPRESS, "Command line summary:\n",
                                             mapper.writer().writeValueAsString(summary));
    }

    /**
     * Sets the applications ICommandLine in the shutdown hook. This enables a command line summary when a sigterm
     * signal is encountered
     * 
     * @param cmdline
     *            The applications ICommandLine
     */
    public void setCommandLine(final ICommandLine cmdline) {
        this.cmdline = cmdline;
    }


}
