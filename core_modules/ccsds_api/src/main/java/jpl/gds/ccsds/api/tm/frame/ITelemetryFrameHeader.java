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
package jpl.gds.ccsds.api.tm.frame;

import java.util.List;
import java.util.Optional;

import jpl.gds.dictionary.api.channel.FrameHeaderFieldName;
import jpl.gds.shared.annotation.CustomerAccessible;
import jpl.gds.shared.annotation.Mutator;
import jpl.gds.shared.time.ISclk;

/**
 * The IFrameHeader interface is to be implemented by all classes that provide
 * transfer frame header implementations.
 * <p>
 * <b>THIS IS A MULTI-MISSION CORE ADAPTATION INTERFACE</b>
 * <p>
 * <b>This is a controlled interface. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b>
 * <p>
 * IFrameHeader defines methods needed by the telemetry processing system in
 * order to manipulate frame headers. Frame header content and format may vary
 * from mission to mission. For this reason, IFrameHeader instances should
 * always be created using the FrameHeaderFactory, and should always be accessed
 * using this interface. Interaction with the actual Frame Header implementation
 * is contrary to multi-mission development standards.
 * <p>
 * For further information on transfer frame headers and field definitions,
 * refer to the CCSDS AOS Space Data Link Protocol 732.0-B-1 Blue Book for AOS
 * V2 frames, or the CCSDS TM Space Data Link Protocol 132.0-B-1 Blue Book for
 * AOS V1 frames.
 *
 * 
 * @see TelemetryFrameHeaderFactory
 */
@CustomerAccessible(immutable = false)
public interface ITelemetryFrameHeader {
   
    /**
     * Constant returned for an undefined or un-populated integer frame header field.
     */
    public static final int UNDEFINED_FIELD = -1;
    /** Maximum Frame body size in bytes */
    public static final int MAX_FRAME  = 6717;

    /**
     * Determines if the current frame should be skipped for further processing,
     * i.e, a frame containing un-processable or dead data. Such frames will be
     * counted, but not processed by the downlink processor, and never stored in
     * the database.
     * 
     * @param buff
     *            the buffer containing the data for the entire frame, including
     *            header
     * @param off
     *            the starting offset of the frame header in the buffer
     * @return true if the frame is a dead code frame; false if not
     */
	public boolean skipContent(byte[] buff, int off);

	/**
	 * Loads the frame header fields from a byte buffer.
	 * 
	 * @param buff
	 *            the buffer containing the frame header data; this buffer is
	 *            not guaranteed to contain the entire frame.
	 * @param off
	 *            the starting offset of the frame header in the buffer
	 * @return the offset of the data following the frame header in the buffer
	 */
    @Mutator
	public int load(byte[] buff, int off);

	/**
	 * Dumps frame header values to the console.
	 * Used for debug output.
	 */
	public void dump();

	/**
	 * Gets the spacecraft ID from the header. Spacecraft
	 * IDs are CCSDS-assigned.
	 * 
	 * @return the numeric scid
	 */
	public int getScid();

	/**
	 * Gets the virtual channel frame counter from the header.
	 * 
	 * @return the VCFC
	 */
	public int getVirtualChannelFrameCount();

	/**
	 * Gets the data pointer offset, which indicates to extraction services
	 * how to locate and process data in the frame. For some frames, this may be
	 * a pointer to the first packet header byte within the frame
	 * data area. For others, it may be a pointer to the last valid byte in the
	 * frame data area.
	 * 
	 * @return byte offset
	 * 
	 */
	public int getDataPointer();

	/**
	 * Gets the virtual channel ID from the frame header.
	 * 
	 * @return the VCID
	 */
	public int getVirtualChannelId();

	/**
	 * Gets the value of the version field in the header. Frame header types
	 * that do not have this field should return 0. It should not be assumed
	 * that this is the CCSDS AOS frame version, since the frame format in use may
	 * not be AOS.
	 * 
	 * @return the version
	 * 
	 * @deprecated This value is not used by AMPCS
	 * 
	 */
	@Deprecated
	public int getVersion();

	/**
	 * Indicates if the frame is an idle frame according
	 * to the header. Data in these frames will not be processed, 
	 * and will not be stored in the AMPCS database unless the default
	 * configuration is modified.
	 * 
	 * @return true if the frame is an idle frame, false if not
	 */
	public boolean isIdle();

	/** 
	 * Gets the maximum possible sequence counter (VCFC) for this frame, , i.e,
	 * the maximum sequence count before the value rolls over.
	 * @return the max VCFC
	 * 
	 */
	public abstract int getMaxSeqCount();
	
	/**
	 * Gets the byte array representing the primary frame header.
	 * 
	 * @return byte[], never null
	 * 
	 */
	public byte[] getPrimaryHeaderBytes();
	
	/**
     * Gets the byte array representing the secondary frame header.
     * 
     * @return Optional byte[], or an empty optional if no secondary header found
     * 
     */
	public Optional<byte[]> getSecondaryHeaderBytes();
	
	/**
     * Gets the byte array representing the entire frame header.
     * 
     * @return byte[], never null
     * 
     */
	public byte[] getAllHeaderBytes();
	
	/**
	 * Indicates whether this frame contains data that is a continuation of
	 * a packet or PDU from a previous frame, and contains no new packet
	 * or PDU headers.
	 * 
	 * @return true if the header indicates this is a continuation frame, false if not
	 * 
	 */
	public boolean isContinuation();
	
	/**
	 * Gets the hardware/spacecraft time code from the frame header.
	 * 
	 * @return ISclk if one exists, empty Optional if not
	 * 
	 */
    public Optional<ISclk> getTimecode();

	/**
	 * Returns the value of the indicated field as an Object, if it exists.
	 * Otherwise, returns null. This method is used only for channelization of
	 * frame header fields.
	 * 
	 * @param name
	 *            the enumeration value for the field to get
	 * @return the field value, or null if it does not exist in this version of
	 *         the frame header
	 */
	public Object getFieldValue(FrameHeaderFieldName name);
	
	/**
	 * Sets the list of VCIDs to consider as idle.
	 * 
	 * @param vcidsToSet List of Integer vcids
	 */
    @Mutator
	public void setIdleVcids(List<Integer> vcidsToSet);
}
