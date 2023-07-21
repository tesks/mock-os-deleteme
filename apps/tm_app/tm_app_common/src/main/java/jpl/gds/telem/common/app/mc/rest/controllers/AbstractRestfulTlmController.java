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
package jpl.gds.telem.common.app.mc.rest.controllers;


import jpl.gds.db.api.rest.IRestfulDbHelper;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.exceptions.RestfulTelemetryException;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.metadata.context.IContextKey;
import jpl.gds.shared.string.StringResponse;
import jpl.gds.telem.common.ITelemetryServer;
import jpl.gds.telem.common.ITelemetryWorker;
import jpl.gds.telem.common.app.mc.rest.resources.WorkerStateChangeResponse;
import jpl.gds.telem.common.state.WorkerState;
import jpl.gds.telem.common.state.WorkerStateChangeAction;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;

/**
 * Abstract common REST controller for downlink telemetry applications (TI/TP).
 *
 * As much as possible, return actual objects from the REST API calls, rather than ResponseEntity
 * Spring will serialize these automatically
 * All APIs should return JSON
 *
 *
 */
public class AbstractRestfulTlmController {

    /**
     * Worker Started status
     */
    private static final String WORKER_STARTED = "WORKER STARTED";

    /**
     * Worker Stopped status
     */
    private static final String WORKER_STOPPED = "WORKER STOPPED";

    /**
     * Worker Released status
     */
    private static final String WORKER_RELEASED = "WORKER RELEASED";

    /**
     * Worker Released status
     */
    private static final String WORKER_ABORTED = "WORKER ABORTED";

    /**
     * Exiting Server status
     */
    private static final String EXITING_SERVER = "EXITING SERVER";

    /**
     * Worker Not Attached status
     */
    protected static final String WORKER_NOT_ATTACHED = "WORKER NOT ATTACHED";

    /**
     * Worker Not Ready status
     */
    protected static final String WORKER_NOT_READY = "WORKER NOT READY";

    /**
     * Worker Not Active status
     */
    protected static final String WORKER_NOT_ACTIVE = "WORKER NOT ACTIVE";

    /**
     * Worker Not Running status
     */
    protected static final String WORKER_NOT_RUNNING = "WORKER NOT RUNNING";

    /**
     * Worker Not Started status
     */
    protected static final String WORKER_NOT_STARTED = "WORKER NOT STARTED";

    /**
     * not stopped status
     */
    protected static final String WORKER_NOT_STOPPED = "WORKER NOT STOPPED";

    /**
     * Worker Already Stopped status
     */
    protected static final String WORKER_ALREADY_STOPPED = "WORKER ALREADY STOPPED";

    /**
     * Worker Not Released status
     */
    protected static final String WORKER_NOT_RELEASED = "WORKER NOT RELEASED";

    /**
     * Internal Server Error
     */
    protected static final String INTERNAL_SERVER_ERROR = "Internal Server Error";

    /**
     * Session key description
     */
    protected static final String PARAM_SESSION_KEY = "the key of a session; unsigned integer";

    /**
     * Session host description
     */
    protected static final String PARAM_SESSION_HOST = "session hostname";

    /**
     * Session fragment description
     */
    protected static final String PARAM_SESSION_FRAGMENT = "session fragment; unsigned integer";

    /**
     * Directory parameter description
     */
    protected static final String PARAM_DICT_NAME = "dictionary name to cache";

    private static final String WORKER_NOT_READY_OR_STOPPED = "WORKER IS NOT READY OR STOPPED ";

    /**
     * Worker state before state change
     */
    protected WorkerState priorState;

    /**
     * Worker state after state change
     */
    protected WorkerState newState;

    protected final ApplicationContext appContext;
    protected final IRestfulDbHelper   logHelper;

    protected Tracer log;

    /**
     * Abstract REST controller Constructor
     * @param appContext Spring application context
     */
    public AbstractRestfulTlmController(final ApplicationContext appContext) {
        this.appContext = appContext;
        this.logHelper = appContext.getBean(IRestfulDbHelper.class);
    }


    /**
     * Shutdown server
     *
     * @param app The ITelemetryServer to stop
     * @return StringResponse
     */

    protected StringResponse execShutdown(final ITelemetryServer app) {
        try {
            if (null != app) {
                app.exitCleanly();
                return new StringResponse(EXITING_SERVER);
            }
            else {
                throw new RestfulTelemetryException(HttpStatus.PRECONDITION_FAILED, "Server is null");

            }
        }
        catch (final Exception e) {
            throw new RestfulTelemetryException(HttpStatus.INTERNAL_SERVER_ERROR, ExceptionTools.getMessage(e));
        }
    }

    /**
     * Change worker state based on the specified state change action
     *
     * @param workerManager     The ITelemetryServer worker manager that handles the worker state change operation
     * @param key               Session key of worker
     * @param host              Session host of worker
     * @param fragment          Session fragment of worker
     * @param stateChangeAction The worker state change action
     * @return WorkerStateChangeResponse
     */
    protected synchronized WorkerStateChangeResponse changeState(final ITelemetryServer workerManager, final long key,
                                                                 final String host, final int fragment, final WorkerStateChangeAction stateChangeAction) {

        String message = "";
        final ITelemetryWorker worker = workerManager.getWorker(key, host, fragment);

        // Make sure worker exists
        if (worker == null) {
            throw new RestfulTelemetryException(HttpStatus.PRECONDITION_FAILED,
                                                "Worker with ID: " + key + "/" + host + "/" + fragment + " does not exist");
        }

        final IContextKey contextKey = worker.getContextConfiguration().getContextId().getContextKey();
        final WorkerState currentState = worker.getState();
        priorState = currentState;

        switch (stateChangeAction) {
            case START:
                if (currentState == WorkerState.READY) {
                    workerManager.startWorker(key, host, fragment);
                    message = WORKER_STARTED;
                    newState = WorkerState.ACTIVE;
                }
                else {
                    throw new RestfulTelemetryException(HttpStatus.PRECONDITION_FAILED,
                                                        WORKER_NOT_READY + " (" + currentState + ")");
                }
                break;
            case STOP:
                if (currentState == WorkerState.ACTIVE) {
                    workerManager.stopWorker(key, host, fragment);
                    message = WORKER_STOPPED;
                    newState = WorkerState.STOPPED;
                }
                else {
                    throw new RestfulTelemetryException(HttpStatus.PRECONDITION_FAILED,
                                                        WORKER_NOT_ACTIVE + " (" + currentState + ")");
                }
                break;
            case ABORT:
                if (currentState != WorkerState.STOPPED) {
                    workerManager.abortWorker(key, host, fragment);
                    message = WORKER_ABORTED;
                    newState = WorkerState.STOPPED;
                }
                else {
                    throw new RestfulTelemetryException(HttpStatus.PRECONDITION_FAILED,
                                                        WORKER_ALREADY_STOPPED + " (" + currentState + ")");
                }
                break;
            case RELEASE:
                if (currentState == WorkerState.STOPPED || currentState == WorkerState.READY) {
                    workerManager.releaseWorker(key, host, fragment);
                    message = WORKER_RELEASED;
                    newState = null;
                }
                else {
                    throw new RestfulTelemetryException(HttpStatus.PRECONDITION_FAILED,
                                                        WORKER_NOT_READY_OR_STOPPED + "(" + currentState + ")");
                }
                break;
            default:
                throw new RestfulTelemetryException(HttpStatus.INTERNAL_SERVER_ERROR,
                                                    "Unrecognised Worker State Change Action: " + stateChangeAction);
        }

        return new WorkerStateChangeResponse(message, contextKey, priorState, newState);
    }
}
