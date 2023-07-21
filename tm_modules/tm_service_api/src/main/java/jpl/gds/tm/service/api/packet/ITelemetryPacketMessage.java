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

import jpl.gds.common.types.IFillSupport;
import jpl.gds.context.api.filtering.IScidFilterable;
import jpl.gds.context.api.filtering.IStationFilterable;
import jpl.gds.context.api.filtering.IVcidFilterable;
import jpl.gds.serialization.packet.Proto3TelemetryPacketMessage;
import jpl.gds.shared.holders.FrameIdHolder;
import jpl.gds.shared.holders.HeaderHolder;
import jpl.gds.shared.holders.PacketIdHolder;
import jpl.gds.shared.holders.TrailerHolder;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.station.api.dsn.chdo.IChdoSfdu;


/**
 * The ITelemetryPacketMessage interface is to be implemented by all classes that must
 * provide packet message information to adaptation implementations.
 * <p>
 * <b>THIS IS A MULTI-MISSION CORE ADAPTATION INTERFACE</b>
 * <p>
 * <b>This is a controlled interface. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b>
 * <p>
 * IPacketMessage defines methods needed by downstream telemetry processing 
 * components for obtaining both packet data and metadata about the
 * packet. Adaptations should only access Packet Message objects via this
 * interface. Interaction with the actual Packet Message implementation classes
 * in an adapter implementation is contrary to multi-mission development
 * standards.
 * 
 * @version 1.0 - Initial Implementation
 * @version 2.0 - Added many more methods to bring this up to date with
 *                the current packet message implementation. Javadoc cleanup.
 * @version 3.0 - Added getFrameId MPCS-5932 09/02/15
 * 
 * @see ITelemetryPacketInfo
 */
public interface ITelemetryPacketMessage extends IMessage, IFillSupport, IScidFilterable, IVcidFilterable, IStationFilterable {

    /**
     * Gets the IChdoSfdu object associated with this packet, if any. This object
     * is available only during telemetry ingestion. If the message has been transmitted
     * via the message service, this object will be null.
     * 
     * @return IChdoSfdu 
     */
    IChdoSfdu getChdoObject();
    
    /**
     * Sets the IChdoSfdu object associated with this packet, if any.
     * 
     * @param chdo IChdoSfdu object that arrived with the packet 
     */
    void setChdoObject(IChdoSfdu chdo);
    
    /**
     * Gets the packet metadata object from this packet message.
     * 
     * @return IPacketInfo object containing packet metadata
     */
    ITelemetryPacketInfo getPacketInfo();

    /**
     * Gets the whole packet body, including primary and secondary header bytes, 
     * but less any station header or trailer.
     * 
     * from this IPacketMessage.
     * 
     * @return byte array of packet data; may be null if not yet set
     */
    byte[] getPacket();
    
    /**
     * Gets the number of bytes in the whole packet body, including 
     * primary and secondary header bytes, but not including any
     * station header or trailer.
     * 
     * @return number of bytes
     */
    int getNumBytes();
    
    /**
     * Gets the Record Creation Time (RCT) of this packet message.
     * 
     * @return the RCT time
     */
    IAccurateDateTime getRct();
    
    /**
     * Sets the Record Creation Time (RCT) for this packet message.
     * 
     * @param rct the time to set
     */
    void setRct(IAccurateDateTime rct);
    
    /**
     * Gets the downlink-assigned packet ID. The packet ID will be unique
     * among all FSW packets processed by the same downlink session, or 
     * unique among all SSE/GSE packets processed by the current session.
     * 
     * @return PacketIdHolder; never null
     */
    PacketIdHolder getPacketId();

    /**
     * Gets the downlink-assigned frame ID. If this Packet came from a Frame,
     * this is the database id assigned to the first Frame that contained
     * any of this packet. Otherwise, the value is never null, but rather
     * unsupported, which goes into the database as a NULL. Always unsupported
     * for SsePacket.
     * 
     * @return FrameIdHolder; never null
     *
     */
    FrameIdHolder getFrameId();

    /**
     * Sets the bytes for the entire packet body, including 
     * primary and secondary header bytes, but not including any
     * station header or trailer. ALL implementations of this
     * method MUST copy the input data.
     * 
     * @param packetBytes the array of bytes to set
     * @param offset the starting offset of the data in the input array
     * @param numBytes the number of bytes in the packet body
     */
    void setPacket(byte[] packetBytes, int offset, int numBytes);

    /**
     * Sets the bytes for the entire packet body, including primary and
     * secondary header bytes, but not including any station header or trailer.
     * ALL implementations of this method MUST copy the input data.
     * 
     * @param packetBytes
     *            the array of bytes to set
     * @param numBytes
     *            the number of bytes in the packet body, which may be less than
     *            the size of the input array.
     */
    void setPacket(byte[] packetBytes, int numBytes);

    /**
     * Gets the HeaderHolder, which contains any bytes that preceded the
     * primary packet header, usually pre-prepended by a station, but could 
     * also be a flight or ground processing artifact.
     * 
     * @return HeaderHolder object; never null, but will be 
     *         HeaderHolder.NULL_HOLDER if there were no header bytes.
     */
    HeaderHolder getHeader();

    /**
     * Gets the TrailerHolder, which contains any bytes that followed the
     * packet body, usually appended by a station, but could 
     * also be a flight or ground processing artifact.
     * 
     * @return TrailerHolder object; never null, but will be 
     *         TrailerHolder.NULL_HOLDER if there were no trailer bytes.
     */
    TrailerHolder getTrailer();
    
	/**
	 * Transforms the content of the ITelemetryPacketMessage object to a
	 * Protobuf message
	 * 
	 * @return the Protobuf message representing this object
	 */
    @Override
    Proto3TelemetryPacketMessage build();
}
