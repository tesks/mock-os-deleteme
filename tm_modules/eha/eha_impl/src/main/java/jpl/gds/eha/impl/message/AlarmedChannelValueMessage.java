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
package jpl.gds.eha.impl.message;

import com.google.protobuf.InvalidProtocolBufferException;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.dictionary.api.alarm.AlarmLevel;
import jpl.gds.eha.api.channel.IChannelValueFactory;
import jpl.gds.eha.api.channel.IClientChannelValue;
import jpl.gds.eha.api.channel.serialization.Proto3AlarmedChannelValueMessage;
import jpl.gds.eha.api.channel.serialization.Proto3AlarmedChannelValueMessage.HasStreamIdCase;
import jpl.gds.eha.api.message.EhaMessageType;
import jpl.gds.eha.api.message.IAlarmedChannelValueMessage;
import jpl.gds.eha.api.message.IChannelValueMessage;
import jpl.gds.serialization.messages.Proto3AbstractMessage;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.interfaces.EscapedCsvSupport;
import jpl.gds.shared.message.BaseBinaryMessageParseHandler;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.MessageRegistry;
import org.springframework.context.ApplicationContext;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * AlarmedChannelValueMessage is an message that indicates the generation of an alarmed
 * expanded channelized data value.
 * 
 */

public class AlarmedChannelValueMessage extends AbstractChannelMessage implements
    IAlarmedChannelValueMessage {

	/**
	 * Channel value.
	 */
	protected IClientChannelValue channelVal;

	/**
	 * Creates an instance of AlarmedChannelValueMessage with a current event time and the specified channel value.
	 *
	 * @param val               the channel value
	 * @param missionProperties mission properties
	 */
	public AlarmedChannelValueMessage(final IClientChannelValue val, final MissionProperties missionProperties) {
		super(EhaMessageType.AlarmedEhaChannel, System.currentTimeMillis(), missionProperties);
		setChannelValue(val);
	}
	
	/**
	 * object containing a Protocol Buffer Serialization.
	 *
	 * @param msg
	 *            the protobuf message used to create this object
	 * @param val
	 *            the fully populated channel value that is in alarm
	 * @throws InvalidProtocolBufferException
	 *             if there is an issue creating the message from the protobuf
	 *
	 */
	public AlarmedChannelValueMessage(final Proto3AlarmedChannelValueMessage msg, final IClientChannelValue val, final MissionProperties missionProperties)
			throws InvalidProtocolBufferException {
		super(EhaMessageType.AlarmedEhaChannel, msg.getSuper(), missionProperties);
		setChannelValue(val);
		if (msg.getHasStreamIdCase().equals(HasStreamIdCase.STREAMID)) {
			setStreamId(msg.getStreamId());
		}

	}

	/**
	 * Creates an instance of AlarmedChannelValueMessage from the given BasicChannelValueMessage.
	 *
	 * @param message           the BasicChannelValueMessage
	 * @param missionProperties mission properties
	 */
	public AlarmedChannelValueMessage(final IChannelValueMessage message, final MissionProperties missionProperties) {
		super(EhaMessageType.AlarmedEhaChannel, message.getRawEventTime(), missionProperties);
		setStreamId(message.getStreamId());
		setChannelValue(message.getChannelValue());
		setContextHeader(message.getMetadataHeader());
		setFromSse(message.isFromSse());
	}

    /**
	 * Retrieves the realtime flag, indicating whether this is a realtime or a
	 * recorded channel
	 * 
	 * @return true if the channel value is realtime, false if recorded
	 */
	@Override
    public boolean isRealtime() {
		return channelVal == null? true : channelVal.isRealtime();
	}


	/**
	 * Sets the channel value object associated with this message.
	 * 
	 * @param val the value as an IInternalChannelValue object
	 */
	protected void setChannelValue(final IClientChannelValue val) {
		channelVal = val;
	}

	/**
	 * Retrieves the channel value object associated with this message.
	 * 
	 * @return the IInternalChannelValue
	 */
	@Override
    public IClientChannelValue getChannelValue() {
		return channelVal;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.shared.message.IMessage#toString()
	 */
	@Override
	public String toString() {
		return "AlarmedChannelValueMessage[" + "ChanVal=" + getChannelValue() + ", "
				+ "RCT=" + getChannelValue().getRct() + ", " + "ERT="
				+ getChannelValue().getErt() + ", " + "SCLK="
				+ getChannelValue().getSclk() + ", " + "SCET="
				+ getChannelValue().getScet() + ", " + "LST="
				+ getChannelValue().getLst() + "]";
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.shared.interfaces.EscapedCsvSupport#getEscapedCsv()
	 */
	@Override
	public String getEscapedCsv() {
		final StringBuilder builder = new StringBuilder(256);
		builder.append("chan");
		builder.append(CSV_SEPARATOR);
		if (getEventTime() != null) {
			builder.append(getEventTimeString());
			builder.append(CSV_SEPARATOR);
			builder.append(getEventTime().getTime());
			builder.append(CSV_SEPARATOR);
		} else {
			builder.append("");
			builder.append(CSV_SEPARATOR);
			builder.append(0);
			builder.append(CSV_SEPARATOR);
		}

		builder.append(((EscapedCsvSupport) channelVal).getEscapedCsv());

		return builder.toString();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public synchronized void setTemplateContext(final Map<String, Object> map) {
		super.setTemplateContext(map);
		
		if (getEventTime() != null) {
			map.put(IMessage.EVENT_TIME_TAG, getEventTimeString());
		} else {
			map.put(IMessage.EVENT_TIME_TAG, null);
		}
		if (channelVal != null) {
		    channelVal.setTemplateContext(map);
		}

		if (missionProperties != null) {
			map.put("missionId", missionProperties.getMissionId());
			map.put("missionName", missionProperties.getMissionLongName());
			final int scid = missionProperties.getDefaultScid();
			map.put("spacecraftName", missionProperties.mapScidToName(scid));
			map.put("spacecraftId", scid);
		}

		/* R8 Refactor TODO - Alarms and realtime flag now added
		 * by the channel value. But the format of the alarms is now
		 * different. Old templates will break. They must be fixed, and 
		 * this must be flagged as an interface change.
		 */
	}
	
	   /**
     * {@inheritDoc}
     * @see jpl.gds.shared.xml.stax.StaxSerializable#generateStaxXml(javax.xml.stream.XMLStreamWriter)
     */
    @Override
    public void generateStaxXml(final XMLStreamWriter writer) throws XMLStreamException
    {
        writer.writeEmptyElement(MessageRegistry.getDefaultInternalXmlRoot(EhaMessageType.AlarmedEhaChannel));
    }
	
	/**
	 * BinaryParseHandler is the message-specific SAX parse handler for creating
	 * this Message from its binary representation.
	 * 
	 */
	public static class BinaryParseHandler extends BaseBinaryMessageParseHandler {
		private final IChannelValueFactory channelValueFactory;
		private final MissionProperties missionProperties;

        /**
         * Constructor.
         * 
         * @param appContext
         *            the current application context
         */
		public BinaryParseHandler(final ApplicationContext appContext) {
            this.channelValueFactory = appContext.getBean(IChannelValueFactory.class);
			this.missionProperties = appContext.getBean(MissionProperties.class);
		}

        @Override
        public IMessage[] parse(final List<byte[]> content) throws IOException {
            for (final byte[] msgBytes : content) {
                final Proto3AlarmedChannelValueMessage msg = Proto3AlarmedChannelValueMessage.parseFrom(msgBytes);
                final IClientChannelValue val = channelValueFactory.createClientChannelValue(msg.getChanVal());
                final AlarmedChannelValueMessage message = new AlarmedChannelValueMessage(msg, val, missionProperties);

                addMessage(message);
            }

            return getMessages();
        }

	}

	
	/**
	 * {@inheritDoc}
	 * @see jpl.gds.shared.message.IMessage#getOneLineSummary()
	 */
	@Override
	public synchronized String getOneLineSummary() {
		final StringBuilder result = new StringBuilder();
		if (channelVal != null) {
			if (channelVal.getChanId() != null) {
				result.append("Channel ID=" + channelVal.getChanId());
			} else {
				result.append("No Channel ID");
			}
			if (channelVal.getTitle() != null) {
				result.append(" Title="
						+ channelVal.getTitle());
			}
			/* R8 Refactor TODO - Removed output of channel name. Need to straighten out
			 * what is name and what is title and we have both. This is an interface change.
			 */
//			if (channelVal.getName() != null) {
//				result.append(" FswName="
//						+ channelVal.getChannelDefinition().getName());
//			}
			if (channelVal.getDn() != null) {
				result.append(" DN=" + channelVal.getDn().toString());
			}
			/*  Use hasEu() on channel value rather than on definition. */
			if (channelVal.hasEu()) {
				result.append(" EU=" + channelVal.getEu());
			}
			/* R8 Refactor TODO - Removed output of units.  This is an interface change.
             */
//			if (channelVal.getChannelDefinition() != null
//					&& channelVal.getChannelDefinition().getDnUnits() != null
//					&& channelVal.getChannelDefinition().getDnUnits().length() != 0) {
//				result.append(" Units="
//						+ channelVal.getChannelDefinition().getDnUnits());
//			}
			if (channelVal.getSclk() != null) {
				result.append(" SCLK=" + channelVal.getSclk().toString());
			}
			if (channelVal.getScet() != null) {
				result.append(" SCET="
						+ channelVal.getScet().getFormattedScet(true));
			}
			if (channelVal.getLst() != null) {
				result.append(" LST="
						+ channelVal.getLst().getFormattedSol(true));
			}
			if (channelVal.getErt() != null) {
				result.append(" ERT="
						+ channelVal.getErt().getFormattedErt(true));
			}
			if (channelVal.getDnAlarmLevel() != AlarmLevel.NONE ||
			        channelVal.getEuAlarmLevel() != AlarmLevel.NONE) {
				result.append(" InAlarm=yes");
			}
		} else {
			result.append("No Channel Value");
		}
		return result.toString();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.shared.template.FullyTemplatable#getMtakFieldCount()
	 */
	@Override
	public int getMtakFieldCount() {
		return 0;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see jpl.gds.shared.template.FullyTemplatable#getXmlRootName()
	 */
	@Override
	public String getXmlRootName() {
		return null;
	}
    
	/**
	 * {@inheritDoc}
	 * @see jpl.gds.shared.message.IMessage#toBinary()
	 */
	@Override
	public byte[] toBinary() {

        return this.build().toByteArray();
	}
	
	@Override
	public Proto3AlarmedChannelValueMessage build() {

        /*
         * Set non calculated values in message
         */
        final Proto3AlarmedChannelValueMessage.Builder retVal = Proto3AlarmedChannelValueMessage.newBuilder();
        retVal.setSuper((Proto3AbstractMessage)super.build());
        retVal.setChanVal(this.channelVal.build());
        if (getStreamId() != null) {
            retVal.setStreamId(this.streamId);
        }
		return retVal.build();
	}
}
