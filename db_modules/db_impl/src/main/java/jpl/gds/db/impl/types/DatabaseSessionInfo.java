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
package jpl.gds.db.impl.types;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressWarnings;
import jpl.gds.common.config.types.DownlinkStreamType;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.fetch.ISessionFetch;
import jpl.gds.db.api.types.IDbSessionInfoUpdater;
import jpl.gds.shared.holders.SessionFragmentHolder;
import jpl.gds.shared.time.DbTimeUtility;
import jpl.gds.shared.time.IAccurateDateTime;


/**
 * When querying any information out of the database, that information must be able to be associated with
 * a Session. All the database tables have a referential link to the Session table. Therefore,
 * all of the query applications require an instance of this object as an input to their query functions.
 *
 * This object contains all the information necessary to identify one or more Contexts (Sessions). It can also
 * generate templated SQL as well as fill in templated SQL so that any query application does not need to
 * worry about handling the join between its target table and the test session table.
 *
 */
public class DatabaseSessionInfo extends DatabaseContextInfo implements IDbSessionInfoUpdater
{
    /**
     * The lower bound for the start of test time
     */
    private IAccurateDateTime startTimeLowerBound;

    /**
     * The upper bound for the start of test time
     */
    private IAccurateDateTime startTimeUpperBound;

    /**
     * The flight software version string
     * (pattern implies this value is used in the SQL "LIKE"
     * statement and so it may contain wildcards such as "%")
     */
    private List<String> fswVersionPatternList;

    /**
     * The SSE version string
     * (pattern implies this value is used in the SQL "LIKE"
     * statement and so it may contain wildcards such as "%")
     */
    private List<String> sseVersionPatternList;

    /**
     * The downlink stream ID (only applicable to the testbed)
     */
    private List<String> downlinkStreamIdList;

    /**
     * The pattern for the description of the test session
     * (pattern implies this value is used in the SQL "LIKE"
     * statement and so it may contain wildcards such as "%")
     */
    private List<String> descriptionPatternList;

    /**
     * Session fragment to filter for
     */
    protected SessionFragmentHolder sessionFragment;

    /**
     * Creates an instance of DatabaseSessionInfo.
     */
    public DatabaseSessionInfo()
    {
        super();
        init();
    }

    /**
     *
     * Creates an instance of DatabaseSessionInfo.
     * 
     * @param contextConfig
     *            the Context Configuration to install
     */
    public DatabaseSessionInfo(final IContextConfiguration contextConfig)
    {
        super(contextConfig);
        init();

        this.startTimeLowerBound = contextConfig.getContextId().getStartTime();
        this.startTimeUpperBound = contextConfig.getContextId().getEndTime();

        if(contextConfig.getDictionaryConfig().getFswVersion() != null)
        {
            this.fswVersionPatternList.add(contextConfig.getDictionaryConfig().getFswVersion());
        }

        if(contextConfig.getDictionaryConfig().getSseVersion() != null)
        {
            this.sseVersionPatternList.add(contextConfig.getDictionaryConfig().getSseVersion());
        }

        // MPCS-4819 Remove test for UNKNOWN

        this.downlinkStreamIdList.add(
            DownlinkStreamType.convert(contextConfig.getVenueConfiguration().getDownlinkStreamId()));

        if(contextConfig.getContextId().getDescription() != null)
        {
            this.descriptionPatternList.add(contextConfig.getContextId().getDescription());
        }
    }

    private void init(){
        this.startTimeLowerBound = null;
        this.startTimeUpperBound = null;
        this.fswVersionPatternList = new ArrayList<>(16);
        this.sseVersionPatternList = new ArrayList<>(16);
        this.downlinkStreamIdList = new ArrayList<>(16);
        this.descriptionPatternList = new ArrayList<>(16);
    }

