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

import jpl.gds.message.api.status.IStatusMessageFactory;
import jpl.gds.shared.log.IPublishableLogMessage;
import jpl.gds.shared.log.LogMessageType;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessageType;
import jpl.gds.shared.message.PublishableLogMessage;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;

import java.util.Date;

/**
 * A factory for common status and control messages, including log messages.
 */
public class StatusMessageFactory implements IStatusMessageFactory {

	/**
	 * Constructor.
	 */
	public StatusMessageFactory() {
		// Do nothing
	}

	@Override
    public IPublishableLogMessage createPublishableLogMessage(
            final TraceSeverity classify, final String logNote) {
		return createPublishableLogMessage(classify, logNote,
                LogMessageType.GENERAL);

	}

	@Override
    public IPublishableLogMessage createPublishableLogMessage(
            final TraceSeverity classify, final String logNote, final LogMessageType type) {
        return new PublishableLogMessage(classify, logNote, type);
	}

    @Override
    public IPublishableLogMessage createPublishableLogMessage(final TraceSeverity classify, final String logNote,
            final LogMessageType type, final IMessageType msgType) {
        return new PublishableLogMessage(msgType, classify, type, logNote);
    }


	@Override
    public IPublishableLogMessage createConnectMessage(final String source,
			final Date time) {
	    return new ConnectMessage(source, time);
	}

	@Override
    public IPublishableLogMessage createConnectMessage(final String source) {
		return createConnectMessage(source, new AccurateDateTime());
	}

	@Override
    public IPublishableLogMessage createDisconnectMessage(final String source,
			final Date time) {
	    return new DisconnectMessage(source, time);
	}

	@Override
    public IPublishableLogMessage createDisconnectMessage(final String source) {
		return createDisconnectMessage(source, new AccurateDateTime());
	}

	@Override
    public IPublishableLogMessage createEndOfDataMessage() {
	    return new EndOfDataMessage();
	}

	@Override
    public IPublishableLogMessage createPauseMessage(final Date startTime) {
	    return new PauseMessage(startTime);
	}

	@Override
    public IPublishableLogMessage createResumeMessage(final Date resumeTime) {
		return new ResumeMessage(resumeTime);
	}

	@Override
    public IPublishableLogMessage createRunningMessage(final String processInfo) {
		return new RunningMessage(processInfo);
	}

	@Override
    public IPublishableLogMessage createStopMessage(final IAccurateDateTime stopTime) {
		return new StopMessage(stopTime);
	}

	@Override
    public IPublishableLogMessage createStartOfDataMessage() {
		return new StartOfDataMessage();
	}

    @Override
    public IMessage createClientHeartbeatMessage(final String source) {
        return new ClientHeartbeatMessage(source);
    }


}
