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


public class StoreConfiguration implements IStoreConfiguration {

    private StoreIdentifier id;
    private String valueTable;
    private String metadataTable;
    private String valueFields;
    private String metadataFields;
    private String setClause;
    private boolean forSse;


    /**
     * @param id
     *            the StoreIdentifier for this store configuration
     */
    public StoreConfiguration(StoreIdentifier id) {
        this(id, null, null, null, null, null, false);
    }
    
    /**
     * @param id
     *            the StoreIdentifier for this store configuration
     * @param valueTableName
     *            the value table name for this store configuration
     * @param valueFields
     *            the fieled names for the value table for this store
     *            configuration
     * @param metadataTableName
     *            the matadata table name for this store configuration
     * @param metadataFields
     *            the field names for the metadata table for this store
     *            configuration
     * @param setClause
     *            the set clause (may be null)
     * @param isSse
     *            true if this store is for SSE values, false if not
     */
    public StoreConfiguration(StoreIdentifier id, String valueTableName, String valueFields, String metadataTableName, String metadataFields, String setClause, boolean isSse) {
        this.id = id;
        this.valueTable = valueTableName;
        this.metadataTable = metadataTableName;
        this.valueFields = valueFields;
        this.metadataFields = metadataFields;
        this.setClause = setClause;
        this.forSse = isSse;
    }

    @Override
    public StoreIdentifier getStoreIdentifier() {
        return this.id;
    }

    @Override
    public String getValueTableName() {
        return this.valueTable;
    }

    @Override
    public String getMetadataTableName() {
        return this.metadataTable;
    }

    @Override
    public String getValueFields() {
        return this.valueFields;
    }

    @Override
    public String getMetadataFields() {
        return this.metadataFields;
    }

    @Override
    public String getSetClause() {
        return this.setClause;
    }

    @Override
    public boolean isSse() {
        return this.forSse;
    }

}
