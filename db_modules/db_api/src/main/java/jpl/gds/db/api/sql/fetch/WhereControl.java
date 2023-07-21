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

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import jpl.gds.db.api.DatabaseException;
import jpl.gds.shared.string.StringUtil;
import jpl.gds.shared.time.DbTimeUtility;
import jpl.gds.shared.types.Pair;
import jpl.gds.shared.types.Quadruplet;
import jpl.gds.shared.types.Quintuplet;
import jpl.gds.shared.types.Triplet;


/**
 * Keeps track of query parameters in order to build where clauses for fetch.
 */
abstract public class WhereControl extends Object
{
    private final List<IWhereType> _clauses = new ArrayList<IWhereType>();

    private String _canned = "";


    /**
     * Constructor WhereControl.
     *
     * @param canned Clauses not under our control
     */
    protected WhereControl(final String canned)
    {
        super();

        _canned = StringUtil.safeTrim(canned);
    }


    /**
     * Allow these to be set after construction.
     *
     * @param canned Clauses not under our control
     */
    public void setCanned(final String canned)
    {
        _canned = StringUtil.safeTrim(canned);
    }


    /**
     * Add a query for a column to join against another table.
     *
     * @param column
     *            Column name
     * @param joinColumn
     *            Join column name
     *
     * @throws WhereControlException
     *             if an error occurs WhereControl exception
     */
    protected void addJoinQuery(final String column,
                                final String joinColumn)
        throws WhereControlException
    {
        _clauses.add(new JoinWhereType(column, joinColumn));
    }


    /**
     * Add a query for a column with a direct value (no parameter). The value
     * may or may not need to be quoted.
     *
     * @param column
     *            Column name
     * @param value
     *            Object value
     * @param quote
     *            True means quote
     * @param operator
     *            Operator
     *
     * @throws WhereControlException
     *             if an error occurs WhereControl exception
     */
    protected void addSimpleQuery(final String  column,
                                  final Object  value,
                                  final boolean quote,
                                  final String  operator)
        throws WhereControlException
    {
        _clauses.add(new SimpleWhereType(column, value, quote, operator));
    }


    /**
     * Add a query for an ERT column with a direct value (no parameter).
     *
     * @param coarse
     *            Coarse column
     * @param fine
     *            Fine column
     * @param value
     *            Time in milliseconds
     * @param nanoValue
     *            Time accuracy for nanoseconds
     * @param upperBound
     *            True if an upper-bound, else lower-bound
     *
     * @throws WhereControlException
     *             if an error occurs WhereControl exception
     * 
     * @version MPCS-8104 - Updated to reflect change to quintuplet
     */
    protected void addErtQuery(final String  coarse,
                               final String  fine,
                               final long    value,
                               final long    nanoValue,
                               final boolean upperBound)
        throws WhereControlException
    {
        _clauses.add(new ErtWhereType(coarse, fine, value, nanoValue, upperBound));
    }


    /**
     * Add a query for a RCT column with a direct value (no parameter).
     *
     * @param coarse
     *            Coarse column
     * @param fine
     *            Fine column
     * @param value
     *            Time in milliseconds
     * @param upperBound
     *            True if an upper-bound, else lower-bound
     *
     * @throws WhereControlException
     *             if an error occurs WhereControl exception
     *
     * @version MPCS-6808 New method
     */
    protected void addRctQuery(final String  coarse,
                               final String  fine,
                               final long    value,
                               final boolean upperBound)
        throws WhereControlException
    {
        _clauses.add(new RctWhereType(coarse, fine, value, upperBound));
    }


    /**
     * Add a query for a column that is integer, but we want a boolean state,
     * where a zero value is true, and hence, non-zero is false. It is optional
     * whether we add a check for NULL (meaning same as zero, that is, true.)
     *
     * @param column
     *            Column name
     * @param value
     *            Boolean value
     * @param checkNull
     *            True means check for null
     *
     * @throws WhereControlException
     *             if an error occurs WhereControl exception
     */
    protected void addTrueIsZeroQuery(final String  column,
                                      final Boolean value,
                                      final boolean checkNull)
        throws WhereControlException
    {
        _clauses.add(new TrueIsZeroWhereType(column, value, checkNull));
    }


