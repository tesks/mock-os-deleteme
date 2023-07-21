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

import java.nio.ByteBuffer;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;

import jpl.gds.globallad.data.container.IGlobalLadDeltaQueryable.DeltaQueryStatus;
import jpl.gds.globallad.data.json.views.GlobalLadSerializationViews;
import jpl.gds.serialization.globallad.data.Proto3BaseGladData;
import jpl.gds.serialization.primitives.time.Proto3Adt;
import jpl.gds.serialization.primitives.time.Proto3Sclk;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ILocalSolarTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.LocalSolarTime;
import jpl.gds.shared.time.Sclk;
import jpl.gds.shared.time.SclkFmt;
import jpl.gds.shared.time.TimeProperties;


/**
 * Given the byte buffer will parse out the common fields.  It will then call the abstract method parseUseData with the 
 * byte buffer position pointer at the first byte of the user data.  Is expected that the input byte buffer has the current byte pointer set 
 * the first bit of the sclk source field.
 */
public abstract class AbstractGlobalLadData implements IByteBufferManipulator, IGlobalLADData {
	/*
	 * The start and end sequences for a chill down data message.  
	 */
	public static final byte[] GLAD_PACKET_START_WORD;
	
	static {
		GLAD_PACKET_START_WORD = new byte[4];
		GDR.set_i32(GLAD_PACKET_START_WORD, 0, 0x1acffc1d);
	}

	protected static final double MS_DIVISOR = new Double(1000);
	
	protected long eventTime;
	@JsonView(GlobalLadSerializationViews.SerializationView.class) 
	protected long sclkCoarse;
	@JsonView(GlobalLadSerializationViews.SerializationView.class) 
	protected long sclkFine;
	@JsonView(GlobalLadSerializationViews.SerializationView.class) 
	protected long ertMilliseconds;
	@JsonView(GlobalLadSerializationViews.SerializationView.class) 
	protected long ertNanoseconds;
	@JsonView(GlobalLadSerializationViews.SerializationView.class) 
	protected long scetMilliseconds;
	@JsonView(GlobalLadSerializationViews.SerializationView.class) 
	protected long scetNanoseconds;
	
	@JsonView(GlobalLadSerializationViews.GlobalView.class) 
	protected String venue;
	@JsonView(GlobalLadSerializationViews.GlobalView.class) 
	protected long sessionNumber;
	@JsonView(GlobalLadSerializationViews.GlobalView.class) 
	protected int scid;
	@JsonView(GlobalLadSerializationViews.GlobalView.class) 
	protected byte dssId;
	@JsonView(GlobalLadSerializationViews.GlobalView.class) 
	protected byte vcid;
	@JsonView(GlobalLadSerializationViews.GlobalView.class) 
	protected String host;
	
	@JsonView(GlobalLadSerializationViews.SerializationView.class) 
	protected byte userDataType = Byte.MAX_VALUE;
	
	/**
	 * Set when the object is inserted into a global lad data container.  
	 * Including insert number in all output views.
	 */
	@JsonView(GlobalLadSerializationViews.GlobalView.class) 
	protected long insertNumber = -1;
	
	protected GlobalLadPrimaryTime primaryTime = GlobalLadPrimaryTime.ERT;
	
	protected static final SclkFmt sclkFmt = TimeProperties.getInstance().getSclkFormatter();

	/**
	 * Constructor used for marshalling and should not be used. 
	 */
	public AbstractGlobalLadData() {
		eventTime = -1;
		sclkCoarse = -1;
		sclkFine = -1;
		ertMilliseconds = -1;
		ertNanoseconds = -1;
		scetMilliseconds = -1;
		scetNanoseconds = -1;
		sessionNumber = -1;
		scid = -1;
		venue = null;
		dssId = -1;
		vcid = -1;
		host = null;
	}


