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

import java.io.File;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Types;
import java.sql.Wrapper;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.context.ApplicationContext;

import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.IDbSqlArchiveController;
import jpl.gds.db.api.sql.store.IDbSqlStore;
import jpl.gds.db.api.sql.store.IStoreConfiguration;
import jpl.gds.db.api.sql.store.IStoreConfigurationMap;
import jpl.gds.db.api.sql.store.StoreIdentifier;
import jpl.gds.db.mysql.impl.sql.AbstractMySqlInteractor;
import jpl.gds.shared.annotation.ToDo;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.database.BytesBuilder;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.exceptions.SqlExceptionTools;
import jpl.gds.shared.holders.StationIdHolder;
import jpl.gds.shared.message.BaseMessageHandler;
import jpl.gds.shared.string.StringUtil;
import jpl.gds.shared.sys.SystemUtilities;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.util.HostPortUtility;


/**
 * 
 * This is the abstract superclass that is extended by all database classes
 * that are responsible for inserting data into the database.
 *
 * Extended by AbstractLDIStore to implement Load Data Infile capability.
 *
 * @version MPCS-9891 Moved common code from SessionStore
 */
public abstract class AbstractMySqlStore extends AbstractMySqlInteractor implements IDbSqlStore
{

    private static final String CLOSE_TRANSACTION = "closeTransaction";

    /** Enum for the three SQL Wrapper classes with warnings */
    protected enum WarningEnum
    {
        CONNECTION,
        STATEMENT,
        RESULT_SET
    };

    /** Out-of-range integers are replaced with this if possible */
    private static final long           INTEGER_DEFAULT   = 0L;

    /** Largest unsigned int as a long */
    protected static final long         MAX_UNSIGNED_INT  = (1L << Integer.SIZE) - 1L;

    /**
     * The handler used by subclasses to subscribe to messages on the internal
     * bus
     */
    protected BaseMessageHandler        handler           = null;

    /**
     * The handler used by subclasses to subscribe to messages on the internal
     * bus, partial products
     */
    protected BaseMessageHandler        handler2          = null;

    /**
     * A boolean indicating whether this store is running or stopped
     */
    protected final AtomicBoolean       isStoreStopped    = new AtomicBoolean(true);

    /**
     * The prepared statement object used to execute insert queries
     */
    protected PreparedStatement         preparedStatement = null;

    /**
     * Store identifier for this store
     */
    protected final StoreIdentifier     si;

    /**
     * The configuration of this Archive Store retrieved from Spring Application
     * Context
     */
    protected final IStoreConfiguration storeConfig;
    
    /**
     * A common  Store Controller object for use with all database unit tests
     * MPCS-9572 - Moved here from AbstractMySQLInteractor.
     */
    protected final IDbSqlArchiveController    archiveController;


    /**
     * MPCS-7733 Remove batchSize and batchCount and lastInsertTime
     */

	/**
     * Creates an instance of AbstractMySqlStore.
     *
     * @param appContext
     *            the Spring Application Context
     * @param si
     *            the Store Identifier for this Store
     * @param connect
     *            if true, execute a connection to the database on instantiation
     *            if false, do not execute a connection to the database on
     *            instantiation
     */
	public AbstractMySqlStore(final ApplicationContext appContext, final StoreIdentifier si, final boolean connect)
	{
		super(appContext, true, connect); // Want a PreparedStatement
		this.si = si;
		/* MPCS-9572 - init archive controller here rather than in super class */
		this.archiveController = appContext.getBean(IDbSqlArchiveController.class);
        this.storeConfig = appContext.getBean(IStoreConfigurationMap.class).get(si);
        init();		
	}

	
	/**
	 * Initialize this store
	 *
	 */
	protected void init()
	{
		if(this.dbProperties.getUseDatabase() == false)
		{
			return;
		}
		
		this.handler    = null;
        this.handler2   = null;
		this.isStoreStopped.set(true);
		
        /** MPCS-7733  No batch config */

        if (isConnected())
        {
            // See if we can use the Connection by creating a statement

            PreparedStatement ps = null;

		    try
		    {
                ps = getPreparedStatement("select 1");
		    }
            catch (final DatabaseException e)
		    {
                trace.error("Error initializing database store ", this.getStoreIdentifier() + ": ", e.getMessage(),
                        e.getCause());
		    }
            finally
            {
                try
                {
                    if (ps != null)
                    {
                        ps.close();
                    }
                }
                catch (final SQLException sqle)
                {
                	 // ignore this - nothing we can do 
                }
            }
        }
	}


