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
package jpl.gds.shared.message;

import java.text.DateFormat;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.google.protobuf.GeneratedMessageV3;
import com.google.protobuf.InvalidProtocolBufferException;

import jpl.gds.serialization.messages.Proto3AbstractMessage;
import jpl.gds.serialization.metadata.Proto3MetadataMap;
import jpl.gds.shared.metadata.ISerializableMetadata;
import jpl.gds.shared.metadata.MetadataMap;
import jpl.gds.shared.metadata.context.ContextKey;
import jpl.gds.shared.metadata.context.IContextKey;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.TimeUtility;
import jpl.gds.shared.xml.stax.StaxSerializable;
import jpl.gds.shared.xml.stax.StaxStreamWriterFactory;

/**
 * This is a base class that can be extended by internal messages that implement
 * the IMessage interface. All such messages can be published using an
 * IMessageContext implementation, which uses the "type" field to identify
 * the subscribers for specific messages. The only members at this time are the message
 * event time, a flag indicating if the message contains SSE/GSE as opposed to
 * flight content.
 * 
 *
 */
public abstract class Message implements StaxSerializable, IMessage {
    
	
	/**
	 * The configuration type for this message.
	 */
	protected IMessageType configType;

	/**
	 * A general event time that can be used in any message. This is stored
	 * as milliseconds.
	 */
    protected long     eventTime;

	/**
	 * ISO string representation of the event time.
	 */
	protected String eventTimeStr;

	/**
	 * True if this message was generated in response to an SSE event, false if
	 * it was generated in response to a FSW (or some other) event.
	 */
	protected boolean fromSse = false;
	
	/**
	 * The metadata context header for this message.
	 */
	protected ISerializableMetadata contextHeader;
	
	/**
	 * The IContextKey for this message.
	 */
	protected IContextKey key = new ContextKey();

	protected boolean externallyPublishable = true;
	
	
    /**
     * This creates a new message with the given type. The type is used to
     * decide which subscribers receive the message.
     * 
     * @param paramType
     *            the type of message
     */
    protected Message(final IMessageType paramType) {
        this.configType = paramType;
    }
    
    /**
     * This creates a new message with the given type. The type is used to
     * decide which subscribers receive the message. The time supplied is 
     * an event time in milliseconds for the message.
     * 
     * @param paramType
     *            the type of message
     * @param time
     *            event time for the message, millis           
     */
    protected Message(final IMessageType paramType, final long time) {
        this.configType = paramType;
        this.eventTime = time;
    }
    
    /**
     * Constructor that creates a new message with the given type and values
     * represented in the given protobuf message
     * 
     * @param paramType
     *            the type of message
     * @param msg
     *            a protobuf message with values to be set
     * @throws InvalidProtocolBufferException
     *             an error was encountered with the provided protobuf message
     */
    protected Message(final IMessageType paramType, final Proto3AbstractMessage msg)
            throws InvalidProtocolBufferException {
    	this.configType = paramType;
    	
    	this.eventTime = msg.getEventTime();
    	this.eventTimeStr = null;

    	this.contextHeader =  new MetadataMap();

    	this.contextHeader.load(msg.getContextMap());
    	if (!this.contextHeader.isIdOnly()) {
    		this.setContextHeader(this.contextHeader);
    	}
    	this.setContextKey(this.contextHeader.getContextKey());
    	
    	this.setFromSse(msg.getFromSse());
    }

    
    @Override
	public IContextKey getContextKey() {
    	return this.key;
    }
    
    @Override
	public void setContextKey(final IContextKey key) {
    	this.key = key;
    }

	@Override
    public IAccurateDateTime getEventTime() {
        return new AccurateDateTime(this.eventTime);
	}
	
	@Override
    public abstract String getOneLineSummary();


	@Override
    public boolean isFromSse() {
		return this.fromSse;
	}

	@Override
    public void setEventTime(final IAccurateDateTime paramEventTime) {
        this.eventTime = paramEventTime.getTime();
        this.eventTimeStr = null;
		 /**
         *  Removed time formatting for performance improvements.
         * This should only happen if the message is being monitored
         */
	}


	@Override
    public void setFromSse(final boolean paramFromSse) {
		this.fromSse = paramFromSse;
	}

	@Override
	public boolean isExternallyPublishable() {
		return this.externallyPublishable;
	}

	@Override
	public void setIsExternallyPublishable(boolean value) {
		this.externallyPublishable = value;
	}

