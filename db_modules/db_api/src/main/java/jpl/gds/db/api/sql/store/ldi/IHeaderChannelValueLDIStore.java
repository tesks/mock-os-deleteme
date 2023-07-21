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

import jpl.gds.db.api.sql.store.StoreIdentifier;

public interface IHeaderChannelValueLDIStore extends IChannelValueLDIStore {
    /**
     * Store Identification for LDI Inserters
     */
    public static final StoreIdentifier STORE_IDENTIFIER = StoreIdentifier.HeaderChannelValue;
    
    /** Parent id DB table fields as CSV */
    public static final String PARENT_ID_FIELDS = "parentId";

    /** Parent type DB table fields as CSV */
    public static final String PARENT_TYPE_FIELDS = "parentType";

    /** From-SSE DB table fields as CSV */
    public static final String FROM_SSE_FIELDS = "fromSse";
    
    /** Auxiiary DB table fields as CSV */
    public final String DB_HEADER_CHANNEL_DATA_FIELDS = SESSION_ID + "," + HOST_ID + "," + FRAGMENT_ID + "," + "id" + "," + "channelId" + ","
            + "fromSse" + "," + "type" + "," + "channelIndex" + "," + "module" + "," + "name" + "," + "dnFormat" + ","
            + "euFormat";

    /** Main header table fields as CSV */
    public static final String DB_HEADER_CHANNEL_VALUE_FIELDS = IChannelValueLDIStore.COMMON_FIELDS + "," + PARENT_ID_FIELDS + "," + PARENT_TYPE_FIELDS + ","
            + FROM_SSE_FIELDS + "," + IChannelValueLDIStore.ERT_FIELDS + "," + IChannelValueLDIStore.DSS_ID_FIELDS + ","
            + IChannelValueLDIStore.VCID_FIELDS;
}