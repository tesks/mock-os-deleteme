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
import jpl.gds.evr.api.IEvr;

public interface IEvrLDIStore extends ILDIStore {
    /**
     * Store Identification for LDI Inserters
     */
    public static final StoreIdentifier STORE_IDENTIFIER = StoreIdentifier.Evr;
    
    /** FSW table fields as CSV */
    public static final String FIELDS_FSW = "dssId" + "," +
                                             "vcid"  + "," +
                                             "isRealtime";

    /** Maximum length of Evr.name column */
    public static final int NAME_LENGTH = 128;
    
    /** Maximum length of Evr.level column */
    public static final int LEVEL_LENGTH = 16;
    
    /** Maximum length of Evr.module column */
    public static final int MODULE_LENGTH = 32;
    
    /** Maximum length of Evr.message column */
    /** MPCS-7917 400 => 2048 */
    public static final int MESSAGE_LENGTH = 2048;
    
    /** Maximum length of EvrMetadata.value column */
    /** MPCS-7917 => 400 */
    public static final int VALUE_LENGTH = 400;

    /** Common table fields as CSV */
    public static final String FIELDS_COMMON = SESSION_ID + "," + HOST_ID + "," + FRAGMENT_ID + "," + "id" + "," + "packetId" + "," + "name"
            + "," + "eventId" + "," + "ertCoarse" + "," + "ertFine" + "," + "scetCoarse" + "," + "scetFine" + ","
            + "rctCoarse" + "," + "rctFine" + "," + "sclkCoarse" + "," + "sclkFine" + "," + "level" + "," + "module"
            + "," + "message";
    
    /** Table fields as CSV */
    public static final String DB_EVR_DATA_FIELDS = FIELDS_COMMON + "," + FIELDS_FSW;
    
    /** Metadata table fields as CSV */
    public static final String DB_EVR_METADATA_FIELDS = SESSION_ID + "," + HOST_ID + "," + FRAGMENT_ID + "," + "id" + "," + "keyword" + ","
            + "value";
    
    /**
     * Insert an EVR and its metadata into the database.
     *
     * @param evr EVR object
     *
     * @throws DatabaseException SQL exception
     */
    public void insertEvr(IEvr evr) throws DatabaseException;
}