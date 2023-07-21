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

import java.text.DateFormat;

import jpl.gds.shared.log.LogMessageType;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.message.PublishableLogMessage;
import jpl.gds.shared.template.Templatable;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.TimeUtility;

/**
 * StopMessage is a message class that indicates that processing of input data
 * by the application has been deliberately stopped before end of data is
 * reached.
 */
class StopMessage extends PublishableLogMessage implements Templatable {

    private final IAccurateDateTime stopTime;

    /**
     * Constructs a StopMessage with the given stop time.
     * @param stopTime
     *            a Timestamp indicating stop time
     */
    StopMessage(final IAccurateDateTime stopTime) {
        super(TraceSeverity.INFO, null, LogMessageType.STOP_PROCESSING);
        this.stopTime = stopTime;
        setEventTime(new AccurateDateTime(System.currentTimeMillis()));
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.message.IMessage#getOneLineSummary()
     */
    @Override
    public String getOneLineSummary() {
        final DateFormat df = TimeUtility.getFormatterFromPool();
        final String result =
                "Stopping telemetry input at "
                        + (this.stopTime == null ? "Unknown" : df
                                .format(this.stopTime));
        TimeUtility.releaseFormatterToPool(df);
        return result;
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
     * @see jpl.gds.shared.message.IMessage#toString()
     */
    @Override
    public String toString() {
        return getOneLineSummary();
    }

}
