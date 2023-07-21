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

import jpl.gds.shared.config.IGdsConfiguration;
import jpl.gds.shared.template.Templatable;

/**
 * Interface for Database Properties
 */
public interface IDatabaseProperties extends Templatable, IGdsConfiguration {

    /**
     * Gets the monitor channel ID prefix
     * TODO Remove references to database properties specifying MONITOR and HEADER Prefixes
     *
     * @return monitor channel ID prefix followed by dash
     */

    String getMonitorPrefix();

    /**
     * Gets the header channel ID prefix
     * TODO: Remove references to database properties specifying MONITOR and HEADER Prefixes
     *
     * @return header channel ID prefix followed by dash
     */

    String getHeaderPrefix();

    /**
     * Get the ChannelValue --channelTypes default.
     *
     * @return Default
     */
    String getChannelTypesDefault();

    /**
     * Get the EVR --evrTypes default.
     *
     * @return Default
     */
    String getEvrTypesDefault();

    /**
     * Get the Packet --packetTypes default.
     *
     * @return Default
     */
    String getPacketTypesDefault();

    /**
     * Get the commands --uplinkTypes default
     *
     * @return the default characters to be used for --uplinkTypes
     */
    String getUplinkTypesDefault();

    /**
     * Retrieve the unencrypted password
     * 
     * @return the actual, encrypted password value retrieved from config or
     *         manually set.
     */
    String getRawPassword();

    /**
     * Mutator for the database password.
     *
     * @param password
     *            The new password
     */
    void setPassword(String password);

    /**
     * Getter for the decrypted database password. Empty password always verifies true.
     * 
     * @return decrypted password
     *
     * @throws GeneralSecurityException
     *             If unable to decrypt
     */
    String getPassword() throws GeneralSecurityException;

    /**
     * Mutator for the "use database" flag.
     *
     * @param useDatabase
     *            The new value for "use database"
     */
    void setUseDatabase(boolean useDatabase);

    /**
     * Accessor for the "use database" flag.
     *
     * @return True if database operation is turned on, false otherwise
     * 
     */
    boolean getUseDatabase();

    /**
     * Mutator for the database host.
     *
     * @param host
     *            The new host name
     */
    void setHost(String host);

    /**
     * Get the database host.
     *
     * @return The configured database host
     */
    String getHost();

    /**
     * Mutator for the database port.
     *
     * @param port
     *            The new port number
     */
    void setPort(int port);

    /**
     * Get the database port.
     *
     * @return The configured database port
     */
    int getPort();

    /**
     * Mutator for the database username
     *
     * @param userName
     *            The new user name
     */
    void setUsername(String userName);

    /**
     * Accessor for the database username.
     *
     * @return The current configured database username
     */
    String getUsername();

    /**
     * Get the JDBC connection string prefix.
     *
     * @return The configured connection string prefix
     */
    String getConnectionStringPrefix();

    /**
     * Get the JDBC connection string security suffix.
     *
     * @return The configured connection string security suffix
     */
    String getConnectionStringSecurity();

    /**
     * Get the JDBC connection string suffix.
     *
     * @return The configured connection string suffix
     */
    String getConnectionStringSuffix();

    /**
     * @return the class name of the GDBC driver.
     */
    String getJdbcDriverName();

    /**
     * Find out if a particular archive (e.g. Evr Store) should be used.
     *
     * @param tableIdentifier
     *            The identifier for the table to check
     *
     * @return True if the table should be used, false otherwise
     */
    boolean getUseArchive(String tableIdentifier);

    /**
     * Accessor for the store idle frames flag.
     *
     * @return True if idle frames should be stored, false otherwise
     */
    boolean getStoreIdleFrames();

    /**
     * Accessor for the store idle packets flag.
     *
     * @return True if idle/fill packets should be stored, false otherwise
     */
    boolean getStoreIdlePackets();

    /**
     * Accessor for the store bad frames flag.
     *
     * @return True if bad/invalid frames should be stored, false otherwise
     */
    boolean getStoreBadFrames();

    /**
     * Accessor for the store bad packets flag.
     *
     * @return True if bad/invalid packets should be stored, false otherwise
     */
    boolean getStoreBadPackets();

    /**
     * Mutator for the database name property.
     *
     * @param dbName
     *            The new database name
     */
    void setDatabaseName(String dbName);

    /**
     * Get the name of the database to connect to.
     *
     * @return The current configured database name
     */
    String getDatabaseName();

    /**
     * Accessor for the base database name (the part of the database name that
     * is mission independent). The whole database name consists of
     * <mission>_<base-database-name>.
     *
     * @return The current base database name
     */
    String getBaseDatabaseName();
    
    /**
     * Accessor for the reconnection interval, which is the interval between
     * database connection attempts (milliseconds)
     * 
     * @return connection retry interval (milliseconds)
     * 
     */
    long getReconnectDelayMilliseconds();
    
    /**
     * Accessor for the number of database connection attempts (retries).
     * 
     * @return number of connection attempts to make
     * 
     */
    int getReconnectAttempts();
    
}