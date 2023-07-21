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
package jpl.gds.tm.service.impl.frame;

import java.text.DateFormat;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.springframework.context.ApplicationContext;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import jpl.gds.shared.log.LogMessageType;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.message.BaseXmlMessageParseHandler;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessageType;
import jpl.gds.shared.message.MessageConstants;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.shared.message.PublishableLogMessage;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.TimeUtility;
import jpl.gds.station.api.IStationInfoFactory;
import jpl.gds.station.api.IStationTelemInfo;
import jpl.gds.tm.service.api.frame.IFrameEventMessage;
import jpl.gds.tm.service.api.frame.ITelemetryFrameInfo;
import jpl.gds.tm.service.api.frame.ITelemetryFrameInfoFactory;

abstract class AbstractFrameEventMessage extends PublishableLogMessage implements IFrameEventMessage {
    
    private final IMessageType messageType;
    
    
    /**
     * This is a IStationTelemInfo object to store the station header information.
     */
    private final IStationTelemInfo stationInfo;
    
    /**
     * This is an IFrameInfo object to store the transfer frame info, if any.
     */
    private final ITelemetryFrameInfo tfInfo;
    
    protected AbstractFrameEventMessage(final IMessageType messageType, final LogMessageType eventType, final IStationTelemInfo dsnI, final ITelemetryFrameInfo tfI) {
        this(messageType, TraceSeverity.INFO, eventType, dsnI, tfI);
    }
    
    protected AbstractFrameEventMessage(final IMessageType messageType, final TraceSeverity severity, final LogMessageType eventType, final IStationTelemInfo dsnI, final ITelemetryFrameInfo tfI) {
        super(messageType, severity, eventType);
        this.messageType = messageType;
        this.stationInfo = dsnI;
        this.tfInfo = tfI;
    }


    /**
     * @{inheritDoc}
     * @see jpl.gds.tm.service.api.frame.IFrameEventMessage#getFrameInfo()
     */
    @Override
    public ITelemetryFrameInfo getFrameInfo() {
        return tfInfo;
    }


    /**
     * @{inheritDoc}
     * @see jpl.gds.tm.service.api.frame.IFrameEventMessage#getStationInfo()
     */
    @Override
    public IStationTelemInfo getStationInfo() {
        return stationInfo;
    }
    
    /**
     * @{inheritDoc}
     * @see jpl.gds.tm.service.api.frame.IFrameEventMessage#getMessageType()
     */
    @Override
    public IMessageType getMessageType() {
        return this.messageType;
    }


    @Override
    public String toString() {
        return getOneLineSummary();
    }
    
    
    /* (non-Javadoc)
     * @see jpl.gds.log.external.SessionBasedLogMessage#getMessage()
     */
    @Override
    public String getMessage() {
        return getOneLineSummary();
    }

    /* (non-Javadoc)
     * @see jpl.gds.log.external.SessionBasedLogMessage#setTemplateContext(java.util.HashMap)
     */
    @Override
    public void setTemplateContext(final Map<String,Object> map) {
        super.setTemplateContext(map);

        if (this.stationInfo != null) {
            map.put("dsnInfo", "true");
            map.put("stationInfo", "true");
            this.stationInfo.setTemplateContext(map);
        }
        if (this.tfInfo != null) {
            map.put("tfInfo", "true");
            this.tfInfo.setTemplateContext(map);
        }
    }
    
    @Override
    public void generateStaxXml(final XMLStreamWriter writer) throws XMLStreamException
    {
        final DateFormat df = TimeUtility.getFormatterFromPool();
        try
        {
            writer.writeStartElement(getType() + MessageConstants.MESSAGE_ROOT_SUFFIX); 
            writer.writeAttribute(IMessage.EVENT_TIME_TAG,getEventTimeString());
                       
            super.generateXmlForContext(writer);
            
            generateInternalStaxXml(writer);
            
            if (this.stationInfo != null) {
                this.stationInfo.generateStaxXml(writer);
            }
            if (this.tfInfo != null) {
                this.tfInfo.generateStaxXml(writer);
            }
            
            
            writer.writeEndElement(); 
        }
        finally
        {
            TimeUtility.releaseFormatterToPool(df);
        }
    }
    
