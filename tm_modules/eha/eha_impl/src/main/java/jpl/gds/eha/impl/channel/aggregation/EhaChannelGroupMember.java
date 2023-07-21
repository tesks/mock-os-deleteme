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

import com.google.protobuf.InvalidProtocolBufferException;

import jpl.gds.dictionary.api.alarm.AlarmLevel;
import jpl.gds.dictionary.api.channel.ChannelType;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.eha.api.channel.ChannelCategoryEnum;
import jpl.gds.eha.api.channel.IAlarmValueSet;
import jpl.gds.eha.api.channel.IClientChannelValue;
import jpl.gds.eha.api.channel.IServiceChannelValue;
import jpl.gds.eha.api.channel.aggregation.serialization.Proto3EhaGroupMember;
import jpl.gds.eha.api.channel.aggregation.serialization.Proto3EhaGroupMember.HasEuCase;
import jpl.gds.eha.api.channel.serialization.Proto3Dn;
import jpl.gds.eha.api.channel.serialization.Proto3DnType;
import jpl.gds.eha.api.message.aggregation.IEhaChannelGroupMember;
import jpl.gds.eha.impl.alarm.AlarmValueSet;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.Sclk;

/**
 *  Class EhaChannelGroupMember
 *         Set individual channel fields. Note: Invariant fields are represented
 *         by group metadata, and do not need to be stored with the group member
 *         data.
 * 
 *         NOTE: There are no getters or setters. This is an immutable class,
 *         and is designed for high-performance data access. All data members
 *         have package-private access.
 * 
 * @TODO R8 Refactor TODO - add javadoc when this capability is mature
 */
public class EhaChannelGroupMember implements IEhaChannelGroupMember {
    final IAccurateDateTime eventTime;
    final String channelId;
    final ISclk sclk;
    final IAccurateDateTime ert;
    final IAccurateDateTime rct;
    final Object dn;
    double eu;
    
    
    final String status;
    final ChannelType type;
    IAlarmValueSet alarmValueSet;
    AlarmLevel aLevel;
    String aState;
    final String name;
    String module;
    final String dnFormat;
    final String euFormat;
    long packetId;
    final IAccurateDateTime scet;
	private boolean hasEu;
	int channelIndex;

    /**
     * @param date 
     * @param value
     */
    public EhaChannelGroupMember(final IAccurateDateTime eventTime, final IClientChannelValue value) {
        this.eventTime = eventTime;
        this.channelId = value.getChanId().toUpperCase();
        
        if (((IServiceChannelValue)value).getChannelCategory().equals(ChannelCategoryEnum.FRAME_HEADER)) {
        	this.sclk = null;
        } else {
        	this.sclk = value.getSclk();
        }
        this.rct = value.getRct();
        this.ert = value.getErt();
        this.dn = value.getDn();
        if (value.hasEu()) {
        	this.eu = value.getEu();
        	this.hasEu = true;
    	}
        this.status = value.getStatus();
        this.type = value.getChannelType();
        this.alarmValueSet = ((IServiceChannelValue)value).getAlarms();
        this.aState = (null == value.getDnAlarmState()) ? "" : value.getDnAlarmState();
        this.aLevel = value.getDnAlarmLevel();
        this.name = value.getTitle();
        this.channelIndex = ((IServiceChannelValue)value).getChannelDefinition().getIndex();
        this.module = value.getCategory(IChannelDefinition.MODULE);
        this.dnFormat = ((IServiceChannelValue)value).getChannelDefinition().getDnFormat();
        this.euFormat = ((IServiceChannelValue)value).getChannelDefinition().getEuFormat();
        this.scet = value.getScet();
        if (((IServiceChannelValue)value).getPacketId().getValue() != null) {
        	this.packetId = ((IServiceChannelValue)value).getPacketId().getValue().longValue();
        }
    }

    /**
     * @param value
     * @throws InvalidProtocolBufferException 
     */
    public EhaChannelGroupMember(final byte[] buffer) throws InvalidProtocolBufferException {
    	this(Proto3EhaGroupMember.parseFrom(buffer));
    }
    
