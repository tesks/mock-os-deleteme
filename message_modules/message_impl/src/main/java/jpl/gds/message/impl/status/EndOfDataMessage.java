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

/**
 * EndOfDataMessage signals the end of an input data stream.
 */
class EndOfDataMessage extends PublishableLogMessage {

    /**
     * Creates an instance of EndOfDataMessage.
     */
    EndOfDataMessage() {
        super(CommonMessageType.EndOfData, TraceSeverity.INFO, LogMessageType.END_DATA);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.message.IMessage#getOneLineSummary()
     */
    @Override
    public String getOneLineSummary() {
        return "End of input telemetry data stream at time " + getEventTimeString() + "; data staging is complete;"
                + " staged data is being processed";
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
