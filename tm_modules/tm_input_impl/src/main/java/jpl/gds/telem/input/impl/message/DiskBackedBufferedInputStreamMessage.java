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
package jpl.gds.telem.input.impl.message;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.Message;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.telem.input.api.InternalTmInputMessageType;

/**
 * The DiskBackedBufferedInputStreamMessage is utilized for passing messages
 * from a LoggingDiskBackedBufferedInputStream instance to any subscribers. The
 * internal message type is left open in order to allow multiple message
 * DiskBackedBufferedInputStreams to publish messages to different handlers.
 * 
 *
 */
public class DiskBackedBufferedInputStreamMessage extends Message implements IMessage{

	private String message;
	
	/**
	 * Default constructor. This message will be a DiskBackedBufferedInputStream message type
	 */
	public DiskBackedBufferedInputStreamMessage(){
		super(InternalTmInputMessageType.LoggingDiskBackedBufferedInputStream);
	}
	
	/**
	 * Constructor that sets the contained message.
	 * 
	 * @param message
	 *            A string to be published as a message
	 */
	public DiskBackedBufferedInputStreamMessage(final String message){
		super(InternalTmInputMessageType.LoggingDiskBackedBufferedInputStream);
		this.message = message;
	}

	@Override
	public void generateStaxXml(final XMLStreamWriter writer) throws XMLStreamException {
	    writer.writeEmptyElement(MessageRegistry.getDefaultInternalXmlRoot(getType()));
		
	}

	@Override
	public String getOneLineSummary() {
		return getMessage();
	}

	@Override
	public String toString() {
		return getOneLineSummary();
	}
	
	/**
	 * Retrieve the message stored in this object
	 * @return the String message contained
	 */
	public String getMessage(){
		return this.message;
	}
	
	/**
	 * Stores the supplied String in the object as the message
	 * 
	 * @param message
	 *            A string to be published as a message
	 */
	public void setMessage(final String message){
		this.message = message;
	}
	
}