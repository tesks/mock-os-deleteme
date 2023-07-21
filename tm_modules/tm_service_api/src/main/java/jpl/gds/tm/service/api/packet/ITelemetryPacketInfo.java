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
package jpl.gds.tm.service.api.packet;

import java.util.List;

import jpl.gds.context.api.filtering.IScidFilterable;
import jpl.gds.context.api.filtering.IStationFilterable;
import jpl.gds.context.api.filtering.IVcidFilterable;
import jpl.gds.dictionary.api.channel.PacketHeaderFieldName;
import jpl.gds.serialization.packet.Proto3TelemetryPacketInfo;
import jpl.gds.shared.template.Templatable;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ILocalSolarTime;
import jpl.gds.shared.time.ISclk;

/**
 * The IPacketInfo interface is to be implemented by all classes that must
 * provide packet metadata to adaptation implementations.
 * <p>
 * <b>THIS IS A MULTI-MISSION CORE ADAPTATION INTERFACE</b>
 * <p>
 * <b>This is a controlled interface. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b>
 * <p>
 * IPacketInfo defines methods needed by the channel extraction and EVR
 * extraction adapters for obtaining metadata about the source data packet.
 * Adaptations should only access Packet Info objects via this interface.
 * Interaction with the actual Packet Info implementation classes in an adapter
 * implementation is contrary to multi-mission development standards.
 * 
 * @version 1.0 - Initial Implementation
 * @version 2.0 - Greatly expanded and reorganized this interface. Cleaned up
 *          javadoc. Changes not individually marked. (MPCS-7289 - 4/30/15)
 * @version 2.1 - Added build and load functions for or creating and parsing
 * 			Proto3TelemetryPacketInfo messages. (MPCS-4599 - 10/30/17)
 * 
 * @see ITelemetryPacketMessage
 */
public interface ITelemetryPacketInfo extends IScidFilterable, IVcidFilterable, IStationFilterable, Templatable {

    /**
     * Gets the packet Application process ID (APID), which identifies
     * the type of data the packet contains.
     * 
     * @return APID number
     */
    int getApid();

    /**
     * Sets the packet Application process ID (APID), which identifies
     * the type of data the packet contains. 
     *
     * @param apid The APID number to set.
     */
    void setApid(final int apid);
    
    /**
     * Sets the APID name, from the APID dictionary, that maps to the
     * packet's APID number. 
     * 
     * @param name the APID name. Will be set to "Unknown" if null.
     */
    void setApidName(String name);

    /**
     * Gets the APID name, from the APID dictionary, that maps to the
     * packet's APID number. 
     * 
     * @return APID name, or "Unknown"
     */
    String getApidName();

    /**
     * Gets the packet sequence counter.
     * 
     * @return source packet sequence counter
     */
    int getSeqCount();

    /**
     * Sets the packet sequence counter.
     *
     * @param seqCount the source sequence count to set
     */
    void setSeqCount(final int seqCount);

    /**
     * Gets the packet spacecraft clock (SCLK), if one exists.
     * 
     * @return packet SCLK, or null if no packet SCLK defined
     */
    ISclk getSclk();

    /**
     * Sets the packet spacecraft clock (SCLK).
     * 
     * @param _sclk SCLK to set
     */
    void setSclk(final ISclk _sclk);

    /**
     * Gets the spacecraft event time (SCET) that maps to the packet SCLK, if
     * one exists.
     * 
     * @return packet SCET, or null if no packet SCET defined
     */
    IAccurateDateTime getScet();
    
    /**
     * Sets the spacecraft event time (SCET) that maps to the packet SCLK.
     * 
     * @param _scet SCET to set
     */
    void setScet(final IAccurateDateTime _scet);

    /**
     * Gets the earth receive time (ERT) of the packet.
     * 
     * @return packet ERT
     */
    IAccurateDateTime getErt();

    /**
     * Sets the earth receive time (ERT) of the packet.
     * 
     * @param ert ERT time to set 
     */
    void setErt(final IAccurateDateTime ert);

    /**
     * Gets the local solar time (LST) that maps to the packet SCLK, if one
     * exists.
     * 
     * @return packet LST, or null if no packet SCLK defined or the current
     *         configuration does not support LST calculation.
     */
    ILocalSolarTime getLst();

    /**
     * Sets the local solar time (LST) that maps to the packet SCLK.
     * 
     * @param _sol LST to set
     */
    void setLst(final ILocalSolarTime _sol);

    /**
     * Gets the total size of the packet, including both primary and 
     * secondary header, in bytes.
     * 
     * @return number of bytes in packet
     */
    int getSize();

    /**
     * Sets the total size of the packet, including the header, in bytes.
     *
     * @param size The size to set.
     */
    void setSize(final int size);