    /**
     * Add a query for a column that may be null. We want a boolean state where
     * non-null is considered false and null true.
     *
     * @param column
     *            Column name
     * @param value
     *            Boolean
     *
     * @throws WhereControlException
     *             if an error occurs WhereControl exception
     */
    protected void addTrueIsNullQuery(final String  column,
                                      final Boolean value)
        throws WhereControlException
    {
        _clauses.add(new TrueIsNullWhereType(column, value));
    }


    /**
     * Add a query for a VCFC range.
     *
     * @param vcfcs
     *            VCFCS
     *
     * @throws WhereControlException
     *             if an error occurs WhereControl exception
     */
    protected void addVcfcRangesQuery(final VcfcRanges vcfcs)
        throws WhereControlException
    {
        _clauses.add(new VcfcRangesWhereType(vcfcs));
    }


    /**
     * Add a query for DSS ids
     *
     * @param column
     *            Column name
     * @param dss
     *            DSS id
     *
     * @throws WhereControlException
     *             if an error occurs WhereControl exception
     */
    protected void addDssQuery(final String       column,
                               final Set<Integer> dss)
        throws WhereControlException
    {
        _clauses.add(new DssWhereType(column, dss));
    }


    /**
     * Add a query for VCIDs
     *
     * @param column
     *            Column name
     * @param vcid
     *            VCID
     *
     * @throws WhereControlException
     *             if an error occurs WhereControl exception
     */
    protected void addVcidQuery(final String       column,
                                final Set<Integer> vcid)
        throws WhereControlException
    {
        _clauses.add(new VcidWhereType(column, vcid));
    }


    /**
     * Create entire where clause for all elements.
     *
     * @param abbrev
     *            Table abbreviation
     * @param joinAbbrev
     *            Join table abbreviation
     *
     * @return Where clause as string
     *
     * @throws WhereControlException
     *             if an error occurs WhereControl exception
     */
    public String generateWhereClause(final String abbrev,
                                      final String joinAbbrev)
        throws WhereControlException
    {
        if (abbrev == null)
        {
            throw new WhereControlException("Null table abbreviation");
        }

        if (joinAbbrev == null)
        {
            throw new WhereControlException("Null join table abbreviation");
        }

        final boolean haveCanned = (_canned.length() > 0);

        if (! haveCanned && _clauses.isEmpty())
        {
            return "";
        }

        final StringBuilder sb    = new StringBuilder(" WHERE ");
        boolean             first = true;

        if (haveCanned)
        {
            // Place before all of our clauses

            sb.append("(");
            sb.append(_canned);
            sb.append(")");

            first = false;
        }

        for (final IWhereType wt : _clauses)
        {
            if (first)
            {
                first = false;
            }
            else
            {
                sb.append(" AND ");
            }

            wt.addToWhereClause(sb, abbrev, joinAbbrev);
        }

        return sb.toString();
    }


    /**
     * Sets parameters in prepared statement as required
     *
     * @param ps
     *            Prepared statement
     * @param index
     *            Beginning index for parameters
     *
     * @throws WhereControlException
     *             if an error occurs WhereControl exception
     * @throws DatabaseException
     *             SQL exception
     */
    public void setParameters(final PreparedStatement ps,
                              final int               index)
        throws WhereControlException, SQLException
    {
        int useIndex = index;

        for (final IWhereType wt : _clauses)
        {
            if (wt.setParameter(ps, useIndex))
            {
                ++useIndex;
            }
        }
    }


    /**
     * Quotes string and appends it to string builder.
     *
     * @param sb
     * @param s
     *
     * @throws WhereControlException
     *             if an error occurs
     */
    private static void quote(final StringBuilder sb,
                              final String        s)
        throws WhereControlException
    {
        if (sb == null)
        {
            throw new WhereControlException("Null string builder in quote");
        }

        if (s == null)
        {
            throw new WhereControlException("Null string in quote");
        }

        final int length = s.length();

        sb.append("'");

        for (int i = 0; i < length; ++i)
        {
            final char c = s.charAt(i);

            if (c == '\'')
            {
                sb.append("\\'");
            }
            else
            {
                sb.append(c);
            }
        }

        sb.append("'");
    }


