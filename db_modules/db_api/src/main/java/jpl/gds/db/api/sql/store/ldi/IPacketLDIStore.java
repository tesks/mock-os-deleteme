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
import jpl.gds.shared.holders.FrameIdHolder;
import jpl.gds.shared.holders.HeaderHolder;
import jpl.gds.shared.holders.PacketIdHolder;
import jpl.gds.shared.holders.TrailerHolder;
import jpl.gds.tm.service.api.packet.ITelemetryPacketInfo;

public interface IPacketLDIStore extends ILDIStore{
    /**
     * Store Identification for LDI Inserters
     */
    public static final StoreIdentifier STORE_IDENTIFIER = StoreIdentifier.Packet;
    
    /** Additional FSW table fields as CSV */
    public static final String FIELDS_FSW   = "frameId"    + "," +
                                               "dssId"      + "," +
                                               "vcid"       + "," +
                                               "sourceVcfc" + "," +
                                               "fillFlag";

    /** Common table fields as CSV */
    public static final String FIELDS_COMMON = SESSION_ID + "," + HOST_ID + "," + FRAGMENT_ID + "," + "id" + "," + "rctCoarse" + ","
            + "rctFine" + "," + "scetCoarse" + "," + "scetFine" + "," + "ertCoarse" + "," + "ertFine" + ","
            + "sclkCoarse" + "," + "sclkFine" + "," + "apid" + "," + "apidName" + "," + "spsc" + "," + "badReason" + ","
            + "bodyLength" + "," + "headerLength" + "," + "trailerLength";
    
    /** FSW table fields as CSV */
    public static final String DB_PACKET_DATA_FIELDS = FIELDS_COMMON + "," + FIELDS_FSW;
    
    /** Body table fields as CSV. Do not put a VARBINARY last. */
    public static final String DB_PACKET_BODY_FIELDS = SESSION_ID + "," + HOST_ID + "," + FRAGMENT_ID + "," + "@body" + "," + "header" + ","
            + "trailer" + "," + "id";
    
    /** SQL for compressing body */
    public static final String DB_PACKET_SET_CLAUSE = " SET body = COMPRESS(@body)";

    /**
     * Insert a packet and its body into the database via LDI
     *
     * @param pi         Packet info object
     * @param packetData Raw binary packet
     * @param frameId    Parent frame
     * @param dssId      DSS
     * @param packetId   Packet id
     * @param hdr        Header holder
     * @param tr         Trailer holder
     *
     * @throws DatabaseException SQL exception
     *
     * @version MPCS-5932 Use frame id holder
     */
    public void insertPacket(ITelemetryPacketInfo pi, byte[] packetData, FrameIdHolder frameId, int dssId,
            PacketIdHolder packetId, HeaderHolder hdr, TrailerHolder tr) throws DatabaseException;
}