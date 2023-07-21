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

import java.util.ArrayList;
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

/**
 * A command option parsing class for  ID values.
 * 
 *
 * @since R8
 */
@SuppressWarnings("serial")
public class CsvIntOptionParser extends AbstractOptionParser<Collection<Integer>>{

	private final boolean sort;
	private final boolean removeDuplicates;
    private final List<Integer> validValues;
    
    /**
	 * Constructor.
	 * 
     * @param sort - If true the output values will be sorted.
     * @param removeDuplicates - If true will remove all duplicate entries to return a set of unique input values.
	 * @param valid list of valid values. May be null.
     */
	public CsvIntOptionParser(final boolean sort, final boolean removeDuplicates, final List<Integer> valid) {
		super();
		this.sort = sort;
		this.removeDuplicates = removeDuplicates;
		this.validValues = valid;
	}
	
	/**
	 * Constructor.
	 * 
     * @param sort - If true the output values will be sorted.
     * @param removeDuplicates - If true will remove all duplicate entries to return a set of unique input values.
     */
	public CsvIntOptionParser(final boolean sort, final boolean removeDuplicates) {
		this(sort, removeDuplicates, null);
	}
	
	@Override
	public Collection<Integer> parse(final ICommandLine commandLine, final ICommandLineOption<Collection<Integer>> opt)
			throws ParseException {
		
		final String testKeyStr = getValue(commandLine,opt);
		
		 if (testKeyStr == null) {
	            return Collections.<Integer>emptyList();
	        } else {
	        	
	        	final Collection<Integer> csv = new ArrayList<Integer>();

	            for (final String testKey : testKeyStr.split(",")) {
	                for (final String next : CliUtility.expandRange(testKey.trim())) {
	                	Integer tempInt;
	                    try {
	                    	tempInt = Integer.parseInt(next.trim());
	                    }
	                    catch (final NumberFormatException e1) {
	                        throw new ParseException("Value of --"+ opt.getLongOrShort() + " option must be a list of comma-separated integer values, but the value '" + next + "' is invalid.");
	                    }
	                    if(validValues != null && !validValues.contains(tempInt)){
	                		throw new ParseException("The value " + next + " is not valid for command line option --" + opt.getLongOrShort());
	                	}
                     csv.add(tempInt);
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
	                Collections.sort((List<Integer>) csv);
	                return csv;
	            }
	        }
	}
}
