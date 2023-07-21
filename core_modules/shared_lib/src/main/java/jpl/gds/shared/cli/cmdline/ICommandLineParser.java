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
package jpl.gds.shared.cli.cmdline;

import org.apache.commons.cli.ParseException;

/**
 * A generic interface to be implemented by command line parsers. Dependent upon
 * Apache commons-cli libraries.
 * 
 *
 */
public interface ICommandLineParser {

    /**
     * Parses command line arguments given a set of command line options and a
     * list of string arguments.
     * 
     * @param options
     *            OptionSet object containing defined ICommandLineOption
     *            objects.
     * @param arguments
     *            the list of argument strings from the command line
     * @return Apache ICommandLine object
     * @throws ParseException
     *             if the parsing encounters command line errors
     */
    public ICommandLine parse(OptionSet options, String[] arguments)
            throws ParseException;
}