    /**
     * Returns a binary representation of this message.
     * 
     * @return a protobuf byte array
     */
    /**
     * {@inheritDoc}
     * 
     * The default implementation just throws UnsupportedOperationException.
     * 
     * @see jpl.gds.shared.message.IMessage#toBinary()
     */
	@Override
    public byte[] toBinary() {
		return build().toByteArray();
	}
	
    /**
     * Converts the context information into a protobuf message
     * 
     * @return the context data as a protobuf message
     */
	private Proto3MetadataMap toContextProto(){
	    ISerializableMetadata keyMd = null;
	    
		if (this.key != null) {
			keyMd = this.key.getMetadataHeader();
		}
	    if (this.contextHeader != null) {
	    	if (keyMd != null) {
	    		keyMd.addContextValues(contextHeader);
	    	} else {
	    		keyMd = contextHeader;
	    	}
	    }
	    if (keyMd != null) {
				return keyMd.build();
	    } else {
	    	return Proto3MetadataMap.newBuilder().build();
	    }
	  
	}
	
	@Override
	public GeneratedMessageV3 build(){
		final Proto3AbstractMessage.Builder retVal = Proto3AbstractMessage.newBuilder();
		
		retVal.setEventTime(this.eventTime);
		
		retVal.setContextMap(toContextProto());
		
		retVal.setFromSse(this.isFromSse());
		
		return retVal.build();
		
	}

	@Override
	public abstract String toString();


	@Override
	public String toXml() {
		String output = "";
		
		try {
			output = StaxStreamWriterFactory.toXml(this);
		} catch (final XMLStreamException e) {
			e.printStackTrace();
			output = "<error>Error producing XML for message</error>";
		}

		return (output);
	}
	
	@Override
    public long getRawEventTime() {
	    return this.eventTime;
	}


	@Override
	public String getEventTimeString() {

		if (this.eventTimeStr == null) {		    
			final DateFormat df = TimeUtility
					.getFormatterFromPool();
			this.eventTimeStr = df.format(this.eventTime);
			TimeUtility.releaseFormatterToPool(df);
		}
		return this.eventTimeStr;
	}
   
	@Override
	public void setTemplateContext(final Map<String, Object> map) {

		if (this.eventTime != 0) {
			map.put(EVENT_TIME_EXACT_TAG, this.eventTime);
			map.put(EVENT_TIME_TAG, this.getEventTimeString());
		}

		map.put(FROM_SSE_TAG, this.isFromSse());
		
		ISerializableMetadata keyMd = null;

		if (this.key != null) {
			keyMd = this.key.getMetadataHeader();
            this.key.setTemplateContext(map);
		}
		if (this.contextHeader != null) {
			if (keyMd != null) {
				keyMd.addContextValues(contextHeader);
			} else {
				keyMd = contextHeader;
			}
		}

		if (keyMd != null) {
		    keyMd.setTemplateContext(map);
		}
	}
	
	@Override
    public void setContextHeader(final ISerializableMetadata header) {
	    this.contextHeader = header;
	}
	
	@Override
	 public ISerializableMetadata getMetadataHeader() {
	    return this.contextHeader;
	}
	
	@Override
	public void generateStaxXml(final XMLStreamWriter writer)
	        throws XMLStreamException {

		generateXmlForContext(writer);

	}
	
	/**
	 * Generates the XML for context key and metadata members.
	 * @param writer XMLStreamWriter for output
	 * @throws XMLStreamException if there is an error generating the XML
	 */
	protected void generateXmlForContext(final XMLStreamWriter writer) throws XMLStreamException {
		ISerializableMetadata keyMd = null;

		if (this.key != null) {
			keyMd = this.key.getMetadataHeader();
		}
		if (this.contextHeader != null) {
			if (keyMd != null) {
				keyMd.addContextValues(contextHeader);
			} else {
				keyMd = contextHeader;
			}
		}

		if (keyMd != null) {
			keyMd.toContextXml(writer);
		}
	}
	
	@Override
    public boolean isType(final IMessageType inType) {
	    return (configType != null && inType != null && IMessageType.matches(inType, configType));
	}
	
	@Override
    public IMessageType getType() {
	    return this.configType;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int)eventTime;
		result = prime * result + (fromSse ? 1231 : 1237);
		result = prime * result + (configType == null ? 0 : configType.getSubscriptionTag().hashCode());
		return result;
	}

	@Override
	public boolean equals(final Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final Message other = (Message) obj;
		if (eventTime != other.eventTime)
			return false;
		if (fromSse != other.fromSse)
			return false;
		if (configType == null) {
			if (other.configType != null)
				return false;
		} else if (!configType.equals(other.configType))
			return false;
		return true;
	}

}
