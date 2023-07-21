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

package jpl.gds.ccsds.impl.tm.frame;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import jpl.gds.ccsds.api.tm.frame.ITelemetryFrameHeader;
import jpl.gds.dictionary.api.channel.FrameHeaderFieldName;
import jpl.gds.dictionary.api.frame.IFrameTimeFieldDefinition;
import jpl.gds.dictionary.api.frame.ITransferFrameDefinition;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.ISclkExtractor;

/**
 * CcsdsFrameHeaderV2 represents the fields in a CCSDS version 1 (TM)
 * frame header as described in CCSDS 132.0-B-1. It is capable of parsing the 
 * header from data bytes.
 * <p>
 * Additional public methods should NOT be implemented in this class without
 * consulting the CogE. All use of this class must go through the IFrameHeader
 * interface, or the frame adaptation will break.
 * <br><br>
 * A CCSDS V1 (TM) frame consists of a 6-byte primary header:<br>
 *    Bits 0-1:   Frame version number<br>
 *    Bits 2-11:  Spacecraft ID<br>
 *    Bits 12-14: Virtual Channel Identifier<br>
 *    Bit  15:    Operational Control Field Flag<br>
 *    Bits 16-23: Master Channel Frame Count<br>
 *    Bits 24-31: Virtual Channel Frame Count<br>
 *    Bits 23-47: Frame Data Field Status (with last 11 bits 
 *                being First Header Pointer)<br>
 * <br>               
 * This is followed by an optional secondary header, the
 * data area, an optional 32-bit operational control field, and 
 * an optional 16-bit FECF, computed according to CCSDS 132.0-B-1.
 * <br>
 * If an instance is created without any associated ITransferFrameDefinition,
 * optional fields are assumed to not exist.
 *
 *
 */
class CcsdsFrameHeaderV1 implements ITelemetryFrameHeader {
	/** The total header size in bytes. */
	private static final int PRIMARY_HEADER_SIZE = 6;

	private byte[] headerBytes;
	private ITransferFrameDefinition formatDef;

	private List<Integer> idleVcids = new LinkedList<Integer>();

	/**
	 * Creates an instance of CcsdsFrameHeaderV1.
	 */
	protected CcsdsFrameHeaderV1() { 
		// do nothing
	}
	
	/**
     * Creates an instance of CcsdsFrameHeaderV1.
     * 
     * @param def the ITransferFrameDefinition of the frame from the frame dictionary
     */
	protected CcsdsFrameHeaderV1(final ITransferFrameDefinition def) { 
	    formatDef = def;
	}
	
