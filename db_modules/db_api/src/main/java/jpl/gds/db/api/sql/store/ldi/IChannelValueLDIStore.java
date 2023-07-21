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

import java.util.List;

import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.store.StoreIdentifier;
import jpl.gds.eha.api.channel.IServiceChannelValue;
import jpl.gds.shared.performance.IPerformanceData;

public interface IChannelValueLDIStore extends ILDIStore {
    /**
     * Store Identification for LDI Inserters
     */
    public static final StoreIdentifier STORE_IDENTIFIER = StoreIdentifier.ChannelValue;
    
    /** isRealtime DB table fields as CSV */
    public final String ISREALTIME_FIELDS = "isRealtime";

    /** VCID DB table fields as CSV */
    public final String VCID_FIELDS = "vcid";
    
    /** SCLK/SCET table fields as CSV */
    public final String SCLK_FIELDS = "sclkCoarse" + "," + "sclkFine" + "," + "scetCoarse" + "," + "scetFine";
    
    /** Packet id DB table fields as CSV */
    public final String PACKET_ID_FIELDS = "packetId";
    
    /** ERT DB table fields as CSV */
    public final String ERT_FIELDS = "ertCoarse" + "," + "ertFine";
    
    /** DSS id DB table fields as CSV */
    public final String DSS_ID_FIELDS = "dssId";
    
    /** SQL to compute CRC32 of channelId */
    public final String DB_CHANNEL_SET_CLAUSE = " SET channelIdCrc = CRC32(@channelId)";
    
    /** Common DB table fields as CSV */
    public final String COMMON_FIELDS = SESSION_ID + "," + HOST_ID + "," + FRAGMENT_ID + "," + "id" + "," + "@channelId" + ","
            + "rctCoarse" + "," + "rctFine" + "," + "dnUnsignedValue" + "," + "dnIntegerValue" + "," + "dnDoubleValue"
            + "," + "dnDoubleFlag" + "," + "dnStringValue" + "," + "eu" + "," + "euFlag" + "," + "dnAlarmState" + ","
            + "euAlarmState";
    
    /** Main DB table fields as CSV */
    public final String DB_CHANNEL_VALUE_FIELDS = COMMON_FIELDS + "," + PACKET_ID_FIELDS + "," + ERT_FIELDS + "," + SCLK_FIELDS + "," + DSS_ID_FIELDS
            + "," + VCID_FIELDS + "," + ISREALTIME_FIELDS;
    
    /** Auxiiary DB table fields as CSV */
    public final String DB_CHANNEL_DATA_FIELDS = SESSION_ID + "," + HOST_ID + "," + FRAGMENT_ID + "," + "id" + "," + "channelId" + ","
            + "fromSse" + "," + "type" + "," + "channelIndex" + "," + "module" + "," + "name" + "," + "dnFormat" + ","
            + "euFormat";

    /**
     * Insert a channel value into the database
     *
     * @param val The channel value to insert
     *
     * @throws DatabaseException Throws exception on error
     */
    void insertChannelValue(IServiceChannelValue val) throws DatabaseException;

    /**
    * Supplies performance data to the performance summary publisher. Consists
    * of one queue performance object, and only if running in async mode.
    * 
    * @return list of IPerformanceData, empty if not async
    *      * @version MPCS-7168 - Added method.
    */
    public List<IPerformanceData> getPerformanceData();
}