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
package jpl.gds.message.impl.status;

import jpl.gds.shared.log.LogMessageType;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.message.PublishableLogMessage;
import jpl.gds.shared.time.AccurateDateTime;

/**
 * RunningMessage is a message class that indicates that a process is running.
 */
class RunningMessage extends PublishableLogMessage {
 
    private final String process;

    /**
     * Creates an instance of RunningMessage for a process with the given
     * name.
     * @param paramProcess
     *            a string that identifies the running process
     */
    RunningMessage(final String paramProcess) {
        super(TraceSeverity.INFO, null, LogMessageType.RUNNING_PROCESS);
        this.process = paramProcess;
        setEventTime(new AccurateDateTime());
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.message.IMessage#getOneLineSummary()
     */
    @Override
    public String getOneLineSummary() {
        return "Process " + (this.process == null ? "Unknown" : this.process)
                + " is running";
    }
    
    @Override
    public String getMessage() {
        return getOneLineSummary();
    }


    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.message.IMessage#toString()
     */
    @Override
    public String toString() {
        return getOneLineSummary();
    }
}
