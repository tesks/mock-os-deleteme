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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;


/**
 * An ICommandLineParser implementation that just extends the Apache DefaultParser.
 * 
 *
 */
public class ApacheCommandLineParser extends DefaultParser implements ICommandLineParser {

    /**
     * @{inheritDoc}
     * @see jpl.gds.shared.cli.cmdline.ICommandLineParser#parse(jpl.gds.shared.cli.cmdline.OptionSet, java.lang.String[])
     */
    @Override
    public ICommandLine parse(OptionSet options, String[] arguments)
            throws ParseException {
        return new ApacheCommandLine(super.parse(options.getInnerOptions(), arguments));
    }
    
    /**
     * @{inheritDoc}
     * @see org.apache.commons.cli.DefaultParser#parse(org.apache.commons.cli.Options, java.lang.String[])
     * 
     * @throws UnsupportedOperationException always
     */
    @Override
    public CommandLine parse(Options options, String[] arguments)
            throws ParseException {
        throw new UnsupportedOperationException("This operation is not supported");
    }
    
    /**
     * @{inheritDoc}
     * @see org.apache.commons.cli.DefaultParser#parse(org.apache.commons.cli.Options, java.lang.String[], boolean)
     * 
     * @throws UnsupportedOperationException always
     */
    @Override
    public CommandLine parse(Options options, String[] arguments, boolean stopAtNonOption)
            throws ParseException {
        throw new UnsupportedOperationException("This operation is not supported");
    }

}