    /**
     * Class that represents a query on a column joined with another table and
     * no parameter necessary.
     */
    private static class JoinWhereType
        extends Pair<String, String> implements IWhereType
    {
		private static final long serialVersionUID = 1L;


		/**
         * Constructor.
         *
         * @param column
         *            the column to join
         * @param joinColumn
         *            the column to which the join is made
         *
         * @throws WhereControlException
         *             if an error occurs
         */
        public JoinWhereType(final String column,
                             final String joinColumn)
            throws WhereControlException
        {
            super(column, joinColumn);

            if (column == null)
            {
                throw new WhereControlException("Null column");
            }

            if (joinColumn == null)
            {
                throw new WhereControlException("Null join column");
            }
        }


        /**
         * Generates portion of where clause.
         *
         * @param sb
         *            Where clause in progress
         * @param abbrev
         *            Table abbreviation
         * @param joinAbbrev
         *            Join table abbreviation
         *
         * @throws WhereControlException
         *             if an error occurs
         */
        @Override
		public void addToWhereClause(final StringBuilder sb,
                                     final String        abbrev,
                                     final String        joinAbbrev)
            throws WhereControlException
        {
            if (sb == null)
            {
                throw new WhereControlException("Null string builder");
            }

            if (abbrev == null)
            {
                throw new WhereControlException("Null table abbreviation");
            }

            if (joinAbbrev == null)
            {
                throw new WhereControlException("Null join table abbreviation");
            }

            sb.append("(");

            sb.append(abbrev).append(".").append(getOne()).append("=");
            sb.append(joinAbbrev).append(".").append(getTwo());

            sb.append(")");
        }


        /**
         * Sets parameter in prepared statement as required
         *
         * @param ps
         *            Prepared statement
         * @param index
         *            Beginning index for parameters
         *
         * @return True if parameter was set (and index used)
         *
         * @throws WhereControlException
         *             if an error occurs
         */
        @Override
		public boolean setParameter(final PreparedStatement ps,
                                    final int               index)
                throws WhereControlException
        {
            if (ps == null)
            {
                throw new WhereControlException("Null prepared statement");
            }

            return false;
        }
    }


    /**
     * Class that represents a query on a column with a direct value and no
     * parameter necessary.
     */
    private static class SimpleWhereType
        extends Quadruplet<String, Object, Boolean, String> implements IWhereType
    {
		private static final long serialVersionUID = 1L;


		/**
         * Constructor.
         *
         * @param column
         *            the column to query
         * @param value
         *            the value to match
         * @param quote
         *            true means quote the value, false means do not quote the
         *            value
         * @param operator
         *            the operator (?)
         *
         * @throws WhereControlException
         *             if an error occurs
         */
        public SimpleWhereType(final String  column,
                               final Object  value,
                               final boolean quote,
                               final String  operator)
            throws WhereControlException
        {
            super(column, value, quote, operator);

            if (column == null)
            {
                throw new WhereControlException("Null column");
            }

            if (value == null)
            {
                throw new WhereControlException("Null value");
            }

            if (operator == null)
            {
                throw new WhereControlException("Null operator");
            }
        }


        /**
         * Generates portion of where clause.
         *
         * @param sb
         *            Where clause in progress
         * @param abbrev
         *            Table abbreviation
         * @param joinAbbrev
         *            Join table abbreviation
         *
         * @throws WhereControlException
         *             if an error occurs
         */
        @Override
		public void addToWhereClause(final StringBuilder sb,
                                     final String        abbrev,
                                     final String        joinAbbrev)
            throws WhereControlException
        {
            if (sb == null)
            {
                throw new WhereControlException("Null string builder");
            }

            if (abbrev == null)
            {
                throw new WhereControlException("Null table abbreviation");
            }

            if (joinAbbrev == null)
            {
                throw new WhereControlException("Null join table abbreviation");
            }

            sb.append("(");

            sb.append(abbrev).append(".").append(getOne()).append(getFour());

            final String value = getTwo().toString().trim();

            if (getThree())
            {
                quote(sb, value);
            }
            else
            {
                sb.append(value);
            }

            sb.append(")");
        }


        /**
         * Sets parameter in prepared statement as required
         *
         * @param ps
         *            Prepared statement
         * @param index
         *            Beginning index for parameters
         *
         * @return True if parameter was set (and index used)
         *
         * @throws WhereControlException
         *             if an error occurs
         */
        @Override
		public boolean setParameter(final PreparedStatement ps,
                                    final int               index)
                throws WhereControlException
        {
            if (ps == null)
            {
                throw new WhereControlException("Null prepared statement");
            }

            return false;
        }
    }


