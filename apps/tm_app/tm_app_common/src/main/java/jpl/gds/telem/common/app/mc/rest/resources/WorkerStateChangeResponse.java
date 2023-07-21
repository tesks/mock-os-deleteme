/*
 * Copyright 2006-2019. California Institute of Technology.
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
package jpl.gds.telem.common.app.mc.rest.resources;

import jpl.gds.shared.metadata.context.IContextKey;
import jpl.gds.telem.common.state.WorkerState;

/**
 * Simple POJO used as a worker state change container within REST response objects
 * for JSON serialization
 */
public class WorkerStateChangeResponse {

    private String message;
    private WorkerState priorState;
    private WorkerState currentState;
    private WorkerId workerId;

    /**
     * Constructor which takes a message in string form, the Context Key of the worker,
     * the state of the worker before change and state of the worker after change.
     *
     * @param message The state change confirmation message
     * @param contextKey The context key of the worker
     * @param priorState The state of the worker before change
     * @param newState The state of the worker after change
     */
    public WorkerStateChangeResponse(final String message, final IContextKey contextKey,
                                     final WorkerState priorState, final WorkerState newState) {
        this.message = message;
        this.workerId = new WorkerId(contextKey);
        this.priorState = priorState;
        this.currentState = newState;
    }

    /**
     * Get the state of the worker before the state change operation.
     * If the prior state was null, the string 'N/A' will be returned
     *
     * @return the prior state of the worker
     */
    public String getPriorState() {
        return priorState == null ? "N/A" : priorState.toString();
    }

    /**
     * Set the state of the worker before the state change operation
     *
     * @param priorState the prior state of the worker
     */
    public void setPriorState(final WorkerState priorState) {
        this.priorState = priorState;
    }

    /**
     * Get the current state of the worker after the state change operation
     * If the current state is null, the string 'N/A' will be returned
     *
     * @return the current state of the worker
     */
    public String getCurrentState() {
        return currentState == null ? "N/A" : currentState.toString();
    }

    /**
     * Set the current stat of the worker after the state change operation
     *
     * @param currentState the current state of the worker
     */
    public void setCurrentState(final WorkerState currentState) {
        this.currentState = currentState;
    }

    /**
     * Get the worker id object
     *
     * @return the WorkerId object
     */
    public WorkerId getWorkerId() {
        return workerId;
    }

    /**
     * Set the worker id object
     *
     * @param workerId the WorkerId object
     */
    public void setWorkerId(final WorkerId workerId) {
        this.workerId = workerId;
    }

    /**
     * Get the message associated with the state change operation
     *
     * @return the state change message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Set the state change message
     *
     * @param message the state change message
     */
    public void setMessage(final String message) {
        this.message = message;
    }

}