	/**
	 * Get the identifying string for this store for reading
	 * store-specific values out of the configuration file
	 * 
	 * @return The string identifier for this store
	 */
    protected StoreIdentifier getStoreIdentifier() {
        return si;
    }

    /** MPCS-7733  Remove prepared statement batch methods */

	/* (non-Javadoc)
     * @see jpl.gds.db.api.sql.store.IDbSqlStore#start()
     */
	@Override
    public void start()
	{
		if(!this.dbProperties.getUseDatabase() || !this.isStoreStopped.get())
		{
			return;
		}
		startResource();
		isStoreStopped.set(false);
	}

	/* (non-Javadoc)
     * @see jpl.gds.db.api.sql.store.IDbSqlStore#stop()
     */
    @Override
    public void stop()
    {
    	if(!this.dbProperties.getUseDatabase() || this.isStoreStopped.get())
    	{
    		return;
    	}
    
        /** MPCS-7733  No need to flush, no batch */
    	
    	/* 
    	 * MPCS-7135. Moved setting of "stopped" to the end,
    	 * so LDI files can still be written during shutdown. 
    	 */
    	if(this.messagePublicationBus != null)
    	{
                        if (this.handler != null)
                        {
    		    this.messagePublicationBus.unsubscribeAll(this.handler);
                        }
    
                        if (this.handler2 != null)
                        {
    		    this.messagePublicationBus.unsubscribeAll(this.handler2);
                        }
    	}
    	
    	synchronized(this)
    	{
//            System.out.println("3-RRRRRRRRRRRRRRRRRRRRRRRRR (AbstractMySqlStore): " + this.getClass().getSimpleName() + ".stop(): isStoreStopped = " + this.isStoreStopped.get() + ", useDatabase = " + this.dbProperties.getUseDatabase());
    		if(this.preparedStatement != null)
    		{
    			try
    			{
    				this.preparedStatement.close();
    			}
    			catch(final SQLException e)
    			{
    				//do nothing
    			}
    		}
    		this.preparedStatement = null;
    	}
    	this.stopResource();
        this.isStoreStopped.set(true);
    }


    /**
	 * Take the necessary steps to make sure the store is initialized and allow
	 * it to start receiving data and inserting it into the database.
	 *
	 */
	protected void startResource() {
	    // Do nothing.
	}
	

    /**
     * Take the necessary steps to make sure the store is cleaned up properly
     * and no longer allows reception of data and no longer inserts any data
     * into the database.
     */
    protected void stopResource() {
        close();
    }

    /** MPCS-7733 JRemove batchSize getter/setter */

    /**
     * Format a warning message for a time too large to store in the DB.
     *
     * @param what  The name of the time field
     * @param extra Extra identifying information
     * @param scet  The time in question
     * @param ert   The ERT corresponding to the time in question
     *
     * @return Formatted message
     */
    protected String scetExceedsWarning(final String           what,
                                               final String           extra,
                                               final IAccurateDateTime scet,
                                               final IAccurateDateTime ert)
    {
        final StringBuilder sb = new StringBuilder(what);

        if ((extra != null) && ! extra.isEmpty())
        {
            sb.append('(').append(extra).append(')');
        }

        sb.append(" of ").append(scet.getFormattedScet(false));

        if (ert != null)
        {
            sb.append(" at ERT ").append(ert.getFormattedErt(false));
        }

        sb.append(" exceeds maximum");

        return sb.toString();
    }


    /**
     * Format a warning message for a time too large to store in the DB.
     *
     * @param what  The name of the time field
     * @param extra Extra identifying information
     * @param ert   The time in question
     *
     * @return Formatted message
     */
    protected String ertExceedsWarning(final String           what,
                                              final String           extra,
                                              final IAccurateDateTime ert)
    {
        final StringBuilder sb = new StringBuilder(what);

        if ((extra != null) && ! extra.isEmpty())
        {
            sb.append('(').append(extra).append(')');
        }

        sb.append(" of ").append(ert.getFormattedErt(false));

        sb.append(" exceeds maximum");

        return sb.toString();
    }


    /**
     * Format a warning message for a time too large to store in the DB.
     *
     * @param what  The name of the time field
     * @param extra Extra identifying information
     * @param date  The time in question
     *
     * @return Formatted message
     */
    protected String dateExceedsWarning(final String what,
                                               final String extra,
                                        final IAccurateDateTime date)
    {
        final StringBuilder sb = new StringBuilder(what);

        if ((extra != null) && ! extra.isEmpty())
        {
            sb.append('(').append(extra).append(')');
        }

        sb.append(" of ").append(date.toString());

        sb.append(" exceeds maximum");

        return sb.toString();
    }