    /**
     * Gets the source virtual channel ID (VCID) on which 
     * the packet arrived.
     * 
     * @return the source VCID; null if not known
     */
    @Override
    Integer getVcid();

    /**
     * Sets the source virtual channel ID (VCID) for this packet.
     * 
     * @param vcid the VCID to set
     */
    void setVcid(final Integer vcid);

    /**
     * Indicates if this is a fill packet that does not contain
     * processable data.
     * 
     * @return true if a fill packet, false if not
     */
    boolean isFill();

    /**
     * Sets the flag indicating this is a fill packet not containing
     * processable data.
     *
     * @param isIdle true to set this as a fill packet, false if not
     */
    void setFill(final boolean isIdle);

    /**
     * Gets the flag indicating whether the packet has a secondary header.
     * 
     * @return true if the packet has a secondary header, false if not
     */
    boolean getSecondaryHeaderFlag();

    /**
     * Sets the secondary header flag, indicating whether the packet has a
     * secondary header. Some missions use different headers for different types
     * of packets. Do not count on all instances of IPacketInfo returning the
     * same value.
     * 
     * @param flag
     *            true if packet has secondary header, false if not
     */
    void setSecondaryHeaderFlag(final boolean flag);

    /**
     * Gets the primary header length for this packet. Note that the default
     * is set from configuration, which will generally be the primary header
     * length for realtime, non-product packets. Some missions use different
     * header lengths for different types of packets. Do not count on all
     * instances of IPacketInfo returning the same value.
     * 
     * @return primary header length in bytes
     */
    int getPrimaryHeaderLength();
    
    /**
     * Sets the primary header length for this packet. Note that the default
     * is set from configuration, which will generally be the primary header
     * length for realtime, non-product packets. 
     * 
     * @param len primary header length in bytes
     */
    void setPrimaryHeaderLength(int len);

    /**
     * Gets the secondary header length for this packet. Note that the default
     * is set from configuration, which will generally be the secondary header
     * length for realtime, non-product packets. Some missions use different
     * header lengths for different types of packets. Do not count on all
     * instances of IPacketInfo returning the same value.
     * 
     * @return secondary header length in bytes
     */
    int getSecondaryHeaderLength();
    
    /**
     * Sets the secondary header length for this packet. Note that the default
     * is set from configuration, which will generally be the secondary header
     * length for realtime, non-product packets. 
     * 
     * @param len secondary header length in bytes
     */
    void setSecondaryHeaderLength(int len);

    /**
     * Gets the segmentation or grouping flags for the packet. Returns
     * 0 if the current packet header types does not support these flags.
     * 
     * @return grouping flags
     */
    byte getGroupingFlags();

    /**
     * Sets the packet segmentation of grouping flags. 
     *
     * @param groupingFlags The grouping flags to set.
     */
    void setGroupingFlags(final byte groupingFlags);

    /**
     * Gets the Data Source Station ID (DSS ID) on which 
     * the packet arrived. Not used for SSE/GSE packets.
     * 
     * @return the source DSS ID; null if not known
     */
    @Override
    Integer getDssId();

    /**
     * Sets the Data Source Station (DSS ID) for this packet. Not used
     * for SSE/GSE packets.
     * 
     * @param dssId to set; null if not known
     */
    void setDssId(final Integer dssId);

    /**
     * Indicates if this is an SSE/GSE packet.
     * 
     * @return true if an SSE packet, false if flight packet.
     */
    boolean isFromSse();

    /**
     * Sets the flag indicating this is an SSE/GSE packet.
     *
     * @param fromSse
     *            true of the packet is an SSE/GSE packet, false for flight
     *            packets
     */
    void setFromSse(final boolean fromSse);

    /**
     * Adds virtual channel frame counter to the list of VCFCs for frames that
     * transported this packet. Not used for SSE/GSE packets.
     * 
     * @param vcfc
     *            virtual channel frame counter to add
     */
    void addSourceVcfc(final long vcfc);
    
    /**
     * Gets the list of source virtual channel frame counters for the
     * frames that transported the packets. May be null of empty if
     * the source frame is unknown or not applicable. Not used
     * for SSE/GSE packets.
     * 
     * @return List of long VCFCs, or null
     */
    List<Long> getSourceVcfcs();
    
    /**
     * Clears the list of source virtual frame counters.
     */
    void clearSourceVcfcs();
    
    /**
     * Gets the receiving bit rate (in bits per second) for this packet,
     * if known.
     * 
     * @return the bitrate or 0.0 if not known
     */
    double getBitRate();
    
	/**
	 * Sets the receiving bit rate (in bits per second) for this packet,
	 * if known.
	 * 
	 * @param rate the bitrate to set
	 */
	void setBitRate(double rate);

	
	/**
	 * Gets the numeric spacecraft ID for this packet. This should
	 * be the spacecraft that originally generated the packet.
	 * 
	 * @return spacecraft ID
	 */
	@Override
    Integer getScid();
	
