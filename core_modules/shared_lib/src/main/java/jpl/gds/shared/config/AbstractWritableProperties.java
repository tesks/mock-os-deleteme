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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.cli.ParseException;

import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.exceptions.PropertyLoadException;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.time.TimeUtility;
import jpl.gds.shared.util.HostPortUtility;

/**
 * Abstraction class for a writeable configuration file.
 * 
 * Handles common operations for persistent processes that
 * need to write configuration to a file and handle reloading of properties
 * 
 *
 */
public abstract class AbstractWritableProperties extends GdsHierarchicalProperties implements IWritableProperties {
    private static final String PROPERTY_FILE_SUFFIX         = "_config.properties";
    
    private String        writableConfigDir;

    
    /**
     * @param baseFileName
     *            The property file to load
     * @param sseFlag
     *            The SSE context flag
     */
    protected AbstractWritableProperties(final String baseFileName, final SseContextFlag sseFlag) {
        super(baseFileName, sseFlag);

        log.trace("Looking up writable config ", getPropertyPrefix(), ".",
                  IWritableProperties.WRITABLE_DIRECTORY_PROPERTY,
                  "\n and default property is ", WRITABLE_CONFIG_FILE_PATH, HostPortUtility.getLocalHostName(), "_",
                  getConfigName(), PROPERTY_FILE_SUFFIX);

        this.writableConfigDir = getProperty(getPropertyPrefix()
                + IWritableProperties.WRITABLE_DIRECTORY_PROPERTY, WRITABLE_CONFIG_FILE_PATH);
    }
    
    @Override
    public String getWritablePropertiesName() {
        return HostPortUtility.getLocalHostName() + "_" + getConfigName()
                + PROPERTY_FILE_SUFFIX;
    }

    @Override
    public String getWritablePropertiesPath() {
        return getWritablePropertiesDir() + File.separator + getWritablePropertiesName();
    }

    @Override
    public String getWritablePropertiesDir() {
        return writableConfigDir;
    }

    @Override
    public void setWritablePropertiesDir(final String dir) {
        this.writableConfigDir = dir;
    }

    @Override
    public Properties getAllWriteableProperties() {
        final Properties p = new Properties();
        for (final Entry<Object, Object> prop : properties.entrySet()) {
            if (((String) prop.getKey()).startsWith(getPropertyPrefix())) {
                p.put(prop.getKey(), prop.getValue());
            }
        }
        return p;

    }

    @Override
    public void createAndPopulateNewConfigFile() throws ParseException {
        log.warn("Writable configuration file ", getWritablePropertiesPath(), " doesn't exist");

        try {
            writeProperties(copyProperties(getWritablePropertiesPath()));

            log.info("Created a new writable configuration file ", getWritablePropertiesPath(),
                     " using default application properties=",
                     getPropertyPrefix(), IWritableProperties.WRITABLE_DIRECTORY_PROPERTY);
        }
        catch (final Exception ie) {
            log.error("Failed to create new writable configuration file: ", getWritablePropertiesPath(), " ",
                      ExceptionTools.getMessage(ie));
            log.error("Is the directory hierarchy writable? Or change the config file location via AMPCS property ",
                      getPropertyPrefix(), IWritableProperties.WRITABLE_DIRECTORY_PROPERTY);
            throw new ParseException(ExceptionTools.getMessage(ie));
        }
    }

    @Override
    public Properties copyProperties(final String filename) throws IOException {
        final Properties properties = new OrderedProperties();

        final Properties writeableProperties = getAllWriteableProperties();

        for (final Enumeration<?> propertyNames = writeableProperties.propertyNames(); propertyNames.hasMoreElements();) {
            final Object key = propertyNames.nextElement();
            properties.put(key, writeableProperties.get(key));
        }

        // Create directories if missing
        final Path p = Paths.get(filename);
        if (p.getParent() != null && Files.notExists(p.getParent())) {
            log.info("Attempting to create missing directories ", p.getParent());
            Files.createDirectories(p.getParent());
        }

        try (OutputStream fos = new FileOutputStream(filename)) {
            final DateFormat dateFormatter = TimeUtility.getFormatterFromPool();
            properties.store(fos, "Auto-created " + dateFormatter.format(new Date())
                    + " using default application properties");
            TimeUtility.releaseFormatterToPool(dateFormatter);
        }
        return properties;
    }

    @Override
    public void writeProperties(final Properties p) throws PropertyLoadException {

        final Properties propertiesToCopy = new PropertiesLoader().load(getWritablePropertiesPath())
                                                                  .confirmRequiredPropertiesLoaded(p).getProperties();

            properties.clear();
            for (final Enumeration<?> propertyNames = propertiesToCopy.propertyNames(); propertyNames.hasMoreElements();) {
                final Object key = propertyNames.nextElement();
                properties.put(key, propertiesToCopy.get(key));
            }
    }

}