    /**
     * Check that at least one piece of information in this
     * object has been given a non-null value
     *
     * @return True if at least one test-related value in this
     * class is non-null, false otherwise
     */
    @Override
	public boolean isSearchCriteriaSet()
    {
        return super.isSearchCriteriaSet() ||
                this.startTimeLowerBound           != null ||
                this.startTimeUpperBound           != null ||
               !this.fswVersionPatternList.isEmpty()      ||
               !this.sseVersionPatternList.isEmpty()      ||
               !this.downlinkStreamIdList.isEmpty()       ||
               !this.descriptionPatternList.isEmpty();
    }
    
    /**
     * Check that at least one piece of information in this
     * object has been given a non-null value
     *
     * @return True if at least one test-related value in this
     * class is non-null, false otherwise
     */
    @Override
	public boolean isIdHostSearchCriteriaOnlySet()
    {
        return super.isIdHostSearchCriteriaOnlySet() &&
                this.startTimeLowerBound == null &&
                this.startTimeUpperBound == null &&
                this.fswVersionPatternList.isEmpty() &&
                this.sseVersionPatternList.isEmpty() &&
                this.downlinkStreamIdList.isEmpty() &&
                this.descriptionPatternList.isEmpty();
    }


    /**
     * This function generates a templated string of SQL that can be used in the
     * WHERE clause of a prepared SQL statement.  The word "templated" implies
     * that ? characters have been inserted in place of actual values.
     *
     * <span style="font-weight:bold">IMPORTANT</span>: Once this function is
     * called, do not make any changes to this class before calling
     * "fillInSqlTemplate" or unpredictable behavior may occur.
     *
     * @param tablePrefix Table abbreviation
     *
     * @return The templated SQL string for use in an SQL WHERE clause.
     *         There are no leading or trailing SQL connector keywords
     *         (no preceding or trailing ANDs/ORs)
     */
    @Override
	public String getSqlTemplate(final String tablePrefix)
    {
    	final boolean idOnlyQuery = this.isIdHostSearchCriteriaOnlySet();

        final StringBuilder keySql = new StringBuilder(256);

        if(!this.keyList.isEmpty())
        {
        	if (idOnlyQuery)
            {
                keySql.append(tablePrefix + ".sessionId IN (?");
        	}
            else
            {
        		keySql.append(ISessionFetch.DB_SESSION_TABLE_ABBREV + ".sessionId IN (?");
        	}

            for(int i=1; i < this.keyList.size(); i++)
            {
                keySql.append(",?");
            }
            keySql.append(")");
        }

        final StringBuilder paramSql = new StringBuilder(512);

        if(this.startTimeLowerBound != null)
        {
            if(paramSql.length() != 0)
            {
                paramSql.append(" AND ");
            }

            addLowerBoundTime(paramSql);
        }

        if(this.startTimeUpperBound != null)
        {
            if(paramSql.length() != 0)
            {
                paramSql.append(" AND ");
            }

            addUpperBoundTime(paramSql);
        }

        if(!this.fswVersionPatternList.isEmpty())
        {
            if(paramSql.length() != 0)
            {
                paramSql.append(" AND ");
            }

            paramSql.append("(");
            paramSql.append(ISessionFetch.DB_SESSION_TABLE_ABBREV + ".fswVersion LIKE ?");
            for(int i=1; i < this.fswVersionPatternList.size(); i++)
            {
                paramSql.append(" OR " + ISessionFetch.DB_SESSION_TABLE_ABBREV + ".fswVersion LIKE ?");
            }
            paramSql.append(")");
        }

        if(!this.sseVersionPatternList.isEmpty())
        {
            if(paramSql.length() != 0)
            {
                paramSql.append(" AND ");
            }

            paramSql.append("(");
            paramSql.append(ISessionFetch.DB_SESSION_TABLE_ABBREV + ".sseVersion LIKE ?");
            for(int i=1; i < this.sseVersionPatternList.size(); i++)
            {
                paramSql.append(" OR " + ISessionFetch.DB_SESSION_TABLE_ABBREV + ".sseVersion LIKE ?");
            }
            paramSql.append(")");
        }

        if(!this.userPatternList.isEmpty())
        {
            if(paramSql.length() != 0)
            {
                paramSql.append(" AND ");
            }

            paramSql.append("(");
            paramSql.append(ISessionFetch.DB_SESSION_TABLE_ABBREV + ".user LIKE ?");
            for(int i=1; i < this.userPatternList.size(); i++)
            {
                paramSql.append(" OR " + ISessionFetch.DB_SESSION_TABLE_ABBREV + ".user LIKE ?");
            }
            paramSql.append(")");
        }

        if (!this.hostPatternList.isEmpty())
        {
            if (paramSql.length() != 0)
            {
                paramSql.append(" AND ");
            }

            paramSql.append("(");

            paramSql.append(tablePrefix + ".host LIKE ?");

            for(int i=1; i < this.hostPatternList.size(); i++)
            {
                paramSql.append(" OR " + tablePrefix + ".host LIKE ?");
            }

            paramSql.append(")");
        }

        if(!this.downlinkStreamIdList.isEmpty())
        {
            if(paramSql.length() != 0)
            {
                paramSql.append(" AND ");
            }

            paramSql.append(ISessionFetch.DB_SESSION_TABLE_ABBREV + ".downlinkStreamId IN (?");
            for(int i=1; i < this.downlinkStreamIdList.size(); i++)
            {
                paramSql.append(",?");
            }
            paramSql.append(")");
        }

        addPatternsWithIfnull(paramSql,
                              this.descriptionPatternList,
                              "description", ISessionFetch.DB_SESSION_TABLE_ABBREV);

        addPatternsWithIfnull(paramSql, this.typePatternList, "type", ISessionFetch.DB_SESSION_TABLE_ABBREV);

        if(!this.namePatternList.isEmpty())
        {
            if(paramSql.length() != 0)
            {
                paramSql.append(" AND ");
            }

            paramSql.append("(");
            paramSql.append(ISessionFetch.DB_SESSION_TABLE_ABBREV + ".name LIKE ?");
            for(int i=1; i < this.namePatternList.size(); i++)
            {
                paramSql.append(" OR " + ISessionFetch.DB_SESSION_TABLE_ABBREV + ".name LIKE ?");
            }
            paramSql.append(")");
        }

        final StringBuilder finalSql = new StringBuilder(1024);
        if(keySql.length() != 0 && paramSql.length() != 0)
        {
            finalSql.append("((");
            finalSql.append(keySql.toString());
            finalSql.append(") AND (");
            finalSql.append(paramSql.toString());
            finalSql.append("))");
        }
        else if(keySql.length() != 0)
        {
            finalSql.append(keySql);
        }
        else if(paramSql.length() != 0)
        {
            finalSql.append(paramSql);
        }
        else
        {
            return("");
        }

        return(finalSql.toString());
    }


