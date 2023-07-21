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

import jpl.gds.context.api.ISimpleContextConfiguration;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.fetch.IContextConfigFetch;
import jpl.gds.db.api.types.IDbContextInfoUpdater;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Object used to provide database query criteria for Context Config
 */
public class DatabaseContextInfo implements IDbContextInfoUpdater {
    private static final int KEY_CAPACITY = 256;
    private static final int PARAM_CAPACITY = 512;
    private static final int FINAL_CAPACITY = 1024;

    /**
     * The pattern of the username of the user who ran or is running the test
     * (pattern implies this value is used in the SQL "LIKE"
     * statement and so it may contain wildcards such as "%")
     */
    protected List<String> userPatternList;

    /**
     * The pattern for the test type
     * (pattern implies this value is used in the SQL "LIKE"
     * statement and so it may contain wildcards such as "%")
     */
    protected List<String> typePatternList;

    /**
     * The pattern for the test host
     * (pattern implies this value is used in the SQL "LIKE"
     * statement and so it may contain wildcards such as "%")
     */
    protected List<String> hostPatternList;

    /**
     * The unique integer identifier of a test session.  If this value is set, all other
     * values in this class will be ignored.
     */
    protected List<Long> keyList;

    /**
     * Ids for parent key. Used when querying by session and context at the same time
     */
    protected List<Long> parentKeyList;

    /**
     * The pattern for the test name
     * (pattern implies this value is used in the SQL "LIKE"
     * statement and so it may contain wildcards such as "%")
     */
    protected List<String> namePatternList;

    /**
     * Creates an instance of DatabaseContextInfo.
     */
    public DatabaseContextInfo() {
        this.userPatternList = new ArrayList<>(16);
        this.typePatternList = new ArrayList<>(16);
        this.hostPatternList = new ArrayList<>(16);
        this.keyList = new ArrayList<>(16);
        this.namePatternList = new ArrayList<>(16);
        this.parentKeyList = new ArrayList<>(16);
    }

    /**
     *
     * Creates an instance of DatabaseContextInfo.
     *
     * @param contextConfig
     *            the Context Configuration to install into the created DatabaseContextInfo
     */
    public DatabaseContextInfo(final ISimpleContextConfiguration contextConfig) {
        this();

        if(contextConfig.getContextId().getUser() != null)
        {
            this.userPatternList.add(contextConfig.getContextId().getUser());
        }

        if(contextConfig.getContextId().getHost() != null)
        {
            this.hostPatternList.add(contextConfig.getContextId().getHost());
        }

        if(contextConfig.getContextId().getType() != null)
        {
            this.typePatternList.add(contextConfig.getContextId().getType());
        }

        if(contextConfig.getContextId().getNumber() != null)
        {
            this.keyList.add(contextConfig.getContextId().getNumber());
        }
        if(contextConfig.getContextId().getName() != null)
        {
            this.namePatternList.add(contextConfig.getContextId().getName());
        }
    }
    
    @Override
    public boolean isSearchCriteriaSet()
    {
        return(
                !this.userPatternList.isEmpty()           ||
                !this.typePatternList.isEmpty()           ||
                !this.hostPatternList.isEmpty()           ||
                !this.keyList.isEmpty()                   ||
                !this.namePatternList.isEmpty()
        );
    }

    @Override
    public boolean isIdHostSearchCriteriaOnlySet()
    {
        return((
                        this.userPatternList.isEmpty()  &&
                        this.typePatternList.isEmpty()  &&
                        this.namePatternList.isEmpty()
        )
                && (!this.hostPatternList.isEmpty() ||
                !this.keyList.isEmpty())
        );
    }

