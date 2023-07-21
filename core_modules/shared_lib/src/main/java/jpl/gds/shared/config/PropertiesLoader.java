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


import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.exceptions.PropertyLoadException;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;

/**
 * Helper class for loading, writing, and reloading Properties
 * 
 *
 */
public class PropertiesLoader {

    private String           configFile;
    private final Properties properties = new Properties();
    private final Tracer     log;

    /**
     * Default constructor
     */
    public PropertiesLoader() {
        log = TraceManager.getTracer(Loggers.CONFIG);
    }

    /**
     * Loads properties from a config file
     * 
     * @param configFile
     *            The config file to load properties from
     * @return PropertiesLoader
     */
    public PropertiesLoader load(final String configFile) {

        this.configFile = configFile;

        try (InputStream is = new FileInputStream(configFile)) {
            properties.load(is);
        }
        catch (final IOException e) {
            log.error("Error loading configuration properties from ", configFile, " ", ExceptionTools.getMessage(e));
        }
        return this;
    }

    /**
     * @param requiredProperties
     *            required properties to load
     * @return PropertiesLoader
     * @throws PropertyLoadException
     *             when required properties are missing
     */
    public PropertiesLoader confirmRequiredPropertiesLoaded(final Properties requiredProperties)
            throws PropertyLoadException {

        if (!properties.stringPropertyNames().containsAll(requiredProperties.keySet())) {
            final Set<Object> missingKeys = new HashSet<>(requiredProperties.size());
            missingKeys.removeAll(properties.stringPropertyNames());
            throw new PropertyLoadException("Required configuration properties are missing in "
                    + configFile + ": " + Arrays.toString(missingKeys.toArray()));
        }
        return this;
    }

    /**
     * @return the properties
     */
    public Properties getProperties() {
        return properties;
    }

}
