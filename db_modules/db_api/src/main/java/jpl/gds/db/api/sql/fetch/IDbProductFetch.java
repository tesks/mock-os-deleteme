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
package jpl.gds.db.api.sql.fetch;

import java.util.Map;

import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.types.IDbSessionInfoProvider;
import jpl.gds.product.api.IProductMetadataUpdater;

public interface IDbProductFetch extends IDbSqlFetch {
    /** The abbreviation that should as a variable name in an SQL statement using the product table */
	public final String DB_PRODUCT_TABLE_ABBREV = "pr";

    /**
     * Create empty metadata.
     *
     * @return An empty metadata
     */    
    public <T extends IProductMetadataUpdater> T createEmptyMetadata();
    
	/**
     * Retrieve a count of how many product entries exist in the database for
     * each APID
     * 
     * @param dbSession
     *            the TestSessionInfo for the test being queried
     * @param completeOnly
     *            true: retrieves number of complete products by apid false:
     *            retrieves number of partial products by apid null: retrieves
     *            total number of products by apid
     * @return A hashtable of the form (key,value) = (apid,count) that gives the
     *         number of product entries for each APID
     * @throws DatabaseException
     *             if an error occurs caused by database access
     */
	public Map<Integer, Integer> countProductsByApid(IDbSessionInfoProvider dbSession, Boolean completeOnly) throws DatabaseException;
}