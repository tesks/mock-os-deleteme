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
package jpl.gds.session.message;

import java.text.DateFormat;
import java.util.Date;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.springframework.context.ApplicationContext;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import jpl.gds.common.config.service.ServiceConfiguration;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.context.api.message.IContextHeartbeatMessage;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.TimeUtility;

/**
 * SessionHeartbeatMessage is a message class that indicates an ongoing session is still in
 * progress.
 */
public class SessionHeartbeatMessage extends AbstractSessionMessage implements IContextHeartbeatMessage {

    /**
     * XmlParseHandler performs the parsing of an XML version of the message into
     * a new instance of SessionHeartbeatMessage.
     */
    public static class XmlParseHandler extends FullSessionMessageParseHandler {

    	
    	private SessionHeartbeatMessage msg;
    	
    	/**
    	 * Constructor.
    	 * 
    	 * @param appContext the current ApplicationContext
    	 */
        public XmlParseHandler(final ApplicationContext appContext) {
			super(appContext);
		}
        
        /**
         * {@inheritDoc}
         * @see org.xml.sax.ContentHandler#endElement(java.lang.String,
         *      java.lang.String, java.lang.String)
         */
        @Override
        public void endElement(final String uri, final String localName,
                final String qname) throws SAXException {

            super.endElement(uri, localName, qname);

            if (qname.equalsIgnoreCase(MessageRegistry.getDefaultInternalXmlRoot(SessionMessageType.SessionHeartbeat))) {
                setInMessage(false);
            } else if (qname.equals("startTime")) {
                msg.setStartTime(getDateFromBuffer());

            } else if (msg != null && msg.getServiceConfiguration() != null) {
                /*
                 *  Parse service configuration elements out of the message.
                 */
                msg.getServiceConfiguration().endElement(qname, getBufferText());
            }
        }

        /**
         * {@inheritDoc}
         * @see org.xml.sax.ContentHandler#startElement(java.lang.String,
         *      java.lang.String, java.lang.String, org.xml.sax.Attributes)
         */
        @Override
        public void startElement(final String uri, final String localName,
                final String qname, final Attributes attr)
                        throws SAXException {

            super.startElement(uri, localName, qname, attr);

            if (qname.equalsIgnoreCase(MessageRegistry.getDefaultInternalXmlRoot(SessionMessageType.SessionHeartbeat))) {

                setInMessage(true);
                final IAccurateDateTime d = getDateFromAttr(attr, IMessage.EVENT_TIME_TAG);
                msg = new SessionHeartbeatMessage();
                msg.setEventTime(d);
                addMessage(msg);  
                

            } else if (qname.equalsIgnoreCase("ServiceConfiguration")) {
                /*
                 * Add service configuration object to the message.
                 */
                msg.setServiceConfiguration(new ServiceConfiguration());
            }
        }
    }

    private Date startTime;
    /*
     *  Added service configuration to this message.
     */
    private ServiceConfiguration serviceConfig;

    /**
     * Constructs a HeartbeatMessage and sets the test start time to the
     * current time.
     */
    protected SessionHeartbeatMessage() {
        super(SessionMessageType.SessionHeartbeat);
        startTime = new AccurateDateTime(System.currentTimeMillis());
        setEventTime(new AccurateDateTime(System.currentTimeMillis()));
    }

