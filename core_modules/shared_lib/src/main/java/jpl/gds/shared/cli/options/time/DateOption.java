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

import jpl.gds.shared.cli.options.CommandLineOption;
import jpl.gds.shared.time.IAccurateDateTime;

/**
 * A command line option class for an option whose value is a Date formatted to
 * AMPCS standards.
 * 
 *
 */
public class DateOption extends CommandLineOption<IAccurateDateTime> {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor for a validating date option. Date must be a date/time
     * formatted according to AMPCS mission standards.
     * 
     * @param shortOpt
     *            short option name; may be null
     * @param longOpt
     *            long option name; may only be null if shortOpt is not
     * @param argName
     *            the name of the argument for help text; may not be null
     * @param description
     *            description of the argument for help text
     * @param required
     *            true if the argument must always be present on the command
     *            line
     */
    public DateOption(final String shortOpt, final String longOpt,
            final String argName,
            final String description, final boolean required) {
        super(shortOpt, longOpt, true, argName, description, required, new DateOptionParser());
    }
    

    
    /**
     * Note this method overrides the interface return type to
     * DateOptionParser.
     * 
     * @{inheritDoc
     * @see jpl.gds.shared.cli.options.ICommandLineOption#getParser()
     */
    @Override
    public DateOptionParser getParser() {
        return (DateOptionParser) parser;
    }

}