    /**
     * <span style="font-weight:bold">IMPORTANT</span>: This function should
     * only be called if no changes have been made to this object since the
     * getSqlTemplate() method was called. Otherwise unpredictable behavior may
     * occur.
     *
     * The input index value should be pointing at the first spot in the
     * prepared statement that should be filled in by this method (the first ?
     * that was in the string returned from the getSqlTemplate() method)
     *
     * Early_start is set to force the start-time (if specified) to be zero.
     * This is needed for channel-value-change processing when all values are
     * required.
     *
     * @param index       Index
     * @param statement   Prepared statement
     * @param earlyStart Early-start status
     *
     * @return Updated index value
     *
     * @throws DatabaseException If the statement cannot be filled in properly
     */
    @Override
	public int fillInSqlTemplate(final int               index,
                                 final PreparedStatement statement,
                                 final boolean           earlyStart)
        throws DatabaseException
    {
        try {
            int i = index;

            // if we have a test key, that should be the only parameter
            if (!this.keyList.isEmpty()) {
                for(Long key: keyList){
                    statement.setLong(i++, key);
                }
            }

            if (startTimeLowerBound != null) {
                i = setTimeBounds(statement, i, earlyStart ? 0L : startTimeLowerBound.getTime());
            }

            if (startTimeUpperBound != null)
            {
                i = setTimeBounds(statement, i, earlyStart ? 0L : startTimeUpperBound.getTime());
            }

            if (!this.fswVersionPatternList.isEmpty()) {
                for(String host: fswVersionPatternList){
                    statement.setString(i++, host);
                }
            }

            if (!this.sseVersionPatternList.isEmpty()){
                for(String host: sseVersionPatternList){
                    statement.setString(i++, host);
                }
            }

            if (!this.userPatternList.isEmpty()){
                for(String host: userPatternList){
                    statement.setString(i++, host);
                }
            }

            if (!this.hostPatternList.isEmpty()){
                for(String host: hostPatternList){
                    statement.setString(i++, host);
                }
            }

            if (!this.downlinkStreamIdList.isEmpty()){
                for(String host: downlinkStreamIdList){
                    statement.setString(i++, host);
                }
            }

            if (!this.descriptionPatternList.isEmpty()){
                for(String host: descriptionPatternList){
                    statement.setString(i++, host);
                }
            }

            if (!this.typePatternList.isEmpty()){
                for(String type: typePatternList){
                    statement.setString(i++, type);
                }
            }

            if (!this.namePatternList.isEmpty()){
                for(String type: namePatternList){
                    statement.setString(i++, type);
                }
            }

            return (i);
        }
        catch (final SQLException se) {
            throw new DatabaseException(se);
        }
    }


