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
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.context.api.ISimpleContextConfiguration;
import jpl.gds.context.cli.app.mc.IRestFulServerCommandLineApp;
import jpl.gds.session.config.SessionConfiguration;
import jpl.gds.shared.exceptions.ApplicationException;
import jpl.gds.telem.common.app.mc.rest.resources.WorkerId;
import jpl.gds.telem.common.state.WorkerState;
import org.springframework.context.ApplicationContext;

import javax.annotation.PostConstruct;
import java.util.List;

/**
 * Interface that defines a Telemetry Server that manages workers
 * Throws ApplicationException when errors are found
 *
 */
public interface ITelemetryServer extends IRestFulServerCommandLineApp, Runnable {
    /**
     * Init app
     */
    @PostConstruct
    void init();

    /**
     * Create worker from Session, insert into DB, and schedule for execution (TI and TP)
     * 
     * @param config
     *            SessionConfiguration
     * @return IContextIdentification
     */
    IContextIdentification createWorker(SessionConfiguration config) throws ApplicationException;

    /**
     * Create worker from Session, set arguments into Session, insert into DB, and schedule for execution (TI and TP)
     * 
     * @param config
     *            SessionConfiguration
     * @param args
     *            Command-line arguments to configure the session with
     * @return IContextIdentification
     */
    IContextIdentification createWorker(SessionConfiguration config, final String[] args) throws ApplicationException;

    /**
     * Start worker
     *
     * @param key Session key
     * @param host Session host
     * @param fragment Session fragment
     */
    void startWorker(long key, String host, int fragment);

    /**
     * Stop worker
     * @param key Session key
     * @param host Session host
     * @param fragment Session fragment
     */
    void stopWorker(long key, String host, int fragment);

    /**
     * Release worker
     *
     * @param key Session key
     * @param host Session host
     * @param fragment Session fragment
     */
    void releaseWorker(long key, String host, int fragment);

    /**
     * Abort worker
     *
     * @param key Session key
     * @param host Session host
     * @param fragment Session fragment
     */
    void abortWorker(long key, String host, int fragment);

    /**
     * Get worker by session
     * @param key Session key
     * @param host Session host
     * @param fragment Session fragment
     *
     * @return IProcessWorker
     */
    ITelemetryWorker getWorker(long key, String host, int fragment);

    /**
     * Get list of worker IDs
     *
     * @return the list of worker IDs
     * @see jpl.gds.shared.metadata.context.ContextKey toString() for the format of the IDs
     */
    List<WorkerId> getWorkers();


    /**
     * Get all workers
     * @return List of ITelemetryWorker objects
     */
    List<ITelemetryWorker> getAllWorkers();

    /**
     * Gets the summary object for the provided session. If the session has not been stopped or completed, the data in
     * the summary object will be incomplete. If a session has never been started, this method will return null.
     *
     * @param key Session key
     * @param host Session host
     * @param fragment Session fragment
     *
     * @return SessionSummary object, or null if run() has never been called.
     */
    ITelemetrySummary getSessionSummary(long key, String host, int fragment);

    /**
     * Gets the current application context.
     *
     * @return application context
     */
    ApplicationContext getAppContext();

    /**
     * Determines if the downlink has ever been started
     *
     * @param key Session key
     * @param host Session host
     * @param fragment Session fragment
     *
     * @return true if it has been started, false if it has never been started
     */
    boolean hasBeenStarted(long key, String host, int fragment);

    /**
     * Gets the current context configuration.
     *
     * @return IContextConfiguration object
     */
    ISimpleContextConfiguration getContextConfiguration();

    /**
     * Get worker state
     * @param key Session key
     * @param host Session host
     * @param fragment Session fragment
     * @return the current processing state of chill_down
     */
    WorkerState getWorkerState(long key, String host, int fragment);

    /**
     * Copies necessary values from the parent server to the worker configuration
     *
     * @param workerContext The worker spring application context
     * @param workerSession The workers session configuration
     */
    void setValuesFromParent(final ApplicationContext workerContext, final IContextConfiguration workerSession);


    /**
     * Retrieve a list of the available dictionaries
     *
     * @param directory dictionary directory (optional)
     * @return list of available dictionaries
     */
    List<String> getAvailableDictionaries(final String directory);
}