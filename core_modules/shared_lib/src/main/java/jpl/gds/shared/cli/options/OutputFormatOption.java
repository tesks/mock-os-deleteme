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

import java.util.List;

/**
 * A command line option for entering the velocity formatting style to be used
 * for application output.
 * 
 *
 * @since R8
 *
 */
@SuppressWarnings("serial")
public class OutputFormatOption extends StringOption {
	/**
	 * The long option name.
	 */
	public static final String LONG_OPTION = "outputFormat";
	/**
	 * The short option nme.
	 */
	public static final String SHORT_OPTION = "o";

	/**
	 * Constructor.
	 * 
	 * @param required
	 *            true if the option is required, false if not
	 */
	public OutputFormatOption(boolean required) {
		this(required, null);
	}

	/**
	 * Constructor.
	 * 
	 * @param required
	 *            true of the option is required, false if not
	 * @param restrictionValues
	 *            list of values to restrict the argument to
	 */
	public OutputFormatOption(boolean required, List<String> restrictionValues) {
		super(SHORT_OPTION, LONG_OPTION, "format",
				"the formatting style for output with a Velocity template.",
				required, restrictionValues);
	}

}
