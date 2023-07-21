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
package jpl.gds.ccsds.api.packet;

import jpl.gds.shared.annotation.CustomerAccessible;
import jpl.gds.shared.annotation.Mutator;

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
 * The ISpacePacketHeader interface is to be implemented by all classes that provide
 * packet header implementations.
 * <p>
 * <b>THIS IS A MULTI-MISSION CORE ADAPTATION INTERFACE</b>
 * <p>
 * <b>This is a controlled interface. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b>
 * <p>
 * ISpacePacketHeader defines methods needed by the telemetry processing system in
 * order to manipulate packet headers. Packet header content and format may vary
 * from mission to mission. For this reason, ISpacePacketHeader instances should
 * always be created using the PacketHeaderFactory, and should always be
 * accessed using this interface. Interaction with the actual Packet Header
 * implementation is contrary to multi-mission development standards.
 * <p>
 * For further information on space packet headers and field definitions, refer
 * to the CCSDS AOS Space Packet Protocol 133.0-B-1 Blue Book.
 *
 * 
 * @see PacketHeaderFactory
 */
@CustomerAccessible(immutable = false)
public interface ISpacePacketHeader {
    /** Maximum allowable APID. */
    public static final int MAX_DATA_APID = 2039;
    
    /**
     * Increased to 65542 from 65536. A space packet
     *     consists of 6 header bytes and 1 - 65536 packet data bytes.
     */
    /** Maximum packet size in bytes. */
    public static final int MAX_PACKET = 65542;

    /** Maximum allowable grouping flags value. */
    public static final short MAX_GROUPING_FLAGS = 3;

    /** Maximum allowable version value. */
    public static final short MAX_VERSION = 7;
    
    /** Default primary header length */
    public static final int DEFAULT_PRIMARY_HEADER_LENGTH = 6;

    /**
     * Determine if the packet header fields say this is a valid packet header
     * 
     * @return True if the packet header is valid, false otherwise
     */
    public boolean isValid();

    /**
     * Gets the packet APID number.
     * 
     * @return APID number.
     */
    public short getApid();

    /**
     * Sets the packet APID number.
     * 
     * @param apid
     *            The APID number to set.
     */
    @Mutator
    public void setApid(final short apid);

    /**
     * Gets the packet data length in bytes (excluding primary header bytes),
     * LESS ONE, per the CCSDS standard.
     * 
     * @return packet data length
     */
    public int getPacketDataLength();

    /**
     * Sets the packet data length in bytes (excluding primary header bytes).
     * Should be a LESS ONE value.
     * 
     * @param packetDataLength
     *            The length to set
     */
    @Mutator
    public void setPacketDataLength(final int packetDataLength);

    /**
     * Indicates if the packet has a secondary header.
     * 
     * @return true if secondary header present, false if not
     */
    public byte getSecondaryHeaderFlag();

    /**
     * Sets the flag that indicates if the packet has a secondary header.
     * 
     * @param secondaryHeaderFlag
     *            true if secondary header present, false if not
     */
    @Mutator
    public void setSecondaryHeaderFlag(final byte secondaryHeaderFlag);

    /**
     * Gets the packet grouping flags from the header. For CCSDS space packets,
     * per the CSSDS standard: CONTINUING_GROUP = 0, FIRST_IN_GROUP = 1,
     * LAST_IN_GROUP = 2; NOT_IN_GROUP = 3. Other packet types may use this
     * field differently or not use it at all.
     * 
     * @return packet grouping flags
     */
    public byte getGroupingFlags();

    /**
     * Sets the packet grouping flags.. For CCSDS space packets, per the CSSDS
     * standard: CONTINUING_GROUP = 0, FIRST_IN_GROUP = 1, LAST_IN_GROUP = 2;
     * NOT_IN_GROUP = 3. Other packet types may use this field differently or
     * not use it at all.
     * 
     * @param segmentFlags
     *            The grouping flags to set.
     */
    @Mutator
    public void setGroupingFlags(final byte segmentFlags);

    /**
     * Gets the source packet sequence counter.
     * 
     * @return sequence number
     */
    public short getSourceSequenceCount();

