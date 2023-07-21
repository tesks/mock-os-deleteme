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
package jpl.gds.telem.common.worker;

import jpl.gds.shared.exceptions.ExcessiveInterruptException;
import jpl.gds.shared.thread.SleepUtilities;
import jpl.gds.telem.common.ITelemetryWorker;
import jpl.gds.telem.common.state.WorkerState;

/**
 * Utility methods for telemetry workers
 *
 */
public class WorkerUtils {

    /**
     * Poll a worker every 1/10 of a second for the specified duration in seconds,
     * or until the target state has been reached
     *
     * @param worker The ITelemetryWorker
     * @param target The target WorkerState
     * @param duration The duration to wait in seconds
     * @throws ExcessiveInterruptException
     */
    public static void pollWorker(final ITelemetryWorker worker, final WorkerState target,
                                  final int duration) throws ExcessiveInterruptException {

        final int sleepInterval = 100;
        final int msecs = duration * 1000;
        final int loopCount = msecs / sleepInterval;

        for (int i = 0; i < loopCount && worker.getState() != target; i++) {
            SleepUtilities.fullSleep(sleepInterval);
        }
    }
}
