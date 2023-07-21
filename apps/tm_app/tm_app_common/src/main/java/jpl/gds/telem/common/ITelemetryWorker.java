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

package jpl.gds.telem.common;

import jpl.gds.common.service.telem.ITelemetrySummary;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.shared.cli.app.ICommandLineApp;
import jpl.gds.shared.exceptions.ApplicationException;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.MessageSubscriber;
import jpl.gds.shared.sys.IQuitSignalHandler;
import jpl.gds.shared.sys.SystemUtilities;
import jpl.gds.telem.common.app.mc.rest.resources.DownlinkStatusResource;
import jpl.gds.telem.common.state.WorkerState;
import org.springframework.context.ApplicationContext;

/**
 * Common interface for TI and TP workers
 *
 */
public interface ITelemetryWorker extends IQuitSignalHandler, Runnable, ICommandLineApp, MessageSubscriber {
    /**
     * Determines if the downlink has ever been started
     *
     * @return true if it has been started, false if it has never been started
     */
    boolean hasBeenStarted();

    /**
     * Aborts an ACTIVE ITelemetryWorker
     *
     * @throws IllegalStateException when a telemetry worker is not active
     */
    void abort() throws IllegalStateException;

    /**
     * Stops an ACTIVE ITelemetryWorker
     *
     * @throws IllegalStateException when a telemetry worker is not active
     */
    void stop() throws IllegalStateException;

    /**
     * Release a STOPPED ITelemetryWorker
     *
     * @throws IllegalStateException when a telemetry worker is not active
     */
    void release() throws IllegalStateException;

    /**
     * Gets the current context configuration.
     *
     * @return IContextConfiguration object
     */
    IContextConfiguration getContextConfiguration();

    /**
     * Gets the summary object for the latest session. If the session has not been stopped or completed, the data in the
     * summary object will be incomplete. If a session has never been started, this method will return null.
     *
     * @return SessionSummary object, or null if run() has never been called.
     */
    ITelemetrySummary getSessionSummary();

    /**
     * Gets the current application context.
     *
     * @return application context
     */
    ApplicationContext getAppContext();

    /**
     * Gets the WorkerState for an ITelemetryWorker
     * @return the current control state of a telemetry worker
     */
    WorkerState getState();

    /**
     * Update list of session IDs associated with the Telemetry Server
     *
     * @param sessionIds List fo session IDs (separated by colon)
     * @throws ApplicationException If there are problems updating the database
     */
    void updateContextDbSessionsList(String sessionIds) throws ApplicationException;

    /**
     * Exposes the workers Tracer for REST API logging
     * @return gets the worker's Tracer object
     */
    Tracer getTracer();

    /**
     * Get performance status
     * @return DownlinkStatusResource object
     */
    DownlinkStatusResource getPerfStatus();

    /**
     * Get telemetry status
     * @return ITelemetryDelegateResource implementation
     */
    ITelemetrySummaryDelegateResource getTelemStatus();

    @Override
    default void showHelp() {
        SystemUtilities.doNothing();
    }

    @Override
    default void showVersion() {
        SystemUtilities.doNothing();
    }
}
