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
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.TreeSet;

import org.apache.commons.cli.ParseException;

import jpl.gds.shared.cli.CliUtility;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.AbstractOptionParser;
import jpl.gds.shared.cli.options.ICommandLineOption;
import jpl.gds.shared.types.UnsignedInteger;

/**
 * An option parser for a list of unsigned numbers. Splits a string by a comma.  
 * Always removes all empty values but sorting and removing duplicates are option.  
 * If no value was given or the values were all are empty returns an empty collection.
 * 
 *
 */
public class CsvUnsignedIntOptionParser extends AbstractOptionParser<Collection<UnsignedInteger>> {
	private final boolean sort;
	private final boolean removeDuplicates;
    private final List<UnsignedInteger> validValues;
	
	/**
	 * Constructor
	 * @param sort - If true the output values will be sorted.
	 * @param removeDuplicates - If true will remove all duplicate entries to return a set of unique input values.
	 * @param valid list of valid values; may be null
	 */
	public CsvUnsignedIntOptionParser(final boolean sort, final boolean removeDuplicates, final List<UnsignedInteger> valid) {
		super();
		this.sort = sort;
		this.removeDuplicates = removeDuplicates;
		this.validValues = valid;
	}
	
	/**
     * Constructor
     * @param sort - If true the output values will be sorted.
     * @param removeDuplicates - If true will remove all duplicate entries to return a set of unique input values.
     */
	public CsvUnsignedIntOptionParser(final boolean sort, final boolean removeDuplicates) {
        super();
        this.sort = sort;
        this.removeDuplicates = removeDuplicates;
        this.validValues = null;
    }

	@Override
	public Collection<UnsignedInteger> parse(final ICommandLine commandLine, final ICommandLineOption<Collection<UnsignedInteger>> opt)
			throws ParseException {
		final String str = getValue(commandLine,opt);

		if (str == null) {
			return Collections.<UnsignedInteger>emptyList();
		} else {

			try {
				final Collection<UnsignedInteger> csv = CliUtility.expandCsvRangeUnsigned(str);
				
				// Verify the valid values.
		        if (validValues != null && !validValues.isEmpty()) {
		        		for (UnsignedInteger s : csv) {
		        			if (!validValues.contains(s)) {
		        				throw new ParseException(THE_VALUE + opt.getLongOpt() + " includes " + s + 
		        						" which is not a valid value in the current configuration");
		        			}
		        		}
		        }


				if (csv.isEmpty() || (!sort && !removeDuplicates)) {
					return csv;
				} else if (sort && removeDuplicates) {
					// Easy, create a sorted set and return.
					return new TreeSet<>(csv);
				} else if (removeDuplicates) {
					/**
					 * Use a LinkedHashSet because it will remove duplicates but will preserve order.
					 */
					return new LinkedHashSet<>(csv);
				} else  {
					// Only sort, leave in any duplicates.
					Collections.sort((List<UnsignedInteger>) csv);
					return csv;
				}
			} catch (ParseException e) {
				throw new ParseException(THE_VALUE + opt.getLongOpt() + " must include only unsigned integers and range strings.");
			}
		}
	}
}