    protected void generateInternalStaxXml(final XMLStreamWriter writer) throws XMLStreamException {
        return;
    }
    
    public String getOneLineSummary(final String conditionTag) {

        return conditionTag
                + " condition set at time "
                + getEventTimeString()
                + "; DSSID="
                + (stationInfo == null ? "UNKNOWN" : this.stationInfo
                        .getDssId())
                + ", Frame Type="
                + (tfInfo == null ? "UNKNOWN" : this.tfInfo.getType())
                + ", VCID="
                + (tfInfo == null ? "UNKNOWN" : tfInfo.getVcid())
                + ", ERT="
                + (stationInfo == null ? "UNKNOWN" : this.stationInfo
                        .getErtString())
                + ", VCFC="
                + (tfInfo == null ? "UNKNOWN" : this.tfInfo.getSeqCount())
                + ", Bitrate="
                + (stationInfo == null ? "UNKNOWN" : String
                        .valueOf(this.stationInfo.getBitRate()));
    }

    
    /**
     * ParseHandler is the message-specific SAX parse handler for creating this Message
     * from its XML representation.
     * 
     */
    protected abstract static class FrameEventMessageParseHandler extends BaseXmlMessageParseHandler {
        protected IMessage msg;
        private IStationTelemInfo dsnInfo;
        private ITelemetryFrameInfo tfInfo;
        private boolean inDsnInfo;
        private boolean inTfInfo;
        private IAccurateDateTime time;
        protected String messageRootName;
        private final IStationInfoFactory stationInfoFactory;
        private final ITelemetryFrameInfoFactory frameInfoFactory;
        
        /**
         * Constructor.
         * 
         * @param appContext the current application context
         * @param msgConfig the actual message type
         */
        protected FrameEventMessageParseHandler(final ApplicationContext appContext, final IMessageType msgConfig) {
            this.messageRootName = MessageRegistry.getDefaultInternalXmlRoot(msgConfig);
            this.stationInfoFactory = appContext.getBean(IStationInfoFactory.class);
            this.frameInfoFactory = appContext.getBean(ITelemetryFrameInfoFactory.class);
        }
        
        /* (non-Javadoc)
         * @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
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
                this.msg = null;
                this.dsnInfo = null;
                this.inDsnInfo = false;
                this.inTfInfo = false;
                this.tfInfo = null;
                this.time = getDateFromAttr(attr, IMessage.EVENT_TIME_TAG);
            } else if (qname.equalsIgnoreCase("StationInfo")) {
                this.dsnInfo = stationInfoFactory.create();
                this.inDsnInfo = true;
            } else if (qname.equalsIgnoreCase("TransferFrameInfo")) {
                this.tfInfo = frameInfoFactory.create();
                this.tfInfo.setType(attr.getValue("name"));
                this.tfInfo.setScid(this.getIntFromAttr(attr, "scid"));
                this.inTfInfo = true;
            }
        }
    
        /* (non-Javadoc)
         * @see org.xml.sax.ContentHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
         */
        @Override
        public void endElement(final String uri,
                               final String localName,
                               final String qname)
        throws SAXException {
            super.endElement(uri, localName, qname);
            
            if (qname.equalsIgnoreCase(messageRootName)) {
                this.msg = createMessage(this.dsnInfo, this.tfInfo, this.time);
                addMessage(this.msg);
                setInMessage(false);
            } else if (qname.equalsIgnoreCase("StationInfo")) {
                this.inDsnInfo = false;
            } else if (qname.equalsIgnoreCase("TransferFrameInfo")) {
                this.inTfInfo = false;
            } else if (this.inDsnInfo) {
                this.dsnInfo.parseFromElement(qname, getBufferText());
            } else if (this.inTfInfo) {
                this.tfInfo.parseFromElement(qname, getBufferText());
            }
        }  
        
        protected abstract IMessage createMessage(IStationTelemInfo stationInfo, ITelemetryFrameInfo tfInfo,
                                                  IAccurateDateTime time);
    }

}
