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
package jpl.gds.db.mysql.config;

import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import jpl.gds.common.config.gdsdb.IDatabaseProperties;
import jpl.gds.db.api.adaptation.IMySqlAdaptationProperties;
import jpl.gds.shared.config.GdsHierarchicalProperties;
import jpl.gds.shared.spring.context.flag.SseContextFlag;

/**
 * This class is responsible for managing a subset of the overall GDS
 * configuration. All database-related information in the configuration is
 * managed by this singleton object.
 *
 * "DM" refers to the JDBC DriverManager.
 * 
 * R8: Converted to load from database.properties instead of
 * Gds*Config.xml.
 *
 */
public class MySqlArchiveAdaptationProperties extends GdsHierarchicalProperties
        implements IMySqlAdaptationProperties {
    /**
     * Name of the default properties file.
     */
    private static final String             PROPERTY_FILE                  = "mysql.properties";

    /** configuration file main property block */
    private static final String             MYSQL_CONFIG_BLOCK_NAME        = "database.mysql";

    private static final String             UNIT_TEST_DB_PROPERTY          = "internal.unitTestDatabase";

    private static final String             POSTFIX_DB_PROPERTY            = "internal.extendedPostfix";
    private static final String             POSTFIX_DB_DEFAULT             = "2";
    private static final String             TABLES_DB_PROPERTY             = "internal.extendedTableList";

    private static final String             UNIT_TEST_DB_PREFIX            = "internal.unitTestDatabasePrefix";
    private static final String             DM_LOG_PROPERTY                = "dmLog";

    /* MPCS-7135  - Add asyncQueueSize property */
    private static final String             ASYNC_QUEUE_SIZE_PROPERTY      = "internal.asyncQueueSize";

    /** MPCS-7733  Now ldiFlushMilliseconds */
    private static final String             LDI_FLUSH_PROPERTY             = "internal.ldiFlushMilliseconds";

    private static final String             SAVE_LDI_PROPERTY              = "saveLDI";
    private static final String             CONCURRENT_LDI_PROPERTY        = "concurrentLDI";

    /** MPCS-7714  */
    private static final String             LDI_ROW_LIMIT_PROPERTY         = "ldiRowLimit";

    private static final String             EXPORT_LDI_PROPERTY            = "exportLDI";
    private static final String             EXPORT_LDI_CHANNEL_PROPERTY    = "exportLDIChannel";
    private static final String             EXPORT_LDI_EVR_PROPERTY        = "exportLDIEvr";
    private static final String             EXPORT_LDI_PRODUCT_PROPERTY    = "exportLDIProduct";
    private static final String             EXPORT_LDI_COMMAND_PROPERTY    = "exportLDICommand";
    private static final String             EXPORT_LDI_FRAME_PROPERTY      = "exportLDIFrame";
    private static final String             EXPORT_LDI_LOG_PROPERTY        = "exportLDILog";
    private static final String             EXPORT_LDI_PACKET_PROPERTY     = "exportLDIPacket";
    private static final String             EXPORT_LDI_DIR_PROPERTY        = "exportLDIDir";
    private static final String             EXPORT_LDI_HOST_OFFSET         = "exportLDIHostOffset";
    private static final String             EXPORT_LDI_CFDP_PROPERTY       = "exportLDICfdp";

    private static final String             EXPORT_LDI_CHANNEL_AGGREGATE_PROPERTY = "exportLDIChannelAggregate";

    private static final String             LOG_MESSAGE_LDI_PROPERTY       = "logMessageLDI";
    private static final String             SHUTDOWN_IDLE_CHECK            = "shutdownIdleCheck";
    private static final String             IDLE_DURATION_MS               = "idleDurationMS";
    private static final String             IDLE_CHECK_RETRY_MS            = "idleCheckRetryMS";
    private static final String             IDLE_CHECK_MAX_ATTEMPTS        = "idleCheckMaxAttempts";
    
    private static final String             ALWAYS_PREQUERY_PROPERTY       = "alwaysRunChannelPrequery";

    /* MPCS-7168 -  Added queue red/yellow level properties. */
    /**
     * Configuration property for the inserter queue length at which health
     * should be considered YELLOW.
     */
    private static final String             INS_QUEUE_YELLOW_PROPERTY      = "internal.inserterQueueYellowLength";
    /**
     * Configuration property for the inserter queue length at which health
     * should be considered RED.
     */
    private static final String             INS_QUEUE_RED_PROPERTY         = "internal.inserterQueueRedLength";

    private static final String             QS_PROPERTY_BLOCK              = "internal.querySocket";
    private static final String             TIMEOUT_MS_PROPERTY            = "timeoutMS";

    private static final boolean            DEFAULT_SAVE_LDI               = false;
    private static final boolean            DEFAULT_CONCURRENT_LDI         = true;
    private static final boolean            DEFAULT_EXPORT_LDI             = false;
    private static final boolean            DEFAULT_EXPORT_LDI_CHANNEL     = false;
    private static final boolean            DEFAULT_EXPORT_LDI_EVR         = false;
    private static final boolean            DEFAULT_EXPORT_LDI_PRODUCT     = false;
    private static final boolean            DEFAULT_EXPORT_LDI_COMMAND     = false;
    private static final boolean            DEFAULT_EXPORT_LDI_FRAME       = false;
    private static final boolean            DEFAULT_EXPORT_LDI_LOG         = false;
    private static final boolean            DEFAULT_EXPORT_LDI_PACKET      = false;
    private static final boolean            DEFAULT_EXPORT_LDI_CFDP        = false;
    private static final String             DEFAULT_EXPORT_LDI_DIR         = "/tmp/export";
    private static final int                DEFAULT_EXPORT_LDI_HOST_OFFSET = -1;

    private static final boolean            DEFAULT_EXPORT_LDI_CHANNEL_AGGREGATE = false;
    
    private static final boolean            DEFAULT_DM_LOG                 = true;
    private static final boolean            DEFAULT_ALWAYS_PREQUERY        = true;

    private static final boolean            DEFAULT_SHUTDOWN_IDLE_CHECK    = true;
    private static final long               DEFAULT_IDLE_DURATION_MS       = 1000L;
    private static final long               DEFAULT_IDLE_CHECK_RETRY_MS    = 500L;
    private static final int                DEFAULT_IDLE_CHECK_MAX_ATTEMPTS = 10;

    /** MPCS-7714  New */
    private static final long               DEFAULT_LDI_ROW_LIMIT          = 10000L;

    /** MPCS-8384 Cache stuff here for performance */
    private final String                    extendedPostfix;

    private final Set<String>               extendedTables;

    private final IDatabaseProperties dbProperties;


    /**
     * Test constructor
     */
    public MySqlArchiveAdaptationProperties(final IDatabaseProperties dbProperties) {
        this(dbProperties, new SseContextFlag());
    }

    /**
     * Constructor for MySqlArchiveAdaptationProperties
     * Delegates to an instance of IDatabaseProperties passed in as an argument
     * 
     * @param dbProperties
     *            the Singleton IDatabaseProperties from Common.
     * @param sseFlag
     *            The SSE context flag
     */
    public MySqlArchiveAdaptationProperties(final IDatabaseProperties dbProperties, final SseContextFlag sseFlag) {
        super(PROPERTY_FILE, sseFlag);
        this.dbProperties = dbProperties;

        /** MPCS-8384 - Cache flags here for performance */

        this.extendedPostfix = getProperty(MYSQL_CONFIG_BLOCK_NAME + '.' + POSTFIX_DB_PROPERTY, POSTFIX_DB_DEFAULT);
        this.extendedTables = Collections.<String>unmodifiableSet(new HashSet<String>(getListProperty(MYSQL_CONFIG_BLOCK_NAME
                + '.' + TABLES_DB_PROPERTY, null, ",")));
    }

    /**
     * Gets the name of the property configuration block for database
     * configuration in the GDS configuration file.
     * 
     * @return the database config block name
     */
    public static String getDbConfigBlockName() {
        return MYSQL_CONFIG_BLOCK_NAME;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized String getDatabaseUrl() {
        final StringBuilder sb = new StringBuilder();
        final String security = getConnectionStringSecurity();
        final String suffix = getConnectionStringSuffix();

        sb.append(getConnectionStringPrefix());
        sb.append(getHost()).append(':');
        sb.append(getPort()).append('/');
        sb.append(getDatabaseName()).append('?');
        sb.append(security);

        if (!security.isEmpty() && !suffix.isEmpty()) {
            sb.append('&');
        }

        sb.append(suffix);

        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getExtendedPostfix() {
        return this.extendedPostfix;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Set<String> getExtendedTables() {
        return this.extendedTables;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean getUnitTestDatabase() {
        return (getBooleanProperty(MYSQL_CONFIG_BLOCK_NAME + "." + UNIT_TEST_DB_PROPERTY, true));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized int getAsyncQueueSize(final String tableIdentifier) {
        final int size = getIntProperty(MYSQL_CONFIG_BLOCK_NAME + "." + ASYNC_QUEUE_SIZE_PROPERTY + "."
                + tableIdentifier, 0);

        if (size < 0) {
            return 0;
        }

        return size;
    }

    /** MPCS-7733  No more batchSize */

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized long getLdiFlushMilliseconds() {
        return getLongProperty(MYSQL_CONFIG_BLOCK_NAME + "." + LDI_FLUSH_PROPERTY, 4000L);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean getDmLog() {
        return getBooleanProperty(MYSQL_CONFIG_BLOCK_NAME + "." + DM_LOG_PROPERTY, DEFAULT_DM_LOG);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean getSaveLDI() {
        return getBooleanProperty(MYSQL_CONFIG_BLOCK_NAME + "." + SAVE_LDI_PROPERTY, DEFAULT_SAVE_LDI);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized long getLdiRowLimit() {
        return getLongProperty(MYSQL_CONFIG_BLOCK_NAME + "." + LDI_ROW_LIMIT_PROPERTY, DEFAULT_LDI_ROW_LIMIT);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean getConcurrentLDI() {
        return getBooleanProperty(MYSQL_CONFIG_BLOCK_NAME + "." + CONCURRENT_LDI_PROPERTY, DEFAULT_CONCURRENT_LDI);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean getExportLDI() {
        return getBooleanProperty(MYSQL_CONFIG_BLOCK_NAME + "." + EXPORT_LDI_PROPERTY, DEFAULT_EXPORT_LDI);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean getExportLDIAny() {
        return getExportLDI() || getExportLDIChannel() || getExportLDICommands() || getExportLDIEvrs()
                || getExportLDIFrame() || getExportLDILog() || getExportLDIPacket() || getExportLDIProduct();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean getExportLDIChannel() {
        return getBooleanProperty(MYSQL_CONFIG_BLOCK_NAME + "." + EXPORT_LDI_CHANNEL_PROPERTY,
                                  DEFAULT_EXPORT_LDI_CHANNEL);
    }

    @Override
    public synchronized boolean getExportLDIChannelAggregates() {
        return getBooleanProperty(MYSQL_CONFIG_BLOCK_NAME + "." + EXPORT_LDI_CHANNEL_AGGREGATE_PROPERTY,
                DEFAULT_EXPORT_LDI_CHANNEL_AGGREGATE);
    }    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean getExportLDICommands() {
        return getBooleanProperty(MYSQL_CONFIG_BLOCK_NAME + "." + EXPORT_LDI_COMMAND_PROPERTY,
                                  DEFAULT_EXPORT_LDI_COMMAND);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean getExportLDIEvrs() {
        return getBooleanProperty(MYSQL_CONFIG_BLOCK_NAME + "." + EXPORT_LDI_EVR_PROPERTY, DEFAULT_EXPORT_LDI_EVR);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean getExportLDIFrame() {
        return getBooleanProperty(MYSQL_CONFIG_BLOCK_NAME + "." + EXPORT_LDI_FRAME_PROPERTY, DEFAULT_EXPORT_LDI_FRAME);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean getExportLDILog() {
        return getBooleanProperty(MYSQL_CONFIG_BLOCK_NAME + "." + EXPORT_LDI_LOG_PROPERTY, DEFAULT_EXPORT_LDI_LOG);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean getExportLDIPacket() {
        return getBooleanProperty(MYSQL_CONFIG_BLOCK_NAME + "." + EXPORT_LDI_PACKET_PROPERTY,
                                  DEFAULT_EXPORT_LDI_PACKET);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean getExportLDIProduct() {
        return getBooleanProperty(MYSQL_CONFIG_BLOCK_NAME + "." + EXPORT_LDI_PRODUCT_PROPERTY,
                                  DEFAULT_EXPORT_LDI_PRODUCT);
    }

    /**
     * {@inheritDoc}
     */
	@Override
	public boolean getExportLDICfdp() {
        return getBooleanProperty(MYSQL_CONFIG_BLOCK_NAME + "." + EXPORT_LDI_CFDP_PROPERTY,
                DEFAULT_EXPORT_LDI_CFDP);
	}

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized String getExportLDIDir() {
        return getProperty(MYSQL_CONFIG_BLOCK_NAME + "." + EXPORT_LDI_DIR_PROPERTY, DEFAULT_EXPORT_LDI_DIR);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized int getExportLDIHostOffset() {
        return getIntProperty(MYSQL_CONFIG_BLOCK_NAME + "." + EXPORT_LDI_HOST_OFFSET, DEFAULT_EXPORT_LDI_HOST_OFFSET);
    }
    
    /**
     * {@inheritDoc}
     * 
     * @version MPCS-9421 - Now just a passthrough to the database properties object
     */
    @Override
    public synchronized int getReconnectAttempts() {
        return dbProperties.getReconnectAttempts();
    }

    /**
     * {@inheritDoc}
     *
     * @version MPCS-9421 - Now just a passthrough to the database properties object
     */
    @Override
    public synchronized long getReconnectDelayMilliseconds() {
        return dbProperties.getReconnectDelayMilliseconds();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized boolean getAlwaysRunChannelPrequery() {
        return getBooleanProperty(MYSQL_CONFIG_BLOCK_NAME + "." + ALWAYS_PREQUERY_PROPERTY, DEFAULT_ALWAYS_PREQUERY);
    }

    // MPCS-2473 -  Remove "always use strict"

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized String getUnitTestDatabasePrefix() {
        return getProperty(MYSQL_CONFIG_BLOCK_NAME + "." + UNIT_TEST_DB_PREFIX);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized long getInserterQueueYellowLength() {
        return Math.max(0, getLongProperty(MYSQL_CONFIG_BLOCK_NAME + "." + INS_QUEUE_YELLOW_PROPERTY, 30));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized long getInserterQueueRedLength() {
        return Math.max(0, getLongProperty(MYSQL_CONFIG_BLOCK_NAME + "." + INS_QUEUE_RED_PROPERTY, 80));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized int getQuerySocketTimeoutMs() {
        return Math.max(getIntProperty(MYSQL_CONFIG_BLOCK_NAME + "." + QS_PROPERTY_BLOCK + "." + TIMEOUT_MS_PROPERTY,
                                       10000),
                        0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPropertyPrefix() {
        return MYSQL_CONFIG_BLOCK_NAME + ".";
    }

    /*
     * The following methods delegate to DatabaseProperties methods.
     * This is in lieu of the adaptation of DatabaseProperties extending DatabaseProperties,
     * which would cause all kinds of unwanted, architectural complexity.
     */

    /*
     * BEGIN: MPCS-5254: make channel ID special prefixes
     * configurable
     * 
     * TODO: MPCS-8984 : Remove references to database properties specifying MONITOR and HEADER
     * Prefixes
     */
    /**
     * {@inheritDoc}
     */
    @Override
    public String getMonitorPrefix() {
        return dbProperties.getMonitorPrefix();
    }

    /*
     * TODO: MPCS-8984 - Remove references to database properties specifying MONITOR and HEADER
     * Prefixes
     */
    @Override
    public String getHeaderPrefix() {
        return dbProperties.getHeaderPrefix();
    }
    /*
     * END: MPCS-5254: make channel ID special prefixes
     * configurable
     */

    /* MPCS-8984: Removed references to database properties specifying SSE Prefixes */

    /**
     * {@inheritDoc}
     */
    @Override
    public String getChannelTypesDefault() {
        return dbProperties.getChannelTypesDefault();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getEvrTypesDefault() {
        return dbProperties.getEvrTypesDefault();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPacketTypesDefault() {
        return dbProperties.getPacketTypesDefault();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUplinkTypesDefault() { return dbProperties.getUplinkTypesDefault(); }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getUseDatabase() {
        return dbProperties.getUseDatabase();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setHost(final String host) {
        dbProperties.setHost(host);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setUseDatabase(final boolean useDatabase) {
        dbProperties.setUseDatabase(useDatabase);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setPort(final int port) {
        dbProperties.setPort(port);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setUsername(final String userName) {
        dbProperties.setUsername(userName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setPassword(final String password) {
        dbProperties.setPassword(password);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getPassword() throws GeneralSecurityException {
        return dbProperties.getPassword();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getHost() {
        return dbProperties.getHost();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getPort() {
        return dbProperties.getPort();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUsername() {
        return dbProperties.getUsername();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getConnectionStringPrefix() {
        return dbProperties.getConnectionStringPrefix();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getConnectionStringSuffix() {
        return dbProperties.getConnectionStringSuffix();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getConnectionStringSecurity() {
        return dbProperties.getConnectionStringSecurity();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getJdbcDriverName() {
        return dbProperties.getJdbcDriverName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getRawPassword() {
        return dbProperties.getRawPassword();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getUseArchive(final String tableIdentifier) {
        return dbProperties.getUseArchive(tableIdentifier);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getStoreBadFrames() {
        return dbProperties.getStoreBadFrames();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getStoreIdleFrames() {
        return dbProperties.getStoreIdleFrames();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getStoreIdlePackets() {
        return dbProperties.getStoreIdlePackets();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getStoreBadPackets() {
        return dbProperties.getStoreBadPackets();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setDatabaseName(final String dbName) {
        dbProperties.setDatabaseName(dbName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getDatabaseName() {
        return dbProperties.getDatabaseName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getBaseDatabaseName() {
        return dbProperties.getBaseDatabaseName();
    }

    @Override
    public boolean getLogLdiShutdownIdleCheck() {
        return getBooleanProperty(MYSQL_CONFIG_BLOCK_NAME + "." + INTERNAL_PROPERTY_PREFIX + "."
                + LOG_MESSAGE_LDI_PROPERTY + "." + SHUTDOWN_IDLE_CHECK, DEFAULT_SHUTDOWN_IDLE_CHECK);
    }

    @Override
    public long getLogLdiIdleDurationMS() {
        return getLongProperty(MYSQL_CONFIG_BLOCK_NAME + "." + INTERNAL_PROPERTY_PREFIX + "."
                + LOG_MESSAGE_LDI_PROPERTY + "." + IDLE_DURATION_MS, DEFAULT_IDLE_DURATION_MS);
    }

    @Override
    public long getLogLdiIdleCheckRetryMS() {
        return getLongProperty(MYSQL_CONFIG_BLOCK_NAME + "." + INTERNAL_PROPERTY_PREFIX + "."
                + LOG_MESSAGE_LDI_PROPERTY + "." + IDLE_CHECK_RETRY_MS, DEFAULT_IDLE_CHECK_RETRY_MS);
    }

    @Override
    public int getLogLdiIdleCheckMaxAttempts() {
        return getIntProperty(MYSQL_CONFIG_BLOCK_NAME + "." + INTERNAL_PROPERTY_PREFIX + "."
        + LOG_MESSAGE_LDI_PROPERTY + "." + IDLE_CHECK_MAX_ATTEMPTS, DEFAULT_IDLE_CHECK_MAX_ATTEMPTS);
    }

    @Override
    public void setTemplateContext(final Map<String, Object> map) {
        //MPCS-9421 -  Nothing was implemented here!
        dbProperties.setTemplateContext(map);
    }

}
