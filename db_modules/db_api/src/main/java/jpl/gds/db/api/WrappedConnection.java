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
package jpl.gds.db.api;

import jpl.gds.db.api.adaptation.IMySqlAdaptationProperties;
import jpl.gds.shared.exceptions.SqlExceptionTools;
import jpl.gds.shared.log.Markers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.thread.SleepUtilities;

import java.security.GeneralSecurityException;
import java.sql.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * This class wraps a JDBC Connection and a Statement so we can monitor things and
 * recreate them if they drop mysteriously. Not much is done for
 * PreparedStatement at this time.
 *
 * We do not expect to be called by multiple threads.
 *
 * DriverManager logging is set up by AbstractMySqlInteractor. It also
 * loads the JDBC driver class.
 *
 * What we call "connecting" involves both connecting to the DB and creating
 * a statement.
 *
 * Sometimes people start out with prepared statements and no SQL. That's
 * actually illegal, so we use something innocuous.
 *
 *
 * @version MPCS-6718  Refactor to use new tools
 */
public class WrappedConnection extends Object {
    private static final String              ME                 = "WrappedConnection";
    private static final String              ME_DOT             = ME + ".";
    private static final int                 DEFAULT_TYPE       = ResultSet.TYPE_FORWARD_ONLY;
    private static final int                 DEFAULT_CONCUR     = ResultSet.CONCUR_READ_ONLY;
    private static final String              DEFAULT_SQL        = "select 1";
    private static final boolean             TRACE_CONNECT      = false;

    private static final String              USING              = "' using '";
    private static final String              ATTEMPT            = "' attempt ";
    private static final String              SER                = "standard error stream";

    private static boolean                   fakeFail           = false;
    private static final boolean             FAKE_FAIL_COMPLETE = false;

    private static final Set<Connection>     OPEN_CONNECTIONS   = Collections.synchronizedSet(new HashSet<Connection>());

    private final IMySqlAdaptationProperties _dbConfig;
    private final String                     _dbUrl;
    private final String                     _dbUser;
    private final boolean                    _prepared;
    private final Tracer                     _trace;
    private final int                        _attempts;
    private final long                       _delay;

    private Connection                       _connection        = null;
    private boolean                          _controlled        = false;
    private Statement                        _statement         = null;
    private boolean                          _closed            = true;
    private String                           _sql               = DEFAULT_SQL;
    private int                              _type              = DEFAULT_TYPE;
    private int                              _concur            = DEFAULT_CONCUR;
    private boolean                          _sigtermHandle     = false;


    /**
     * Construct and create an initial Connection and Statement.
     *
     * @param dbProperties
     *            the current database configuration object
     * @param trace
     *            the Tracer object to use for log output
     * @param prepared
     *            flag indicating whether a prepared statement or plain
     *            statement is desired
     * @param attempts
     *            number of database connection attempts to make
     * @param delay
     *            delay between connection attempts, milliseconds
     *
     * @throws DatabaseException
     *             SQL error
     */
    public WrappedConnection(final IMySqlAdaptationProperties dbProperties, final Tracer trace, final boolean prepared,
            final int attempts, final long delay) throws DatabaseException {
        this(dbProperties, null, prepared, DEFAULT_TYPE, DEFAULT_CONCUR, trace, attempts, delay);
    }

    /**
     * Construct and create an initial Connection and PreparedStatement.
     *
     * @param dbProperties
     *            the current database configuration object
     * @param sql
     *            the SQL string to use in the created statement
     * @param type
     *            the JDBC Result Set type: ResultSet.TYPE_FORWARD_ONLY,
     *            ResultSet.TYPE_SCROLL_INSENSITIVE, or
     *            ResultSet.TYPE_SCROLL_SENSITIVE
     * @param concur
     *            Concurrence state
     * @param trace
     *            the Tracer object to use for log output
     * @param attempts
     *            number of database connection attempts to make
     * @param delay
     *            delay between connection attempts, milliseconds
     *
     * @throws DatabaseException
     *             SQL error
     */
    public WrappedConnection(final IMySqlAdaptationProperties dbProperties, final String sql, final int type,
            final int concur, final Tracer trace, final int attempts, final long delay) throws DatabaseException {
        this(dbProperties, sql, true, type, concur, trace, attempts, delay);
    }

