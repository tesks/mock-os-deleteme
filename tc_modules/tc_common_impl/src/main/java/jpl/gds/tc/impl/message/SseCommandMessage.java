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
package jpl.gds.tc.impl.message;

import java.text.DateFormat;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.springframework.context.ApplicationContext;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import jpl.gds.shared.message.BaseXmlMessageParseHandler;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.shared.time.TimeUtility;
import jpl.gds.tc.api.command.ISseCommand;
import jpl.gds.tc.api.config.CommandProperties;
import jpl.gds.tc.api.message.CommandMessageType;
import jpl.gds.tc.api.message.ITransmittableCommandMessage;

/**
 * 
 * The representation of an SSE command as a notification sent on the
 * message bus.  This message is sent out on the bus when an SSE command
 * is successfully transmitted to the SSE.
 * 
 *
 */
public class SseCommandMessage extends AbstractCommandMessage implements ITransmittableCommandMessage
{
	
	/**
	 * Unique ID used for linking this command message to its TransmitEvent
	 * object
	 */
	private int transmitEventId;
	
	/**
	 * Whether or not uplink was successful
	 */
	private boolean isSuccessful =  true;

	/**
	 * Create an SSE command message
	 */
	public SseCommandMessage()
	{
		super(CommandMessageType.SseCommand);
	}

	/**
	 * Create an SSE command message for the given command
	 * 
	 * @param commandString The SSE command that was sent
	 */
	public SseCommandMessage(final String commandString)
	{
		super(CommandMessageType.SseCommand,commandString);
	}
	
    
	/**
	 * {@inheritDoc}
	 * @see jpl.gds.shared.xml.stax.StaxSerializable#generateStaxXml(javax.xml.stream.XMLStreamWriter)
	 */
    @Override
    public void generateStaxXml(final XMLStreamWriter writer) throws XMLStreamException
    {
    	final DateFormat df = TimeUtility.getFormatterFromPool();
    	try
    	{
    		writer.writeStartElement("SseCommandMessage"); // <SseCommandMessage>
			writer.writeAttribute(IMessage.EVENT_TIME_TAG,getEventTimeString());
			
			super.generateStaxXml(writer);
			
			writer.writeStartElement("SseCommand"); // <SseCommand>
			
			writer.writeStartElement("CommandString"); // <CommandString>
			writer.writeCharacters(getDatabaseString());
			writer.writeEndElement(); // </CommandString>
			
			writer.writeEndElement(); // </SseCommand>
			writer.writeEndElement(); // </SseCommandMessage>
		}
    	finally
    	{
    		TimeUtility.releaseFormatterToPool(df);
    	}
    }
	
    /**
	 * Gets the id that links this cmd message with a transmit event
	 * 
	 * @return hashcode object associated with TransmitEvent object
	 */
	@Override
    public int getTransmitEventId() {
		return transmitEventId;
	}

	/**
	 * Sets the id that links this cmd message with a transmit event
	 * 
	 * @param transmitEventId hashcode associated with TransmitEvent object for
	 *            this msg
	 */
	@Override
    public void setTransmitEventId(final int transmitEventId) {
		this.transmitEventId = transmitEventId;
	}
	
	/**
	 * Get whether or not uplink was successful
	 * @return a flag indicating whether or not uplink was successful
	 */
	@Override
    public boolean isSuccessful() {
		return isSuccessful;
	}

	/**
	 * Set whether or not uplink was successful
	 * @param isSuccessful whether or not uplink was successful
	 */
	@Override
    public void setSuccessful(final boolean isSuccessful) {
		this.isSuccessful = isSuccessful;
	}
	
	/**
	 * ParseHandler is the message-specific SAX parse handler for creating this Message from its XML representation.
	 * 
	 *
	 */
	public static class XmlParseHandler extends BaseXmlMessageParseHandler
	{
		/**
		 * The SSE command message being rebuilt from XML
		 */
		private SseCommandMessage msg;
		
		private final ApplicationContext appContext;

		/**
		 * Create a new parse handler
		 */
		public XmlParseHandler(final ApplicationContext appContext)
		{
			super();
			this.appContext = appContext;
			this.msg = null;
		}
		
		/**
		 * {@inheritDoc}
		 * @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
		 */
		@Override
		public void startElement(final String uri, final String localName, final String qname, final Attributes attr) throws SAXException
		{
			super.startElement(uri, localName, qname, attr);

			if(qname.equalsIgnoreCase(MessageRegistry.getDefaultInternalXmlRoot(CommandMessageType.SseCommand)))
			{
				setInMessage(true);
				this.msg = new SseCommandMessage();
				this.msg.setEventTime(getDateFromAttr(attr, IMessage.EVENT_TIME_TAG));
			}
		}

		/**
		 * {@inheritDoc}
		 * @see org.xml.sax.ContentHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
		 */
		@Override
		public void endElement(final String uri, final String localName, final String qname) throws SAXException
		{
			super.endElement(uri, localName, qname);

			if(qname.equals(MessageRegistry.getDefaultInternalXmlRoot(CommandMessageType.SseCommand)))
			{
				addMessage(this.msg);
				setInMessage(false);
			}
			else if(qname.equals("CommandString"))
			{
				String cmdString = getBufferText();
				if(ISseCommand.isSseCommand(appContext.getBean(CommandProperties.class), cmdString) == false)
				{
					cmdString = appContext.getBean(CommandProperties.class).getSseCommandPrefix() + cmdString;
				}
				this.msg.setCommandString(cmdString);
			}
		}
	}
}
