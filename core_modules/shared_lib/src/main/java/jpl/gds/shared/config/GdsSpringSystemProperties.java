/*
 * Copyright 2006-2017. California Institute of Technology.
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
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySources;

import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.spring.context.flag.SseContextFlag;

/**
 * This is a static class used to setup spring specific system properties in order to support hierarchical loading of
 * spring properties files
 * 
 *
 */
public class GdsSpringSystemProperties {
    /** The Spring placeHolder for scanpath */
    public static final String         SCAN_PATH                 = "springScanPath";

    /** An AtomicBoolean that prevents multiple calls to load the SpringSystemProperties */
    private static final AtomicBoolean springPropertiesAreLoaded = new AtomicBoolean(false);

    private GdsSpringSystemProperties() {
        // empty private constructor
    }

    /**
     * @param scanPath
     *            a list of String that represents the package names to include in the scanpath
     */
    public static void setScanPath(final List<String> scanPath) {
        setSystemListProperty(SCAN_PATH, scanPath);

    }

    /**
     * Sets the specified system property with a comma-delimited String representing the list
     * 
     * @param sysPropName
     *            the name of the System property to set (sysPropName != null)
     * 
     * @param list
     *            List of strings to delimit with commas and assign to system property (list != null)
     */
    public static void setSystemListProperty(final String sysPropName, final List<String> list) {
        setSystemProperty(sysPropName, ",", list);
    }

    /**
     * Sets the specified system property with a string version of the list of strings, delimited by the specified
     * delimiter
     * 
     * @param sysPropName
     *            the name of the System property to set (sysPropName != null)
     * @param delimiter
     *            the delimiter with which to separate the elements of the list in the string (delimiter != null)
     * @param list
     *            the list to use (list != null)
     */
    public static void setSystemProperty(final String sysPropName, final String delimiter, final List<String> list) {
        GdsSystemProperties.setSystemProperty(sysPropName, String.join(delimiter, list));
    }

    /**
     * Construct a Spring PropertySources object based upon a filename, and the current AMPCS Hierarchical Property Path
     * 
     * @param env
     *            the Spring Configurable Environment to update
     * @param tracer
     *            the Tracer to use for logging.
     * @param springPropertiesFilename
     *            the name of the Spring Properties File to read along the GdsHierarchicalProperties path
     * @param sseFlag
     *            The SSE context flag
     * @return a PropertySources object created from the AMPCS GdsHierarchicalProperties path.
     * @throws IOException
     *             if the System file cannot be loaded
     */
    public static PropertySources loadAMPCSProperySources(final ConfigurableEnvironment env, final Tracer tracer,
                                                          final String springPropertiesFilename,
                                                          final SseContextFlag sseFlag)
            throws IOException {
        return loadAMPCSProperySources(env, tracer, springPropertiesFilename, true, sseFlag);
    }

    /**
     * Construct a Spring PropertySources object based upon a filename, and the current AMPCS Hierarchical Property Path
     * 
     * @param env
     *            the Spring Configurable Environment to update
     * @param tracer
     *            the Tracer to use for logging.
     * @param springPropertiesFilename
     *            the name of the Spring Properties File to read along the GdsHierarchicalProperties path
     * @param systemRequired
     *            true means that the System (first) property file load is NOT optional
     *            false means that the System (first) property file load IS optional
     * @param sseFlag
     *            The SSE context flag
     * @return a PropertySources object created from the AMPCS GdsHierarchicalProperties path.
     * @throws IOException
     *             if the System file cannot be loaded, and the 'systemRequired' flag is set
     */
    public static PropertySources loadAMPCSProperySources(final ConfigurableEnvironment env, final Tracer tracer,
                                                          final String springPropertiesFilename,
                                                          final boolean systemRequired, final SseContextFlag sseFlag)
            throws IOException {
        final MutablePropertySources sources = env.getPropertySources();

        /* Only allow Spring System Properties to be loaded one time */
        if (!springPropertiesAreLoaded.getAndSet(true)) {
            /*
             * Retrieve AMPCS Hierarchical Load Path
             */
            final List<String> fullConfigPath = GdsSystemProperties.getFullConfigPathList(sseFlag.isApplicationSse());

            /*
             * Iterate over path and add the passed-in filename to the end
             */
            boolean firstAttempt = true;
            for (final String path : fullConfigPath) {
                boolean fileLoaded = false;

                final String fileName = new StringBuilder(path).append(File.separator).append(springPropertiesFilename)
                                                               .toString();
                final File file = new File(fileName);
                if (file.exists()) {
                    tracer.debug("LOADING       : Property File: " + file.getAbsolutePath());
                    final Properties p = new Properties();
                    try {
                        p.load(new FileReader(file));
                        sources.addFirst(new PropertiesPropertySource(file.getAbsolutePath(), p));
                        fileLoaded = true;
                    }
                    catch (final IOException e) {
                        tracer.error("FAILED TO LOAD: Property File: " + file.getAbsolutePath());
                    }
                }
                else {
                    tracer.debug("SKIPPING      : Property File: " + file.getAbsolutePath());
                }
                if (systemRequired && firstAttempt && !fileLoaded) {
                    throw new IOException("Could not load System level Properties file: " + file.getAbsolutePath());
                }
                firstAttempt = false;
            }
        }
        return sources;
    }
}
