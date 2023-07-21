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
import jpl.gds.shared.time.TimeUtility;

/**
 * ConnectMessage is a message class that indicates that a connection has been
 * established to the input source of an application.
 */
class ConnectMessage extends PublishableLogMessage {

    private final Date connectTime;

    private final String source;


    /**
     * Creates an instance of ConnectMessage with a given timestamp.
     * @param paramSource
     *            a string that identifies the data source
     * @param time
     *            connect time
     */
    ConnectMessage(final String paramSource, final Date time) {
        super(TraceSeverity.INFO, null,
                LogMessageType.CONNECT);
        this.connectTime = time;
        this.source = paramSource;
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
     * @see jpl.gds.shared.message.IMessage#getOneLineSummary()
     */
    @Override
    public String getOneLineSummary() {
        return "Connected to data source "
                + (this.source == null ? "Unknown" : this.source)
                + " at "
                + (this.connectTime == null ? "Unknown" : TimeUtility
                        .getFormatter().format(this.connectTime));
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