    /**
     * See three-argument form. Here we just set early_start to false.
     *
     * @param index     Index
     * @param statement Prepared statement
     *
     * @return Updated value of index
     *
     * @throws DatabaseException SQL exception
     */
    @Override
	public int fillInSqlTemplate(final int               index,
                                 final PreparedStatement statement)
        throws DatabaseException
    {
        return fillInSqlTemplate(index, statement, false);
    }


    /**
     * Get description pattern list.
     *
     * @return Returns the descriptionPatternList.
     */
    @Override
	public List<String> getDescriptionPatternList()
    {
        return this.descriptionPatternList;
    }

    /**
     * Add a description pattern
     *
     * @param descPattern Description pattern
     */
    @Override
	public void addDescriptionPattern(final String descPattern)
    {
        if(descPattern == null)
        {
            throw new IllegalArgumentException("The input description pattern cannot be null");
        }

        if(!this.descriptionPatternList.contains(descPattern))
        {
            this.descriptionPatternList.add(descPattern);
        }
    }

    /**
     * Sets the descriptionPatternList
     *
     * @param descriptionPatternList The descriptionPatternList to set.
     */
    @Override
	public void setDescriptionPatternList(final List<String> descriptionPatternList)
    {
        if(descriptionPatternList == null)
        {
            throw new IllegalArgumentException("The list of description patterns may not be null.");
        }

        this.descriptionPatternList = descriptionPatternList;
    }

    /**
     * Get downlink stream id list.
     *
     * @return Returns the downlinkStreamIdList.
     */
    @Override
	public List<String> getDownlinkStreamIdList()
    {
        return this.downlinkStreamIdList;
    }

    /**
     * Add a downlink stream ID
     *
     * @param downlinkStreamId Downlink stream id
     */
    @Override
	public void addDownlinkStreamId(final String downlinkStreamId)
    {
        if(downlinkStreamId == null)
        {
            throw new IllegalArgumentException("The input downlink stream ID cannot be null");
        }

        if (! downlinkStreamId.equals("UNKNOWN") && ! this.downlinkStreamIdList.contains(downlinkStreamId))
        {
            this.downlinkStreamIdList.add(downlinkStreamId);
        }
    }

    /**
     * Sets the downlinkStreamIdList
     *
     * @param downlinkStreamIdList The downlinkStreamIdList to set.
     */
    @Override
	public void setDownlinkStreamIdList(final List<String> downlinkStreamIdList)
    {
        if(downlinkStreamIdList == null)
        {
            throw new IllegalArgumentException("The list of downlink stream IDs cannot be null.");
        }

        this.downlinkStreamIdList = downlinkStreamIdList;
    }

