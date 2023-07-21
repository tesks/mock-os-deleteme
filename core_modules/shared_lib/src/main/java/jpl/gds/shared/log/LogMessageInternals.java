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
import jpl.gds.shared.message.IMessageType;

/**
 * Purpose of this class is to provide a central implementation of common code
 * that <code>ILogMessage</code> classes need to implement. Since the
 * <code>ILogMessage</code> implementation classes already extend respective
 * abstract classes, and because Java does not allow multiple inheritance,
 * common code of <code>ILogMessage</code> that are to be shared are implemented
 * here and the classes should simply call the method provided so that we don't
 * introduce code duplication.
 * 
 */
public final class LogMessageInternals {

	private static LogMessageInternals instance = null;

	/**
	 * Gets the singleton object of the class.
	 * 
	 * @return the singleton object
	 */
	public static synchronized LogMessageInternals getInstance() {

		if (instance == null) {
			instance = new LogMessageInternals();
		}

		return instance;
	}

	private LogMessageInternals() {
	}

	/**
	 * Populate values in the provided <code>ILogMessage</code> object using the
	 * provided parameters. Default to type GENERAL and assign it the current
	 * event time.
	 * 
	 * @param lm
	 *            the log message object to populate values in
	 * @param classify
	 *            the severity of the message
	 * @param logNote
	 *            the message text
	 */
	public void populate(final ILogMessage lm, final TraceSeverity classify,
			final String logNote) {
        populate(lm, classify, logNote, LogMessageType.GENERAL);
	}

	/**
	 * Populate values in the provided <code>ILogMessage</code> object. Using
	 * the provided parameters.
	 * 
	 * @param lm
	 *            the log message object to populate values in
	 * @param classify
	 *            the severity of the message
	 * @param logNote
	 *            the message text
	 * @param time
	 *            timestamp value of the message
	 * @param type
	 *            log message's type
	 */
	public void populate(final ILogMessage lm, final TraceSeverity classify,
                         final String logNote, final LogMessageType type) {
		lm.setSeverity(classify);
		lm.setLogType(type);
		if (logNote != null) {
			lm.setMessage(logNote.replaceAll("[\n\r]", " "));
		} else {
			lm.setMessage(null);
		}

	}

	/**
	 * Internal implementation for toString() methods of the
	 * <code>ILogMessage</code> classes. Uses the values in the provided log
	 * message object to generate a string representation.
	 * 
	 * @param lm
	 *            the log message object to convert to string
	 * @param type
	 *            type of the log message
	 * @return string representation of <code>lm</code>
	 */
	public String convertToString(final ILogMessage lm, final IMessageType type) {
		return "MSG:" + type.getSubscriptionTag() + " time=" + lm.getEventTimeString() + " class="
				+ lm.getSeverity() + " message=" + lm.getMessage();
	}

	/**
	 * Internal implementation for setTemplateContext(...) methods of the
	 * <code>ILogMessage</code> classes. Uses the values in the provided log
	 * message object to populate the key-value mappings in <code>map</code> for
	 * template engine's use (such as Velocity).
	 * 
	 * @param lm
	 *            the log message object to set the template context for
	 * @param map
	 *            the destination map object where template context should be
	 *            saved
	 */
	public void setTemplateContextForLogMessage(final ILogMessage lm,
			final Map<String, Object> map) {

		if (lm.getEventTime() != null) {
			map.put(IMessage.EVENT_TIME_TAG, lm.getEventTimeString());
		} else {
			map.put(IMessage.EVENT_TIME_TAG, "");
		}
		if (lm.getSeverity() != null) {
			map.put("severity", lm.getSeverity());
		}
		if (lm.getMessage() != null) {
			map.put("message", lm.getMessage());
		}
		if (lm.getLogType() != null) {
			map.put("type", lm.getLogType().getValueAsString());
		}

	}

	/**
	 * Internal implementation for generateStaxXml(...) methods of the
	 * <code>ILogMessage</code> classes. Uses the values in the provided log
	 * message object to generate a STAX XML string, and writes it to the
	 * provided output writer.
	 * 
	 * @param lm
	 *            the log message object to convert to STAX XML string
	 * @param writer
	 *            output destination for the STAX XML string
	 * @throws XMLStreamException
	 *             exception that gets thrown if there's an error while
	 *             converting to STAX XML
	 */
	public void generateStaxXmlForLogMessage(final ILogMessage lm,
			final XMLStreamWriter writer) throws XMLStreamException {
		writer.writeStartElement("LogMessage"); // <LogMessage>
		writer.writeAttribute(IMessage.EVENT_TIME_TAG, lm.getEventTimeString());

		writer.writeStartElement("severity"); // <severity>
		writer.writeCharacters(lm.getSeverity() != null ? lm.getSeverity()
				.toString() : "");
		writer.writeEndElement(); // </severity>

		writer.writeStartElement("type"); // <type>
		writer.writeCharacters(lm.getLogType() != null ? lm.getLogType()
				.toString() : "");
		writer.writeEndElement(); // </type>

		writer.writeStartElement("message"); // <message>
		writer.writeCData(lm.getMessage() != null ? lm.getMessage() : "");
		writer.writeEndElement(); // </message>

		writer.writeEndElement(); // </LogMessage>
	}

	/**
	 * Returns the one-line-summary version of the log message.
	 * 
	 * @param lm
	 *            the log message object to convert to one-line-summary string
	 * @return one-line-summary of the log message
	 */
	public String getOneLineSummaryForLogMessage(final ILogMessage lm) {
		return (lm.getSeverity() == null ? "Unknown" : lm.getSeverity())
				+ ": (" + lm.getLogType().getValueAsString() + ") "
				+ (lm.getMessage() == null ? "No Message" : lm.getMessage());
	}
}
