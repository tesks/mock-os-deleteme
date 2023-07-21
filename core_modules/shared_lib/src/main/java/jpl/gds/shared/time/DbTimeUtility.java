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
package jpl.gds.shared.time;

import jpl.gds.shared.types.Pair;


/**
 * Utility methods for database time manipulation.
 * 
 */
public class DbTimeUtility extends Object
{
    /** Maximum value for coarse time */
    public static final long MAX_COARSE    = -1L >>> 32L;

    /** Maximum value for ERT fine time */
    public static final int  MAX_ERT_FINE  = 999999999; // Nanoseconds

    /** Maximum value for long SCET fine time */
    public static final int  MAX_SCET_FINE       = 999999999; // Nanoseconds
    /** Maximum value for short SCET fine time */
    public static final int  MAX_SCET_FINE_SHORT =       999; // Milliseconds

    /** Maximum value for non-ERT/SCET fine time */
    public static final int  MAX_FINE      =      999; // Milliseconds

    private static final long THOUSAND_L = 1000L;
    private static final int  MILLION_I  = 1000000;
    private static final long MILLION_L  = 1000000L;

    /** Conversion for SCET fine time <=> extended/short  */
    public static final int SCET_SHORT_CONVERSION = MILLION_I;


    /**
     * Private constructor to prevent creation.
     */
    private DbTimeUtility()
    {
        super();
    }


    /**
     * Convert ERT times stored in database as coarse and fine back to standard
     * Java time. I.e., get milliseconds from fine and add to coarse.
     *
     * @param coarse The coarse time as seconds since 1970 (unsigned int)
     * @param fine   The fine time as nanoseconds          (unsigned int)
     *
     * @return Exact time as milliseconds
     */
    public static long exactFromErtCoarseFine(final long coarse,
                                              final int  fine)
    {
        return ((coarse * THOUSAND_L) + (fine / MILLION_I));
    }


    /**
     * Convert SCET times stored in database as coarse and fine back to standard
     * Java time. I.e., get milliseconds from fine and add to coarse.
     *
     * @param coarse The coarse time as seconds since 1970 (unsigned int)
     * @param fine   The fine time as nanoseconds          (unsigned int)
     *
     * @return Exact time as milliseconds
     *
     */
    public static long exactFromScetCoarseFine(final long coarse,
                                               final int  fine)
    {
        return ((coarse * THOUSAND_L) + (fine / MILLION_I));
    }


    /**
     * Convert ERT time stored in database as fine back to standard fine.
     * I.e., get rid of milliseconds leaving microseconds and nanoseconds.
     *
     * @param fine The fine time as nanoseconds (unsigned int)
     *
     * @return Fine time (microseconds and nanoseconds)
     */
    public static int fineFromErtFine(final int fine)
    {
        return (fine % MILLION_I);
    }


    /**
     * Convert SCET time stored in database as fine back to standard fine.
     * I.e., get rid of milliseconds leaving microseconds and nanoseconds.
     *
     * @param fine The fine time as nanoseconds (unsigned int)
     *
     * @return Fine time (microseconds and nanoseconds)
     */
    public static int fineFromScetFine(final int fine)
    {
        return (fine % MILLION_I);
    }


    /**
     * Convert times stored in database as coarse and fine back to standard
     * Java time. For non-ERT where fine is just milliseconds.
     *
     * @param coarse The coarse time as seconds since 1970 (unsigned int)
     * @param fine   The fine time as milliseconds         (unsigned short)
     *
     * @return Exact time as milliseconds
     */
    public static long exactFromCoarseFine(final long coarse,
                                           final int  fine)
    {
        return ((coarse * THOUSAND_L) + fine);
    }


    /**
     * Convert times stored in database as coarse and fine to a Date.
     *
     * @param coarse The coarse time as seconds since 1970 (unsigned int)
     * @param fine   The fine time as milliseconds         (unsigned short)
     *
     * @return Exact time as Date
     */
    public static IAccurateDateTime dateFromCoarseFine(final long coarse,
                                          final int  fine)
    {
        return new AccurateDateTime(exactFromCoarseFine(coarse, fine));
    }