    /**
     * Check a string column for length, and, if too long, truncate and issue
     * a warning message.
     *
     * @param what      The name of the column
     * @param maxLength The maximum supported length
     * @param value     The value in question
     *
     * @return Value truncated if necessary
     */
    protected String checkLength(final String what,
                                        final int    maxLength,
                                        final String value)
    {
        if (value == null)
        {
            return null;
        }

        final String useValue = value.trim();
        final int    length   = useValue.length();

        if (length <= maxLength)
        {
            return useValue;
        }

        final StringBuilder sb = new StringBuilder(what);

        sb.append(" of ").append(length);

        sb.append(" is truncated to ").append(maxLength);

        sb.append((maxLength != 1) ? " characters" : " character");

        trace.warn(sb.toString());


        return useValue.substring(0, maxLength);
    }


    /**
     * Check a string column for length, and, if too long, truncate and issue
     * a warning message. If empty, return null.
     *
     * @param what      The name of the column
     * @param maxLength The maximum supported length
     * @param value     The value in question
     *
     * @return Value truncated if necessary
     */
    protected String checkLengthAndEmpty(final String what,
                                                final int    maxLength,
                                                final String value)
    {
        final String clean = checkLength(what, maxLength, value);

        return (((clean == null) || clean.isEmpty()) ? null : clean);
    }


    /**
     * Check a string column for length, and, if too long, truncate and issue
     * a warning message. If empty, return null. Turn localhost to local host
     * name.
     *
     * @param what      The name of the column
     * @param maxLength The maximum supported length
     * @param value     The value in question
     *
     * @return Value truncated if necessary
     */
    protected String checkLengthAndEmptyAndLocalhost(
                                final String what,
                                final int    maxLength,
                                final String value)
    {
        /** MPCS-5672 New method */
        /** MPCS-5666 Also */

        final String cleaner = checkLengthAndEmpty(what, maxLength, value);

        if (HostPortUtility.LOCALHOST.equalsIgnoreCase(cleaner))
        {
            return HostPortUtility.getLocalHostName();
        }

        return cleaner;
    }


    /**
     * Validate a DSS against a session DSS.
     *
     * @param dssId        DSS id in question
     * @param sessionDssId Session DSS id
     *
     * @return True if OK
     */
    public boolean isValidDssId(final int dssId,
                                       final Integer sessionDssId)
    {
        // MPCS-4839  Use constants

        /* MPCS-9798 - Need to also check for MIN_VALUE in addition to null. */
    	if (sessionDssId == null || sessionDssId.intValue() == StationIdHolder.MIN_VALUE) {
    		return true;
    	}
    	
    	return dssId == StationIdHolder.MIN_VALUE || dssId == sessionDssId.intValue();
    }


    /**
     * Check whether a station is in range.
     *
     * MPCS-4839 Use constants
     *
     * @param dssId Station in question
     *
     * @return True if OK
     */
    @ToDo("Replace the int with the holder, here and elsewhere.")
    public boolean isStationInRange(final int dssId)
    {
        return ((dssId >= StationIdHolder.MIN_VALUE) &&
                (dssId <= StationIdHolder.MAX_VALUE));
    }


    /**
     * Check an integer column for range, and, if bad, set to
     * default value and issue a warning message. We're not trying
     * to handle unsigned longs that are legitimately negative.
     *
     * @param what     The name of the column
     * @param minValue The minimum value
     * @param maxValue The maximum value
     * @param nullOK   True if NULL is OK
     * @param value    The value in question
     *
     * @return Value or default
     */
    protected final Long checkIntegerRange(final String  what,
                                                  final long    minValue,
                                                  final long    maxValue,
                                                  final boolean nullOK,
                                                  final Long    value)
    {
        /** MPCS-5835 New */

        // If standard default is not within range, use minimum

        final Long defalt =
            Long.valueOf(
                ((INTEGER_DEFAULT < minValue) || (INTEGER_DEFAULT > maxValue))
                    ? minValue
                    : INTEGER_DEFAULT);

        if (value == null)
        {
            if (nullOK)
            {
                return null;
            }

            final StringBuilder sb = new StringBuilder(what);

            sb.append(" cannot be NULL and is forced to ").append(defalt);

            trace.warn(sb.toString());


            return defalt;
        }

        final long    useValue = value.longValue();
        final boolean tooSmall = (useValue < minValue);

        if (! tooSmall && (useValue <= maxValue))
        {
            return value;
        }

        final StringBuilder sb = new StringBuilder(what);

        sb.append(" of ").append(useValue);

        if (tooSmall)
        {
            sb.append(" is less than ").append(minValue);
        }
        else
        {
            sb.append(" is greater than ").append(maxValue);
        }

        sb.append(" and is forced to ").append(defalt);

        trace.warn(sb.toString());


        return defalt;
    }