    /**
     * Constructs a HeartbeatMessage with the given test start time.
     * @param config the context (session) configuration for this heartbeat
     */
    public SessionHeartbeatMessage(final IContextConfiguration config) {
        super(SessionMessageType.SessionHeartbeat);
        this.sessionConfig = config;
        this.startTime = new AccurateDateTime(sessionConfig.getContextId().getStartTime());
        setEventTime(new AccurateDateTime(System.currentTimeMillis()));
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.xml.stax.StaxSerializable#generateStaxXml(javax.xml.stream.XMLStreamWriter)
     */
    @Override
    public void generateStaxXml(final XMLStreamWriter writer)
            throws XMLStreamException {
        final DateFormat df = TimeUtility.getFormatterFromPool();
        try {
            writer.writeStartElement("SessionHeartbeatMessage"); // <HeartbeatMessage>
            writer.writeAttribute(IMessage.EVENT_TIME_TAG, getEventTimeString());
            
            super.generateStaxXml(writer);

            writer.writeStartElement("fromSSE"); //<fromSSE>
            writer.writeCharacters(String.valueOf(this.isFromSse()));
            writer.writeEndElement(); //</fromSSE>
            
            writer.writeStartElement("startTime"); // <startTime>
            writer.writeCharacters(df.format(startTime));
            writer.writeEndElement(); // </startTime>

            if (sessionConfig != null) {
                sessionConfig.generateStaxXml(writer);
            }

            /*
             * Add service configuration to the XML output.
             */
            if (getServiceConfiguration() != null) {
                getServiceConfiguration().generateStaxXml(writer);
            }

            writer.writeEndElement(); // </HeartbeatMessage>
        } finally {
            TimeUtility.releaseFormatterToPool(df);
        }
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.interfaces.EscapedCsvSupport#getEscapedCsv()
     */
    @Override
    public String getEscapedCsv() {
        return "<heartbeat></heartbeat>";
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.message.IMessage#getOneLineSummary()
     */
    @Override
    public String getOneLineSummary() {
        final StringBuilder ret = new StringBuilder("Heartbeat for session ");
        if (sessionConfig == null) {
            ret.append("Unknown (key=Unknown)");
        } else {
            ret.append(sessionConfig.getContextId().getName() + " (key ");
            if (sessionConfig.getContextId().getNumber() == null) {
                ret.append("Unknown)");
            } else {
                ret.append(sessionConfig.getContextId().getNumber() + ")");
            }
        }
        if (getEventTime() == null) {
            ret.append(" at time Unknown");
        } else {
            ret.append(" at time " + getEventTimeString());
        }
        return ret.toString();
    }

    /**
     * Gets the test start time.
     * @return the start Timestamp
     */
    public Date getStartTime() {
        return startTime;
    }

    /**
     * Sets the test start time.
     * @param time
     *            the start Timestamp
     */
    public void setStartTime(final IAccurateDateTime time) {
        startTime = new Date(time.getTime());
    }

    /**
     * Gets the service configuration object from this message.
     * 
     * @return ServiceConfiguration object; may be null
     * 
     */
    @Override
    public ServiceConfiguration getServiceConfiguration() {

        return serviceConfig;
    }

    /**
     * Sets the service configuration object in this message.
     * 
     * @param serviceConfig ServiceConfiguration object to set; may be null
     * 
     */
    @Override
    public void setServiceConfiguration(final ServiceConfiguration serviceConfig) {

        this.serviceConfig = serviceConfig;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.message.Message#setTemplateContext(java.util.Map)
     */
    @Override
    public void setTemplateContext(final Map<String,Object> map) {
        super.setTemplateContext(map);
        final DateFormat df = TimeUtility.getFormatterFromPool();
        if (getEventTime() != null) {
            map.put(IMessage.EVENT_TIME_TAG, getEventTimeString());
        } else {
            map.put(IMessage.EVENT_TIME_TAG, null);
        }
        map.put("startTime", df.format(startTime));
        if (sessionConfig != null) {
            sessionConfig.setTemplateContext(map);
            map.put("sessionConfiguration", this.sessionConfig.toXml());
            map.put("sessionName", sessionConfig.getContextId().getFullName());
        }

        /*
         * Add service configuration to the map.
         */
        if (serviceConfig != null) {
            map.put("serviceConfiguration", this.serviceConfig.toXml());
            serviceConfig.setTemplateContext(map);
        }
        TimeUtility.releaseFormatterToPool(df);
    }

    /**
     * {@inheritDoc}
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "MSG:" + getType() + " [start="
                + TimeUtility.getFormatter().format(startTime) + "]";
    }
    
}