    /**
     * Convert Java time to coarse time (seconds since 1970.)
     *
     * @param exact Exact time as milliseconds
     *
     * @return Coarse time
     *
     * @throws TimeTooLargeException Time out of bounds
     */
    public static long coarseFromExact(final long exact)
        throws TimeTooLargeException
    {
        if (exact < 0L)
        {
            throw new TimeTooLargeException();
        }

        final long result = exact / THOUSAND_L;

        if (result > MAX_COARSE)
        {
            throw new TimeTooLargeException();
        }

        return result;
    }


    /**
     * Convert Java time to coarse time (seconds since 1970.)
     *
     * @param exact Exact time as milliseconds
     *
     * @return Coarse time
     */
    public static long coarseFromExactNoThrow(final long exact)
    {
        if (exact < 0L)
        {
            return MAX_COARSE;
        }

        final long result = exact / THOUSAND_L;

        if (result > MAX_COARSE)
        {
            return MAX_COARSE;
        }

        return result;
    }


    /**
     * Convert Java time to fine time (milliseconds)
     *
     * @param exact Exact time as milliseconds
     *
     * @return Fine time
     */
    public static int fineFromExact(final long exact)
    {
        return (int) (exact % THOUSAND_L);
    }


    /**
     * Convert Java time to ERT fine time (nanoseconds). In this case we have
     * nothing below milliseconds.
     *
     * @param exact Exact time as milliseconds
     *
     * @return Fine time
     */
    public static int ertFineFromExact(final long exact)
    {
        return (int) ((exact % THOUSAND_L) * MILLION_L);
    }


    /**
     * Convert Java time to ERT fine time (nanoseconds).
     *
     * @param exact Exact time as milliseconds
     * @param fine  Fine addendum (microseconds and nanoseconds)
     *
     * @return Fine time
     */
    public static int ertFineFromExact(final long exact,
                                       final int  fine)
    {
        return (int) (((exact % THOUSAND_L) * MILLION_L) + fine);
    }


    /**
     * Convert Java time to SCET fine time (nanoseconds).
     *
     * @param exact Exact time as milliseconds
     * @param fine  Fine addendum (microseconds and nanoseconds)
     *
     * @return Fine time
     *
     */
    public static int scetFineFromExact(final long exact,
                                        final int  fine)
    
    { 
        return (int) (((exact % THOUSAND_L) * MILLION_L) + fine);
    }


    /**
     * Convert Java time to SCET fine time (millseconds).
     *
     * @param exact Exact time as milliseconds
     * @param fine  Fine addendum (microseconds and nanoseconds)
     *
     * @return Fine time
     *
     */
    public static int scetFineFromExactShort(final long exact,
                                             final int  fine)
    {
        return scetFineFromExact(exact, fine) / SCET_SHORT_CONVERSION;
    }


