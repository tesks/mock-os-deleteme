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
package jpl.gds.shared.log;

import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.time.IAccurateDateTime;

/**
 * This interface defines the methods that all log message classes should
 * implement, in order to be used with the tracer objects.
 * <code>ILogMessage</code> objects are used to generate notifications of
 * significant events.
 * 
 *
 */
public interface ILogMessage extends IMessage {

	/**
	 * Set the severity/priority level of the log message.
	 * 
	 * @param classify
	 *            the severity level of the message to set
	 */
	public void setSeverity(final TraceSeverity classify);

	/**
	 * Fetch the severity/priority level of the log message.
	 * 
	 * @return the severity level of the message
	 */
	public TraceSeverity getSeverity();

	/**
	 * Set the type of the log message.
	 * 
	 * @param type
	 *            log message type to set
	 */
	public void setLogType(final LogMessageType type);

	/**
	 * Fetch the type of the log message.
	 * 
	 * @return log message's type
	 */
	public LogMessageType getLogType();

	/**
	 * Set the message content.
	 * 
	 * @param msg
	 *            the message string of the log message to set
	 */
	public void setMessage(final String msg);

	/**
	 * Fetch the message content.
	 * 
	 * @return the message string of the log message
	 */
	public String getMessage();


	/**
	 * Sets the timestamp of the log message.
	 * 
	 * @param time
	 *            timestamp of the log message
	 */
	@Override
    public void setEventTime(final IAccurateDateTime time);

	/**
	 * For template engines such as Velocity: save the internal values of the
	 * log message into the provided template map.
	 * 
	 * @param map
	 *            the template map to save the log message's values in to
	 */
	@Override
    public void setTemplateContext(final Map<String, Object> map);

	/**
	 * Write the STAX XML of the log message into the provided XMLStreamWriter
	 * object.
	 * 
	 * @param writer
	 *            the output destination of the STAX XML of the log message
	 * @throws XMLStreamException
	 *             exception that is thrown when there is an error generating
	 *             the STAX XML
	 */
	public void generateStaxXml(final XMLStreamWriter writer)
			throws XMLStreamException;

}
