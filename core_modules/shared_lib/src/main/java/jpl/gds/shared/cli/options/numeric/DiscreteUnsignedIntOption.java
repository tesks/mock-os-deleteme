/*
 * Copyright 2006-2019. California Institute of Technology.
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

import java.util.List;

import jpl.gds.shared.cli.options.CommandLineOption;
import jpl.gds.shared.types.UnsignedInteger;

/**
 * A class that represents a command line option that takes an unsigned integer value.
 * Optionally, the option value may also have a restricted set of values that 
 * do not have to consist of consecutive values.
 * 
 */
public class DiscreteUnsignedIntOption extends CommandLineOption<UnsignedInteger> {

	private static final long serialVersionUID = 1L;

	/**
	 * Constructor for the option that accepts all valid integers
	 * @param opt
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
	public DiscreteUnsignedIntOption(final String opt, final String longOpt, final String argName, final String description, final boolean required) {
		super(opt, longOpt, true, argName, description, required, new DiscreteUnsignedIntOptionParser(null));
	}
	
	/**
	 * Constructor for the option that has a restricted set of valid unsigned integer values
	 * @param opt
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
     * @param valid
     *            the List of UnsignedInteger values that this option will accept as valid input
	 */
	public DiscreteUnsignedIntOption(final String opt, final String longOpt, final String argName, final String description, final boolean required, final List<UnsignedInteger> valid) {
		super(opt, longOpt, true, argName, description, required, new DiscreteUnsignedIntOptionParser(valid));
	}
}
