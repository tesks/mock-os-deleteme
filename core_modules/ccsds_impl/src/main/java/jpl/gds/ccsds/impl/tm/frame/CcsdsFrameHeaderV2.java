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
import jpl.gds.dictionary.api.config.IFrameFormatDefinition;
import jpl.gds.dictionary.api.frame.IFrameTimeFieldDefinition;
import jpl.gds.dictionary.api.frame.ITransferFrameDefinition;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.ISclkExtractor;

/**
 * CcsdsFrameHeaderV2 represents the fields in a CCSDS version 2 primary AOS
 * frame header as described by CCSDS 732.0-2. It is capable of parsing the 
 * header from data bytes.
 * <p>
 * Additional public methods should NOT be implemented in this class without
 * consulting the CogE. All use of this class must go through the IFrameHeader
 * interface, or the frame adaptation will break.
 * <br><br>
 * A CCSDS V2 (AOS) frame consists of a 6 or 8 byte primary header:<br>
 *     Bits 0-1:   Frame version number <br>
 *     Bits 2-9:   Spacecraft ID<br>
 *     Bits 10-15: Virtual Channel Identifier<br>
 *     Bits 16-39: Virtual Channel Frame Count<br>
 *     Bits 40-47: Signaling Field<br>
 *     Bits 48-63: Optional Frame Header Error Control <br>
 * <br><br>
 * This is followed by an optional frame insert zone, the data area,
 * an optional 32-bit operational control field, and an optional 
 * 16-bit FECF. For the purposes of the IFrameHeader interface, the insert zone
 * is treated as a secondary header. 
 * <br><br>
 *  Two types of content are accepted in the frame data area: a CCSDS M_PDU and a CCSDS 
 *  B_PDU. Format CCSDS_1_AOS_MPDU is assumed to contain a CCSDS M_PDU header as the first 
 *  item in the frame data area. This header is assumed to consist of:<br>
 *     Bits 0-4:   Spare<br>
 *     Bits 5-15:  First Header Pointer<br>
 * <br><br>
 * Format CCSDS_1_AOS_BPDU is assumed to contain a CCSDS B_PDU header as the first
 * item in the frame data area. This header is assumed to consist of:<br>
 *     Bits 0-1:   Spare<br>
 *     Bits 2-15:  Bit Stream Data Pointer <br>
 * <br>
 * If an instance is created without any associated ITransferFrameDefinition,
 * optional fields are assumed to not exist.    
 *
 */
class CcsdsFrameHeaderV2 implements ITelemetryFrameHeader {
	/** The total header size in bytes. */
	private static final int PRIMARY_HEADER_SIZE = 6;

	private ITransferFrameDefinition formatDef;
    private byte[] headerBytes;
    
	private List<Integer> idleVcids = new LinkedList<Integer>();

	/**
	 * Creates an instance of CcsdsFrameHeaderV2.
	 */
	protected CcsdsFrameHeaderV2() {
		// do nothing
	}
	