    /**
     * Get FSW version pattern list.
     *
     * @return Returns the fswVersionPatternList.
     */
    @Override
	public List<String> getFswVersionPatternList()
    {
        return this.fswVersionPatternList;
    }

    /**
     * Add an FSW version pattern
     *
     * @param fswVersionPattern FSW version pattern
     */
    @Override
	public void addFswVersionPattern(final String fswVersionPattern)
    {
        if(fswVersionPattern == null)
        {
            throw new IllegalArgumentException("The input FSW version pattern cannot be null");
        }

        if(!this.fswVersionPatternList.contains(fswVersionPattern))
        {
            this.fswVersionPatternList.add(fswVersionPattern);
        }
    }

    /**
     * Sets the fswVersionPatternList
     *
     * @param fswVersionPatternList The fswVersionPatternList to set.
     */
    @Override
	public void setFswVersionPatternList(final List<String> fswVersionPatternList)
    {
        if(fswVersionPatternList == null)
        {
            throw new IllegalArgumentException("The list of FSW versions cannot be null");
        }

        this.fswVersionPatternList = fswVersionPatternList;
    }



    /**
     * Get SSE version pattern list.
     *
     * @return Returns the sseVersionPatternList.
     */
    @Override
	public List<String> getSseVersionPatternList()
    {
        return this.sseVersionPatternList;
    }

    /**
     * Add an SSE version pattern
     *
     * @param sseVersionPattern SSE version pattern
     */
    @Override
	public void addSseVersionPattern(final String sseVersionPattern)
    {
        if(sseVersionPattern == null)
        {
            throw new IllegalArgumentException("The input SSE version pattern cannot be null");
        }

        if(!this.sseVersionPatternList.contains(sseVersionPattern))
        {
            this.sseVersionPatternList.add(sseVersionPattern);
        }
    }

    /**
     * Sets the sseVersionPatternList
     *
     * @param sseVersionPatternList The sseVersionPatternList to set.
     */
    @Override
	public void setSseVersionPatternList(final List<String> sseVersionPatternList)
    {
        if(sseVersionPatternList == null)
        {
            throw new IllegalArgumentException("The list of SSE version patterns may not be null.");
        }

        this.sseVersionPatternList = sseVersionPatternList;
    }

    /**
     * Get test key list.
     *
     * @return Returns the testKeyList.
     */
    @Override
	public List<Long> getSessionKeyList()
    {
        return this.keyList;
    }

    /**
     * Add a test key
     *
     * @param testKey Test key
     */
    @Override
	public void addSessionKey(final Long testKey)
    {
        if(testKey == null)
        {
            throw new IllegalArgumentException("The input test key cannot be null");
        }

        if(!this.keyList.contains(testKey))
        {
            this.keyList.add(testKey);
        }
    }


    /**
     * Set a test key
     *
     * @param testKey Test key
     */
    @Override
	public void setSessionKey(final Long testKey)
    {
        if (testKey == null)
        {
            throw new IllegalArgumentException("The input test key cannot be null");
        }

        keyList.clear();
        keyList.add(testKey);
    }


    /**
     * Add a test key range
     *
     * @param testKeyStart Test key start
     * @param testKeyEnd   Test key end
     */
    @Override
	public void addSessionKeyRange(final Long testKeyStart,
                                   final Long testKeyEnd)
    {
        if(testKeyStart == null || testKeyEnd == null)
        {
            throw new IllegalArgumentException("The input test key cannot be null");
        }
        for (long l = testKeyStart; l <= testKeyEnd; l++)
        {
            addSessionKey(l);
        }
    }

    /**
     * Sets the testKeyList
     *
     * @param testKeyList The testKeyList to set.
     */
    @Override
	public void setSessionKeyList(final List<Long> testKeyList)
    {
        if(testKeyList == null)
        {
            throw new IllegalArgumentException("The list of test keys is not allowed to be null.");
        }

        this.keyList = testKeyList;
    }

