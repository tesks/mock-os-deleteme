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
package jpl.gds.shared.cli.options.numeric;

import jpl.gds.shared.cli.options.CommandLineOption;
import jpl.gds.shared.types.UnsignedLong;

/**
 * A class that represents a command line option that takes an UnsignedLong value.
 * Optionally, the option value may also have a restricted range.
 * 
 *
 */
public class UnsignedLongOption extends CommandLineOption<UnsignedLong> {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor for an unsigned long option with a restricted range.
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
     * @param minValue
     *            the minimum value (inclusive) for the argument; may be null
     *            for no lower bound
     * @param maxValue
     *            the maximum value (inclusive) for the argument; may be null
     *            for no upper bound
     */
    public UnsignedLongOption(final String shortOpt, final String longOpt,
            final String argName,
            final String description, final boolean required,
            UnsignedLong minValue, UnsignedLong maxValue) {
        super(shortOpt, longOpt, true, argName, description, required,
                new UnsignedLongOptionParser(minValue, maxValue));
      
    }
    
    /**
     * Constructor for an unsigned long option with default range.
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
    public UnsignedLongOption(final String shortOpt, final String longOpt,
            final String argName,
            final String description, final boolean required) {
        super(shortOpt, longOpt, true, argName, description, required,
                new UnsignedLongOptionParser());
    }
     
    /**
     * Note this method overrides the interface return type to
     * UnsignedLongOptionParser.
     * 
     * @{inheritDoc}
     * @see jpl.gds.shared.cli.options.ICommandLineOption#getParser()
     */
    @Override
    public UnsignedLongOptionParser getParser() {
        return (UnsignedLongOptionParser) parser;
    }


}