    /**
     * Class that represents a query on an ERT column with a direct value and no
     * parameter necessary.
     * 
     * @version MPCS-8104 - changed to a quintuplet in order to store
     *                            nanosecond value for increased query resolution.
     */
    @SuppressWarnings("serial")

	private static class ErtWhereType
        extends Quintuplet<String, String, Long, Long, Boolean> implements IWhereType
    {
        /**
         * Constructor.
         *
         * @param coarse
         *            Coarse column
         * @param fine
         *            Fine column
         * @param value
         *            Time value in milliseconds
         * @param nanoValue
         *            Time value for nanosecond accuracy
         * @param upperBound
         *            True for upper-bound, else lower-bound
         *
         * @throws WhereControlException
         *             if an error occurs
         * 
         * @version MPCS-8104 - Updated to reflect change to
         *          quintuplet
         */
        public ErtWhereType(final String  coarse,
                            final String  fine,
                            final long    value,
                            final long    nanoValue,
                            final boolean upperBound)
            throws WhereControlException
        {
            super(coarse, fine, value, nanoValue, upperBound);

            if (coarse == null)
            {
                throw new WhereControlException("Null coarse column");
            }

            if (fine == null)
            {
                throw new WhereControlException("Null fine column");
            }
        }


        /**
         * Generates portion of where clause.
         *
         * @param sb
         *            Where clause in progress
         * @param abbrev
         *            Table abbreviation
         * @param joinAbbrev
         *            Join table abbreviation
         *
         * @throws WhereControlException
         *             if an error occurs
         * 
         * @version MPCS-8104 - Updated to reflect change to
         *          quintuplet
         */
        @Override
		public void addToWhereClause(final StringBuilder sb,
                                     final String        abbrev,
                                     final String        joinAbbrev)
            throws WhereControlException
        {
            if (sb == null)
            {
                throw new WhereControlException("Null string builder");
            }

            if (abbrev == null)
            {
                throw new WhereControlException("Null table abbreviation");
            }

            if (joinAbbrev == null)
            {
                throw new WhereControlException("Null join table abbreviation");
            }

            final char op     = (getFive() ? '<' : '>');
            final long time   = getThree();
            final long nanoTime = getFour();
            final long coarse = DbTimeUtility.coarseFromExactNoThrow(time);
            final int  fine   = DbTimeUtility.ertFineFromExact(time, (int) nanoTime);

            sb.append('(');

            sb.append('(');
            sb.append(abbrev).append('.').append(getOne());
            sb.append(' ').append(op).append(' ').append(coarse);
            sb.append(')');

            sb.append(" OR ");

            sb.append('(');

                sb.append('(');
                sb.append(abbrev).append('.').append(getOne());
                sb.append(" = ").append(coarse);
                sb.append(')');

                sb.append(" AND ");

                sb.append('(');
                sb.append(abbrev).append('.').append(getTwo());
                sb.append(' ').append(op).append("= ").append(fine);
                sb.append(')');

            sb.append(')');

            sb.append(')');
        }


        /**
         * Sets parameter in prepared statement as required
         *
         * @param ps
         *            Prepared statement
         * @param index
         *            Beginning index for parameters
         *
         * @return True if parameter was set (and index used)
         *
         * @throws WhereControlException
         *             if an error occurs
         */
        @Override
		public boolean setParameter(final PreparedStatement ps,
                                    final int               index)
                throws WhereControlException
        {
            if (ps == null)
            {
                throw new WhereControlException("Null prepared statement");
            }

            return false;
        }
    }