    /**
     * Construct a where clause from a begin and/or end time range.
     *
     * @param abbrev   Table abbreviation
     * @param dtr      Time range
     * @param product  True for Product, false otherwise
     * @param monitor  True for MonitorChannelValue, false otherwise
     * @param extended True if using extended tables
     *
     * @return String Resulting where clause
     * 
     */
    public static String generateTimeWhereClause(
                             final String            abbrev,
                             final DatabaseTimeRange dtr,
                             final boolean           product,
                             final boolean           monitor,
                             final boolean           extended)
    {
        if ((dtr == null) || ! dtr.isRangeSpecified())
        {
            return "";
        }

        final Pair<String, String> columns = columnNames(dtr, product, monitor, false);

        boolean          doStart = false;
        boolean          doEnd   = false;
        long             sCoarse = 0L;
        long             sFine   = 0L;
        long             eCoarse = 0L;
        long             eFine   = 0L;
        IAccurateDateTime dtemp   = null;
        ISclk            stemp = null;
        long             ltemp   = 0L;
        long             ntemp   = 0L;
        boolean          scet    = false;

        switch (dtr.getTimeType().getValueAsInt())
        {
            case DatabaseTimeType.RCT_TYPE:
            case DatabaseTimeType.CREATION_TIME_TYPE:
            case DatabaseTimeType.EVENT_TIME_TYPE:
                dtemp = dtr.getStartTime();

                if (dtemp != null)
                {
                    doStart = true;
                    ltemp   = dtemp.getTime();
                    sCoarse = coarseFromExactNoThrow(ltemp);
                    sFine   = fineFromExact(ltemp);
                }

                dtemp = dtr.getStopTime();

                if (dtemp != null)
                {
                    doEnd   = true;
                    ltemp   = dtemp.getTime();
                    eCoarse = coarseFromExactNoThrow(ltemp);
                    eFine   = fineFromExact(ltemp);
                }

                break;

            // Get and use the millisecond value in ERT queries
            case DatabaseTimeType.ERT_TYPE:
                dtemp = dtr.getStartTime();

                if (dtemp != null)
                {
                    doStart = true;
                    ltemp   = dtemp.getTime();
                    ntemp   = dtemp.getNanoseconds();
                    sCoarse = coarseFromExactNoThrow(ltemp);
                    sFine   = ertFineFromExact(ltemp, (int) ntemp);
                }

                dtemp = dtr.getStopTime();

                if (dtemp != null)
                {
                    doEnd   = true;
                    ltemp   = dtemp.getTime();
                    ntemp   = dtemp.getNanoseconds();
                    eCoarse = coarseFromExactNoThrow(ltemp);
                    eFine   = ertFineFromExact(ltemp, (int) ntemp);
                }

                break;

            case DatabaseTimeType.SCET_TYPE:
                scet  = true;
                dtemp = dtr.getStartTime();

                if (dtemp != null)
                {
                    doStart = true;
                    ltemp   = dtemp.getTime();
                    ntemp   = dtemp.getNanoseconds();
                    sCoarse = coarseFromExactNoThrow(ltemp);
                    sFine   = ertFineFromExact(ltemp, (int) ntemp);
                }

                dtemp = dtr.getStopTime();

                if (dtemp != null)
                {
                    doEnd   = true;
                    ltemp   = dtemp.getTime();
                    ntemp   = dtemp.getNanoseconds();
                    eCoarse = coarseFromExactNoThrow(ltemp);
                    eFine   = ertFineFromExact(ltemp, (int) ntemp);
                }

                break;

            case DatabaseTimeType.SCLK_TYPE:
            case DatabaseTimeType.LST_TYPE:
                stemp = dtr.getStartSclk();

                if (stemp != null)
                {
                    doStart = true;
                    sCoarse = stemp.getCoarse();
                    sFine   = stemp.getFine();
                }

                stemp = dtr.getStopSclk();

                if (stemp != null)
                {
                    doEnd   = true;
                    eCoarse = stemp.getCoarse();
                    eFine   = stemp.getFine();
                }

                break;
        }

        final StringBuilder sb = new StringBuilder();

        if (doStart)
        {
            generateTimeWhereClause(sb,
                                    columns.getOne(),
                                    columns.getTwo(),
                                    abbrev,
                                    sCoarse,
                                    sFine,
                                    false,
                                    extended,
                                    scet);
        }

        if (doEnd)
        {
            if (doStart)
            {
                sb.append(" AND ");
            }

            generateTimeWhereClause(sb,
                                    columns.getOne(),
                                    columns.getTwo(),
                                    abbrev,
                                    eCoarse,
                                    eFine,
                                    true,
                                    extended,
                                    scet);
        }

        return sb.toString();
    }


