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
package jpl.gds.db.mysql.impl.sql.store;

import java.io.File;
import java.io.FileOutputStream;

import jpl.gds.db.api.sql.store.IDbSqlStore;
import jpl.gds.db.api.sql.store.IStoreMonitor;
import jpl.gds.db.api.sql.store.StoreIdentifier;
import jpl.gds.db.api.sql.store.ldi.IInserter;
import jpl.gds.shared.types.Pair;

/**
 * A structure to capture the monitoring of various stores. The members of this
 * class used to be discreet static variables located in StaticLDIStore.
 *
 */
public class StoreMonitor implements IStoreMonitor {
    /**
     * Store Name
     */
    private final IDbSqlStore            store;

    /**
     * Inserter ID
     */
    private final StoreIdentifier        si;

    /**
     * true if this store monitor is monitoring an Bulk Loading store
     */
    private final boolean                isBulkLoadable;

    /**
     * Objects used for synchronization Value doesn't matter, but must be unique
     */
    private final Object                 syncMonitor        = new Object();

    /**
     * Active flags for all types
     */
    private boolean                      isActive           = false;

    /**
     * Inserter for this data type
     */
    private IInserter                    inserter;

    /**
     * Output Stream to write values
     */
    private Pair<File, FileOutputStream> valueStream        = null;

    /**
     * Output Stream to write meteadata
     */
    private Pair<File, FileOutputStream> metadataStream     = null;

    /**
     * Number of value rows to be written to the value stream
     */
    private long                         valuesInStream     = 0L;

    /**
     * Number of metadata rows to be written in the metadata stream
     */
    private long                         metadataInStream   = 0L;

    /**
     * Number of value rows processed
     */
    private long                         valuesProcessed    = 0L;

    /**
     * Number of metadata rows written
     */
    private long                         meteadataProcessed = 0L;

    /**
     * True if exporting this data type
     */
    private boolean                      export             = false;

    /**
     * Constructor for Store monitoring
     * 
     * @param store
     *            the Store associated with this StoreMonitor
     * @param si
     *            the Store Identifier associated with this StoreMonitor
     */
    public StoreMonitor(final IDbSqlStore store, final StoreIdentifier si) {
        this(store, si, true);
    }

    /**
     * Constructor for Store monitoring
     * 
     * @param store
     *            the Store associated with this StoreMonitor
     * @param si
     *            the Store Identifier associated with this StoreMonitor
     * @param isBulkLoadable
     *            (LDI for MySql DBMS)
     */
    public StoreMonitor(final IDbSqlStore store, final StoreIdentifier si, final boolean isBulkLoadable) {
        this.store = store;
        this.si = si;
        this.isBulkLoadable = isBulkLoadable;
    }

    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.db.mysql.impl.sql.store.IStoreMonitor#getStore()
     */
    @Override
    public IDbSqlStore getStore() {
        return store;
    }

    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.db.mysql.impl.sql.store.IStoreMonitor#isActive()
     */
    @Override
    public boolean isActive() {
        return isActive;
    }

    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.db.mysql.impl.sql.store.IStoreMonitor#setActive(boolean)
     */
    @Override
    public void setActive(final boolean isActive) {
        this.isActive = isActive;
    }

    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.db.mysql.impl.sql.store.IStoreMonitor#getSyncMonitor()
     */
    @Override
    public Object getSyncMonitor() {
        return syncMonitor;
    }

    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.db.mysql.impl.sql.store.IStoreMonitor#hasEnoughToFlush(long)
     */
    @Override
    public boolean hasEnoughToFlush(final long minimum) {
        boolean hasEnough = this.valuesInStream > minimum;
        if (si == StoreIdentifier.CommandMessage) {
            hasEnough |= this.metadataInStream > minimum;
        }
        return hasEnough;
    }

    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.db.mysql.impl.sql.store.IStoreMonitor#getIe()
     */
    @Override
    public StoreIdentifier getSi() {
        return si;
    }

    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.db.mysql.impl.sql.store.IStoreMonitor#isBulkLoadable()
     */
    @Override
    public boolean isBulkLoadable() {
        return isBulkLoadable;
    }
    
