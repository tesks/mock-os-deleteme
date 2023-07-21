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
package jpl.gds.context.impl.message;

import jpl.gds.common.config.service.ServiceConfiguration;
import jpl.gds.context.api.ISimpleContextConfiguration;
import jpl.gds.context.api.message.ContextMessageType;
import jpl.gds.context.api.message.IContextHeartbeatMessage;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.shared.metadata.MetadataKey;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.TimeUtility;
import org.springframework.context.ApplicationContext;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.text.DateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

/**
 * ContextHeartbeatMessage is a message class that indicates a process with an IContextConfiguration is still in
 * progress.
 *
 */
public class ContextHeartbeatMessage extends AbstractContextMessage implements IContextHeartbeatMessage {
    private static final String START_TIME = "startTime";
    private static final String SERVICE_CONFIGURATION = "ServiceConfiguration";
    private static final String CTX_MESSAGE = "ContextHeartbeatMessage";

    /**
     * XmlParseHandler performs the parsing of an XML version of the message into a new instance of
     * ContextHeartbeatMessage.
     */
    public static class XmlParseHandler extends ContextMessageParseHandler {

        private ContextHeartbeatMessage msg;

        /**
         * Constructor.
         *
         * @param appContext
         *            the current ApplicationContext
         */
        public XmlParseHandler(final ApplicationContext appContext) {
            super(appContext);
        }

        @Override
        public void endElement(final String uri, final String localName, final String qname) throws SAXException {

            super.endElement(uri, localName, qname);

            if (qname.equalsIgnoreCase(
                    MessageRegistry.getDefaultInternalXmlRoot(ContextMessageType.ContextHeartbeat))) {
                setInMessage(false);
            } else if (qname.equals(START_TIME)) {
                if(getDateFromBuffer() != null) {
                    msg.setStartTime(getDateFromBuffer());
                }
            } else if (msg != null && msg.getServiceConfiguration() != null) {
                msg.getServiceConfiguration().endElement(qname, getBufferText());
            }
        }

        @Override
        public void startElement(final String uri, final String localName, final String qname, final Attributes attr)
                throws SAXException {
            super.startElement(uri, localName, qname, attr);

            if (qname.equalsIgnoreCase(
                    MessageRegistry.getDefaultInternalXmlRoot(ContextMessageType.ContextHeartbeat))) {

                setInMessage(true);
                final IAccurateDateTime d = getDateFromAttr(attr, IMessage.EVENT_TIME_TAG);
                msg = new ContextHeartbeatMessage();
                if(d != null) {
                    msg.setEventTime(d);
                }
                addMessage(msg);
            } else if (qname.equalsIgnoreCase(SERVICE_CONFIGURATION)) {
                /*
                 * Add service configuration object to the message.
                 */
                msg.setServiceConfiguration(new ServiceConfiguration());
            }
        }
    }


    private Date startTime;
    private ServiceConfiguration serviceConfig;

    /**
     * Constructs a HeartbeatMessage and sets the test start time to the current time.
     */
    private ContextHeartbeatMessage() {
        super(ContextMessageType.ContextHeartbeat);
        startTime = new AccurateDateTime(System.currentTimeMillis());
        setEventTime(new AccurateDateTime(System.currentTimeMillis()));
    }


    /**
     * Constructs a HeartbeatMessage with the given test start time.
     *
     * @param config the context (session) configuration for this heartbeat
     */
    public ContextHeartbeatMessage(final ISimpleContextConfiguration config) {
        super(ContextMessageType.ContextHeartbeat);
        this.contextConfig = config;
        this.startTime = new AccurateDateTime(contextConfig.getContextId().getStartTime());
        setEventTime(new AccurateDateTime(System.currentTimeMillis()));
    }

    @Override
    public void generateStaxXml(final XMLStreamWriter writer) throws XMLStreamException {
        final DateFormat df = TimeUtility.getFormatterFromPool();
        try {
            writer.writeStartElement(CTX_MESSAGE); // <ContextHeartbeatMessage>
            writer.writeAttribute(IMessage.EVENT_TIME_TAG, getEventTimeString());

            super.generateStaxXml(writer);

            writer.writeStartElement("fromSSE"); // <fromSSE>
            writer.writeCharacters(String.valueOf(this.isFromSse()));
            writer.writeEndElement(); // </fromSSE>

            writer.writeStartElement(START_TIME); // <startTime>
            writer.writeCharacters(df.format(startTime));
            writer.writeEndElement(); // </startTime>

            if (contextConfig != null) {
                contextConfig.generateStaxXml(writer);
            }


            if (getServiceConfiguration() != null) {
                getServiceConfiguration().generateStaxXml(writer);
            }

            writer.writeEndElement(); // </ContextHeartbeatMessage>
        } finally {
            TimeUtility.releaseFormatterToPool(df);
        }
    }

    @Override
    public String getEscapedCsv() {
        return "<heartbeat></heartbeat>";
    }

    @Override
    public String getOneLineSummary() {
        final StringBuilder ret = new StringBuilder("Heartbeat for Context ");
        if (contextConfig == null) {
            ret.append("Unknown (key=Unknown)");
        } else {
            ret.append(contextConfig.getContextId().getName() + " (key ");
            if (contextConfig.getContextId().getNumber() == null) {
                ret.append("Unknown)");
            } else {
                ret.append(contextConfig.getContextId().getContextKey().toString() + ")");
            }

            ret.append(", host: " + contextConfig.getContextId().getHost());
            ret.append(", user: " + contextConfig.getContextId().getUser());

            ret.append(", Meta: [ ");

            Map<MetadataKey, Object> map = contextConfig.getMetadata().getMap();
            Iterator<MetadataKey> it = map.keySet().iterator();
            while (it.hasNext()) {
                MetadataKey key = it.next();
                ret.append(key.getName() + ": " + map.get(key).toString());
                if (it.hasNext()) {
                    ret.append(", ");
                }
            }

            ret.append(" ]");
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
     *
     * @return the start Timestamp
     */
    public Date getStartTime() {
        return startTime;
    }

    /**
     * Sets the test start time.
     *
     * @param time the start Timestamp
     */
    public void setStartTime(final IAccurateDateTime time) {
        startTime = new Date(time.getTime());
    }

    @Override
    public ServiceConfiguration getServiceConfiguration() {
        return serviceConfig;
    }

    @Override
    public void setServiceConfiguration(final ServiceConfiguration serviceConfig) {
        this.serviceConfig = serviceConfig;
    }

    @Override
    public void setTemplateContext(final Map<String, Object> map) {
        super.setTemplateContext(map);
        final DateFormat df = TimeUtility.getFormatterFromPool();
        if (getEventTime() != null) {
            map.put(IMessage.EVENT_TIME_TAG, getEventTimeString());
        } else {
            map.put(IMessage.EVENT_TIME_TAG, null);
        }
        map.put(START_TIME, df.format(startTime));
        if (contextConfig != null) {
            contextConfig.setTemplateContext(map);
            map.put("contextConfiguration", this.contextConfig.toXml());
            map.put("contextName", contextConfig.getContextId().getFullName());

        }


        if (serviceConfig != null) {
            map.put("serviceConfiguration", this.serviceConfig.toXml());
            serviceConfig.setTemplateContext(map);
        }
        TimeUtility.releaseFormatterToPool(df);
    }

    @Override
    public String toString() {
        return "MSG:" + getType() + " [start=" + TimeUtility.getFormatter().format(startTime) + "]";
    }

}