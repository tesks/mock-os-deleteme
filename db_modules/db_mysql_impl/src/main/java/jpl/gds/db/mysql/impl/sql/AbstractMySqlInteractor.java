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
package jpl.gds.db.mysql.impl.sql;

import static jpl.gds.shared.exceptions.ExceptionTools.rollUpMessages;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.Set;

import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.context.api.EnableFswDownlinkContextFlag;
import jpl.gds.context.api.EnableSseDownlinkContextFlag;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.context.api.IGeneralContextInformation;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.IDbInteractor;
import jpl.gds.db.api.WrappedConnection;
import jpl.gds.db.api.adaptation.IMySqlAdaptationProperties;
import jpl.gds.db.api.types.IDbChannelSampleFactory;
import jpl.gds.db.api.types.IDbContextInfoFactory;
import jpl.gds.db.api.types.IDbSessionInfoFactory;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.time.TimeProperties;

/**
 * This is the superclass for all database store and fetch objects (not
 * command-line applications). It is responsible for creating and maintaining a
 * connection to the database and keeping track of configuration information.
 */
public abstract class AbstractMySqlInteractor implements IDbInteractor {
    /** MPCS-8384  Added */
    protected final boolean                    _extendedDatabase;

    /**
     * MPCS-9572 -  Changed name. This is a postfix, not a prefix.
     * 
     */
    protected final String                     _extendedPostfix;

    private static final boolean               EXTRA_WARN    = false;

    /**
     * A reference to the internal message bus
     */
    protected final IMessagePublicationBus     messagePublicationBus;

    /**
     * A singleton object that contains data for the EndSession table
     */
    protected final EndSessionInfo             endSessionInfo;

    /**
     * Spring Appication Context
     */
    protected final ApplicationContext         appContext;

    /**
     * Database Configuration from Spring Application Context
     */
    protected final IMySqlAdaptationProperties dbProperties;

    /**
     * Context indicating FSW environment
     */
    protected EnableFswDownlinkContextFlag     runFswDownlinkContextFlag;

    /**
     * Context indicating SSE environment
     */
    protected EnableSseDownlinkContextFlag     runSseDownlinkContextFlag;

    /**
     * 
     */
    protected final MissionProperties          missionProps;

    /**
     * We want to set the log writer but once
     */
    private boolean                            _logWriterSet = false;

    /**
     * True if one-time global initialization has been performed
     */
    private boolean                            initialized   = false;

    /**
     * The JDBC connection to the database
     */
    private WrappedConnection                  connection    = null;


    /* MPCS-9572 - removed archive controller member. */
    
    /**
     * A common context information object for use with all database unit tests
     * TODO: Are all these context thingies needed?
     */
    protected IContextIdentification           contextId;

    /**
     * A common context information object for use with all database unit tests
     * TODO: Are all these context thingies needed?
     */
    protected IGeneralContextInformation       contextInfo;

    /**
     * A common context configuration object for use with all database unit
     * tests TODO: Are all these context thingies needed?
     */
    protected IContextConfiguration            contextConfig;

    /**
     * A factory to create IDbChannelSampleProvider objects
     */
    protected IDbChannelSampleFactory          dbChannelSampleFactory;

    /*
     * BEGIN: MPCS-5254: make channel ID special prefixes
     * configurable
     */
    /** Prefix for monitor channel values */
    /*
     * TODO: MPCS-8984 : Remove references to database properties specifying MONITOR and HEADER
     * Prefixes
     */
    protected final String                     monitorChannelPrefix;

    /** Prefix for header channel values */
    /*
     * TODO: MPCS-8984 : Remove references to database properties specifying MONITOR and HEADER
     * Prefixes
     */
    protected final String                     headerChannelPrefix;

    /**
     * MPCS-10486: Made Tracers static for performance
     */
    protected static final Tracer              trace          = TraceManager.getTracer(Loggers.LDI_INSERTER);

    /** The Tracer for gatherer logging */
    protected static final Tracer              gathererTracer = TraceManager.getTracer(Loggers.LDI_GATHERER);

    /** 
     * Names of extended tables from config.
     *  MPCS-9572 - Added member.
     */
    private final Set<String> _dbExtendedTables;

    /** 
     *  Session info factory.
     *  MPCS-9572 -  Added member.
     */
    protected final IDbSessionInfoFactory dbSessionInfoFactory;

    /**
     *  Context info factory.
     *  MPCS-9891 - Added member.
     */
    protected final IDbContextInfoFactory dbContextInfoFactory;

    /** SseContextFlag */
    protected final SseContextFlag             sseFlag;