	/**
	 * Initializes this with base data proto buffer
	 * 
	 * @param proto base data
	 * @throws GlobalLadDataException
	 */
	public AbstractGlobalLadData(Proto3BaseGladData proto) throws GlobalLadDataException {
		this.userDataType = (byte) proto.getUserDataType();
		this.dssId = (byte) proto.getDssId();
		this.vcid = (byte) proto.getVcid();
		this.scid = proto.getScid();
		this.sessionNumber = proto.getSessionNumber();
		
		this.host = proto.getHost().intern();
		this.venue = proto.getVenue().intern();

		switch(proto.getHasEventTimeCase()) {
		case EVENTTIME:
			this.eventTime = proto.getEventTime();
			break;
		case HASEVENTTIME_NOT_SET:
			break;
		}
		
		switch(proto.getHasSclkCase()) {
		case SCLK:
			this.sclkCoarse = proto.getSclk().getSeconds();
			this.sclkFine = proto.getSclk().getNanos();
			break;
		case HASSCLK_NOT_SET:
			break;
		}

		switch(proto.getHasErtCase()) {
		case ERT:
			this.ertMilliseconds = proto.getErt().getMilliseconds();
			this.ertNanoseconds = proto.getErt().getNanoseconds();
			break;
		case HASERT_NOT_SET:
			break;
		}
		
		switch(proto.getHasScetCase()) {
		case SCET:
			this.scetMilliseconds = proto.getScet().getMilliseconds();
			this.scetNanoseconds = proto.getScet().getNanoseconds();
			break;
		case HASSCET_NOT_SET:
			break;
		}
	}
		
	
	/**
	 * Sets the internal event time to the current time.
	 * 
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
	 * @param userDateType
	 */
	public AbstractGlobalLadData(
			final long sclkCoarse, 
			final long sclkFine, 
			final long ertMilliseconds,
			final long ertNanoseconds, 
			final long scetMilliseconds, 
			final long scetNanoseconds, 
			final long sessionNumber, 
			final int scid,
			final String venue, 
			final byte dssId,
			final byte vcid, 
			final String host) {
		super();
		this.eventTime = System.currentTimeMillis();
		this.sclkCoarse = sclkCoarse;
		this.sclkFine = sclkFine;
		this.ertMilliseconds = ertMilliseconds;
		this.ertNanoseconds = ertNanoseconds;
		this.scetMilliseconds = scetMilliseconds;
		this.scetNanoseconds = scetNanoseconds;
		this.sessionNumber = sessionNumber;
		this.scid = scid;
		this.venue = venue;
		this.dssId = dssId;
		this.vcid = vcid;
		this.host = host;
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.IGlobalLADData#getPrimaryTimeType()
	 */
	@Override
	public GlobalLadPrimaryTime getPrimaryTimeType() {
		return primaryTime;
	}

	/**
	 * @param primaryTime the primaryTime to set
	 */
	public void setPrimaryTime(final GlobalLadPrimaryTime primaryTime) {
		this.primaryTime = primaryTime;
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.IGlobalLADData#getPrimaryTimeCoarse()
	 */
	@JsonIgnore
	@Override
	public long getPrimaryMilliseconds() {
		switch(getPrimaryTimeType()) {
		case ERT:
			return getErtMilliseconds();
		case EVENT:
			return getEventTime();
		case SCET: 
		case LST:
		case SCLK:
		default:
			return getScetMilliseconds();
		}
	}
	
	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.IGlobalLADData#getPrimaryTimeFine()
	 */
	@JsonIgnore
	@Override
	public long getPrimaryTimeNanoseconds() {
		switch(getPrimaryTimeType()) {
		case ERT:
			return getErtNanoseconds();
		case SCET:
		case SCLK:
		case LST:
			return getScetNanoseconds();
		case EVENT:
			// Event time does not have coarse time, it is a millisecond deal. 
		default:
			return 0L;
		}		
	}
	
	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.IGlobalLADData#getEventTime()
	 */
	@Override
	@JsonProperty("eventTimeRaw")
	@JsonView(GlobalLadSerializationViews.SerializationView.class) 
	public long getEventTime() {
		return eventTime;
	}
	
	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.IGlobalLADData#getCreateTimeDelta()
	 */
	@Override
	public long getCreateTimeDelta() {
		return System.currentTimeMillis() - eventTime;
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.IGlobalLADData#getSclkCoarse()
	 */
	@Override
	public long getSclkCoarse() {
		return sclkCoarse;
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.IGlobalLADData#getSclkFine()
	 */
	@Override
	public long getSclkFine() {
		return sclkFine;
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.IGlobalLADData#getErtMilliseconds()
	 */
	@Override
	public long getErtMilliseconds() {
		return ertMilliseconds;
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.IGlobalLADData#getErtNanoseconds()
	 */
	@Override
	public long getErtNanoseconds() {
		return ertNanoseconds;
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.IGlobalLADData#getScetMilliseconds()
	 */
	@Override
	public long getScetMilliseconds() {
		return scetMilliseconds;
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.IGlobalLADData#getScetNanoseconds()
	 */
	@Override
	public long getScetNanoseconds() {
		return scetNanoseconds;
	}

	/**
	 * Including event time and insert number in all outputs, so it will be part of the global view.
	 */
	@JsonView(GlobalLadSerializationViews.RestRequestView.class)
	@JsonProperty("eventTime")
	public String getEventTimeStr() {
		final IAccurateDateTime et = new AccurateDateTime(eventTime, 0);
		return et.getFormattedErt(true);
	}
	
	/**
	 * Converts the sclk coarse and fine to a sclk string.  This is a decimal
	 * sub seconds, not ticks.
	 * 
	 * @return - Converted sclk value as a decimal string.
	 */
	@JsonView(GlobalLadSerializationViews.RestRequestView.class)
	public String getSclk() {
		final ISclk sclk = new Sclk(getSclkCoarse(), getSclkFine());
		/**
		 * Was getting the decimal format every time and not
		 * honoring the setting in the time config.  Use sclkFmt.
		 */
		return sclkFmt.fmt(sclk);
	}

	/*
	 * Adding LST to the output.
	 */
	
	/**
	 * Converts the scet to lst string. If this is a mission that does not use
	 * LST, or scet is before SCET0 the LST will be the all zero LST time
	 * string.
	 * 
	 * @return - Converted scet to LST.
	 */
	@JsonView(GlobalLadSerializationViews.RestRequestView.class)
	public String getLst() {
		final IAccurateDateTime scet = new AccurateDateTime(getScetMilliseconds(), getScetNanoseconds());
		final ILocalSolarTime lst = new LocalSolarTime(scid, scet);
		return lst.getFormattedSol(true);
	}	

	/**
	 * @return - Formated ERT string.
	 */
	@JsonView(GlobalLadSerializationViews.RestRequestView.class)
	public String getErt() {
		final IAccurateDateTime ert = new AccurateDateTime(getErtMilliseconds(), getErtNanoseconds());
		return ert.getFormattedErt(true);
	}
	
	/**
	 * @return - Formated SCET string.
	 */
	@JsonView(GlobalLadSerializationViews.RestRequestView.class)
	public String getScet() {
		final IAccurateDateTime scet = new AccurateDateTime(getScetMilliseconds(), getScetNanoseconds());
		return scet.getFormattedScet(true);
	}
	
	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.IGlobalLADData#getUserDataType()
	 */
	@Override
	public byte getUserDataType() {
		return this.userDataType;
	}
	
	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.IGlobalLADData#setUserDataType(byte)
	 */
	@Override
	public void setUserDataType(final byte userDataType) {
		this.userDataType = userDataType;
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.IGlobalLADData#getSessionNumber()
	 */
	@Override
	public long getSessionNumber() {
		return sessionNumber;
	}

	
	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.IGlobalLADData#getScid()
	 */
	@Override
	public int getScid() {
		return scid;
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.IGlobalLADData#getVenue()
	 */
	@Override
	public String getVenue() {
		return venue;
	}
	
	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.IGlobalLADData#getDssId()
	 */
	@Override
	public byte getDssId() {
		return dssId;
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.IGlobalLADData#getVcid()
	 */
	@Override
	public byte getVcid() {
		return vcid;
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.IGlobalLADData#getHost()
	 */
	@Override
	public String getHost() {
		return host;
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.IGlobalLADData#insertNumber()
	 */
	@Override
	public long getInsertNumber() {
		return this.insertNumber;
	}

	/**
	 * @param eventTime the eventTime to set.
	 */
	@JsonProperty("eventTimeRaw")
	public void setEventTime(final long eventTime) {
		this.eventTime = eventTime;
	}
	
	/**
	 * @param sclkCoarse the sclkCoarse to set
	 */
	public void setSclkCoarse(final long sclkCoarse) {
		this.sclkCoarse = sclkCoarse;
	}

	/**
	 * @param sclkFine the sclkFine to set
	 */
	public void setSclkFine(final long sclkFine) {
		this.sclkFine = sclkFine;
	}

	/**
	 * @param ertMilliseconds
	 */
	public void setErtMilliseconds(final long ertMilliseconds) {
		this.ertMilliseconds = ertMilliseconds;
	}

	/**
	 * @param ertNanoseconds the ertFine to set
	 */
	public void setErtNanoseconds(final long ertNanoseconds) {
		this.ertNanoseconds = ertNanoseconds;
	}

	/**
	 * @param scetMilliseconds 
	 */
	public void setScetMilliseconds(final long scetMilliseconds) {
		this.scetMilliseconds = scetMilliseconds;
	}

	/**
	 * @param scetNanoseconds 
	 */
	public void setScetNanoseconds(final long scetNanoseconds) {
		this.scetNanoseconds = scetNanoseconds;
	}

	/**
	 * @param sessionNumber the sessionNumber to set
	 */
	public void setSessionNumber(final long sessionNumber) {
		this.sessionNumber = sessionNumber;
	}

	/**
	 * @param venue the venue to set
	 */
	public void setVenue(final String venue) {
		this.venue = venue;
	}

	/**
	 * @param dssId the dssId to set
	 */
	public void setDssId(final byte dssId) {
		this.dssId = dssId;
	}
	
	/**
	 * @param scid the scid to set
	 */
	public void setScid(final int scid) {
		this.scid = scid;
	}

	/**
	 * @param vcid the vcid to set
	 */
	public void setVcid(final byte vcid) {
		this.vcid = vcid;
	}

	/**
	 * @param host the host to set
	 */
	public void setHost(final String host) {
		this.host = host;
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.IGlobalLADData#setInsertNumber(long)
	 */
	@Override
	public void setInsertNumber(final long insertNumber) {
		this.insertNumber = insertNumber;
	}

	/**
	 * These methods are used for serialization.  It creates a way that set verification can be included in the output by 
	 * using mix-in classes.  You must NOT put any annotations on these initially because for some reason the CSV mapper gets 
	 * confused when using mixins and things will not work properly.
	 */	
	public DeltaQueryStatus complete() {return DeltaQueryStatus.complete;}
	public DeltaQueryStatus incomplete() {return DeltaQueryStatus.incomplete;}
	public DeltaQueryStatus unknown() {return DeltaQueryStatus.unknown;}
	
	
	/**
	 * {@inheritDoc}
	 * @see jpl.gds.globallad.data.IGlobalLADData#serializeBase()
	 */
	@Override
	public Proto3BaseGladData serializeBase() {
		Proto3BaseGladData.Builder builder = Proto3BaseGladData.newBuilder()
				.setEventTime(eventTime)
				.setUserDataType(userDataType)
				.setScid(scid)
				.setDssId(dssId)
				.setVcid(vcid)
				.setSessionNumber(sessionNumber)
				.setVenue(venue)
				.setHost(host);


		if (eventTime >= 0) {
			builder.setEventTime(this.eventTime);
		}

		// Only set the time values if they are not the default values.
		if (ertMilliseconds >= 0 && ertNanoseconds >= 0) {
			builder.setErt(Proto3Adt.newBuilder()
					.setMilliseconds(ertMilliseconds)
					.setNanoseconds(ertNanoseconds)
					.build());
		}
		
		if (scetMilliseconds >= 0 && scetNanoseconds >= 0) {
			builder.setScet(Proto3Adt.newBuilder()
					.setMilliseconds(scetMilliseconds)
					.setNanoseconds(scetNanoseconds)
					.build());
		}

		if (sclkCoarse >= 0 && sclkFine >= 0) {
			builder.setSclk(Proto3Sclk.newBuilder()
					.setSeconds(sclkCoarse)
					.setNanos(sclkFine)
					.build());
		}

//				.setLst(Proto3Lst.newBuilder()
//						.setSol(value))
		
		return builder.build();
	}


	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final StringBuilder builder = new StringBuilder();
		builder.append("AbstractGlobalLadData [");
		builder.append("insertNumber=");
		builder.append(insertNumber);
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
		builder.append(", ");
		if (venue != null) {
			builder.append("venue=");
			builder.append(venue);
			builder.append(", ");
		}
		builder.append("sessionNumber=");
		builder.append(sessionNumber);
		builder.append(", dssId=");
		builder.append(dssId);
		builder.append(", vcid=");
		builder.append(vcid);
		builder.append(", ");
		if (host != null) {
			builder.append("host=");
			builder.append(host);
			builder.append(", ");
		}
		builder.append(", ");
		if (primaryTime != null) {
			builder.append("primaryTime=");
			builder.append(primaryTime);
		}
		builder.append("]");
		return builder.toString();
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.IGlobalLADData#compareInsert(jpl.gds.globallad.data.IGlobalLADData)
	 */
	@Override
	public int compareInsert(final IGlobalLADData other) {
		int cmp = Long.compare(eventTime, other.getEventTime());

		/**
		 * Include a comparison of the user data type.  Merging of values with the same
		 * user data type could mean that they looked equal and data would be lost.
		 */
		cmp = cmp == 0 ? Byte.compare(userDataType, other.getUserDataType()) : cmp;

		return cmp == 0 ? Long.compare(insertNumber, other.getInsertNumber()) : cmp;
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.IGlobalLADData#compareScet(jpl.gds.globallad.data.IGlobalLADData)
	 */
	@Override
	public int compareScet(final IGlobalLADData other) {
		int cmp = Long.compare(scetMilliseconds, other.getScetMilliseconds());
		cmp = cmp == 0 ? Long.compare(scetNanoseconds, other.getScetNanoseconds()) : cmp;

		/**
		 * Include a comparison of the user data type. Merging of values with the same
		 * user data type could mean that they looked equal and data would be lost.
		 */
		cmp = cmp == 0 ? Byte.compare(userDataType, other.getUserDataType()) : cmp;

		return cmp == 0 ? Long.compare(insertNumber, other.getInsertNumber()) : cmp;
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.IGlobalLADData#compareErt(jpl.gds.globallad.data.IGlobalLADData)
	 */
	@Override
	public int compareErt(final IGlobalLADData other) {
		int cmp = Long.compare(ertMilliseconds, other.getErtMilliseconds());
		cmp = cmp == 0 ? Long.compare(ertNanoseconds, other.getErtNanoseconds()) : cmp;
		
		/**
		 * Include a comparison of the user data type.  Merging of values with the same user
		 * data type could mean that they looked equal and data would be lost.
		 */
		cmp = cmp == 0 ? Byte.compare(userDataType, other.getUserDataType()) : cmp;
		
		return cmp == 0 ? Long.compare(insertNumber, other.getInsertNumber()) : cmp;
	}
	
	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.IGlobalLADData#compareTo(jpl.gds.globallad.data.IGlobalLADData)
	 */
	@Override
	public int compareTo(final Object data) {
		if (data == null || ! (data instanceof IGlobalLADData)) {
			return -1;
		} else {
			final IGlobalLADData data_ = (IGlobalLADData) data;
			/**
			 * Check the time values first and lastly the insert number.  We will never actually have a 
			 * duplicate since we use the insert number.
			 */
			int compare = Long.compare(getPrimaryMilliseconds(), data_.getPrimaryMilliseconds());
			compare = compare == 0 ? Long.compare(getPrimaryTimeNanoseconds(), data_.getPrimaryTimeNanoseconds()) : compare;

			/**
			 * Include a comparison of the user data type. Merging of values with the same user
			 * data type could mean that they looked equal and data would be lost.
			 */
			compare = compare == 0 ? Byte.compare(userDataType, data_.getUserDataType()) : compare;
			return compare == 0 ? Long.compare(getInsertNumber(), data_.getInsertNumber()) : compare;
		}
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + dssId;
		result = prime * result + (int) (ertMilliseconds ^ (ertMilliseconds >>> 32));
		result = prime * result + (int) (ertNanoseconds ^ (ertNanoseconds >>> 32));
		result = prime * result + (int) (eventTime ^ (eventTime >>> 32));
		result = prime * result + ((host == null) ? 0 : host.hashCode());
		result = prime * result + (int) (scetMilliseconds ^ (scetMilliseconds >>> 32));
		result = prime * result + (int) (scetNanoseconds ^ (scetNanoseconds >>> 32));
		result = prime * result + scid;
		result = prime * result + (int) (sclkCoarse ^ (sclkCoarse >>> 32));
		result = prime * result + (int) (sclkFine ^ (sclkFine >>> 32));
		result = prime * result + (int) (sessionNumber ^ (sessionNumber >>> 32));
		result = prime * result + userDataType;
		result = prime * result + vcid;
		result = prime * result + ((venue == null) ? 0 : venue.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object obj) {
		/**
		 * This equals method checks all values EXCEPT for the insert number. 
		 */
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof AbstractGlobalLadData)) {
			return false;
		}
		final AbstractGlobalLadData other = (AbstractGlobalLadData) obj;
		if (dssId != other.dssId) {
			return false;
		}
		if (ertMilliseconds != other.ertMilliseconds) {
			return false;
		}
		if (ertNanoseconds != other.ertNanoseconds) {
			return false;
		}
		if (eventTime != other.eventTime) {
			return false;
		}
		if (host == null) {
			if (other.host != null) {
				return false;
			}
		} else if (!host.equals(other.host)) {
			return false;
		}
		if (scetMilliseconds != other.scetMilliseconds) {
			return false;
		}
		if (scetNanoseconds != other.scetNanoseconds) {
			return false;
		}
		if (scid != other.scid) {
			return false;
		}
		if (sclkCoarse != other.sclkCoarse) {
			return false;
		}
		if (sclkFine != other.sclkFine) {
			return false;
		}
		if (sessionNumber != other.sessionNumber) {
			return false;
		}
		if (userDataType != other.userDataType) {
			return false;
		}
		if (vcid != other.vcid) {
			return false;
		}
		if (venue == null) {
			if (other.venue != null) {
				return false;
			}
		} else if (!venue.equals(other.venue)) {
			return false;
		}
		return true;
	}
	
	@Override
	public byte[] toPacketByteArray() {
		byte[] data = serialize().toByteArray();
		
		ByteBuffer retData = ByteBuffer.allocate(GLAD_PACKET_START_WORD.length + Integer.BYTES + data.length);
		
		retData.put(GLAD_PACKET_START_WORD);
		retData.putInt(data.length + Integer.BYTES);
		retData.put(data);
		
		return retData.array();
		
	}
	
}
