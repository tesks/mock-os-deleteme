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
package jpl.gds.common.options;

import jpl.gds.shared.cli.options.StringOption;

import java.util.List;

/**
 * A command line option class for testbed name.
 * 
 *
 * @since R8
 */
@SuppressWarnings("serial")
public class TestbedNameOption extends StringOption {

	/**
	 * Long option name.
	 */
	public static final String LONG_OPTION = "testbedName";
	/**
	 * Short option name.
	 */
	public static final String SHORT_OPTION = "G";

	/**
	 * Description
	 */
	public static final String DESCRIPTION = "the name of the testbed; only applicable if venue type supports testbed names";
	/**
	 * Constructor.
	 * 
	 * @param restrictTo
	 *            list of testbed names to restrict the argument value to
	 * @param required
	 *            true if the option is required, false if not
	 */
	public TestbedNameOption(List<String> restrictTo, boolean required) {
		super(
				SHORT_OPTION,
				LONG_OPTION,
				"testbed",
				DESCRIPTION,
				required, restrictTo);
	}

	/**
	 * Constructor.
	 * 
	 * @param required
	 *            true if the option is required, false if not
	 */
	public TestbedNameOption(boolean required) {
		this(null, required);
	}

}
