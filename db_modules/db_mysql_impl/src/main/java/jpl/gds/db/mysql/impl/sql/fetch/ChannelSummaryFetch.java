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

import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.fetch.IChannelSummaryFetch;
import jpl.gds.db.api.sql.fetch.IDbSessionPreFetch;
import jpl.gds.db.api.types.IDbChannelMetaDataFactory;
import jpl.gds.db.api.types.IDbChannelMetaDataProvider;
import jpl.gds.db.api.types.IDbContextInfoProvider;
import jpl.gds.db.api.types.IDbRecord;
import jpl.gds.shared.exceptions.SqlExceptionTools;
import jpl.gds.shared.sys.SystemUtilities;
import jpl.gds.shared.time.DatabaseTimeRange;
import org.springframework.context.ApplicationContext;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


/**
 * This is the database read/retrieval interface to the ChannelData table in
 * the AMPCS database. It provides a summary of all channels with values for
 * selected sessions.
 *
 * This code was heavily revised to fix the SQL for --count and SQL in general
 * because it was becoming very hard to understand and had a lot of obsolete
 * stuff. It was decided to eliminate the dependencies upon query_config.xml
 * because that just made it harder.
 *
 * There are four basic SQL queries:
 *
 * --count
 * --count --stemOnly
 * --stemOnly
 * Neither
 *
 * All four of those can have --stem, but that just goes into the WHERE clause
 * and doesn't alter the basic SQL.
 *
 * --stemOnly:
 *
 * SELECT DISTINCT SUBSTRING_INDEX(cd.channelId,'-',1) AS channelStem
 * FROM ChannelData AS cd
 * WHERE ((cd.hostId=65536) AND (cd.sessionId=1820)) AND
 *       (SUBSTRING_INDEX(cd.channelId,'-',1)='T')
 * ORDER BY channelStem
 *
 * Neither:
 *
 * SELECT cd.channelId,
 *        SUBSTRING_INDEX(cd.channelId,'-',1) AS channelStem,
 *        cd.name
 * FROM ChannelData AS cd
 * WHERE ((cd.hostId=65536) AND (cd.sessionId=1820)) AND
 *       (SUBSTRING_INDEX(cd.channelId,'-',1)='T')
 * ORDER BY channelId
 *
 *
 * NB: UNION ALL and UNION will be equivalent since we expect
 * that channel-ids (and even stems) do not occur in more than
 * one channel value table. UNION ALL eliminates bothering to look
 * for duplicates.
 *
 * If there are duplicates (more likely for stem) there will be
 * more than one row for each one.
 *
 * NB: See getGroupBy for explanation of the odd order-bys.
 *
 * NB: If the query returns more than one session, there is the
 * possibility that the channel-id name could have been changed
 * for one or more channel-ids. If that is the case the name
 * returned will be any one of the possible names.
 *
 * NB: Most of the options supported by this application refer
 * to the session/host.
 *
 * MPCS-10478 : chill_get_chan_summary --count not working with EHA aggregation
 * With the conversion of our legacy ChannelValue tables to ChannelAggregate tables, the
 * channel sample count code no longer worked as the BLOB structures had to be unpacked.
 *
 * All logic and SQL code related to --count has been taken out of this class. The --count
 * option is handled by calling 'chill_get_chanvals'. For more detail
 * @see jpl.gds.db.app.ChannelSummaryFetchApp
 *
 */
public final class ChannelSummaryFetch extends AbstractMySqlFetch implements IChannelSummaryFetch
{
    /**
     * MPCS-6032 rewrite of SQL for --count option
     * and general refactor. Most attributes removed.
     */

	/** This gets the stem from the channel id */
	private static final String	STEM		= "SUBSTRING_INDEX(cd.channelId,'-',1)";

	private static final String	STEM_AS		= STEM + " AS channelStem";

