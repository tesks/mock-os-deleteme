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

import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import jpl.gds.shared.message.BaseXmlMessageParseHandler;
import jpl.gds.shared.message.CommonMessageType;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.Message;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.shared.time.IAccurateDateTime;

/**
 * ClientHeartbeatMessage is a message class this is published by clients both
 * to indicate that they are still alive and to detect when something has gone
 * wrong with the message service connection.
 */
public class ClientHeartbeatMessage extends Message {

	/**
	 * The unique identifier for the current client.
	 */
	private String source;

	/**
	 * Constructs a ClientHeartbeatMessage and sets the event time to the
	 * current time.
	 */
	public ClientHeartbeatMessage() {

		super(CommonMessageType.ClientHeartbeat, System.currentTimeMillis());
	}

	/**
	 * Constructs a ClientHeartbeatMessage and sets the event time to the
	 * current time and the client source name to the given string.
	 * 
	 * @param clientSource
	 *            a string uniquely identifying the client issuing
	 * 
	 */
	public ClientHeartbeatMessage(final String clientSource) {

		super(CommonMessageType.ClientHeartbeat, System.currentTimeMillis());
		if (clientSource == null) {
			throw new IllegalArgumentException(
					"clientSource argument cannot be null");
		}
		this.source = clientSource;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.shared.message.Message#generateStaxXml(javax.xml.stream.XMLStreamWriter)
	 */
	@Override
	public void generateStaxXml(final XMLStreamWriter writer)
			throws XMLStreamException {

		writer.writeStartElement(MessageRegistry.getDefaultInternalXmlRoot(getType())); // <ClientHeartbeatMessage>
		writer.writeAttribute(IMessage.EVENT_TIME_TAG, getEventTimeString());

		writer.writeStartElement("source"); // <source>
		writer.writeCharacters(this.source == null ? "Unknown" : this.source);
		writer.writeEndElement(); // </source>

		writer.writeEndElement(); // </ClientHeartbeatMessage>
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.shared.message.IMessage#getOneLineSummary()
	 */
	@Override
	public String getOneLineSummary() {

		return "Heartbeat from client source " + this.source == null ? "Unknown"
				: this.source;
	}

	/**
	 * Gets the identification of the client source.
	 * 
	 * @return the client source ID string
	 */
	public String getSource() {

		return this.source;
	}

	/**
	 * Sets the identification of the client source.
	 * 
	 * @param paramSource
	 *            The source to set.
	 */
	public void setSource(final String paramSource) {

		if (paramSource == null) {
			throw new IllegalArgumentException(
					"paramSource argument cannot be null");
		}
		this.source = paramSource;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.shared.message.Message#setTemplateContext(java.util.Map)
	 */
	@Override
	public void setTemplateContext(final Map<String, Object> map) {

		super.setTemplateContext(map);

		if (this.source != null) {
			map.put("source", this.source);
		} else {
			map.put("source", "Unknown");
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {

		return "MSG:" + getType() + " [client heartbeat="
				+ (this.source == null ? "Unknown" : this.source) + "]";
	}

	/**
	 * ParseHandler performs the parsing of an XML version of the message into a
	 * new instance of ClientHeartbeatMessage.
	 */
	public static class XmlParseHandler extends BaseXmlMessageParseHandler {
		private ClientHeartbeatMessage msg;

		/**
		 * {@inheritDoc}
		 * 
		 * @see org.xml.sax.ContentHandler#endElement(java.lang.String,
		 *      java.lang.String, java.lang.String)
		 */
		@Override
		public void endElement(final String uri, final String localName,
				final String qname) throws SAXException {

			super.endElement(uri, localName, qname);

			if (qname.equalsIgnoreCase(MessageRegistry.getDefaultInternalXmlRoot(CommonMessageType.ClientHeartbeat))) {
				setInMessage(false);
			} else if (qname.equalsIgnoreCase("source")) {
				this.msg.setSource(getBufferText());
			}
		}

		/**
		 * {@inheritDoc}
		 * 
		 * @see org.xml.sax.ContentHandler#startElement(java.lang.String,
		 *      java.lang.String, java.lang.String, org.xml.sax.Attributes)
		 */
		@Override
		public void startElement(final String uri, final String localName,
				final String qname, final Attributes attr) throws SAXException {

			super.startElement(uri, localName, qname, attr);

			if (qname.equalsIgnoreCase(MessageRegistry.getDefaultInternalXmlRoot(CommonMessageType.ClientHeartbeat))) {
				setInMessage(true);
				this.msg = new ClientHeartbeatMessage();
                final IAccurateDateTime d = this.getDateFromAttr(attr,
						Message.EVENT_TIME_TAG);
				this.msg.setEventTime(d);
				addMessage(this.msg);
			}
		}
	}

}
