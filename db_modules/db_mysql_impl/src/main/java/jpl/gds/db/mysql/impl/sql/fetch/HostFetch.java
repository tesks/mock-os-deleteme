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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import org.springframework.context.ApplicationContext;

import jpl.gds.context.api.ISimpleContextConfiguration;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.fetch.IHostFetch;
import jpl.gds.db.api.sql.store.IHostStore;
import jpl.gds.db.api.types.IDbRecord;
import jpl.gds.db.api.types.IDbContextInfoProvider;
import jpl.gds.shared.exceptions.SqlExceptionTools;
import jpl.gds.shared.time.DatabaseTimeRange;


/**
 * Fetch class for Host table. Just used to get the hostId from the host name.
 */
public class HostFetch extends AbstractMySqlFetch implements IHostFetch
{
    private static final String DB_TABLE   = IHostStore.DB_HOST_STORE_TABLE_NAME;

    /** Table abbreviation for Host table */
    public static final String TABLE_ABBREV = "hs";

    private static final String ID     = TABLE_ABBREV + "." + HOST_ID;
    private static final String NAME   = TABLE_ABBREV + "." + "hostName";
    private static final String OFFSET = TABLE_ABBREV + "." + "hostOffset";

    private static final String SELECT_CLAUSE =
        "SELECT * FROM " + DB_TABLE + " AS " + TABLE_ABBREV;

    private static final int MAX_ORDINAL  = 0xFFFF;
    private static final int ORDINAL_MASK = 0xFFFF;
    private static final int OFFSET_SHIFT = 16;


    /**
     * Constructor.
     * 
     * @param appContext
     *            the Spring Application Context
     */
    public HostFetch(final ApplicationContext appContext)
    {
        super(appContext, false);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public int get(final String hn) throws DatabaseException
    {
        if (! dbProperties.getUseDatabase())
        {
            throw new DatabaseException("Not set up to use a database");
        }

        final String hostName = hn.toLowerCase();

        final StringBuilder sb = new StringBuilder(SELECT_CLAUSE);

        this.statement = getPreparedStatement(sb.toString(),
                                              ResultSet.TYPE_FORWARD_ONLY,
                                              ResultSet.CONCUR_READ_ONLY);
         
        try {
            this.statement.setFetchSize(Integer.MIN_VALUE);

            this.results = this.statement.executeQuery();

            int hostId = -1;
            int offset = -1;
            int ordinal = -1;
            boolean foundLocal = false;

            while (results.next())
            {
                final String nextName = results.getString(NAME);
                final int nextHostId = results.getInt(ID);
                final int nextOffset = results.getInt(OFFSET);

                if (offset >= 0) {
                    if (offset != nextOffset) {
                        throw new DatabaseException("Inconsistent hostOffset in Host");
                    }
                }
                else {
                    // First one
                    offset = nextOffset;
                }

                final int activeOffset = nextHostId >>> OFFSET_SHIFT;

                if (activeOffset != offset) {
                    continue;
                }

                // This row is a local offset row

                ordinal = Math.max(ordinal, nextHostId & ORDINAL_MASK);
                foundLocal = true;

                if (nextName.equalsIgnoreCase(hostName))
                {
                    if (hostId >= 0) {
                        throw new DatabaseException("Multiple hostId entries in Host " + "matching '" + hostName + "'");
                    }

                    hostId = nextHostId;
                }
            }

            // Handle any unhandled warnings
            /** MPCS-6718 */
            SqlExceptionTools.logWarning(trace, results);

            try {
                statement.close();
            }
            catch (final SQLException sqle) {
                statement = null;
            }
            finally {
                statement = null;

                close();
            }

            if (!foundLocal)
            {
                throw new DatabaseException("No local hostId entries in Host");
            }

            if (hostId >= 0)
            {
                // We already have an entry
                return hostId;
            }

            // Must make a new entry

            ++ordinal;

            if (ordinal > MAX_ORDINAL)
            {
                throw new DatabaseException("Too many local hostId entries in Host");
            }

            return (offset << OFFSET_SHIFT) | ordinal;
        }
        catch (final SQLException se) {
            throw new DatabaseException(se);
        }
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.sql.fetch.IHostFetch#getHostId(jpl.gds.session.config.SessionConfiguration)
     */
    @Override
    public void getHostId(final ISimpleContextConfiguration tc) throws DatabaseException{
        tc.getContextId().setHostId(get(tc.getContextId().getHost()));
    }


    /**
     * Create where clause for host name.
     *
     * @param hostName Host name
     *
     * @return Generated where clause
     */
    protected String getIdHostWhereClause(final String hostName) {
        final StringBuilder sb = new StringBuilder(1024);

        sb.append(" WHERE ").append(TABLE_ABBREV).append(".hostName = '").append(
            hostName).append('\'');

        return sb.toString();
    }


    /**
     * Not used.
     *
     * {@inheritDoc}
     */
    @Override
    public List<? extends IDbRecord> get(final IDbContextInfoProvider tsi,
            final DatabaseTimeRange range, final int batchSize, final Object... params)
            throws DatabaseException {

        throw new UnsupportedOperationException();
    }

    /**
     * Not used.
     *
     * {@inheritDoc}
     */
    @Override
    public List<? extends IDbRecord> getNextResultBatch()
            throws DatabaseException {

        throw new UnsupportedOperationException();
    }


    /**
     * Not used.
     *
     * {@inheritDoc}
     */
    @Override
    protected List<? extends IDbRecord> getResults()
            throws DatabaseException {

        throw new UnsupportedOperationException();
    }


    /**
     * Not used.
     *
     * {@inheritDoc}
     */
    @Override
    public String getSqlWhereClause(final String testSqlTemplate, final DatabaseTimeRange range,
            final Object... params) throws DatabaseException {

        throw new UnsupportedOperationException();
    }

    /**
     * Not used.
     *
     * {@inheritDoc}
     */
    @Override
    protected List<? extends IDbRecord> populateAndExecute(final IDbContextInfoProvider tsi, final DatabaseTimeRange range,
            final String whereClause, final Object... params) throws DatabaseException {
        throw new UnsupportedOperationException();
    }
}
