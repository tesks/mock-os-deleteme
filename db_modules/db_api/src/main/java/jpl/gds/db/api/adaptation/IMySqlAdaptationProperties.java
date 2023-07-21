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
package jpl.gds.db.api.adaptation;

import java.util.Set;

import jpl.gds.common.config.gdsdb.IDatabaseProperties;

public interface IMySqlAdaptationProperties extends IDatabaseProperties {
    /**
     * Constructs the entire JDBC database connection string needed to open a
     * database connection. Most of the values in this connection string are
     * defined in the GDS configuration.
     *
     * Note that the username and password are no longer returned as part of
     * this string, but are instead specified as separate arguments.
     *
     * The returned string is in the form
     *
     * {prefix}{host}:{port}/{databaseName}?{security}{suffix}
     *
     * A sample connection string could look like this:
     *
     * jdbc:mysql://localhost:3306/generic_ampcs_v3_0_0?
     *
     * @return The entire database connection string
     */
    String getDatabaseUrl();

    /**
     * Accessor for the "unit test database" flag.
     *
     * @return True if database unit testing is turned on, false otherwise
     */
    boolean getUnitTestDatabase();

    /**
     * Get the enable state for DriverManager logging.
     *
     * @return true if enabled
     */
    boolean getDmLog();

    /**
     * @return the Database Unit Test Prefix
     */
    String getUnitTestDatabasePrefix();

    /**
     * Gets the number of milliseconds before the ServerSocketWriter's declares the socket attempting to be opened for a
     * database query has failed
     * 
     * @return numer of milliseconds to wait for the socket to open
     */
    int getQuerySocketTimeoutMs();

    /**
     * Accessor for the "extended tables" item.
     *
     * @return Set of table names that can be extended
     *
     *         MPCS-8384 New method
     */
    Set<String> getExtendedTables();

    /**
     * Accessor for the "extended postfix" item.
     *
     * @return Postfix to table names
     *
     *         MPCS-8384 New method
     */
    String getExtendedPostfix();

    /**
     * Get the export state for LDI, indicating that LDI files should be written
     * to an export directory for external consumption.
     *
     * @return true if LDI files should be exported
     */
    boolean getExportLDI();

    /**
     * Get the export state for LDI if any LDI is being written out to the
     * export directory
     * 
     * @return true if any LDI files are being exported
     */
    boolean getExportLDIAny();

    /**
     * Get the export Channel state for LDI.
     * 
     * @return true if LDI Channel files should be exported.
     */
    boolean getExportLDIChannel();

    /**
     * Get the export Command state for LDI.
     * 
     * @return true if LDI Command files should be exported.
     */
    boolean getExportLDICommands();

    /**
     * Get the export Evr state for LDI.
     * 
     * @return true if LDI Evr files should be exported.
     */
    boolean getExportLDIEvrs();

    /**
     * Get the export Frame state for LDI.
     *
     * @return true if LDI Frame files should be exported
     */
    boolean getExportLDIFrame();

    /**
     * Get the export Log state for LDI.
     *
     * @return true if LDI Log files should be exported
     */
    boolean getExportLDILog();

    /**
     * Get the export Packet state for LDI.
     *
     * @return true if LDI Packet files should be exported
     */
    boolean getExportLDIPacket();

    /**
     * Get the export Product state for LDI.
     *
     * @return true if LDI Product files should be exported
     */
    boolean getExportLDIProduct();

    /**
     * Get the export CFDP state for LDI.
     *
     * @return true if LDI CFDP files should be exported
     */
    boolean getExportLDICfdp();

    /**
     * Get the export directory for LDI.
     *
     * @return the export directory
     */
    String getExportLDIDir();

    /**
     * Get the time amount (in milliseconds) for how often LDI should be
     * flushed.
     *
     * @return The flush time in milliseconds
     */
    long getLdiFlushMilliseconds();

    /**
     * Get the save state for LDI, indictaing whether LDI files should be saved
     * or deleted.
     *
     * @return true if LDI files should be saved and not deleted
     */
    boolean getSaveLDI();

    /**
     * Get the limit on the size of LDI files. When they get this big, force
     * them out.
     *
     * @return Limit
     *
     * @version MPCS-7714  New
     */
    long getLdiRowLimit();

    /**
     * Get the ChannelValue prequery state. If true, a prequery will be
     * performed whenever channel ids are provided, not just wildcards or
     * modules.
     *
     * @return true if prequery should be forced
     */
    boolean getAlwaysRunChannelPrequery();

    /**
     * Get the export host offset.
     *
     * @return the export directory
     */
    int getExportLDIHostOffset();

    /**
     * Get the asynchronous serialization queue size for a particular archive.
     *
     * @param tableIdentifier
     *            The identifier for the table whose queue size is wanted
     *
     * @return The asynchronous queue size for the table, or 0 if no size is
     *         configured
     * 
     * @version MPCS-7135 - Added method.
     */
    int getAsyncQueueSize(String tableIdentifier);

    /**
     * Get the concurrent state for LDI, indicating whether to load data
     * concurrently.
     *
     * @return true if LDI files should be run with the "CONCURRENT" option
     */
    boolean getConcurrentLDI();

    /**
     * Gets the Inserter queue length at which the performance status of the
     * queue should be considered YELLOW.
     * 
     * @return queue length, >= 0
     * 
     * @version MPCS-7168 - Added method.
     */
    long getInserterQueueYellowLength();

    /**
     * Gets the Inserter queue length at which the performance status of the
     * queue should be considered RED.
     * 
     * @return queue length, >= 0
     * 
     * @version MPCS-7168 - Added method.
     */
    long getInserterQueueRedLength();

    /**
     * Get the export Channel Aggregate state for LDI.
     * 
     * @return true if LDI Channel Aggregate files should be exported.
     */
    boolean getExportLDIChannelAggregates();

    /**
     * Get the idle shutdown check for Log Message LDI, indicating if the LogMessageLDIStore should check for
     * idleness before shutting down entirely.
     *
     * @return log message LDI store idle check enabled
     */
    boolean getLogLdiShutdownIdleCheck();

    /**
     * Get the duration of time beyond which the Log Message LDI Store will be considered idle.
     *
     * @return log message LDI store idle duration
     */
    long getLogLdiIdleDurationMS();

    /**
     * Get the time interval that the LogMessageLDIStore will repeat idle checks.
     *
     * @return log message LDI store idle check retry interval
     */
    long getLogLdiIdleCheckRetryMS();

    /**
     * Get the maximum number of attempts the LogMessageLDIStore should make when checking for idleness.
     *
     * @return max attempts to check for idleness
     */
    int getLogLdiIdleCheckMaxAttempts();
}