/*
 * Copyright 2006-2019. California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government sponsorship acknowledged.
 *
 *  This software is subject to U. S. export control laws and
 *  regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 *  extent that the software is subject to U.S. export control laws
 *  and regulations, the recipient has the responsibility to obtain
 *  export licenses or other export authority as may be required
 *  before exporting such information to foreign countries or
 *  providing access to foreign nationals.
 */

package jpl.gds.cfdp.processor.config;

import cfdp.engine.ampcs.OrderedProperties;
import jpl.gds.cfdp.processor.error.MissingRequiredPropertiesException;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class ConfigurationLoader {

    private String configFile;
    private Properties properties = new OrderedProperties();
    private Tracer log;

    /**
     * Default constructor
     */
    public ConfigurationLoader() {
        log = TraceManager.getTracer(Loggers.CFDP);
    }

    public ConfigurationLoader load(String configFile) {

        this.configFile = configFile;

        try (InputStream is = new FileInputStream(configFile)) {
            properties.load(is);
        } catch (IOException e) {
            log.error("Error loading configuration properties from " + configFile);
            e.printStackTrace();
        }

        return this;
    }

    public ConfigurationLoader confirmRequiredPropertiesLoaded(Collection<String> requiredKeys)
            throws MissingRequiredPropertiesException {

        if (!properties.stringPropertyNames().containsAll(requiredKeys)) {
            Set<Object> missingKeys = new HashSet<>(requiredKeys);
            missingKeys.removeAll(properties.stringPropertyNames());
            throw new MissingRequiredPropertiesException("Required configuration properties are missing in "
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