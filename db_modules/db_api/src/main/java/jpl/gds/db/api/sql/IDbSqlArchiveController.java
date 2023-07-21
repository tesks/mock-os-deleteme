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
package jpl.gds.db.api.sql;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Collection;

import jpl.gds.common.service.telem.ITelemetrySummary;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.db.api.sql.store.ICommandUpdateStore;
import jpl.gds.db.api.sql.store.IContextConfigStore;
import jpl.gds.db.api.sql.store.IDbSqlStore;
import jpl.gds.db.api.sql.store.IEndSessionStore;
import jpl.gds.db.api.sql.store.IHostStore;
import jpl.gds.db.api.sql.store.ISessionStore;
import jpl.gds.db.api.sql.store.IStoreMonitor;
import jpl.gds.db.api.sql.store.StoreIdentifier;
import jpl.gds.db.api.sql.store.ldi.IChannelValueLDIStore;
import jpl.gds.db.api.sql.store.ldi.ICommandMessageLDIStore;
import jpl.gds.db.api.sql.store.ldi.IEvrLDIStore;
import jpl.gds.db.api.sql.store.ldi.IFrameLDIStore;
import jpl.gds.db.api.sql.store.ldi.IGatherer;
import jpl.gds.db.api.sql.store.ldi.IHeaderChannelValueLDIStore;
import jpl.gds.db.api.sql.store.ldi.ILogMessageLDIStore;
import jpl.gds.db.api.sql.store.ldi.IMonitorChannelValueLDIStore;
import jpl.gds.db.api.sql.store.ldi.IPacketLDIStore;
import jpl.gds.db.api.sql.store.ldi.IProductLDIStore;
import jpl.gds.db.api.sql.store.ldi.ISseChannelValueLDIStore;
import jpl.gds.db.api.sql.store.ldi.ISseEvrLDIStore;
import jpl.gds.db.api.sql.store.ldi.ISsePacketLDIStore;
import jpl.gds.db.api.sql.store.ldi.aggregate.IChannelAggregateLDIStore;
import jpl.gds.db.api.sql.store.ldi.aggregate.IHeaderChannelAggregateLDIStore;
import jpl.gds.db.api.sql.store.ldi.aggregate.IMonitorChannelAggregateLDIStore;
import jpl.gds.db.api.sql.store.ldi.aggregate.ISseChannelAggregateLDIStore;
import jpl.gds.db.api.sql.store.ldi.cfdp.ICfdpFileGenerationLDIStore;
import jpl.gds.db.api.sql.store.ldi.cfdp.ICfdpFileUplinkFinishedLDIStore;
import jpl.gds.db.api.sql.store.ldi.cfdp.ICfdpIndicationLDIStore;
import jpl.gds.db.api.sql.store.ldi.cfdp.ICfdpPduReceivedLDIStore;
import jpl.gds.db.api.sql.store.ldi.cfdp.ICfdpPduSentLDIStore;
import jpl.gds.db.api.sql.store.ldi.cfdp.ICfdpRequestReceivedLDIStore;
import jpl.gds.db.api.sql.store.ldi.cfdp.ICfdpRequestResultLDIStore;
import jpl.gds.db.api.types.IDbSessionInfoProvider;
import jpl.gds.shared.types.Pair;

/**
 * Interface for a DB SQL archive controller
 *
 */
public interface IDbSqlArchiveController {
    /** Get rid of ODASA warnings about using this string over and over. */
    String  WRITE_ERROR    = "Unable to write ";

    /** No LDI boolean */
    boolean NO_LDI         = false;                                                             // true
    /** Extra delay boolean */
    boolean EXTRA_DELAY    = false;
    /** Try operation integer */
    int     TRY_OPERATION  = 10;
    /** use fields boolean  */
    boolean USE_FIELDS     = true;
    /** use SQL 5.0.38 boolean  */
    boolean SQL_5_0_38     = true;
    /** open repeat integer  */
    int     OPEN_REPEAT    = 100;
    /** one second long */
    long    ONE_SECOND     = 1000L;
    /** one second double */
    double  D_ONE_SECOND   = 1000.0;
    /** min flush wait, long */
    long    MIN_FLUSH      = 1L * ONE_SECOND;
    /** gatherer join wait, long */
    long    GATHERER_JOIN  = 15L * ONE_SECOND;
    /** inserter join wait, long */
    long    INSERTER_JOIN  = 5L * 60L * ONE_SECOND;
    /** Shutdown timeout */
    long    SHUTDOWN_JOIN  = 15L * ONE_SECOND;
    /** inserter wait, long */
    long    INSERTER_WAIT  = 1L * ONE_SECOND;
    /** MPCS-7168 - Reduced inserter check interval */
    long    INSERTER_CHECK = 3L * ONE_SECOND;
    /** LDI directory string */
    String  LDI_DIR        = "ldi";
    /** default base */
    String  DEFAULT_BASE   = File.separator + "tmp" + File.separator + LDI_DIR + File.separator;