	/**
	 * Sets the numeric spacecraft ID for this packet. This should
	 * be the spacecraft that originally generated the packet.
	 * 
	 * @param scid spacecraft ID to set
	 */
	void setScid(Integer scid);
	
	/**
	 * Gets the numeric relay spacecraft ID for this packet. This should
	 * be the spacecraft that last relayed the packet to earth from the
	 * source spacecraft. Not used for SSE/GSE packets.
	 * 
	 * @return relay scid
	 */
	int getRelayScid();
	
	/**
     * Sets the numeric relay spacecraft ID for this packet. This should
     * be the spacecraft that last relayed the packet to earth from the
     * source spacecraft. Not used for SSE/GSE packets.
     * 
     * @param scid relay spacecraft ID to set
     */
	void setRelayScid(int scid);
 	
	/**
     * Gets the frame type name for the frame(s) that transported this
     * packet.  This type name must match a frame type in the Transfer
     * Frame Dictionary. Not used for SSE/GSE packets.
     * 
     * @return the frame type name, or null if not applicable
     */
	String getFrameType();
	
	/**
	 * Sets the frame type name for the frame(s) that transported this
	 * packet.  This type name must match a frame type in the Transfer
	 * Frame Dictionary. Not used for SSE/GSE packets.
	 * 
	 * @param type the frame type name
	 */
	void setFrameType(String type);

	/**
     * Gets the packet header version. This is a value placed
     * into the header by standards organizations in order to 
     * distinguish version of the header standard.
     * 
     * @return the packet version; 0 if not applicable 
     */
    byte getPacketVersion();

    /**
     * Sets the packet header version. This is a value placed
     * into the header by standards organizations in order to 
     * distinguish version of the header standard.
     * 
     * @param version the packet version; 0 if not applicable 
     */
    void setPacketVersion(byte version);

    /**
     * Gets the packet header type. This is a value placed
     * into the header by standards organizations in order to 
     * distinguish major type of the packet. It is distinct
     * from APID.
     * 
     * @return the packet type; 0 if not applicable 
     */
    byte getPacketType();

    /**
     * Sets the packet header type. This is a value placed
     * into the header by standards organizations in order to 
     * distinguish major type of the packet. It is distinct
     * from APID.
     * 
     * @param type the packet type; 0 if not applicable 
     */
    void setPacketType(byte type);

    /**
     * Serializes the entire content of the ITelemetryPacketInfo object to a
     * byte array.
     * 
     * @return byte array representing object content
     */
    byte[] toBinary();
    
    /**
     * Serializes the entire content of the ITelemetryPacketInfo object to the
     * given byte array, starting at the given offset, and return the ending
     * offset.
     * 
     * @param buff
     *            byte array to write content to
     * @param startOff
     *            starting offset in the array
     * @return new offset after the write
     */
    int toBinary(byte[] buff, int startOff);
      
    /**
     * Returns the binary size of the current instance, i.e., the length of the
     * array that would be returned by the next toBinary(). call Note that if
     * any attribute in the object changes, the resulting binary length may
     * change, so do not cache this value.
     * 
     * @return byte length required to serialize this instance
     */
    int getBinarySize();
    
    /**
     * De-serializes the entire content of the ITelemetryPacketInfo object from
     * a byte array and sets all object attributes.
     * 
     * @param buff
     *            byte array containing object content
     * @param startOff
     *            starting offset of the ITelemetryPacketInfo data in the given byte
     *            array.
     * @return the next offset in the byte array after parsing          
     */
    int parseFromBinary(byte[] buff, int startOff);
    
    /**
     * Gets a printable string summarizing the packet header content and
     * timestamps. Used for debugging and logging purposes. This method
     * takes time to construct the string, so use it wisely in high-performance
     * situations.
     * 
     * @return printable text describing the packet
     */
    String getIdentifierString();

    /**
     * Returns the value of the indicated header field as an Object, if it
     * exists. Otherwise, returns null. This method may always return null if no
     * header channelization is being performed (is disabled in the
     * configuration file) or the type of packet represented does not contain
     * the requested field.
     * 
     * @param name
     *            the enumeration value for the field to get
     * @return the field value, or null if it does not exist in
     */
    Object getFieldValue(PacketHeaderFieldName name);
    
    /**
	 * Transforms the entire content of the ITelemetryPacketInfo object to a
	 * prototobuf message
	 * 
	 * @return protobuf message representing object content
	 */
    Proto3TelemetryPacketInfo  build();
    
    /**
	 * Transforms this ITelemetryPacketInfo object into the object represented in
	 * the contents of the supplied Protobuf message
	 * 
	 * @param msg a Protobuf message representing an ITelemetryPacketInfo object
	 */
    void load(Proto3TelemetryPacketInfo msg);
    
}