    /**
     * @param value
     */
    public EhaChannelGroupMember(final Proto3EhaGroupMember member) {
        /*
         * Set discrete values to be serialized to data archive
         */
        this.eventTime = new AccurateDateTime(member.getEventTime());
        this.channelId = member.getChannelId();
        this.sclk = new Sclk(member.getSclk().getSeconds(), member.getSclk().getNanos());
        this.scet = new AccurateDateTime(member.getScet().getMilliseconds(), member.getScet().getNanoseconds());
        this.ert = new AccurateDateTime(member.getErt().getMilliseconds(), member.getErt().getNanoseconds());
        this.rct = new AccurateDateTime(member.getRct().getMilliseconds(), member.getRct().getNanoseconds());

        if (member.getName() != null) {
        	this.name = member.getName();
        } else {
        	this.name = "";
        }
        if (member.getModule() != null) {
        	this.module = member.getModule();
        } else {
        	this.module = "";
        }
        
        final Proto3Dn protoDn = member.getDn();
        switch (protoDn.getType()) {
			case DN_TYPE_ASCII:
				this.dn = protoDn.getString();
				break;
			case DN_TYPE_BOOLEAN:
				this.dn = protoDn.getBool();
				break;
			case DN_TYPE_SIGNED_INT:
				this.dn = Integer.valueOf(protoDn.getInt());
				break;
			case DN_TYPE_DIGITAL:
			case DN_TYPE_TIME:
			case DN_TYPE_STATUS:
				switch (protoDn.getDnCase()) {
					case _UINT:
						this.dn = protoDn.getUint();
						break;
					case _ULONG:
						this.dn = protoDn.getUlong();
						break;
					default:
                    	throw new RuntimeException("Illegal value encountered for \"" + protoDn.getType() + "\": (" + protoDn.getDnCase() + ")" + member.getDn());
				}
				break;
			case DN_TYPE_UNSIGNED_INT:
				this.dn = protoDn.getUint();
				break;
			case DN_TYPE_FLOAT:
				this.dn = protoDn.getDouble();
				break;
			case UNRECOGNIZED:
			case DN_TYPE_UNKNOWN:
			default:
				this.dn = null;
				break;
        }
        if(member.getHasEuCase().equals(HasEuCase.EU)){
			this.eu = member.getEu();
			this.hasEu = true;
		}
        this.type = ChannelType.valueOf(protoDn.getType().name().substring("DN_TYPE_".length()));
		switch(member.getHasAlarmValueSetCase()) {
		case ALARMVALUESET:
			this.alarmValueSet = new AlarmValueSet(member.getAlarmValueSet());
			break;
		case HASALARMVALUESET_NOT_SET:
			this.alarmValueSet = null;
			break;
		}
        //this.dssId = member.getDssId();
        this.status = member.getStatus();
        this.dnFormat = (member.getDnFormat() == null) ? "" : member.getDnFormat();
        this.euFormat = (member.getEuFormat() == null) ? "" : member.getEuFormat();
        this.packetId = member.getPacketId();

    }
    