	/**
	 * {@inheritDoc}
	 * @see jpl.gds.ccsds.api.tm.frame.ITelemetryFrameHeader#skipContent(byte[], int)
	 */
	@Override
	public boolean skipContent(final byte[] buff, final int off) {
	    
	    /* Note this is a JPL-specific
	     * implementation.  Likely any implementation will be specific,
	     * as there is no standard frame content that says "skip me".
	     */
		final long tf = GDR.get_u32(buff, off+4);
		if (tf == 0xdeadc0de) {
			return true;
		}
		final long v1 = GDR.get_u32(buff, off+4+4);
		final long v2 = GDR.get_u32(buff, off+4+4+4);
		return v1 == v2;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.ccsds.api.tm.frame.ITelemetryFrameHeader#load(byte[], int)
	 */
	@Override
	public int load(final byte[] buff, final int inOffset) {
	    final int neededLen = formatDef == null ? PRIMARY_HEADER_SIZE : formatDef.getTotalHeaderSizeBytes();
	    
	    if (buff.length - inOffset + 1 < neededLen) {
	        throw new IllegalArgumentException("Input buffer is not large enough for a transfer frame header");
	    }
	    headerBytes = new byte[neededLen];
	    System.arraycopy(buff, inOffset, headerBytes, 0, neededLen);
	    
	    if (formatDef != null && !formatDef.hasOperationalControl() && getOpcntlFlag() != 0) {
	        TraceManager.getDefaultTracer().

	           warn("Transfer frame header indicates presence of operational control field, in conflict with the transfer frame dictionary entry for frame type " 
	              + formatDef.getName());
	    }
	    
	    if (formatDef != null) {
	        if (!formatDef.hasSecondaryHeader() && getSecondaryHeaderFlag() != 0) {
            TraceManager.getDefaultTracer().

               warn("Transfer frame header indicates presence of secondary header, in conflict with the transfer frame dictionary entry for frame type "
                       + formatDef.getName());
	        }
	        if (formatDef.getSecondaryHeaderSizeBytes() != getSecondaryHeaderLength() + 1) {
	            TraceManager.getDefaultTracer().

	            warn("Transfer frame header indicates secondary header of length " +
	                    String.valueOf(getSecondaryHeaderLength() + 1) + ", in conflict with the transfer frame dictionary entry for frame type "
	                    + formatDef.getName());
	        }
        }

		return inOffset + headerBytes.length;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.ccsds.api.tm.frame.ITelemetryFrameHeader#dump()
	 */
	@Override
	public void dump() {
		final StringBuilder sb = new StringBuilder(256);
		sb.append("--CCSDS TM Telemetry Frame--\n");
		sb.append("mcid=" + getMasterChannelId() + "\n");
		sb.append("  : scid=" + getScid() + "\n");
		sb.append("  : version=" + getVersion() +"\n");
		sb.append("vcid=" + getVirtualChannelId() + "\n");
		sb.append("op control flag=" + getOpcntlFlag() +"\n");
		sb.append("mcfc=" + getMasterChannelFrameCount() + "\n");
		sb.append("vcfc=" + getVirtualChannelFrameCount() + "\n");
		sb.append("data field status " + Integer.toHexString(getDataFieldStatus()) + "\n");
		sb.append("  : secondary header flag=" + getSecondaryHeaderFlag() +"\n");
		sb.append("  : synchronization flag=" + getSynchronizationFlag() +"\n");
		sb.append("  : packet order flag=" + getPacketOrderFlag() +"\n");
		sb.append("  : segment length ID=" + getSegmentLengthId() +"\n");
		sb.append("fhp=" + getDataPointer());
		System.out.println(sb);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.ccsds.api.tm.frame.ITelemetryFrameHeader#getScid()
	 */
	@Override
	public int getScid() {
        return GDR.cbits_to_u16(headerBytes, 0, 2, 10);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.ccsds.api.tm.frame.ITelemetryFrameHeader#getVirtualChannelFrameCount()
	 */
	@Override
	public int getVirtualChannelFrameCount() {
		return 0xff & headerBytes[3];
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.ccsds.api.tm.frame.ITelemetryFrameHeader#getDataPointer()
	 */
	@Override
	public int getDataPointer() {
	    if (getSynchronizationFlag() == 0) {
	        return 0x7ff & GDR.cbits_to_u16(headerBytes, 4, 5, 11);
	    } else {
	        throw new UnsupportedOperationException(
	            "Attempting to fetch data pointer from CCSDS TM frame header, but frame synchronization flag is not 0");
	    }
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.ccsds.api.tm.frame.ITelemetryFrameHeader#getVirtualChannelId()
	 */
	@Override
	public int getVirtualChannelId() {
		return GDR.cbits_to_u16(headerBytes, 0, 12, 3); 
	}
	
    /**
     * {@inheritDoc}
     * @see jpl.gds.ccsds.api.tm.frame.ITelemetryFrameHeader#isIdle()
     */
    @Override
    public boolean isIdle() {
    	/*
    	 * Add VCID check for IDLE frames in addition to the FPP check.
    	 */
    	return getDataPointer() == 0x7fe || idleVcids.contains(getVirtualChannelId());
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.ccsds.api.tm.frame.ITelemetryFrameHeader#isContinuation()
     */
    @Override
    public boolean isContinuation() {
        /* Note that all V1 (TM) frames have a first
         * header pointer in the primary header, which is assumed to point to the
         * first packet header as an M_PDU in the AOS standard.  There seem to be
         * no other options for TM frames.
         */
        return getDataPointer() == 0x7ff;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.ccsds.api.tm.frame.ITelemetryFrameHeader#getMaxSeqCount()
     */
    @Override
    public int getMaxSeqCount() {
    	return 0xFF;
    }


    @SuppressWarnings("deprecation")
	@Override
    public Object getFieldValue(final FrameHeaderFieldName field) {
    	switch (field) {
    	case TRANSFER_FRAME_VERSION_NUMBER: return getVersion() + 1;
    	case SPACECRAFT_ID: return getScid();
    	case VCID: return getVirtualChannelId();
    	case VCFC: return getVirtualChannelFrameCount();
    	case REPLAY_FLAG: return null;
    	case VC_FRAME_COUNT_USAGE_FLAG: return null;
    	case VC_FRAME_COUNT_CYCLE: return null; 
    	case OPERATIONAL_CONTROL: return getOpcntlFlag();
    	case MCID: return getMasterChannelId();
    	case MFC: return getMasterChannelFrameCount();
    	case SECONDARY_HEADER_FLAG: return getSecondaryHeaderFlag();
    	case PACKET_SEQ_FLAG: return getSynchronizationFlag();
    	case PACKET_SYNC_FLAG: return getSynchronizationFlag();
    	case PACKET_ORDER: return getPacketOrderFlag();
    	case PACKET_SEGMENT_LENGTH_ID: return getSegmentLengthId();
    	case FIRST_PACKET_POINTER: return getDataPointer();
        case BITSTREAM_DATA_POINTER: return null;
        case DATA_FIELD_STATUS: return getDataFieldStatus();
        case SECONDARY_HEADER_ID: return getSecondaryHeaderId();
        case SECONDARY_HEADER_LENGTH: return getSecondaryHeaderLength();
        case SIGNALING_FIELD: return null;
        case HEADER_ERROR_CONTROL: return null;
		case TIMECODE: return getTimecode();
		default:
			break;
    	}
    	return null;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.ccsds.api.tm.frame.ITelemetryFrameHeader#getPrimaryHeaderBytes()
     */
    @Override
    public byte[] getPrimaryHeaderBytes() {
        final byte[] result = new byte[PRIMARY_HEADER_SIZE];
        if (headerBytes != null) {
            System.arraycopy(headerBytes, 0, result, 0, PRIMARY_HEADER_SIZE);
        }
        return result;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.ccsds.api.tm.frame.ITelemetryFrameHeader#getSecondaryHeaderBytes()
     */
    @Override
    public Optional<byte[]> getSecondaryHeaderBytes() {
        if (formatDef == null || formatDef.getSecondaryHeaderSizeBytes() == 0) {
            return Optional.empty();
        }
        final byte[] result = new byte[formatDef.getSecondaryHeaderSizeBytes()];
        if (headerBytes != null) {
            System.arraycopy(headerBytes, PRIMARY_HEADER_SIZE, result, 0, formatDef.getSecondaryHeaderSizeBytes());
        }
        return Optional.ofNullable(result);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.ccsds.api.tm.frame.ITelemetryFrameHeader#getAllHeaderBytes()
     */
    @Override
    public byte[] getAllHeaderBytes() {
        return Arrays.copyOf(headerBytes, headerBytes.length);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.ccsds.api.tm.frame.ITelemetryFrameHeader#getVersion()
     */
    @Override
    public int getVersion() {
    	return GDR.cbits_to_u16(headerBytes, 0, 0, 2);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.ccsds.api.tm.frame.ITelemetryFrameHeader#getTimecode()
     */
    @Override
    public Optional<ISclk> getTimecode() {
        if (formatDef == null) {
            return Optional.empty();
        }
        final Optional<IFrameTimeFieldDefinition> timeField = formatDef.getTimeField();
        if (!timeField.isPresent()) {
            return Optional.empty();
        }
        try {
            final ISclkExtractor extractor = timeField.get().getExtractor();
            return Optional.ofNullable(extractor.getValueFromBytes(headerBytes, timeField.get().getBitOffset()/Byte.SIZE));
        } catch (final Exception e) {
            TraceManager.getDefaultTracer().

            warn("Unable to extract time code from transfer frame header for frame type "
                    + formatDef.getName() + ", reason=" + e.toString());
        }
     
        return Optional.empty();    
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.ccsds.api.tm.frame.ITelemetryFrameHeader#setIdleVcids(java.util.List)
     */
    @Override
	public void setIdleVcids(final List<Integer> vcidsToSet) {
		if (vcidsToSet == null) {
			this.idleVcids = new LinkedList<Integer>();
		} else {
			this.idleVcids = vcidsToSet;
		}	
	}

	/**
	 * Gets the master channel ID from the primary header.
	 * 
	 * @return master channel identifier.
	 */
	private int getMasterChannelId() {
	    return GDR.cbits_to_u16(headerBytes, 0, 0, 12);
	}
	
	/**
     * Gets the data field status from the primary header.
     * 
     * @return master channel identifier.
     */
	private int getDataFieldStatus() {
	    return GDR.get_u16(headerBytes, 4);
	}

	/**
	 * Gets the identification field from the secondary header. According to the
	 * CCSDS TM standard, the only allowed value is 0. (This actually represents
	 * version 1.)
	 * 
	 * @return secondary header ID, or UNDEFINED FIELD if there is no
	 *         associated ITransferFrameDefinition or there is no secondary
	 *         header.
	 */
	private int getSecondaryHeaderId() {
	    if (formatDef == null) {
	        return UNDEFINED_FIELD;
	    } else if (formatDef.hasSecondaryHeader()) {
	        return GDR.get_u8(headerBytes, PRIMARY_HEADER_SIZE) + 1;
	    }
	    
	    return UNDEFINED_FIELD;
	    
	}
	
	/**
     * Gets the secondary header length field from the secondary header. The
     * length returned is in bytes, and is a less one value.
     * 
     * @return secondary header length in bytes, or UNDEFINED FIELD if there is no
     *         associated ITransferFrameDefinition or there is no secondary
     *         header.
     */
	private int getSecondaryHeaderLength() {
	    if (formatDef == null) {
            return UNDEFINED_FIELD;
        } else if (formatDef.hasSecondaryHeader()) {
            return GDR.get_u8(headerBytes, PRIMARY_HEADER_SIZE) & 0x3f;
        }
        
        return UNDEFINED_FIELD;
	}

	/**
	 * Simple function that gets the secondary header flag from the primary header
	 * 
	 * @return the secondary header flag: 0 or 1
	 * 
	 */
	private int getSecondaryHeaderFlag() {
		return GDR.cbits_to_u16(headerBytes, 4, 0, 1);
	}

	/**
	 * Simple function that returns the packet synchronization flag from the primary header.
	 * 
	 * @return the packet synchronization flag: 0 or 1
	 * 
	 */
	private int getSynchronizationFlag() {
		return GDR.cbits_to_u16(headerBytes, 4, 1, 1);
	}

	/**
	 * Simple function that returns the packet segment length id from the primary header.
	 * 
	 * @return the packet segment length id
	 * 
	 */
	private int getSegmentLengthId() {
		return GDR.cbits_to_u16(headerBytes, 4, 3, 2);
	}

	/**
	 * Simple function that returns the master channel frame count.
	 * @return master channel frame count
	 * 
	 */
	private int getMasterChannelFrameCount() {
		return 0xff & headerBytes[2];
	}

    /**
     * Simple function that returns the operational control field (OCF) flag
     * from the primary header.
     * 
     * @return operational control field flag: 0 or 1
     * 
     */
	private int getOpcntlFlag() {
		return GDR.cbits_to_u16(headerBytes, 0, 15, 1); 
	}

	/**
	 * Simple function that returns the packet order flag from the primary header.
	 * 
	 * @return the packet order flag: 0 or 1
	 * 
	 */
	private int getPacketOrderFlag() {
		return GDR.cbits_to_u16(headerBytes, 4, 2, 1);
	}

}
