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
package jpl.gds.shared.config;

import java.io.IOException;
import java.util.Properties;

import org.apache.commons.cli.ParseException;

import jpl.gds.shared.exceptions.PropertyLoadException;

/**
 * Interface for a writable properties file outside the standard GdsHierarchicalProperties loading path. Useful for
 * persistent servers to write its current configuration, which may have changed during runtime
 * 
 */
public interface IWritableProperties {

    /** The writable config directory property */
    public static final String WRITABLE_DIRECTORY_PROPERTY = "writable.config.directory";

    /** Base directory path for an <IWritableProperties> configuration */
    public static final String WRITABLE_CONFIG_FILE_PATH   = "/ammos/ampcs/services/";

    /**
     * Gets the unique writable configuration file name prefix
     * 
     * @return The writable properties file prefix
     */
    public String getConfigName();

    /**
     * Gets the writable properties directory location
     * 
     * @return Writable properties directory path
     */
    public String getWritablePropertiesDir();

    /**
     * Sets the writable properties directory location
     */
    public void setWritablePropertiesDir(String dir);

    /**
     * Gets the writable properties file name
     * 
     * @return Writable properties file name
     */
    public String getWritablePropertiesName();

    /**
     * Gets the path to the writable properties file
     * 
     * @return Path location of the writable properties file
     */
    public String getWritablePropertiesPath();

    /**
     * Creates and writes a new properties configuration file
     * 
     * @throws ParseException
     *             if an error occurs writing the properties file
     */
    public void createAndPopulateNewConfigFile() throws ParseException;

    /**
     * Gets all the writable properties
     * 
     * @return all writable properties
     */
    public Properties getAllWriteableProperties();

    /**
     * Writes the specified properties
     * 
     * @param p
     *            properties to write
     * @throws PropertyLoadException
     *             If an error occurs writing properties
     */
    public void writeProperties(final Properties p) throws PropertyLoadException;

    /**
     * Copies properties from a file to Properties object
     * 
     * @param filename
     *            The configuration file to load properties from
     * @return Properties loaded from the file
     * @throws IOException
     *             If an error occurs copying the properties
     */
    public Properties copyProperties(final String filename) throws IOException;

}
