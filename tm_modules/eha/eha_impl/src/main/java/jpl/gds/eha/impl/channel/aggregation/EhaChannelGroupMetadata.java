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
package jpl.gds.eha.impl.channel.aggregation;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TimerTask;
import java.util.TreeSet;

import com.google.protobuf.InvalidProtocolBufferException;

import jpl.gds.eha.api.channel.IClientChannelValue;
import jpl.gds.eha.api.channel.aggregation.serialization.Proto3ChanCategory;
import jpl.gds.eha.api.channel.aggregation.serialization.Proto3EhaAggregatedGroup;
import jpl.gds.eha.api.channel.aggregation.serialization.Proto3EhaGroupDiscriminator;
import jpl.gds.eha.api.channel.aggregation.serialization.Proto3EhaGroupMember;
import jpl.gds.eha.api.channel.serialization.Proto3ChanDefType;
import jpl.gds.eha.api.message.IAlarmedChannelValueMessage;
import jpl.gds.eha.api.message.aggregation.AggregateMessageType;
import jpl.gds.eha.api.message.aggregation.IEhaChannelGroupDiscriminator;
import jpl.gds.eha.api.message.aggregation.IEhaChannelGroupMember;
import jpl.gds.eha.api.message.aggregation.IEhaChannelGroupMetadata;
import jpl.gds.serialization.primitives.time.Proto3Adt;
import jpl.gds.serialization.primitives.time.Proto3ErtRange;
import jpl.gds.serialization.primitives.time.Proto3RctRange;
import jpl.gds.serialization.primitives.time.Proto3ScetRange;
import jpl.gds.serialization.primitives.time.Proto3Sclk;
import jpl.gds.serialization.primitives.time.Proto3SclkEncoding;
import jpl.gds.serialization.primitives.time.Proto3SclkRange;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.Sclk;

/**
 * This Class represents a grouping of EHA that has common metadata. It also
 * maintains a lifecycle timer such that, each group is serialized and
 * transmitted when it has achieved a certain size, or it has existed for a
 * maximum interval.
 * <p>
 * To facilitate the time aspect of this, this class extends TimerTask.
 * 
 * @TODO R8 Refactor TODO - add javadoc when this capability is mature
 */
public class EhaChannelGroupMetadata extends TimerTask implements IEhaChannelGroupMetadata {
    /**
     * 
     */
    private final GroupedChannelAggregationService service;
    
    /**
     * The descriminatorKey is an object whose equals() and hashCode() methods
     * that can be used to indicate compatibility between EhaChannelGroupMetadata.
     * This is used to look up an IEhaChannelGroupMetadata object that is compatible
     * with a prospective channel sample.
     */
    private final IEhaChannelGroupDiscriminator discriminatorKey;
    
    /**
     * The list of individual channels to be associated and serialized as
     * part of is group.
     */
    private final List<EhaChannelGroupMember> groupMembers;

    /**
     * These metadata collect ranges to which the associated group of
     * channels belong
     */
    private final SortedSet<String> channelIds;
    private final SortedSet<ISclk> sclkRange;
    private final SortedSet<IAccurateDateTime> scetRange;
    private final SortedSet<IAccurateDateTime> ertRange;
    private final SortedSet<IAccurateDateTime> rctRange;
    
    private AggregateMessageType aggregateMessageType;

    /**
     * Constructor that initializes values to that contained within the
     * IAlarmedChannelValueMessage
     * 
     * @param m
     *            the currently message being processed
     */
    public EhaChannelGroupMetadata(final GroupedChannelAggregationService service, final IEhaChannelGroupDiscriminator key, final IAlarmedChannelValueMessage m) {
        if (null == service) {
            throw new IllegalArgumentException("Service can not be null");
        }
        if (null == m) {
            throw new IllegalArgumentException("Message can not be null");
        }
        
        switch (m.getChannelValue().getDefinitionType()) {
		case FSW:
			this.aggregateMessageType = AggregateMessageType.AggregateAlarmedEhaChannel;
			break;
		case SSE:
			this.aggregateMessageType = AggregateMessageType.AggregateSseChannel;
			break;
		case M:
			this.aggregateMessageType = AggregateMessageType.AggregateMonitorChannel;
			break;
		case H:
	        this.aggregateMessageType = AggregateMessageType.AggregateHeaderChannel;
			break;
		default:
			break;
		}

        this.service = service;
        this.discriminatorKey = key;
        this.channelIds = new TreeSet<>();
        this.sclkRange = new TreeSet<>();
        this.scetRange = new TreeSet<>();
        this.ertRange = new TreeSet<>();
        this.rctRange = new TreeSet<>();
        this.groupMembers = new ArrayList<>();

        /*
         * Update range metadata
         */
        final IClientChannelValue value = m.getChannelValue();
        this.channelIds.add(value.getChanId());
        this.sclkRange.add(value.getSclk());
        this.scetRange.add(value.getScet());
        this.ertRange.add(value.getErt());
        this.rctRange.add(value.getRct());

        /*
         * Set invariant fields. Note: Invariant fields are represented by
         * group metadata, and do not need to be stored with the group
         * member data.
         */
        this.groupMembers.add(new EhaChannelGroupMember(m.getEventTime(), value));
    }
    
