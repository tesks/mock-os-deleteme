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
import jpl.gds.tm.service.api.frame.IFrameSequenceAnomalyMessage;
import jpl.gds.tm.service.api.frame.ITelemetryFrameInfo;

/**
 * The FrameSequenceAnomalyMessage class is a message to create when processing
 * a frame encounters an anomaly.
 */
public class FrameSequenceAnomalyMessage extends AbstractFrameEventMessage implements IFrameSequenceAnomalyMessage
{	

    /**
     * A long to store the actual Vcfc.
     */
    private long actualVcfc;
    /**
     * A long to store the expected Vcfc.
     */
    private long expectedVcfc;
    
    /**
     * Creates an instance of FrameSequenceAnomalyMessage.
     *
     * @param dsnI
     *            the station information object associated with the message
     * @param frameInfo
     *            the frame information object for the last in-sync frame
     * @param logType
     *            The log type of this frame event message.
     * @param expected
     *            the expected frame counter (VCFC)
     * @param actual
     *            the actual frame counter (VCFC)
     * @throws IllegalArgumentException
     *             If an illegal argument is passed.
     */
    protected FrameSequenceAnomalyMessage(final IStationTelemInfo dsnI,
            final ITelemetryFrameInfo frameInfo, final LogMessageType logType,
            final long expected, final long actual) {
        super(TmServiceMessageType.FrameSequenceAnomaly,
                TraceSeverity.WARNING, logType, dsnI, frameInfo);
        this.expectedVcfc = expected;
        this.actualVcfc = actual;
    } 
    
    /**
     * Returns the actual VCFC associated with this message.
     * @return the actual VCFC associated with this message.
     */
    @Override
    public long getActualVcfc() {
		return actualVcfc;
	}

	/**
	 * Sets the actual VCFC associated with this message.
	 * @param actualVcfc The actual VCFC to set.
	 */
	public void setActualVcfc(final long actualVcfc) {
		this.actualVcfc = actualVcfc;
	}

	/**
	 * Returns the expected VCFC associated with this message.
	 * @return the expected VCFC associated with this message.
	 */
	@Override
    public long getExpectedVcfc() {
		return expectedVcfc;
	}

	/**
	 * Sets the expected VCFC associated with this message.
	 * @param expectedVcfc The expected VCFC to set.
	 */
	public void setExpectedVcfc(final long expectedVcfc) {
		this.expectedVcfc = expectedVcfc;
	}
    
    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.message.PublishableLogMessage#getMessage()
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
        map.put("actualVcfc", this.actualVcfc);
        map.put("expectedVcfc", this.expectedVcfc);
        if (this.getLogType() != null) {
            map.put("anomalyType", this.getLogType().toString());
        }
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.tm.service.impl.frame.AbstractFrameEventMessage#generateInternalStaxXml(javax.xml.stream.XMLStreamWriter)
     */
    @Override
    public void generateInternalStaxXml(final XMLStreamWriter writer) throws XMLStreamException
    {
        writer.writeStartElement("anomalyType"); // <anomalyType>
        writer.writeCharacters(this.logType != null ? this.logType.toString() : "");
        writer.writeEndElement(); // </anomalyType>

        writer.writeStartElement("expectedVcfc"); // <expectedVcfc>
        writer.writeCharacters(Long.toString(this.expectedVcfc));
        writer.writeEndElement(); // </expectedVcfc>

        writer.writeStartElement("actualVcfc"); // <actualVcfc>
        writer.writeCharacters(Long.toString(this.actualVcfc));
        writer.writeEndElement(); // </actualVcfc>

    }

    /**
     * XML parsing class for this message.
     * 
     *
     * @since R8
     */
    public static class XmlParseHandler extends FrameEventMessageParseHandler {

        private LogMessageType type;
        private long expected;
        private long actual;
        
        
        /**
         * Constructor.
         * @param appContext the current application context
         */
        public XmlParseHandler(final ApplicationContext appContext) {
            super(appContext, TmServiceMessageType.FrameSequenceAnomaly);
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
                this.type = null;
                this.expected = 0;
                this.actual = 0;
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
                ((FrameSequenceAnomalyMessage)this.msg).actualVcfc = actual;
                ((FrameSequenceAnomalyMessage)this.msg).expectedVcfc= expected;
                ((FrameSequenceAnomalyMessage)this.msg).setLogType(this.type);
            	setInMessage(false);
            } else if (qname.equalsIgnoreCase("anomalyType")) {
                this.type = LogMessageType.fromStringValue(getBufferText());
            } else if (qname.equalsIgnoreCase("actualVcfc")) {
                this.actual = Long.parseLong(getBufferText());
            } else if (qname.equalsIgnoreCase("expectedVcfc")) {
                this.expected = Long.parseLong(getBufferText());
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
            final IFrameSequenceAnomalyMessage msg = new FrameSequenceAnomalyMessage(stationInfo, tfInfo, null, 0, 0);
            msg.setEventTime(time);
            return msg;
        }
    }


    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.message.PublishableLogMessage#getOneLineSummary()
     */
    @Override
    public String getOneLineSummary() {
        return super.getOneLineSummary("FRAME SEQ ANOMALY") + ", " +
        (this.getLogType() == null? "Unknown" : this.getLogType().toString()) +  
        ", Expected VCFC=" + this.expectedVcfc +
        ", Actual VCFC=" + this.actualVcfc;
    }

    
}
