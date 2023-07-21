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

/**
 * A command line options object for "show columns", e.g, an option to indicate
 * that column headers should be output for CSV or tabular output.
 * 
 *
 * @since R8
 */
@SuppressWarnings("serial")
public class ShowColumnsOption extends FlagOption {

	/**
	 * The long option name.
	 */
	public static final String LONG_OPTION = "showColumns";
	/**
	 * The short option name,
	 */
	public static final String SHORT_OPTION = "m";

	/**
	 * Constructor.
	 * 
	 * @param required
	 *            true if the option is required, false if not
	 */
	public ShowColumnsOption(boolean required) {
		super(SHORT_OPTION, LONG_OPTION, "include column names in CSV output",
				required);
	}

}
