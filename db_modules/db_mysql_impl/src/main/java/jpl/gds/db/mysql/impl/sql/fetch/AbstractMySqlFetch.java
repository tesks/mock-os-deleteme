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
package jpl.gds.db.mysql.impl.sql.fetch;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.context.ApplicationContext;

import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.fetch.IDbSqlFetch;
import jpl.gds.db.api.sql.fetch.IDbSqlFetchFactory;
import jpl.gds.db.api.sql.fetch.QueryConfig;
import jpl.gds.db.api.types.IDbContextInfoProvider;
import jpl.gds.db.api.types.IDbRecord;
import jpl.gds.db.mysql.impl.sql.AbstractMySqlInteractor;
import jpl.gds.shared.exceptions.SqlExceptionTools;
import jpl.gds.shared.holders.ApidHolder;
import jpl.gds.shared.holders.HeaderHolder;
import jpl.gds.shared.holders.StationIdHolder;
import jpl.gds.shared.holders.TrailerHolder;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.string.StringUtil;
import jpl.gds.shared.time.DatabaseTimeRange;
import jpl.gds.shared.types.UnsignedLong;


/**
 * This is the abstract parent class for all database fetch classes
 * (classes that are responsible for retrieving information from the
 * database).  It contains some common convenience methods for use
 * by subclasses.
 *
 */
public abstract class AbstractMySqlFetch extends AbstractMySqlInteractor implements IDbSqlFetch
{
	/**
	 * The prepared statement used to build and execute fetch queries
	 */
	protected PreparedStatement statement = null;


	/**
	 * The statement used to build and execute fetch queries
	 */
	protected Statement plainStatement = null;

	/**
	 * The flag that indicates whether the SQL statement should
	 * simply be printed out or executed
	 */
	protected boolean printStmtOnly;

	/**
	 * The set of results obtained by executing a query
	 */
	protected ResultSet results;

	/**
	 * The size of the batches of results that will be returned to the user
	 */
	protected int batchSize;

    // The next four are defined for Frame and *Packet

    /** Set true initially to mark first row received */
    protected boolean firstRow = true;

    /** Keep track of header "null state" of each row as it arrives */
    protected boolean headerNullState = true;

    /** Keep track of trailer "null state" of each row as it arrives */
    protected boolean trailerNullState = true;

    /** Keep track of type of each row as it arrives. Just set to null if not needed. */
    protected String type = null;

    /**
     * The Query Configuration retrieved from the Spring Application Context in
     * the init() method
     */
    protected QueryConfig queryConfig;


    /**
     * MPCS-9572 - added fetch factory member, for direct use
     * rather than going through the archive controller.
     */
    protected final IDbSqlFetchFactory fetchFactory; 

	/**
     * Creates an instance of AbstractMySqlFetch.
     * 
     * @param appContext
     *            the Spring Application Context
     * @param printSqlStmt
     *            The flag that indicates whether the fetch class should print
     *            out the SQL statement only or execute it.
     * @param prepared
     *            True if prepared statement needed
     */
	public AbstractMySqlFetch(final ApplicationContext appContext, final boolean printSqlStmt,
                         final boolean prepared) 
	{
		super(appContext, prepared, true); // Connect

		/* MPCS-9572  init fetch factory */
		this.fetchFactory = appContext.getBean(IDbSqlFetchFactory.class);
		init(printSqlStmt);
		
	}


	/**
     * Creates an instance of AbstractMySqlFetch.
     * 
     * @param appContext
     *            the Spring Application Context
     * @param printSqlStmt
     *            The flag that indicates whether the fetch class should print
     *            out the SQL statement only or execute it.
     */
	public AbstractMySqlFetch(final ApplicationContext appContext, final boolean printSqlStmt) 
	{
        this(appContext, printSqlStmt, true);
		// Need a prepared statement in WrappedConnection
	}


	/**
	 * Initialize class member variables
	 *
	 */
	private void init(final boolean printSqlStmt) 
	{
		queryConfig = appContext.getBean(QueryConfig.class);
		
		results = null;
		statement = null;
        plainStatement = null;
		batchSize = 1;
		printStmtOnly = printSqlStmt;
	}

