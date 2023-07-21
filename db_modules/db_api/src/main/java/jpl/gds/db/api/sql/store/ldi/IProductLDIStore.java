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

import java.math.BigDecimal;

import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.store.StoreIdentifier;
import jpl.gds.product.api.IProductMetadataProvider;

public interface IProductLDIStore extends ILDIStore {
    /**
     * Store Identification for LDI Inserters
     */
    public static final StoreIdentifier STORE_IDENTIFIER = StoreIdentifier.Product;
    
    /**
     * Default version.
     */
    BigDecimal VERSION_DEFAULT = BigDecimal.ONE;
    
    /**
     * Database table fields as CSV.
     */
    String DB_PRODUCT_DATA_FIELDS = 
            SESSION_ID           + "," +
            HOST_ID              + "," +
            FRAGMENT_ID          + "," +
            "rctCoarse"          + "," +
            "rctFine"            + "," +
            "creationTimeCoarse" + "," +
            "creationTimeFine"   + "," +
            "dvtScetCoarse"      + "," +
            "dvtScetFine"        + "," +
            "vcid"               + "," +
            "isPartial"          + "," +
            "apid"               + "," +
            "apidName"           + "," +
            "sequenceId"         + "," +
            "sequenceVersion"    + "," +
            "commandNumber"      + "," +
            "xmlVersion"         + "," +
            "totalParts"         + "," +
            "dvtSclkCoarse"      + "," +
            "dvtSclkFine"        + "," +
            "fullPath"           + "," +
            "fileName"           + "," +
            "ertCoarse"          + "," +
            "ertFine"            + "," +
            "groundStatus"       + "," +
            "sequenceCategory"   + "," +
            "sequenceNumber"     + "," +
            "version";

    /**
     * Product fields as CSV.
     */
    public static final String DB_REFERENCE_PRODUCT_DATA_FIELDS = 
            IProductLDIStore.DB_PRODUCT_DATA_FIELDS + "," +
            "checksum"                     + "," +
            "cfdpTransactionId"            + "," +
            "fileSize";

    /**
     * Get field list as CSV.
     *
     * @return The comma-separated field list
     */
    String getFields();

    /**
     * Insert a product into the database
     *
     * @param pmd
     *            The metadata for the product to insert
     *
     * @throws DatabaseException
     *             Throws DatabaseException
     */
    void insertProduct(IProductMetadataProvider pmd) throws DatabaseException;

}