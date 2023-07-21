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
import jpl.gds.tc.api.message.IFlightCommandMessage;

/**
 * The representation of a sequence directive as a notification sent on the
 * message bus. This message is sent out on the bus when a sequence directive is
 * successfully transmitted to the FSW.
 * 
 *
 * This class was updated for MPCS-6328 to make sequence directives work
 *       in the database and the chill_up GUI as they did for MSL. However, if
 *       it becomes necessary to support sequence directives through CPD, this
 *       message is inadequate. The CPD/ICMD-related members and behaviors
 *       associated with FlighSoftwareCommandMessage are needed here. MPCS-6620
 *       has been filed to track this issue.  9/15/14.
 *       
 * MPCS-9142 - 10/02/17 - (R8 clone of MPCS-6620) Updated
 *      SequenceDirectiveMessage to be able to be sent via CPD. This class now
 *      implements the ICpdUplinkMessage interface, all necessary variables 
 *      (iCmdRequestId, iCmdRequestStatus, iCmdRequestFairureReason, scmfChcksum,
 *      totalCltus, bit1RadTime, lastBitRadTime, and scmfFile), their associated
 *      get and set functions have been added, and ParseHandler has been updated
 *      to parse the new values.
 */
public class SequenceDirectiveMessage extends AbstractCommandMessage implements IFlightCommandMessage
{
	/* MPCS-6328 - 9/15/14. Add transmit ID and success flag*/
	/**
	 * Unique ID used for linking this command message to its TransmitEvent
	 * object
	 */
	private int transmitEventId;

	// MPCS-9142 - 10/02/17 - removed isSuccessful, no longer used.
    
    /** ICMD request ID for the command request represented in this message. */
    protected String iCmdRequestId;

    /** Status from ICMD for the command request represented in this message. */
    protected CommandStatusType iCmdRequestStatus;

    /**
     * Failure reason reported by ICMD for the command request represented in
     * this message.
     */
    protected UplinkFailureReason iCmdRequestFailureReason;

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
    private IAccurateDateTime bit1RadTime;

    /**
     * The radiation time of the last bit
     */
    private IAccurateDateTime lastBitRadTime;
    
    /** Generated SCMF file path and name. **/
    private String scmfFile;

	/**
	 * Create a new sequence directive message
	 */
	protected SequenceDirectiveMessage()
	{
		super(CommandMessageType.SequenceDirective);
	}
	
	/**
	 * Create a new sequence directive message
	 * 
	 * @param commandString The sequence directive that was transmitted to FSW
	 */
	public SequenceDirectiveMessage(final String commandString)
	{
		super(CommandMessageType.SequenceDirective,commandString);
	}


	/**
     * @see jpl.gds.tc.api.message.ITransmittableCommandMessage#getTransmitEventId()
     */
	@Override
    public int getTransmitEventId() {
		return transmitEventId;
	}

	/**
     * @see jpl.gds.tc.api.message.ITransmittableCommandMessage#setTransmitEventId(int)
     */
	@Override
    public void setTransmitEventId(final int transmitEventId) {
		this.transmitEventId = transmitEventId;
	}

	// MPCS-6620 - 08/30/17 - removed isSuccessful and setSuccessful, no longer used.
	
	@Override
    public String getICmdRequestId() {
        return this.iCmdRequestId;
    }

    @Override
    public void setICmdRequestId(final String iCmdRequestId) {
        this.iCmdRequestId = iCmdRequestId;
        
    }

    @Override
    public CommandStatusType getICmdRequestStatus() {
        return this.iCmdRequestStatus;
    }

    @Override
    public void setICmdRequestStatus(final CommandStatusType iCmdRequestStatus) {
        this.iCmdRequestStatus = iCmdRequestStatus;
        
    }

    @Override
    public String getICmdRequestFailureReason() {
        if (this.iCmdRequestFailureReason == null) {
            this.iCmdRequestFailureReason = UplinkFailureReason.NONE;
        }
        
        return this.iCmdRequestFailureReason.toString();
    }

