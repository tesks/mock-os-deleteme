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
import com.googlecode.protobuf.format.JsonFormat;
import com.googlecode.protobuf.format.XmlFormat;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.dictionary.api.channel.ChannelDefinitionFactory;
import jpl.gds.dictionary.api.channel.ChannelDefinitionType;
import jpl.gds.dictionary.api.channel.ChannelType;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.eha.api.channel.ChannelCategoryEnum;
import jpl.gds.eha.api.channel.ChannelValueFilter;
import jpl.gds.eha.api.channel.IChannelValueFactory;
import jpl.gds.eha.api.channel.IServiceChannelValue;
import jpl.gds.eha.api.channel.aggregation.serialization.Proto3ChanCategory;
import jpl.gds.eha.api.channel.aggregation.serialization.Proto3EhaAggregatedChannelValueMessage;
import jpl.gds.eha.api.channel.aggregation.serialization.Proto3EhaAggregatedGroup;
import jpl.gds.eha.api.channel.aggregation.serialization.Proto3EhaGroupDiscriminator;
import jpl.gds.eha.api.channel.aggregation.serialization.Proto3EhaGroupMember;
import jpl.gds.eha.api.channel.serialization.Proto3AlarmedChannelValueMessage;
import jpl.gds.eha.api.channel.serialization.Proto3ChanDefType;
import jpl.gds.eha.api.channel.serialization.Proto3ChannelValue;
import jpl.gds.eha.api.message.EhaMessageType;
import jpl.gds.eha.api.message.IEhaGroupedChannelValueMessage;
import jpl.gds.eha.api.message.IEhaMessageFactory;
import jpl.gds.eha.api.message.aggregation.IEhaChannelGroupMetadata;
import jpl.gds.serialization.messages.Proto3AbstractMessage;
import jpl.gds.serialization.primitives.time.Proto3Adt;
import jpl.gds.serialization.primitives.time.Proto3Lst;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.message.BaseBinaryMessageParseHandler;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.time.AccurateDateTime;
import org.springframework.context.ApplicationContext;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Class EhaGroupedChannelValueMessage
 * @TODO R8 Refactor TODO - add javadoc when this capability is mature
 *
 */
public class EhaGroupedChannelValueMessage extends AbstractChannelMessage implements IEhaGroupedChannelValueMessage {

    private final Proto3EhaAggregatedGroup aggregatedGroup;

    private List<IServiceChannelValue> unpackedChans;

    /**
     * Constructor.
     *
     * @param aggMsg The EhaGroupedChannelValueMessage to be instantiated
     * @throws InvalidProtocolBufferException an error was encountered while parsing
     *                                        the protobuf message
     */
    public EhaGroupedChannelValueMessage(final Proto3EhaAggregatedChannelValueMessage aggMsg, final MissionProperties missionProperties)
            throws InvalidProtocolBufferException {
        super(EhaMessageType.GroupedEhaChannels, aggMsg.getSuper(), missionProperties);

        this.aggregatedGroup = aggMsg.getAggregatedGroup();
    }