    /**
     * Construct and create an initial Connection and PreparedStatement.
     *
     * @param dbProperties
     *            the current database configuration object
     * @param sql
     *            the SQL string to use in the created statement
     * @param trace
     *            the Tracer object to use for log output
     * @param attempts
     *            number of database connection attempts to make
     * @param delay
     *            delay between connection attempts, milliseconds
     *
     * @throws DatabaseException
     *             SQL error
     */
    public WrappedConnection(final IMySqlAdaptationProperties dbProperties, final String sql, final Tracer trace,
            final int attempts, final long delay) throws DatabaseException {
        this(dbProperties, sql, DEFAULT_TYPE, DEFAULT_CONCUR, trace, attempts, delay);
    }

    /**
     * Construct and create an initial Connection and Statement
     * (or PreparedStatement).
     *
     * @param dbProperties
     *            the current database configuration object
     * @param sql
     *            the SQL string to use in the created statement
     * @param type
     *            the JDBC Result Set type: ResultSet.TYPE_FORWARD_ONLY,
     *            ResultSet.TYPE_SCROLL_INSENSITIVE, or
     *            ResultSet.TYPE_SCROLL_SENSITIVE
     * @param concur
     *            the JDBC Result Set concurrency type:
     *            ResultSet.CONCUR_READ_ONLY
     *            or ResultSet.CONCUR_UPDATABLE
     * @param prepared
     *            flag indicating whether a prepared statement or plain
     *            statement is desired
     * @param trace
     *            the Tracer object to use for log output
     * @param attempts
     *            number of database connection attempts to make
     * @param delay
     *            delay between connection attempts, milliseconds
     *
     * @throws DatabaseException
     *             SQL error
     */
    private WrappedConnection(final IMySqlAdaptationProperties dbProperties, final String sql, final boolean prepared,
            final int type, final int concur, final Tracer trace, final int attempts, final long delay)
            throws DatabaseException {
        super();

        _dbConfig = dbProperties;
        _dbUrl = dbProperties.getDatabaseUrl();
        _dbUser = dbProperties.getUsername();
        _sql = safeSQL(sql);
        _prepared = prepared;
        _type = type;
        _concur = concur;
        _trace = trace == null ? TraceManager.getDefaultTracer() : trace;
        _attempts = Math.max(attempts, 3);
        _delay = Math.max(delay, 1L);
        
        // MPCS-9450 - Ctrl+C shutdown hook to stop reconnection attempts
        Runtime.getRuntime().addShutdownHook(new Thread(new CloseConnections(), "WrappedConnectionQuitSignalHandler"));

        // Start out connected
        createConnection();
    }

    /**
     * Mark that connection is NOT to be closed by shutdown hook.
     */
    public void markControlled() {
        _controlled = true;

        OPEN_CONNECTIONS.remove(_connection);
    }

