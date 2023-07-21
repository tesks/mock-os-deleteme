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
package jpl.gds.message.api.status;

import java.util.Date;

import jpl.gds.shared.log.IPublishableLogMessage;
import jpl.gds.shared.log.LogMessageType;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessageType;
import jpl.gds.shared.time.IAccurateDateTime;

/**
 * An interface to be implemented by factories that create status and control
 * messages.
 */
public interface IStatusMessageFactory {

    /**
     * Creates a log message.
     * 
     * @param classify
     *            the severity of the message
     * @param logNote
     *            the text of the message
     * @return IPublishableLogMessage instance
     */
    public IPublishableLogMessage createPublishableLogMessage(
            TraceSeverity classify, String logNote);

    /**
     * Creates a log message.
     * 
     * @param classify
     *            the severity of the message
     * @param logNote
     *            the text of the message
     * @param type
     *            the type of the log message
     * @return IPublishableLogMessage instance
     */
    public IPublishableLogMessage createPublishableLogMessage(
            TraceSeverity classify, String logNote, LogMessageType type);

    /**
     * Creates a log message.
     * 
     * @param classify
     *            the severity of the message
     * @param logNote
     *            the text of the message
     * @param type
     *            the type of the log message
     * @param msgType
     *            The IMessageType type associated with this log
     * @return IPublishableLogMessage instance
     */
    public IPublishableLogMessage createPublishableLogMessage(TraceSeverity classify, String logNote,
            LogMessageType type, IMessageType msgType);

    /**
     * Creates a network or file source connection message.
     * 
     * @param source
     *            the source connected to
     * @param time
     *            the time the connection was made
     * @return IPublishableLogMessage instance
     */
    public IPublishableLogMessage createConnectMessage(String source, Date time);

    /**
     * Creates a network or file source connection message with the connect time
     * set to the current time.
     * 
     * @param source
     *            the source connected to
     * @return IPublishableLogMessage instance
     */
    public IPublishableLogMessage createConnectMessage(String source);

    /**
     * Creates a network or file source disconnection message.
     * 
     * @param source
     *            the source disconnected from
     * @param time
     *            the time the disconnection occurred
     * @return IPublishableLogMessage instance
     */
    public IPublishableLogMessage createDisconnectMessage(String source,
            Date time);

    /**
     * Creates a network or file source disconnection message with the
     * disconnect time set to the current time.
     * 
     * @param source
     *            the source disconnected from
     * @return IPublishableLogMessage instance
     */
    public IPublishableLogMessage createDisconnectMessage(String source);

    /**
     * Creates an end of telemetry data message.
     * 
     * @return IPublishableLogMessage instance
     */
    public IPublishableLogMessage createEndOfDataMessage();

    /**
     * Creates a processing pause message.
     * 
     * @param startTime
     *            the time the pause started
     * 
     * @return IPublishableLogMessage instance
     */
    public IPublishableLogMessage createPauseMessage(Date startTime);

    /**
     * Creates a processing resume message.
     * 
     * @param resumeTime
     *            the time of resumption
     * 
     * @return IPublishableLogMessage instance
     */
    public IPublishableLogMessage createResumeMessage(Date resumeTime);

    /**
     * Creates a process running message.
     * 
     * @param processInfo
     *            any string identifying the process
     * 
     * @return IPublishableLogMessage instance
     */
    public IPublishableLogMessage createRunningMessage(String processInfo);

    /**
     * Creates a processing stopped message.
     * 
     * @param stopTime
     *            processing stopped
     * 
     * @return IPublishableLogMessage instance
     */
    public IPublishableLogMessage createStopMessage(IAccurateDateTime stopTime);

    /**
     * Creates a start of telemetry data message.
     * 
     * @return IPublishableLogMessage instance
     */
    public IPublishableLogMessage createStartOfDataMessage();
    
    /**
     * Creates a message client heartbeat message.
     * 
     * @param source a string identifying the client that is producing the heartbeat
     * 
     * @return new message instance
     */
    public IMessage createClientHeartbeatMessage(String source);
}