    /**
     * Create a new EhaCHannelGroupMetadata object from a protocol buffer.
     * 
     * @param buffer
     *            input byte buffer
     * @throws InvalidProtocolBufferException
     *             if there is an error parsing the buffer
     */
    public EhaChannelGroupMetadata(final byte[] buffer) throws InvalidProtocolBufferException {
    	this(Proto3EhaAggregatedGroup.parseFrom(buffer));
    }

    /**
     * Create a new EhaChannelGroupMetadata object from protocol buffer class.
     * 
     * @param proto
     */
    @SuppressWarnings("serial")
	public EhaChannelGroupMetadata(final Proto3EhaAggregatedGroup proto) {
    	this.service = null;
    	this.discriminatorKey = new EhaChannelGroupDiscriminator(proto.getDiscriminatorKey());
    	this.channelIds = new TreeSet<String>(proto.getChannelIdsList());
    	this.sclkRange = new TreeSet<ISclk>() {{
    		add(new Sclk(proto.getSclkRange().getMin().getSeconds(), proto.getSclkRange().getMin().getNanos(), EhaChannelGroupMetadata.this.discriminatorKey.getSclkEncoding()));
    		add(new Sclk(proto.getSclkRange().getMax().getSeconds(), proto.getSclkRange().getMax().getNanos(), EhaChannelGroupMetadata.this.discriminatorKey.getSclkEncoding()));
    	}};
        this.scetRange = new TreeSet<IAccurateDateTime>() {{
            add(new AccurateDateTime(proto.getScetRange().getMin().getMilliseconds(), proto.getScetRange().getMin().getNanoseconds()));
            add(new AccurateDateTime(proto.getScetRange().getMax().getMilliseconds(), proto.getScetRange().getMax().getNanoseconds()));
        }};
       	this.ertRange = new TreeSet<IAccurateDateTime>() {{
    		add(new AccurateDateTime(proto.getErtRange().getMin().getMilliseconds(), proto.getErtRange().getMin().getNanoseconds()));
    		add(new AccurateDateTime(proto.getErtRange().getMax().getMilliseconds(), proto.getErtRange().getMax().getNanoseconds()));
    	}};
        this.rctRange = new TreeSet<IAccurateDateTime>() {{
            add(new AccurateDateTime(proto.getRctRange().getMin().getMilliseconds(), proto.getRctRange().getMin().getNanoseconds()));
            add(new AccurateDateTime(proto.getRctRange().getMax().getMilliseconds(), proto.getRctRange().getMax().getNanoseconds()));
        }};

    	this.groupMembers = new ArrayList<>();
    	for (final Proto3EhaGroupMember member: proto.getValuesList()) {
    		this.groupMembers.add(new EhaChannelGroupMember(member));
    	}
    }

    /* (non-Javadoc)
     * @see jpl.gds.eha.impl.channel.aggregation.IEhaChannelGroupMetadata#updateMetadata(jpl.gds.eha.impl.channel.aggregation.EhaChannelGroupDiscriminator)
     */
    @Override
    public void updateMetadata(final IAccurateDateTime eventTime, final IClientChannelValue value) {
        this.channelIds.add(value.getChanId());
        this.ertRange.add(value.getErt());
        this.rctRange.add(value.getRct());
        this.sclkRange.add(value.getSclk());
        this.scetRange.add(value.getScet());
        this.groupMembers.add(new EhaChannelGroupMember(eventTime, value));
    }

    /* (non-Javadoc)
     * @see java.util.TimerTask#run()
     */
    @Override
    public void run() {
        service.publish(this);
    }