    /**
     * @return true if the store represented by this monitor is a bulk-loading store,
     * and is still active, false if not.
     */
    @Override
    public boolean isBulkLoadableAndActive() {
        return isBulkLoadable && isActive;
    }

    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.db.mysql.impl.sql.store.IStoreMonitor#getInserter()
     */
    @Override
    public IInserter getInserter() {
        return inserter;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * jpl.gds.db.mysql.impl.sql.store.IStoreMonitor#setInserter(jpl.gds.db.
     * mysql.impl.sql.store.ldi.Inserter)
     */
    @Override
    public void setInserter(final IInserter inserter) {
        this.inserter = inserter;
    }

    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.db.mysql.impl.sql.store.IStoreMonitor#getValueStream()
     */
    @Override
    public Pair<File, FileOutputStream> getValueStream() {
        return valueStream;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * jpl.gds.db.mysql.impl.sql.store.IStoreMonitor#setValueStream(jpl.gds.
     * shared.types.Pair)
     */
    @Override
    public void setValueStream(final Pair<File, FileOutputStream> valueStream) {
        this.valueStream = valueStream;
    }

    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.db.mysql.impl.sql.store.IStoreMonitor#getMetadataStream()
     */
    @Override
    public Pair<File, FileOutputStream> getMetadataStream() {
        return metadataStream;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * jpl.gds.db.mysql.impl.sql.store.IStoreMonitor#setMetadataStream(jpl.gds.
     * shared.types.Pair)
     */
    @Override
    public void setMetadataStream(final Pair<File, FileOutputStream> metadataStream) {
        this.metadataStream = metadataStream;
    }

    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.db.mysql.impl.sql.store.IStoreMonitor#clearValuesInStream()
     */
    @Override
    public void clearValuesInStream() {
        this.valuesInStream = 0L;
    }

    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.db.mysql.impl.sql.store.IStoreMonitor#incValuesInStream()
     */
    @Override
    public void incValuesInStream() {
        this.valuesInStream++;
    }

    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.db.mysql.impl.sql.store.IStoreMonitor#getValuesInStream()
     */
    @Override
    public long getValuesInStream() {
        return valuesInStream;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * jpl.gds.db.mysql.impl.sql.store.IStoreMonitor#clearMetadataInStream()
     */
    @Override
    public void clearMetadataInStream() {
        this.metadataInStream = 0L;
    }

    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.db.mysql.impl.sql.store.IStoreMonitor#incMetadataInStream()
     */
    @Override
    public void incMetadataInStream() {
        this.metadataInStream++;
    }

    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.db.mysql.impl.sql.store.IStoreMonitor#getMetadataInStream()
     */
    @Override
    public long getMetadataInStream() {
        return metadataInStream;
    }

    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.db.mysql.impl.sql.store.IStoreMonitor#clearValuesProcessed()
     */
    @Override
    public void clearValuesProcessed() {
        this.valuesProcessed = 0L;
    }

    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.db.mysql.impl.sql.store.IStoreMonitor#incValuesProcessed()
     */
    @Override
    public void incValuesProcessed() {
        this.valuesProcessed++;
    }

    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.db.mysql.impl.sql.store.IStoreMonitor#getValuesProcessed()
     */
    @Override
    public long getValuesProcessed() {
        return valuesProcessed;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * jpl.gds.db.mysql.impl.sql.store.IStoreMonitor#clearMeteadataProcessed()
     */
    @Override
    public void clearMeteadataProcessed() {
        this.meteadataProcessed = 0L;
    }

    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.db.mysql.impl.sql.store.IStoreMonitor#incMetadataProcessed()
     */
    @Override
    public void incMetadataProcessed() {
        this.meteadataProcessed++;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * jpl.gds.db.mysql.impl.sql.store.IStoreMonitor#getMeteadataProcessed()
     */
    @Override
    public long getMeteadataProcessed() {
        return meteadataProcessed;
    }

    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.db.mysql.impl.sql.store.IStoreMonitor#isExport()
     */
    @Override
    public boolean isExport() {
        return this.export;
    }

    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.db.mysql.impl.sql.store.IStoreMonitor#setExport(boolean)
     */
    @Override
    public void setExport(final boolean export) {
        this.export = export;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("StoreMonitor [store=").append(store.getClass().getSimpleName()).append(", si=").append(si).append(", isBulkLoadable=").append(isBulkLoadable)
                .append(", syncMonitor=").append(syncMonitor).append(", isActive=").append(isActive).append(", inserter=").append(inserter)
                .append(", valueStream=").append(valueStream).append(", metadataStream=").append(metadataStream).append(", valuesInStream=")
                .append(valuesInStream).append(", metadataInStream=").append(metadataInStream).append(", valuesProcessed=").append(valuesProcessed)
                .append(", meteadataProcessed=").append(meteadataProcessed).append(", export=").append(export).append("]");
        return builder.toString();
    }
}