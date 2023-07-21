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
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.springframework.context.ApplicationContext;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import jpl.gds.common.config.service.ServiceConfiguration;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.context.api.message.IStartOfContextMessage;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.TimeUtility;

/**
 * StartOfSessionMessage is a message class that indicates the start of a session
 * run.
 */
public class StartOfSessionMessage extends AbstractSessionMessage implements IStartOfContextMessage {

    /**
     * ParseHandler performs the parsing of an XML version of the message into
     * a new instance of StartOfSessionMessage.
     */
    public static class XmlParseHandler extends FullSessionMessageParseHandler {
        private StartOfSessionMessage msg;
       	
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
         * @see jpl.gds.session.message.AbstractSessionMessage.FullSessionMessageParseHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
         */
        @Override
        public void endElement(final String uri, final String localName,
                final String qname) throws SAXException {

            super.endElement(uri, localName, qname);

            if (qname.equalsIgnoreCase("StartOfSessionMessage")) {
            	setInMessage(false);
            } else if (qname.equals("startTime")) {
                msg.setStartTime(getDateFromBuffer());

            } else if (msg != null && msg.getServiceConfiguration() != null) {
                /*
                 * Parse service configuration elements out of the message.
                 */
                msg.getServiceConfiguration().endElement(qname, getBufferText());
            }
        }

        /**
         * {@inheritDoc}
         * @see jpl.gds.session.message.AbstractSessionMessage.FullSessionMessageParseHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
         */
        @Override
        public void startElement(final String uri, final String localName,
                final String qname, final Attributes attr)
                        throws SAXException {

            super.startElement(uri, localName, qname, attr);

            if (qname.equalsIgnoreCase("StartOfSessionMessage")) {
            	setInMessage(true);
                final IAccurateDateTime d = getDateFromAttr(attr, IMessage.EVENT_TIME_TAG);
                msg = new StartOfSessionMessage();
                msg.setEventTime(d);
                addMessage(msg);

            } else if (qname.equalsIgnoreCase("ServiceConfiguration")) {
                /*
                 *  Add service configuration object to the message.
                 */
                msg.setServiceConfiguration(new ServiceConfiguration());
            }
        }
    }

    private IAccurateDateTime    startTime;
    /*
     * Added service configuration to this message.
     */
    private ServiceConfiguration serviceConfig;

    /**
     * Constructs a StartOfSessionMessage.
     * @param context the context configuration (session) for this message
     */
    public StartOfSessionMessage(final IContextConfiguration context) {
        this();
        this.sessionConfig = context;
        startTime = this.sessionConfig.getContextId().getStartTime();
    }
    
    /**
     * Protected no-arg constructor. Should be used only by the message parser.
     */
    protected StartOfSessionMessage() {
        super(SessionMessageType.StartOfSession);
        startTime = new AccurateDateTime(System.currentTimeMillis());
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
            writer.writeStartElement("StartOfSessionMessage"); // <StartOfSessionMessage>
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

            writer.writeEndElement(); // </StartOfSessionMessage>
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
        return "<start></start>";
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.message.IMessage#getOneLineSummary()
     */
    @Override
    public String getOneLineSummary() {
        final DateFormat format = TimeUtility.getFormatterFromPool();
        final StringBuilder ret = new StringBuilder("Started session ");
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
        if (startTime == null) {
            ret.append(" at time Unknown");
        } else {
            ret.append(" at time " + format.format(startTime));
        }
        TimeUtility.releaseFormatterToPool(format);

        return ret.toString();
    }

    /**
     * Gets the test start time.
     * @return the start Timestamp
     */
    public IAccurateDateTime getStartTime() {
        return startTime;
    }

    /**
     * Sets the test start time.
     * @param time
     *            the start Timestamp
     */
    public void setStartTime(final IAccurateDateTime time) {
        startTime = new AccurateDateTime(time.getTime());
    }

    /**
     * Gets the service configuration object from this message.
     * 
     * @return ServiceConfiguration object; may be null
     * 
     */
    public ServiceConfiguration getServiceConfiguration() {

        return serviceConfig;
    }

    /**
     * Sets the service configuration object in this message.
     * 
     * @param serviceConfig ServiceConfiguration object to set; may be null
     * 
     */
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
            map.put("sessionConfiguration", sessionConfig.toXml());
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
     * @see jpl.gds.shared.message.IMessage#toString()
     */
    @Override
    public String toString() {
        return "MSG:" + getType() + " [start="
                + TimeUtility.getFormatter().format(startTime) + "]";
    }
    
}