    /**
     * @return the discriminatorKey
     */
    @Override
    public IEhaChannelGroupDiscriminator getDiscriminatorKey() {
        return discriminatorKey;
    }

    /**
     * Returns a copy of the group member list
     * 
     * @return the groupMembers
     */
    @Override
    public List<IEhaChannelGroupMember> getGroupMembers() {
        return new ArrayList<>(groupMembers);
    }

    /**
     * Returns a copy of the list of Channel IDs contained in this group
     * 
     * @return the channelIds
     */
    @Override
    public List<String> getChannelIds() {
        return new ArrayList<>(channelIds);
    }
    
    /**
     * @return the number of unique channel IDs contained in this group
     */
    @Override
    public int getUniqueChannelIdCount() {
        return channelIds.size();
    }
    
    /**
     * @return the number of unique SCLKs contained in this group
     */
    @Override
    public int getUniqueSclkCount() {
        return sclkRange.size();
    }

    /**
     * @return the Low and High SCLK values in the current IEhaChannelGroup
     */
    @Override
    @SuppressWarnings("serial")
    public List<ISclk> getSclkRange() {
        return new ArrayList<ISclk>() {{
            add(sclkRange.first());
            add(sclkRange.last());
        }};
    }

    @Override
    public int size() {
        return groupMembers.size();
    }
    
    /**
     * Unpacks this aggregation and returns a list of individual channel
     * messages.
     * 
     * @return list of channel messages
     */
    @Override
    public List<IAlarmedChannelValueMessage> unpackAlarmedChannelValueMessages() {
    	final List<IAlarmedChannelValueMessage> msgList = new ArrayList<>();

    	/**
    	 * triviski 3/29/2017 - This was merged in from R8 and caused exceptions.  This seems to not be called by 
    	 * anything so for now it is commented out until Mark can look at it.
    	 */
//    	for (final EhaChannelGroupMember member: this.groupMembers) {
//    		final IChannelDefinition def = ChannelDefinitionFactory.createChannel(member.channelId, member.type, discriminatorKey.getChanType());
//    		final IServiceChannelValue value = ChannelValueFactory.createServiceChannelValue(def);
//    		value.setErt(discriminatorKey.getErt());
//    		value.setRealtime(discriminatorKey.isRealtime());
//    		value.setVcid(discriminatorKey.getVcid());
//
//    		value.setDn(member.dn);
//    		value.setEu(member.eu);
//    		value.setSclk(member.sclk);
//    		
////    		value.setAlarms(alarmSet);
////    		value.setChannelCategory(cce);
////    		value.setDssId(dssId);
////    		value.setFrameId(frameId);
////    		value.setLst(sol);
////    		value.setPacketId(packetId);
////    		value.setRct(postTime);
////    		value.setScet(scet);
//    		
//    		final IAlarmedChannelValueMessage msg = EhaMessageFactory.createAlarmedChannelMessage(value);
//    		msg.setEventTime(member.eventTime);
//    		msgList.add(msg);
//    	}
    	
    	return msgList;
    }
    
