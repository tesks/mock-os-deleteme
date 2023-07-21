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

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import jpl.gds.shared.log.LogMessageType;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.message.BaseXmlMessageParseHandler;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.shared.message.PublishableLogMessage;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.TimeUtility;
import jpl.gds.telem.input.api.TmInputMessageType;
import jpl.gds.telem.input.api.message.ITelemetrySummaryMessage;


/**
 * Raw data summary message class.
 *
 */
public class TelemetrySummaryMessage extends PublishableLogMessage implements ITelemetrySummaryMessage {
	
	private long readCount;
	private Date lastDataReadTime;
	private boolean connected;
	private boolean flowing;
	
	// MPCS-8083 06/06/16 - Added variable to store buffer info
	private String bufferedRawInputStreamInfo = "Input buffering not enabled";


    /**
     * Constructor.
     */
	protected TelemetrySummaryMessage() {
		super(TmInputMessageType.TelemetryInputSummary, 
		        TraceSeverity.INFO, LogMessageType.RAW_INPUT_SUMMARY);
		setEventTime(new AccurateDateTime());
        // special case log these messages seperate
        // doTrace();
	}
	

    /**
     * @{inheritDoc}
     * @see jpl.gds.telem.input.api.message.ITelemetrySummaryMessage#incrementReadCount()
     */
	@Override
    public void incrementReadCount() {
		this.readCount++;
		this.lastDataReadTime = new AccurateDateTime();
	}
	

    /**
     * @{inheritDoc}
     * @see jpl.gds.telem.input.api.message.ITelemetrySummaryMessage#getReadCount()
     */
	@Override
    public long getReadCount() {
		return readCount;
	}


    /**
     * @{inheritDoc}
     * @see jpl.gds.telem.input.api.message.ITelemetrySummaryMessage#setReadCount(long)
     */
	@Override
    public void setReadCount(final long readCount) {
		this.readCount = readCount;
	}


    /**
     * @{inheritDoc}
     * @see jpl.gds.telem.input.api.message.ITelemetrySummaryMessage#getLastDataReadTime()
     */
	@Override
    public Date getLastDataReadTime() {
		return lastDataReadTime;
	}


    /**
     * @{inheritDoc}
     * @see jpl.gds.telem.input.api.message.ITelemetrySummaryMessage#setLastDataReadTime(java.util.Date)
     */
	@Override
    public void setLastDataReadTime(final Date lastDataReadTime) {
		this.lastDataReadTime = lastDataReadTime;
	}


    /**
     * @{inheritDoc}
     * @see jpl.gds.telem.input.api.message.ITelemetrySummaryMessage#isConnected()
     */
	@Override
    public boolean isConnected() {
		return connected;
	}


    /**
     * @{inheritDoc}
     * @see jpl.gds.telem.input.api.message.ITelemetrySummaryMessage#setConnected(boolean)
     */
	@Override
    public void setConnected(final boolean connected) {
		this.connected = connected;
	}


    /**
     * @{inheritDoc}
     * @see jpl.gds.telem.input.api.message.ITelemetrySummaryMessage#isFlowing()
     */
	@Override
    public boolean isFlowing() {
		return flowing;
	}


    /**
     * @{inheritDoc}
     * @see jpl.gds.telem.input.api.message.ITelemetrySummaryMessage#setFlowing(boolean)
     */
	@Override
    public void setFlowing(final boolean flowing) {
		this.flowing = flowing;
	}
	
	// MPCS-8083 06/06/16 - Added getter and setter for bufferedRawInputStreamInfo
	/**
	 * Get the input buffer info.
	 * 
	 * @return input buffer info string
	 */
	@Override
	public String getBufferedRawInputStreamInfo(){
		return this.bufferedRawInputStreamInfo;
	}
	
	/**
	 * Set the input buffer info
	 * 
	 * @param bufferedRawInputStreamInfo String containing the input buffer info
	 */
	@Override
	public void setBuffereredRawInputStreamInfo(final String bufferedRawInputStreamInfo){
		this.bufferedRawInputStreamInfo = bufferedRawInputStreamInfo;
	}


	/**
     * {@inheritDoc}
     */
	@Override
	public String getOneLineSummary() {
	    final DateFormat format = TimeUtility.getFormatterFromPool();	
	
	    String result = "";
	    
		if (this.lastDataReadTime == null) {
			if (connected) {
				result = "Data source connected";
			} else {
				result = "Data source not connected";
			}
			result += ", no data is flowing yet";
		} else {
			if (connected) {
				result = "Data source connected";
				if (flowing) {
					result += ", data flowing";
				} else {
					result += ", data not flowing";
				}
			} else {
				result = "Data source not connected";
			}
			result += ", last input data seen at " + format.format(this.lastDataReadTime) + ", total reads=" + this.readCount;
		}
		
		// MPCS-8083 06/06/16 - Add input buffer info, if available.
		if(bufferedRawInputStreamInfo != null){
			result += ", " + bufferedRawInputStreamInfo;
		}
		
		TimeUtility.releaseFormatterToPool(format);
		return result;
	}


