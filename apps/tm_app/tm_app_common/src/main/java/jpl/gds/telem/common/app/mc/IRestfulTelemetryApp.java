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
package jpl.gds.telem.common.app.mc;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.ConfigurationDumpUtility;
import jpl.gds.common.config.GeneralProperties;
import jpl.gds.common.service.telem.ITelemetrySummary;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.context.cli.app.mc.IRestFulServerCommandLineApp;
import jpl.gds.shared.config.GdsHierarchicalProperties;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.Markers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;

/**
 * Common Restful controller Interface for downlink server applications
 *
 *
 */
public interface IRestfulTelemetryApp extends IRestFulServerCommandLineApp {



    /**
     * @return the current processing state of chill_down
     */
    public default DownlinkProcessingState getProcessingState() {
        return DownlinkProcessingState.UNBOUND;
    }

    /**
     * Stops processing of raw input. The input stream is no longer read.
     *
     * @throws IllegalStateException
     *             If a component was not in the proper state for stopping
     *
     * Throws IllegalStateException if
     *          sessionManager encounters an issue while stopping
     */
    public void stop();

    /**
     * Indicates the current downlink session should be aborted.
     */
    public void abort();

    /**
     * Pauses processing of raw input; the incoming data is thrown away
     * while paused.
     */
    public void pause();

    /**
     * Resumes processing of raw input.
     */
    public void resume();

    /**
     * Gets the summary object for the latest session. If the session has not been stopped
     * or completed, the data in the summary object will be incomplete. If a session has never
     * been started, this method will return null.
     *
     * @return SessionSummary object, or null if run() has never been called.
     */
    public ITelemetrySummary getSessionSummary();


    /**
     * Gets the name of the application script for this application.
     *
     * @return script name
     */
    public String getAppName();

    /**
     * Clear the buffer within the telemetry input handler's InputStream
     *
     * @throws IOException
     *             if the <code>clearBufferCallable</code> threw an exception,
     *             was interrupted while waiting, or could not be scheduled for
     *             execution
     */
    public void clearInputStreamBuffer() throws IOException;

    /**
     * Gets the current application context.
     *
     * @return application context
     */
    public ApplicationContext getAppContext();

    /**
     * Determines if the downlink has ever been started
     *
     * @return true if it has been started, false if it has never been started
     */
    boolean hasBeenStarted();

    /**
     * Executes the downlink process, and allow the app to decide whether it is the command line version or the GUI
     * version
     */
    public void launchApp();

    /**
     * Executes the downlink process as a command line application.
     *
     */
    public void launchCommandLineApp();

    /**
     *
     * If the downlink command-line app is started (--noGUI), --autoRun is NOT specified, and the RESTful Interface is
     * enabled, downlink will wait for a RESTful 'start' command before beginning to process downlink data. Calling this
     * method will start the processing of downlink data.
     *
     * If the above preconditions are not met, then this method does nothing.
     */
    void startDownlink();


    /**
     * @param springContext
     *            the Spring Application Context
     */
    public static void writeOutConfigProperties(final ApplicationContext springContext) {
        final ConfigurationDumpUtility cdu = new ConfigurationDumpUtility(springContext);
        final Map<String, String> props = cdu.collectProperties(true, false, GdsHierarchicalProperties.PropertySet.NO_DESCRIPTIVES);
        final Tracer log = TraceManager.getTracer(springContext, Loggers.CONFIG);

        log.info(Markers.SUPPRESS, "==================Configuration Dump==================");
        for (final Map.Entry<String, String> entry : props.entrySet()) {
            log.info(Markers.SUPPRESS, "Property: " + entry.getKey() + "=" + entry.getValue());
        }
        log.info(Markers.SUPPRESS, "==================End Configuration Dump==================");
    }

    /**
     * Writes out a context configuration to the user's configuration directory. Will only write
     * out the config if in a non-integrated GUI configuration OR there has been a
     * context restart.
     *
     * @param inSpringContext
     *            the current application context
     * @param contextConfig
     *            the IContextConfiguration to write
     * @param restart
     *            true if this is a session restart, false if not
     *
     * Added flag to call to control whether config is written
     *          based upon whether there has been a session restart
     */
    public static void writeOutContextConfig(final ApplicationContext inSpringContext,
                                             final IContextConfiguration contextConfig, final boolean restart) {
        if (GdsSystemProperties.isIntegratedGui() && !restart) {
            return;
        }
        final Tracer log = TraceManager.getTracer(inSpringContext, Loggers.CONFIG);
        final String outputFile = GdsSystemProperties.getUserConfigDir() + File.separator
                + inSpringContext.getBean(GeneralProperties.class).getDefaultContextConfigFileName();

        try {
            final File f = new File(GdsSystemProperties.getUserConfigDir());
            boolean ok = true;
            if (!f.exists()) {
                ok = f.mkdirs();
            }
            if (!ok) {
                log.warn("Error making directories for output file " + outputFile);
            }
            else {
                contextConfig.save(outputFile);
                log.debug("Wrote configuration to ", outputFile);
            }
        }
        catch (final IOException e) {
            log.warn("Error writing output file " + outputFile + ": " + e.toString());
        }
    }

    /**
     * Gets the current context configuration.
     *
     * @return IContextConfiguration object
     */
    public IContextConfiguration getContextConfiguration();

    /**
     * Sets the context configuration and resets
     * the instance fields if the input flag is set. The latter will force a new
     * context configuration to be created in the database during execution.
     *
     * @param config
     *            the IContextConfiguration to set
     * @param setInstanceFields
     *            true to clear the start time and context number from the
     *            configuration, which will trigger the creation of a new database key
     *
     */
    public void setContextConfiguration(final IContextConfiguration config, final boolean setInstanceFields);
}
