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
package jpl.gds.cli.legacy.app;

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import jpl.gds.shared.sys.IQuitSignalHandler;

/**
 * This is a basic attempt to create a top-level interface to be implemented by
 * all of our command line applications.
 * 
 * MPCS-9677 - 4/23/18 - Extend IQuitSignalHandler
 */
public interface CommandLineApp extends IQuitSignalHandler {
    
    /** indicates whether the showHelp() method has ever been called. */
    public static final AtomicBoolean helpDisplayed    = new AtomicBoolean(false);

    /** indicates whether the showVersion() method has ever been called. */
    public static final AtomicBoolean versionDisplayed = new AtomicBoolean(false);

    /**
     * Creates command line options and an Options object.
     * @return Options object
     */
    Options createOptions();

    /**
     * Configures the application from the parsed command line.
     * @param commandLine CommandLine parsed command line
     * @throws ParseException if there is an error configuring the application
     */
    void configure(final CommandLine commandLine)
            throws ParseException;

    /**
     * Displays the application help text to the console.
     */
    void showHelp();
}
