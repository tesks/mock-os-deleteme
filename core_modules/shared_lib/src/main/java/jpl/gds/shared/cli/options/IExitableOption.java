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
package jpl.gds.shared.cli.options;

import org.apache.commons.cli.ParseException;

import jpl.gds.shared.cli.cmdline.ICommandLine;

/**
 * An interface to be implemented by ICommandLineOption classes that can
 * optionally cause the application to exit.
 * 
 *
 * @param <T>
 *            the data type of the ICommandLineOption
 */
public interface IExitableOption<T extends Object> {

    /**
     * Parsing method for use when the option may or may not exit the
     * application. WARNING: may call System.exit().
     * 
     * @param commandLine
     *            the ICommandLine object containing parsed command line options
     * @param required
     *            true if the option is required, false if not
     * @param exit
     *            true if the application should exit owing to the presence of
     *            this option
     * @return the parsed object value, specific to the data type of this option
     * @throws ParseException
     *             if the option is required but not present, or if the option
     *             is not properly specified on the command line
     */
    public T parseWithExit(ICommandLine commandLine, boolean required, boolean exit)
            throws ParseException;

}
