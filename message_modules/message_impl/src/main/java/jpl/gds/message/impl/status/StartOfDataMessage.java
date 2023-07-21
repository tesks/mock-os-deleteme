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
import jpl.gds.shared.message.CommonMessageType;
import jpl.gds.shared.message.PublishableLogMessage;
import jpl.gds.shared.time.AccurateDateTime;

/**
 * StartOfDataMessage signals the start of an input data stream.
 */
public class StartOfDataMessage extends PublishableLogMessage {

    /**
     * Creates an instance of StartOfDataMessage.
     */
    StartOfDataMessage() {
        super(CommonMessageType.StartOfData, TraceSeverity.INFO,
                LogMessageType.START_DATA);
        setEventTime(new AccurateDateTime());
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.message.PublishableLogMessage#getMessage()
     */
    @Override
    public String getMessage() {
        return getOneLineSummary();
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.message.PublishableLogMessage#getOneLineSummary()
     */
    @Override
    public String getOneLineSummary() {
        return "Telemetry data flow started at time " + getEventTimeString();
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.message.PublishableLogMessage#toString()
     */
    @Override
    public String toString() {
        return getOneLineSummary();
    }
}
