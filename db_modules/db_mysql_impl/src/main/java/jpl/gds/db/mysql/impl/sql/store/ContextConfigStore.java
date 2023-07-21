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

package jpl.gds.db.mysql.impl.sql.store;

import jpl.gds.common.config.ConfigurationConstants;
import jpl.gds.context.api.ISimpleContextConfiguration;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.fetch.IDbSqlFetchFactory;
import jpl.gds.db.api.sql.fetch.IHostFetch;
import jpl.gds.db.api.sql.store.IContextConfigStore;
import jpl.gds.db.api.types.IDbContextInfoUpdater;
import jpl.gds.shared.config.ReleaseProperties;
import jpl.gds.shared.database.BytesBuilder;
import jpl.gds.shared.log.Markers;
import jpl.gds.shared.metadata.MetadataKey;
import jpl.gds.shared.metadata.MetadataMap;
import jpl.gds.shared.process.ProcessLauncher;
import jpl.gds.shared.string.StringUtil;
import jpl.gds.shared.types.Pair;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.context.ApplicationContext;

/**
 *
 * This is the database write/storage interface to the Context Config (simple session)
 * table in the MPCS database. This class will receive an input context configuration and write
 * it to the ContextConfig table in the database and return the unique ID for the
 * context configuration.
 *
 */
public class ContextConfigStore extends AbstractMySqlStore implements IContextConfigStore {
    private static final int TYPE_LENGTH     =   64;
    private static final int USER_LENGTH     =   32;
    private static final int HOST_LENGTH     =   64;
    private static final int MVERSION_LENGTH =   16;
    private static final int VALUE_LENGTH    = 4096;


    //field names in DB
    private static final String FIELD_CONTEXT_ID = "contextId";
    private static final String FIELD_HOST_ID = "hostId";
    private static final String FIELD_HOST_NAME = "host";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_USER = "user";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_MPCS_VERSION = "mpcsVersion";
    private static final String FIELD_SESSION_ID = "sessionId";
    private static final String FIELD_PARENT_ID = "parentId";
    private static final String FIELD_KEY = "keyName";
    private static final String FIELD_VALUE = "value";

    private static final String DEFAULT_MPCS_VERSION = "0.0.0";

    private IDbSqlFetchFactory      fetchFactory;

    // Alternate database table names
    private static final String ALT_DB_TABLE_NAME = "." + DB_CONTEXT_CONFIG_TABLE_NAME;
    private static final String ALT_DB_KV_TABLE_NAME = "." + DB_CONTEXT_CONFIG_KEYVAL_TABLE_NAME;

    /**
     * The SQL template to use for inserting a new context
     * (contextId is auto-increment but can also be forced.)
     */
    private static final String INSERT_SQL_TEMPLATE =
            "INSERT INTO "
                    + DB_CONTEXT_CONFIG_TABLE_NAME
                    + "("
                    + FIELD_CONTEXT_ID + ", " + FIELD_HOST_ID + ", " + FIELD_TYPE + ", " + FIELD_USER + ", "
                    + FIELD_HOST_NAME + "," + FIELD_NAME + ", " + FIELD_MPCS_VERSION + ", " + FIELD_SESSION_ID + ", "
                    + FIELD_PARENT_ID + ") "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

    //support for key-value insert
    private static final String INSERT_KV_SQL_TEMPLATE =
            "INSERT INTO "
                    + DB_CONTEXT_CONFIG_KEYVAL_TABLE_NAME
                    + "("
                    + FIELD_CONTEXT_ID + ", " + FIELD_HOST_ID + ", "
                    + FIELD_KEY + ", " + FIELD_VALUE
                    + ") "
                    + "VALUES (?, ?, ?, ?)";

    private static final String UPDATE_SQL_TEMPLATE = "UPDATE " + DB_CONTEXT_CONFIG_KEYVAL_TABLE_NAME + " SET value=? "
            + " WHERE contextId=? AND keyName='" + MetadataKey.SESSION_IDS + "'";

    /**
     * Creates an instance of ContextConfigStore
     *
     * @param appContext
     *            the Spring Application Context
     */
    public ContextConfigStore(final ApplicationContext appContext)
    {
        super(appContext, IContextConfigStore.STORE_IDENTIFIER, true);
        fetchFactory = appContext.getBean(IDbSqlFetchFactory.class);
    }

