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

import jpl.gds.shared.gdr.GDR;

/**
 *
 * This class represents a standard CCSDS-defined packet header.
 *
 *
 * Adding ISclk extraction logic.
 * Though timecodes in the secondary header are not strictly part of the CCSDS packet version
 * 1, it is allowed as a possibility in the secondary header.  It is also the only valid area for a
 * packet implementing this protocol can put a time code in.
 */
class CcsdsPacketHeader extends AbstractPacketHeader
{
	/** Constant for an idle APID */
    public static final short IDLE_APID = 2047;
    
	/** The amount of bits in the version number */
	private static final int VERSION_NUMBER_BIT_LENGTH = 3;

	/** The number of bits in the packet type */
	private static final int PACKET_TYPE_BIT_LENGTH = 1;

	/** The number of bits in the secondary header flag */
	private static final int SEC_HDR_FLAG_BIT_LENGTH = 1;

	/** The bit length of the APID */
	private static final int APID_BIT_LENGTH = 11;

	/** Max apid supported by APID bit size **/
	private static final int MAX_ALLOWABLE_APID =
        (int) (Math.pow(2.0, APID_BIT_LENGTH) - 1.0);

	/** The bit length of the segment flags */
	private static final int SEGMENT_FLAGS_BIT_LENGTH = 2;

	/** The bit length of the packet sequence count */
	private static final int SRC_SEQ_COUNT_BIT_LENGTH = 14;

	/** The bit length of the packet data length field */
	private static final int PACKET_DATA_LENGTH_BIT_LENGTH = 16;

	/** Byte length of the packet ID fields (version, type, secondary header flag, apid) */
	private static final int ID_FIELDS_BYTE_LENGTH = 2;
	/** Byte length of the sequence control fields (segment flags, source sequence count) */
	private static final int SEQ_CONTROL_FIELDS_BYTE_LENGTH = 2;
	/** Byte length of the entire packet header */
	private static final int PACKET_HEADER_BYTE_LENGTH = ID_FIELDS_BYTE_LENGTH +
														SEQ_CONTROL_FIELDS_BYTE_LENGTH +
														(PACKET_DATA_LENGTH_BIT_LENGTH/8);

	/*
	 *           allows the value to be set once and not calculated every time
	 */
	/** Max value of the packet data length field */
	private static final int MAX_PACKET_DATA_LENGTH = (int)Math.pow(2,PACKET_DATA_LENGTH_BIT_LENGTH) - 1;

	/** Max sequence number value */
	private static final int MAX_SEQUENCE_NUMBER = (int)Math.pow(2,SRC_SEQ_COUNT_BIT_LENGTH) - 1;

    /**
	 * Creates an instance of CcsdsPacketHeader.
	 */
	protected CcsdsPacketHeader()
	{
        super();
	}



	/**
	 * {@inheritDoc}
	 * @see jpl.gds.ccsds.api.packet.ISpacePacketHeader#setPrimaryValuesFromBytes(byte[], int)
	 */
	@Override
	public int setPrimaryValuesFromBytes(final byte[] buff, final int inOffset)
	{
	    int off = inOffset;
	    
		if(buff == null)
		{
			throw new IllegalArgumentException("Null input byte array");
		}
		else if(off < 0 || off >= buff.length)
		{
			throw new IndexOutOfBoundsException("The offset " + off + " does not fall into the range of the input buffer");
		}
		else if((off+PACKET_HEADER_BYTE_LENGTH) > buff.length)
		{
			throw new IllegalArgumentException("Offset + packet header length is beyond the length of the input buffer");
		}

		int bitoff = 0;

		// read the packet identification fields
		setVersionNumber((byte)GDR.cbits_to_u8(buff, off, bitoff, VERSION_NUMBER_BIT_LENGTH));
		bitoff += VERSION_NUMBER_BIT_LENGTH;
		setPacketType((byte)GDR.cbits_to_u8(buff, off, bitoff, PACKET_TYPE_BIT_LENGTH));
		bitoff += PACKET_TYPE_BIT_LENGTH;
		setSecondaryHeaderFlag((byte)GDR.cbits_to_u8(buff, off, bitoff, SEC_HDR_FLAG_BIT_LENGTH));
		bitoff += SEC_HDR_FLAG_BIT_LENGTH;
		setApid((short)GDR.cbits_to_u16(buff, off, bitoff, APID_BIT_LENGTH));
		bitoff += APID_BIT_LENGTH;

		// move past the packet identification fields
		off += ID_FIELDS_BYTE_LENGTH;
		bitoff = 0;

		// read the packet sequence control fields
		setGroupingFlags((byte)GDR.cbits_to_u8(buff, off, bitoff, SEGMENT_FLAGS_BIT_LENGTH));
		bitoff += SEGMENT_FLAGS_BIT_LENGTH;
		setSourceSequenceCount((short)GDR.cbits_to_u16(buff, off, bitoff, SRC_SEQ_COUNT_BIT_LENGTH));
		bitoff += SRC_SEQ_COUNT_BIT_LENGTH;

		// move past the packet sequence control fields
		off += SEQ_CONTROL_FIELDS_BYTE_LENGTH;
		bitoff = 0;

		// read the packet length field
		setPacketDataLength(GDR.get_u16(buff, off));
		off += (PACKET_DATA_LENGTH_BIT_LENGTH/8);

		return(off);
	}

