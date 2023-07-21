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
package jpl.gds.globallad.data;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.protobuf.ByteString;

import jpl.gds.dictionary.api.alarm.AlarmLevel;
import jpl.gds.dictionary.api.channel.ChannelType;
import jpl.gds.eha.api.channel.IAlarmValue;
import jpl.gds.eha.api.channel.IAlarmValueSet;
import jpl.gds.globallad.data.json.ByteArrayDeSerializer;
import jpl.gds.globallad.data.json.ByteArraySerializer;
import jpl.gds.globallad.data.json.views.GlobalLadSerializationViews;
import jpl.gds.serialization.globallad.data.Proto3EhaGladData;
import jpl.gds.serialization.globallad.data.Proto3GlobalLadTransport;
import jpl.gds.shared.gdr.GDR;

/**
 * The EHA channel type implementation.  
 */
public class EhaGlobalLadData extends AbstractGlobalLadData {
	private static final Map<Byte, ChannelType> typeMap;
	private static final Map<ChannelType, Byte> reverseTypeMap;
	
	/**
	 * Using the ordinal of the enum instead of an arbitrary number.
	 */
	static {
		typeMap = new HashMap<Byte, ChannelType>();
		reverseTypeMap = new HashMap<ChannelType, Byte>();
		
		for (ChannelType channelType : ChannelType.values()) {
			typeMap.put((byte) channelType.ordinal(), channelType);
			reverseTypeMap.put(channelType, (byte) channelType.ordinal());
		}
	}

	@JsonView(GlobalLadSerializationViews.GlobalView.class)
	private boolean isRealTime;
	@JsonView(GlobalLadSerializationViews.SerializationView.class)
	private boolean isHeader;
	@JsonView(GlobalLadSerializationViews.SerializationView.class)
	private boolean isMonitor;
	@JsonView(GlobalLadSerializationViews.SerializationView.class)
	private boolean isSse;
	@JsonView(GlobalLadSerializationViews.SerializationView.class)
	private boolean isFsw;

	@JsonView(GlobalLadSerializationViews.GlobalView.class)
	private String channelId;
	
	@JsonView(GlobalLadSerializationViews.SerializationView.class)
	private byte dnType;
	
	private IAlarmValueSet alarmSet;

	@JsonSerialize(using = ByteArraySerializer.class)
	@JsonDeserialize(using = ByteArrayDeSerializer.class)
	@JsonView(GlobalLadSerializationViews.SerializationView.class)
	private byte[] dnRaw;

	@JsonSerialize(using = ByteArraySerializer.class)
	@JsonDeserialize(using = ByteArrayDeSerializer.class)
	@JsonView(GlobalLadSerializationViews.SerializationView.class)
	private byte[] euRaw;
	
	@JsonView(GlobalLadSerializationViews.GlobalView.class)
	private String status;
	
	
	/**
	 * @param proto
	 * @throws GlobalLadDataException
	 */
	public EhaGlobalLadData(Proto3EhaGladData proto) throws GlobalLadDataException {
		super(proto.getBase());
		
		this.channelId = proto.getChannelId();
		this.dnType = (byte) proto.getDnType();
		this.dnRaw = proto.getDnBytes().toByteArray();
		this.euRaw = proto.getEuBytes().toByteArray();
		this.status = proto.getStatus();

		this.alarmSet = new GladAlarmValueSet(proto.getAlarms());
		
		this.isFsw = proto.getIsFsw();
		this.isSse = proto.getIsSse();
		this.isHeader = proto.getIsHeader();
		this.isRealTime = proto.getIsRealtime();
		this.isMonitor = proto.getIsMonitor();
	}

	/**
	 * Base constructor.
	 */
	public EhaGlobalLadData() {
		super();
		isRealTime = false;
		isHeader = false;
		isMonitor = false;
		isSse = false;
		isFsw = false;
		channelId = null;
		dnType = -1;
		dnRaw = null;
		euRaw = null;
	}
	
