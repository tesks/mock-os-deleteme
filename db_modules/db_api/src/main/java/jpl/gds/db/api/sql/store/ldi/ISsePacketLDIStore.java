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
package jpl.gds.db.api.sql.store.ldi;

import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.store.StoreIdentifier;
import jpl.gds.shared.holders.HeaderHolder;
import jpl.gds.shared.holders.PacketIdHolder;
import jpl.gds.shared.holders.TrailerHolder;
import jpl.gds.tm.service.api.packet.ITelemetryPacketInfo;

public interface ISsePacketLDIStore extends IPacketLDIStore {
    /**
     * Store Identification for LDI Inserters
     */
    public static final StoreIdentifier STORE_IDENTIFIER = StoreIdentifier.SsePacket;
    
    /** SSE packet fields in CSV */
    String DB_SSE_PACKET_DATA_FIELDS = IPacketLDIStore.FIELDS_COMMON;
    
    /** SSE packet body fields in CSV */
    String DB_SSE_PACKET_BODY_FIELDS = IPacketLDIStore.DB_PACKET_BODY_FIELDS;
    
    /** SQL for compressing body */
    String DB_SSE_PACKET_SET_CLAUSE = IPacketLDIStore.DB_PACKET_SET_CLAUSE;

    /**
     * Insert a SSE packet and its body into the database via LDI
     *
     * @param pi         Packet info object
     * @param packetData Raw binary packet
     * @param packetId   Packet id
     * @param hdr        Header holder
     * @param tr         Trailer holder
     *
     * @throws DatabaseException SQL exception
     */
    public void insertSsePacket(ITelemetryPacketInfo pi, byte[] packetData, PacketIdHolder packetId, HeaderHolder hdr,
            TrailerHolder tr) throws DatabaseException;

}