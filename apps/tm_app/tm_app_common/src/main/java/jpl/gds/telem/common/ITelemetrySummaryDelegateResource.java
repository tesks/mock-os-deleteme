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

package jpl.gds.telem.common;

import jpl.gds.telem.common.state.WorkerState;

/**
 * Common interface for telemetry summary delegate resources
 * @see jpl.gds.common.service.telem.ITelemetrySummary
 */
public interface ITelemetrySummaryDelegateResource {

    /**
     * Get context key
     * @return Context Key
     */
    long getContextKey();

    /**
     * Get control state
     * @return Control state
     */
    WorkerState getControlState();

    /**
     * Get full name
     * @return Full name
     */
    String getFullName();

    /**
     * Get output directory
     * @return output directory
     */
    String getOutputDirectory();

    /**
     * Get start time
     * @return start time
     */
    String getStartTime();

    /**
     * Get stop time
     * @return stop time
     */
    String getStopTime();

    /**
     * Gets the status of the worker
     *
     * @return the status of the worker
     */
    ITelemetryStatus getStatus();
}