    /**
     * Close and recreate a connection and statement.
     *
     * @throws DatabaseException
     *             SQL error
     */
    private void createConnection() throws DatabaseException {
        String password = null;
        Exception warnDecrypt = null;

        try {
            password = _dbConfig.getPassword();

            if (!password.isEmpty()) {
                _trace.debug("createConnection Connecting to DB as '", _dbUser, "' with decrypted password");
            }
            else {
                _trace.debug("createConnection Connecting to DB as '", _dbUser, "' with no password");
            }
        }
        catch (final GeneralSecurityException gse) {
            _trace.debug("createConnection Connecting to DB as '", _dbUser,
                    "' with undecrypted password" + gse.getMessage(), gse.getCause());

            warnDecrypt = gse;
            password = _dbConfig.getRawPassword();
        }

        int tryNumber = 0;
        while (tryNumber < _attempts && !_sigtermHandle) {
            final boolean lastTry = (tryNumber >= (_attempts - 1));

            closeConnection();

            if (tryNumber > 0) {
                checkedSleep(_delay);
            }

            try {
                _trace.debug("createConnection Connecting to DB as '", _dbUser, USING, _dbUrl, "' attempts allowed "
                        , _attempts);

                _connection = DriverManager.getConnection(_dbUrl, _dbUser, password);

                if (!_controlled) {
                    OPEN_CONNECTIONS.add(_connection);
                }

                if (TRACE_CONNECT) {
                    _trace.debug("createConnection ", System.identityHashCode(_connection));

                    Thread.dumpStack();
                }

                _statement = _prepared ? _connection.prepareStatement(_sql, _type, _concur)
                        : _connection.createStatement(_type, _concur);
                _closed = false;

                checkConnectionWarnings();
                checkStatementWarnings();
                tryNumber++;

                return;
            }
            catch (final SQLException sqle) {
            		tryNumber++;
                if (lastTry || _sigtermHandle) {
                    _trace.error("createConnection Error connecting to DB as '", _dbUser, USING, _dbUrl, ATTEMPT, tryNumber,
                            1, "; printing exception message to ", SER + sqle.getMessage() + ". See application log "
                                         + "file for more exception detail. ");

                    _trace.error(Markers.SUPPRESS, "createConnection Error connecting to DB exception is: ",
                                 SqlExceptionTools.rollUpExceptions(sqle));
                }
                else {
                    _trace.info("createConnection Error connecting to DB as '", _dbUser, USING, _dbUrl, ATTEMPT, tryNumber,
                            1, ": ", sqle.getMessage(), sqle.getCause());
                }
            }
        }

        if (password.isEmpty()) {
            _trace.error("createConnection Could not connect to DB as '", _dbUser, USING, _dbUrl, "' and no password");
        }
        else if (warnDecrypt == null) {
            _trace.error("createConnection Could not connect to DB as '", _dbUser, USING, _dbUrl,
                    "' and encrypted password");
        }
        else {
            _trace.error("createConnection Could not connect to DB as '", _dbUser, USING, _dbUrl,
                    "' and unencrypted password");

            _trace.warn("createConnection Could not decrypt password: ", warnDecrypt);
        }

        closeConnection();

        throw new DatabaseException(ME_DOT + "createConnection Could not connect to DB '" + _dbUrl + "'");
    }

    /**
     * Close and recreate the current prepared statement.
     *
     * @param sql
     *            the SQL string to use in the prepared statement
     *
     * @throws DatabaseException
     *             SQL error
     */
    public void recreatePreparedStatement(final String sql) throws DatabaseException {
        recreatePreparedStatement(sql, _type, _concur);
    }

    /**
     * Close and recreate the current prepared statement. If we cannot recreate, we
     * let createConnection do all the hard work of retrying.
     *
     * @param sql
     *            the SQL string to use in the prepared statement
     * @param type
     *            the JDBC Result Set type: ResultSet.TYPE_FORWARD_ONLY,
     *            ResultSet.TYPE_SCROLL_INSENSITIVE, or ResultSet.TYPE_SCROLL_SENSITIVE
     * @param concur
     *            the JDBC Result Set concurrency type: ResultSet.CONCUR_READ_ONLY
     *            or ResultSet.CONCUR_UPDATABLE
     *
     * @throws DatabaseException
     *             SQL error
     */
    public void recreatePreparedStatement(final String sql, final int type, final int concur) throws DatabaseException {
        checkSane(true);

        closeStatement();

        _sql = safeSQL(sql);
        _type = type;
        _concur = concur;

        try {
            _statement = _connection.prepareStatement(_sql, _type, _concur);
            _closed = false;

            checkConnectionWarnings();
            checkStatementWarnings();

            return;
        }
        catch (final SQLException sqle) {
            // MPCS-5523: Change from info to debug statement */
            _trace.debug("recreatePreparedStatement Could not recreate: '", _sql, "': " + sqle.getMessage(),
                    sqle.getCause());

            System.err.println(SqlExceptionTools.rollUpExceptions(sqle));
        }

        // Couldn't do it, start over with a new connection and statement

        createConnection();
    }

