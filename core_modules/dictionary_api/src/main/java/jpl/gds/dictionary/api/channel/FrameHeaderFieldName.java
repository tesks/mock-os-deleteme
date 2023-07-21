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
package jpl.gds.dictionary.api.channel;

/**
 * FrameHeaderFieldName enumerates the different fields found in transfer frame
 * headers that can be used to populate header channels, as defined in 
 * a header channel dictionary.
 * <br>
 * <br><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br><br>
 * 
 * FrameHeaderFieldName enumerates the different fields found in frame headers.
 * The enumeration names must not be carelessly modified, as the header channel
 * dictionary relies on these names. If modification is needed,
 * header_channel.rnc and all header-channel dictionaries that depend on it must
 * subsequently be changed, as well.
 * <p>
 * For further information on transfer frame headers and field definitions,
 * refer to the CCSDS AOS Space Data Link Protocol 732.0-B-1 Blue Book for AOS
 * V2 frames, or the CCSDS TM Space Data Link Protocol 132.0-B-1 Blue Book for
 * TM V1 frames.
 * 
 */
public enum FrameHeaderFieldName {

	/** 
	 * CCSDS transfer frame version number: 1 or 2. Note that this value
	 * is 1 + the actual value in the frame header.
	 */
	TRANSFER_FRAME_VERSION_NUMBER,

	/** Numeric spacecraft ID. */
	SPACECRAFT_ID,

	/** Virtual channel identifier. */
	VCID,

	/** Virtual channel frame counter. */
	VCFC,

	/** 
	 * Frame replay flag, from the CCSDS AOS V2 header. N/A for
	 * CCSDS TM V1 frames.
	 */
	REPLAY_FLAG,

	/** 
	 * Virtual channel frame count use flag in the
	 * CCSDS AOS V2 frame header. N/A for CCSDS TM V1 frames. 
	 */
	VC_FRAME_COUNT_USAGE_FLAG,

	/** 
	 * Virtual channel frame cycle use flag in the
	 * CCSDS AOS V2 frame header. N/A for CCSDS TM V1 frames. 
	 */
	VC_FRAME_COUNT_CYCLE,

	/** 
	 * In CCSDS TM V1 frames, this is the operational control
	 * field (OCF) flag, indicating whether there is an 
	 * operational control field present in the frame trailer. 
	 * N/A to CCSDS AOS V2 transfer frames. 
	 */
	OPERATIONAL_CONTROL,

	/** 
	 * Master channel identifier. In both CCSDS TM V1 and AOS V2 frames, this
	 * is a compound field consisting of both transfer frame version number 
	 * and spacecraft ID. 
	 */
	MCID,

	/**
	 * Master channel frame count for CCSDS TM V1 frames.
	 * N/A for CCSDS AOS V2 transfer frames. 
	 */
	MFC,

	/**
	 * Secondary header presence flag, from the transfer frame data status field
	 * for CCSDS TM V1 frames. N/A for CCSDS AOS V2 transfer frames.
	 */
	SECONDARY_HEADER_FLAG,
	
	/**
     * Secondary header ID, from the secondary header of 
     * CCSDS TM V1 frames. N/A for CCSDS AOS V2 transfer frames.
     * 
     */
    SECONDARY_HEADER_ID,

    /**
     * Secondary header length, from the secondary header of 
     * CCSDS TM V1 frames. N/A for CCSDS AOS V2 transfer frames.
     * 
     */
    SECONDARY_HEADER_LENGTH,

	/**
	 * Packet sequence flag. Left here for backward compatibility
	 * of header channel dictionaries. In truth, there has never been any
	 * "packet sequence" flag in either of the CCSDS frame headers! The 
	 * correct field is PACKET_SYNC_FLAG.
	 * 
	 */
	@Deprecated
	PACKET_SEQ_FLAG,
	
    /**
     * Packet synchronization flag, from the transfer frame data status field of
     * CCSDS TM V1 frames. N/A for CCSDS AOS V2 transfer frames.
     * 
     */
	PACKET_SYNC_FLAG,

	/**
	 * Packet order flag, from the transfer frame data status field for AOS
	 * V1 frames. N/A for AOS V2 transfer frames.
	 */
	PACKET_ORDER,

	/**
	 * Packet segment length identifier, from the transfer frame data status
	 * field for CCSDS TM V1 frames. N/A for CCSDS AOS V2 transfer frames.
	 */
	PACKET_SEGMENT_LENGTH_ID,

    /**
     * First packet header pointer. In CCSDS TM V1 frames, this comes from the
     * data field status in the primary header and is the pointer (offset of)
     * the first complete packet header in the frame. In CCSDS AOS V2 frames
     * which contain an M_PDU in the data area, it service the same purpose, but
     * comes from the M_PDU header.
     */ 
	FIRST_PACKET_POINTER,
	
    /**
     * Bitstream data pointer. For CCSDS AOS V2 frames which contain a B_PDU in
     * the data area, it is the pointer (offset of) the last valid byte in the frame
     * data. For CCSDS AOS frames that contain an M_PDU, and for CCSDS TM V1
     * frames, it is N/A.
     * 
     */
	BITSTREAM_DATA_POINTER,
	
    /**
     * Data field status. For CCSDS TM V1 frames, this is the complete data
     * field status from the primary header. For CCSDS AOS V2 frames, it is N/A.
     * 
     */
	DATA_FIELD_STATUS,
	
	/**
     * Signaling field. For CCSDS AOS V2 frames, this is the complete signaling
     * field from the primary header. For CCSDS TM V1 frames, it is N/A.
     * 
     */
	SIGNALING_FIELD,
	
	/**
	 * Header error control field, or FHECF. For CCSDS AOS V2 frames, this is
	 * an optional field in the primary header. For CCSDS TM V1 frames, it is N/A.
	 * 
	 */
	HEADER_ERROR_CONTROL,
	
	/**
	 * Optional hardware or spacecraft timecode from the frame header.
	 * 
	 */
	TIMECODE;
}