    /**
     * Construct a where clause from a begin or end time range.
     *
     * @param sb           Append to this
     * @param coarseColumn Name of column for time coarse
     * @param fineColumn   Name of column for time fine
     * @param abbrev       Table abbreviation
     * @param coarse       Coarse value
     * @param fine         Fine value
     * @param upperBound   True if upper-bound, else lower-bound
     * @param extended     True for extended tables
     * @param scet         True for SCET
     *
     */
    private static void generateTimeWhereClause(
                            final StringBuilder sb,
                            final String        coarseColumn,
                            final String        fineColumn,
                            final String        abbrev,
                            final long          coarse,
                            final long          fine,
                            final boolean       upperBound,
                            final boolean       extended,
                            final boolean       scet)
    {
        final char op = (upperBound ? '<' : '>');

        sb.append('(');

        sb.append('(');
            sb.append(abbrev).append('.').append(coarseColumn);
            sb.append(' ').append(op).append(' ').append(coarse);
        sb.append(')');

        sb.append(" OR ");

        sb.append('(');
            sb.append('(');
                sb.append(abbrev).append('.').append(coarseColumn);
                sb.append(" = ").append(coarse);
            sb.append(')');

            sb.append(" AND ");

            sb.append('(');
                if (extended || ! scet)
                {
                    sb.append(abbrev).append('.').append(fineColumn);
                }
                else
                {
                    sb.append('(');
                        sb.append(abbrev).append('.');
                        sb.append(fineColumn).append(" * ");
                        sb.append(SCET_SHORT_CONVERSION);
                    sb.append(')');
                }

                sb.append(' ').append(op).append("= ").append(fine);
            sb.append(')');
        sb.append(')');

        sb.append(')');
    }


