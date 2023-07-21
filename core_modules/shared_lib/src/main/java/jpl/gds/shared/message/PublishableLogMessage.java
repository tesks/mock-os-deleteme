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

import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import jpl.gds.shared.log.IPublishableLogMessage;
import jpl.gds.shared.log.LogMessageInternals;
import jpl.gds.shared.log.LogMessageType;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;

/**
 * PublishableLogMessage is used to generate notifications of significant events.
 * It is considered "publishable" because it will be routed to external outputs
 * as opposed to just the console or application log.
 */
public class PublishableLogMessage extends Message implements
        IPublishableLogMessage {

    /**
     * The message information string.
     */
    protected String                  message;
    /**
     * The message severity.
     */
    protected TraceSeverity           severity;

    /**
     * The log type of the message
     */
    protected LogMessageType          logType;
    
    private final LogMessageInternals internal      = LogMessageInternals.getInstance();


    /**
     * Creates an empty trace message. DO NOT USE THIS CONSTRUCTOR DIRECTLY.
     * It is only here to fix a "code smell" for serialized messages
     * 
     */
    @SuppressWarnings("unused")
    public PublishableLogMessage() {
        this(TraceSeverity.TRACE, "");
    }

    /**
     * Creates an instance of PublishableLogMessage of type GENERAL and assigns
     * it a current event time.
     * 
     * @param classify
     *            the severity of the message
     * @param logNote
     *            the message text
     */
    public PublishableLogMessage(final TraceSeverity classify, final String logNote) {
        this(classify, logNote, LogMessageType.GENERAL);
    }

    /**
     * Creates an instance of PublishableLogMessage of type GENERAL and assigns
     * it a current event time.
     * 
     * @param classify
     *            the severity of the message
     * @param logNote
     *            the message text
     * @param type
     *            the log type of the message
     */
    public PublishableLogMessage(final TraceSeverity classify, final String logNote, final LogMessageType type) {
        super(CommonMessageType.Log, System.currentTimeMillis());
        internal.populate(this, classify, logNote, type);
    }

    /**
     * Creates an instance of PublishableLogMessage.
     * 
     * @param msgType
     *            the internal message type of the invoking subclass
     * @param classify
     *            the severity of the message
     * @param type
     *            the LogMessageType
     */
    public PublishableLogMessage(final IMessageType msgType, final TraceSeverity classify,
            final LogMessageType type) {
        super(msgType, System.currentTimeMillis());
        internal.populate(this, classify, null, type);
    }

    /**
     * Creates an instance of PublishableLogMessage.
     * 
     * @param msgType
     *            the internal message type of the invoking subclass
     * @param classify
     *            the severity of the message
     * @param type
     *            the LogMessageType
     * @param logNote
     *            The log message
     */
    public PublishableLogMessage(final IMessageType msgType, final TraceSeverity classify, final LogMessageType type,
            final String logNote) {
        super(msgType, System.currentTimeMillis());
        internal.populate(this, classify, logNote, type);
    }

    /**
     * @return the message severity
     */
    @Override
    public TraceSeverity getSeverity() {
        return severity;
    }

    /**
     * @return the message text
     */
    @Override
    public String getMessage() {
        return message;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.shared.log.ILogMessage#getLogType()
     */
    @Override
    public LogMessageType getLogType() {
        return logType;
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.shared.message.IMessage#toString()
     */
    @Override
    public String toString() {
        return internal.convertToString(this, getType());
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.shared.message.Message#setTemplateContext(java.util.Map)
     */
    @Override
    public void setTemplateContext(final Map<String, Object> map) {
        super.setTemplateContext(map);
        internal.setTemplateContextForLogMessage(this, map);
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.shared.xml.stax.StaxSerializable#generateStaxXml(javax.xml.stream.XMLStreamWriter)
     */
    @Override
    public void generateStaxXml(final XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement(MessageRegistry.getDefaultInternalXmlRoot(CommonMessageType.Log)); // <LogMessage>
        writer.writeAttribute(IMessage.EVENT_TIME_TAG, getEventTimeString());
        
        super.generateStaxXml(writer);

        writer.writeStartElement("severity"); // <severity>
        writer.writeCharacters(getSeverity() != null ? getSeverity()
                .toString() : "");
        writer.writeEndElement(); // </severity>

        writer.writeStartElement("type"); // <type>
        writer.writeCharacters(getLogType() != null ? getLogType()
                .toString() : "");
        writer.writeEndElement(); // </type>

        writer.writeStartElement("message"); // <message>
        writer.writeCData(getMessage() != null ? getMessage() : "");
        writer.writeEndElement(); // </message>

        writer.writeEndElement(); // </LogMessage>
    }

    /**
     *
     *         ParseHandler is the message-specific SAX parse handler for
     *         creating this Message from its XML representation.
     */
    public static class XmlParseHandler extends BaseXmlMessageParseHandler {
        private IPublishableLogMessage msg;
        private String                 msgText;
        private TraceSeverity          classify;
        private LogMessageType         logType;
        private IAccurateDateTime      time;

        /**
         * {@inheritDoc}
         * 
         */
        @Override
        public void startElement(final String uri, final String localName, final String qname, final Attributes attr)
                throws SAXException {
            super.startElement(uri, localName, qname, attr);

            if (qname.equals(MessageRegistry.getDefaultInternalXmlRoot(CommonMessageType.Log))) {
                setInMessage(true);
                msg = null;
                msgText = null;
                classify = null;
                logType = LogMessageType.GENERAL;
                time = getDateFromAttr(attr, IMessage.EVENT_TIME_TAG);
            }
        }

        /**
         * {@inheritDoc}
         * 
         */
        @Override
        public void endElement(final String uri, final String localName, final String qname) throws SAXException {
            super.endElement(uri, localName, qname);

            if (qname.equalsIgnoreCase(MessageRegistry.getDefaultInternalXmlRoot(CommonMessageType.Log))) {
                msg = new PublishableLogMessage(classify, msgText, logType);
                msg.setEventTime(time);
                addMessage(msg);
                setInMessage(false);
            } else if (qname.equalsIgnoreCase("severity")) {
                classify = TraceSeverity.fromStringValue(getBufferText());
            } else if (qname.equals("type")) {
                logType = LogMessageType.fromStringValue(getBufferText());
            } else if (qname.equalsIgnoreCase("message")) {
                msgText = getBufferText();
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.shared.message.IMessage#getOneLineSummary()
     */
    @Override
    public String getOneLineSummary() {
        return internal.getOneLineSummaryForLogMessage(this);
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.shared.log.ILogMessage#setSeverity(jpl.gds.shared.log.TraceSeverity)
     */
    @Override
    public void setSeverity(final TraceSeverity classify) {
        severity = classify;
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.shared.log.ILogMessage#setLogType(jpl.gds.shared.log.LogMessageType)
     */
    @Override
    public void setLogType(final LogMessageType type) {
        logType = type;

    }

    /**
     * {@inheritDoc}
     *
     * @see jpl.gds.shared.log.ILogMessage#setMessage(java.lang.String)
     */
    @Override
    public void setMessage(final String msg) {
        message = msg;
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        }
        final PublishableLogMessage fobj = (PublishableLogMessage) obj;
        return (message.equals(fobj.getMessage()) && logType.equals(fobj.getLogType()) && configType.equals(fobj.getType())
                && severity.equals(fobj.getSeverity()) && getContextKey().equals(fobj.getContextKey()));
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

}
