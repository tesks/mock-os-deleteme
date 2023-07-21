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
import jpl.gds.shared.time.TimeUtility;

/**
 * ResumeMessage is a message class that indicates the application has resumed
 * processing of input data.
 */
class ResumeMessage extends PublishableLogMessage implements Templatable {

    private final Date resumeTime;

    /**
     * Creates an instance of ResumeMessage and assigns it the current time as
     * the resume time.
     * @param pause
     *            the start time of the last pause, as a Timestamp
     */
    ResumeMessage(final Date resume) {
        super(TraceSeverity.INFO, null, LogMessageType.RESUME_PROCESSING);
        this.resumeTime = resume;
        setEventTime(new AccurateDateTime(System.currentTimeMillis()));
    }
    
    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.message.IMessage#getOneLineSummary()
     */
    @Override
    public String getOneLineSummary() {
        return "Resumed input processing at "
                + (this.resumeTime == null ? "Unknown" : TimeUtility
                        .getFormatter().format(this.resumeTime)
                        + "; input data will now be processed");
    }
 
    @Override
    public String toString() {
        return getOneLineSummary();
    }
    
    @Override
    public String getMessage() {
        return getOneLineSummary();
    }
}
