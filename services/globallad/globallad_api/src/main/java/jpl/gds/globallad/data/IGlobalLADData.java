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
/**
 * 
 */
package jpl.gds.globallad.data;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import jpl.gds.serialization.globallad.data.Proto3BaseGladData;
import jpl.gds.serialization.globallad.data.Proto3GlobalLadTransport;

/**
 * Interface for the re-implemented global lad data objects.  These objects will be 
 * created in chill down and sent to the global lad server over a socket.
 * 
 * Note, nothing has been forced with the Jackson annotations for serialization.  If a new type is added it has to be added to the 
 * sub types below.  All other field, ignore, etc annotations have to be handled by the subclass.
 */
@JsonTypeInfo(use=JsonTypeInfo.Id.CLASS, include=JsonTypeInfo.As.PROPERTY, property="@class")
@JsonSubTypes({
	@JsonSubTypes.Type(value=EhaGlobalLadData.class, name="eha"),
	@JsonSubTypes.Type(value=EvrGlobalLadData.class, name="evr"),
	@JsonSubTypes.Type(value=AlarmHistoryGlobalLadData.class, name="history")
})
public interface IGlobalLADData extends Comparable<Object> {
	public enum GlobalLadPrimaryTime {
		/**
		 *  SCET, ERT and EVENT times are the only valid time types for query. 
		 */
		SCET, 
		ERT, 
		EVENT, 
		
		/**
		 * To make characterizing and parsing of query params easier this is included.  These will be treated
		 * the same as SCET.
		 */
		SCLK,
		LST,
		/**
		 * Used for queries and indicates that all of the data needs to be returned regardless of the 
		 * type.  This time value must be enabled in the global lad properties.
		 */
		ALL
	}

	/**
	 * Used to identify this object.  If this object is EHA should be the channel id.  Evr should be 
	 * the evr id and so on.  
	 * 
	 * @return the identifer
	 */
	public Object getIdentifier();
	
	/**
	 * @return the milliseconds time that corresponds to the configured GlobalLadPrimaryTime. 
	 */
	public long getPrimaryMilliseconds();
	
	/**
	 * @return the nanosecond time that corresponds to the configured GlobalLadPrimaryTime. 
	 */
	public long getPrimaryTimeNanoseconds();

	/**
	 * @return the primaryTimeType.
	 */
	public GlobalLadPrimaryTime getPrimaryTimeType();
	
	/**
	 * @return the spacecraft ID.
	 */
	public int getScid();
	
	/**
	 * A unique number designating when this object was inserted into a global lad container.   Used
	 * for ordering.  
	 * 
	 * @return the insert number.
	 */
	public long getInsertNumber();
	
	/**
	 * Sets the insert number.
	 * 
	 * @param insertNumber
	 */
	public void setInsertNumber(final long insertNumber);
	
	/**
	 * @return the sclkCoarse
	 */
	public long getSclkCoarse();

	/**
	 * @return the dssId
	 */
	public byte getDssId();

	/**
	 * @return the vcid
	 */
	public byte getVcid();

	/**
	 * @return the sclk fine
	 */
	public long getSclkFine();

	/**
	 * @return ert milliseconds
	 */
	public long getErtMilliseconds();

	/**
	 * @return ert nanoseconds.
	 */
	public long getErtNanoseconds();

	/**
	 * @return scet milliseconds
	 */
	public long getScetMilliseconds();

	/**
	 * @return scet nanoseconds
	 */
	public long getScetNanoseconds();

	/**
	 * @return the sessionNumber
	 */
	public long getSessionNumber();

	/**
	 * @return the venue
	 */
	public String getVenue();
	
	/**
	 * @return the host
	 */
	public String getHost();
	
	/**
	 * @return milliseconds since this object was created.
	 */
	public long getCreateTimeDelta();
	
	/**
	 * @return the event time, represented in milliseconds from the Java epoch.
	 */
	public long getEventTime();
		
	/**
	 * @return the userDataType
	 */
	public byte getUserDataType();
	
	/**
	 * 
	 * @param userDataType the new user data type.
	 */
	public void setUserDataType(byte userDataType);
	
	/**
	 * Compare the scet times of this and other.
	 * 
	 * @param other
	 * @return 0 if equal, -1 if this is less than other and 1 if this is greather than other.
	 */
	public int compareScet(final IGlobalLADData other);
	
	/**
	 * Compare the scet times of this and other.
	 * 
	 * @param other
	 * @return 0 if equal, -1 if this is less than other and 1 if this is greather than other.
	 */
	public int compareErt(final IGlobalLADData other);
	
	/**
	 * Compares based on insert time.  
	 * 
	 * @param other
	 * @return 0 if equal, -1 if this is less than other and 1 if this is greather than other.
	 */
	public int compareInsert(final IGlobalLADData other);

	/**
	 * Create the object that is going to be sent accross the wire.
	 * 
	 * @return transport object
	 */
	public Proto3GlobalLadTransport serialize();
	
	/**
	 * Serialize the base data
	 * 
	 * @return
	 */
	public Proto3BaseGladData serializeBase();
	
	/**
	 * create a byte array that is ready for transporting. Prefixes with the
	 * glad packet start word and length
	 * @return
	 */
	public byte[] toPacketByteArray();
}
