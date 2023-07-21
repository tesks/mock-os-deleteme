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
package jpl.gds.tc.impl.message;

import java.text.DateFormat;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import jpl.gds.shared.message.BaseXmlMessageParseHandler;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.TimeUtility;
import jpl.gds.tc.api.CommandStatusType;
import jpl.gds.tc.api.UplinkFailureReason;
import jpl.gds.tc.api.message.CommandMessageType;
import jpl.gds.tc.api.message.ICpdUplinkMessage;
import jpl.gds.tc.api.message.IFlightCommandMessage;

/**
 * The representation of an FSW command as a notification sent on the
 * message bus. This message is sent out on the bus when an FSW command is
 * successfully transmitted to the FSW.
 * 
 */
public class FlightSoftwareCommandMessage extends AbstractCommandMessage
implements ICpdUplinkMessage, IFlightCommandMessage {


    /** Generated SCMF file path and name. **/
    private String scmfFile;

    /** ICMD request ID for the command request represented in this message. */
    private String iCmdRequestId;

    /** Status from ICMD for the command request represented in this message. */
    private CommandStatusType iCmdRequestStatus;

    /**
     * Failure reason reported by ICMD for the command request represented in
     * this message.
     */
    private UplinkFailureReason iCmdRequestFailureReason;

    /**
     * Unique ID used for linking this command message to its TransmitEvent
     * object
     */
    private int transmitEventId;

    /**
     * The SCMF checksum
     */
    private Long scmfChecksum;

    /**
     * The total CLTUs
     */
    private Long totalCltus;

    /**
     * The radiation time of the first bit
     */
    private IAccurateDateTime   bit1RadTime;

    /**
     * The radiation time of the last bit
     */
    private IAccurateDateTime   lastBitRadTime;

    /**
     * Create a new FSW command message
     */
    protected FlightSoftwareCommandMessage() {
        super(CommandMessageType.FlightSoftwareCommand);
        this.scmfFile = null;
        this.iCmdRequestId = null;
        this.iCmdRequestStatus = null;
        this.iCmdRequestFailureReason = null;
    }

    /**
     * Create a new FSW command message
     * 
     * @param commandString The FSW command that triggered this message to be
     *            created
     */
    public FlightSoftwareCommandMessage(final String commandString) {
        super(CommandMessageType.FlightSoftwareCommand, commandString);
        this.scmfFile = null;
        this.iCmdRequestId = null;
        this.iCmdRequestStatus = null;
        this.iCmdRequestFailureReason = null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.shared.xml.stax.StaxSerializable#generateStaxXml(javax.xml.stream.XMLStreamWriter)
     */
    @Override
    public void generateStaxXml(final XMLStreamWriter writer)
            throws XMLStreamException {
        final DateFormat df = TimeUtility.getFormatterFromPool();
        try {
            writer.writeStartElement("FlightSoftwareCommandMessage"); // <FlightSoftwareCommandMessage>
            writer.writeAttribute(IMessage.EVENT_TIME_TAG, getEventTimeString());
            
            super.generateStaxXml(writer);

            writer.writeStartElement("FlightSoftwareCommand"); // <FlightSoftwareCommand>

            writer.writeStartElement("ICmdRequestId"); // <ICmdRequestId>
            writer.writeCharacters(this.iCmdRequestId != null ? this.iCmdRequestId
                    : "");
            writer.writeEndElement(); // </ICmdRequestId>

            writer.writeStartElement("ICmdRequestStatus"); // <ICmdRequestStatus>
            writer.writeCharacters(this.iCmdRequestStatus != null ? this.iCmdRequestStatus
                    .toString() : "");
            writer.writeEndElement(); // </ICmdRequestStatus>

            if (this.iCmdRequestFailureReason != null) {
                writer.writeStartElement("ICmdRequestFailureReason"); // <ICmdRequestFailureReason>
                writer.writeCharacters(this.iCmdRequestFailureReason.toString());
                writer.writeEndElement(); // </ICmdRequestFailureReason>
            }

            writer.writeStartElement("CommandString"); // <CommandString>
            writer.writeCharacters(getDatabaseString());
            writer.writeEndElement(); // </CommandString>

            writer.writeStartElement("ScmfFile"); // <ScmfFile>
            writer.writeCharacters(this.scmfFile != null ? this.scmfFile : "");
            writer.writeEndElement(); // </ScmfFile>

            writer.writeStartElement("ScmfChecksum"); // <ScmfChecksum>
            writer.writeCharacters(this.scmfChecksum != null ? Long.toString(this.scmfChecksum) : "-1");
            writer.writeEndElement(); // </ScmfChecksum>

            writer.writeStartElement("TotalCltus"); // <TotalCltus>
            writer.writeCharacters(this.totalCltus != null ? Long.toString(this.totalCltus) : "-1");
            writer.writeEndElement(); // </TotalCltus>

            writer.writeEndElement(); // </FlightSoftwareCommand>
            writer.writeEndElement(); // </FlightSoftwareCommandMessage>
        } finally {
            TimeUtility.releaseFormatterToPool(df);
        }
    }

    /**
     * ParseHandler is the message-specific SAX parse handler for creating this
     * Message from its XML representation.
     * 
     */
    public static class XmlParseHandler extends BaseXmlMessageParseHandler {
        /** The FSW command message being recreated from XML */
        private FlightSoftwareCommandMessage msg;

        /**
         * Create a new parse handler
         */
        public XmlParseHandler() {
            super();

            this.msg = null;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.xml.sax.ContentHandler#startElement(java.lang.String,
         *      java.lang.String, java.lang.String, org.xml.sax.Attributes)
         */
        @Override
        public void startElement(final String uri, final String localName, final String qname,
                final Attributes attr) throws SAXException {
            super.startElement(uri, localName, qname, attr);

            if (qname.equalsIgnoreCase(MessageRegistry.getDefaultInternalXmlRoot(CommandMessageType.FlightSoftwareCommand))) {
            	setInMessage(true);
                this.msg = new FlightSoftwareCommandMessage();
                this.msg.setEventTime(getDateFromAttr(attr,
                        IMessage.EVENT_TIME_TAG));
            }
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.xml.sax.ContentHandler#endElement(java.lang.String,
         *      java.lang.String, java.lang.String)
         */
        @Override
        public void endElement(final String uri, final String localName, final String qname)
                throws SAXException {
            super.endElement(uri, localName, qname);

            if (qname.equalsIgnoreCase("ICmdRequestId")) {
                this.msg.setICmdRequestId(getBufferText());
            } else if (qname.equalsIgnoreCase("ICmdRequestStatus")) {

                try {
                    this.msg.setICmdRequestStatus(CommandStatusType
                            .valueOf(getBufferText()));
                } catch (final IllegalArgumentException iae) {
                    // ICmdRequestStatus value was invalid. Do nothing. If the
                    // parsed messages' status is null, here is probably why.
                }

            } else if (qname.equalsIgnoreCase("ICmdRequestFailureReason")) {
                this.msg.setICmdRequestFailureReason(UplinkFailureReason
                        .safeValueOf(getBufferText()));
            } else if (qname.equalsIgnoreCase("CommandString")) {
                this.msg.setCommandString(getBufferText());
            } else if (qname.equalsIgnoreCase("ScmfFile")) {
                this.msg.setScmfFilename(getBufferText());
            } else if (qname.equalsIgnoreCase("ScmfChecksum")) {
                this.msg.setChecksum(Long.parseLong(getBufferText()));
            } else if (qname.equalsIgnoreCase("TotalCltus")) {
                this.msg.setTotalCltus(Long.parseLong(getBufferText()));
            } else if (qname.equals(MessageRegistry.getDefaultInternalXmlRoot(CommandMessageType.FlightSoftwareCommand))) {
                addMessage(this.msg);
                setInMessage(false);
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.tc.api.message.ICpdUplinkMessage#getICmdRequestId()
     */
    @Override
    public String getICmdRequestId() {
        return this.iCmdRequestId;
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.tc.api.message.ICpdUplinkMessage#setICmdRequestId(java.lang.String)
     */
    @Override
    public void setICmdRequestId(final String iCmdRequestId) {
        this.iCmdRequestId = iCmdRequestId;
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.tc.api.message.ICpdUplinkMessage#getICmdRequestStatus()
     */
    @Override
    public CommandStatusType getICmdRequestStatus() {
        return this.iCmdRequestStatus;
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.tc.api.message.ICpdUplinkMessage#setICmdRequestStatus(jpl.gds.tc.api.CommandStatusType)
     */
    @Override
    public void setICmdRequestStatus(final CommandStatusType CommandStatusType) {
        this.iCmdRequestStatus = CommandStatusType;
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.tc.api.message.ICpdUplinkMessage#getICmdRequestFailureReason()
     */
    @Override
    public String getICmdRequestFailureReason() {
        if (this.iCmdRequestFailureReason == null) {
            this.iCmdRequestFailureReason = UplinkFailureReason.NONE;
        }

        return this.iCmdRequestFailureReason.toString();
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.tc.api.message.ICpdUplinkMessage#setICmdRequestFailureReason(UplinkFailureReason)
     */
    @Override
    public void setICmdRequestFailureReason(
            final UplinkFailureReason iCmdRequestFailureReason) {
        this.iCmdRequestFailureReason = iCmdRequestFailureReason;
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.tc.api.message.ICpdUplinkMessage#getOriginalFilename()
     */
    @Override
    public String getOriginalFilename() {
        // there is no original file/SCMF, only generated one.
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.tc.api.message.ICpdUplinkMessage#setOriginalFilename(java.lang.String)
     */
    @Override
    public void setOriginalFilename(final String origFilename) {
        // do nothing
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.tc.api.message.ICpdUplinkMessage#getScmfFilename()
     */
    @Override
    public String getScmfFilename() {
        return scmfFile;
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.tc.api.message.ICpdUplinkMessage#setScmfFilename(java.lang.String)
     */
    @Override
    public void setScmfFilename(final String scmfFilename) {
        this.scmfFile = scmfFilename;
    }

    /**
     * Gets the id that links this cmd message with a transmit event
     * 
     * @return hashcode object associated with TransmitEvent object
     */
    @Override
    public int getTransmitEventId() {
        return transmitEventId;
    }

    /**
     * Sets the id that links this cmd message with a transmit event
     * 
     * @param transmitEventId hashcode associated with TransmitEvent object for
     *            this msg
     */   
    @Override
    public void setTransmitEventId(final int transmitEventId) {
        this.transmitEventId = transmitEventId;
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.tc.impl.message.AbstractUplinkMessage#setTemplateContext(java.util.Map)
     */
    @Override
    public void setTemplateContext(final Map<String, Object> map) {
        super.setTemplateContext(map);

        map.put("scmfFile", this.scmfFile != null ? this.scmfFile : "");

        map.put("iCmdRequestId", this.iCmdRequestId);
        map.put("iCmdRequestStatus", this.iCmdRequestStatus);

        if (this.iCmdRequestFailureReason != null) {
            map.put("iCmdRequestFailureReason", this.iCmdRequestFailureReason);
        }
        
        if (this.scmfChecksum != null) {
             map.put("scmfChecksum", this.scmfChecksum);
        }
        if (this.totalCltus != null) {
            map.put("totalCltus", this.totalCltus);
        }

    }

    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.tc.impl.icmd.ICpdUplinkMessage#getChecksum()
     */
    @Override
    public Long getChecksum() {
        return this.scmfChecksum;
    }

    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.tc.impl.icmd.ICpdUplinkMessage#setChecksum(java.lang.Long)
     */
    @Override
    public void setChecksum(final Long checksum) {
        this.scmfChecksum = checksum;
    }

    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.tc.impl.icmd.ICpdUplinkMessage#getTotalCltus()
     */
    @Override
    public Long getTotalCltus() {
        return this.totalCltus;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * jpl.gds.tc.impl.icmd.ICpdUplinkMessage#setTotalCltus(java.lang.Long)
     */
    @Override
    public void setTotalCltus(final Long totalCltus) {
        this.totalCltus = totalCltus;
    }

    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.tc.impl.icmd.ICpdUplinkMessage#getDssId()
     */
    @Override
    public Integer getDssId() {
        return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.tc.impl.icmd.ICpdUplinkMessage#getBit1RadTime()
     */
    @Override
    public IAccurateDateTime getBit1RadTime() {
        return this.bit1RadTime;
    }

    @Override
    public void setBit1RadTime(final IAccurateDateTime bit1RadTime) {
        this.bit1RadTime = bit1RadTime;
    }

    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.tc.impl.icmd.ICpdUplinkMessage#getLastBitRadTime()
     */
    @Override
    public IAccurateDateTime getLastBitRadTime() {
        return this.lastBitRadTime;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * jpl.gds.tc.impl.icmd.ICpdUplinkMessage#setLastBitRadTime(java.util.Date)
     */
    @Override
    public void setLastBitRadTime(final IAccurateDateTime lastBitRadTime) {
        this.lastBitRadTime = lastBitRadTime;
    }
    
}
