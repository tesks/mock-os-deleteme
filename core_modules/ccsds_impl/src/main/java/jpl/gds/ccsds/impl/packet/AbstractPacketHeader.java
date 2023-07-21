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
package jpl.gds.ccsds.impl.packet;

import jpl.gds.ccsds.api.packet.ISpacePacketHeader;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.Sclk;


/**
 *
 * AbstractPacketHeader is the base class for PacketHeader classes.
 *
 */
abstract class AbstractPacketHeader implements ISpacePacketHeader
{
	/** Packet version number */
	private byte versionNumber;

	/** The packet type field (0 for telemetry, 1 for telecommand) */
	private byte packetType;

	/** Indicates presence of secondary header flag (1 for present, 0 for absent) */
	private byte secondaryHeaderFlag;

	/** The application process identifier */
	protected short apid;

    /** The segment flags to indicate where in a stream of packets this packet falls
     * (00 continuation, 01 first segment, 10 last segment, 11 unsegmented)
     */
	private byte segmentFlags;

	/** The sequence count for this packet */
	private short sourceSequenceCount;

	/** The length of the data portion of this packet */
	private int packetDataLength;

	/** Remove static configuration of secondary header length.  Secondary header
	 * length can only be known once the packets format is known.  Currently, this is only determined by apid.
	 */
	protected int secondaryHeaderLength = 0;
	
	// Until the raw header bytes containing a timecode are fed to this object, the ISclk for the packet remains zero.
	// If there is no timecode in the packet header at all, the ISclk will stay 0.
	protected ISclk packetSclk = Sclk.MIN_SCLK;

	/** Idle packets from frames are not being correctly validated.
	 * CCSDS Space Packet Protocol recommends Idle packets to not have a secondary header but mission
	 * like MSL and M20 deviate from this recommendation.
	 *
	 * This flag is used to control packet header validation for Idle packets.
	 */
	protected boolean idlePacketSecondaryHeaderAllowed;

	/**
	 * CCSDS Space Packet Protocol recommends Idle packets to not have a secondary header but mission
	 * like MSL and M20 deviate from this recommendation.
	 *
	 * We currently don't do a good job of reporting the exact reason of the packet header validation failure.
	 * This variable is used to report the exact reason as to why the packet was marked invalid.
	 */
	private String invalidReason;

