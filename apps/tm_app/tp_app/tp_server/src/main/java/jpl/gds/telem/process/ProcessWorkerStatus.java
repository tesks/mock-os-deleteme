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

package jpl.gds.telem.process;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * This class is used to report the processing state/status
 * of the TP Worker. Combines the state of number of flags
 * to report status of (WAITING, PROCESSING or DONE)
 *
 */
public class ProcessWorkerStatus implements IProcessWorkerStatus {

    private ProcessingStatus processingStatus;
    private final static long IDLE_TIME_THRESHOLD_IN_SECONDS = 3;

    /**
     * Constructor for creating a ProcessWorkerStatus object
     * @param hasBeenStarted whether or not the processor has been started
     * @param hasBacklog whether or not the processor has a backlog
     * @param hasSessionEnded whether or not the processor's session has ended
     * @param idleTimeSeconds idle time in seconds
     */
    public ProcessWorkerStatus(final boolean hasBeenStarted, final boolean hasBacklog,
                               final boolean hasSessionEnded, final long idleTimeSeconds) {
        setStatus(hasBeenStarted, hasBacklog, hasSessionEnded, idleTimeSeconds);
    }

    private void setStatus(final boolean hasBeenStarted, final boolean hasBacklog,
                           final boolean hasSessionEnded, final long idleTimeSeconds) {

        // Session has not been started yet OR
        // Session has started, has not ended, no backlog and last message
        // received was more than the idle time threshold
        // Worker is WAITING for data
        if (!hasBeenStarted ||
                (hasBeenStarted && !hasBacklog &&
                        !hasSessionEnded && idleTimeSeconds >= IDLE_TIME_THRESHOLD_IN_SECONDS)) {
            this.processingStatus = ProcessingStatus.WAITING;
        }
        // Session has been started and there is backlog OR
        // session has been started, there is no backlog, has not ended and
        // last message received was within the idle time threshold
        // Worker is PROCESSING data
        else if ((hasBeenStarted && hasBacklog) ||
                (hasBeenStarted && !hasBacklog &&
                        !hasSessionEnded && idleTimeSeconds < IDLE_TIME_THRESHOLD_IN_SECONDS)) {
            this.processingStatus = ProcessingStatus.PROCESSING;
        }
        // Session was started, has ended and there is no backlog
        // Worker is DONE
        else if (hasBeenStarted && !hasBacklog && hasSessionEnded) {
            this.processingStatus = ProcessingStatus.DONE;
        }
    }

    @Override
    public boolean isWorking() {
        return processingStatus == ProcessingStatus.PROCESSING;
    }

    @Override
    @JsonProperty("stage")
    public ProcessingStatus getStatus() {
        return this.processingStatus;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append(getClass().getSimpleName())
          .append(" is ")
          .append(processingStatus.toString());

        return sb.toString();
    }


}