    @Override
    public long insertContext(ISimpleContextConfiguration config) throws DatabaseException {
        return insertContext(config, 0L);
    }

    @Override
    public long insertContext(ISimpleContextConfiguration contextConfig, long sessionId) throws DatabaseException {
        if (contextConfig == null) {
            throw new DatabaseException("Null input context configuration!");
        }

        // Make sure we're supposed to be using the database
        if (!dbProperties.getUseDatabase() || isStoreStopped.get()) {
            return 0;
        }

        synchronized (this) {
            if (! isConnected()) {
                throw new IllegalStateException("The database connection in " + this.getClass().getName()
                                                        + " has already been closed");
            }

            /*
             * The majority of the code in the try block below is responsible
             * for reading all of the information out of the input test
             * configuration and inserting it into the SQL insert template. A
             * few fields such as test name and host will throw exceptions if
             * they are null, but otherwise all null values are simply replaced
             * with reasonable defaults (usually an empty string)
             */

            int i = 1;

            preparedStatement = null;

            Connection connection = null;
            Statement  statement  = null;

            try {
                // Get a Connection and Statement for use with transactions
                connection = getConnection().getConnection();

                try {
                    statement = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
                }
                finally {
                    printSqlWarnings(connection);
                }

                preparedStatement = getPreparedStatement(INSERT_SQL_TEMPLATE);

                // We want a new context id, so take the auto-increment
                preparedStatement.setNull(i++, Types.BIGINT);

                // Fetches the hostId to set the IContextIdentification hostId
                final IHostFetch hostFetch = fetchFactory.getHostFetch();
                try {
                    hostFetch.getHostId(contextConfig);
                }
                catch (final DatabaseException de)
                {
                    throw new DatabaseException(
                            "Unable to fetch the hostId for the host " + contextConfig.getContextId().getHost() +
                                    " specified in the IContextIdentification object", de);
                }

                // make sure there's a hostId
                if (contextConfig.getContextId().getHostId() == null) {
                    throw new IllegalArgumentException("The input test configuration has no hostId value.  The hostId is not allowed to be null");
                } else {
                    preparedStatement.setInt(i++, contextConfig.getContextId().getHostId());
                }


                i = setStringOrNull(i, checkLengthAndEmpty(FIELD_TYPE, TYPE_LENGTH,
                                                           contextConfig.getContextId().getType()));

                i = setString(i, checkLength(FIELD_USER, USER_LENGTH, contextConfig.getContextId().getUser() != null ?
                        contextConfig.getContextId().getUser() : ""));

                //host name
                String oldHost = contextConfig.getContextId().getHost();
                String newHost = checkLengthAndEmptyAndLocalhost(FIELD_HOST_NAME, HOST_LENGTH, oldHost);

                // Make sure there's a host name
                if (newHost == null)
                {
                    throw new IllegalArgumentException("The input test configuration had a null or empty " +
                                                               "host value, which is not allowed");
                }

                i = setString(i, newHost);

                if (!newHost.equals(oldHost)) {
                    contextConfig.getContextId().setHost(newHost);
                }

                // context name - can be null
                if (contextConfig.getContextId().getName() != null) {
                    i = setString(i, checkLength(FIELD_NAME, ConfigurationConstants.NAME_LENGTH,
                                                 contextConfig.getContextId().getName()));
                }

                final String version = checkLengthAndEmpty(FIELD_MPCS_VERSION, MVERSION_LENGTH,
                                                           ReleaseProperties.getShortVersion());

                i = setString(i, (version != null) ? version : DEFAULT_MPCS_VERSION);

                //sessionId and parentId
                if(sessionId > 0L) {
                    preparedStatement.setLong(i++, sessionId);
                }
                else{
                    preparedStatement.setNull(i++, Types.BIGINT);
                }

                if(contextConfig.getContextId().getContextKey().getParentNumber() != null) {
                    preparedStatement.setLong(i, contextConfig.getContextId().getContextKey().getParentNumber());
                }
                else{
                    preparedStatement.setNull(i, Types.BIGINT);
                }

                // insert the context configuration into the database
                final int rows = preparedStatement.executeUpdate();
                preparedStatement.close();
                if (rows == 0) {
                    throw new DatabaseException(
                            "Test Context information was not inserted into database.");
                }
                preparedStatement = null;

                // Commit and get new Context
                final long ctxId = issuePostInsertStatements(connection, statement);

                for (Map.Entry<MetadataKey, Object> entry : contextConfig.getMetadata().getMap().entrySet()){
                    //skip auto-added CONTEXT_ID
                    if(entry.getKey() != MetadataKey.CONTEXT_ID) {
                        insertKeyValue(ctxId, contextConfig.getContextId().getHostId(), entry.getKey(), entry.getValue());
                    }
                }

                contextConfig.getContextId().setNumber(ctxId);

                //write LDI
                if (dbProperties.getExportLDIAny()) {
                    writeLDI(contextConfig);
                }

                return ctxId;
            }
            catch (final SQLException e)
            {
                // Rollback transaction
                rollbackTransaction(connection);

                e.printStackTrace();
                String sql = "";
                if(preparedStatement != null)
                {
                    sql = preparedStatement.toString();
                }
                final String msg = "Exception encountered while inserting Context Config record into database: " +
                        e.getMessage() + ". (" + sql + ")";
                System.err.println(msg);

                throw new DatabaseException(msg, e);
            }
            finally
            {
                closeTransaction(connection, statement);

                if (statement != null)
                {
                    try
                    {
                        statement.close();
                    }
                    catch (final SQLException sse)
                    {
                        printSqlWarnings(statement);
                    }
                }
            }
        }
    }