	/**
	 * Initialize this class for starting a new query
	 * 
	 * @param batchSize The new batch size value for the upcoming query
	 */
	protected void initQuery(final int batchSize)
	{
		//make sure the result set is closed
		if(results != null)
		{
			try
			{
				results.close();
				results = null;
			}
			catch(final SQLException sqle)
			{
				//don't care
			}
		}

		if (! isConnected())
		{
			throw new IllegalStateException(
			"This connection has already been closed");
		}

		this.batchSize = batchSize;
	}

	/**
	 * Releases the connection resources for the query. It is a good idea to call close after completely processing the query results.
	 */
	@Override
	public void close()
	{
		if(results != null)
		{
			try
			{
				results.close();
			}
			catch(final SQLException ignore)
			{
                  // ignore this - nothing we can do 
			}
			results = null;
		}

		if(statement != null)
		{
			try
			{
				statement.close();
			}
			catch(final SQLException ignore)
			{
				 // ignore this - nothing we can do 
			}
			statement = null;
		}

        if (plainStatement != null)
        {
            try
            {
                plainStatement.close();
            }
            catch(final SQLException ignore)
            {
                plainStatement = null;
            }
            finally
            {
                plainStatement = null;
            }
        }

		batchSize = 1;
		super.close();
	}

    /* (non-Javadoc)
     * @see jpl.gds.db.api.sql.fetch.IDbSqlFetch#get(jpl.gds.shared.time.DatabaseTimeRange, int, java.lang.Object[])
     */
    @Override
    public List<? extends IDbRecord> get(final DatabaseTimeRange range, 
                                         final int batchSize,
                                         final Object... params) throws DatabaseException {
        /* MPCS-9572 - use session info factory rather than archive controller */
        return get(this.dbSessionInfoFactory.createQueryableProvider(), range, batchSize, params);
    }

	/* MPCS-10119 -  Added support to query by context */
	@Override
	public List<? extends IDbRecord> getByContext(final DatabaseTimeRange range, final int batchSize, final Object... params) throws DatabaseException{
		return get(this.dbContextInfoFactory.createQueryableProvider(), range, batchSize, true, params);
	}

	/**
	 * Populate the templated WHERE clause portion of the SQL and then execute
     * the query and return the results of the query.
	 *
	 * @param tsi         The test session information object used to specify
     *                    test sessions in the database to retrieve.
	 * @param range       The database time range specifying the type of time
     *                    and time range for querying.
     * @param whereClause The where clause itself
     * @param params      Parameters used to populate the where clause
     *
	 * @return The list of rows that matched the input search information
     *
	 * @throws DatabaseException If there is a problem retrieving from the database
	 *
	 * MPCS-9891 - Replaced first parameter with IDbContextInfoProvider
	 */
	protected abstract List<? extends IDbRecord>
        populateAndExecute(final IDbContextInfoProvider tsi,
                           final DatabaseTimeRange      range,
                           final String                 whereClause,
                           final Object...              params)
        throws DatabaseException;


    /**
     * This is the internal class method that keeps track of the JDBC ResultSet
     * returned by a query. Every call to this method will return a list of test
     * configurations that match the original query. The size of the returned
     * lists is determined by the batch size that was entered when the query was
     * made. When there are no more results, this method will return an empty
     * list.
     *
     * Warning is false positive.
     *
     * NB: Originally we declined to set LST for any monitor or header channel
     * because they would not have a SCET. But since then we have added the
     * ability of header channels to attempt to join with the parent
     * Packet/SsePacket. It is not our business here to decide why or why not
     * there is a SCET; but if the row has a non-NULL SCET we accept it.
     *
     * To make things more complicated, we make up a artificial (but approximate)
     * SCET from ERT for frame header channels. That is because they will never
     * have a Packet to join back to to get the SCET. That will cause trouble
     * with the global LAD; so we make one up so frame header channel values can
     * be stored therein. We do NOT make up a SCET for any other reason.
     *
     * Made-up SCETs are not used to derive a LST.
     *
     * All of that is now compatible with the header channelizers.
     *
     * @return The list of channel values that is part of the results of a query
     *         executed using the "getChannelValues(...)" methods. When an empty
     *         list is returned, it means no more results are left.
     * @throws DatabaseException
     *             if a database error occurs
     */
    protected abstract List<? extends IDbRecord> getResults()
        throws DatabaseException;