    @Override
    public String getSqlTemplate(final String tablePrefix) {
        final boolean idOnlyQuery = this.isIdHostSearchCriteriaOnlySet();

        final StringBuilder keySql = new StringBuilder(KEY_CAPACITY);

        if(!this.keyList.isEmpty())
        {
            if (idOnlyQuery)
            {
                keySql.append(tablePrefix + ".contextId IN (?");
            }
            else
            {
                keySql.append(IContextConfigFetch.DB_CONTEXT_TABLE_ABBREV + ".contextId IN (?");
            }

            for(int i=1; i < this.keyList.size(); i++)
            {
                keySql.append(",?");
            }
            keySql.append(")");
        }

        final StringBuilder paramSql = new StringBuilder(PARAM_CAPACITY);

        if(!this.userPatternList.isEmpty())
        {
            if(paramSql.length() != 0)
            {
                paramSql.append(" AND ");
            }

            paramSql.append("(");
            paramSql.append(IContextConfigFetch.DB_CONTEXT_TABLE_ABBREV + ".user LIKE ?");
            for(int i=1; i < this.userPatternList.size(); i++)
            {
                paramSql.append(" OR " + IContextConfigFetch.DB_CONTEXT_TABLE_ABBREV + ".user LIKE ?");
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

        addPatternsWithIfnull(paramSql, this.typePatternList, "type", IContextConfigFetch.DB_CONTEXT_TABLE_ABBREV);

        if(!this.namePatternList.isEmpty())
        {
            if(paramSql.length() != 0)
            {
                paramSql.append(" AND ");
            }

            paramSql.append("(");
            paramSql.append(tablePrefix + ".name LIKE ?");
            for(int i=1; i < this.namePatternList.size(); i++)
            {
                paramSql.append(" OR " + tablePrefix + ".name LIKE ?");
            }
            paramSql.append(")");
        }

        final StringBuilder finalSql = new StringBuilder(FINAL_CAPACITY);
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

    @Override
    public int fillInSqlTemplate(final int               index,
                                 final PreparedStatement statement,
                                 final boolean           earlyStart)
            throws DatabaseException {
        try {
            int i = index;

            // if we have a test key, that should be the only parameter
            if (!this.keyList.isEmpty()) {
                for(Long key: keyList){
                    statement.setLong(i++, key);
                }
            }

            if (!this.userPatternList.isEmpty()){
                for(String user: userPatternList){
                    statement.setString(i++, user);
                }
            }

            if (!this.hostPatternList.isEmpty()){
                for(String host: hostPatternList){
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

    @Override
    public int fillInSqlTemplate(final int index, final PreparedStatement statement)
            throws DatabaseException {
        return fillInSqlTemplate(index, statement, false);
    }

    @Override
    public void addSessionKey(final Long testKey)
    {
        if(testKey == null) {
            throw new IllegalArgumentException("The input context key cannot be null");
        }

        if(!this.keyList.contains(testKey)) {
            this.keyList.add(testKey);
        }
    }

    @Override
    public void addParentKey(final Long testKey)
    {
        if(testKey == null) {
            throw new IllegalArgumentException("The input context key cannot be null");
        }

        if(!this.parentKeyList.contains(testKey)) {
            this.parentKeyList.add(testKey);
        }
    }

    @Override
    public void setSessionKey(final Long testKey)
    {
        if (testKey == null) {
            throw new IllegalArgumentException("The input test key cannot be null");
        }

        keyList.clear();
        keyList.add(testKey);
    }

    @Override
    public void setSessionKeyList(final List<Long> testKeyList)
    {
        if(testKeyList == null)
        {
            throw new IllegalArgumentException("The list of context keys is not allowed to be null.");
        }

        this.keyList = testKeyList;
    }

    @Override
    public void addTypePattern(final String typePattern)
    {
        if(typePattern == null) {
            throw new IllegalArgumentException("The input type pattern cannot be null");
        }

        if(!this.typePatternList.contains(typePattern)) {
            this.typePatternList.add(typePattern);
        }
    }

    @Override
    public void setTypePatternList(final List<String> typePatternList)
    {
        if(typePatternList == null) {
            throw new IllegalArgumentException("The list of type patterns cannot be null.");
        }

        this.typePatternList = typePatternList;
    }

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

    @Override
    public void setHostPattern(final String hostPattern)
    {
        if (hostPattern == null)
        {
            throw new IllegalArgumentException("The input host pattern cannot be null");
        }

        hostPatternList.clear();
        hostPatternList.add(hostPattern);
    }

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
     * Get name pattern list.
     *
     * @return Returns the namePatternList.
     */
    @Override
    public List<String> getNamePatternList()
    {
        return this.namePatternList;
    }

    /**
     * Add a name pattern
     *
     * @param namePattern Name pattern
     */
    @Override
    public void addNamePattern(final String namePattern)
    {
        if(namePattern == null)
        {
            throw new IllegalArgumentException("The input name pattern cannot be null");
        }

        if(!this.namePatternList.contains(namePattern))
        {
            this.namePatternList.add(namePattern);
        }
    }

    /**
     * Sets the namePatternList
     *
     * @param namePatternList The namePatternList to set.
     */
    @Override
    public void setNamePatternList(final List<String> namePatternList)
    {
        if(namePatternList == null)
        {
            throw new IllegalArgumentException("The list of name patterns may not be null.");
        }

        this.namePatternList = namePatternList;
    }

    @Override
    public List<Long> getSessionKeyList() {
        return keyList;
    }

    @Override
    public List<Long> getParentKeyList() {
        return parentKeyList;
    }

    @Override
    public List<String> getTypePatternList() {
        return typePatternList;
    }

    @Override
    public List<String> getUserPatternList() {
        return userPatternList;
    }

    @Override
    public List<String> getHostPatternList() {
        return hostPatternList;
    }

    /**
     * Add a where-clause sequence of string patterns that take IFNULL.
     *
     * @param sb       Where clause we are building
     * @param patterns List of patterns
     * @param column   Column
     */
    protected static void addPatternsWithIfnull(final StringBuilder sb, final List<String>  patterns,
                                              final String        column, String tableAbbrev) {
        if (patterns.isEmpty()) {
            return;
        }

        if (sb.length() > 0) {
            sb.append(" AND ");
        }

        final boolean singleton = (patterns.size() == 1);

        boolean first = true;

        if (! singleton) {
            sb.append('(');
        }

        for (final String d : patterns) {
            if (first) {
                first = false;
            }
            else {
                sb.append(" OR ");
            }

            sb.append("(IFNULL(");
            sb.append(tableAbbrev).append('.');
            sb.append(column).append(", '') LIKE ?)");
        }

        if (! singleton) {
            sb.append(')');
        }
    }
}