	/**
	 * When using this constructor the user data type is not set because the UDT mapping has been decoupled with the data objects and has been added to the 
	 * data factories.  A call to setUserDataType must be done with the UDT or the generateDataWord method will throw because the UDT is not known.  
	 * 
	 * @param chanType
	 * @param channelId
	 * @param dn
	 * @param dnAlarmSet
	 * @param eu
	 * @param euAlarmSet
	 * @param isRealTime
	 * @param isHeader
	 * @param isMonitor
	 * @param isSse
	 * @param isFsw
	 * @param sclkCoarse
	 * @param sclkFine
	 * @param ertMilliseconds
	 * @param ertNanoseconds
	 * @param scetMilliseconds
	 * @param scetNanoseconds
	 * @param sessionNumber
	 * @param scid
	 * @param venue
	 * @param dssId
	 * @param vcid
	 * @param host
	 * @param status
	 * @throws GlobalLadDataException
	 */
	public EhaGlobalLadData(
			ChannelType chanType, 
			String channelId, 
			Object dn, 
			Double eu, 
			IAlarmValueSet alarmSet,
			boolean isRealTime, 
			boolean isHeader, 
			boolean isMonitor, 
			boolean isSse, 
			boolean isFsw, 
			long sclkCoarse, 
			long sclkFine, 
			long ertMilliseconds, 
			long ertNanoseconds, 
			long scetMilliseconds, 
			long scetNanoseconds, 
			long sessionNumber, 
			int scid,
			String venue, 
			byte dssId,
			byte vcid, 
			String host, 
			String status) throws GlobalLadDataException {
		super(sclkCoarse, sclkFine, ertMilliseconds, ertNanoseconds, scetMilliseconds, scetNanoseconds, 
				sessionNumber, scid, venue, dssId, vcid, host);
		
		this.channelId = channelId;
		this.isRealTime = isRealTime;
		this.isHeader = isHeader;
		this.isMonitor = isMonitor;
		this.isSse = isSse;
		this.isFsw = isFsw;
		
		this.alarmSet = alarmSet;
		
		/**
		 * Status is valid for status or boolean.
		 */
		this.status = chanType.equals(ChannelType.STATUS) || chanType.equals(ChannelType.BOOLEAN) ? status : "";
		
		if (eu == null) {
			this.euRaw = new byte[0];
		} else {
			this.euRaw = new byte[8];
			GDR.set_double(this.euRaw, 0,  eu);
		}

		setDnType(chanType);
		setDn(chanType, dn);
	}

