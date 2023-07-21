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
package jpl.gds.shared.cli.app;

import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.sys.SystemUtilities;


/**
 * This static class is an interface to the Java System properties object that
 * provides setters and accessors for configuration properties used by command
 * line applications. The current application properties are the application
 * root directory, or install directory, and the name of the current
 * application. These are implemented as system properties, because it is common
 * to set them on the Java command line. This does mean that they can be changed
 * outside of this class, though.
 * 
 */
public final class ApplicationConfiguration {

    /**
     * Name of the system property that defines the application installation, or
     * root, directory.
     */
    public static final String ROOT_DIR = "GdsApplicationRoot";

    /**
     * Name of the system property that defines the current application name.
     */
    public static final String APP_NAME = "GdsAppName";

    /**
     * Application name used when application name has not been set.
     */
    public static final String UNKNOWN_APPLICATION = "Unknown";

    /**
     * Installation directory used when directory has not been set.
     */
    public static final String DEFAULT_ROOT_DIR = ".";

    /**
     * Private constructor to enforce static nature of this class.
     */
    private ApplicationConfiguration() {

        SystemUtilities.doNothing();
    }

    /**
     * Resets the System application properties to default values.
     */
    public static void reset() {
        setRootDir(DEFAULT_ROOT_DIR);
        setApplicationName(UNKNOWN_APPLICATION);
    }

    /**
     * Sets the root installation directory of the current installation.
     * 
     * @param dir
     *            the directory to set.
     */
    public static void setRootDir(final String dir) {

        GdsSystemProperties.setSystemProperty(ROOT_DIR, dir);
    }

    /**
     * Gets the root installation directory of the current application.
     * 
     * @return the root install directory, or DEFAULT_ROOT_DIR if none set
     */
    public static String getRootDir() {

        return GdsSystemProperties.getSystemProperty(ROOT_DIR, DEFAULT_ROOT_DIR);
    }

    /**
     * Sets the current application name.
     * 
     * @param app
     *            the name of the application to set
     */
    public static void setApplicationName(final String app) {

        GdsSystemProperties.setSystemProperty(APP_NAME, app);
    }

    /**
     * Gets the current application name.
     * 
     * @return the application name, or the UNKNOWN_APPLICATION constant if not set
     */
    public static String getApplicationName() {

        return GdsSystemProperties.getSystemProperty(APP_NAME, UNKNOWN_APPLICATION);
    }
    
    /**
     * Gets the current application name.
     * 
     * @param defValue the default value if none is defined
     * 
     * @return the application name, or the UNKNOWN_APPLICATION constant if not set
     */
    public static String getApplicationName(final String defValue) {

        return GdsSystemProperties.getSystemProperty(APP_NAME, defValue);
    }
}
