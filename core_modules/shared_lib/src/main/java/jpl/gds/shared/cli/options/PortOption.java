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

import jpl.gds.shared.cli.options.numeric.UnsignedIntOption;
import jpl.gds.shared.holders.PortHolder;
import jpl.gds.shared.types.UnsignedInteger;

/**
 * A command line option class to be used for options whose argument
 * is a network port number.
 * 
 *
 */
public class PortOption extends UnsignedIntOption {
    
    private static final long serialVersionUID = 1L;

    /**
     * Constructor for a port option with a restricted range.
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
     *            to indicate the default lower bound for ports
     * @param maxValue
     *            the maximum value (inclusive) for the argument; may be null
     *            to indicate the default upper bound for ports
     */
    public PortOption(String shortOpt, String longOpt, String argName,
            String description, boolean required, UnsignedInteger minValue,
            UnsignedInteger maxValue) {
        super(shortOpt, longOpt, argName, description, required, minValue, maxValue);
        if (minValue == null) {
            getParser().setMinValue(UnsignedInteger.valueOf(PortHolder.MIN_VALUE));
        } else if (minValue.intValue() <  PortHolder.MIN_VALUE) {
            throw new IllegalArgumentException("minimum value for a port may not be less than " + PortHolder.MIN_VALUE);
        }
        if (maxValue == null) {
            getParser().setMaxValue(UnsignedInteger.valueOf(PortHolder.MAX_VALUE));
        } else if (maxValue.intValue() > PortHolder.MAX_VALUE) {
            throw new IllegalArgumentException("maximum value for a port may not be more than " + PortHolder.MAX_VALUE);
        }
    }

    /**
     * Constructor for a port option with default range (1 to 65535)
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
    public PortOption(String shortOpt, String longOpt, String argName,
            String description, boolean required) {
        this(shortOpt, longOpt, argName, description, required, null, null);
    }
}
