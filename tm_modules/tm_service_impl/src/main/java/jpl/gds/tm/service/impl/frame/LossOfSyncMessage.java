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
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.station.api.IStationTelemInfo;
import jpl.gds.tm.service.api.TmServiceMessageType;
import jpl.gds.tm.service.api.frame.IFrameEventMessage;
import jpl.gds.tm.service.api.frame.ILossOfSyncMessage;
import jpl.gds.tm.service.api.frame.ITelemetryFrameInfo;

/**
 * LossOfSyncMessage is the message sent when an in-sync state of the frame
 * synchronization stage of telemetry processing loses sync.
 * 
 */
public class LossOfSyncMessage extends AbstractFrameEventMessage implements ILossOfSyncMessage {

    /**
     * This is a string that stores the reason it loses sync.
     */
    protected String reason;
    /**
     * This is the last ERT time.
     */
    protected IAccurateDateTime lastErt;
    
    /**
     * Creates an instance of LossOfSyncMessage.
     *
     * @param dsnI the IStationTelemInfo object associated with the frame event
     * @param frameInfo the ITelemetryFrameInfo for the last in-sync frame
     * @param reason text describing the reason for this event, if known
     * @param lastErt the last Earth Received Time
     * @throws IllegalArgumentException If an illegal argument is passed.
     */
    protected LossOfSyncMessage(final IStationTelemInfo dsnI, final ITelemetryFrameInfo frameInfo, final String reason, final IAccurateDateTime lastErt) {
        super(TmServiceMessageType.LossOfSync, TraceSeverity.WARNING, LogMessageType.LOSS_OF_SYNC, dsnI, frameInfo);
        this.reason = reason;
        this.lastErt = lastErt;
    }

    
    /**
     * This gets the last ERT time.
     * @return The last ERT time.
     */
    @Override
    public IAccurateDateTime getLastErt() {
		return lastErt;
	}
    /**
     * This gets the text describing the reason for this event
     * @return the text describing the reason for this event
     */
    @Override
    public String getReason() { 
        return this.reason; 
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.shared.message.PublishableLogMessage#getOneLineSummary()
     */
    @Override
    public String getOneLineSummary() {
        return super.getOneLineSummary("LOSS OF SYNC") +
        ", Last ERT=" + (this.lastErt == null ? "None" : this.lastErt.getFormattedErt(true)) +
        ", Reason=" + this.reason;
    }
    
    /**
     * @{inheritDoc}
     * @see jpl.gds.tm.service.impl.frame.AbstractFrameEventMessage#setTemplateContext(java.util.Map)
     */
    @Override
    public void setTemplateContext(final Map<String,Object> map) {
        super.setTemplateContext(map);

        if (this.reason != null) {
            map.put("reason", this.reason);
        }
        if (this.lastErt != null) {
        	map.put("lastFrameErt", this.lastErt.getFormattedErt(true));
        } else {
        	map.put("lastFrameErt", new AccurateDateTime().getFormattedErt(true));
        }
    }
    
    
    /**
     * @{inheritDoc}
     * @see jpl.gds.tm.service.impl.frame.AbstractFrameEventMessage#generateInternalStaxXml(javax.xml.stream.XMLStreamWriter)
     */
    @Override
    public void generateInternalStaxXml(final XMLStreamWriter writer) throws XMLStreamException
    {
        writer.writeStartElement("lastFrameErt"); // <lastFrameErt>
        writer.writeCharacters(this.lastErt != null ? this.lastErt.getFormattedErt(true) : new AccurateDateTime().getFormattedErt(true));
        writer.writeEndElement(); // </lastFrameErt>

        writer.writeStartElement("reason"); // <reason>
        writer.writeCharacters(this.reason != null ? this.reason : "");
        writer.writeEndElement(); // </reason>
    }
    
    /**
     * XmlParseHandler is the message-specific SAX parse handler for creating this Message
     * from its XML representation.
     * 
     */
    public static class XmlParseHandler extends FrameEventMessageParseHandler {
       
        private String reason;
        private IAccurateDateTime lastErt;
        
        
        /**
         * Constructor.
         * @param appContext the current application context
         */
        public XmlParseHandler(final ApplicationContext appContext) {
            super(appContext, TmServiceMessageType.LossOfSync);
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
            	setInMessage(true);
                this.reason = null;
                lastErt = null;
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
                ((LossOfSyncMessage)this.msg).lastErt = this.lastErt;
                ((LossOfSyncMessage)this.msg).reason = this.reason;
            	setInMessage(false);
            } else if (qname.equalsIgnoreCase("reason")) {
                this.reason = getBufferText();
            } else if (qname.equalsIgnoreCase("lastFrameErt")) {
                try {
                    if (!getBufferText().equals("")) {
                        lastErt = new AccurateDateTime(getBufferText());
                    } 
                } catch (final java.text.ParseException e) {
                    e.printStackTrace();
                }
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
            final ILossOfSyncMessage msg = new LossOfSyncMessage(stationInfo, tfInfo, null, null);
            msg.setEventTime(time);
            return msg;
        }
    }

}