    /*
     * 
     * Removed instantiation of tracers in the Archive Controller:
     * FastTracer that does not record to database, for logging errors that can
     * cause infinite loops
     */

    /**
     * Creates the individual stores. Must be called after construction.
     */
    void init();

    /**
     * Creates the individual stores. Must be called after construction.
     * 
     * @param storesToStart
     *            an array of StoreIdentifiers specifying which stores to start
     */
    void init(final StoreIdentifier... storesToStart);

    /**
     * Creates the individual stores. Must be called after construction.
     * 
     * @param storesToStartList
     *            a list of StoreIdentifiers specifying which stores to start
     */
    void init(Collection<StoreIdentifier> storesToStartList);
    
    /**
     * @return true if successful, false if not
     */
    boolean startSessionStores();

    /**
     * This method will start the Session store without inserting/creating a new session.
     *
     * @return true if successful, false if not
     */
    boolean startSessionStoresWithoutInserting();

    /**
     * @return true if successful, false if not
     */
    boolean startContextConfigStore();

    /**
     * Start all the database stores running
     *
     * @return True if started
     */
    boolean startAllStores();

    /**
     * Start the context config store and all peripheral database stores (that have been marked as needed) but not
     * session config store.
     *
     * @return True if started
     */
    boolean startAllNonSessionStores();

    /**
     * Stop all the running database stores
     *
     */
    void stopAllStores();

    /**
     * Stop all the running Session and EndSession database stores
     *
     */
    void stopSessionStores();

    /**
     * Start all the other database stores that depend on a test session
     * configuration
     */
    void startPeripheralStores();

    /**
     * Start some of the other database stores that depend on a test session
     * configuration: log and command
     */
    void startLogCommandStores();

    /**
     * Start some of the other database stores that depend on a test session
     * configuration: log and command
     *
     * @param alsoLog
     *            Also start log
     */
    void startLogCommandStores(boolean alsoLog);

    /**
     * Stop the test session store from running
     *
     */
    void stopSessionStore();

    /**
     * Stop the context config store from running
     *
     */
    void stopContextConfigStore();

    /**
     * Stop the test end session store from running
     *
     */
    void stopEndSessionStore();

    /**
     * Stop the host store from running
     */
    void stopHostStore();

    /**
     * Stop some of the stores that depend on the current test: log and command
     */
    void stopLogCommandStores();

    /**
     * Stop all the stores that depend on the current test
     */
    void stopPeripheralStores();

    /**
     * Update session end-time.
     *
     * @param contextConfig
     *            Session configuration
     * @param sum
     *            Session summary
     */
    void updateSessionEndTime(IContextConfiguration contextConfig, ITelemetrySummary sum);

    /**
     * Add table name of store that is needed. I.e., that will be started.
     *
     * @param si
     *            Database table name
     */
    void addNeededStore(StoreIdentifier si);

    /**
     * Access to Frame store. For use by performance tools only!
     *
     * @return Frame store
     */
    IFrameLDIStore getFrameStore();

    /**
     * Access to Packet store. For use by performance tools only!
     *
     * @return Packet store
     */
    IPacketLDIStore getPacketStore();

    /**
     * Access to ChannelValue store. For use by performance tools only!
     *
     * @return Channel value store
     */
    IChannelValueLDIStore getChannelValueStore();    
    
    /**
     * Access to Evr store. For use by performance tools only!
     *
     * @return Evr store
     */
    IEvrLDIStore getEvrStore();

    /**
     * @return
     */
    ISessionStore getSessionStore();

    /**
     * Access to COntext Configuration store
     *
     * @return COntext Config store
     */
    IContextConfigStore getContextConfigStore();

    /**
     * @return
     */
    IEndSessionStore getEndSessionStore();

    /**
     * @return
     */
    IHostStore getHostStore();

    /**
     * @return
     */
    IHeaderChannelValueLDIStore getHeaderChannelValueStore();

    /**
     * @return
     */
    IMonitorChannelValueLDIStore getMonitorChannelValueStore();

    /**
     * @return
     */
    ISseChannelValueLDIStore getSseChannelValueStore();

    /**
     * @return
     */
    ISseEvrLDIStore getSseEvrStore();

    /**
     * @return
     */
    ISsePacketLDIStore getSsePacketStore();