    /**
     * Class that represents a query on a RCT column with a direct value and no
     * parameter necessary.
     *
     * @version MPCS-6808 New class
     */
    @SuppressWarnings("serial")
	private static class RctWhereType
        extends Quadruplet<String, String, Long, Boolean> implements IWhereType
    {
        /**
         * Constructor.
         *
         * @param coarse
         *            Coarse column
         * @param fine
         *            Fine column
         * @param value
         *            Time value in milliseconds
         * @param upperBound
         *            True for upper-bound, else lower-bound
         *
         * @throws WhereControlException
         *             if an error occurs
         */
        public RctWhereType(final String  coarse,
                            final String  fine,
                            final long    value,
                            final boolean upperBound)
            throws WhereControlException
        {
            super(coarse, fine, value, upperBound);

            if (coarse == null)
            {
                throw new WhereControlException("Null coarse column");
            }

            if (fine == null)
            {
                throw new WhereControlException("Null fine column");
            }
        }


        /**
         * Generates portion of where clause.
         *
         * @param sb
         *            Where clause in progress
         * @param abbrev
         *            Table abbreviation
         * @param joinAbbrev
         *            Join table abbreviation
         *
         * @throws WhereControlException
         *             if an error occurs
         */
        @Override
		public void addToWhereClause(final StringBuilder sb,
                                     final String        abbrev,
                                     final String        joinAbbrev)
            throws WhereControlException
        {
            if (sb == null)
            {
                throw new WhereControlException("Null string builder");
            }

            if (abbrev == null)
            {
                throw new WhereControlException("Null table abbreviation");
            }

            if (joinAbbrev == null)
            {
                throw new WhereControlException("Null join table abbreviation");
            }

            final char op     = (getFour() ? '<' : '>');
            final long time   = getThree();
            final long coarse = DbTimeUtility.coarseFromExactNoThrow(time);
            final int  fine   = DbTimeUtility.fineFromExact(time);

            sb.append('(');

            sb.append('(');
            sb.append(abbrev).append('.').append(getOne());
            sb.append(' ').append(op).append(' ').append(coarse);
            sb.append(')');

            sb.append(" OR ");

            sb.append('(');

                sb.append('(');
                sb.append(abbrev).append('.').append(getOne());
                sb.append(" = ").append(coarse);
                sb.append(')');

                sb.append(" AND ");

                sb.append('(');
                sb.append(abbrev).append('.').append(getTwo());
                sb.append(' ').append(op).append("= ").append(fine);
                sb.append(')');

            sb.append(')');

            sb.append(')');
        }


        /**
         * Sets parameter in prepared statement as required
         *
         * @param ps
         *            Prepared statement
         * @param index
         *            Beginning index for parameters
         *
         * @return True if parameter was set (and index used)
         *
         * @throws WhereControlException
         *             if an error occurs
         */
        @Override
		public boolean setParameter(final PreparedStatement ps,
                                    final int               index)
                throws WhereControlException
        {
            if (ps == null)
            {
                throw new WhereControlException("Null prepared statement");
            }

            return false;
        }
    }