    @Override
    public Proto3EhaGroupMember build() {
        /*
         * Create a builder for this channel sample
         */
        final Proto3EhaGroupMember.Builder member = Proto3EhaGroupMember.newBuilder();
                
        /*
         * Set non-calculated values
         */
        member.setChannelId(this.channelId)
        .setErt(this.ert.buildAccurateDateTime())
        .setRct(this.rct.buildAccurateDateTime())
        .setStatus((this.status == null) ? "" : this.status)
        .setEventTime(this.eventTime.buildAccurateDateTime())
        .setName(this.name)
        .setChannelIndex(this.channelIndex)
        .setModule((this.module == null) ? "" : this.module.toUpperCase())
        .setDnFormat((this.dnFormat == null) ? "" : this.dnFormat)
        .setEuFormat((this.euFormat == null) ? "" : this.euFormat)
        .setPacketId(this.packetId);
        
        if (sclk != null) {
        	member.setSclk(this.sclk.buildSclk());
        }
        
        if (scet != null) {
        	member.setScet(this.scet.buildAccurateDateTime());
        }
        
        if (alarmValueSet != null) {
        	member.setAlarmValueSet(this.alarmValueSet.getProto());
        }
        
        if (hasEu) {
        	member.setEu(eu);
        }
        
        /*
         * Prepare DN and EU builders
         */
        final Proto3Dn.Builder dn = Proto3Dn.newBuilder();
        
        /*
         * Set DN Type
         */
        dn.setType(Proto3DnType.valueOf("DN_TYPE_" + type.name()));
        
        /*
         * Set appropriate DN Value based upon DN Type
         */
        switch (this.type ) {
            case ASCII:
                dn.setString(this.dn.toString().trim());
                break;
            case BOOLEAN:
                if (this.dn instanceof Byte) {
                    dn.setBool(((Byte)this.dn) != 0);
                }
                else if (this.dn instanceof Integer) {
                    dn.setBool(((Integer)this.dn) != 0);
                }
                else if (this.dn instanceof Long) {
                    dn.setBool(((Long)this.dn) != 0);
                }
                else if (this.dn instanceof String) {
                    dn.setBool(Boolean.valueOf((String)this.dn));
                }
                else if (null == this.dn) {
                    dn.setBool(false);
                }
                else {
                    throw new RuntimeException("Illegal value encountered for \"" + this.type + "\": (" + this.dn.getClass() + ")" + this.dn);
                }
                break;
            case STATUS:
            case SIGNED_INT:
                if (this.dn instanceof Byte) {
                    dn.setInt(((Byte)this.dn).intValue());
                }
                else if (this.dn instanceof Short) {
                    dn.setInt(((Short)this.dn).intValue());
                }
                else if (this.dn instanceof Integer) {
                    dn.setInt(((Integer)this.dn).intValue());
                }
                else if (this.dn instanceof Long) {
                    dn.setLong(((Long)this.dn).longValue());
                }
                else {
                    throw new RuntimeException("Illegal value encountered for \"" + this.type + "\": (" + this.dn.getClass() + ")" + this.dn);
                }
                break;
            case UNSIGNED_INT:
            case DIGITAL:
            case TIME:
                if (this.dn instanceof Short) {
                    dn.setUint((Short) this.dn);
                } else if (this.dn instanceof Integer) {
                    dn.setUint((Integer) this.dn);
                }
                else if (this.dn instanceof Long) {
                    dn.setUlong((Long) this.dn);
                }
                else {
                    throw new RuntimeException("Illegal value encountered for \"" + this.type + "\": (" + this.dn.getClass() + ")" + this.dn);
                }
                break;
            case FLOAT:
                if (this.dn instanceof Float) {
                    dn.setFloat((Float) this.dn);
                }
                else if (this.dn instanceof Double) {
                    dn.setDouble((Double) this.dn);
                }
                else {
                    throw new RuntimeException("Illegal value encountered for \"" + this.type + "\": (" + this.dn.getClass() + ")" + this.dn);
                }
                break;
            case UNKNOWN:
            default:
                break;
            
        }
        member.setDn(dn);

        /*
         * Build, and add to list
         */
        return member.build();
    }

   /**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("EhaChannelGroupMember [eventTime=");
		builder.append(eventTime);
		builder.append(", channelId=");
		builder.append(channelId);
		builder.append(", sclk=");
		builder.append(sclk);
		builder.append(", dn=");
		builder.append(dn);
		builder.append(", eu=");
		builder.append(eu);
		builder.append(", alarms=");
		builder.append(alarmValueSet);
		//builder.append(", dssId=");
		//builder.append(dssId);
		builder.append(", status=");
		builder.append(status);
		builder.append(", type=");
		builder.append(type);
		builder.append("]");
		return builder.toString();
	}
}