	/**
	 * This method formats and prints to standard output the
	 * toString() value of a PreparedStatement object.
	 * 
	 * @param stmt PreparedStatement object whose toString() value
	 * 			   will be printed to standard output.
	 */
	protected void printSqlStatement(final PreparedStatement stmt)
	{
		printSqlStatement(stmt, "Main");
	}


	/**
	 * This method formats and prints to standard output the
	 * SQL used in a statement or prepared statement.
	 * 
	 * @param stmtStr SQL from statement
     * @param name    Name of statement
	 */
	public static void printSqlStatement(final String stmtStr,
                                         final String name)
	{
		final StringBuilder sb = new StringBuilder("\n\n");

		if (name != null)
		{
			sb.append(name).append(": ");
		}

		int ix    = stmtStr.toUpperCase().indexOf("SELECT");
		final int paren = stmtStr.indexOf('(');
		
		if (paren < ix)
        {
		    ix = paren;
		}

		sb.append((ix >= 0) ? stmtStr.substring(ix) : stmtStr).append('\n');

		System.out.println(sb);
	}


	/**
	 * This method formats and prints to standard output the
	 * toString() value of a PreparedStatement object.
	 * 
	 * @param stmt Prepared statement
     * @param name Name of statement
	 */
	public static void printSqlStatement(final PreparedStatement stmt,
                                         final String            name)
	{
        printSqlStatement(stmt.toString(), name);
	}

	/**
	 * Adds the given condition to the given where clause, and returns the
	 * new where clause
	 * @param oldWhere the existing where clause; if null, the WHERE keyword will be added first
	 * @param newClause the new condition to be ANDed with the existing where clause
	 * @return the new where clause
	 */
	public static String addToWhere(final String oldWhere, final String newClause)
    {
        final String newC = (newClause != null) ? newClause : "";
        final String oldW = (oldWhere  != null) ? oldWhere  : "";

		if (oldW.length() == 0)
        {
            if (newC.length() > 0)
            {
                return " WHERE " + newC;
            }

            return "";
		}

        if (newC.length() > 0)
        {
            return (oldW + " AND " + newC);
        }

        return oldW;
	}


	/**
	 * Adds the given condition to the given where clause, and returns the
	 * new where clause.
     *
	 * @param oldWhere  the existing where clause; if empty, the WHERE keyword will be added first
	 * @param newClause the new condition to be ANDed with the existing where clause

	 * @return the new where clause
	 */
	protected static StringBuilder addToWhere(final StringBuilder oldWhere,
                                              final String        newClause)
    {
        /** MPCS-6032 New method */

        final StringBuilder oldW = ((oldWhere != null) ? oldWhere : new StringBuilder());
        final String        newC = StringUtil.safeTrim(newClause);

        if (newC.isEmpty())
        {
            return oldW;
        }

		if (oldW.length() == 0)
        {
            oldW.append(" WHERE ");
		}
        else
        {
            oldW.append(" AND ");
        }

        return oldW.append(newC);
	}


    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.db.api.sql.fetch.IDbFetchx#abortQuery()
     */
    @Override
    public void abortQuery() {
        try {
            if (this.statement != null) {
                statement.cancel();
            }
        }
        catch (final SQLException e) {
            // e.printStackTrace();
        }

        try {
            if (plainStatement != null) {
                plainStatement.cancel();
            }
        }
        catch (final SQLException e) {
            // e.printStackTrace();
        }

        try {
            if (this.results != null) {
                this.results.close();
            }
        }
        catch (final SQLException e) {
            // e.printStackTrace();
        }

        this.results = null;
    }

