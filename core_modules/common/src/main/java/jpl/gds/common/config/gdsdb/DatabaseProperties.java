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
package jpl.gds.common.config.gdsdb;

import java.security.GeneralSecurityException;
import java.util.Map;

import jpl.gds.shared.config.GdsHierarchicalProperties;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.string.StringEncrypter;
import jpl.gds.shared.string.StringUtil;

/**
 * This class is responsible for managing a subset of the overall GDS
 * configuration. All database-related information in the configuration is
 * managed by this class.
 *
 * "DM" refers to the JDBC DriverManager.
 * 
 * 05/19/2017: R8: Converted to load from database.properties instead of
 * Gds*Config.xml.
 *
 */
public class DatabaseProperties extends GdsHierarchicalProperties
        implements IDatabaseProperties {
    /**
     * Name of the default properties file.
     */
    private static final String PROPERTY_FILE                 = "database.properties";

    /** configuration file main property block */
    private static final String DB_CONFIG_BLOCK_NAME          = "database";
    private static final String USE_DB_PROPERTY               = "useDatabase";
    private static final String BASE_DB_NAME_PROPERTY         = "baseDatabaseName";

    private static final String CONN_STRING_PREFIX_PROPERTY   = "connectionStringPrefix";
    private static final String CONN_STRING_SECURITY_PROPERTY = "connectionStringSecurity";
    private static final String CONN_STRING_SUFFIX_PROPERTY   = "connectionStringSuffix";

    private static final String HOST_PROPERTY                 = "host";
    private static final String PORT_PROPERTY                 = "port";
    private static final String USERNAME_PROPERTY             = "username";
    private static final String PASSWORD_PROPERTY             = "password";
    private static final String JDBC_DRIVER_PROPERTY          = "jdbcDriver";
    private static final String USE_ARCHIVE_PROPERTY          = "archive";
    
    private static final String             RECONNECT_CONFIG_BLOCK_NAME    = "reconnect";
    private static final String             RECONNECT_ATTEMPTS_PROPERTY    = "attempts";
    private static final String             RECONNECT_DELAY_PROPERTY       = "delayMilliseconds";


    /** Property name for monitor channel prefix.
     * TODO: Remove references to database properties specifying MONITOR and HEADER Prefixes
     */
    private static final String MONITOR_PREFIX_PROPERTY_BLOCK = DB_CONFIG_BLOCK_NAME + ".monitorChannelIdPrefix";

    /** Property name for header channel prefix.
     * Remove references to database properties specifying MONITOR and HEADER Prefixes
     */
    private static final String HEADER_PREFIX_PROPERTY_BLOCK  = DB_CONFIG_BLOCK_NAME + ".headerChannelIdPrefix";

    private static final String STORE_IDLE_PACKETS_PROPERTY   = "storeIdlePackets";
    private static final String STORE_IDLE_FRAMES_PROPERTY    = "storeIdleFrames";
    private static final String STORE_BAD_FRAMES_PROPERTY     = "storeBadFrames";
    private static final String STORE_BAD_PACKETS_PROPERTY    = "storeBadPackets";


    private static final String CHANNEL_TYPES_PROPERTY        = "channelTypesDefault";
    private static final String EVR_TYPES_PROPERTY            = "evrTypesDefault";
    private static final String PACKET_TYPES_PROPERTY         = "packetTypesDefault";
    private static final String UPLINK_TYPES_PROPERTY         = DB_CONFIG_BLOCK_NAME + ".uplinkTypesDefault";


    /** Default monitor channel prefix. */
    private static final String DEFAULT_MONITOR_PREFIX        = "M-";
    /** Default header channel prefix. */
    private static final String DEFAULT_HEADER_PREFIX         = "H-";

    /**
     *  Cache and allow mutation of these properties
     */
    private boolean             useDatabase;
    private String              host;
    private int                 port;
    private String              username;
    private String              password;

    /** The encrypter userd to encrypt database password */
    private StringEncrypter     encrypter                     = null;

    /**
     * A specific database name if the user wants to override the configured one
     * (the main purpose of this is for pointing to the unit test database)
     */
    private String              overrideDatabaseName;

    /**
     *  Cache and allow mutation of these properties
     */
    private final String        baseDatabaseName;

    /**
     * No-Arg Test constructor for DatabaseConfiguration
     */
    public DatabaseProperties() {
        this(new SseContextFlag());
    }

    /**
     * constructor for DatabaseConfiguration
     * 
     * @param sseFlag
     *            the SSE context flag
     */
    public DatabaseProperties(final SseContextFlag sseFlag) {
        super(PROPERTY_FILE, sseFlag);
        this.overrideDatabaseName = null;

        /*
         * Cache the mutable properties here for performance and mutability
         */
        this.baseDatabaseName = getProperty(DB_CONFIG_BLOCK_NAME + "." + BASE_DB_NAME_PROPERTY, "ampcs_v5_0_3");

        /*
         * Cache the mutable properties here for performance and mutability
         */
        this.useDatabase = getBooleanProperty(DB_CONFIG_BLOCK_NAME + "." + USE_DB_PROPERTY, true);
        this.host = getProperty(DB_CONFIG_BLOCK_NAME + "." + HOST_PROPERTY, "localhost");
        this.port = getIntProperty(DB_CONFIG_BLOCK_NAME + "." + PORT_PROPERTY, 3306);
        this.username = getProperty(DB_CONFIG_BLOCK_NAME + "." + USERNAME_PROPERTY, "root");
        this.password = getProperty(DB_CONFIG_BLOCK_NAME + "." + PASSWORD_PROPERTY, "");
    }

    /**
     * Gets the name of the property configuration block for database
     * configuration in the GDS configuration file.
     * 
     * @return the database config block name
     */
    public static String getDbConfigBlockName() {
        return DB_CONFIG_BLOCK_NAME;
    }

    /**
     * Gets the name of the "use archive" property configuration block in the
     * database configuration in the GDS configuration file. Note that this must
     * be appended to the database config block name (with a . separator) to get
     * the whole property name.
     * 
     * @return the "use archive" config block name
     */
    public static String getUseArchiveProperty() {
        return USE_ARCHIVE_PROPERTY;
    }

  
    @Override
    public boolean getUseDatabase() {
        return this.useDatabase;
    }

  
    @Override
    public String getConnectionStringPrefix() {
        return (getProperty(DB_CONFIG_BLOCK_NAME + "." + CONN_STRING_PREFIX_PROPERTY, "jdbc:mariadb://"));
    }

  
    @Override
    public String getConnectionStringSecurity() {
        return (getProperty(DB_CONFIG_BLOCK_NAME + "." + CONN_STRING_SECURITY_PROPERTY, ""));
    }

  
    @Override
    public String getConnectionStringSuffix() {
        return (getProperty(DB_CONFIG_BLOCK_NAME + "." + CONN_STRING_SUFFIX_PROPERTY, ""));
    }

  
    @Override
    public synchronized String getHost() {
        return this.host;
    }

  
    @Override
    public synchronized int getPort() {
        return this.port;
    }

  
    @Override
    public boolean getStoreIdleFrames() {
        return (getBooleanProperty(DB_CONFIG_BLOCK_NAME + "." + STORE_IDLE_FRAMES_PROPERTY, false));
    }

  
    @Override
    public synchronized boolean getStoreIdlePackets() {
        return (getBooleanProperty(DB_CONFIG_BLOCK_NAME + "." + STORE_IDLE_PACKETS_PROPERTY, false));
    }

  
    @Override
    public boolean getStoreBadFrames() {
        return (getBooleanProperty(DB_CONFIG_BLOCK_NAME + "." + STORE_BAD_FRAMES_PROPERTY, false));
    }

  
    @Override
    public boolean getStoreBadPackets() {
        return (getBooleanProperty(DB_CONFIG_BLOCK_NAME + "." + STORE_BAD_PACKETS_PROPERTY, false));
    }

  
    @Override
    public synchronized String getUsername() {
        return this.username;
    }

  
    @Override
    public synchronized String getRawPassword() {
        return this.password;
    }

  
    @Override
    public synchronized void setPassword(final String password) {
        this.password = password;
    }

  
    @Override
    public synchronized String getPassword() throws GeneralSecurityException {
        if ((null == password) || password.isEmpty()) {
            return this.password;
        }
        if (encrypter == null) {
            encrypter = new StringEncrypter();
        }
        return encrypter.decrypt(this.password);
    }

  
    @Override
    public synchronized void setUseDatabase(final boolean useDatabase) {
        /* Cache value */
        this.useDatabase = useDatabase;
    }

  
    @Override
    public synchronized void setHost(final String host) {
        this.host = host;
    }

  
    @Override
    public synchronized void setPort(final int port) {
        this.port = port;
    }

  
    @Override
    public synchronized void setUsername(final String userName) {
        this.username = userName;
    }

  
    @Override
    public String getJdbcDriverName() {
        return (getProperty(DB_CONFIG_BLOCK_NAME + "." + JDBC_DRIVER_PROPERTY, "org.mariadb.jdbc.Driver"));
    }

  
    @Override
    public boolean getUseArchive(final String tableIdentifier) {
        boolean useArchive = true;
        try {
            final String booleanString = getProperty(DB_CONFIG_BLOCK_NAME + "." + USE_ARCHIVE_PROPERTY + "."
                    + tableIdentifier);
            useArchive = GDR.parse_boolean(booleanString);
        }
        catch (final NumberFormatException nfe) {
            useArchive = false;
        }
        catch (final NullPointerException npe) {
            useArchive = false;
        }

        return (useArchive);
    }

    /**
     * Find out if a particular archive is listed in the configuration.
     *
     * @param tableIdentifier
     *            The identifier for the table to check
     *
     * @return True if the table is listed, false otherwise
     */
    public boolean useArchiveExists(final String tableIdentifier) {
        final String str = getProperty(DB_CONFIG_BLOCK_NAME + "." + USE_ARCHIVE_PROPERTY + "." + tableIdentifier);

        if (str == null) {
            return false;
        }
        else {
            return true;
        }
    }

  
    @Override
    public String getChannelTypesDefault() {
        return getProperty(DB_CONFIG_BLOCK_NAME + "." + CHANNEL_TYPES_PROPERTY, "");
    }

  
    @Override
    public String getEvrTypesDefault() {
        return getProperty(DB_CONFIG_BLOCK_NAME + "." + EVR_TYPES_PROPERTY, "");
    }

  
    @Override
    public String getPacketTypesDefault() {
        return getProperty(DB_CONFIG_BLOCK_NAME + "." + PACKET_TYPES_PROPERTY, "");
    }


    @Override
    public String getUplinkTypesDefault() {
        return getProperty(UPLINK_TYPES_PROPERTY, "c");
    }

    /*
     * Remove references to database properties specifying MONITOR and HEADER prefixes
     */
    @Override
    public String getMonitorPrefix() {

        final String prefix = StringUtil.emptyAsNull(getProperty(MONITOR_PREFIX_PROPERTY_BLOCK,
                                                                 DEFAULT_MONITOR_PREFIX));

        return checkPrefix(prefix, DEFAULT_MONITOR_PREFIX, MONITOR_PREFIX_PROPERTY_BLOCK);
    }

  
    /* Remove references to database properties specifying MONITOR and HEADER Prefixes
     */
    @Override
    public String getHeaderPrefix() {

        final String prefix = StringUtil.emptyAsNull(getProperty(HEADER_PREFIX_PROPERTY_BLOCK, DEFAULT_HEADER_PREFIX));

        return checkPrefix(prefix, DEFAULT_HEADER_PREFIX, HEADER_PREFIX_PROPERTY_BLOCK);
    }



    /* Use default prefix if null or doesn't match pattern. Convert to upper case.
     */
    private String checkPrefix(String prefix, final String defaultPrefix, final String prefixProperty) {

        if (prefix == null) {
            prefix = defaultPrefix;
        }
        else if (!prefix.matches("[A-Za-z]{1}[A-Za-z0-9]{0,3}-")) {
            log.warn("Invalid " + prefixProperty + " defined in " + "configuration. (\"" + prefix
                    + "\"). Using default prefix \"" + defaultPrefix + "\" instead.");

            prefix = defaultPrefix;
        }

        return prefix.toUpperCase();
    }

    /**
     * This method is part of a proper singleton class. It prevents using
     * cloning as a hack around the singleton.
     *
     * @return It never returns
     * @throws CloneNotSupportedException
     *             This function always throws this exception
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    @Override
    public void setTemplateContext(final Map<String, Object> map) {
        map.put("databaseHost", this.host);
        map.put("databasePort", this.port);
        map.put("databaseName", this.getDatabaseName());
        map.put("databaseUser", this.username);
    }

    @Override
    public String getPropertyPrefix() {
        return DB_CONFIG_BLOCK_NAME + ".";
    }

  
    @Override
    public synchronized void setDatabaseName(final String dbName) {
        this.overrideDatabaseName = dbName;
    }

  
    @Override
    public synchronized String getDatabaseName() {
        if (this.overrideDatabaseName != null) {
            return (this.overrideDatabaseName);
        }
        return GdsSystemProperties.getSystemMission().toLowerCase() + "_" + getBaseDatabaseName();
    }

  
    @Override
    public String getBaseDatabaseName() {
        return this.baseDatabaseName;
    }
    
  
    @Override
    public int getReconnectAttempts() {
        return getIntProperty(DB_CONFIG_BLOCK_NAME + "." + RECONNECT_CONFIG_BLOCK_NAME + "."
                + RECONNECT_ATTEMPTS_PROPERTY, 10);
    }

  
    @Override
    public long getReconnectDelayMilliseconds() {
        return getLongProperty(DB_CONFIG_BLOCK_NAME + "." + RECONNECT_CONFIG_BLOCK_NAME + "."
                + RECONNECT_DELAY_PROPERTY, 5000L);
    }
}