    /**
     * Close the current connection. The current statement means nothing without a connection,
     * so it is closed as well.
     */
    private void closeConnection() {
        closeStatement();

        if (_connection == null) {
            return;
        }

        try {
            _connection.close();

            if (TRACE_CONNECT) {
                _trace.debug("closeConnection ", System.identityHashCode(_connection));

                Thread.dumpStack();
            }
        }
        catch (final SQLException sqle) {
            _trace.error(
                    "closeConnection; printing exception message " + "to standard error stream " + sqle.getMessage(),
                    sqle.getCause());

            System.err.println(SqlExceptionTools.rollUpExceptions(sqle));
        }
        finally {
            OPEN_CONNECTIONS.remove(_connection);

            _connection = null;
        }
    }

    /**
     * Close the current statement.
     */
    private void closeStatement() {
        if (_statement == null) {
            return;
        }

        try {
            _statement.close();
        }
        catch (final SQLException sqle) {
            _trace.error(
                    "closeStatement; printing exception message " + "to standard error stream " + sqle.getMessage(),
                    sqle.getCause());

            System.err.println(SqlExceptionTools.rollUpExceptions(sqle));
        }
        finally {
            _statement = null;
            _closed = true;
        }
    }

    /**
     * Return the current statement as a Statement. This call is used if the user
     * can't use the auto-recovery features. (For example, with batching.)
     *
     * @return Statement the current statement
     *
     * @throws DatabaseException
     *             SQL error
     */
    public Statement getStatement() throws DatabaseException {
        checkSane(false);

        return _statement;
    }

    /**
     * Return the current statement as a PreparedStatement, or throw if prepared
     * was not specified. For PreparedStatements, we do not currently support
     * the automatic recovery features.
     *
     * @return PreparedStatement the current statement
     *
     * @throws DatabaseException
     *             SQL error
     */
    public PreparedStatement getPreparedStatement() throws DatabaseException {
        checkSane(true);

        return (PreparedStatement) _statement;
    }

    /**
     * Not called by us! A trap to detect unexpected closes.
     *
     * @throws DatabaseException
     *             SQL error
     */
    public void close() throws DatabaseException {
        _trace.error("close");

        Thread.dumpStack();
    }

    /**
     * Called by us to close the connection and statement.
     */
    public void closeAtEnd() {
        _trace.debug("closeAtEnd");

        closeConnection();
    }

    /**
     * Check for a reasonable state.
     *
     * @param forPrepared
     *            flag indicating whether we are checking for a
     *            prepared statement or a plain statement
     *
     * @throws DatabaseException
     *             SQL error
     */
    private void checkSane(final boolean forPrepared) throws DatabaseException {
        if (forPrepared && !_prepared) {
            throw new DatabaseException(ME_DOT + "checkSane Statement should be prepared");
        }

        if (_closed) {
            throw new DatabaseException(ME_DOT + "checkSane Closed");
        }

        if (_connection == null) {
            throw new DatabaseException(ME_DOT + "checkSane No connection");
        }

        if (_statement == null) {
            throw new DatabaseException(ME_DOT + "checkSane No statement");
        }
    }

    /**
     * Executes then given SQL using the current statement, re-attempting upon
     * failure if necessary.
     * (Number of retries is specified when creating the statement.)
     * 
     * @param sql
     *            the SQL string to execute
     *
     * @return boolean true if the statement execution succeeded, false if not
     *
     * @throws DatabaseException
     *             SQL error
     */
    public boolean execute(final String sql) throws DatabaseException {
        checkSane(false);

        SQLException lastSqle = null;

        for (int i = 0; i < _attempts; ++i) {
            final boolean lastTry = (i >= (_attempts - 1));

            if (i > 0) {
                checkedSleep(_delay);
            }

            try {
                if (FAKE_FAIL_COMPLETE || ((i < 2) && fakeFail)) {
                    throw new DatabaseException("Fake fail");
                }

                final boolean status = _statement.execute(sql);
                checkConnectionWarnings();
                checkStatementWarnings();

                if (i > 0) {
                    _trace.info("execute recovered: '", sql, "': ", lastSqle);
                }

                fakeFail = false;

                return status;
            }
            catch (final SQLException sqle) {
                lastSqle = sqle;

                if (lastTry) {
                    _trace.error("execute failed: '", sql, ATTEMPT, i, 1, "; printing exception message to ", SER, " ",
                            sqle.getMessage(), sqle.getCause());

                    System.err.println(SqlExceptionTools.rollUpExceptions(sqle));
                }
                else {
                    _trace.debug("execute failed: '", sql, ATTEMPT, i, 1, ": ", sqle);
                }

                if (i > 0) {
                    // Allow one retry before recreating
                    createConnection();
                }
            }
        }

        _trace.error("execute failed: '", sql);

        closeConnection();

        throw new DatabaseException(ME_DOT + "execute failed: '" + sql + "'");
    }