    /**
     * Generate where clause segment for VCIDs. If column is nullable, NULL is
     * added as a wildcard.
     *
     * @param vcids
     *            Collection of VCIDs
     * @param abbrev
     *            Table abbreviation
     * @param nullable
     *            True if vcid column is nullable
     *
     * @return Where clause segment for the VCIDs
     */
    public static String generateVcidWhere(final Collection<Integer> vcids,
                                           final String abbrev,
                                           final boolean nullable) {
        if ((vcids == null) || vcids.isEmpty()) {
            return "";
        }

        final StringBuilder sb = new StringBuilder();
        boolean first = true;

		//	 if (nullable)
		// {
		// sb.append('(');
		// }

        sb.append('(').append(abbrev).append(".vcid IN (");

        for (final int vcid : vcids) {
            if (first) {
                first = false;
            }
            else {
                sb.append(',');
            }

            sb.append(vcid);
        }

        sb.append("))");

		// if (nullable)
		// {
		// sb.append(" OR (").append(abbrev).append(".vcid IS NULL))");
		// }

        return sb.toString();
    }

    /**
     * Generate where clause segment for DSS ids. We no longer add zero as a
     * wildcard.
     *
     * @param dssIds
     *            Collection of DSS ids
     * @param abbrev
     *            Table abbreviation
     *
     * @return Where clause segment for the DSS ids
     */
    public static String generateDssIdWhere(final Collection<Integer> dssIds,
                                            final String              abbrev)
    {
        if ((dssIds == null) || dssIds.isEmpty())
        {
            return "";
        }

        final Set<Integer> extra = new TreeSet<Integer>(dssIds);

        // Make sure we have zero, the wildcard
        // extra.add(0);

        final StringBuilder sb    = new StringBuilder();
        boolean             first = true;

        sb.append('(').append(abbrev).append(".dssId IN (");

        for (final int dssId : extra)
        {
            if (first)
            {
                first = false;
            }
            else
            {
                sb.append(',');
            }

            sb.append(dssId);
        }

        sb.append("))");

        return sb.toString();
    }


    /**
     * Generate where clause segment for APIDs
     *
     * @param apids  Collection of APIDs.
     * @param abbrev Table abbreviation
     *
     * @return Where clause segment for the APIDS
     */
    public static String generateApidWhere(final Collection<ApidHolder> apids,
                                           final String                 abbrev)
    {
        if ((apids == null) || apids.isEmpty())
        {
            return "";
        }

        final Set<ApidHolder> sorted = new TreeSet<ApidHolder>(apids);
        final StringBuilder   sb     = new StringBuilder();
        boolean               first  = true;

        sb.append('(').append(abbrev).append(".apid IN (");

        for (final ApidHolder apid : sorted)
        {
            if (first)
            {
                first = false;
            }
            else
            {
                sb.append(',');
            }

            sb.append(apid);
        }

        sb.append("))");

        return sb.toString();
    }


    /**
     * Get body and check length. Fix length if necessary.
     * Do not add headers and trailers.
     *
     * @param results         Result set
     * @param basicBodyLength Desired body length from database
     *
     * @return Body as bytes
     *
     * @throws DatabaseException On error
     */
    protected static byte[] fetchBody(final ResultSet results,
                                      final int       basicBodyLength)
        throws DatabaseException
    {
        // Do not add abbreviation
        byte[] basicBody;
        try {
            basicBody = results.getBytes("body");
        }
        catch (final SQLException e) {
            throw new DatabaseException(e.getMessage(), e);
        }

        if (basicBody == null)
        {
            trace.error("Body was NULL, needed: ", basicBodyLength,
                        ". Padded");

            return new byte[basicBodyLength];
        }

        if (basicBodyLength > basicBody.length)
        {
            trace.error("Body length too short: ", basicBodyLength, " versus ", basicBody.length,
                        ". Padded");

            return Arrays.copyOf(basicBody, basicBodyLength);
        }

        if (basicBodyLength < basicBody.length)
        {
            trace.error("Body length too long: ", basicBodyLength, " versus ", basicBody.length,
                        ". Truncated");

            return Arrays.copyOf(basicBody, basicBodyLength);
        }

        return basicBody;
    }


