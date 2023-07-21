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
/* */
package jpl.gds.tm.service.impl.frame;

import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.springframework.context.ApplicationContext;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import jpl.gds.shared.log.LogMessageType;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.station.api.IStationTelemInfo;
import jpl.gds.tm.service.api.TmServiceMessageType;
import jpl.gds.tm.service.api.frame.IFrameEventMessage;
import jpl.gds.tm.service.api.frame.IOutOfSyncDataMessage;
import jpl.gds.tm.service.api.frame.ITelemetryFrameInfo;

/**
 * OutOfSyncBytesMessage is the message sent when bytes not in a
 * frame are encountered in the input stream by the frame sychronizer.
 * 
 */
public class OutOfSyncDataMessage extends AbstractFrameEventMessage implements IOutOfSyncDataMessage {	
    
    /**
     * This is a long to store the out of sync bytes length
     */
    private long outOfSyncBytesLength;
    /**
     * This is a byte buffer to store the out of sync data.
     */
    private final byte[] outOfSyncData;
    
    /**
     * Creates an instance of OutOfSyncDatasMessage.
     *
     * @param dsnI the IStationTelemInfo object associated with this message
     * @param length the length in bytes of the out-of-sync data
     * @param data the actual out-of-sync data bytes
     */
    protected OutOfSyncDataMessage(final IStationTelemInfo dsnI, final long length, final byte[] data) {
        super(TmServiceMessageType.OutOfSyncData,
              TraceSeverity.WARNING,
              LogMessageType.OUT_OF_SYNC_DATA,
              dsnI, null);
        this.outOfSyncBytesLength = length;
        this.outOfSyncData = data;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.shared.message.PublishableLogMessage#getOneLineSummary()
     */
    @Override
    public String getOneLineSummary() {
        return super.getOneLineSummary("OUT-OF-SYNC DATA") +
                ", OutOfSyncBytesLength=" + this.outOfSyncBytesLength;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.tm.service.impl.frame.AbstractFrameEventMessage#setTemplateContext(java.util.Map)
     */
    @Override
    public void setTemplateContext(final Map<String,Object> map) {
        super.setTemplateContext(map);

        map.put("outOfSyncBytesLength", this.outOfSyncBytesLength);
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.tm.service.impl.frame.AbstractFrameEventMessage#generateInternalStaxXml(javax.xml.stream.XMLStreamWriter)
     */
    @Override
    public void generateInternalStaxXml(final XMLStreamWriter writer) throws XMLStreamException
    {
    
        writer.writeStartElement("outOfSyncBytesLength"); // <outOfSyncBytesLength>
        writer.writeCharacters(Long.toString(this.outOfSyncBytesLength));
        writer.writeEndElement(); // </outOfSyncBytesLength>
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.tm.service.api.frame.IOutOfSyncDataMessage#getOutOfSyncBytesLength()
     */
    @Override
    public long getOutOfSyncBytesLength() {
        return outOfSyncBytesLength;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.tm.service.api.frame.IOutOfSyncDataMessage#getOutOfSyncData()
     */
    @Override
    public byte[] getOutOfSyncData() {
        return outOfSyncData;
    }

    /**
     *
     * XmlParseHandler is the message-specific SAX parse handler for creating this Message
     * from its XML representation.
     * 
     */
    public static class XmlParseHandler extends FrameEventMessageParseHandler {
       
        private long outOfSyncBytesLength;
        
        
        /**
         * Constructor.
         * @param appContext the current application context
         */
        public XmlParseHandler(final ApplicationContext appContext) {
            super(appContext, TmServiceMessageType.OutOfSyncData);
        }
        
        /**
         * @{inheritDoc}
         * @see jpl.gds.tm.service.impl.frame.AbstractFrameEventMessage.FrameEventMessageParseHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
         */
        @Override
        public void startElement(final String uri,
                                 final String localName,
                                 final String qname,
                                 final Attributes attr)
        throws SAXException {
            super.startElement(uri, localName, qname, attr);
            
            if (qname.equalsIgnoreCase(messageRootName)) {
                outOfSyncBytesLength = 0;
            	setInMessage(true);
            } 
        }
    
        /**
         * @{inheritDoc}
         * @see jpl.gds.tm.service.impl.frame.AbstractFrameEventMessage.FrameEventMessageParseHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
         */
        @Override
        public void endElement(final String uri,
                               final String localName,
                               final String qname)
        throws SAXException {
            super.endElement(uri, localName, qname);
            
            if (qname.equalsIgnoreCase(messageRootName)) {
                ((OutOfSyncDataMessage)this.msg).outOfSyncBytesLength = this.outOfSyncBytesLength;
            	setInMessage(false);
            
            } else if (qname.equalsIgnoreCase("outOfSyncBytesLength")) {
                this.outOfSyncBytesLength = Long.parseLong(getBufferText());
            }
        }

        /**
         * @{inheritDoc}
         * @see jpl.gds.tm.service.impl.frame.AbstractFrameEventMessage.FrameEventMessageParseHandler#createMessage(jpl.gds.station.api.IStationTelemInfo, jpl.gds.tm.service.api.frame.ITelemetryFrameInfo, java.util.Date)
         */
        @Override
        protected IFrameEventMessage createMessage(
                                                   final IStationTelemInfo stationInfo,
                                                   final ITelemetryFrameInfo tfInfo, final IAccurateDateTime time) {
            final IOutOfSyncDataMessage msg = new OutOfSyncDataMessage(stationInfo, 0, null);
            msg.setEventTime(time);
            return msg;
        }
    }

}