    /**
     * Creates an instance of AbstractMySqlInteractor.
     *
     * If requested, set up logging for the MySQL driver manager (and drivers.)
     * We need a test configuration to get the output directory. When running
     * chill_down, etc., there will be multiple instantiations of this class,
     * but all will have the same output directory, so we take the first one we
     * see. Test session store does not provide a test configuration, but we
     * will see others. Utility programs will not have a test configuration, and
     * so will not get logs.
     *
     * @param appContext
     *            The application context
     * @param prepared
     *            True if we need to use "prepared" statements
     * @param validate
     *            Validate tc if true
     * @param connect
     *            True if connection needed
     */
    private AbstractMySqlInteractor(final ApplicationContext appContext, final boolean prepared, final boolean validate,
            final boolean connect) {
        this.appContext = appContext;
        this.contextId = appContext.getBean(IContextIdentification.class);
        this.contextInfo = appContext.getBean(IGeneralContextInformation.class);
        this.contextConfig = appContext.getBean(IContextConfiguration.class);
        this.dbProperties = appContext.getBean(IMySqlAdaptationProperties.class);
        this.dbChannelSampleFactory = appContext.getBean(IDbChannelSampleFactory.class);
        this.missionProps = appContext.getBean(MissionProperties.class);
        this.messagePublicationBus = appContext.getBean(IMessagePublicationBus.class);
        /* 
         * MPCS-9572 - Removed init of archive controller.  Replaced by instance of the
         * DB session info factory.
         */
        this.dbSessionInfoFactory = appContext.getBean(IDbSessionInfoFactory.class);
        this.dbContextInfoFactory = appContext.getBean(IDbContextInfoFactory.class);
        this.runFswDownlinkContextFlag = appContext.getBean(EnableFswDownlinkContextFlag.class);
        this.runSseDownlinkContextFlag = appContext.getBean(EnableSseDownlinkContextFlag.class);

        this.sseFlag = appContext.getBean(SseContextFlag.class);

        /*
         * TODO: MPCS-8984 - Remove references to database properties specifying MONITOR and HEADER
         * Prefixes
         */
        this.monitorChannelPrefix = dbProperties.getMonitorPrefix();
        this.headerChannelPrefix = dbProperties.getHeaderPrefix();
        trace.setAppContext(appContext);
        gathererTracer.setAppContext(appContext);

        /*
         * Set what used to be static constants:
         * 
         * MPCS-9572 -  Init directly from config, rather than going through the
         * archive controller. 
         */
        _extendedDatabase = TimeProperties.getInstance().useExtendedScet();
        _extendedPostfix = dbProperties.getExtendedPostfix();
        _dbExtendedTables = dbProperties.getExtendedTables();

        if (validate) {
            if (contextConfig == null) {
                throw new NullPointerException("Null input test configuration");
            }

            if (contextConfig.getContextId().getNumber() == null) {
                throw new IllegalArgumentException("The input test configuration did not have a valid test number set");
            }
        }

        connection = null;
        endSessionInfo = EndSessionInfo.getInstance();

        synchronized (AbstractMySqlInteractor.class) {
            if (!initialized) {
                // Load and install our driver

                try {
                    Class.forName(dbProperties.getJdbcDriverName()).newInstance();

                    initialized = true;
                }
                catch (final InstantiationException ie) {
                    trace.error("Database Exception, constructor: " + rollUpMessages(ie), ie.getCause());
                }
                catch (final ClassNotFoundException cnfe) {
                    trace.error("Database Exception, constructor: " + rollUpMessages(cnfe), cnfe.getCause());
                }
                catch (final IllegalAccessException iae) {
                    trace.error("Database Exception, constructor: " + rollUpMessages(iae), iae.getCause());
                }
            }
        }

        if (dbProperties.getDmLog()) {
            synchronized (this) {
                if (!_logWriterSet) {
                    final Writer dmLogWriter = getDMLogWriter();

                    if (dmLogWriter != null) {
                        DriverManager.setLogWriter(new PrintWriter(dmLogWriter, true));
                        _logWriterSet = true;
                    }
                }
            }
        }

        if (connect) {
            try {
                connectToDatabase(prepared);
            }
            catch (final DatabaseException e) {
                trace.error("Database I/O Exception, constructor: " + rollUpMessages(e), e.getCause());
            }
        }
    }

    /**
     * Creates an instance of AbstractMySqlInteractor.
     * 
     * @param appContext
     *            the Spring Application Context
     * @param prepared
     *            True if we need to use "prepared" statements
     * @param connect
     *            True if connection needed
     */
    protected AbstractMySqlInteractor(final ApplicationContext appContext, final boolean prepared,
            final boolean connect) {
        this(appContext, prepared, false, connect);
    }