    /**
     * Get body and check lengths and types. Fix length if necessary. Add
     * headers and trailers as necessary.
     *
     * If types need not be checked, just set newType to null.
     *
     * This method takes the length and fetches the body, and then calls the
     * related method to do the restore. It is used when you have not yet
     * fetched the basic body and do not need to add anything to it prior to
     * adding the headers/trailers.
     * 
     * @param basicBodyLength
     *            Basic body length
     * @param newType
     *            Type of row (can be null)
     *
     * @return Body as bytes with header and trailer as necessary
     *
     * @throws DatabaseException
     *             On error
     */
    protected byte[] fetchRestoredBody(final int    basicBodyLength,
                                       final String newType)
        throws DatabaseException
    {
        return fetchRestoredBody(fetchBody(results, basicBodyLength), newType);
    }


    /**
     * Get body and check lengths and types. Fix length if necessary.
     * Add headers and trailers as necessary.
     *
     * If types need not be checked, just set newType to null.
     *
     * This method takes the basic body and then adds the headers/trailers.
     * it is used directly when you might have to add something to the body
     * from the database, such as the ASM.
     *
     * @param basicBody Unrestored body (ASMs handled already)
     * @param newType   Type of row (can be null)
     *
     * @return Body as bytes with header and trailer as necessary
     *
     * @throws DatabaseException On error
     */
    protected byte[] fetchRestoredBody(final byte[] basicBody,
                                       final String newType)
        throws DatabaseException
    {
        final int basicBodyLength = basicBody.length;

        // Get lengths of header and trailer, which might be NULL

        Integer rawHeaderLength = null;
        try {
            rawHeaderLength = HeaderHolder.getLengthFromDb(
                                                results, "headerLength", null);
        }
        catch (final SQLException e) {
           throw new DatabaseException(e.getMessage(), e);
        }
    
        Integer rawTrailerLength = null;
        try {
            rawTrailerLength = TrailerHolder.getLengthFromDb(
                                                 results, "trailerLength", null);
        }
        catch (final SQLException e) {
           throw new DatabaseException(e.getMessage(), e);
        }
        
        if (firstRow)
        {
            headerNullState  = (rawHeaderLength  == null);
            trailerNullState = (rawTrailerLength == null);
            type             = newType;
            firstRow         = false;

            if (headerNullState && trailerNullState)
            {
                // Either an upgraded row or a type that does not have headers
                // or trailers
                // MPCS-5189 Reword

                if (type != null)
                {
                    trace.error("The first row is of type ", type, " which is unsuitable for restoring headers ",
                            "because there is neither a header nor a ",
                                "trailer");
                }
                else
                {
                    trace.error("The first row is unsuitable for restoring ",
                            "headers because there is neither a header ",
                                "nor a trailer");
                }

                throw new DatabaseException("Exiting due to fatal error");
            }
        }
        else
        {
            // MPCS-5189

            final boolean newHeaderNullState  = (rawHeaderLength  == null);
            final boolean newTrailerNullState = (rawTrailerLength == null);

            final String message = getInconsistentHeaderStateString(
                                       newHeaderNullState,
                                       newTrailerNullState,
                                       headerNullState,
                                       trailerNullState,
                                       newType,
                                       type);
            if (message != null)
            {
                trace.error(message);

                throw new DatabaseException("Exiting due to fatal error");
            }

            // MPCS-48910. Frame type is not relevant. DO NOT check this.
        }

        // Get the actual headers and trailers, such as they are
        // (possibly NULL)

        /** MPCS-6718  Look for warnings */

        final List<SQLWarning> warnings = new ArrayList<SQLWarning>();

        HeaderHolder header;
        try {
            header = HeaderHolder.getFromDb(results, "header", warnings);
        }
        catch (final SQLException e) {
            throw new DatabaseException(e.getMessage(), e);
        }

        TrailerHolder trailer;
        try {
            trailer = TrailerHolder.getFromDb(results, "trailer", warnings);
        }
        catch (final SQLException e) {
            throw new DatabaseException(e.getMessage(), e);
        }

        SqlExceptionTools.logWarning(warnings, trace);
    
        final int    headerLength  = header.getLength();
        final int    trailerLength = trailer.getLength();
        final byte[] body          = new byte[headerLength    +
                                              basicBodyLength +
                                              trailerLength];
        int next = 0;

        if (headerLength > 0)
        {
            System.arraycopy(header.getValue(),
                             0,
                             body,
                             next,
                             headerLength);

            next += headerLength;
        }

        if (basicBodyLength > 0)
        {
            System.arraycopy(basicBody,
                             0,
                             body,
                             next,
                             basicBodyLength);

            next += basicBodyLength;
        }

        if (trailerLength > 0)
        {
            System.arraycopy(trailer.getValue(),
                             0,
                             body,
                             next,
                             trailerLength);
        }

        // Handle any unhandled warnings
        /** MPCS-6718  */
        SqlExceptionTools.logWarning(trace, results);

        return body;
    }


