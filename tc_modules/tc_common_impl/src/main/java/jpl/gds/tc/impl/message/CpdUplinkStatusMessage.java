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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.springframework.context.ApplicationContext;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.common.config.mission.StationMapper;
import jpl.gds.shared.message.BaseXmlMessageParseHandler;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.tc.api.CommandStatusType;
import jpl.gds.tc.api.ICpdUplinkStatus;
import jpl.gds.tc.api.message.CommandMessageType;
import jpl.gds.tc.api.message.ICommandMessageFactory;
import jpl.gds.tc.api.message.ICpdUplinkStatusMessage;
import jpl.gds.tc.impl.icmd.CpdUplinkStatus;

/**
 * This class represents the message of ICMD-based uplink status. It carries a
 * single <code>CpdUplinkStatus</code> object, which has been polled from CPD.
 * 
 * @since AMPCS R3
 */
public class CpdUplinkStatusMessage extends AbstractUplinkMessage implements ICpdUplinkStatusMessage {

	/** The status */
	private final ICpdUplinkStatus status;

	/** If true, do not insert into database. Not part of message state, */
	private boolean doNotInsert = false;

	/**
	 * Constructs a new ICmdUplinkStatusesMessage.
	 */
	public CpdUplinkStatusMessage(final ICpdUplinkStatus status) {
		super(CommandMessageType.UplinkStatus, status.getTimestamp());
		this.status = status;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.message.ICpdUplinkStatusMessage#getStatus()
     */
	@Override
    public ICpdUplinkStatus getStatus() {
		return status;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.shared.xml.stax.StaxSerializable#generateStaxXml(javax.xml.stream.XMLStreamWriter)
	 */
	@Override
	public void generateStaxXml(final XMLStreamWriter writer)
			throws XMLStreamException {

		writer.writeStartElement(MessageRegistry.getDefaultInternalXmlRoot(CommandMessageType.UplinkStatus)); // <CpdUplinkStatusMessage>
		writer.writeAttribute(IMessage.EVENT_TIME_TAG, getEventTimeString());
		
		super.generateStaxXml(writer);

		writer.writeStartElement("RequestID"); // <RequestID>
		writer.writeCharacters(status.getId());
		writer.writeEndElement(); // </RequestID>

		writer.writeStartElement("StatusUpdateTime"); // <StatusUpdateTime>
		writer.writeCharacters(status.getTimestampString());
		writer.writeEndElement(); // </StatusUpdateTime>

		writer.writeStartElement("RequestStatus"); // <RequestStatus>
		writer.writeCharacters(status.getStatus().toString());
		writer.writeEndElement(); // </RequestStatus>

		if (status.getFilename() != null) {
			writer.writeStartElement("Filename"); // <Filename>
			writer.writeCharacters(status.getFilename());
			writer.writeEndElement(); // </Filename>
		}

		if (status.getBitrates() != null && !status.getBitrates().isEmpty()) {
			writer.writeStartElement("BitrateRange"); // <BitrateRange>

			for (int i = status.getBitrates().size() - 1; i >=0; i--) {
				writer.writeStartElement("Bitrate"); // <Bitrate>
				writer.writeCharacters(Float.toString(status.getBitrates().get(i)));
				writer.writeEndElement(); // </Bitrate>
			}

			writer.writeEndElement(); // </BitrateRange>
		}

		if (status.getEstRadDurations() != null && !status.getEstRadDurations().isEmpty()) {
			writer.writeStartElement("RadDurationRange"); // <RadDurationRange>

			for (int i = status.getEstRadDurations().size() - 1; i >=0; i--) {
				writer.writeStartElement("Duration"); // <Duration>
				writer.writeCharacters(Float.toString(status.getEstRadDurations().get(i)));
				writer.writeEndElement(); // </Duration>
			}

			writer.writeEndElement(); // </RadDurationRange>
		}

		if (status.getUserId() != null) {
			writer.writeStartElement("UserID"); // <UserID>
			writer.writeCharacters(status.getUserId());
			writer.writeEndElement(); // </UserID>
		}

		if (status.getRoleId() != null) {
			writer.writeStartElement("RoleID"); // <RoleID>
			writer.writeCharacters(status.getRoleId());
			writer.writeEndElement(); // </RoleID>
		}

		if (status.getSubmitTime() != null) {
			writer.writeStartElement("SubmitTime"); // <SubmitTime>
			writer.writeCharacters(status.getSubmitTime());
			writer.writeEndElement(); // </SubmitTime>
		}

		if (status.getIncludedInExeList() != null) {
			writer.writeStartElement("IncludedInExeList"); // <IncludedInExeList>
			writer.writeCharacters(status.getIncludedInExeList());
			writer.writeEndElement(); // </IncludedInExeList>
		}

		if (status.getUplinkMetadata() != null) {
			writer.writeStartElement("UplinkMetadata"); // <UplinkMetadata>
			writer.writeCharacters(status.getUplinkMetadata()
					.toMetadataString());
			writer.writeEndElement(); // </UplinkMetadata>
		}

		if (status.getBit1RadTime() != null && status.getBit1RadTimeString() != null) {
			writer.writeStartElement("Bit1RadTime"); // <Bit1RadTime>
			writer.writeCharacters(status.getBit1RadTimeString());
			writer.writeEndElement(); // </Bit1RadTime>
		}

		if (status.getLastBitRadTime() != null && status.getLastBitRadTimeString() != null) {
			writer.writeStartElement("LastBitRadTime"); // <LastBitRadTime>
			writer.writeCharacters(status.getLastBitRadTimeString());
			writer.writeEndElement(); // </LastBitRadTime>
		}

		if (status.getTotalCltus() > -1) {
			writer.writeStartElement("TotalCltus"); // <TotalCltus>
			writer.writeCharacters(Integer.toString(status.getTotalCltus()));
			writer.writeEndElement(); // </TotalCltus>
		}

		if (status.getChecksum() != null) {
			writer.writeStartElement("Checksum"); // <Checksum>
			writer.writeCharacters(status.getChecksum());
			writer.writeEndElement(); // </Checksum>
		}

		writer.writeEndElement(); // </CpdUplinkStatusMessage>
	}

	/**
	 * Message-specific SAX parse handler for creating this Message from its XML
	 * representation.
	 * 
	 * @since AMPCS R3
	 */
	public static class XmlParseHandler extends BaseXmlMessageParseHandler {
		
		private StationMapper stationMapper;
        private ICommandMessageFactory msgFactory;

		public XmlParseHandler(final ApplicationContext appContext) {
			this.stationMapper = appContext.getBean(MissionProperties.class).getStationMapper();
			this.msgFactory = appContext.getBean(ICommandMessageFactory.class);
		}

		/** The value holders needed to rebuild messages from XML. */
        private IAccurateDateTime currentMsgEventTime;
		private String currentReqId;
		private CommandStatusType currentStatus;
        private IAccurateDateTime currentTimestamp;
		private String currentFilename;
		private List<Float> currentBitrates;
		private List<Float> currentRadDurations;
		private String currentUserId;
		private String currentRoleId;
		private String currentSubmitTime;
		private String currentIncludedInExeList;
		private String currentUplinkMetadataString;
        private IAccurateDateTime currentBit1RadTime;
        private IAccurateDateTime currentLastBitRadTime;
		private int currentTotalCltus;
		private String currentChecksum;

		/**
		 * Constructs a new parse handler.
		 */
		public XmlParseHandler() {
			super();

			resetAllValueHolders();
		}

		private void resetAllValueHolders() {
			currentMsgEventTime = null;

			currentReqId = null;
			currentStatus = null;
			currentTimestamp = null;
			currentFilename = null;
			currentBitrates = null;
			currentRadDurations = null;
			currentUserId = null;
			currentRoleId = null;
			currentSubmitTime = null;
			currentIncludedInExeList = null;
			currentUplinkMetadataString = null;
		}

		/**
		 * @{inheritDoc}
		 * @see jpl.gds.shared.message.BaseXmlMessageParseHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
		 */
		@Override
		public void startElement(final String uri, final String localName, final String qName,
				final Attributes attr) throws SAXException {
			super.startElement(uri, localName, qName, attr);

			if (qName.equalsIgnoreCase(MessageRegistry.getDefaultInternalXmlRoot(CommandMessageType.UplinkStatus))) {
				setInMessage(true);
				currentMsgEventTime = getDateFromAttr(attr,
						IMessage.EVENT_TIME_TAG);

			} else if (qName.equalsIgnoreCase("BitrateRange")) {
				currentBitrates = new ArrayList<Float>();

			} else if (qName.equalsIgnoreCase("RadDurationRange")) {
				currentRadDurations = new ArrayList<Float>();

			}

		}


		/**
		 * @{inheritDoc}
		 * @see jpl.gds.shared.message.BaseXmlMessageParseHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
		 */
		@Override
		public void endElement(final String uri, final String localName, final String qName)
				throws SAXException {
			super.endElement(uri, localName, qName);

			if (qName.equalsIgnoreCase("RequestID")) {
				currentReqId = getBufferText();

			} else if (qName.equalsIgnoreCase("StatusUpdateTime")) {
				currentTimestamp = getDateFromBuffer();

			} else if (qName.equalsIgnoreCase("RequestStatus")) {
				currentStatus = CommandStatusType.valueOf(getBufferText());

			} else if (qName.equalsIgnoreCase("Filename")) {
				currentFilename = getBufferText();

			} else if (qName.equalsIgnoreCase("Bitrate")) {
				currentBitrates.add(Float.valueOf(getBufferText()));

			} else if (qName.equalsIgnoreCase("Duration")) {
				currentRadDurations.add(Float.valueOf(getBufferText()));

			} else if (qName.equalsIgnoreCase("UserID")) {
				currentUserId = getBufferText();

			} else if (qName.equalsIgnoreCase("RoleID")) {
				currentRoleId = getBufferText();

			} else if (qName.equalsIgnoreCase("SubmitTime")) {
				currentSubmitTime = getBufferText();

			} else if (qName.equalsIgnoreCase("IncludedInExeList")) {
				currentIncludedInExeList = getBufferText();

			} else if (qName.equalsIgnoreCase("UplinkMetadata")) {
				currentUplinkMetadataString = getBufferText();
			} else if (qName.equalsIgnoreCase("Bit1RadTime")) {
				currentBit1RadTime = getDateFromBuffer();
			} else if (qName.equalsIgnoreCase("LastBitRadTime")) {
				currentLastBitRadTime = getDateFromBuffer();
			} else if (qName.equalsIgnoreCase("TotalCltus")) {
				currentTotalCltus = Integer.parseInt(getBufferText());
				
			} else if (qName.equalsIgnoreCase(MessageRegistry.getDefaultInternalXmlRoot(CommandMessageType.UplinkStatus))) {
				final ICpdUplinkStatusMessage msg = msgFactory
                        .createCpdUplinkStatusMessage(new CpdUplinkStatus(stationMapper, 
                        		currentReqId, currentStatus,
								currentTimestamp, currentFilename,
								currentBitrates, currentRadDurations,
								currentUserId, currentRoleId,
								currentSubmitTime, currentIncludedInExeList,
								currentUplinkMetadataString, currentChecksum,
								currentTotalCltus, currentBit1RadTime,
								currentLastBitRadTime));
				msg.setEventTime(currentMsgEventTime);
				addMessage(msg);

				resetAllValueHolders();
				setInMessage(false);
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
		final StringBuilder sb = new StringBuilder(1024);
		sb.append("Uplink request status update: Status=");
		sb.append(status.getStatus().toString());
		sb.append(" (Request ID=");
		sb.append(status.getId());
		sb.append(")");
		return sb.toString();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.shared.message.IMessage#toString()
	 */
	@Override
	public String toString() {
		return getOneLineSummary();
	}

	/**
	 * Get database string. Not used.
	 * 
	 * @return Database string.
	 */
	@Override
	public String getDatabaseString() {
		return null;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.session.message.SessionBasedMessage#setTemplateContext(java.util.Map)
	 */
	@Override
	public void setTemplateContext(final Map<String, Object> map) {
		super.setTemplateContext(map);

		map.put(IMessage.EVENT_TIME_TAG,
				getEventTime() != null ? getEventTimeString() : null);
		map.put("requestId", status.getId());
		map.put("statusUpdateTime", status.getTimestampString());
		map.put("requestStatus", status.getStatus().toString());
		map.put("filename", status.getFilename());
		map.put("bitrateRange", status.getBitrates());
		if (status.getEstRadDurations() != null) {
		    map.put("radDurationRange", status.getEstRadDurations());
		}
		map.put("userId", status.getUserId());
		map.put("roleId", status.getRoleId());
		map.put("submitTime", status.getSubmitTime());
		map.put("includedInExeList", status.getIncludedInExeList());
		if (status.getUplinkMetadata() != null) {
		    map.put("uplinkMetadata", status.getUplinkMetadata()
                    .toMetadataString());
        }
		if (status.getBit1RadTime() != null && status.getBit1RadTimeString() != null) {
            map.put("bit1RadTime", status.getBit1RadTimeString());
        }
		
	    if (status.getLastBitRadTime() != null && status.getLastBitRadTimeString() != null) {
            map.put("lastBitRadTime", status.getLastBitRadTimeString());
        }

        if (status.getTotalCltus() > -1) {
            map.put("totalCltus", status.getTotalCltus()); // <TotalCltus>
        }

        if (status.getChecksum() != null) {
            map.put("checksum", status.getChecksum()); // <Checksum>
        }

	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.message.ICpdUplinkStatusMessage#setDoNotInsert()
     */
	@Override
    public void setDoNotInsert() {
		doNotInsert = true;
	}

	/**
     * @{inheritDoc}
     * @see jpl.gds.tc.api.message.ICpdUplinkStatusMessage#getDoNotInsert()
     */
	@Override
    public boolean getDoNotInsert() {
		return doNotInsert;
	}
	
}