	/**
     * Creates an instance of CcsdsFrameHeaderV2.
     * 
     * @param def the ITransferFrameDefinition of the frame from the frame dictionary
     */
	protected CcsdsFrameHeaderV2(final ITransferFrameDefinition def) {
	    this.formatDef = def;
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
		final long tf = GDR.get_u16(buff, off);
		if (tf == 0xdead) {
			return true;
		} 
		final long v1 = GDR.get_u32(buff, off);
		final long v2 = GDR.get_u32(buff, off+4);
		if (v1 == v2) {
		    return true;
		}
		
		/* Also must skip content if there is no PDU header, as we currently can
		 * do nothing with it.
		 */
		if (formatDef == null || formatDef.hasPduHeader() == false) {
		    return true;
		}
		return false;
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
        
		return inOffset + neededLen;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.ccsds.api.tm.frame.ITelemetryFrameHeader#dump()
	 */
	@Override
	public void dump() {
	    final StringBuilder sb = new StringBuilder(256);
	    sb.append("--CCSDS AOS Transfer Frame--");
	    sb.append("mcid=" + getMasterChannelId() + "\n");
	    sb.append("  : scid=" + getScid() + "\n");
	    sb.append("  : version=" + getVersion() +"\n");
	    sb.append("vcid=" + getVirtualChannelId() + "\n");
	    sb.append("vcfc=" + getVirtualChannelFrameCount() + "\n");
	    sb.append("signaling field " + Integer.toHexString(getSignalingField()) + "\n");
	    sb.append("  : replay flag=" + getReplayFlag() +"\n");
	    sb.append("  : vcfc cycle usage flag=" + getVcfcCycleUsageFlag() +"\n");
	    sb.append("  : vcfc cycle=" + getVcfcCycle() +"\n");
	    if (formatDef != null && formatDef.hasHeaderErrorControl()) {
	        sb.append("fhec=" + getHeaderErrorControl());
	    }
	    if (formatDef != null || formatDef.getFormat().getType() == IFrameFormatDefinition.TypeName.CCSDS_AOS_2_MPDU) {
	        sb.append("M_PDU fhp=" + getDataPointer());
	    } else if (formatDef != null || formatDef.getFormat().getType() == IFrameFormatDefinition.TypeName.CCSDS_AOS_2_BPDU) {
	        sb.append("B_PDU bdp=" + getDataPointer());
	    }
	    System.out.println(sb);
	}
	
    /**
     * {@inheritDoc}
     * @see jpl.gds.ccsds.api.tm.frame.ITelemetryFrameHeader#getScid()
     */
    @Override
    public int getScid() {
    	return GDR.cbits_to_u16(headerBytes, 0, 2, 8);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.ccsds.api.tm.frame.ITelemetryFrameHeader#getVirtualChannelFrameCount()
     */
    @Override
    public int getVirtualChannelFrameCount() {
    	return GDR.get_u24(headerBytes, 2);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.ccsds.api.tm.frame.ITelemetryFrameHeader#getDataPointer()
     */
    @Override
    public int getDataPointer() {
        if (formatDef == null) {
            throw new IllegalStateException("Attempting to fetch data pointer from CCSDS AOS frame header, but frame format is null");
        }
        int offset = PRIMARY_HEADER_SIZE;
        if (formatDef.hasHeaderErrorControl()) {
            offset += formatDef.getHeaderErrorControlSizeBytes();
        }
        if (this.formatDef.hasSecondaryHeader()) {
            offset += formatDef.getSecondaryHeaderSizeBytes();
        }
        if (this.formatDef.getFormat().getType() == IFrameFormatDefinition.TypeName.CCSDS_AOS_2_MPDU) {
            return GDR.cbits_to_u16(headerBytes, offset, 5, 11) & 0x7ff;
        }
        else if (this.formatDef.getFormat().getType() == IFrameFormatDefinition.TypeName.CCSDS_AOS_2_BPDU) {
            return GDR.cbits_to_u16(headerBytes, offset, 2, 14) & 0x3fff;
        }
        throw new UnsupportedOperationException("Attempting to fetch data pointer from CCSDS TM frame header, "
                + "but data pointer is undefined for the current frame format:" + this.formatDef.getFormat().getType());
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.ccsds.api.tm.frame.ITelemetryFrameHeader#getVirtualChannelId()
     */
    @Override
    public int getVirtualChannelId() {
    	return GDR.cbits_to_u16(headerBytes, 1, 2, 6);
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
        if (formatDef != null) {
            if (formatDef.getFormat().getType() == IFrameFormatDefinition.TypeName.CCSDS_AOS_2_MPDU) {
                return getDataPointer() == 0x7fe || idleVcids.contains(getVirtualChannelId());
            } else {
                return getDataPointer() == 0x3ffe || idleVcids.contains(getVirtualChannelId());
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.ccsds.api.tm.frame.ITelemetryFrameHeader#isContinuation()
     */
    @Override
    public boolean isContinuation() {
        if (formatDef != null) {
            if (formatDef.getFormat().getType() == IFrameFormatDefinition.TypeName.CCSDS_AOS_2_MPDU) {
                return getDataPointer() == 0x7ff;
            } else {
                return getDataPointer() == 0x3fff;
            }
        }
        return false;
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
     * @see jpl.gds.ccsds.api.tm.frame.ITelemetryFrameHeader#getMaxSeqCount()
     */
    @Override
    public int getMaxSeqCount() {
    	return 0xFFFFFF;
    }


    /**
     * {@inheritDoc}
     * @see jpl.gds.ccsds.api.tm.frame.ITelemetryFrameHeader#getFieldValue(jpl.gds.dictionary.impl.impl.api.channel.FrameHeaderFieldName)
     */
    @SuppressWarnings("deprecation")
	@Override
    public Object getFieldValue(final FrameHeaderFieldName field) {
    	switch (field) {
    	case TRANSFER_FRAME_VERSION_NUMBER: return getVersion() + 1;
    	case SPACECRAFT_ID: return getScid();
    	case VCID: return getVirtualChannelId();
    	case VCFC: return getVirtualChannelFrameCount();
    	case REPLAY_FLAG: return getReplayFlag();
    	case VC_FRAME_COUNT_USAGE_FLAG: return getVcfcCycleUsageFlag();
    	case VC_FRAME_COUNT_CYCLE: return getVcfcCycle();
    	case OPERATIONAL_CONTROL: return null;
    	case MCID: return getMasterChannelId();
    	case MFC: return null;
    	case SECONDARY_HEADER_FLAG: return null;
    	case PACKET_SEQ_FLAG: return null;
    	case PACKET_ORDER: return null;
    	case PACKET_SEGMENT_LENGTH_ID: return null;
    	case FIRST_PACKET_POINTER: 
    	    if (this.formatDef != null && this.formatDef.getFormat().getType() == IFrameFormatDefinition.TypeName.CCSDS_AOS_2_MPDU) {
    	        return getDataPointer();
    	    } else {
    	        return null;
    	    }
        case BITSTREAM_DATA_POINTER:
            if (this.formatDef != null && this.formatDef.getFormat().getType() == IFrameFormatDefinition.TypeName.CCSDS_AOS_2_BPDU) {
                return getDataPointer();
            } else {
                return null;
            }
        case DATA_FIELD_STATUS: return null;
        case PACKET_SYNC_FLAG: return null;
        case SECONDARY_HEADER_ID: return null;
        case SECONDARY_HEADER_LENGTH: return null;
        case SIGNALING_FIELD: return getSignalingField();
        case HEADER_ERROR_CONTROL: return getHeaderErrorControl();
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
    	int size = PRIMARY_HEADER_SIZE;
    	if (formatDef != null && formatDef.hasHeaderErrorControl()) {
    		size += formatDef.getHeaderErrorControlSizeBytes();
    	}
    	final byte[] result = new byte[size];
    	System.arraycopy(headerBytes, 0, result, 0, size);
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
        int primaryLen = PRIMARY_HEADER_SIZE;
        if (formatDef != null && formatDef.hasHeaderErrorControl()) {
            primaryLen += formatDef.getHeaderErrorControlSizeBytes();
        }
        final byte[] result = new byte[formatDef.getSecondaryHeaderSizeBytes()];
        if (headerBytes != null) {
            System.arraycopy(headerBytes, primaryLen, result, 0, formatDef.getSecondaryHeaderSizeBytes());
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
     * Gets the master channel ID from the primary frame header.
     * 
     * @return master channel ID
     */
    private int getMasterChannelId() {
        return GDR.cbits_to_u16(headerBytes, 0, 0, 10);
    }

	/**
	 * Returns the value of the signaling field.
	 * 
	 * @return signaling value
	 */
	private int getSignalingField() {
	    return GDR.get_u8(headerBytes, 5);
	}

	/**
	 * Returns the value of the replay flag from the header.
	 * @return the replay flag
	 * 
	 */
	private int getReplayFlag() {
		return GDR.cbits_to_u8(headerBytes, 5, 0, 1);
	}

	/**
	 * Get the frame counter cycle usage flag.
	 * @return 0 if cycle not used, non-zero if used
	 * 
	 */
	private int getVcfcCycleUsageFlag() {
		return  GDR.cbits_to_u8(headerBytes, 5, 1, 1);
	}

	/**
	 * Gets the frame counter cycle number.
	 * @return the frame counter cycle value
	 * 
	 */
	private int getVcfcCycle() {
		return GDR.cbits_to_u8(headerBytes, 5, 4, 4);
	}

	/**
	 * Gets the header error control field (FHECF). 
	 * 
	 * @return value of the FHECF, or UNDEFINED_FIELD if there is no
	 *         associated frame definition or the field is not present
	 */
	private int getHeaderErrorControl() {
	    if (formatDef != null && formatDef.hasHeaderErrorControl()) {
            return GDR.get_u16(headerBytes, 6);
        } else {
            return UNDEFINED_FIELD;
        }
	}
}
