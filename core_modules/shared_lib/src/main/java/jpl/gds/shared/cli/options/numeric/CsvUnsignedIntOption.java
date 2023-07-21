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

import java.util.Collection;
import java.util.List;

import jpl.gds.shared.cli.options.CommandLineOption;
import jpl.gds.shared.types.UnsignedInteger;

/**
 * Creates an option for parsing csv input strings.  
 * 
 *
 */
public class CsvUnsignedIntOption extends CommandLineOption<Collection<UnsignedInteger>> {

	private static final long serialVersionUID = -1737757802006553242L;

	/**
	 * @param opt
     *            the short option name (letter); may be null
     * @param longOpt
     *            the long option name; may be null only if opt is not
     * @param argName
     *            the name of the option's argument for help text
     * @param description
     *            the description of the option for help text
	 * @param sort
	 * 		true to sort the resulting collection based on the natural ordering.
	 * @param removeDuplicates
	 * 		true to remove any duplicates from the resulting collection.
     * @param required
     *            true if the option is always required on the command line         
	 */
	public CsvUnsignedIntOption(final String opt, final String longOpt, final String argName, final String description,
			final boolean sort, final boolean removeDuplicates, final boolean required) {
		super(opt, longOpt, true, argName, description, required, new CsvUnsignedIntOptionParser(sort, removeDuplicates, null));
	}
	
	/**
	 * @param opt
     *            the short option name (letter); may be null
     * @param longOpt
     *            the long option name; may be null only if opt is not
     * @param argName
     *            the name of the option's argument for help text
     * @param description
     *            the description of the option for help text
	 * @param sort
	 * 		true to sort the resulting collection based on the natural ordering.
	 * @param removeDuplicates
	 * 		true to remove any duplicates from the resulting collection.
     * @param required
     *            true if the option is always required on the command line
     * @param valid 
     *            The List of valid UnsignedInteger values      
	 */
	public CsvUnsignedIntOption(final String opt, final String longOpt, final String argName, final String description,
			final boolean sort, final boolean removeDuplicates, final boolean required, final List<UnsignedInteger> valid) {
		super(opt, longOpt, true, argName, description, required, new CsvUnsignedIntOptionParser(sort, removeDuplicates, valid));
	}
}