    //transactions

    /**
     * This method resets all of the transaction changes. It is
     * called whether we failed or succeeded. We do not throw.
     *
     * @param connection Connection
     * @param statement  Statement
     */
    protected static void closeTransaction(final Connection connection,
                                         final Statement  statement)
    {
        trace.debug(CLOSE_TRANSACTION, " Closing transaction");

        try
        {
            connection.setAutoCommit(true);
        }
        catch (final SQLException sse)
        {
            /** MPCS-6718 Print everything in exception */

            trace.error(CLOSE_TRANSACTION, " setAutoCommit failure: ",
                                SqlExceptionTools.rollUpExceptions(sse));
        }
        finally
        {
            printSqlWarnings(connection);
        }

        try
        {
            final int count = statement.executeUpdate("UNLOCK TABLES");

            if (count != 0)
            {
                trace.warn(CLOSE_TRANSACTION, " UNLOCK TABLES returned ", count, " instead of 0");
            }
        }
        catch (final SQLException sse)
        {
            /** MPCS-6718 Print everything in exception */

            trace.error(CLOSE_TRANSACTION, " UNLOCK TABLES failure: ",
                                SqlExceptionTools.rollUpExceptions(sse));
        }
        finally
        {
            printSqlWarnings(statement);
        }

        trace.debug(CLOSE_TRANSACTION, " Successful close");
    }

    /**
     * We get here if something failed along the line. We rollback. We do
     * not clean up here, the caller does that. We do not throw, as the
     * caller already has an exception.
     *
     * @param connection Connection
     */
    protected static void rollbackTransaction(final Connection connection)
    {
        trace.debug("rollbackTransaction ",
                            "Rolling back transaction");
        try
        {
            connection.rollback();
        }
        catch (final SQLException se)
        {
            /** MPCS-6718 Print everything in exception */

            trace.error("rollbackTransaction ", "Rollback failure: ",
                                SqlExceptionTools.rollUpExceptions(se));
        }
        finally
        {
            printSqlWarnings(connection);
        }

        trace.debug("rollbackTransaction ",
                            "Successful rollback");
    }

    //prepared statement utils

    /**
     * Set a non-null string into prepared statement.
     *
     * @param index Index at which to set
     * @param value String value
     *
     * @return New index
     *
     * @throws DatabaseException
     */
    protected int setString(final int    index,
                          final String value) throws DatabaseException
    {
        /** MPCS-5153 New */
        try {
            preparedStatement.setString(index, StringUtil.checkSqlText(value));
        }
        catch (final SQLException se) {
            throw new DatabaseException(se);
        }
        return index + 1;
    }

    /**
     * Set a non-empty string or a NULL into prepared statement. Value is
     * converted to a string (it may already be one.)
     *
     * @param index Index at which to set
     * @param value Object whose string value we want
     *
     * @return New index
     *
     * @throws DatabaseException
     */
    protected int setStringOrNull(final int    index,
                                final Object value) throws DatabaseException
    {
        /** MPCS-5153  */
        final String v = (value != null)
                ? StringUtil.checkSqlText(value.toString().trim())
                : "";

        try {
            if (!v.isEmpty()) {
                preparedStatement.setString(index, v);
            }
            else {
                preparedStatement.setNull(index, Types.VARCHAR);
            }
        }
        catch (final SQLException se) {
            throw new DatabaseException(se);
        }
        return index + 1;
    }



    /**
     * Set an unsigned Integer or a NULL into prepared statement.
     *
     * @param index Index at which to set
     * @param value Integer whose value we want
     *
     * @return New index
     *
     * @throws DatabaseException
     */
    protected int setUnsignedIntegerOrNull(final int     index,
                                         final Integer value) throws DatabaseException
    {
        try {
            if (value != null) {
                /** MPCS-7639 Use Java8 unsigned support */
                preparedStatement.setLong(index, Integer.toUnsignedLong(value));
            }
            else {
                preparedStatement.setNull(index, Types.INTEGER);
            }
        }
        catch (final SQLException se) {
            throw new DatabaseException(se);
        }
        return index + 1;
    }