    /**
     * Class that represents a query for a column that is integer, but we want
     * a boolean state, where a zero value is true, and hence, non-zero is
     * false. It is optional whether we add a check for NULL (meaning same as
     * zero, that is, true.) No parameter is necessary.
     */
    private static class TrueIsZeroWhereType
        extends Triplet<String, Boolean, Boolean> implements IWhereType
    {
		private static final long serialVersionUID = 1L;


		/**
         * Constructor.
         *
         * @param column
         *            the column to query
         * @param value
         *            the value to query for (true means query for 0, false
         *            means query for non-zero)
         * @param checkNull
         *            true means return rows with NULL values, false means do
         *            not return rows with NULL values
         *
         * @throws WhereControlException
         *             if an error occurs
         */
        public TrueIsZeroWhereType(final String  column,
                                   final Boolean value,
                                   final boolean checkNull)
            throws WhereControlException
        {
            super(column, value, checkNull);

            if (column == null)
            {
                throw new WhereControlException("Null column");
            }

            if (value == null)
            {
                throw new WhereControlException("Null value");
            }
        }


        /**
         * Generates portion of where clause.
         *
         * @param sb
         *            Where clause in progress
         * @param abbrev
         *            Table abbreviation
         * @param joinAbbrev
         *            Join table abbreviation
         *
         * @throws WhereControlException
         *             if an error occurs
         */
        @Override
		public void addToWhereClause(final StringBuilder sb,
                                     final String        abbrev,
                                     final String        joinAbbrev)
            throws WhereControlException
        {
            if (sb == null)
            {
                throw new WhereControlException("Null string builder");
            }

            if (abbrev == null)
            {
                throw new WhereControlException("Null table abbreviation");
            }

            if (joinAbbrev == null)
            {
                throw new WhereControlException("Null join table abbreviation");
            }

            final String  column = getOne();
            final boolean value  = getTwo();
            final boolean doNull = value && getThree();

            if (doNull)
            {
                // Open extra level
                sb.append("(");

                sb.append("(");
                sb.append(abbrev).append(".").append(column);
                sb.append(" is NULL");
                sb.append(")");

                sb.append(" OR ");
            }

            sb.append("(");
            sb.append(abbrev).append(".").append(column);

            // True => column is true if zero
            sb.append(value ? "=0" : "!=0");

            sb.append(")");

            if (doNull)
            {
                // Close extra level
                sb.append(")");
            }
        }


        /**
         * Sets parameter in prepared statement as required
         *
         * @param ps
         *            Prepared statement
         * @param index
         *            Beginning index for parameters
         *
         * @return True if parameter was set (and index used)
         *
         * @throws WhereControlException
         *             if an error occurs
         */
        @Override
		public boolean setParameter(final PreparedStatement ps,
                                    final int               index)
                throws WhereControlException
        {
            if (ps == null)
            {
                throw new WhereControlException("Null prepared statement");
            }

            return false;
        }
    }


    /**
     * Class that represents a query for a column that may be null. We want a
     * boolean state where non-null is considered false and null true.
     */
    private static class TrueIsNullWhereType
        extends Pair<String, Boolean> implements IWhereType
    {
		private static final long serialVersionUID = 1L;


		/**
         * Constructor.
         *
         * @param column
         *            the column to query
         * @param value
         *            the value for which to query
         *
         * @throws WhereControlException
         *             if an error occurs
         */
        public TrueIsNullWhereType(final String  column,
                                   final Boolean value)
            throws WhereControlException
        {
            super(column, value);

            if (column == null)
            {
                throw new WhereControlException("Null column");
            }

            if (value == null)
            {
                throw new WhereControlException("Null value");
            }
        }


        /**
         * Generates portion of where clause.
         *
         * @param sb
         *            Where clause in progress
         * @param abbrev
         *            Table abbreviation
         * @param joinAbbrev
         *            Join table abbreviation
         *
         * @throws WhereControlException
         *             if an error occurs
         */
        @Override
		public void addToWhereClause(final StringBuilder sb,
                                     final String        abbrev,
                                     final String        joinAbbrev)
            throws WhereControlException
        {
            if (sb == null)
            {
                throw new WhereControlException("Null string builder");
            }

            if (abbrev == null)
            {
                throw new WhereControlException("Null table abbreviation");
            }

            if (joinAbbrev == null)
            {
                throw new WhereControlException("Null join table abbreviation");
            }

            final String  column = getOne();
            final boolean value  = getTwo();

            sb.append("(");

            sb.append(abbrev).append(".").append(column);

            // Column is true if NULL

            sb.append(value ? " is NULL" : " is not NULL");

            sb.append(")");
        }


        /**
         * Sets parameter in prepared statement as required
         *
         * @param ps
         *            Prepared statement
         * @param index
         *            Beginning index for parameters
         *
         * @return True if parameter was set (and index used)
         *
         * @throws WhereControlException
         *             if an error occurs
         */
        @Override
		public boolean setParameter(final PreparedStatement ps,
                                    final int               index)
                throws WhereControlException
        {
            if (ps == null)
            {
                throw new WhereControlException("Null prepared statement");
            }

            return false;
        }
    }


