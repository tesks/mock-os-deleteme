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

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.common.service.telem.ITelemetrySummary;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.context.api.message.IEndOfContextMessage;
import jpl.gds.session.config.SessionIdentification;
import jpl.gds.session.config.SessionIdentificationParser;
import jpl.gds.shared.message.BaseXmlMessageParseHandler;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.Message;
import jpl.gds.shared.message.MessageConstants;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.TimeUtility;

/**
 * EndOfSessionMessage is a message class that indicates the
 *         end of a session run.
 *
 */
public class EndOfSessionMessage extends Message implements IEndOfContextMessage {

    /**
     * ParseHandler performs the parsing of an XML version of the message into
     * a new instance of EndOfSessionMessage.
     */
    public static class XmlParseHandler extends BaseXmlMessageParseHandler {
        private EndOfSessionMessage msg;
        private boolean inSessionId;
        
        /**
         * Parse handler for session identification information.
         */
        private SessionIdentificationParser sessionIdParser;
        private final ApplicationContext appContext;
        
        /**
         * Constructor.
         * 
         * @param appContext the current application context
         */
        public XmlParseHandler(final ApplicationContext appContext) {
            this.appContext = appContext;
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

            if (qname.equalsIgnoreCase(MessageRegistry.getDefaultInternalXmlRoot(SessionMessageType.EndOfSession))) {
            	setInMessage(false);
                msg = null;
                
            } else if (qname.equalsIgnoreCase(SessionIdentificationParser.SESSION_ID_TAG)) {
                this.inSessionId = false;
                this.sessionIdParser.endElement(uri, localName, qname, buffer);

            } else if (inSessionId) {
                this.sessionIdParser.endElement(uri, localName, qname, buffer);
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

            if (qname.equalsIgnoreCase(SessionMessageType.EndOfSession + MessageConstants.MESSAGE_ROOT_SUFFIX)) {
            	setInMessage(true);
                final IAccurateDateTime d = this.getDateFromAttr(attr, IMessage.EVENT_TIME_TAG);
                msg = new EndOfSessionMessage(new SessionIdentification(appContext.getBean(MissionProperties.class)));
                msg.setEventTime(d);
                addMessage(msg);
            } else if (qname.equalsIgnoreCase(SessionIdentificationParser.SESSION_ID_TAG)) {
                this.inSessionId = true;
                this.sessionIdParser = new SessionIdentificationParser(msg.sessionId);
                this.sessionIdParser.startElement(uri, localName, qname, attr);
            } else if (this.inSessionId) {
                 this.sessionIdParser.startElement(uri, localName, qname, attr);
            }
        }
    }

    private final ITelemetrySummary summary;
    
    private IContextIdentification sessionId;
    
    /**
     * Creates an instance of EndOfSessionMessage with the given SessionIdentification.
     * Start and end times must be set into the ID object by the caller.
     * 
     * @param id the SessionIdentification
     */
    public EndOfSessionMessage(final IContextIdentification id) {
        super(SessionMessageType.EndOfSession, System.currentTimeMillis());
        summary = null;
        this.sessionId = id;
    }



    /**
     * Creates an instance of EndOfSessionMessage. The start and end time must be set
     * into the supplied session identification object by the caller.
     * 
     * @param id the SessionIdentification object
     * @param ss
     *            session summary
     */
    public EndOfSessionMessage(final IContextIdentification id,
            final ITelemetrySummary ss) {
        super(SessionMessageType.EndOfSession, System.currentTimeMillis());

        sessionId = id;
        summary = ss;

        setEventTime(new AccurateDateTime());
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
            writer.writeStartElement(MessageRegistry.getDefaultInternalXmlRoot(getType())); // <EndOfSessionMessage>
            writer.writeAttribute(IMessage.EVENT_TIME_TAG, getEventTimeString());
            
            super.generateStaxXml(writer);
            
            writer.writeStartElement("fromSSE"); //<fromSSE>
            writer.writeCharacters(String.valueOf(this.isFromSse()));
            writer.writeEndElement(); //</fromSSE>
            
            if (sessionId != null) {
                sessionId.generateStaxXml(writer);
            }

            writer.writeEndElement(); // </EndOfSessionMessage>
        } finally {
            TimeUtility.releaseFormatterToPool(df);
        }
    }

    /**
     * Retrieves the endTime member.
     * @return Returns the endTime as a Timestamp
     */
    public IAccurateDateTime getEndTime() {
        return sessionId == null ? null : sessionId.getEndTime();
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.interfaces.EscapedCsvSupport#getEscapedCsv()
     */
    @Override
	public String getEscapedCsv() {
        return "<end></end>";
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.message.IMessage#getOneLineSummary()
     */
    @Override
    public String getOneLineSummary() {
        final StringBuilder ret = new StringBuilder("End of session ");
        if (sessionId == null) {
            ret.append("Unknown (key=Unknown)");
        } else {
            ret.append(sessionId.getName() + " (key ");
            if (sessionId.getNumber() == null) {
                ret.append("Unknown)");
            } else {
                ret.append(sessionId.getNumber() + ")");
            }
        }
        if (sessionId == null || sessionId.getEndTime() == null) {
            ret.append(" at time Unknown");
        } else {
            ret.append(" at time " + sessionId.getEndTimeStr());
        }
        return ret.toString();
    }

    /**
     * Gets the SessionSummary object for the session that just ended.
     * 
     * @return Session summary
     */
    public ITelemetrySummary getSessionSummary() {
        return summary;
    }

    /**
     * Gets the ContextIdentification object for the session.
     * 
     * @return ContextIdentification object
     */
    public IContextIdentification getContextId() {
        return this.sessionId;
    }
    
    /**
     * Retrieves the session start time.
     * 
     * @return Returns the startTime as a time stamp
     */
    public IAccurateDateTime getStartTime() {
        return sessionId == null ? null : sessionId.getStartTime();
    }

    /**
     * Overridden by remote subclasses.
     * @return True if this is a remote message
     */
    public boolean isRemote() {
        return false;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.message.Message#setTemplateContext(java.util.Map)
     */
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

        if (sessionId != null) {
            sessionId.setTemplateContext(map);
            map.put("sessionConfiguration", sessionId.toXml());
        }
        TimeUtility.releaseFormatterToPool(df);
    }

    /**
     * {@inheritDoc}
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getOneLineSummary();
    }
    
    /**
     * Sets the context identification object for the session that ended.
     * 
     * @param id ContextIdentification object
     */
    public void setContextId(final IContextIdentification id) {
        sessionId = id;
        
    }
}