    /**
     * Set a Long or a NULL into prepared statement.
     *
     * @param index Index at which to set
     * @param value Long whose value we want
     *
     * @return New index
     *
     * @throws DatabaseException
     */
    protected int setLongOrNull(final int  index,
                              final Long value) throws DatabaseException
    {
        try {
            if (value != null) {
                preparedStatement.setLong(index, value);
            }
            else {
                preparedStatement.setNull(index, Types.BIGINT);
            }
        }
        catch (final SQLException se) {
            throw new DatabaseException(se);
        }
        return index + 1;
    }


    /**
     * Set a port or a NULL into prepared statement.
     *
     * @param index Index at which to set
     * @param port  Port whose value we want
     *
     * @return New index
     *
     * @throws DatabaseException
     */
    protected int setPortOrNull(final int index,
                              final int port) throws DatabaseException
    {
        try {
            if (port > 0) {
                preparedStatement.setInt(index, port);
            }
            else {
                preparedStatement.setNull(index, Types.INTEGER);
            }
        }
        catch (final SQLException se) {
            throw new DatabaseException(se);
        }
        return index + 1;
    }


    /**
     * Insert a port or a NULL into a bytes builder.
     *
     * @param bb   Bytes builder
     * @param port Port whose value we want
     */
    protected void insertPortOrNull(final BytesBuilder bb,
                                  final int          port)
    {
        if (port > 0)
        {
            bb.insert(port);
        }
        else
        {
            bb.insertNULL();
        }
    }

    // sql warnings

    /**
     * Print a SQL warning. Do not follow the chain.
     *
     * @param warning A single SQL warning
     */
    private static void printSqlWarning(final SQLWarning warning)
    {
        /** MPCS-6718 Print everything in exception */

        if (warning != null)
        {
            trace.warn("SQL warning: code ", warning.getErrorCode(),
                               " state '", warning.getSQLState(), "' ",
                               SqlExceptionTools.rollUpExceptions(warning));
        }
    }


    /**
     * Print SQL warnings if any, and clear.
     *
     * @param warnings List of SQLWarning
     *
     */
    protected static void printSqlWarnings(final List<SQLWarning> warnings)
    {
        for (final SQLWarning warning : warnings)
        {
            printSqlWarning(warning);
        }
    }


    /**
     * Print SQL warnings if any, and clear.
     *
     * @param type    Type of SQL wrapper
     * @param wrapper SQL wrapper
     */
    private static void printSqlWarnings(final WarningEnum type,
                                         final Wrapper wrapper)
    {
        if (checkSqlClosed(type, wrapper))
        {
            return;
        }

        try
        {
            SQLWarning warning = getSqlWarnings(type, wrapper);

            while (warning != null)
            {
                printSqlWarning(warning);

                warning = warning.getNextWarning();
            }
        }
        finally
        {
            clearSqlWarnings(type, wrapper);
        }
    }


    /**
     * Print SQL warnings if any, and clear.
     *
     * @param wrapper SQL connection
     */
    protected static void printSqlWarnings(final Connection wrapper)
    {
        printSqlWarnings(WarningEnum.CONNECTION, wrapper);
    }


    /**
     * Print SQL warnings if any, and clear.
     *
     * @param wrapper SQL statement
     */
    protected static void printSqlWarnings(final Statement wrapper)
    {
        printSqlWarnings(WarningEnum.STATEMENT, wrapper);
    }


    /**
     * Print SQL warnings if any, and clear.
     *
     * @param wrapper SQL result set
     */
    protected static void printSqlWarnings(final ResultSet wrapper)
    {
        printSqlWarnings(WarningEnum.RESULT_SET, wrapper);
    }

    /**
     * Clear SQL warnings from SQL object.
     *
     * @param type    Type of SQL wrapper
     * @param wrapper SQL wrapper
     *
     */
    protected static void clearSqlWarnings(final WarningEnum type,
                                         final Wrapper     wrapper)
    {
        if (checkSqlClosed(type, wrapper))
        {
            return;
        }

        try
        {
            switch (type)
            {
                case CONNECTION:
                    Connection.class.cast(wrapper).clearWarnings();
                    break;

                case STATEMENT:
                    Statement.class.cast(wrapper).clearWarnings();
                    break;

                case RESULT_SET:
                default:
                    ResultSet.class.cast(wrapper).clearWarnings();
                    break;
            }
        }
        catch (final SQLException sqle)
        {
            // Give up, they are just warnings
            SystemUtilities.doNothing();
        }
    }

