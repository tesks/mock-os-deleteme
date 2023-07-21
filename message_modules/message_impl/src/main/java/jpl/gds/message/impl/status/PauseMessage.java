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

import java.util.Date;

import jpl.gds.shared.log.LogMessageType;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.message.PublishableLogMessage;
import jpl.gds.shared.template.Templatable;
import jpl.gds.shared.time.AccurateDateTime;

/**
 * Pause Message is a message class that indicates that processing of input
 * data by the application has temporarily ceased.
 */
class PauseMessage extends PublishableLogMessage implements Templatable {

    private final Date pauseTime;

    /**
     * Constructs a PauseMessage with the given pause start time.
     * @param paramStartTime
     *            a Timestamp indicating pause start time
     */
    PauseMessage(final Date paramStartTime) {
        super(TraceSeverity.WARN, null, LogMessageType.PAUSE_PROCESSING);
        this.pauseTime = paramStartTime;
        setEventTime(new AccurateDateTime(System.currentTimeMillis()));
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.message.IMessage#getOneLineSummary()
     */
    @Override
    public String getOneLineSummary() {
        return "Paused input processing at "
                + (this.pauseTime == null ? "Unknown" : this.pauseTime
                        .toString()) + "; input data will be discarded";
    }
    
    /**
     * @{inheritDoc}
     * @see jpl.gds.shared.message.PublishableLogMessage#getMessage()
     */
    @Override
    public String getMessage() {
        return getOneLineSummary();
    }

    /**
     * {@inheritDoc}
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getOneLineSummary();
    }
}