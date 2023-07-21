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

import java.util.Collection;
import java.util.List;

import jpl.gds.shared.cli.options.CommandLineOption;

/**
 * A command option class for product application ID values.
 * 
 *
 * @since R8
 */
@SuppressWarnings("serial")
public class CsvIntOption extends CommandLineOption<Collection<Integer>> {
	
	/**
	 * @param shortOpt
     * 		the short option name (letter); may be null
     * @param longOpt
     * 		the long option name; may be null only if shortOpt is not
     * @param argName
     *		the name of the argument for help text
     * @param description
     *		the description of the option for help text
	 * @param sort
	 * 		true to sort the resulting collection based on the natural ordering.
	 * @param removeDuplicates
	 * 		true to remove any duplicates from the resulting collection.
     * @param required
     *            true if the option is always required on the command line
	 */
	public CsvIntOption(final String shortOpt, final String longOpt, final String argName,
			final String description, final boolean sort, final boolean removeDuplicates, final boolean required) {
		super(shortOpt, longOpt, true, argName, description, required, new CsvIntOptionParser(sort, removeDuplicates));
		
	}
	
	
	/**
	 * @param shortOpt
     * 		the short option name (letter); may be null
     * @param longOpt
     * 		the long option name; may be null only if shortOpt is not
     * @param argName
     *		the name of the argument for help text
     * @param description
     *		the description of the option for help text
	 * @param sort
	 * 		true to sort the resulting collection based on the natural ordering.
	 * @param removeDuplicates
	 * 		true to remove any duplicates from the resulting collection.
     * @param required
     *            true if the option is always required on the command line
     * @param valid list of valid values 
	 */
	public CsvIntOption(final String shortOpt, final String longOpt, final String argName,final String description,
			final boolean sort, final boolean removeDuplicates, final boolean required, final List<Integer> valid) {
		super(shortOpt, longOpt, true, argName, description, required, new CsvIntOptionParser(sort, removeDuplicates, valid));
		
	}
	
	
}