    /**
     * Get test start time lower-bound.
     *
     * @return Returns the testStartTimeLowerBound.
     */
    @Override
	@SuppressWarnings("EI_EXPOSE_REP")
    public IAccurateDateTime getStartTimeLowerBound()
    {
        return this.startTimeLowerBound;
    }

    /**
     * Sets the testStartTimeLowerBound
     *
     * @param testStartTimeLowerBound The testStartTimeLowerBound to set.
     */
    @Override
	@SuppressWarnings("EI_EXPOSE_REP2")
    public void setStartTimeLowerBound(final IAccurateDateTime testStartTimeLowerBound)
    {
        this.startTimeLowerBound = testStartTimeLowerBound;
    }

    /**
     * Get start time upper-bound.
     *
     * @return Returns the testStartTimeUpperBound.
     */
    @Override
	@SuppressWarnings("EI_EXPOSE_REP")
    public IAccurateDateTime getStartTimeUpperBound()
    {
        return this.startTimeUpperBound;
    }

    /**
     * Sets the testStartTimeUpperBound
     *
     * @param startTimeUpperBound The testStartTimeUpperBound to set.
     */
    @Override
	@SuppressWarnings("EI_EXPOSE_REP2")
    public void setStartTimeUpperBound(final IAccurateDateTime startTimeUpperBound)
    {
        this.startTimeUpperBound = startTimeUpperBound;
    }

    /**
     * Get type pattern list.
     *
     * @return Returns the typePatternList.
     */
    @Override
	public List<String> getTypePatternList()
    {
        return this.typePatternList;
    }

    /**
     * Add a type pattern
     *
     * @param typePattern Type pattern
     */
    @Override
	public void addTypePattern(final String typePattern)
    {
        if(typePattern == null)
        {
            throw new IllegalArgumentException("The input type pattern cannot be null");
        }

        if(!this.typePatternList.contains(typePattern))
        {
            this.typePatternList.add(typePattern);
        }
    }

    /**
     * Sets the typePatternList
     *
     * @param typePatternList The typePatternList to set.
     */
    @Override
	public void setTypePatternList(final List<String> typePatternList)
    {
        if(typePatternList == null)
        {
            throw new IllegalArgumentException("The list of type patterns cannot be null.");
        }

        this.typePatternList = typePatternList;
    }


    /**
     * Get user pattern list.
     *
     * @return Returns the userPatternList.
     */
    @Override
	public List<String> getUserPatternList()
    {
        return this.userPatternList;
    }

    /**
     * Add a user pattern
     *
     * @param userPattern User pattern
     */
    @Override
	public void addUserPattern(final String userPattern)
    {
        if(userPattern == null)
        {
            throw new IllegalArgumentException("The input user pattern cannot be null");
        }

        if(!this.userPatternList.contains(userPattern))
        {
            this.userPatternList.add(userPattern);
        }
    }


    /**
     * Get host pattern list.
     *
     * @return Returns the hostPatternList.
     */
    @Override
	public List<String> getHostPatternList()
    {
        return this.hostPatternList;
    }


    /**
     * Try to get the host, and return a default if not possible
     *
     * @param dsi    Database session info
     * @param defalt Default value
     *
     * @return Returns the best guess at the host
     */
    public static String getHostPattern(final DatabaseSessionInfo dsi,
                                        final String              defalt)
    {
        if ((dsi == null) || dsi.hostPatternList.isEmpty())
        {
            return defalt;
        }

        final String host = dsi.hostPatternList.get(0);

        if ((host == null) || (host.trim().length() == 0))
        {
            return defalt;
        }

        return host;
    }


    /**
     * Add a host pattern
     *
     * @param hostPattern Host pattern
     */
    @Override
	public void addHostPattern(final String hostPattern)
    {
        if (hostPattern == null)
        {
            throw new IllegalArgumentException(
                          "The input host pattern cannot be null");
        }

        if (!this.hostPatternList.contains(hostPattern))
        {
            this.hostPatternList.add(hostPattern);
        }
    }