    /**
     * Sets the the source packet sequence counter.
     * 
     * @param sourceSequenceCount
     *            the sequence count to set
     */
    @Mutator
    public void setSourceSequenceCount(final short sourceSequenceCount);

    /**
     * Gets the packet version number. In CCSDS packets this is supposed to be
     * 0, but may be used different;y or not at all by other packet types. AMPCS
     * will currently only extract packets with version 0 unless the isValid()
     * method is overridden by the packet adaptation to allow other values.
     * 
     * @return version number
     */
    public byte getVersionNumber();

    /**
     * Sets the packet version number. In CCSDS packets this is supposed to be
     * 0. AMPCS will currently only extract packets with version 0 unless the
     * isValid() method is overridden to allow other values.
     * 
     * @param versionNumber
     *            The version to set.
     */
    @Mutator
    public void setVersionNumber(final byte versionNumber);

    /**
     * Indicates if this is a fill packet containing data not to be processed.
     * Fill packets will not be stored in the database unless the default AMPCS
     * configuration is modified.
     * 
     * @return true if the packet is fill, false if not
     */
    public boolean isFill();

    /**
     * Loads the primary header fields in the packet header from a byte array.
     * 
     * @param buff
     *            The byte array to load the packet from
     * 
     * @param off
     *            The offset into the byte array where the reading should begin
     * 
     * @return The offset into the byte array pointing past the packet
     */
    @Mutator
    public int setPrimaryValuesFromBytes(final byte[] buff, int off);

    /**
     * Extract just the APID from the given buffer containing primary header bytes.
     * 
     * @param buff
     *            Byte buffer
     * @param off
     *            Offset within buffer
     * 
     * @return packet APID
     */
    public int getApidFromBytes(final byte[] buff, final int off);

    /**
     * Gets the length in bytes of the primary packet header.
     * 
     * @return primary header length
     */
    public int getPrimaryHeaderLength();

    /**
     * Gets the maximum allowable APID for this header type.
     * 
     * @return maximum APID number
     */
    public int getMaxAllowableApid();

    /**
     * Get the byte array representation of the primary packet header,
     * in GDR (big-endian) order.
     * 
     * @return The byte array representation of this packet header
     */
    public byte[] getBytes();

    /**
     * Gets the packet type. In CCSDS space packets containing telemetry this
     * value must be 0, and for packets containing command information it must
     * be 1. AMPCS will currently only extract data from packets with version 0
     * unless the isValid() method is overridden to allow other values.
     * 
     * @return the packetType.
     */
    public byte getPacketType();

    /**
     * Sets the packet type. In CCSDS space packets containing telemetry this
     * value must be 0, and for packets containing command information it must
     * be 1. AMPCS will currently only extract data from packets with version 0
     * unless the isValid() method is overridden to allow other values.
     * 
     * @param packetType
     *            The type to set.
     */
    @Mutator
    public void setPacketType(final byte packetType);

    /**
     * Gets the maximum data length value for this packet header.
     * 
     * @return max data length
     */
    public int getMaxPacketDataLength();

    /**
     * Gets the maximum packet sequence number value for this packet header.
     * 
     * @return max packet sequence number
     */
    public int getMaxSequenceNumber();

    /**
     * Sets both primary and secondary header fields from the given byte array
     * at the given offset. This method must check to see if the buffer is long
     * enough to contain the secondary header and should not fail if it is not
     * long enough.
     * 
     * @param buff
     *            the packet data buffer
     * @param off
     *            the starting offset of the primary packet header in the buffer
     */
    @Mutator
    public void setHeaderValuesFromBytes(byte[] buff, int off);

    /**
     * Sets a boolean flag which indicates whether or not IDLE packets are
     * allowed to contain a secondary header. CCSDS Space Packet Protocol
     * recommends Idle packets to not have a secondary header but mission
     * like MSL and M20 deviate from this recommendation.
     *
     * @param idlePacketSecondaryHeaderAllowed true if secondary header is allowed, false otherwise
     */
    @Mutator
    public void setIdlePacketSecondaryHeaderAllowed(final boolean idlePacketSecondaryHeaderAllowed);

    /**
     * Gets the reason why packet header validation failed.
     *
     * @return invalid packet header reason
     */
    public String getInvalidReason();
}
