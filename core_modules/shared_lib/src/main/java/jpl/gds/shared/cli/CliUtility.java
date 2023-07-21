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
package jpl.gds.shared.cli;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.StringUtils;

import jpl.gds.shared.types.UnsignedInteger;


/**
 * Library of command-line utilities.
 *
 */
public class CliUtility extends Object
{
    /**
     * Constructor.
     *
     * Never instantiated.
     */
    private CliUtility()
    {
        super();
    }

    /**
     * Expands a csv or possible range strings.  
     * 
     * example: 1,2,5..10,20,10
     * @param rangeStr
     * @return list of expanded long values
     * @throws ParseException
     */
    public static List<Long> expandCsvRangeLong(final String rangeStr) throws ParseException {
    		try {
    			return expandCsvRangeUnsigned(rangeStr).stream()
    					.map(us -> us.longValue())
    					.collect(Collectors.toList());
    		} catch (NumberFormatException e) {
    			throw new ParseException(e.getMessage());
    		}
    }

    /**
     * Expands a csv or possible range strings.  
     * 
     * example: 1,2,5..10,20,10
     * @param rangeStr
     * @return list of expanded unsigned ints
     * @throws ParseException
     */
    public static List<UnsignedInteger> expandCsvRangeUnsigned(final String rangeStr) throws ParseException {
    		try {
    			return expandCsvRange(rangeStr).stream()
    					.map(UnsignedInteger::valueOf)
    					.collect(Collectors.toList());
    		} catch (NumberFormatException e) {
    			throw new ParseException(e.getMessage());
    		}
    }

    /**
     * Expands a csv or possible range strings.  
     * 
     * example: 1,2,5..10,20,10
     * @param rangeStr
     * @return list of expanded strings
     * @throws ParseException
     */
    public static List<String> expandCsvRange(final String rangeStr) throws ParseException {
    		String[] parts = rangeStr == null ? new String[0] : StringUtils.split(rangeStr, ',');
    		
    		List<String> expanded = new ArrayList<>();
    		
    		for (String part : parts) {
    			expanded.addAll(expandRange(part));
    		}
    		
    		return expanded;
    }
    		

    /**
     * Check for a range of integers and return as a list of strings.
     *
     * This returns strings so that it can be used for any integral numeric
     * type. The user checks for the desired data type. If not a range, the
     * original string is returned without parsing.
     *
     * @param s Possible range string
     *
     * @return List Result as a list
     *
     * @throws ParseException If a range but malformed
     */
    public static List<String> expandRange(final String s)
        throws ParseException
    {
        final List<String> list  = new ArrayList<String>();
        final String[]     range = s.trim().split("\\.\\.", -1);

        if (range.length != 2)
        {
            // Not a range

            list.add(s.trim());

            return list;
        }

        BigInteger start = null;
        BigInteger stop  = null;

        try
        {
            start = new BigInteger(range[0].trim());
            stop  = new BigInteger(range[1].trim());
        }
        catch (NumberFormatException nfe)
        {
            throw new ParseException("Range limits must be integers");
        }

        if ((start.signum() < 0) || (stop.signum() < 0))
        {
            throw new ParseException("Range limits must be positive integers");
        }

        if (start.compareTo(stop) > 0)
        {
            throw new ParseException("Ranges must not be empty");
        }

        while (start.compareTo(stop) <= 0)
        {
            list.add(start.toString().trim());

            start = start.add(BigInteger.ONE);
        }

        return list;
    }
}
