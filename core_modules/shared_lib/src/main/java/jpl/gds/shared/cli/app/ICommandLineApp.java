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

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.cli.ParseException;

import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.sys.IQuitSignalHandler;

/**
 * This is a top-level interface to be implemented by all command line
 * applications.
 * 
 *
 */
public interface ICommandLineApp extends IQuitSignalHandler {
    /** indicates whether the showHelp() method has ever been called. */
    public static final AtomicBoolean helpDisplayed    = new AtomicBoolean(false);

    /** indicates whether the showVersion() method has ever been called. */
    public static final AtomicBoolean versionDisplayed = new AtomicBoolean(false);

    /**
     * Creates command line options enclosed in an BaseCommandOptions object.
     * 
     * @return BaseCommandOptions object containing defined ICommandLineOption objects.
     * 
     */
    public BaseCommandOptions createOptions();

    /**
     * Configures the application from the parsed command line object by
     * extracting command line values and setting member variables and
     * configuration in the application object.
     * 
     * @param commandLine
     *            ICommandLine parsed command line
     * @throws ParseException
     *             if there is an error configuring the application
     *             
     */
    public void configure(final ICommandLine commandLine) throws ParseException;

    /**
     * Displays the application help text to the console. Should NOT exit the
     * VM.
     */
    public void showHelp();

    /**
     * Displays the application version to the console. Should NOT exit the VM.
     */
    public void showVersion();

    /**
     * @return the exit code for this ICommandLineApp
     */
    default public int getErrorCode() {
        return 0;
    }

    /**
     * @param errorCode
     *            the error code to set for this application
     */
    default public void setErrorCode(final int errorCode) {
        throw new UnsupportedOperationException();
    }
}