    /**
     * See if SQL object is already closed.
     *
     * @param type    Type of SQL wrapper
     * @param wrapper SQL wrapper
     *
     * @return True if already closed
     */
    private static boolean checkSqlClosed(final WarningEnum type,
                                          final Wrapper     wrapper)
    {
        if (wrapper == null)
        {
            return true;
        }

        boolean closed = true;

        try
        {
            switch (type)
            {
                case CONNECTION:
                    closed = Connection.class.cast(wrapper).isClosed();
                    break;

                case STATEMENT:
                    closed = Statement.class.cast(wrapper).isClosed();
                    break;

                case RESULT_SET:
                default:
                    closed = ResultSet.class.cast(wrapper).isClosed();
                    break;
            }
        }
        catch (final SQLException se)
        {
            /** MPCS-6718 Print everything in exception */

            trace.error("checkSqlClosed had trouble checking for ", type, ": ",
                                SqlExceptionTools.rollUpExceptions(se));
        }

        return closed;
    }

    /**
     * Get SQL warnings from SQL object.
     *
     * @param type    Type of SQL wrapper
     * @param wrapper SQL wrapper
     *
     * @return SQLWarning Chained warnings or null if none
     *
     * @version MPCS-6718  Trap SQLException
     */
    private static SQLWarning getSqlWarnings(final WarningEnum type,
                                             final Wrapper     wrapper)
    {
        if (checkSqlClosed(type, wrapper))
        {
            return null;
        }

        SQLWarning warning = null;

        try
        {
            switch (type)
            {
                case CONNECTION:
                    warning = Connection.class.cast(wrapper).getWarnings();
                    break;

                case STATEMENT:
                    warning = Statement.class.cast(wrapper).getWarnings();
                    break;

                case RESULT_SET:
                    warning = ResultSet.class.cast(wrapper).getWarnings();
                    break;

                default:
                    break;
            }
        }
        catch (final SQLException sqle)
        {
            // Give up, they are just warnings
            SystemUtilities.doNothing();
        }

        return warning;
    }

     //misc

    /**
     * Get test type from configuration. If there isn't one, use the
     * application names if there are any. The master app name is used for
     * complicated guys such as chill, where the application creating the
     * session (hence fragment 1) is a hidden app unknown externally, and
     * we do not want the Session.type to be set to that name.
     *
     * NB: If the configuration already has a value, we always use that.
     * The idea is to use the application name as a default.
     *
     * @param contextConfig Context configuration
     *
     * @return Type string or null
     */
    protected static String getTestType(final IContextConfiguration contextConfig)
    {
        String type = ((contextConfig != null)
                ? StringUtil.emptyAsNull(contextConfig.getContextId().getType())
                : null);

        // BEGIN MPCS-4915

        if (type == null)
        {
            type = StringUtil.emptyAsNull(GdsSystemProperties.getSystemProperty("GdsMasterAppName"));
        }

        // END MPCS-4915

        if (type == null)
        {
            type = StringUtil.emptyAsNull(ApplicationConfiguration.getApplicationName());
        }

        return type;
    }

    /**
     * Convert a path (as a String) to an absolute path.
     *
     * @param path
     *
     * @return Absolute path
     */
    protected String getAbsolutePath(final String path)
    {
        if (path == null)
        {
            return "";
        }

        final File f = new File(path.trim());

        return f.getAbsolutePath();
    }

    /**
     * Convert a path (as a String) to a canonical path if possible,
     * otherwise, an absolute path.
     *
     * @param path
     *
     * @return Canonical path
     */
    protected String getCanonicalPath(final String path)
    {
        if (path == null)
        {
            return "";
        }

        final File f = new File(path.trim());

        try
        {
            return f.getCanonicalPath();
        }
        catch (final IOException ioe)
        {
            trace.error("Error getting canonical path: ", ExceptionTools.rollUpMessages(ioe));
        }

        return f.getAbsolutePath();
    }

    /**
     * Protect from null objects
     *
     * @param s
     * @param message
     *
     * @return T
     */
    protected static <T> T throwNull(final T s, final String message) {
        if (s == null) {
            throw new IllegalArgumentException(message);
        }

        return s;
    }

    /**
     * Protect from null objects
     *
     * @param s
     * @param v
     *
     * @return T
     */
    protected static <T> T protectNull(final T s, final T v) {
        return (s != null) ? s : v;
    }
}