	/**
	 * {@inheritDoc}
	@Override
	public void setSecondaryValuesFromBytes(byte [] buff, int off) {
		// TODO  Should there be a way to make sure APID is already set? maybe make this method private.
		// TODO add back in when there's actually infrastructure for this
	}*/


    /**
     * Extract just the APID from the buffer.
     *
     * @param buff Byte buffer
     * @param off  Offset within buffer
     *
     * @return Apid
     */
    @Override
    public int getApidFromBytes(final byte[] buff,
                                final int    off)
    {

        if (buff == null)
        {
            throw new IllegalArgumentException("Null input byte array");
        }

        if ((off < 0) || (off >= buff.length))
        {
            throw new IndexOutOfBoundsException(
                          "The offset " +
                          off           +
                          " does not fall into the range of the input buffer");
        }

        if ((off + PACKET_HEADER_BYTE_LENGTH) > buff.length)
        {
            throw new IllegalArgumentException(
                          "Offset + packet header length is beyond " +
                          "the length of the input buffer");
        }

        final int bitoff = VERSION_NUMBER_BIT_LENGTH +
                           PACKET_TYPE_BIT_LENGTH    +
                           SEC_HDR_FLAG_BIT_LENGTH;

        return GDR.cbits_to_u16(buff, off, bitoff, APID_BIT_LENGTH);
    }


	/**
	 * {@inheritDoc}
	 * @see jpl.gds.ccsds.impl.packet.AbstractPacketHeader#toBytes(byte[], int)
	 */
	@Override
	protected int toBytes(final byte[] buff, final int inOffset)
	{
	    int off = inOffset;
		int bitoff = 0;

		//write the packet identification fields
		GDR.u8_to_cbits(buff, off, getVersionNumber(), bitoff, VERSION_NUMBER_BIT_LENGTH);
		bitoff += VERSION_NUMBER_BIT_LENGTH;
		GDR.u8_to_cbits(buff, off, getPacketType(), bitoff, PACKET_TYPE_BIT_LENGTH);
		bitoff += PACKET_TYPE_BIT_LENGTH;
		GDR.u8_to_cbits(buff, off, getSecondaryHeaderFlag(), bitoff, SEC_HDR_FLAG_BIT_LENGTH);
		bitoff += SEC_HDR_FLAG_BIT_LENGTH;
		GDR.u16_to_cbits(buff, off, getApid(), bitoff, APID_BIT_LENGTH);
		bitoff += APID_BIT_LENGTH;

		//move past the packet identification fields
		off += ID_FIELDS_BYTE_LENGTH;
		bitoff = 0;

		//write the packet sequence control fields
		GDR.u8_to_cbits(buff, off, getGroupingFlags(), bitoff, SEGMENT_FLAGS_BIT_LENGTH);
		bitoff += SEGMENT_FLAGS_BIT_LENGTH;
		GDR.u16_to_cbits(buff, off, getSourceSequenceCount(), bitoff, SRC_SEQ_COUNT_BIT_LENGTH);
		bitoff += SRC_SEQ_COUNT_BIT_LENGTH;

		//move past the packet sequence control fields
		off += SEQ_CONTROL_FIELDS_BYTE_LENGTH;
		bitoff = 0;

		//write the packet data length
		GDR.set_u16(buff, off, getPacketDataLength());
		off += (PACKET_DATA_LENGTH_BIT_LENGTH/8);

		return(off);
	}

    /**
     * {@inheritDoc}
     * @see jpl.gds.ccsds.api.packet.ISpacePacketHeader#isFill()
     */
    @Override
	public boolean isFill()
    {
        return getApid() == IDLE_APID;
    }

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.ccsds.api.packet.ISpacePacketHeader#getPrimaryHeaderLength()
	 */
	@Override
	public int getPrimaryHeaderLength() {
		return PACKET_HEADER_BYTE_LENGTH;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.ccsds.api.packet.ISpacePacketHeader#getMaxAllowableApid()
	 */
	@Override
	public int getMaxAllowableApid() {
		return MAX_ALLOWABLE_APID;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.ccsds.api.packet.ISpacePacketHeader#getMaxPacketDataLength()
	 */
	@Override
	public int getMaxPacketDataLength() {
		return MAX_PACKET_DATA_LENGTH;
		
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.ccsds.api.packet.ISpacePacketHeader#getMaxSequenceNumber()
	 */
	@Override
	public int getMaxSequenceNumber() {
		return MAX_SEQUENCE_NUMBER;
	}

}