    /**
     * Class that represents a query for a VCFC range.
     * No parameter is necessary.
     */
    private static class VcfcRangesWhereType extends Object implements IWhereType
    {
        private final VcfcRanges _vcfcs;


        /**
         * Constructor.
         *
         * @param vcfcs
         *            the vcfc range to to query for
         *
         * @throws WhereControlException
         *             if an error occurs
         */
        public VcfcRangesWhereType(final VcfcRanges vcfcs)
            throws WhereControlException
        {
            super();

            if (vcfcs == null)
            {
                throw new WhereControlException("Null vcfcs");
            }

            if (vcfcs.isEmpty())
            {
                throw new WhereControlException("Empty vcfcs");
            }

            _vcfcs = vcfcs;
        }


        /**
         * Generates portion of where clause.
         *
         * @param sb
         *            Where clause in progress
         * @param abbrev
         *            Table abbreviation
         * @param joinAbbrev
         *            Join table abbreviation
         *
         * @throws WhereControlException
         *             if an error occurs
         */
        @Override
		public void addToWhereClause(final StringBuilder sb,
                                     final String        abbrev,
                                     final String        joinAbbrev)
            throws WhereControlException
        {
            if (sb == null)
            {
                throw new WhereControlException("Null string builder");
            }

            if (abbrev == null)
            {
                throw new WhereControlException("Null table abbreviation");
            }

            if (joinAbbrev == null)
            {
                throw new WhereControlException("Null join table abbreviation");
            }

            sb.append(_vcfcs.whereClause(abbrev));
        }


        /**
         * Sets parameter in prepared statement as required
         *
         * @param ps
         *            Prepared statement
         * @param index
         *            Beginning index for parameters
         *
         * @return True if parameter was set (and index used)
         *
         * @throws WhereControlException
         *             if an error occurs
         */
        @Override
		public boolean setParameter(final PreparedStatement ps,
                                    final int               index)
                throws WhereControlException
        {
            if (ps == null)
            {
                throw new WhereControlException("Null prepared statement");
            }

            return false;
        }
    }


    /**
     * Class that represents a query for a DSS set.
     * No parameter is necessary.
     */
    private static class DssWhereType extends Object implements IWhereType
    {
        private final String       _column;
        private final Set<Integer> _dss;


        /**
         * Constructor. We no longer add zero as a wildcard.
         *
         * @param column
         *            the column to query
         * @param dss
         *            the set of DSSIDs to query for
         *
         * @throws WhereControlException
         *             if an error occurs
         */
        public DssWhereType(final String       column,
                            final Set<Integer> dss)
            throws WhereControlException
        {
            super();

            if (column == null)
            {
                throw new WhereControlException("Null column");
            }

            if (dss == null)
            {
                throw new WhereControlException("Null dss");
            }

            if (dss.isEmpty())
            {
                throw new WhereControlException("Empty dss");
            }

            _column = column;
            _dss    = new TreeSet<Integer>(dss);

            // Zero acts like a wild-card

            // _dss.add(0);
        }