    /**
     * Constructor.
     *
     * @param md                The IEhaChannelGroupMetadata to be instantiated
     * @param missionProperties mission properties
     */
    public EhaGroupedChannelValueMessage(final IEhaChannelGroupMetadata md, final MissionProperties missionProperties) {
        super(md.getAggregateMessageType(), System.currentTimeMillis(), missionProperties);
        this.aggregatedGroup = md.build();
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.eha.api.message.IEhaGroupedChannelValueMessage#numChannelValues()
     */
    @Override
    public int numChannelValues() {
        return aggregatedGroup.getSamples();
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * jpl.gds.eha.api.message.IEhaGroupedChannelValueMessage#getChannelValueGroup(
     * jpl.gds.eha.api.channel.ChannelValueFilter)
     */
    @Override
    public List<IServiceChannelValue> getChannelValueGroup(final ChannelValueFilter filter,
            final ApplicationContext appContext) {
        if (this.unpackedChans != null) {
            return this.unpackedChans;
        }

        this.unpackedChans = new ArrayList<IServiceChannelValue>();

        final Proto3EhaGroupDiscriminator dk = aggregatedGroup.getDiscriminatorKey();
        final Proto3ChanDefType ct = dk.getChanType();
        final ChannelDefinitionType chanDefType = ChannelDefinitionType
                .valueOf(ct.name().substring("CHAN_DEF_TYPE_".length()));

        final IChannelValueFactory factory = appContext.getBean(IChannelValueFactory.class);

        for (final Proto3EhaGroupMember member : aggregatedGroup.getValuesList()) {
            final ChannelType chanType = ChannelType
                    .valueOf(member.getDn().getType().name().substring("DN_TYPE_".length()));
            final IChannelDefinition def = ChannelDefinitionFactory.createChannel(member.getChannelId(), chanType,
                    chanDefType);
            final IServiceChannelValue val = factory.createServiceChannelValue(def);

            final Proto3Lst lst = Proto3Lst.newBuilder().setMilliseconds(1).setSol(1).build();
            final Proto3Adt scet = new AccurateDateTime(1, 1).buildAccurateDateTime();

            final Proto3AbstractMessage supes = Proto3AbstractMessage.newBuilder()
                                                                     .setEventTime(member.getEventTime()
                                                                                         .getMilliseconds())
                                                                     .build();
            final Proto3ChannelValue.Builder chanVal = Proto3ChannelValue.newBuilder().setTitle(def.getTitle())
                    .setChannelId(member.getChannelId()).setChanDefType(dk.getChanType()).setDn(member.getDn())
                    .setAlarms(member.getAlarmValueSet()).setVcid(dk.getVcid()).setIsRealtime(dk.getIsRealtime())
                    .setErt(member.getErt()).setRct(member.getRct()).setLst(lst).setScet(scet)
                    .setSclk(member.getSclk());
//  			.setStreamId(null)
            switch (member.getHasStatusCase()) {
            case HASSTATUS_NOT_SET:
                chanVal.setStatus(null);
                break;
            case STATUS:
                chanVal.setStatus(member.getStatus());
                break;
            }

            if (def.hasEu()) {
                chanVal.setEu(member.getEu());
            }

            final Proto3AlarmedChannelValueMessage value = Proto3AlarmedChannelValueMessage.newBuilder().setSuper(supes)
                    .setChanVal(chanVal).build();

            try {
                final AlarmedChannelValueMessage alarmMessage = new AlarmedChannelValueMessage(value, val, missionProperties);
                unpackedChans.add((IServiceChannelValue) alarmMessage.getChannelValue());
            } catch (final IOException e) {
                TraceManager.getDefaultTracer()
                        .warn("An error was encountered while parsing EhaGroupedChannelMessages: "
                                + ExceptionTools.getMessage(e));
            }
        }

        return unpackedChans;
    }

    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.shared.message.Message#getOneLineSummary()
     */
    @Override
    public String getOneLineSummary() {
        if (this.unpackedChans == null) {
            return null;
        }
        final StringBuilder sb = new StringBuilder();
        for (final IServiceChannelValue chan : this.unpackedChans) {
            sb.append(chan.toString() + "\n");
        }

        return sb.toString();

    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("EhaGroupedChannelValueMessage [aggregatedGroup=");
        builder.append(aggregatedGroup);
        builder.append("]");
        return builder.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.eha.impl.channel.aggregation.IEhaChannelGroupMetadata#toBinary()
     */
    @Override
    public byte[] toBinary() {
        return this.build().toByteArray();
    }

    @Override
    public Proto3EhaAggregatedChannelValueMessage build() {
        final Proto3EhaAggregatedChannelValueMessage.Builder retVal = Proto3EhaAggregatedChannelValueMessage
                .newBuilder();

        retVal.setSuper((Proto3AbstractMessage) super.build());
        retVal.setAggregatedGroup(aggregatedGroup);

        return retVal.build();
    }

    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.shared.message.Message#toXml()
     */
    @Override
    public String toXml() {
        final StringBuilder out = new StringBuilder();
        try {
            new XmlFormat().print(aggregatedGroup, out);
        } catch (final IOException e) {
            out.append("\n*** ").append(e.getMessage()).append(" ***\n");
        }
        return out.toString();
    }

    /**
     * Gets the JSON representation of this message.
     * 
     * @return JSON string
     */
    public String toJason() {
        final StringBuilder out = new StringBuilder();
        try {
            new JsonFormat().print(aggregatedGroup, out);
        } catch (final IOException e) {
            out.append("\n*** ").append(e.getMessage()).append(" ***\n");
        }
        return out.toString();
    }

    /**
     * BinaryParseHandler is the message-specific SAX parse handler for creating
     * this Message from its binary representation.
     * 
     */
    public static class BinaryParseHandler extends BaseBinaryMessageParseHandler {
        private final IEhaMessageFactory ehaMessageFactory;
        private final ApplicationContext appContext;

        /**
         * Constructor.
         * 
         * @param appContext the current application context
         */
        public BinaryParseHandler(final ApplicationContext appContext) {
            this.appContext = appContext;
            this.ehaMessageFactory = appContext.getBean(IEhaMessageFactory.class);
        }

        @Override
        public IMessage[] parse(final List<byte[]> content) throws IOException {

            for (final byte[] msgBytes : content) {
                final Proto3EhaAggregatedChannelValueMessage msg = Proto3EhaAggregatedChannelValueMessage
                        .parseFrom(msgBytes);
                final IEhaGroupedChannelValueMessage message = ehaMessageFactory.createGroupedChannelMessage(msg);
                message.getChannelValueGroup(appContext);
                addMessage(message);
            }

            return getMessages();
        }

    }

    @Override
    public boolean isRealtime() {
        return aggregatedGroup.getDiscriminatorKey().getIsRealtime();
    }

    @Override
    public ChannelCategoryEnum getChannelCategory() {
        final Proto3EhaGroupDiscriminator key = aggregatedGroup.getDiscriminatorKey();
        final Proto3ChanDefType cdt = key.getChanType();
        final Proto3ChanCategory ccat = key.getChanCategory();

        if (cdt == null) {
            return ChannelCategoryEnum.LOST_HEADER;
        }

        switch (cdt) {
        case CHAN_DEF_TYPE_FSW:
            return ChannelCategoryEnum.FSW;

        case CHAN_DEF_TYPE_SSE:
            return ChannelCategoryEnum.SSE;

        case CHAN_DEF_TYPE_M:
            return ChannelCategoryEnum.MONITOR;

        case CHAN_DEF_TYPE_H:
            if (key.getIsFromSSE()) {
                return ChannelCategoryEnum.SSEPACKET_HEADER;
            }

            switch (ccat) {
            case FRAME_HEADER:
                return ChannelCategoryEnum.FRAME_HEADER;
            case PACKET_HEADER:
                return ChannelCategoryEnum.PACKET_HEADER;
            }

        default:
            return ChannelCategoryEnum.LOST_HEADER;
        }
    }

    /**
     * Get APID
     *
     * @return APID
     */
    @Override
    public Integer getApid() {
        return aggregatedGroup.getDiscriminatorKey().getApid();
    }

    /**
     * Get VCID
     *
     * @return VCID
     */
    @Override
    public Integer getVcid() {
        return aggregatedGroup.getDiscriminatorKey().getVcid();
    }

    /**
     * Get DSSID
     *
     * @return DSSID
     */
    @Override
    public Integer getDssId() {
        return aggregatedGroup.getDiscriminatorKey().getDssId();
    }

    /**
     * Get ERT range state.
     *
     * @return True if ERT range populated
     */
    @Override
    public boolean hasErtRange() {
        return aggregatedGroup.hasErtRange();
    }

    /**
     * Get ERT minimum.
     *
     * @return ERT minimum
     */
    @Override
    public long getErtMinimumRange() {
        return aggregatedGroup.getErtRange().getMin().getMilliseconds();
    }

    /**
     * Get ERT maximum.
     *
     * @return ERT maximum
     */
    @Override
    public long getErtMaximumRange() {
        return aggregatedGroup.getErtRange().getMax().getMilliseconds();
    }

    /**
     * Get channel ids.
     *
     * @return List of channel ids.
     */
    @Override
    public List<String> getChannelIds() {
        return aggregatedGroup.getChannelIdsList();
    }

    /**
     * Get SCLK range state.
     *
     * @return True if SCLK range populated
     */
    @Override
    public boolean hasSclkRange() {
        return aggregatedGroup.hasSclkRange();
    }

    /**
     * Get SCLK minimum.
     *
     * @return SCLK minimum
     */
    @Override
    public long getSclkMinimumRange() {
        return aggregatedGroup.getSclkRange().getMin().getSeconds();
    }

    /**
     * Get SCLK maximum.
     *
     * @return SCLK maximum
     */
    @Override
    public long getSclkMaximumRange() {
        return aggregatedGroup.getSclkRange().getMax().getSeconds();
    }

    /**
     * Get number of values.
     *
     * @return Count of values
     */
    @Override
    public int getValuesCount() {
        return aggregatedGroup.getValuesCount();
    }

    @Override
    public byte[] toBinaryWithoutContextHeaders() {
        return this.aggregatedGroup.toByteArray();
    }

    @Override
    public Proto3EhaAggregatedGroup getEhaAggregatedGroup() {
        return this.aggregatedGroup;
    }

    @Override
    public long getScetMaximumRange() {
        return aggregatedGroup.getScetRange().getMax().getMilliseconds();
    }

    @Override
    public long getScetMinimumRange() {
        return aggregatedGroup.getScetRange().getMin().getMilliseconds();
    }

    @Override
    public boolean hasScetRange() {
        return aggregatedGroup.hasScetRange();
    }

    @Override
    public boolean hasRctRange() {
        return aggregatedGroup.hasRctRange();
    }

    @Override
    public long getRctMinimumRange() {
        return aggregatedGroup.getRctRange().getMin().getMilliseconds();
    }

    @Override
    public long getRctMaximumRange() {
        return aggregatedGroup.getRctRange().getMax().getMilliseconds();
    }
}
