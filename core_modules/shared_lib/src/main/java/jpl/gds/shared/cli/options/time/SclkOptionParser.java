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
package jpl.gds.shared.cli.options.time;

import org.apache.commons.cli.ParseException;

import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.AbstractOptionParser;
import jpl.gds.shared.cli.options.ICommandLineOption;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.SclkFmt.SclkFormatter;
import jpl.gds.shared.time.TimeProperties;

/**
 * A command line option parsing class for CommandLineOptions whose value is a SCLK formatted
 * according the AMPCS mission standards.
 * 
 *
 */
public class SclkOptionParser extends AbstractOptionParser<ISclk> {
    private final SclkFormatter sclkFmt;

    /**
     * Constructor for a SCLK option parser. Always validating.
     * @param timeConfig the TimeProperties instance dictating allowed SCLK format
     */
    public SclkOptionParser(final TimeProperties timeConfig) {
        super();
        sclkFmt = timeConfig.getSclkFormatter();
        setValidate(true);
    }
    
   

    @Override
    public ISclk parse(final ICommandLine commandLine, final ICommandLineOption<ISclk> opt)
            throws ParseException {
        final String value = getValue(commandLine, opt);
        if (value == null) {
            return null;
        }
        
        try {
            return sclkFmt.valueOf(value.trim());
        } catch (final IllegalArgumentException e) {
            throw new ParseException(THE_VALUE + opt.getLongOpt()
                + " (" + value + ") is not a properly formatted SCLK value: " + e.toString());
        } 
    }
}