        /**
         * Generates portion of where clause.
         *
         * @param sb
         *            Where clause in progress
         * @param abbrev
         *            Table abbreviation
         * @param joinAbbrev
         *            Join table abbreviation
         *
         * @throws WhereControlException
         *             if an error occurs
         */
        @Override
		public void addToWhereClause(final StringBuilder sb,
                                     final String        abbrev,
                                     final String        joinAbbrev)
            throws WhereControlException
        {
            if (sb == null)
            {
                throw new WhereControlException("Null string builder");
            }

            if (abbrev == null)
            {
                throw new WhereControlException("Null table abbreviation");
            }

            if (joinAbbrev == null)
            {
                throw new WhereControlException("Null join table abbreviation");
            }

            boolean first = true;

            sb.append('(');
            sb.append(abbrev).append('.').append(_column).append(" IN (");

            for (final Integer i : _dss)
            {
                if (first)
                {
                    first = false;
                }
                else
                {
                    sb.append(',');
                }

                sb.append(i);
            }

            sb.append("))");
        }


        /**
         * Sets parameter in prepared statement as required
         *
         * @param ps
         *            Prepared statement
         * @param index
         *            Beginning index for parameters
         *
         * @return True if parameter was set (and index used)
         *
         * @throws WhereControlException
         *             if an error occurs
         */
        @Override
		public boolean setParameter(final PreparedStatement ps,
                                    final int               index)
                throws WhereControlException
        {
            if (ps == null)
            {
                throw new WhereControlException("Null prepared statement");
            }

            return false;
        }
    }


    /**
     * Class that represents a query for a VCID set.
     * No parameter is necessary.
     */
    private static class VcidWhereType extends Object implements IWhereType
    {
        private final String       _column;
        private final Set<Integer> _vcid;


        /**
         * Constructor.
         *
         * @param column
         *            the column to query
         * @param vcid
         *            the set of VCIDs to query for
         *
         * @throws WhereControlException
         *             if an error occurs
         */
        public VcidWhereType(final String       column,
                             final Set<Integer> vcid)
            throws WhereControlException
        {
            super();

            if (column == null)
            {
                throw new WhereControlException("Null column");
            }

            if (vcid == null)
            {
                throw new WhereControlException("Null vcid");
            }

            if (vcid.isEmpty())
            {
                throw new WhereControlException("Empty vcid");
            }

            _column = column;
            _vcid   = vcid;
        }


        /**
         * Generates portion of where clause. Note that there is no NULL check
         * because Frame.vcid is not nullable.
         *
         * @param sb
         *            Where clause in progress
         * @param abbrev
         *            Table abbreviation
         * @param joinAbbrev
         *            Join table abbreviation
         *
         * @throws WhereControlException
         *             if an error occurs
         */
        @Override
		public void addToWhereClause(final StringBuilder sb,
                                     final String        abbrev,
                                     final String        joinAbbrev)
            throws WhereControlException
        {
            if (sb == null)
            {
                throw new WhereControlException("Null string builder");
            }

            if (abbrev == null)
            {
                throw new WhereControlException("Null table abbreviation");
            }

            if (joinAbbrev == null)
            {
                throw new WhereControlException("Null join table abbreviation");
            }

            boolean first = true;

            sb.append('(');
            sb.append(abbrev).append('.').append(_column).append(" IN (");

            for (final Integer i : _vcid)
            {
                if (first)
                {
                    first = false;
                }
                else
                {
                    sb.append(',');
                }

                sb.append(i);
            }

            sb.append("))");
        }


        /**
         * Sets parameter in prepared statement as required
         *
         * @param ps
         *            Prepared statement
         * @param index
         *            Beginning index for parameters
         *
         * @return True if parameter was set (and index used)
         *
         * @throws WhereControlException
         *             if an error occurs
         */
        @Override
		public boolean setParameter(final PreparedStatement ps,
                                    final int               index)
                throws WhereControlException
        {
            if (ps == null)
            {
                throw new WhereControlException("Null prepared statement");
            }

            return false;
        }
    }


    /**
     * Standard exception thrown by WhereControl.
     */
    public static class WhereControlException extends Exception
    {
		private static final long serialVersionUID = 1L;


		/**
         * Constructor.
         *
         * @param message Message
         * @param cause   Cause
         */
        public WhereControlException(final String    message,
                                     final Throwable cause)
        {
            super(message, cause);
        }


        /**
         * Constructor.
         *
         * @param message message
         */
        public WhereControlException(final String message)
        {
            super(message);
        }
    }
}