	/**
     * {@inheritDoc}
     */
	@Override
	public String toString() {
		return getOneLineSummary();
	}
	

	/**
     * {@inheritDoc}
     */
	@Override
	public String getMessage() {
		return getOneLineSummary();
	}

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.message.PublishableLogMessage#setTemplateContext(java.util.Map)
     */
    @Override
    public void setTemplateContext(final Map<String,Object> map) {
        super.setTemplateContext(map);

        if (getEventTime() != null) {
            map.put(IMessage.EVENT_TIME_TAG, getEventTimeString());
        } else {
            map.put(IMessage.EVENT_TIME_TAG, "");
        }
        
        if ( lastDataReadTime != null ) 
		{
        	map.put( "lastReadTime", lastDataReadTime );
        }
		else
        {
        	map.put( "lastReadTime", "N/A" );
        }
        map.put("readCount", this.readCount);
        map.put("flowing", flowing);
        map.put("connected", connected);       
    }
    

	/**
     * {@inheritDoc}
     */
	@Override
	public void generateStaxXml(final XMLStreamWriter writer)
			throws XMLStreamException {
		
	    final DateFormat format = TimeUtility.getFormatterFromPool();	
		
		writer.writeStartElement(MessageRegistry.getDefaultInternalXmlRoot(getType())); // <RawDataSummaryMessage>
		writer.writeAttribute(IMessage.EVENT_TIME_TAG, getEventTimeString());
		
		if (getMetadataHeader() != null) {
		    getMetadataHeader().toContextXml(writer);
		}
		
 		writer.writeStartElement("fromSSE"); // <fromSSE>
		writer.writeCharacters(Boolean.toString(isFromSse()));
		writer.writeEndElement(); // </fromSSE>
		
		writer.writeStartElement("isConnected"); // <isConnected>
		writer.writeCharacters(String.valueOf(this.connected));
		writer.writeEndElement(); // </isConnected>
		
		writer.writeStartElement("isFlowing"); // <isFlowing>
		writer.writeCharacters(String.valueOf(this.flowing));
		writer.writeEndElement(); // </isFlowing>
		
		if (this.lastDataReadTime != null) {
			writer.writeStartElement("lastReadTime"); // <lastReadTime>
			writer.writeCharacters(format.format(this.lastDataReadTime));
			writer.writeEndElement(); // </lastReadTime>
		}
		
		writer.writeStartElement("readCount"); // <readCount>
		writer.writeCharacters(String.valueOf(this.readCount));
		writer.writeEndElement(); // </readCount>
		
		writer.writeEndElement(); // </RawDataSummaryMessage>
		
		TimeUtility.releaseFormatterToPool(format);
	}
	

    /**
     * ParseHandler is the message-specific SAX parse handler for creating this
     * Message from its XML representation.
     */
    public static class XmlParseHandler extends BaseXmlMessageParseHandler {

        private TelemetrySummaryMessage msg;

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

            if (qname.equalsIgnoreCase(MessageRegistry.getDefaultInternalXmlRoot(TmInputMessageType.TelemetryInputSummary))) {
            	setInMessage(true);
                this.msg = new TelemetrySummaryMessage();
                this.msg.setEventTime(getDateFromAttr(attr, IMessage.EVENT_TIME_TAG));
                addMessage(this.msg);
            } 
        }


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


            if (qname.equalsIgnoreCase(MessageRegistry.getDefaultInternalXmlRoot(TmInputMessageType.TelemetryInputSummary))) {
            	setInMessage(false);
            } else if (qname.equalsIgnoreCase("isConnected")) {
            	this.msg.setConnected(getBooleanFromBuffer());
            } else if (qname.equalsIgnoreCase("isFlowing")) {
            	this.msg.setFlowing(getBooleanFromBuffer());
            } else if (qname.equalsIgnoreCase("readCount")) {
            	this.msg.setReadCount(getLongFromBuffer());
            } else if (qname.equalsIgnoreCase("lastReadTime")) {
            	final DateFormat df = TimeUtility.getFormatterFromPool();
            	Date d = null;
            	try {
            		d = df.parse(getBufferText());
            	} catch (final ParseException e) {
            		throw new SAXException("Unable to parse last read time: " + getBufferText());
            	} finally {
            		TimeUtility.releaseFormatterToPool(df);
            	}
                this.msg.setLastDataReadTime(d);
            } else if (qname.equalsIgnoreCase("fromSSE")) {
            	this.msg.setFromSse(getBooleanFromBuffer());
            }
        }
    }
 
}
