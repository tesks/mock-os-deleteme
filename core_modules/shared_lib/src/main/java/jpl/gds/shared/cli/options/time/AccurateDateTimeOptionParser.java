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
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;

/**
 * A command line option parsing class for CommandLineOptions whose value is a an Accurate Date/Time formatted
 * according the AMPCS mission standards.
 * 
 *
 */
public class AccurateDateTimeOptionParser extends AbstractOptionParser<IAccurateDateTime> {

    /**
     * Constructor for a date option parser. Always validating.
     */
    public AccurateDateTimeOptionParser() {
        super();
        setValidate(true);
    }
    
   

    @Override
    public IAccurateDateTime parse(final ICommandLine commandLine, final ICommandLineOption<IAccurateDateTime> opt)
            throws ParseException {
        final String value = getValue(commandLine, opt);
        if (value == null) {
            return null;
        }
        
        try {
            return new AccurateDateTime(value.trim());
        } catch (final java.text.ParseException e) {
            throw new ParseException(THE_VALUE + opt.getLongOpt()
                + " (" + value + ") is not a properly formatted date value: " + e.toString());
        } 
    }
}
