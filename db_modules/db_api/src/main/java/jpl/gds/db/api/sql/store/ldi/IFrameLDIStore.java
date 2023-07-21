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
import jpl.gds.shared.holders.TrailerHolder;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.station.api.InvalidFrameCode;

public interface IFrameLDIStore extends ILDIStore {
    /**
     * Store Identification for LDI Inserters
     */
    public static final StoreIdentifier STORE_IDENTIFIER = StoreIdentifier.Frame;
    
    /**
     * Database table fields as CSV
     */
    String DB_FRAME_DATA_FIELDS = SESSION_ID + "," + HOST_ID + "," + FRAGMENT_ID + "," + "id" + "," + "type" + "," + "rctCoarse" + ","
            + "rctFine" + "," + "ertCoarse" + "," + "ertFine" + "," + "relaySpacecraftId" + "," + "vcid" + "," + "vcfc"
            + "," + "dssId" + "," + "bitRate" + "," + "badReason" + "," + "fillFrame" + "," + "bodyLength" + ","
            + "headerLength" + "," + "trailerLength";
    
    /**
     * Database body table fields as CSV. Do not put a VARBINARY last.
     */
    String DB_FRAME_BODY_FIELDS = SESSION_ID + "," + HOST_ID + "," + FRAGMENT_ID + "," + "@body" + "," + "header" + ","
            + "trailer" + "," + "id";
    
    /**
     * SQL to compress body
     */
    String DB_FRAME_SET_CLAUSE = " SET body = COMPRESS(@body)";

    /**
     * Insert a frame and its metadata into the database
     *
     * @param frameType The type of frame
     * @param ert       The ERT for the frame
     * @param relayScid The relay spacecraft ID for the frame
     * @param vcid      The VCID of the frame
     * @param rawVcfc   The VCFC for the frame
     * @param dss       The DSS ID for the frame
     * @param bitRate   The bit rate when MPCS received the frame
     * @param frame     The raw binary frame
     * @param header    The raw binary header
     * @param trailer   The raw binary trailer
     * @param frameId   The frame id
     * @param badReason The error, if any
     * @param fillFrame True if this is a fill frame
     *
     * @throws DatabaseException SQL exception
     *
     * @version MPCS-5932 Redo frame id to use holder
     */
    void insertFrame(String frameType, IAccurateDateTime ert, int relayScid, int vcid, int rawVcfc, int dss,
            double bitRate, byte[] frame, HeaderHolder header, TrailerHolder trailer, FrameIdHolder frameId,
            InvalidFrameCode badReason, boolean fillFrame) throws DatabaseException;

}