    /**
     * Check whether a station is in range, fix it up and log.
     *
     * @param station Station from database
     * @param rs      Result set
     * @param column  Column name
     *
     * @return Row value or corrected value
     *
     * @throws DatabaseException On any SQL error
     *
     * @version MPCS-7106 Added
     */
    private static int getCorrectedStationInner(final int       station,
                                                final ResultSet rs,
                                                final String    column)
        throws DatabaseException
    {
        boolean rsWasNull = false;
        try {
            rsWasNull = rs.wasNull();
        }
        catch (final SQLException e) {
            throw new DatabaseException(e.getMessage(), e);
        }
        if (rsWasNull) {
            TraceManager.getDefaultTracer().warn(

                    "Station (", column, ") was null, forcing to ",
                StationIdHolder.MIN_VALUE);

            return  StationIdHolder.MIN_VALUE;
        }

        if ((station <  StationIdHolder.MIN_VALUE) ||
            (station >  StationIdHolder.MAX_VALUE))
        {
            TraceManager.getDefaultTracer().warn(

                    "Station (", column, ") was out of range (", station, "), forcing to ",
                StationIdHolder.MIN_VALUE);

            return  StationIdHolder.MIN_VALUE;
        }

        return station;
    }


    /**
     * Check whether a station is in range, fix it up and log.
     *
     * MPCS-4839  Added
     *
     * @param rs     ResultSet to read from
     * @param column Column name
     *
     * @return Row value or corrected value
     *
     * @throws DatabaseException On any SQL error
     *
     * @version MPCS-7106  Refactored
     */
    protected static int getCorrectedStation(final ResultSet rs,
                                             final String    column)
        throws DatabaseException
    {
        try {
            return getCorrectedStationInner(rs.getInt(column), rs, column);
        }
        catch (final SQLException e) {
            throw new DatabaseException(e.getMessage(), e);
        }
    }


    /**
     * Check whether a station is in range, fix it up and log.
     *
     * MPCS-7106 Added
     *
     * @param rs     ResultSet to read from
     * @param index  Column index
     * @param column Column name
     *
     * @return Row value or corrected value
     *
     * @throws DatabaseException On any SQL error
     *
     * @version MPCS-7106 Added
     */
    protected static int getCorrectedStation(final ResultSet rs,
                                             final int       index,
                                             final String    column)
        throws DatabaseException
    {
        try {
            return getCorrectedStationInner(rs.getInt(index), rs, column);
        }
        catch (final SQLException e) {
            // TODO Auto-generated catch block
            throw new DatabaseException(e.getMessage(), e);
        }
    }