    @Override
    public void updateSessionId(final IDbContextInfoUpdater updater, final String sessionIds) throws DatabaseException {
        if (updater == null) {
            throw new DatabaseException("Null input context configuration");
        }

        if (isStoreStopped.get()) {
            return;
        }

        synchronized (this) {
            if (!isConnected()) {
                throw new IllegalStateException("The database connection in " + this.getClass().getName() + " has already been closed");
            }

            int i = 1;
            preparedStatement = null;
            try {
                preparedStatement = getPreparedStatement(UPDATE_SQL_TEMPLATE);
                preparedStatement.setString(i++, sessionIds);
                preparedStatement.setLong(i, updater.getSessionKeyList().get(0));

                updater.fillInSqlTemplate(i, preparedStatement);
                preparedStatement.executeUpdate();
                preparedStatement.close();
            }

            catch (final SQLException e) {
                throw new DatabaseException("Error updating session ID for " + DB_CONTEXT_CONFIG_KEYVAL_TABLE_NAME +
                                                    ": " + e.getMessage(), e);
            }
        }
    }

    private void insertKeyValue(long contextId, int hostId, MetadataKey key, Object value) throws SQLException,
            DatabaseException{
        int i = 1;

        preparedStatement = getPreparedStatement(INSERT_KV_SQL_TEMPLATE);

        //set parameters
        preparedStatement.setLong(i++, contextId);
        preparedStatement.setInt(i++, hostId);
        preparedStatement.setString(i++, key.name());
        preparedStatement.setString(i++, value.toString());

        // insert the context configuration into the database
        final int rows = preparedStatement.executeUpdate();
        preparedStatement.close();
        if (rows == 0) {
            throw new DatabaseException("Test key-value information was not inserted into database.");
        }
        preparedStatement = null;

    }