    /**
     * @return
     */
    IProductLDIStore getProductStore();

    /**
     * @return
     */
    ILogMessageLDIStore getLogMessageStore();

    /**
     * @return
     */
    ICommandMessageLDIStore getCommandMessageStore();

    /**
     * @return
     */
    ICfdpIndicationLDIStore getCfdpIndicationStore();

    /**
     * @return
     */
    ICfdpFileGenerationLDIStore getCfdpFileGenerationStore();

    /**
     * @return
     */
    ICfdpFileUplinkFinishedLDIStore getCfdpFileUplinkFinishedStore();

    /**
     * @return
     */
    ICfdpRequestReceivedLDIStore getCfdpRequestReceivedStore();

    /**
     * @return
     */
    ICfdpRequestResultLDIStore getCfdpRequestResultStore();

    /**
     * @return
     */
    ICfdpPduReceivedLDIStore getCfdpPduReceivedStore();

    /**
     * @return
     */
    ICfdpPduSentLDIStore getCfdpPduSentStore();

    /**
     * Access to ChannelAggregate store. For use by performance tools only!
     *
     * @return Channel aggregate store
     */
    IChannelAggregateLDIStore getChannelAggregateStore();

    /**
     * Access to HeaderChannelAggregate store. For use by performance tools only!
     *
     * @return Header Channel aggregate store
     */
    IHeaderChannelAggregateLDIStore getHeaderChannelAggregateStore();
    
    /**
     * Access to SseChannelAggregate store. For use by performance tools only!
     *
     * @return Sse Channel aggregate store
     */
    ISseChannelAggregateLDIStore getSseChannelAggregateStore();
    
    /**
     * Access to MonitorChannelAggregate store. For use by performance tools only!
     *
     * @return Monitor Channel aggregate store
     */
    IMonitorChannelAggregateLDIStore getMonitorChannelAggregateStore();
    
    
    /**
     * @param name
     * @return
     */
    String getFileStatus(String name);

    /**
     * @param file
     * @return
     */
    String getFileStatus(File file);

    /**
     * @param si
     * @return
     */
    IStoreMonitor getStoreMonitor(StoreIdentifier si);

    /**
     * @param si
     * @param store
     */
    IStoreMonitor setStoreMonitor(StoreIdentifier si, IDbSqlStore store);

	/**
	 * @return true if extended database is storing "Extended Precision SCET"
	 */
	boolean isExtendedDatabase();

	/**
	 * @return the PostFix for extended SCET compliant tables
	 */
	String getExtendedPostfix();

    /**
     * @param table
     * @return
     */
    String getActualTableName(String table);

    /**
     * @param sb
     * @param table
     * @return
     */
    StringBuilder getActualTableName(StringBuilder sb, String table);


    /**
     * @return the Context Configuration associated with this Database Controller
     */
    IContextConfiguration getContextConfiguration();

    /**
     * Returns the currently active instance of the IDbSessionInfoProvider
     * 
     * @return an instance of an IDbSessionInfoProvider object created from the
     *         initialized IContextConfiguration after the context is stored to the database.
     */
    IDbSessionInfoProvider getDbSessionInfoProvider();

    /**
     * @return
     */
    ICommandUpdateStore getCommandUpdateStore();

    /**
     * Resets all LDI Statistics, and restarts the Gatherer.
     * TODO: Should be private after logic that calls it moves to controller
     */
    void resetLDIStores();

    /**
     * @return true if database configuration saves files, false if not
     * TODO: Should be private after logic that calls it moves to controller
     */
    boolean isSaveFiles();

    /**
     * TODO: Should be private after logic that calls it moves to controller
     */
    void startGatherer();

    /**
     * TODO: Should be private after logic that calls it moves to controller
     */
    void stopGatherer();

    /**
     * TODO: Should not be necessary once IContextKey is fully implemented in DB
     */
    IContextConfiguration getTestConfig();

    /**
     * TODO: Should not be necessary once IContextKey is fully implemented in DB
     */
    long getLdiRowExceed();

    /**
     * TODO: Should not be necessary once IContextKey is fully implemented in DB
     */
    long getLdiRowLimit();

    /**
     * TODO: Should not be necessary once IContextKey is fully implemented in DB
     */
    boolean isGathererFlushing();

    /**
     * @return
     */
    boolean isApplicationIsSse();

    /**
     * TODO: Should not be necessary once IContextKey is fully implemented in DB
     */
    boolean setGathererFlushing(boolean value);

    /**
     * TODO: Should not be necessary once IContextKey is fully implemented in DB
     */
    String getExportDirectory();

    /**
     * TODO: Should not be necessary once IContextKey is fully implemented in DB
     */
    long getFlushTime();