    /**
     * Executes an SQL query using the current statement.
     *
     * @param sql
     *            Current statement.
     *
     * @return ResultSet the JDBC ResultSet returned by the query
     *
     * @throws DatabaseException
     *             SQL error
     */
    public ResultSet executeQuery(final String sql) throws DatabaseException {
        checkSane(false);

        for (int i = 0; i < _attempts; ++i) {
            final boolean lastTry = (i >= (_attempts - 1));

            if (i > 0) {
                checkedSleep(_delay);
            }

            try {
                final ResultSet status = _statement.executeQuery(sql);

                checkConnectionWarnings();
                checkStatementWarnings();

                return status;
            }
            catch (final SQLException sqle) {
                if (lastTry) {
                    _trace.error("executeQuery failed: '", sql, ATTEMPT, i, 1, "; printing exception message to ", SER,
                            sqle.getMessage(), sqle.getCause());

                    System.err.println(SqlExceptionTools.rollUpExceptions(sqle));
                }
                else {
                    _trace.debug("executeQuery failed: '", sql, ATTEMPT, i, 1, ": ", sqle.getMessage(),
                            sqle.getCause());
                }
            }
        }

        _trace.error("executeQuery failed: '", sql, "'");

        closeConnection();

        throw new DatabaseException(ME_DOT + "executeQuery failed: '" + sql + "'");
    }

    /**
     * Print SQL warnings for connection, then clear them.
     *
     * @version MPCS-6718 Refactor to use new tools
     */
    private void checkConnectionWarnings() {
        if (_connection != null) {
            SqlExceptionTools.logWarning(_trace, _connection);
        }
    }

    /**
     * Print SQL warnings for statement, then clear them
     *
     * @version MPCS-6718 Refactor to use new tools
     */
    private void checkStatementWarnings() {
        if (_statement != null) {
            SqlExceptionTools.logWarning(_trace, _statement);
        }
    }

    /**
     * Sleep for a while, ignoring interrupt. Note that we don't
     * attempt to continue sleeping after an interrupt, so the sleep duration
     * may be less than asked. That's OK, we're just using it when retrying.
     *
     * @param delay
     *            time to sleep, milliseconds
     */
    private static void checkedSleep(final long delay) {
        SleepUtilities.checkedSleep(Math.max(delay, 1L));
    }

    /**
     * Make sure we don't pass obviously bad SQL to a prepared statement.
     *
     * @param sql
     *            the SQL string to check
     *
     * @return Original sql or the safe value
     */
    private static String safeSQL(final String sql) {
        if ((sql == null) || (sql.trim().length() == 0)) {
            return DEFAULT_SQL;
        }

        return sql;
    }

    /**
     * Class used in shutdown hook to make sure all connections are closed.
     * Be very careful and do NOT throw or log.
     */
    private class CloseConnections extends Object implements Runnable {
        /**
         * Constructor.
         */
        public CloseConnections() {
            super();
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void run() {
            try {
            		_sigtermHandle = true;
                innerRun();
            }
            catch (final Exception e) {
                // Give up
            }
        }

        /**
         * Inner run method to do real work.
         */
        private void innerRun() {
            synchronized (OPEN_CONNECTIONS) {
                for (final Connection c : OPEN_CONNECTIONS) {
                    if (c == null) {
                        continue;
                    }

                    try {
                        if (c.isClosed()) {
                            continue;
                        }
                    }
                    catch (final SQLException sqle) {
                        continue;
                    }

                    try {
                        c.close();
                    }
                    catch (final SQLException sqle) {
                        // Do nothing
                    }
                }

                OPEN_CONNECTIONS.clear();
            }
        }
    }

    /**
     * Get connection.
     *
     * @return Connection
     */
    public Connection getConnection() {
        return _connection;
    }
}
