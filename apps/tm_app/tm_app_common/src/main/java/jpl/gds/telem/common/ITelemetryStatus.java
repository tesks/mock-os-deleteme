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

/**
 * Common interface to use for reporting telemetry 'status' for an ITelemetryWorker
 */
public interface ITelemetryStatus {

    /**
     * Whether or not the ITelemetryWorker is 'working' on something
     *
     * @return true if the worker is actively doing something; false otherwise
     */
    boolean isWorking();
}