    /**
     * After the insertion we need to get the id of the last row
     * inserted on this connection. If it is OK, we commit the transaction.
     *
     * The caller handles the errors and the shutdown and resetting.
     *
     * If everything is OK we return the new contextID
     *
     * NB: LAST_INSERT_ID returns the last auto-increment for the Connection,
     * so it is safe from race conditions.
     *
     * @param connection Connection
     * @param statement  Statement
     *
     * @return Inserted context id
     *
     * @throws DatabaseException On any error
     */
    private static long issuePostInsertStatements(final Connection connection, final Statement  statement) throws DatabaseException
    {
        trace.debug("ContextConfigStore.issuePostInsertStatements: Getting results and committing transaction");

        long      id       = 0L;
        ResultSet results  = null;

        try {
            try
            {
                try {
                        // Expect a new contextId
                        results = statement.executeQuery("SELECT " + FIELD_CONTEXT_ID
                                                                 + " FROM " + DB_CONTEXT_CONFIG_TABLE_NAME+ " "
                                                                 + "WHERE (" + FIELD_CONTEXT_ID + "="
                                                                 + "LAST_INSERT_ID()" + ")");
                }
                finally {
                    printSqlWarnings(statement);
                }

                try {
                    if (!results.next()) {
                        throw new DatabaseException(DB_CONTEXT_CONFIG_TABLE_NAME + " row not created");
                    }
                }
                finally {
                    printSqlWarnings(results);
                }

                try
                {
                    id = results.getLong(FIELD_CONTEXT_ID);
                }
                finally
                {
                    printSqlWarnings(results);
                }

                try
                {
                    if (results.wasNull()) {
                        throw new DatabaseException(FIELD_CONTEXT_ID + " cannot be NULL");
                    }
                }
                finally
                {
                    printSqlWarnings(results);
                }

                if (id < 1L)
                {
                    throw new DatabaseException(FIELD_CONTEXT_ID + " must be greater than zero");
                }

                try
                {
                    if (results.next()) {
                        throw new DatabaseException("More than one new " + DB_CONTEXT_CONFIG_TABLE_NAME + " row returned");
                    }
                }
                finally
                {
                    printSqlWarnings(results);
                }

                try {
                    connection.commit();
                }
                finally {
                    printSqlWarnings(connection);
                }
            }
            finally {
                if (results != null) {
                    try {
                        results.close();
                    }
                    catch (final SQLException se) {
                        // No action
                    }
                    finally {
                        printSqlWarnings(results);
                    }
                }
            }

            trace.debug("ContextConfigStore.issuePostInsertStatements Created ", FIELD_CONTEXT_ID,  " ", id);
        }
        catch (final SQLException se) {
            throw new DatabaseException(se);
        }
        return id;
    }

    @Override
    public void writeLDI(final ISimpleContextConfiguration contextConfig) throws DatabaseException {
        final BytesBuilder bb = new BytesBuilder();

        // Format as a line for LDI

        //contextId
        bb.insert(contextConfig.getContextId().getNumber());
        bb.insertSeparator();

        //hostId
        bb.insert(contextConfig.getContextId().getHostId());
        bb.insertSeparator();

        try {
            //user
            bb.insertTextComplainReplace(protectNull(checkLength("user", USER_LENGTH,
                                                                 contextConfig.getContextId().getUser()), ""));
            bb.insertSeparator();

            //type
            final String tt = contextConfig.getContextId().getType();
            if (tt != null && !tt.isEmpty()) {
                bb.insertTextComplainReplace(checkLength("type", TYPE_LENGTH, contextConfig.getContextId().getType()));
            }
            else {
                bb.insertNULL();
            }
            bb.insertSeparator();

            //host
            String oldHost = contextConfig.getContextId().getHost();
            String newHost = checkLengthAndEmptyAndLocalhost("host", HOST_LENGTH, oldHost);

            // Make sure there's a host name
            if (newHost == null) {
                throw new IllegalArgumentException("The input test configuration had a null or empty "
                                                           + "host value, which is not allowed");
            }

            bb.insertTextComplainReplace(newHost);
            bb.insertSeparator();

            if (!newHost.equals(oldHost)) {
                contextConfig.getContextId().setHost(newHost);
            }

            //name
            bb.insertTextComplainReplace(throwNull(checkLength("name", ConfigurationConstants.NAME_LENGTH,
                                                               contextConfig.getContextId().getName()),
                                                   "Context name cannot be null"));

            bb.insertSeparator();

            //mpcsVersion
            final String mv = checkLengthAndEmpty("ContextConfig.mpcsVersion", MVERSION_LENGTH,
                                                  ReleaseProperties.getShortVersion());

            bb.insertTextComplainReplace((mv != null) ? mv : DEFAULT_MPCS_VERSION);

            bb.insertTerminator();

            //format keys and values
            final List<BytesBuilder> metas = formatContextMetadata(contextConfig);

            final String exportDir = dbProperties.getExportLDIDir() + File.separator;
            File contextFile = null;
            File metaFile = null;
            FileOutputStream contextFos = null;
            FileOutputStream metaFos = null;

            try {
                // create files for context and metadata
                final Pair<File, FileOutputStream> pfos = archiveController.openStream(ALT_DB_TABLE_NAME, exportDir, false);
                final Pair<File, FileOutputStream> mfos = archiveController.openStream(ALT_DB_KV_TABLE_NAME, exportDir, false);

                if (pfos == null || mfos == null) {
                    // Errors have already been logged
                    return;
                }

                contextFile = pfos.getOne();
                metaFile = mfos.getOne();
                contextFos = pfos.getTwo();
                metaFos = mfos.getTwo();

                bb.write(contextFos);
                trace.debug("Wrote ", contextFile);

                //write metadata
                for(BytesBuilder meta: metas){
                    meta.write(metaFos);
                }
                trace.debug("Wrote ", metaFile);

            }
            catch (final IOException ioe) {
                trace.error("Unable to write context data" + ioe);
            }
            finally {
                closeQuietly(contextFos);
                closeQuietly(metaFos);
            }

            linkAndDelete(contextFile);
            linkAndDelete(metaFile);
        }
        catch (final SQLException se) {
            throw new DatabaseException(se);
        }
    }

