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
package jpl.gds.db.api.sql.fetch;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.ParseException;

import jpl.gds.shared.string.StringUtil;
import jpl.gds.shared.types.Pair;


/**
 * Keeps track of ranges and implements useful methods.
 *
 */
abstract public class LongRanges extends Object
{
    private static final String ELLIPSIS = "..";


    private final List<LongRange> _numbers = new ArrayList<LongRange>();


    /**
     * Constructor LongRanges.
     */
    protected LongRanges()
    {
        super();
    }

    /**
     * Indicates whether this set of ranges boils down to a single number.
     * @return true if there is one range with start and end number the same, false otherwise
     */
    public boolean isSingleNumber() {
    	if (_numbers.size() != 1) {
    		return false;
    	}
    	final LongRange lr = _numbers.get(0);
    	return lr.getOne().equals(lr.getTwo());
    }

    /**
     * Add a single number to the list.
     *
     * @param number Number
     */
    public void addLong(final long number)
    {
        addLongRange(number, number);
    }


    /**
     * Add a number range to the list.
     *
     * @param start Start
     * @param stop  Stop
     */
    public void addLongRange(final long start,
                              final long stop)
    {
        if ((start >= 0L) && (stop >= 0L) && (start <= stop))
        {
            _numbers.add(new LongRange(start, stop));
        }
    }


    /**
     * Process option arguments.
     *
     * @param numbersStr Option value
     * @param option     Option itself
     * @param name       Name for logging
     * @param limit      Limit of individual values
     *
     * @throws MissingOptionException Missing option exception
     * @throws ParseException         Parse exception
     */
    protected void processOptions(final String numbersStr,
                                  final String option,
                                  final String name,
                                  final long   limit)
        throws MissingOptionException, ParseException
    {
        final String s = StringUtil.safeTrim(numbersStr);

        if (s.length() == 0)
        {
            throw new MissingOptionException("The argument -" +
                                             option           +
                                             " requires a value");
        }

        for (final String next : s.split(",", -1))
        {
            final String value = next.trim();
            final int    dash  = value.indexOf(ELLIPSIS);

            if (dash < 0)
            {
                long number = 0L;

                try
                {
                    number = Long.parseLong(value);
                }
                catch (final NumberFormatException nfe)
                {
                    throw new ParseException(name               +
                                             " is not valid: '" +
                                             value              +
                                             "'");
                }

                if (number < 0L)
                {
                    throw new ParseException(name                    +
                                             " cannot be negative: " +
                                             number);
                }

                if (number > limit)
                {
                    throw new ParseException(name              +
                                             " is too large: " +
                                             number);
                }

                addLong(number);
            }
            else
            {
                long start = 0L;
                long stop  = 0L;

                final String left = value.substring(0, dash).trim();

                try
                {
                    start = Long.parseLong(left);
                }
                catch (final NumberFormatException nfe)
                {
                    throw new ParseException(name               +
                                             " is not valid: '" +
                                             left               +
                                             "'");
                }

                final String right =
                    value.substring(dash + ELLIPSIS.length()).trim();

                try
                {
                    stop = Long.parseLong(right);
                }
                catch (final NumberFormatException nfe)
                {
                    throw new ParseException(name               +
                                             " is not valid: '" +
                                             right              +
                                             "'");
                }

                if (start < 0L)
                {
                    throw new ParseException(name                    +
                                             " cannot be negative: " +
                                             start);
                }

                if (start > limit)
                {
                    throw new ParseException(name              +
                                             " is too large: " +
                                             start);
                }

                if (stop < 0L)
                {
                    throw new ParseException(name                    +
                                             " cannot be negative: " +
                                             stop);
                }

                if (stop > limit)
                {
                    throw new ParseException(name              +
                                             " is too large: " +
                                             stop);
                }

                if (start > stop)
                {
                    throw new ParseException(name                +
                                             " range is empty: " +
                                             start               +
                                             "-"                 +
                                             stop);
                }

                addLongRange(start, stop);
            }
        }
    }


    /**
     * Return true if empty.
     *
     * @return True if nothing in list
     */
    public boolean isEmpty()
    {
        return _numbers.isEmpty();
    }


    /**
     * Append where clause for single number.
     *
     * @param sb
     * @param tableAbbrev
     * @param column
     * @param number
     */
    private static void addWhere(final StringBuilder sb,
                                 final String        tableAbbrev,
                                 final String        column,
                                 final long          number)
    {
        sb.append("(");
        sb.append(tableAbbrev).append(".").append(column).append("=");
        sb.append(number);
        sb.append(")");
    }


    /**
     * Append where clause for number range.
     *
     * @param sb
     * @param tableAbbrev
     * @param column
     * @param start
     * @param stop
     */
    private static void addWhere(final StringBuilder sb,
                                 final String        tableAbbrev,
                                 final String        column,
                                 final long          start,
                                 final long          stop)
    {
        sb.append("((");

        sb.append(tableAbbrev).append(".").append(column).append(">=");
        sb.append(start);

        sb.append(") AND (");

        sb.append(tableAbbrev).append(".").append(column).append("<=");
        sb.append(stop);

        sb.append("))");
    }


    /**
     * Construct where clause for all implemented ranges.
     *
     * @param tableAbbrev Table abbreviation
     * @param column      Column name
     *
     * @return String
     */
    protected String whereClause(final String tableAbbrev,
                                 final String column)
    {
        if (isEmpty())
        {
            return "";
        }

        final StringBuilder sb    = new StringBuilder("(");
        boolean             first = true;

        for (final LongRange vr : _numbers)
        {
            if (first)
            {
                first = false;
            }
            else
            {
                sb.append(" OR ");
            }

            if (vr.isSingle())
            {
                addWhere(sb, tableAbbrev, column, vr.getOne());
            }
            else
            {
                addWhere(sb, tableAbbrev, column, vr.getOne(), vr.getTwo());
            }
        }

        sb.append(")");

        return sb.toString();
    }


    /**
     * Holds a start-stop number range.
     */
    private static class LongRange extends Pair<Long, Long>
    {
		private static final long serialVersionUID = 1L;

		/**
         * Constructor.
         *
         * @param start Start
         * @param stop  Stop
         */
        public LongRange(final long start,
                         final long stop)
        {
            super(start, stop);
        }


        /**
         * Return true if the start and the stop are the same.
         *
         * @return True if the start and the stop are the same
         */
        public boolean isSingle()
        {
            return getOne().equals(getTwo());
        }
    }
}
