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
package jpl.gds.telem.common.state;

/**
 * Enum for ITelemetryWorker state change actions
 *
 */
public enum WorkerStateChangeAction {

    /** Create new worker */
    CREATE,

    /** Create a new worker and attach it to an existing session */
    ATTACH,

    /** Start worker */
    START,

    /** Stop worker */
    STOP,

    /** Abort worker */
    ABORT,

    /** Release worker */
    RELEASE
}