    // Hard link file to the export directory under the regular name, and then delete it
    private void linkAndDelete(File file){
        final String exportDir = dbProperties.getExportLDIDir() + File.separator;

        final String linked = exportDir + file.getName().substring(1);
        final String name = file.getAbsolutePath();
        final String[] command = new String[] { "/bin/ln", name, linked };

        try {
            final int status = ProcessLauncher.launchSimple(command);

            if (status == 0) {
                trace.debug("Exported '", name, "' as '", linked, "'");
            }
            else {
                trace.error(Markers.DB, "Unable to export '" + name + "' as '" + linked + "': " + status);
            }
        }
        catch (final IOException ioe) {
            trace.error(Markers.DB, "Unable to export '" + name + "' as '" + linked + "': " + ioe);
        }

        if (!file.delete()) {
            trace.error(Markers.DB, "Unable to delete: " + name);
        }
    }

    /**
     * Format a ContextConfigKeyValue for LDI.
     *
     * @param contextConfig Simple context configuration
     *
     * @return List of populated BytesBuilder
     *
     * @throws DatabaseException SQL exception
     */
    private List<BytesBuilder> formatContextMetadata(final ISimpleContextConfiguration contextConfig) throws DatabaseException {
        try {
            final MetadataMap metadata = contextConfig.getMetadata();

            if (metadata == null) {
                throw new IllegalArgumentException("Null input metadata");
            }

            final List<BytesBuilder> bbs = new ArrayList<>(metadata.getMap().size());

            final String contextValue = DB_CONTEXT_CONFIG_KEYVAL_TABLE_NAME + ".value";

            // Format all the metadata to the Context metadata SQL

            for(Map.Entry<MetadataKey, Object> entry : metadata.getMap().entrySet()){
                final BytesBuilder bbMeta = new BytesBuilder();

                bbs.add(bbMeta);

                //MPCS-11878 Skip CONTEXT_ID which is automatically added to metadata
                if(entry.getKey() == MetadataKey.CONTEXT_ID){
                    continue;
                }

                //contextId
                bbMeta.insert(contextConfig.getContextId().getNumber());
                bbMeta.insertSeparator();

                //hostId
                bbMeta.insert(contextConfig.getContextId().getHostId());
                bbMeta.insertSeparator();

                //key
                bbMeta.insertTextComplainReplace(entry.getKey().name());
                bbMeta.insertSeparator();

                //value
                bbMeta.insertTextAllowReplace(checkLength(contextValue, VALUE_LENGTH,
                                                           StringUtil.safeTrim(entry.getValue().toString())));
                bbMeta.insertTerminator();
            }

            return bbs;
        }
        catch (final SQLException se) {
            throw new DatabaseException(se);
        }
    }

    private void closeQuietly(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            }
            catch (final IOException ioe) {
                trace.error(Markers.DB, "Unable to close: " + closeable + ": " + ioe);
            }
        }
    }
}