    /**
     * Build an appropriate error message string from the header and trailer
     * NULL states.
     *
     * @param newHeaderNull  True if the latest header was null
     * @param newTrailerNull True if the latest trailer was null
     * @param oldHeaderNull  True if the previous header was null
     * @param oldTrailerNull True if the previous trailer was null
     * @param newType        Latest frame type or null
     * @param type        Previous frame type or null
     *
     * @return Error message or null if there was no error detected
     */
    private static String getInconsistentHeaderStateString(
                              final boolean newHeaderNull,
                              final boolean newTrailerNull,
                              final boolean oldHeaderNull,
                              final boolean oldTrailerNull,
                              final String  newType,
                              final String  type)
    {
        final boolean headerBad  = (newHeaderNull  != oldHeaderNull);
        final boolean trailerBad = (newTrailerNull != oldTrailerNull);

        if (! headerBad && ! trailerBad)
        {
            return null;
        }

        final StringBuilder sb =
            new StringBuilder("Inconsistent header state: ");

        if (headerBad)
        {
            sb.append("there is ");

            if (newHeaderNull)
            {
                sb.append("no header where previously there were headers");
            }
            else
            {
                sb.append("a header where previously there were no headers");
            }
        }

        if (headerBad && trailerBad)
        {
            sb.append(" and ");
        }

        if (trailerBad)
        {
            sb.append("there is ");

            if (newTrailerNull)
            {
                sb.append("no trailer where previously there were trailers");
            }
            else
            {
                sb.append("a trailer where previously there were no trailers");
            }
        }

        if ((newType != null) && (type != null))
        {
            sb.append(" with new type of ").append(newType);
            sb.append(" and previous type of ").append(type);
        }

        return sb.toString();
    }


    /**
     * Look for hint flag and replace if found.
     *
     * @param text  Original text
     * @param hints Hints to insert
     *
     * @return Text modified as required
     *
     * @version MPCS-6784 New
     */
    protected static String insertIndexHints(final String text,
                                             final String hints)
    {
        final String        flag = " -- HINT\n";
        final StringBuilder sb   = new StringBuilder();
        int                 next = 0;

        while (true)
        {
            final int found = text.indexOf(flag, next);

            if (found < 0)
            {
                sb.append(text.substring(next));
                break;
            }

            sb.append(text.substring(next, found));

            if (! hints.isEmpty())
            {
                sb.append(' ').append(hints);
            }

            sb.append('\n');

            next = found + flag.length();
        }

        return sb.toString();
    }


    /**
     * Get UnsignedLong from database.
     *
     * @param rs     ResultSet
     * @param column Column name
     *
     * @return UnsignedLong or null
     *
     * @throws DatabaseException On error
     *
     * @version MPCS-7639  New method
     */
    protected static final UnsignedLong getUnsignedLong(final ResultSet rs,
                                                        final String    column)
        throws DatabaseException
    {
        BigDecimal bd;
        try {
            bd = rs.getBigDecimal(column);
        }
        catch (final SQLException e) {
            throw new DatabaseException(e.getMessage(), e);
        }

        if (bd == null)
        {
            return null;
        }

        try
        {
            return UnsignedLong.valueOf(bd.toBigIntegerExact());
        }
        catch (final ArithmeticException | NumberFormatException e)
        {
            throw new DatabaseException("Not a BIGINT UNSIGNED", e);
        }
    }

	/**
	 * Create a SQL WHERE clause with context IDs provided from command line
	 *
	 * @param contextIds List of context IDs
	 * @param tableAbbrev Table abbreviation
	 * @param useContext Whether to use context / session ID as field name
	 *
	 * @return Where clause
	 */
	protected String createContextIdClause(final List<Long> contextIds, final String tableAbbrev, final boolean useContext){
		final StringBuilder sb = new StringBuilder();
		if(contextIds == null || contextIds.isEmpty()){
			return sb.toString();
		}

		final String fieldName = useContext ? CONTEXT_ID : SESSION_ID;
		int i = 0;
		sb.append("(");
		for(final Long contextId: contextIds){
			sb.append(tableAbbrev).append(".").append(fieldName).append("=").append(contextId);
			if(i < contextIds.size() - 1){
				sb.append(" OR ");
			}
			i++;
		}

		sb.append(")");
		return sb.toString();
	}
}