    /* (non-Javadoc)
     * @see jpl.gds.eha.api.channel.aggregation.IEhaChannelGroupMetadata#build()
     */
    @Override
    public Proto3EhaAggregatedGroup build() {
        /*
         * Build a list of channel values for group
         */
        final List<Proto3EhaGroupMember> ehaList = new ArrayList<>(groupMembers.size());
        for (final EhaChannelGroupMember eha: groupMembers) {
            
            ehaList.add(eha.build());
        }
        
        final Proto3EhaAggregatedGroup.Builder aggregateGroupBuilder = Proto3EhaAggregatedGroup.newBuilder();
        final Proto3EhaGroupDiscriminator.Builder proto3EhaGroupDisc = Proto3EhaGroupDiscriminator.newBuilder();
        
        switch (discriminatorKey.getChanType()) {
		case FSW:
			proto3EhaGroupDisc
				.setDssId(discriminatorKey.getDssId())
				.setVcid(discriminatorKey.getVcid())
				.setApid(discriminatorKey.getApid())
				.setSclkEncoding(Proto3SclkEncoding.newBuilder()
                        .setCoarseBits(discriminatorKey.getSclkEncoding().getCoarseBits())
                        .setFineBits(discriminatorKey.getSclkEncoding().getFineBits())
                        .setMaxFine(discriminatorKey.getSclkEncoding().getMaxFine()));
			
			aggregateGroupBuilder.setSclkRange(Proto3SclkRange.newBuilder()
                    .setMin(Proto3Sclk.newBuilder()
                            .setSeconds(sclkRange.first().getCoarse())
                            .setNanos(sclkRange.first().getFine()))
                    .setMax(Proto3Sclk.newBuilder()
                            .setSeconds(sclkRange.last().getCoarse())
                            .setNanos(sclkRange.last().getFine())));
			break;
		case H:
			proto3EhaGroupDisc
				.setDssId(discriminatorKey.getDssId())
				.setVcid(discriminatorKey.getVcid())
				.setApid(discriminatorKey.getApid());
			
			break;
		case M:
			proto3EhaGroupDisc
				.setDssId(discriminatorKey.getDssId());
		case SSE:
			proto3EhaGroupDisc
				.setApid(discriminatorKey.getApid())
				.setSclkEncoding(Proto3SclkEncoding.newBuilder()
                        .setCoarseBits(discriminatorKey.getSclkEncoding().getCoarseBits())
                        .setFineBits(discriminatorKey.getSclkEncoding().getFineBits())
                        .setMaxFine(discriminatorKey.getSclkEncoding().getMaxFine()));
			
			aggregateGroupBuilder.setSclkRange(Proto3SclkRange.newBuilder()
                    .setMin(Proto3Sclk.newBuilder()
                            .setSeconds(sclkRange.first().getCoarse())
                            .setNanos(sclkRange.first().getFine()))
                    .setMax(Proto3Sclk.newBuilder()
                            .setSeconds(sclkRange.last().getCoarse())
                            .setNanos(sclkRange.last().getFine())));
			break;
		default:
			break;
		}
        
        proto3EhaGroupDisc.setIsRealtime(discriminatorKey.isRealtime())
        	.setIsFromSSE(discriminatorKey.isSSE())
        	.setChanCategory(Proto3ChanCategory.valueOf(discriminatorKey.getChanCatEnum().name()))
        	.setChanType(Proto3ChanDefType.valueOf("CHAN_DEF_TYPE_" + discriminatorKey.getChanType().name()));
        
        
        aggregateGroupBuilder.setDiscriminatorKey(proto3EhaGroupDisc)
        	.setSamples(size())
        	.setErtRange(Proto3ErtRange.newBuilder()
        		.setMin(Proto3Adt.newBuilder()
        				.setMilliseconds(ertRange.first().getRoundedTimeAsMillis())
        				.setNanoseconds(ertRange.first().getNanoseconds()))
        		.setMax(Proto3Adt.newBuilder()
        				.setMilliseconds(ertRange.last().getRoundedTimeAsMillis())
        				.setNanoseconds(ertRange.last().getNanoseconds())))
            .setRctRange(Proto3RctRange.newBuilder()
                    .setMin(Proto3Adt.newBuilder()
                            .setMilliseconds(rctRange.first().getRoundedTimeAsMillis())
                            .setNanoseconds(rctRange.first().getNanoseconds()))
                    .setMax(Proto3Adt.newBuilder()
                            .setMilliseconds(rctRange.last().getRoundedTimeAsMillis())
                            .setNanoseconds(rctRange.last().getNanoseconds())))
        	.setScetRange(Proto3ScetRange.newBuilder()
            		.setMin(Proto3Adt.newBuilder()
            				.setMilliseconds(scetRange.first().getRoundedTimeAsMillis())
            				.setNanoseconds(scetRange.first().getNanoseconds()))
            		.setMax(Proto3Adt.newBuilder()
            				.setMilliseconds(scetRange.last().getRoundedTimeAsMillis())
            				.setNanoseconds(scetRange.last().getNanoseconds())))
        	
        	.addAllValues(ehaList);
        	
        final Proto3EhaAggregatedGroup aggregateGroup = aggregateGroupBuilder.build();
        
        ehaList.clear();
        this.channelIds.clear();
        this.ertRange.clear();
        this.rctRange.clear();
        this.sclkRange.clear();
        this.scetRange.clear();
        this.groupMembers.clear();
        
        return aggregateGroup;
    }
    
    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "EhaChannelGroupMetadata [discriminatorKey=" + discriminatorKey + ", # of Members=" + groupMembers.size() + ", channelIds=" + channelIds + ", sclkRange=" + sclkRange + "]";
    }

	@Override
	public AggregateMessageType getAggregateMessageType() {
		return this.aggregateMessageType;
	}
}
