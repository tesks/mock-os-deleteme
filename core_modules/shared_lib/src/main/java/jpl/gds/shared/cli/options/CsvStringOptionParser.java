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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.TreeSet;

import org.apache.commons.cli.ParseException;

import jpl.gds.shared.cli.cmdline.ICommandLine;

/**
 * Splits a string by a comma.  Always removes all empty values but sorting and removing duplicates are option.  
 * If no value was given or the values were all are empty returns an empty collection.
 * 
 *
 */
public class CsvStringOptionParser extends AbstractOptionParser<Collection<String>> {
	private final boolean sort;
	private final boolean removeDuplicates;
    private final List<String> validValues;
	
	/**
	 * Constructor.
	 * 
	 * @param sort - If true the output values will be sorted.
	 * @param removeDuplicates - If true will remove all duplicate entries to return a set of unique input values.
	 */
	public CsvStringOptionParser(final boolean sort, final boolean removeDuplicates) {
		super();
		this.sort = sort;
		this.removeDuplicates = removeDuplicates;
		this.validValues = null;
	}
	
	/**
	 * Constructor.
	 * 
     * @param sort - If true the output values will be sorted.
     * @param removeDuplicates - If true will remove all duplicate entries to return a set of unique input values.
	 * @param valid list of valid values. May be null.
     */
    public CsvStringOptionParser(final boolean sort, final boolean removeDuplicates, final List<String> valid) {
        super();
        this.sort = sort;
        this.removeDuplicates = removeDuplicates;
        this.validValues = valid;
    }

	@Override
	public Collection<String> parse(final ICommandLine commandLine, final ICommandLineOption<Collection<String>> opt)
			throws ParseException {
        final String str = getValue(commandLine,opt);

        if (str == null) {
            return Collections.<String>emptyList();
        } else {

            final Collection<String> csv = new ArrayList<>();

            for (final String s : str.trim().split(",")) {
                if (!s.trim().isEmpty()) {
                    if (this.validValues != null && !this.validValues.contains(s.trim())) {
                        throw new ParseException("The value " + s + " is not valid for command line option --" + opt.getLongOrShort());
                    }
                    csv.add(s.trim());
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
                Collections.sort((List<String>) csv);
                return csv;
            }
        }
	}
}
