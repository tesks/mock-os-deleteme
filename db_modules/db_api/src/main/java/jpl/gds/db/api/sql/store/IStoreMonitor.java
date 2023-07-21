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

import java.io.File;
import java.io.FileOutputStream;

import jpl.gds.db.api.sql.store.ldi.IInserter;
import jpl.gds.shared.types.Pair;


public interface IStoreMonitor {

    /**
     * @return the store
     */
    IDbSqlStore getStore();

    /**
     * @return the isActive
     */
    boolean isActive();

    /**
     * @param isActive
     *            the isActive to set
     */
    void setActive(boolean isActive);

    /**
     * @return the syncMonitor
     */
    Object getSyncMonitor();

    /**
     * Returns whether there are enough elements to flush
     *
     * @param minimum
     *            the least number of elements to flush
     * @return true if enough, false if not.
     */
    boolean hasEnoughToFlush(long minimum);

    /**
     * @return the StoreIdentifier
     */
    StoreIdentifier getSi();

    /**
     * @return the isBulkLoadable
     */
    boolean isBulkLoadable();

    /**
     * @return true if the store represented by this monitor is a bulk-loading store,
     * and is still active, false if not.
     */
    boolean isBulkLoadableAndActive();

    /**
     * @return the inserter
     */
    IInserter getInserter();

    /**
     * @param inserter
     *            the inserter to set
     */
    void setInserter(IInserter inserter);

    /**
     * @return the valueStream
     */
    Pair<File, FileOutputStream> getValueStream();

    /**
     * @param valueStream
     *            the valueStream to set
     */
    void setValueStream(Pair<File, FileOutputStream> valueStream);

    /**
     * @return the metadataStream
     */
    Pair<File, FileOutputStream> getMetadataStream();

    /**
     * @param metadataStream
     *            the metadataStream to set
     */
    void setMetadataStream(Pair<File, FileOutputStream> metadataStream);

    /**
     * Clear the number of value rows to be processed
     */
    void clearValuesInStream();

    /**
     * Increment the number of value rows to be processed
     */
    void incValuesInStream();

    /**
     * @return the number of value rows to be processed
     */
    long getValuesInStream();

    /**
     * Clear the number of metadata rows to be processed
     */
    void clearMetadataInStream();

    /**
     * Increment the number of metadata rows to be processed
     */
    void incMetadataInStream();

    /**
     * @return the number of metadata rows to be processed
     */
    long getMetadataInStream();

    /**
     * Clear the number of value rows processed
     */
    void clearValuesProcessed();

    /**
     * Increment the number of value rows processedt
     */
    void incValuesProcessed();

    /**
     * @return the number of value rows processed
     */
    long getValuesProcessed();

    /**
     * Clear the number of metadata rows processed
     */
    void clearMeteadataProcessed();

    /**
     * Increment the number of metadata rows processed
     */
    void incMetadataProcessed();

    /**
     * @return the number of meteadata rows processed
     */
    long getMeteadataProcessed();

    /**
     * @return the export
     */
    boolean isExport();

    /**
     * @param export
     *            the export to set
     */
    void setExport(boolean export);
}