    /**
     * TODO: Should not be necessary once IContextKey is fully implemented in DB
     */
    boolean getAndSetStarted(boolean b);

    /**
     * TODO: Should not be necessary once IContextKey is fully implemented in DB
     */
    boolean getConfigured();

    /**
     * TODO: Should not be necessary once IContextKey is fully implemented in DB
     */
    String getFileBase();

    /**
     * TODO: Should not be necessary once IContextKey is fully implemented in DB
     */
    String getFswSse();

    /**
     * TODO: Should not be necessary once IContextKey is fully implemented in DB
     */
    String getPid();

    /**
     * TODO: Should not be necessary once IContextKey is fully implemented in DB
     */
    boolean getStarted();

    /**
     * TODO: Should not be necessary once IContextKey is fully implemented in DB
     */
    boolean getStopped();

    /**
     * TODO: Should not be necessary once IContextKey is fully implemented in DB
     */
    String setFileBase(String string);

    /**
     * TODO: Should not be necessary once IContextKey is fully implemented in DB
     */
    long setFlushTime(long max);

    /**
     * @param b enables or disables serialization flush for idledown
     */
    void setSerializationFlush(boolean b);

    /**
     * @return the state of the ideledown flush
     */
    boolean isSerializationFlush();
    
    /**
     * TODO: Should not be necessary once IContextKey is fully implemented in DB
     */
    String setProductFields(String fields);

    /**
     * TODO: Should not be necessary once IContextKey is fully implemented in DB
     */
    String setFswSse(String value);

    /**
     * TODO: Should not be necessary once IContextKey is fully implemented in DB
     */
    /**
     * TODO: Should not be necessary once IContextKey is fully implemented in DB
     */
    String setExportDirectory(String exportLDIDir);

    /**
     * TODO: Should not be necessary once IContextKey is fully implemented in DB
     */
    boolean setConfigured(boolean b);

    /**
     * TODO: Should not be necessary once IContextKey is fully implemented in DB
     */
    long getAndIncrementUnique();

    /**
     * TODO: Should not be necessary once IContextKey is fully implemented in DB
     */
    IGatherer getGatherer();

    /**
     * @param valueTableName
     * @return
     */
    Pair<File, FileOutputStream> openStream(String valueTableName);

    /**
     * @param altDbTableName
     * @param edir
     * @param b
     * @return
     */
    Pair<File, FileOutputStream> openStream(String altDbTableName, String edir, boolean b);

    /**
     * @return true if Controller is up (started and not stopped).
     */
    boolean isUp();

    /**
     * @return true if any store is still active, false if all stores are not active
     */
    boolean isAnyStoreActive();

    /**
     * Waits for IdleDown to complete
     */
    void waitForIdleDownToComplete();

    /**
     * Shuts down all LDI Inserters
     */
    void shutDownInserters();

    /**
     * Shutdown the controller and all database stores
     */
    void shutDown();
    
    /**
     * Remove all entries in the table.
     * Used by HeaderChannelValue and MonitorChannelValue stores.
     */
    public void clearFswIds();
    
    /**
     * Remove entries in the table that begin with a prefix. Used by
     * HeaderChannelValue and MonitorChannelValue stores.
     *
     * @param prefix
     *            Prefix of channel ids to remove
     */
    public void clearFswIds(final String... prefix);
    
    /**
     * Remove all entries in the table.
     * Used by HeaderChannelValue and MonitorChannelValue stores.
     */
    public void clearSseIds();
    
    /**
     * Remove entries in the table that begin with a prefix. Used by
     * HeaderChannelValue and MonitorChannelValue stores.
     *
     * @param prefix
     *            Prefix of channel ids to remove
     */
    public void clearSseIds(final String... prefix);

    /**
     * @param channelId
     * @param fromSse
     * @return
     */
    Pair<Long, Boolean> getAssociatedId(String channelId, boolean fromSse);

    /**
     * @param si
     * @return
     */
    boolean getUseArchive(StoreIdentifier si);

	/**
     * Restart the archive store controller with a different context
     * configuration
     * 
     * @param contextConfig
     *            the Context Configuration with which to associate the
     *            restarted store
     * @param siList
     *            the Store Identifier of the store to restart
     * 
     * @return the new context configuration
     */
	IContextConfiguration restartArchiveWithNewContext(IContextConfiguration contextConfig, StoreIdentifier... siList);

    /**
     * Check whether or not session stores have been started
     *
     * @return true if session stores have been started, false otherwise
     */
    boolean isSessionStoresStarted();

    /* MPCS-9572 - Remove methods for getting fetch instances. Use the fetch factory instead. */

}