	/**
	 * @return this in protobuffer form.
	 */
	public Proto3EhaGladData serializeEha() {
		Proto3EhaGladData eha = Proto3EhaGladData.newBuilder()
				.setBase(serializeBase())
				.setChannelId(channelId)
				.setDnType(dnType)
				.setDnBytes(ByteString.copyFrom(dnRaw))
				.setEuBytes(ByteString.copyFrom(euRaw))
				.setStatus(status)
				.setAlarms(alarmSet.getProto())
				.setIsRealtime(isRealTime)
				.setIsHeader(isHeader)
				.setIsMonitor(isMonitor)
				.setIsSse(isSse)
				.setIsFsw(isFsw)
				.build();
		
		return eha;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.globallad.data.IGlobalLADData#serialize()
	 */
	@Override
	@JsonIgnore
	public Proto3GlobalLadTransport serialize() {
		Proto3GlobalLadTransport transport = Proto3GlobalLadTransport.newBuilder()
				.setEha(serializeEha())
				.build();
		
		return transport;
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.IGlobalLADData#getIdentifier()
	 */
	@Override
	@JsonIgnore
	public Object getIdentifier() {
		return channelId;
	}

	/**
	 * @return the isRealTime
	 */
	public boolean isRealTime() {
		return isRealTime;
	}

	/**
	 * @return the isHeader
	 */
	public boolean isHeader() {
		return isHeader;
	}

	/**
	 * @return the isMonitor
	 */
	public boolean isMonitor() {
		return isMonitor;
	}

	/**
	 * @return the isSse
	 */
	public boolean isSse() {
		return isSse;
	}

	/**
	 * @return the isFsw
	 */
	public boolean isFsw() {
		return isFsw;
	}

	/**
	 * @return the channelId
	 */
	public String getChannelId() {
		return channelId;
	}

	/**
	 * @return the channelType
	 */
	@JsonView(GlobalLadSerializationViews.RestRequestView.class)
	@JsonProperty("channelType")
	public ChannelType getChannelType() {
		return typeMap.get(this.dnType);
	}

	/**
	 * @return the dnType
	 */
	public byte getDnType() {
		return dnType;
	}

	/**
	 * @return the dn
	 */
	public byte[] getDnRaw() {
		return dnRaw;
	}

	/**
	 * @return the eu
	 */
	public byte[] getEuRaw() {
		return euRaw;
	}

	/**
	 * Converts dn to a byte[] and sets dnRaw based on type.
	 * 
	 * @param type The channel type of this EHA.
	 * @param dn the dn as an object.
	 * @throws GlobalLadDataException
	 */
	@JsonIgnore
	public void setDn(ChannelType type, Object dn) throws GlobalLadDataException {
		String message = null;
		switch(type) {
		case ASCII:
			if (dn instanceof String) {
				this.dnRaw = ((String) dn).getBytes();
			} else {
				message = String.format("Channel type is %s but dn object is a " + dn.getClass(), type);
			}
			break;
		case BOOLEAN:
			if (dn instanceof Number) {
				/**
				 * Boolean should be 0 or 1 but it could be any number conceivably.  To ensure 
				 * bitwise AND with 0x1 to make sure.
				 */
				this.dnRaw = new byte[]{(byte) (((Number) dn).byteValue() & 0x1) };
			} else {
				message = String.format("Channel type is %s but dn object is a " + dn.getClass(), type);
			}
			break;
		case FLOAT:
			if (dn instanceof Float) {
				this.dnRaw = new byte[4];
				GDR.set_float(this.dnRaw, 0, (Float) dn);
			} else if (dn instanceof Double) {
				this.dnRaw = new byte[8];
				GDR.set_double(this.dnRaw, 0, (Double) dn);
			} else {
				message = String.format("Channel type is %s but dn object is a " + dn.getClass(), type);
			}
			break;
		case STATUS:
		case SIGNED_INT:
			if (dn instanceof Byte) {
				this.dnRaw = new byte[1];
				GDR.set_i8(this.dnRaw, 0, (Byte) dn);
			} else if (dn instanceof Short) {
				this.dnRaw = new byte[2];
				GDR.set_i16(dnRaw, 0, (Short) dn);
			} else if (dn instanceof Integer) {
				this.dnRaw = new byte[4];
				GDR.set_i32(this.dnRaw, 0, (Integer) dn);
			} else if (dn instanceof Long) {
				this.dnRaw = new byte[8];
				GDR.set_i64(this.dnRaw, 0, (Long) dn);
			} else {
				message = String.format("Channel type is %s but dn object is a " + dn.getClass(), type);
			}
			break;
		case TIME:
		case DIGITAL:
		case UNSIGNED_INT:
			if (dn instanceof Byte) {
				this.dnRaw = new byte[1];
				GDR.set_u8(this.dnRaw, 0, (Byte) dn);
			} else if (dn instanceof Short) {
				this.dnRaw = new byte[2];
				GDR.set_u16(dnRaw, 0, (Short) dn);
			} else if (dn instanceof Integer) {
				this.dnRaw = new byte[4];
				GDR.set_u32(this.dnRaw, 0, (Integer) dn);
			} else if (dn instanceof Long) {
				this.dnRaw = new byte[8];
				GDR.set_u64(this.dnRaw, 0, (Long) dn);
			} else {
				message = String.format("Channel type is %s but dn object is a " + dn.getClass(), type);
			}
			break;
		case UNKNOWN:
		default:
			message = String.format("Channel type is %s but dn object is a " + dn.getClass(), type);
		}
		
		if (message != null) {
			throw new GlobalLadDataException(message);
		}
	}
	/**
	 * @return the dn based on the channel type. Converts the value from the raw bytes every time this is called.
	 */
	@JsonView(GlobalLadSerializationViews.RestRequestView.class)
	public Object getDn() {
		ChannelType type = typeMap.get(getDnType());
		switch(type) {
		case ASCII:
			return getStringValue(dnRaw);
		case FLOAT:
			if (dnRaw.length > 4) {
				return getDoubleValue(dnRaw);
			} else { 
				return getFloatValue(dnRaw);
			}
		case STATUS:
		case SIGNED_INT:
			if (dnRaw.length > 4) {
				return getLongValue(dnRaw, true);
			} else  {
				return getSignedIntValue(dnRaw);
			}
		case TIME:
		case DIGITAL:
		case UNSIGNED_INT:
		case BOOLEAN:
			/**
			 * Boolean value should return the value as a unsigned int.  The status will be True or False.
			 * Need to check the size and convert accordingly.
			 */
			
			if (dnRaw.length > 4) {
				return getLongValue(dnRaw, false);
			} else {
				return getUnsignedIntValue(dnRaw);
			}
		case UNKNOWN:
		default:
			return dnRaw;
		}
	}

	/**
	 * @return True if euRaw is not null or empty.
	 */
	public boolean hasEu() {
		return euRaw != null && euRaw.length > 0;
	}
	/**
	 * @return the eu.  If there is no EU for this channel returns null.
	 */
	@JsonView(GlobalLadSerializationViews.RestRequestView.class)
	public Double getEu() {
		if (hasEu()) {
			return getDoubleValue(euRaw);
		} else {
			return null;
		}
	}
	
	/**
	 * @return the status.  If no status will be an empty string.
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * Gets the worst alarm set. 
	 * @param isOnEu
	 * @return 
	 */
	private IAlarmValueSet getWorstAlarmList(final boolean isOnEu) {
		if (this.alarmSet == null || !alarmSet.inAlarm()) {
        	return new GladAlarmValueSet();
		} else {

			final IAlarmValueSet yellowSet = alarmSet.getAlarmSet(AlarmLevel.YELLOW, isOnEu);
			final IAlarmValueSet redSet = alarmSet.getAlarmSet(AlarmLevel.RED, isOnEu);
			if (redSet.inAlarm()) {
				return redSet;
			} else if (yellowSet.inAlarm()) {
				return yellowSet;
			} else  {
				return new GladAlarmValueSet();
			}
		}
	}
	
	/**
	 * Finds the worst alarm based on the isOnEu flag and returns the state string.  If nothing is in 
	 * alarm returns empty string.
	 * @param isOnEu
	 * @return  alarm state string.
	 */
	private String computeAlarmState(final boolean isOnEu) {
        final IAlarmValueSet alarms = getWorstAlarmList(isOnEu);
        
        return alarms.inAlarm() ? getAlarmStateString(alarms.getAlarmValueList()) : "";
	}

	/**
	 * @param alarmSet
	 * @return
	 */
	private String getAlarmStateString(final List<IAlarmValue> alarmSet) {
		final StringBuffer buf = new StringBuffer();
		for (int index = 0; index < alarmSet.size(); index++) {
			buf.append(alarmSet.get(index).getState());
			if (index != alarmSet.size() - 1) {
				buf.append(":");
			}
		}
		return buf.toString();
	}

	
	private AlarmLevel computeAlarmLevel(final boolean isOnEu) {
	    AlarmLevel result;
        if (alarmSet == null || !alarmSet.inAlarm()) {
        	result = AlarmLevel.NONE;
        } else {
        	final IAlarmValueSet yellowSet = alarmSet.getAlarmSet(AlarmLevel.YELLOW, isOnEu);
        	final IAlarmValueSet redSet = alarmSet.getAlarmSet(AlarmLevel.RED, isOnEu);
        	if (redSet.inAlarm()) {
        		result = AlarmLevel.RED;
        	} else if (yellowSet.inAlarm()) {
        		result = AlarmLevel.YELLOW;
        	} else {
        		result = AlarmLevel.NONE;
        	}
        }

        return result;
	}

	
	/**
	 * @return the dnAlarmState
	 */
	@JsonView(GlobalLadSerializationViews.RestRequestView.class)
	public String getDnAlarmState() {
		return computeAlarmState(false);
	}


	/**
	 * @return the euAlarmState
	 */
	@JsonView(GlobalLadSerializationViews.RestRequestView.class)
	public String getEuAlarmState() {
		return computeAlarmState(true);
	}

	/**
	 * @return the dnAlarmLevel
	 */
	@JsonView(GlobalLadSerializationViews.RestRequestView.class)
	public AlarmLevel getDnAlarmLevel() {
		return computeAlarmLevel(false);
	}

	/**
	 * @return the euAlarmLevel
	 */
	@JsonView(GlobalLadSerializationViews.RestRequestView.class)
	public AlarmLevel getEuAlarmLevel() {
		return computeAlarmLevel(true);
	}
	
	@JsonIgnore
	public IAlarmValueSet getAlarmValueSet()	 {
		return this.alarmSet;
	}


	/**
	 * @param isRealTime the isRealTime to set
	 */
	public void setRealTime(boolean isRealTime) {
		this.isRealTime = isRealTime;
	}

	/**
	 * @param isHeader the isHeader to set
	 */
	public void setHeader(boolean isHeader) {
		this.isHeader = isHeader;
	}

	/**
	 * @param isMonitor the isMonitor to set
	 */
	public void setMonitor(boolean isMonitor) {
		this.isMonitor = isMonitor;
	}

	/**
	 * @param isSse the isSse to set
	 */
	public void setSse(boolean isSse) {
		this.isSse = isSse;
	}

	/**
	 * @param isFsw the isFsw to set
	 */
	public void setFsw(boolean isFsw) {
		this.isFsw = isFsw;
	}

	/**
	 * @param channelId the channelId to set
	 */
	public void setChannelId(String channelId) {
		this.channelId = channelId;
	}

	/**
	 * @param channelType Looks up the byte mapping for channelType and sets dnType.
	 */
	@JsonIgnore
	public void setDnType(ChannelType channelType) {
		this.dnType = reverseTypeMap.get(channelType);
	}
	
	/**
	 * @param dnType the dnType to set
	 */
	public void setDnType(byte dnType) {
		this.dnType = dnType;
	}

	/**
	 * @param dn the dn to set
	 */
	public void setDnRaw(byte[] dn) {
		this.dnRaw = dn;
	}

	/**
	 * @param eu the eu to set
	 */
	public void setEuRaw(byte[] eu) {
		this.euRaw = eu;
	}

	/**
	 * @param status the status to set
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.AbstractGlobalLadData#toString()
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("EhaGlobalLadData [isRealTime=");
		builder.append(isRealTime);
		builder.append(", insertNumber=");
		builder.append(insertNumber);
		builder.append(", isHeader=");
		builder.append(isHeader);
		builder.append(", isMonitor=");
		builder.append(isMonitor);
		builder.append(", isSse=");
		builder.append(isSse);
		builder.append(", isFsw=");
		builder.append(isFsw);
		builder.append(", channelId=");
		builder.append(channelId);
		builder.append(", dnType=");
		builder.append(dnType);
		builder.append(", dnRaw=");
		builder.append(Arrays.toString(dnRaw));
		builder.append(", euRaw=");
		builder.append(Arrays.toString(euRaw));
		builder.append(", status=");
		builder.append(status);
		builder.append(", eventTime=");
		builder.append(eventTime);
		builder.append(", sclkCoarse=");
		builder.append(sclkCoarse);
		builder.append(", sclkFine=");
		builder.append(sclkFine);
		builder.append(", ertMilliseconds=");
		builder.append(ertMilliseconds);
		builder.append(", ertNanoseconds=");
		builder.append(ertNanoseconds);
		builder.append(", scetMilliseconds=");
		builder.append(scetMilliseconds);
		builder.append(", scetNanoseconds=");
		builder.append(scetNanoseconds);
		builder.append(", venue=");
		builder.append(venue);
		builder.append(", sessionNumber=");
		builder.append(sessionNumber);
		builder.append(", scid=");
		builder.append(scid);
		builder.append(", dssId=");
		builder.append(dssId);
		builder.append(", vcid=");
		builder.append(vcid);
		builder.append(", host=");
		builder.append(host);
		builder.append(", userDataType=");
		builder.append(userDataType);
		builder.append(", primaryTime=");
		builder.append(primaryTime);
		builder.append("]");
		return builder.toString();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result
				+ ((channelId == null) ? 0 : channelId.hashCode());
		result = prime * result + Arrays.hashCode(dnRaw);
		result = prime * result + dnType;
		result = prime * result + Arrays.hashCode(euRaw);
		result = prime * result + (isFsw ? 1231 : 1237);
		result = prime * result + (isHeader ? 1231 : 1237);
		result = prime * result + (isMonitor ? 1231 : 1237);
		result = prime * result + (isRealTime ? 1231 : 1237);
		result = prime * result + (isSse ? 1231 : 1237);
		result = prime * result + ((status == null) ? 0 : status.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (!(obj instanceof EhaGlobalLadData)) {
			return false;
		}
		
		EhaGlobalLadData other = (EhaGlobalLadData) obj;
		

		if (channelId == null) {
			if (other.getChannelId() != null) {
				return false;
			}
		} else if (!channelId.equals(other.getChannelId())) {
			return false;
		}
		if (!Arrays.equals(dnRaw, other.getDnRaw())) {
			return false;
		}
		if (dnType != other.getDnType()) {
			return false;
		}
		if (!Arrays.equals(euRaw, other.getEuRaw())) {
			return false;
		}
		if (isFsw != other.isFsw()) {
			return false;
		}
		if (isHeader != other.isHeader()) {
			return false;
		}
		if (isMonitor != other.isMonitor()) {
			return false;
		}
		if (isRealTime != other.isRealTime()) {
			return false;
		}
		if (isSse != other.isSse()) {
			return false;
		}
		if (status == null || status.isEmpty()) {
			if (other.getStatus() != null && ! other.getStatus().isEmpty()) {
				return false;
			}
		} else if (!status.equals(other.getStatus())) {
			return false;
		}
		return true;
	}
}