    /**
	 * Creates an instance of AbstractPacketHeader.
	 */
	protected AbstractPacketHeader()
	{
		setVersionNumber((byte)0);
		setPacketType((byte)0);
		setSecondaryHeaderFlag((byte)0);
		setApid((short)0);
		setGroupingFlags((byte)0);
		setSourceSequenceCount((short)0);
		setPacketDataLength(0);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.ccsds.api.packet.ISpacePacketHeader#isValid()
	 */
	@Override
	public boolean isValid()
	{
		/*
		 * If this is an Idle Packet, check to make sure the current mission allows Idle Packets
		 * to contain a Secondary Header.
		 *
		 * Also added specific reason as to why the packet header is marked invalid for each of the
		 * following conditions checked.
		 */
		if (isFill()) {
			if (getSecondaryHeaderFlag() == 1 && !idlePacketSecondaryHeaderAllowed) {
				invalidReason = "Idle Packet has Secondary Header but config parameter idlePacketSecondaryHeaderAllowed=false";
				return false;
			}
		}

		if (getVersionNumber() != 0) {
			invalidReason = "Packet Version Number '" + getVersionNumber() + "' != 0";
			return false;
		}

		if (getPacketType() != 0) {
			invalidReason = "Packet Type '" + getPacketType() + "' != 0";
			return false;
		}

		final int apid = getApid();
		/*
		 * any allowable apid is potentially valid
		 */
		if (apid < 0 || apid > getMaxAllowableApid()) {
			invalidReason = "APID '" + apid + "' not between 0 and " + getMaxAllowableApid();
			return false;
		}

		/*
		 * Changed getMaxPacketDataLength to MAX_PACKET
		 * That's the actual value being calculated here
		 */
		if (getPrimaryHeaderLength() + getPacketDataLength() + 1 > MAX_PACKET) {
			invalidReason = "Packet Primary Header Length '" + getPrimaryHeaderLength()
					+ "' + Packet Data Length '" + getPacketDataLength() + "' " +
					"+ 1 > MAX_PACKET_LENGTH '" + MAX_PACKET + "'";
			return false;
		}
		return true;
	}


	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		final StringBuffer sb = new StringBuffer();

		sb.append("Packet Header");
		sb.append("\tVersion Number = " + getVersionNumber() + " pkt_type " + getPacketType() + "\n");
		sb.append("\tSecondary Header Flag = " + getSecondaryHeaderFlag() + "\n");
		sb.append("\tApid = " + getApid() + "\n");
		sb.append("\tSegment Flags = " + getGroupingFlags() + "\n");
		sb.append("\tSource Sequence Count = " + getSourceSequenceCount() + "\n");
		sb.append("\tPacket Data Length = " + getPacketDataLength() + "\n");

		return(sb.toString());
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.ccsds.api.packet.ISpacePacketHeader#getApid()
	 */
	@Override
	public short getApid()
	{
		return apid;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.ccsds.api.packet.ISpacePacketHeader#setApid(short)
	 */
	@Override
	public void setApid(final short apid)
	{
		if(apid < 0 || apid > getMaxAllowableApid())
		{
			throw new IllegalArgumentException("Input apid value must be a positive value less than or equal to" +
											   getMaxAllowableApid());
		}

		this.apid = apid;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.ccsds.api.packet.ISpacePacketHeader#getPacketDataLength()
	 */
	@Override
	public int getPacketDataLength()
	{
		return packetDataLength;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.ccsds.api.packet.ISpacePacketHeader#setPacketDataLength(int)
	 */
	@Override
	public void setPacketDataLength(final int packetDataLength)
	{
		if(packetDataLength < 0 || packetDataLength > getMaxPacketDataLength())
		{
			throw new IllegalArgumentException("Input packet data length must be a positive value that is less than or equal to " +
											   getMaxPacketDataLength());
		}

		this.packetDataLength = packetDataLength;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.ccsds.api.packet.ISpacePacketHeader#getPacketType()
	 */
	@Override
	public byte getPacketType()
	{
		return packetType;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.ccsds.api.packet.ISpacePacketHeader#setPacketType(byte)
	 */
	@Override
	public void setPacketType(final byte packetType)
	{
		if (packetType != 0 && packetType != 1) 
		{
			throw new IllegalArgumentException("Input packet type must be a 0 or 1");
		}

		this.packetType = packetType;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.ccsds.api.packet.ISpacePacketHeader#getSecondaryHeaderFlag()
	 */
	@Override
	public byte getSecondaryHeaderFlag()
	{
		return secondaryHeaderFlag;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.ccsds.api.packet.ISpacePacketHeader#setSecondaryHeaderFlag(byte)
	 */
	@Override
	public void setSecondaryHeaderFlag(final byte secondaryHeaderFlag)
	{
		if(secondaryHeaderFlag != 0 && secondaryHeaderFlag != 1)
		{
			throw new IllegalArgumentException("Input secondary header flag must be 0 or 1");
		}

		this.secondaryHeaderFlag = secondaryHeaderFlag;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.ccsds.api.packet.ISpacePacketHeader#getGroupingFlags()
	 */
	@Override
	public byte getGroupingFlags()
	{
		return segmentFlags;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.ccsds.api.packet.ISpacePacketHeader#setGroupingFlags(byte)
	 */
	@Override
	public void setGroupingFlags(final byte segmentFlags)
	{
		if(segmentFlags < 0 || segmentFlags > MAX_GROUPING_FLAGS)
		{
			throw new IllegalArgumentException("Input segment flags must be a positive value less than or equal to " +
											   MAX_GROUPING_FLAGS);
		}

		this.segmentFlags = segmentFlags;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.ccsds.api.packet.ISpacePacketHeader#getSourceSequenceCount()
	 */
	@Override
	public short getSourceSequenceCount()
	{
		return sourceSequenceCount;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.ccsds.api.packet.ISpacePacketHeader#setSourceSequenceCount(short)
	 */
	@Override
	public void setSourceSequenceCount(final short sourceSequenceCount)
	{
		if(sourceSequenceCount < 0 || sourceSequenceCount > getMaxSequenceNumber())
		{
			throw new IllegalArgumentException("Input source packet sequence counter must be a positive value that is less than or equal to " +
											   getMaxSequenceNumber());
		}

		this.sourceSequenceCount = sourceSequenceCount;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.ccsds.api.packet.ISpacePacketHeader#getVersionNumber()
	 */
	@Override
	public byte getVersionNumber()
	{
		return versionNumber;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.ccsds.api.packet.ISpacePacketHeader#setVersionNumber(byte)
	 */
	@Override
	public void setVersionNumber(final byte versionNumber)
	{
		if(versionNumber < 0 || versionNumber > MAX_VERSION)
		{
			throw new IllegalArgumentException("Input version number must be a positive value that is less than or equal to " +
											   MAX_VERSION);
		}

		this.versionNumber = versionNumber;
	}
	
	/* MPCS=7289 - 4/30/15. Moved getFieldValue(PacketHeaderFieldName) to the
	 * IPacketInfo classes.
	 */
    
	/**
	 * {@inheritDoc}
	 * @see jpl.gds.ccsds.api.packet.ISpacePacketHeader#getBytes()
	 */
	@Override
	public byte[] getBytes()
	{
		final byte[] bytes = new byte[getPrimaryHeaderLength()];
		toBytes(bytes,0);

		return(bytes);
	}
	
	/**
	 * Save the primary packet header into a byte buffer
	 *
	 * @param buff The byte buffer to load the packet header into
	 *
	 * @param off The offset into the byte buffer pointing to where the
	 * packet header will be written
	 *
	 * @return The offset into the input byte buffer pointing just past the
	 * written packet header
	 */
    abstract protected int toBytes(final byte[] buff, int off);
    
	/**
	 * {@inheritDoc}
	 * @see jpl.gds.ccsds.api.packet.ISpacePacketHeader#setHeaderValuesFromBytes(byte[], int)
	 */
	@Override
	public void setHeaderValuesFromBytes(final byte[] buff, final int off) {
		setPrimaryValuesFromBytes(buff, off);
	}

	@Override
	public void setIdlePacketSecondaryHeaderAllowed(final boolean idlePacketSecondaryHeaderAllowed) {
		this.idlePacketSecondaryHeaderAllowed = idlePacketSecondaryHeaderAllowed;
	}

	/**
	 * Get the specific reason for the Packet Header validation failure
	 *
	 * @return the reason header validation failed
	 */
	public String getInvalidReason() {
		return (invalidReason != null) ? invalidReason : "NOT SET";
	}
}
