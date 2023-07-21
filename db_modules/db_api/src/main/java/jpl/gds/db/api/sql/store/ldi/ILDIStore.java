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

import jpl.gds.ccsds.api.packet.ISpacePacketHeader;
import jpl.gds.ccsds.api.tm.frame.ITelemetryFrameHeader;
import jpl.gds.db.api.sql.store.IDbSqlStore;

public interface ILDIStore extends IDbSqlStore {
    /** Maximum header (Frame, *Packet) size in bytes */
    public static final int MAX_FRAME = ITelemetryFrameHeader.MAX_FRAME;

    /** Maximum header (Frame, *Packet) size in bytes */
    public static final int MAX_HEADER = 1024;

    /** Maximum trailer (Frame, *Packet) size in bytes */
    public static final int MAX_TRAILER = 255;

    /** Maximum Packet body size in bytes */
    public static final int MAX_PACKET = ISpacePacketHeader.MAX_PACKET;

    /** First kind of wildcard for SQL */
    public final String WILDCARD1 = "%";
    
    /** Second kind of wildcard for SQL */
    public final String WILDCARD2 = "_";
    
    /** Database enum for infinite value */
    public final String INFINITY = "WAS_PINFINITY";
    
    /** Database enum for negative infinite value */
    public final String NEGATIVE_INFINITY = "WAS_NINFINITY";
    
    /** Database enum for NaN value */
    public final String NAN = "WAS_NAN";
    
    /** Where we start the metadata id sequence */
    public final long ID_START = 1L;
}