    /**
     * Set a host pattern
     *
     * @param hostPattern Host pattern
     */
    @Override
	public void setHostPattern(final String hostPattern)
    {
        if (hostPattern == null)
        {
            throw new IllegalArgumentException(
                          "The input host pattern cannot be null");
        }

        hostPatternList.clear();

        hostPatternList.add(hostPattern);
    }


    /**
     * Sets the hostPatternList
     *
     * @param hostPatternList The hostPatternList to set.
     */
    @Override
	public void setHostPatternList(final List<String> hostPatternList)
    {
        if(hostPatternList == null)
        {
            throw new IllegalArgumentException("The list of host patterns cannot be null.");
        }

        this.hostPatternList = hostPatternList;
    }


    /**
     * Try to get the key, and return a default if not possible
     *
     * @param dsi    Database session info
     * @param defalt Default session key
     *
     * @return Returns the best guess at the session key
     */
    public static long getSessionKey(final DatabaseSessionInfo dsi,
                                     final long                defalt)
    {
        if ((dsi == null) || dsi.keyList.isEmpty())
        {
            return defalt;
        }

        final Long key = dsi.keyList.get(0);

        if ((key == null) || (key < 0L))
        {
            return defalt;
        }

        return key;
    }

    @Override
    public SessionFragmentHolder getSessionFragment() {
        return sessionFragment;
    }

    @Override
    public void setSessionFragment(final SessionFragmentHolder fragment) {
        this.sessionFragment = fragment;
    }

    /**
     * Add lower-bound time expression to where-clause-in-progress
     *
     * @param sb To append to
     */
    private static void addLowerBoundTime(final StringBuilder sb)
    {
        addTime(sb, true);
    }


    /**
     * Add lower-bound time expression to where-clause-in-progress
     *
     * @param sb To append to
     */
    private static void addUpperBoundTime(final StringBuilder sb)
    {
        addTime(sb, false);
    }


    /**
     * Add upper- or lower-bound time expression to where-clause-in-progress
     *
     * @param sb         To append to
     * @param lowerBound True if lower-bound else upper-bound
     */
    private static void addTime(final StringBuilder sb,
                                final boolean       lowerBound)
    {
        final char op = (lowerBound ? '>' : '<');

        sb.append('(');

        sb.append('(');
        sb.append(ISessionFetch.DB_SESSION_TABLE_ABBREV).append(".startTimeCoarse");
        sb.append(' ').append(op).append(" ?");
        sb.append(')');

        sb.append(" OR ");

        sb.append('(');

            sb.append('(');
            sb.append(ISessionFetch.DB_SESSION_TABLE_ABBREV).append(".startTimeCoarse");
            sb.append(" = ?");
            sb.append(')');

            sb.append(" AND ");

            sb.append('(');
            sb.append(ISessionFetch.DB_SESSION_TABLE_ABBREV).append(".startTimeFine");
            sb.append(' ').append(op).append("= ?");
            sb.append(')');

        sb.append(')');

        sb.append(')');
    }


    /**
     * Set time bounds parameters to match addTime.
     *
     * @param statement  Statement in which to set parameters
     * @param startIndex Current parameter index
     * @param time       Time bound
     *
     * @return New value of index
     *
     * @throws DatabaseException
     */
    private static int setTimeBounds(final PreparedStatement statement,
                                     final int               startIndex,
                                     final long              time)
        throws DatabaseException
    {
        int        index  = startIndex;
        final long coarse = DbTimeUtility.coarseFromExactNoThrow(time);
        final int  fine   = DbTimeUtility.fineFromExact(time);

        try {
            statement.setLong(index++, coarse);
            statement.setLong(index++, coarse);
            statement.setInt(index++, fine);
        }
        catch (final SQLException se) {
            throw new DatabaseException(se);
        }
        return index;
    }



}