    /**
     * Add postfix to table name if necessary.
     *
     * @param nominalTable
     *            Standard table name
     *
     * @return Actual table name
     *
     * @version MPCS-8384  new method
     * @version MPCS-9572 - no longer a pass through to archive controller
     */
    protected String getActualTableName(final String nominalTable) {
        
        final StringBuilder sb = new StringBuilder(nominalTable); 

        if (_extendedDatabase && _dbExtendedTables.contains(nominalTable)) {
            sb.append(_extendedPostfix);
        }
        return sb.toString();
    }

    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.db.api.IDbInteractor#isConnected()
     */
    @Override
    public synchronized boolean isConnected() {
        return (connection != null);
    }

    /**
     * Get a prepared statement and check for warnings.
     *
     * @param sql
     *            The SQL string to use in the prepared statement
     * @param type
     *            Result set type
     * @param concur
     *            Result set concurrency
     *
     * @return PreparedStatement the new prepared statement
     *
     * @throws DatabaseException
     *             SQL error
     */
    protected synchronized PreparedStatement getPreparedStatement(final String sql, final int type, final int concur)
            throws DatabaseException {
        connection.recreatePreparedStatement(sql, type, concur);

        return connection.getPreparedStatement();
    }

    /**
     * Get a prepared statement and check for warnings.
     *
     * @param sql
     *            The SQL string to use in the prepared statement
     *
     * @return PreparedStatement the new PreparedStatement
     *
     * @throws DatabaseException
     *             SQL error
     */
    protected synchronized PreparedStatement getPreparedStatement(final String sql) throws DatabaseException {
        connection.recreatePreparedStatement(sql);

        return connection.getPreparedStatement();
    }

    /**
     * Get a statement and check for warnings.
     *
     * @return Statement the new Statement
     *
     * @throws DatabaseException
     *             SQL error
     */
    protected synchronized Statement getStatement() throws DatabaseException {
        return connection.getStatement();
    }

    /**
     * Get a connection to the database via JDBC.
     * 
     * @param prepared
     *            flag indicating whether to create a prepared statement or
     *            plain statement on this connection
     *
     * @throws DatabaseException
     *             SQL error
     */
    protected void connectToDatabase(final boolean prepared) throws DatabaseException {
        // Only connect if we're configured to use the database

        if (dbProperties.getUseDatabase()) {
            synchronized (this) {
                connection = new WrappedConnection(dbProperties, trace, prepared, dbProperties.getReconnectAttempts(),
                                                   dbProperties.getReconnectDelayMilliseconds());
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.db.api.IDbInteractor#close()
     */
    @Override
    public void close() {
        synchronized (this) {
            if (connection != null) {
                connection.closeAtEnd();
            }
        }
    }

    /**
     * Construct log path name for DriverManager logging and construct a file
     * writer for it. Return null if impossible.
     *
     * Use the configured test output directory. Note that if there is no test
     * configuration, we don't do anything. That means no log for utility
     * programs, only chill_down, etc. will get a log.
     *
     * See constructor for more information.
     *
     * @return Writer for DM log or null
     */
    protected Writer getDMLogWriter() {
        if (contextId == null) {
            return null;
        }

        final String outputDir = contextInfo.getOutputDir();

        if (outputDir == null) {
            return null;
        }

        final File odFile = new File(outputDir);

        if (!odFile.exists()) {
            if (EXTRA_WARN) {
                trace.warn("Could not open DM log file, output directory '", outputDir, "' does not exist");
            }

            return null;
        }

        if (!odFile.isDirectory()) {
            trace.error("Could not open DM log file, output 'directory' '", outputDir, "' is not a directory");

            return null;
        }

        final StringBuilder sb = new StringBuilder(outputDir);

        sb.append(File.separator);
        sb.append(sseFlag.isApplicationSse() ? "dm_sse.log" : "dm_fsw.log");

        final String name = sb.toString();

        trace.debug("Using DM log file: ", name);

        FileWriter fw = null;

        try {
            // Append to it if it exists

            fw = new FileWriter(name, true);
        }
        catch (final IOException ioe) {
            trace.error("Could not open DM log file: " + rollUpMessages(ioe), ioe.getCause());
        }

        return fw;
    }

    /**
     * Get wrapped connection.
     *
     * @return Wrapped connection
     */
    protected WrappedConnection getConnection() {
        return connection;
    }

}