	private static final char	COMMA		= ',';
	private static final char	OPEN		= '(';
	private static final char	CLOSE		= ')';
	private static final char	SQ			= '\'';
	private static final char	EQUAL		= '=';

	private IDbSessionPreFetch	spf			= null;
	private boolean				stemOnly	= false;

    private final IDbChannelMetaDataFactory dbChannelMetaDataFactory;

    /**
     * Constructor
     *
     * @param appContext
     *            the Spring Application Context
     * @param printSqlStmt
     *            if true, ONLY prints SQL statement that would be
     *            executed, otherwise executes query without printing SQL
     */
    public ChannelSummaryFetch(final ApplicationContext appContext, final boolean printSqlStmt)
    {
        super(appContext, printSqlStmt);
        dbChannelMetaDataFactory = appContext.getBean(IDbChannelMetaDataFactory.class);
    }


    /**
     * Constructor
     * 
     * @param appContext
     *            the Spring Application Context
     */
    public ChannelSummaryFetch(final ApplicationContext appContext)
    {
        this(appContext, false);
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.sql.fetch.IChannelSummaryFetch#get(jpl.gds.db.api.types.IDbContextInfoProvider, jpl.gds.shared.time.DatabaseTimeRange, int, java.lang.Object)
     */
    @Override
    public List<? extends IDbRecord> get(final IDbContextInfoProvider tsi, final DatabaseTimeRange range, final int batchSize,
            final Object... params) throws DatabaseException {

        if (params.length >= 2) {
            final Object stemOnly = params[1];
            if (stemOnly instanceof Boolean) {
                this.stemOnly = (Boolean) stemOnly;
            }
        }

        this.batchSize = batchSize;

        if (printStmtOnly)
        {
            // This prints the pre-fetch SQL
            fetchFactory.getSessionPreFetch(true).get(tsi);
        }

        String whereClause = null;

        try
        {
            // Must do this even with printStmtOnly
            // so that the main query gets the sessions/hosts

            spf = fetchFactory.getSessionPreFetch(false);

            spf.get(tsi);

            whereClause = spf.getIdHostWhereClause("cd");
        }
        finally
        {
            // Close connection, still usable

            if (spf != null)
            {
                spf.close();
            }
        }

        return populateAndExecute(tsi, range, whereClause, params);
    }


    /* (non-Javadoc)
     * @see jpl.gds.db.api.sql.fetch.IChannelSummaryFetch#getNextResultBatch()
     */
    @Override
    public List<IDbChannelMetaDataProvider> getNextResultBatch()
            throws DatabaseException {
        return (getResults());
    }


    /**
     * {@inheritDoc}
     */
    @Override
    protected List<IDbChannelMetaDataProvider> getResults() throws DatabaseException {
        final List<IDbChannelMetaDataProvider> refs = new ArrayList<IDbChannelMetaDataProvider>();

        if (this.results == null) {
            return (refs);
        }

        int batchCount = 0;
        try {
            // loop through until we fill up our first batch or we've
            // got no more results
            while (batchCount < this.batchSize) {
                if (this.results.next() == false) {
                    break;
                }

                String stem = null;
                String chanId = null;
                String chanName = null;
                int count = -1;

                if (this.stemOnly) {
                    stem = this.results.getString("channelStem");
                } else {
                    chanId = this.results.getString("channelId");
                    chanName = this.results.getString("name");
                }

                refs.add(dbChannelMetaDataFactory.createQueryableProvider(stem, chanId, chanName, count));

                batchCount++;

                // Handle any unhandled warnings
                /** MPCS-6718 */
                SqlExceptionTools.logWarning(trace, results);
            }

            // if we're all done with results, clean up
            // all the resources
            if (this.results.isAfterLast() == true) {
                this.results.close();
                this.statement.close();

                this.results = null;
                this.statement = null;
            }

        } catch (final SQLException e) {
            throw new DatabaseException("Error retrieving channel meta data from database: "
                    + e.getMessage(), e);
        }

        return refs;
    }


    /**
     * Get where clause possibly with stem and stemOnly but not count.
     *
     * @param testSqlTemplate Initial where clause
     * @param stem            Stem or null
     *
     * @return Where clause
     */
    private static String getBasicWhereClause(final String testSqlTemplate,
                                              final String stem)
    {
        final StringBuilder whereClause = new StringBuilder();

        addToWhere(whereClause, testSqlTemplate);

        if (stem != null)
        {
            addToWhere(whereClause, whereStem(stem));
        }

        return whereClause.toString();
    }


    /** MPCS-6032  Removed getCountSqlWhereClause */

    /** MPCS-6032  Removed getCountOuterWhereClause */

    /** MPCS-10478 - Removed */


    /**
     * {@inheritDoc}
     */
    @Override
    protected List<? extends IDbRecord> populateAndExecute(final IDbContextInfoProvider tsi, final DatabaseTimeRange range,
            final String whereClause, final Object... params) throws DatabaseException {
        final String  stem     = ((params.length > 0) ? (String)  params[0] : null);
        final boolean stemOnly = ((params.length > 1) ? (Boolean) params[1] : Boolean.FALSE);

        final Set<Integer> vcids = SystemUtilities.<Set<Integer>> castNoWarning(
            (params.length > 3) ? params[3] : null);

        final Set<Integer> dssIds = SystemUtilities.<Set<Integer>> castNoWarning(
            (params.length > 4) ? params[4] : null);

        final StringBuilder sqlClause = new StringBuilder();


        // Without -count we just query ChannelData

        sqlClause.append("SELECT ");

        // Select list

        if (stemOnly) {
            sqlClause.append("DISTINCT ").append(STEM_AS);
        } else {
            /* MPCS-7533  Added DISTINCT to make channelId unique to avoid duplicate in CSV output. */
            sqlClause.append("DISTINCT ");
            sqlClause.append("cd.channelId");
            sqlClause.append(COMMA);
            sqlClause.append(STEM_AS);
            sqlClause.append(COMMA);
            sqlClause.append("cd.name");
        }

        sqlClause.append(" FROM ChannelData AS cd");
        sqlClause.append(
                getBasicWhereClause(spf.getIdHostWhereClause("cd"), stem));


        sqlClause.append(getOrderBy(stemOnly));

        statement = getPreparedStatement(sqlClause.toString(), ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
        
        if (printStmtOnly)
        {
            printSqlStatement(statement);
        }
        else
        {
            try {
                results = statement.executeQuery();
            }
            catch (final SQLException e) {
                throw new DatabaseException(e.getMessage(), e);
            }
        }

        return getResults();
    }


    /**
     * Build where clause for stem match.
     *
     * @param stem Stem
     *
     * @return Clause
     */
    private static String whereStem(final String stem)
    {
        final StringBuilder sb = new StringBuilder();

        sb.append(OPEN);
        sb.append(STEM).append(EQUAL).append(SQ).append(stem).append(SQ);
        sb.append(CLOSE);

        return sb.toString();
    }


    /**
     * Build order-by for use at the outer level.
     *
     * @param stemOnly True if stem-only is desired
     *
     * @return Order-by clause
     */
    private static String getOrderBy(final boolean stemOnly)
    {
        final StringBuilder sb = new StringBuilder();

        sb.append(" ORDER BY ");

        if (stemOnly)
        {
            // Order by synthetic column

            sb.append("channelStem");
        }
        else
        {
            sb.append("channelId");
        }

        return sb.toString();
    }


    /**
     * {@inheritDoc}
     */
    @Override
	public String getSqlWhereClause(final String              testSqlTemplate,
                                       final DatabaseTimeRange   range,
                                       final Object...           params)
        throws DatabaseException
    {
        throw new DatabaseException("Unsupported method getSqlWhereClause");
    }
}
