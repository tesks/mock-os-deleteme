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
package jpl.gds.db.api.sql.store;

public interface IStoreConfiguration {
    
    public StoreIdentifier getStoreIdentifier();
    
    /**
     * @return the valueTableName
     */
    public String getValueTableName();

    /**
     * @return the metadataTableName
     */
    public String getMetadataTableName();

    /**
     * @return the valueFields
     */
    public String getValueFields();

    /**
     * @return the metadataFields
     */
    public String getMetadataFields();

    /**
     * @return the setClause
     */
    public String getSetClause();

    /**
     * @return true if is an SSE store, false if not
     */
    public boolean isSse();
}