    @Override
    public void setICmdRequestFailureReason(final UplinkFailureReason iCmdRequestFailureReason) {
        this.iCmdRequestFailureReason = iCmdRequestFailureReason;
        
    }

    @Override
    public String getOriginalFilename() {
        // there is no original file/SCMF, only generated one.
        return null;
    }

    @Override
    public void setOriginalFilename(final String origFilename) {
        // do nothing
        
    }

    @Override
    public String getScmfFilename() {
        return this.scmfFile;
    }

    @Override
    public void setScmfFilename(final String scmfFilename) {
        this.scmfFile = scmfFilename;       
    }

    @Override
    public Long getChecksum() {
        return this.scmfChecksum;
    }

    @Override
    public Long getTotalCltus() {
        return this.totalCltus;
    }

    @Override
    public Integer getDssId() {
        return null;
    }

    @Override
    public IAccurateDateTime getBit1RadTime() {
        return this.bit1RadTime;
    }

    @Override
    public IAccurateDateTime getLastBitRadTime() {
        return this.lastBitRadTime;
    }

    @Override
    public void setChecksum(final Long checksum) {
        this.scmfChecksum = checksum;
        
    }

    @Override
    public void setTotalCltus(final Long totalCltus) {
        this.totalCltus = totalCltus;
        
    }

    @Override
    public void setBit1RadTime(final IAccurateDateTime bit1RadTime) {
        this.bit1RadTime = bit1RadTime;
        
    }

    @Override
    public void setLastBitRadTime(final IAccurateDateTime lastBitRadTime) {
        this.lastBitRadTime = lastBitRadTime;
        
    }

    /**
     * {@inheritDoc}
     * 
     * @see 
     *      jpl.gds.tc.impl.message.AbstractUplinkMessage#setTemplateContext(Map
     *      <String,Object>)
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

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.shared.xml.stax.StaxSerializable#generateStaxXml(javax.xml.stream.XMLStreamWriter)
	 */
	@Override
	public void generateStaxXml(final XMLStreamWriter writer) throws XMLStreamException
	{
		final DateFormat df = TimeUtility.getFormatterFromPool();
		try
		{
			writer.writeStartElement("SequenceDirectiveMessage"); // <SequenceDirectiveMessage>
			writer.writeAttribute(IMessage.EVENT_TIME_TAG,getEventTimeString());

			super.generateStaxXml(writer);
			
			writer.writeStartElement("SequenceDirective"); // <SequenceDirective>

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

            writer.writeEndElement(); // </SequenceDirective>
            writer.writeEndElement(); // </SequenceDirectiveMessage>
        } finally {
            TimeUtility.releaseFormatterToPool(df);
        }
	}

	/**
	 * ParseHandler is the message-specific SAX parse handler for creating this Message from its XML representation. 
	 * 
	 */
	public static class XmlParseHandler extends BaseXmlMessageParseHandler
	{
		/** The sequence directive message being rebuilt from XML */
		private SequenceDirectiveMessage msg;

		/**
		 * Create a new parse handler
		 */
		public XmlParseHandler()
		{
			super();

			this.msg = null;
		}

		/**
		 * {@inheritDoc}
		 * @see org.xml.sax.ContentHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
		 */
		@Override
		public void startElement(final String uri, final String localName, final String qname, final Attributes attr) throws SAXException
		{
			super.startElement(uri, localName, qname, attr);			

			if(qname.equalsIgnoreCase(MessageRegistry.getDefaultInternalXmlRoot(CommandMessageType.SequenceDirective)))
			{
				setInMessage(true);
				this.msg = new SequenceDirectiveMessage();
				this.msg.setEventTime(getDateFromAttr(attr, IMessage.EVENT_TIME_TAG));
			}
		}

		/**
		 * {@inheritDoc}
		 * @see org.xml.sax.ContentHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
		 */
		@Override
		public void endElement(final String uri, final String localName, final String qname) throws SAXException
		{
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
            } else if (qname.equals(MessageRegistry.getDefaultInternalXmlRoot(CommandMessageType.SequenceDirective))) {
                addMessage(this.msg);
                setInMessage(false);
            }
		}
	}
}