    /**
     * Construct column names based upon the time type.
     *
     * @param dtr     Time range
     * @param product True for Product, false otherwise
     * @param monitor True for MonitorChannelValue, false otherwise
     *
     * @return Pair of names
     */
    private static Pair<String, String> columnNames(
                                            final DatabaseTimeRange dtr,
                                            final boolean           product,
                                            final boolean           monitor,
                                            final boolean           aggregate)
    {
        String base = "unknown";

        switch (dtr.getTimeType().getValueAsInt())
        {
            case DatabaseTimeType.SCET_TYPE:
                base = (product ? "dvtScet" : "scet");
                break;

            case DatabaseTimeType.RCT_TYPE:
                base = "rct";
                break;

            case DatabaseTimeType.CREATION_TIME_TYPE:
                base = "creationTime";
                break;

            case DatabaseTimeType.EVENT_TIME_TYPE:
                base = "eventTime";
                break;

            case DatabaseTimeType.ERT_TYPE:
                base = (monitor ? "mst" : "ert");
                break;

            case DatabaseTimeType.SCLK_TYPE:
            case DatabaseTimeType.LST_TYPE:
                base = (product ? "dvtSclk" : "sclk");
                break;

            default:
                break;
        }
        
        if (aggregate) {
        	base = base.substring(0, 1).toUpperCase() + base.substring(1);
        }

        return new Pair<String, String>(base + "Coarse", base + "Fine");
    }
    
    
    /**
     * Construct a where clause from a begin and/or end time range for Aggregate tables.
     *
     * @param abbrev   Table abbreviation
     * @param dtr      Time range
     * @param monitor  True for MonitorChannelValue, false otherwise
     *
     * @return String Resulting where clause
     * 
     */
    public static String generateAggregateTimeWhereClause(
                             final String            abbrev,
                             final DatabaseTimeRange dtr,
                             final boolean           monitor)
    {
        if ((dtr == null) || ! dtr.isRangeSpecified())
        {
            return "";
        }

        final Pair<String, String> columns = columnNames(dtr, false, monitor, true);

        boolean          doStart = false;
        boolean          doEnd   = false;
        long             sCoarse = 0L;
        long             eCoarse = 0L;
        IAccurateDateTime dtemp   = null;
        ISclk            stemp = null;
        long             ltemp   = 0L;

        switch (dtr.getTimeType().getValueAsInt())
        {
            case DatabaseTimeType.RCT_TYPE:
            case DatabaseTimeType.CREATION_TIME_TYPE:
            case DatabaseTimeType.EVENT_TIME_TYPE:
                dtemp = dtr.getStartTime();

                if (dtemp != null)
                {
                    doStart = true;
                    ltemp   = dtemp.getTime();
                    sCoarse = coarseFromExactNoThrow(ltemp);
                }

                dtemp = dtr.getStopTime();

                if (dtemp != null)
                {
                    doEnd   = true;
                    ltemp   = dtemp.getTime();
                    eCoarse = coarseFromExactNoThrow(ltemp);
                }

                break;

            // Get and use the millisecond value in ERT queries
            case DatabaseTimeType.ERT_TYPE:
                dtemp = dtr.getStartTime();

                if (dtemp != null)
                {
                    doStart = true;
                    ltemp   = dtemp.getTime();
                    sCoarse = coarseFromExactNoThrow(ltemp);
                }

                dtemp = dtr.getStopTime();

                if (dtemp != null)
                {
                    doEnd   = true;
                    ltemp   = dtemp.getTime();
                    eCoarse = coarseFromExactNoThrow(ltemp);
                }

                break;

            case DatabaseTimeType.SCET_TYPE:
                dtemp = dtr.getStartTime();

                if (dtemp != null)
                {
                    doStart = true;
                    ltemp   = dtemp.getTime();
                    sCoarse = coarseFromExactNoThrow(ltemp);
                }

                dtemp = dtr.getStopTime();

                if (dtemp != null)
                {
                    doEnd   = true;
                    ltemp   = dtemp.getTime();
                    eCoarse = coarseFromExactNoThrow(ltemp);
                }

                break;

            case DatabaseTimeType.SCLK_TYPE:
            case DatabaseTimeType.LST_TYPE:
                stemp = dtr.getStartSclk();

                if (stemp != null)
                {
                    doStart = true;
                    sCoarse = stemp.getCoarse();
                }

                stemp = dtr.getStopSclk();

                if (stemp != null)
                {
                    doEnd   = true;
                    eCoarse = stemp.getCoarse();
                }

                break;
        }

        final StringBuilder sb = new StringBuilder();

        if (doStart)
        {
            // chill_get_chanvals time based query performance issue
            // This change essentially modifies how the WHERE clause specifies the begin and end time
            //
            // Example based on an ERT time range query
            // ...(ca.endErtCoarse >= 1594255415) AND (ca.beginErtCoarse <= 1594256701)
            // to
            // ...(ca.beginErtCoarse >= 1594255415) AND (ca.beginErtCoarse <= 1594256701)
            // There are indexes specified on both the 'beginErtCoarse' field as well as the 'endErtCoarse'
            // however the MariaDB query optimizer decides to only use the beginErtCoarse index.
            // When the time bounds are specified using the same field (beginErtCoarse) the only downside to
            // this is the fact that potentially more aggregate records could match the query condition.
            // The previous approach:
            // |------------------------------------------|
            // |Start record....................End record|
            // New approach with using the 'beginErtCoarse' for time bounds
            // |------------------------------------------|
            // |Start record..............................End record(s)
            // The Java portion of the processing will still filter out the samples based on the times specified
            // So the final record count output will still be the same.
            generateAggregateTimeWhereClause(sb,
                                    "begin" + columns.getOne(),
                                    abbrev,
                                    sCoarse,
                                    true);
        }

        if (doEnd)
        {
            if (doStart)
            {
                sb.append(" AND ");
            }

            generateAggregateTimeWhereClause(sb,
            						"begin" + columns.getOne(),
                                    abbrev,
                                    eCoarse,
                                    false);
        }

        return sb.toString();
    }
    
    /**
     * Construct a where clause from a begin or end time range.
     *
     * @param sb           Append to this
     * @param coarseColumn Name of column for time coarse
     * @param abbrev       Table abbreviation
     * @param coarse       Coarse value
     * @param upperBound   True if upper-bound, else lower-bound
     *
     */
    private static void generateAggregateTimeWhereClause(
            final StringBuilder sb, 
            final String coarseColumn,
            final String abbrev, 
            final long coarse, 
            final boolean upperBound) {
        final String op = (upperBound ? ">=" : "<=");

        sb.append('(');
        sb.append(abbrev).append('.').append(coarseColumn);
        sb.append(' ').append(op).append(' ').append(coarse);
        sb.append(')');
    }

}
