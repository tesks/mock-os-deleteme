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
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.ConfigurationConstants;
import jpl.gds.common.config.connection.IConnectionMap;
import jpl.gds.common.config.connection.IDatabaseConnectionSupport;
import jpl.gds.common.config.connection.IDownlinkConnection;
import jpl.gds.common.config.connection.IFileConnectionSupport;
import jpl.gds.common.config.connection.INetworkConnection;
import jpl.gds.common.config.connection.IUplinkConnection;
import jpl.gds.common.config.types.DatabaseConnectionKey;
import jpl.gds.common.config.types.DownlinkStreamType;
import jpl.gds.common.config.types.TelemetryConnectionType;
import jpl.gds.common.config.types.TelemetryInputType;
import jpl.gds.common.config.types.UplinkConnectionType;
import jpl.gds.common.config.types.VenueType;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.fetch.IDbSqlFetchFactory;
import jpl.gds.db.api.sql.fetch.IHostFetch;
import jpl.gds.db.api.sql.store.ISessionStore;
import jpl.gds.db.api.types.IDbSessionInfoProvider;
import jpl.gds.shared.config.ReleaseProperties;
import jpl.gds.shared.database.BytesBuilder;
import jpl.gds.shared.holders.HolderException;
import jpl.gds.shared.holders.SessionFragmentHolder;
import jpl.gds.shared.holders.StationIdHolder;
import jpl.gds.shared.log.Markers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.process.ProcessLauncher;
import jpl.gds.shared.string.StringUtil;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.DbTimeUtility;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.TimeTooLargeException;
import jpl.gds.shared.types.Pair;
import jpl.gds.shared.util.HostPortUtility;


/**
 * This is the database write/storage interface to the Test Session table in the
 * MPCS database. This class will receive an input test configuration and write
 * it to the Session table in the database and return the unique ID for the
 * test session. This class also has methods that will allow the end time of a
 * test to be written to an EndSession..
 */
public class SessionStore extends AbstractMySqlStore implements ISessionStore
{


    private static final int TYPE_LENGTH     =   64;
    private static final int FULLNAME_LENGTH =  255;

    private static final int USER_LENGTH     =   32;
    private static final int HOST_LENGTH     =   64;

    // MPCS-4819 Increase to 64
    private static final int VERSION_LENGTH  =   64;

    private static final int DSI_LENGTH      =   16;
    private static final int MVERSION_LENGTH =   16;

    private static final String DEFAULT_MPCS_VERSION = "0.0.0";

    /**
     * Alternate database table name
     */
    private static final String ALT_DB_TABLE_NAME = "." + DB_SESSION_DATA_TABLE_NAME;

    private static final String SESSION_ID       = "sessionId";
    private static final String SESSION_FRAGMENT = "sessionFragment";

    /* MPCS-9572 - Use fetch factory rather than archive controller */
    private final IDbSqlFetchFactory fetchFactory;

    /**
     * The SQL template to use for inserting a new test session
     * (sessionId is auto-increment but can also be forced.)
     */
    private static final String INSERT_SQL_TEMPLATE =
              "INSERT INTO "
            + DB_SESSION_DATA_TABLE_NAME
            + "("
            + SESSION_ID + ", "
            + "name, type, description, fullName, user, host, "
            + "downlinkConnectionType, uplinkConnectionType, outputDirectory, fswDictionaryDir, "
            + "sseDictionaryDir, "
            + "sseVersion, fswVersion, venueType, testbedName, "
            + "rawInputType, startTime, startTimeCoarse, startTimeFine, "
            + "spacecraftId, downlinkStreamId, "
            + "mpcsVersion, fswDownlinkHost, fswUplinkHost, "
            + "fswUplinkPort, fswDownlinkPort, "
            + "sseHost, sseUplinkPort, sseDownlinkPort, inputFile, hostId, "
            + SESSION_FRAGMENT + ", "
            + "topic, outputDirectoryOverride, subtopic, dssId, vcid, fswDownlinkFlag, sseDownlinkFlag, "
            + "uplinkFlag, databaseSessionId, databaseHost"
            + ") "
            + "VALUES (?, "
            +         "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
            +         "?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, "
            +         "?, ?, ?, ?, ?, ?, ?, ?)";

    /**
     * The SQL template to use for updating the output directory of a test
     * session that is already stored in the database
     */
    private static final String OUTPUT_DIR_UPDATE_SQL_TEMPLATE = "UPDATE "
            + DB_SESSION_DATA_TABLE_NAME + " AS " + DB_SESSION_DATA_TABLE_NAME_ABBREV
            + " SET outputDirectory=replace(outputDirectory, ?, ?) where outputDirectory like ?";
    private static final String OUTPUT_DIR_UPDATE_SQL_TEMPLATE_ANY_DIR = "UPDATE "
        + DB_SESSION_DATA_TABLE_NAME + " AS " + DB_SESSION_DATA_TABLE_NAME_ABBREV
        + " SET outputDirectory=?";




    /**
     * Creates an instance of SessionStore.
     * 
     * @param appContext
     *            the Spring Application Context
     */
    public SessionStore(final ApplicationContext appContext)
    {
        super(appContext, ISessionStore.STORE_IDENTIFIER, true);
        fetchFactory = appContext.getBean(IDbSqlFetchFactory.class);

        /** MPCS-7733  Get rid of batchSize */
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.sql.store.ISessionStore#insertTestConfig(jpl.gds.session.config.IContextIdentification, long)
     */
    @Override
    public long insertTestConfig(final IContextConfiguration contextConfig,
                                 final long                        sessionId)
        throws DatabaseException
    {
        if (contextConfig == null) {
            throw new DatabaseException("Null input context configuration!");
        }
        
        // Make sure we're supposed to be using the database
        if (!dbProperties.getUseDatabase() || isStoreStopped.get())
        {
            return (0);
        }

        synchronized (this)
        {
            if (! isConnected())
            {
                throw new IllegalStateException("The database connection in "
                        + this.getClass().getName()
                        + " has already been closed");
            }

            /*
             * Fetches the hostId to set the IContextIdentification hostId
             */
            final IHostFetch hostFetch = fetchFactory.getHostFetch();
            try
            {
                hostFetch.getHostId(contextConfig);
            }
            catch (final DatabaseException de)
            {
                throw new DatabaseException(
                              "Unable to fetch the hostId for the host " +
                                  contextConfig.getContextId().getHost()                           +
                                  " specified "                          +
                                  "in the IContextIdentification object",
                                            de);
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

            try
            {
                // Get a Connection and Statement for use with transactions

                connection = getConnection().getConnection();

                try
                {
                    statement = connection.createStatement(ResultSet.TYPE_FORWARD_ONLY,
                                                           ResultSet.CONCUR_READ_ONLY);
                }
                finally
                {
                    printSqlWarnings(connection);
                }

                // Set up transaction and get fragment to use
                contextConfig.getContextId().setFragment(issuePreInsertStatements(connection,
                                                               statement,
                                                               sessionId).getValue());

                preparedStatement = getPreparedStatement(
                                             INSERT_SQL_TEMPLATE);

                if (sessionId > 0L)
                {
                    // We want a specific session id and a new session fragment,
                    // so force the auto-increment to use that session.

                    preparedStatement.setLong(i++, sessionId);
                }
                else
                {
                    // We want a new session id, so take the auto-increment

                    preparedStatement.setNull(i++, Types.BIGINT);
                }

                // make sure there's a test name
                if (contextConfig.getContextId().getName() != null) {
                    i = setString(i, checkLength("name", ConfigurationConstants.NAME_LENGTH, contextConfig.getContextId().getName()));
                } else {
                    throw new IllegalArgumentException(
                            "The input test configuration had a null test name. " +
                            "The test name is not allowed to be null.");
                }

                i = setStringOrNull(i,
                                    checkLengthAndEmpty("type",
                                                        TYPE_LENGTH,
                                                        getTestType(contextConfig)));

                i = setStringOrNull(i,
                                    checkLengthAndEmpty("description",
                                    		            ConfigurationConstants.DESC_LENGTH,
                                                        contextConfig.getContextId().getDescription()));

                // this field is created dynamically so it doesn't need a null check
                i = setString(i, checkLength("fullname", FULLNAME_LENGTH, contextConfig.getContextId().getFullName()));

                i = setString(i, checkLength("user",
                                             USER_LENGTH,
                                             contextConfig.getContextId().getUser() != null ? contextConfig.getContextId().getUser() : ""));

                /** MPCS-5672 */
                /** Should not be null */

                String oldHost = contextConfig.getContextId().getHost();
                String newHost = checkLengthAndEmptyAndLocalhost("host", HOST_LENGTH, oldHost);

                // Make sure there's a host name

                if (newHost == null)
                {
                    throw new IllegalArgumentException("The input test configuration had a null or empty " +
                                                       "host value, which is not allowed");
                }

                i = setString(i, newHost);

                if (! newHost.equals(oldHost))
                {
                    contextConfig.getContextId().setHost(newHost);
                }
                
                final IConnectionMap hc = contextConfig.getConnectionConfiguration();
                
                IDownlinkConnection dc = hc.getFswDownlinkConnection();

                // Downlink connection type may be present

                final TelemetryConnectionType ct = dc == null ? null : dc.getDownlinkConnectionType();

                i = setStringOrNull(i, (ct != TelemetryConnectionType.UNKNOWN) ? ct : null);

                // Uplink connection type may be present
                
                IUplinkConnection uc = hc.getFswUplinkConnection();

                final UplinkConnectionType uct = uc == null ? null : uc.getUplinkConnectionType();

                i = setStringOrNull(i, (uct != UplinkConnectionType.UNKNOWN) ? uct : null);

                // insert the test output directory as a canonical path
                String outputDir = null;

                try
                {
                    outputDir = getAbsolutePath(contextConfig.getGeneralInfo().getOutputDir());
                }
                catch (final IllegalStateException ise)
                {
                    outputDir = "";
                }

                i = setString(i, checkLength("outputDirectory",  ConfigurationConstants.FILE_LENGTH, outputDir));

                // insert the FSW dictionary directory as a canonical path
                String fswDictDir = null;

                try
                {
                    fswDictDir = getAbsolutePath(contextConfig.getDictionaryConfig().getFswDictionaryDir());
                }
                catch (final IllegalStateException ise)
                {
                    fswDictDir = "";
                }

                i = setString(i, checkLength("fswDictionaryDir",  ConfigurationConstants.FILE_LENGTH, fswDictDir));

                /*
                 * MPCS-4865. SSE dictionary directory and version should always be
                 * set to null in the database for missions that have no SSE support.
                 */
                if (missionProps.missionHasSse()) {
	                // insert the SSE dictionary directory as a canonical path
	                String sseDictDir = null;
	
	                try
	                {
	                    sseDictDir = getAbsolutePath(contextConfig.getDictionaryConfig().getSseDictionaryDir());
	                }
	                catch (final IllegalStateException ise)
	                {
	                    sseDictDir = "";
	                }
	
	                i = setStringOrNull(i, checkLength("Session.sseDictionaryDir",
	                		                           ConfigurationConstants.FILE_LENGTH,
	                                                   sseDictDir));
	
	                i = setStringOrNull(i, checkLengthAndEmpty("Session.sseVersion",
	                                                           VERSION_LENGTH,
	                                                           contextConfig.getDictionaryConfig().getSseVersion()));
                } else {
                	
                	i = setStringOrNull(i, null);
                	i = setStringOrNull(i, null);
                }               
                /*
                 * MPCS-4865. End Changes.
                 */

                i = setStringOrNull(i, checkLengthAndEmpty("fswVersion",
                                                           VERSION_LENGTH,
                                                           contextConfig.getDictionaryConfig().getFswVersion()));

                i = setString(i, contextConfig.getVenueConfiguration().getVenueType() != null ? contextConfig.getVenueConfiguration().getVenueType().toString() : VenueType.UNKNOWN.toString());

                i = setStringOrNull(i,
                                    checkLengthAndEmpty("testbedName",
                                    		 ConfigurationConstants.TESTBED_LENGTH,
                                                        contextConfig.getVenueConfiguration().getTestbedName()));
           
                TelemetryInputType rit = dc == null ? null : dc.getInputType();

                if (TelemetryInputType.UNKNOWN.equals(rit))
                {
                    rit = null;
                }

                i = setStringOrNull(i, (rit != null) ? rit.toString() : null);

                final IAccurateDateTime st = contextConfig.getContextId().getStartTime();

                final Timestamp startTime =
                    new Timestamp((st != null) ? st.getTime() : System.currentTimeMillis());

                final long exact  = startTime.getTime();
                final long coarse = DbTimeUtility.coarseFromExactNoThrow(exact);
                final int  fine   = DbTimeUtility.fineFromExact(exact);

                preparedStatement.setTimestamp(i++, startTime);

                preparedStatement.setLong(i++, coarse);
                preparedStatement.setInt(i++, fine);

                preparedStatement.setInt(i++, contextConfig.getContextId().getSpacecraftId());

                // BEGIN MPCS-4819
                // Remove check for UNKNOWN, null turned to NOT_APPLICABLE

                // Check length and warn.
                // Enum, so length check is not expected to fail unless the
                // enums are changed erroneously.

                final DownlinkStreamType sdsie = contextConfig.getVenueConfiguration().getDownlinkStreamId();
                String                            dlsi  = null;

                try
                {
                    dlsi = checkLength("Session.downlinkStreamId",
                                       DSI_LENGTH,
                                       DownlinkStreamType.convert(
                                           sdsie));
                }
                catch (final IllegalArgumentException iae)
                {
                    TraceManager.getDefaultTracer().error(

                        "DownlinkStreamType cannot be null; " +
                        "forced to :"                                  +
                        DownlinkStreamType.NOT_APPLICABLE);

                    dlsi =
                        DownlinkStreamType.NOT_APPLICABLE.toString();
                }

                // END MPCS-4819

                i = setString(i, dlsi);

                final String version = checkLengthAndEmpty("mpcsVersion",
                                                           MVERSION_LENGTH,
                                                           ReleaseProperties.getShortVersion());

                i = setString(i, (version != null) ? version : DEFAULT_MPCS_VERSION);

                oldHost = dc instanceof INetworkConnection ? ((INetworkConnection)dc).getHost() : null;
                newHost = checkLengthAndEmptyAndLocalhost("fswDownlinkHost", HOST_LENGTH, oldHost);

                i = setStringOrNull(i, newHost);

                if (dc instanceof INetworkConnection && (newHost != null) && ! newHost.equals(oldHost))
                {
                	((INetworkConnection)dc).setHost(newHost);
                }

                
                oldHost = uc == null ? null : uc.getHost();
                newHost = checkLengthAndEmptyAndLocalhost("fswUplinkHost", HOST_LENGTH, oldHost);

                i = setStringOrNull(i, newHost);

                if ((newHost != null) && ! newHost.equals(oldHost))
                {
                    uc.setHost(newHost);
                }

                i = setPortOrNull(i, uc == null? HostPortUtility.UNDEFINED_PORT : uc.getPort());
                i = setPortOrNull(i, dc instanceof INetworkConnection ? ((INetworkConnection)dc).getPort() : HostPortUtility.UNDEFINED_PORT);

                dc = hc.getSseDownlinkConnection();
                uc = hc.getSseUplinkConnection();
                
                /* SSE is tricky. The old connection config and the
                 * session config support only one SSE host. The new config map supports
                 * separate SSE uplink and downlink hosts.  The following nonsense 
                 * is meant to compensate for the change.
                 */
                
                oldHost = null;
                if (dc instanceof INetworkConnection) {
                	oldHost = ((INetworkConnection)dc).getHost();
                } else if (uc != null) {
                	oldHost = uc.getHost();
                }
                newHost = checkLengthAndEmptyAndLocalhost("sseHost", HOST_LENGTH, oldHost);

                i = setStringOrNull(i, newHost);

                if ((newHost != null) && ! newHost.equals(oldHost))
                {
                	if (dc instanceof INetworkConnection) {
                		((INetworkConnection)dc).setHost(newHost);
                	}
                	if (uc != null) {
                		uc.setHost(newHost);
                	}
                }

                i = setPortOrNull(i, uc == null ? HostPortUtility.UNDEFINED_PORT : uc.getPort());
                i = setPortOrNull(i, dc instanceof INetworkConnection ? ((INetworkConnection)dc).getPort() : HostPortUtility.UNDEFINED_PORT);

                dc = hc.getFswDownlinkConnection();
                
                final String inputFile = dc instanceof IFileConnectionSupport ? 
                		((IFileConnectionSupport)dc).getFile() : null;

                if (inputFile != null)
                {
                    i = setString(i, checkLength("inputFile",
                    		 ConfigurationConstants.FILE_LENGTH,
                                                 getCanonicalPath(inputFile)));
                }
                
                else
                {
                    preparedStatement.setNull(i++,Types.VARCHAR);
                }

                // make sure there's a hostId
                if (contextConfig.getContextId().getHostId() == null) {
                    throw new IllegalArgumentException("The input test configuration has no hostId value.  The hostId is not allowed to be null");
                } else {
                    preparedStatement.setInt(i++, contextConfig.getContextId().getHostId());
                }

                // sessionFragment

                // make sure there's a fragment
                if (contextConfig.getContextId().getFragment() == null) {
                    throw new IllegalArgumentException("The input test configuration has no fragment value.  The hostId is not allowed to be null");
                } else {
                    preparedStatement.setInt(i++, contextConfig.getContextId().getFragment());
                }
                
                i = setStringOrNull(i,
                                    checkLength("topic",
                                    		 ConfigurationConstants.TOPIC_LENGTH,
                                                contextConfig.getGeneralInfo().getRootPublicationTopic()));

                // outputDirectoryOverride
                preparedStatement.setInt(i++, contextConfig.getGeneralInfo().isOutputDirOverridden() ? 1 : 0);

                // subtopic
                i = setStringOrNull(i,
                                    checkLength("subtopic",
                                    		 ConfigurationConstants.TOPIC_LENGTH,
                                                contextConfig.getGeneralInfo().getSubtopic()));

                // dssId
                final Integer dssObj = contextConfig.getFilterInformation().getDssId();
                final int dss = (dssObj == null ? StationIdHolder.MIN_VALUE: Math.max(dssObj, StationIdHolder.MIN_VALUE));
                preparedStatement.setInt(i++, dss);

                /*
                 * END MPCS-4839
                 */

                // vcid
                i = setUnsignedIntegerOrNull(i, contextConfig.getFilterInformation().getVcid());

                // fswDownlinkFlag
                preparedStatement.setInt(i++, this.runFswDownlinkContextFlag.isFswDownlinkEnabled() ? 1 : 0);

                // sseDownlinkFlag
                preparedStatement.setInt(i++, this.runSseDownlinkContextFlag.isSseDownlinkEnabled() ? 1 : 0);

                // uplinkFlag

                final boolean hasUplink =
                    ((uct != null) && (uct != UplinkConnectionType.UNKNOWN));

                preparedStatement.setInt(i++, hasUplink ? 1 : 0);

                Long   dbKey  = null;
                String dbHost = null;

                if (dc instanceof IDatabaseConnectionSupport)
                {
                    final DatabaseConnectionKey dsi = ((IDatabaseConnectionSupport)dc).getDatabaseConnectionKey();

                    if (dsi != null)
                    {
                        final List<Long> keys = dsi.getSessionKeyList();

                        if ((keys != null) && ! keys.isEmpty())
                        {
                            dbKey = keys.get(0);
                        }

                        final List<String> hosts = dsi.getHostPatternList();

                        if ((hosts != null) && ! hosts.isEmpty())
                        {
                            dbHost = StringUtil.emptyAsNull(hosts.get(0));
                        }
                    }
                }

                // databaseSessionId
                i = setLongOrNull(i, dbKey);

                // databaseHost
                i = setStringOrNull(i, dbHost);

                // insert the test configuration into the database
                final int rows = preparedStatement.executeUpdate();
                preparedStatement.close();
                if (rows == 0) {
                    throw new DatabaseException(
                            "Test Session information was not inserted into database.");
                }
                preparedStatement = null;

                // Commit and get new Session
                final long testId = issuePostInsertStatements(connection,
                                                              statement,
                                                              sessionId);

                contextConfig.getContextId().setNumber(Long.valueOf(testId));
                this.contextId = contextConfig.getContextId();
                return testId;
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
                final String msg = "Exception encountered while inserting Test Session record into database: " +
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


    /* (non-Javadoc)
     * @see jpl.gds.db.api.sql.store.ISessionStore#insertTestConfig(jpl.gds.session.config.IContextIdentification)
     */
    @Override
    public long insertTestConfig(final IContextConfiguration tc)
        throws DatabaseException
    {
        return insertTestConfig(tc, 0L);
    }





    /**
     * Protect from null objects
     *
     * @param d
     *
     * @return Date
     */
    private static IAccurateDateTime protectNull(final IAccurateDateTime d)
    {
        return (d != null) ? d : new AccurateDateTime();
    }





    /*
     * (non-Javadoc)
     * 
     * @see jpl.gds.db.api.sql.store.ISessionStore#writeLDI(jpl.gds.session.config.IContextIdentification)
     */
    @Override
    public void writeLDI(final IContextConfiguration contextConfig) throws DatabaseException {
        /** MPCS-5153  insertText throughout */

        final BytesBuilder bb = new BytesBuilder();

        // Format as a line for LDI

        bb.insert(contextConfig.getContextId().getNumber().longValue());
        bb.insertSeparator();

        try {
            bb.insertTextComplainReplace(throwNull(checkLength("name", ConfigurationConstants.NAME_LENGTH,
                                                               contextConfig.getContextId().getName()),
                                                   "Test name cannot be null"));

            bb.insertSeparator();

            final String tt = getTestType(contextConfig);

            if (tt != null && !tt.isEmpty()) {
                bb.insertTextComplainReplace(checkLength("type", TYPE_LENGTH, tt));
            }
            else {
                bb.insertNULL();
            }

            bb.insertSeparator();

            final String td = checkLength("description", ConfigurationConstants.DESC_LENGTH,
                                          contextConfig.getContextId().getDescription());

            if (td != null && !td.isEmpty()) {
                bb.insertTextComplainReplace(td);
            }
            else {
                bb.insertNULL();
            }

            bb.insertSeparator();

            bb.insertTextComplainReplace(protectNull(checkLength("fullName", FULLNAME_LENGTH,
                                                                 contextConfig.getContextId().getFullName()),
                                                     ""));
            bb.insertSeparator();

            bb.insertTextComplainReplace(protectNull(checkLength("user", USER_LENGTH,
                                                                 contextConfig.getContextId().getUser()),
                                                     ""));
            bb.insertSeparator();

            /** MPCS-5672 */

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

            IDownlinkConnection dc = contextConfig.getConnectionConfiguration().getFswDownlinkConnection();
            final TelemetryConnectionType ct = dc == null ? null : dc.getDownlinkConnectionType();

            if ((ct != null) && (ct != TelemetryConnectionType.UNKNOWN)) {
                bb.insertTextComplainReplace(ct.toString());
            }
            else {
                bb.insertNULL();
            }
            bb.insertSeparator();

            IUplinkConnection uc = contextConfig.getConnectionConfiguration().getFswUplinkConnection();

            final UplinkConnectionType uct = uc == null ? null : uc.getUplinkConnectionType();

            if ((uct != null) && (uct != UplinkConnectionType.UNKNOWN)) {
                bb.insertTextComplainReplace(uct.toString());
            }
            else {
                bb.insertNULL();
            }
            bb.insertSeparator();

            bb.insertTextComplainReplace(protectNull(checkLength("outputDirectory", ConfigurationConstants.FILE_LENGTH,
                                                                 contextConfig.getGeneralInfo().getOutputDir()),
                                                     ""));
            bb.insertSeparator();

            bb.insertTextComplainReplace(protectNull(checkLength("fswDictionaryDir", ConfigurationConstants.FILE_LENGTH,
                                                                 contextConfig.getDictionaryConfig()
                                                                              .getFswDictionaryDir()),
                                                     ""));
            bb.insertSeparator();

            bb.insertTextComplainReplace(protectNull(checkLength("sseDictionaryDir", ConfigurationConstants.FILE_LENGTH,
                                                                 contextConfig.getDictionaryConfig()
                                                                              .getSseDictionaryDir()),
                                                     ""));
            bb.insertSeparator();

            bb.insertTextOrNullComplainReplace(checkLengthAndEmpty("sseVersion", VERSION_LENGTH,
                                                                   contextConfig.getDictionaryConfig()
                                                                                .getSseVersion()));
            bb.insertSeparator();

            bb.insertTextOrNullComplainReplace(checkLengthAndEmpty("fswVersion", VERSION_LENGTH,
                                                                   contextConfig.getDictionaryConfig()
                                                                                .getFswVersion()));
            bb.insertSeparator();

            bb.insertTextComplainReplace(protectNull(contextConfig.getVenueConfiguration().getVenueType(),
                                                     VenueType.UNKNOWN).toString());
            bb.insertSeparator();

            final String testbed = checkLength("testbedName", ConfigurationConstants.TESTBED_LENGTH,
                                               contextConfig.getVenueConfiguration().getTestbedName());

            if (testbed != null && !testbed.isEmpty()) {
                bb.insertTextComplainReplace(testbed);
            }
            else {
                bb.insertNULL();
            }

            bb.insertSeparator();

            TelemetryInputType rit = dc == null ? null : dc.getInputType();

            if (TelemetryInputType.UNKNOWN.equals(rit)) {
                rit = null;
            }

            bb.insertTextOrNullComplainReplace((rit != null) ? rit.toString() : null);
            bb.insertSeparator();

            // If no start time, use now
            final Timestamp startTime = new Timestamp(protectNull(contextConfig.getContextId()
                                                                               .getStartTime()).getTime());

            bb.insert(startTime);
            bb.insertSeparator();

            try {
                bb.insertDateAsCoarseFineSeparate(new AccurateDateTime(startTime));
            }
            catch (final TimeTooLargeException ttle) {
                TraceManager.getDefaultTracer().warn(

                                                     "startTime exceeded maximum");
            }

            bb.insert(contextConfig.getContextId().getSpacecraftId());
            bb.insertSeparator();

            // BEGIN MPCS-4819
            // Remove check for UNKNOWN, null turned to NOT_APPLICABLE

            // Check length and warn, but not for null.
            // Enum, so length check should not fail

            String dlsi = checkLength("Session.downlinkStreamId", DSI_LENGTH,
                                      DownlinkStreamType.convert(contextConfig.getVenueConfiguration()
                                                                              .getDownlinkStreamId()));

            if (dlsi == null) {
                // Don't emit error, because that would have been done already

                dlsi = DownlinkStreamType.NOT_APPLICABLE.toString();
            }

            bb.insertTextComplainReplace(dlsi);
            bb.insertSeparator();

            // END MPCS-4819

            final String mv = checkLengthAndEmpty("Session.mpcsVersion", MVERSION_LENGTH,
                                                  ReleaseProperties.getShortVersion());

            bb.insertTextComplainReplace((mv != null) ? mv : DEFAULT_MPCS_VERSION);
            bb.insertSeparator();

            /** MPCS-5672  */

            //MPCS-11880: write null when uplink only
            oldHost = dc instanceof INetworkConnection ? ((INetworkConnection) dc).getHost() : null ;
            newHost = checkLengthAndEmptyAndLocalhost("fswDownlinkHost", HOST_LENGTH, oldHost);

            bb.insertTextComplainReplace(newHost);
            bb.insertSeparator();

            if (dc instanceof INetworkConnection && (newHost != null) && !newHost.equals(oldHost)) {
                ((INetworkConnection) dc).setHost(newHost);
            }

            /** MPCS-5672 */

            oldHost = uc == null ? null : uc.getHost();
            newHost = checkLengthAndEmptyAndLocalhost("fswUplinkHost", HOST_LENGTH, oldHost);

            bb.insertTextComplainReplace(newHost);
            bb.insertSeparator();

            if (uc != null && (newHost != null) && !newHost.equals(oldHost)) {
                uc.setHost(newHost);
            }

            insertPortOrNull(bb, uc == null ? HostPortUtility.UNDEFINED_PORT : uc.getPort());
            bb.insertSeparator();

            insertPortOrNull(bb, dc instanceof INetworkConnection ?  ((INetworkConnection) dc).getPort() : HostPortUtility.UNDEFINED_PORT);
            bb.insertSeparator();

            /** MPCS-5672  */

            dc = contextConfig.getConnectionConfiguration().getSseDownlinkConnection();
            uc = contextConfig.getConnectionConfiguration().getSseUplinkConnection();

            /*
             *  SSE is tricky. The old connection config and the
             * session config support only one SSE host. The new config map supports
             * separate SSE uplink and downlink hosts. The following nonsense
             * is meant to compensate for the change.
             */

            oldHost = null;
            if (dc instanceof INetworkConnection) {
                oldHost = ((INetworkConnection) dc).getHost();
            }
            else if (uc != null) {
                oldHost = uc.getHost();
            }
            newHost = checkLengthAndEmptyAndLocalhost("sseHost", HOST_LENGTH, oldHost);

            bb.insertTextComplainReplace(newHost);
            bb.insertSeparator();

            if ((newHost != null) && !newHost.equals(oldHost)) {
                if (dc instanceof INetworkConnection) {
                    ((INetworkConnection) dc).setHost(newHost);
                }
                if (uc != null) {
                    uc.setHost(newHost);
                }
            }

            insertPortOrNull(bb, uc == null ? HostPortUtility.UNDEFINED_PORT : uc.getPort());
            bb.insertSeparator();

            insertPortOrNull(bb, dc instanceof INetworkConnection ? ((INetworkConnection) dc).getPort()
                    : HostPortUtility.UNDEFINED_PORT);
            bb.insertSeparator();

            dc = contextConfig.getConnectionConfiguration().getFswDownlinkConnection();

            final String dif = checkLength("inputFile", ConfigurationConstants.FILE_LENGTH,
                                           dc instanceof IFileConnectionSupport
                                                   ? ((IFileConnectionSupport) dc).getFile() : null);
            if (dif != null) {
                bb.insertTextComplainReplace(dif);
            }
            else {
                bb.insertNULL();
            }

            bb.insertSeparator();

            bb.insert(contextConfig.getContextId().getHostId());
            bb.insertSeparator();

            bb.insert(contextConfig.getContextId().getFragment());
            bb.insertSeparator();

            bb.insertTextOrNullComplainReplace(checkLength("topic", ConfigurationConstants.TOPIC_LENGTH,
                                                           contextConfig.getGeneralInfo().getRootPublicationTopic()));
            bb.insertSeparator();

            bb.insert(contextConfig.getGeneralInfo().isOutputDirOverridden() ? 1 : 0);
            bb.insertSeparator();

            bb.insertTextOrNullComplainReplace(checkLength("subtopic", ConfigurationConstants.TOPIC_LENGTH,
                                                           contextConfig.getGeneralInfo().getSubtopic()));
            bb.insertSeparator();

            final Integer dssId = contextConfig.getFilterInformation().getDssId();
            final Long dssIdl = (dssId != null) ? Integer.toUnsignedLong(dssId) : null;

            bb.insertLongOrNull(dssIdl);

            bb.insertSeparator();

            /** MPCS-7639  Use Java8 unsigned support */
            final Integer vcid = contextConfig.getFilterInformation().getVcid();
            final Long vcidl = (vcid != null) ? Integer.toUnsignedLong(vcid) : null;

            bb.insertLongOrNull(vcidl);
            bb.insertSeparator();

            // fswDownlinkFlag
            bb.insert(this.runFswDownlinkContextFlag.isFswDownlinkEnabled() ? 1 : 0);
            bb.insertSeparator();

            // sseDownlinkFlag
            bb.insert(this.runSseDownlinkContextFlag.isSseDownlinkEnabled() ? 1 : 0);
            bb.insertSeparator();

            // MPCS-11880: write uplinkFlag
            bb.insert(uct != null && uct != UplinkConnectionType.UNKNOWN ? 1 : 0);
            bb.insertSeparator();

            Long dbKey = null;
            String dbHost = null;

            if (dc instanceof IDatabaseConnectionSupport) {
                final DatabaseConnectionKey dsi = ((IDatabaseConnectionSupport) dc).getDatabaseConnectionKey();

                if (dsi != null) {
                    final List<Long> keys = dsi.getSessionKeyList();

                    if ((keys != null) && !keys.isEmpty()) {
                        dbKey = keys.get(0);
                    }

                    final List<String> hosts = dsi.getHostPatternList();

                    if ((hosts != null) && !hosts.isEmpty()) {
                        dbHost = StringUtil.emptyAsNull(hosts.get(0));
                    }
                }
            }

            // databaseSessionId
            bb.insertLongOrNull(dbKey);
            bb.insertSeparator();

            // databaseHost
            bb.insertTextOrNullComplainReplace(dbHost);
            bb.insertTerminator();

            final String edir = dbProperties.getExportLDIDir() + File.separator;
            File file = null;
            FileOutputStream fos = null;

            try {
                // Get file; it will be created

                final Pair<File, FileOutputStream> pfos = archiveController.openStream(ALT_DB_TABLE_NAME, edir, false);

                if (pfos == null) {
                    // Errors have already been logged

                    return;
                }

                file = pfos.getOne();
                fos = pfos.getTwo();

                try {
                    bb.write(fos);

                    trace.debug("Wrote ", file);
                }
                catch (final IOException ioe) {
                    trace.error("Unable to write " + ALT_DB_TABLE_NAME + ": " + ioe);
                }
            }
            finally {
                if (fos != null) {
                    try {
                        fos.close();
                    }
                    catch (final IOException ioe) {
                        trace.error(Markers.DB, "Unable to close: " + file + ": " + ioe);
                    }
                }
            }

            // Hard link it to the export directory under the regular name,
            // and then delete it

            final String linked = edir + file.getName().substring(1);
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
        catch (final SQLException se) {
            throw new DatabaseException(se);
        }
    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.sql.store.ISessionStore#updateOutputDirectory(jpl.gds.db.api.types.DatabaseSessionInfo, java.lang.String, java.lang.String)
     */
    @Override
    public void updateOutputDirectory(final IDbSessionInfoProvider tc, final String oldDir, final String newDir) throws DatabaseException {
        if (tc == null) {
            throw new DatabaseException("Null input session configuration");
        }

        if (isStoreStopped.get()) {
            return;
        }

        synchronized (this)
        {
            if (! isConnected())
            {
                throw new IllegalStateException("The database connection in "
                        + this.getClass().getName()
                        + " has already been closed");
            }

            final StringBuilder sql = new StringBuilder();

            if (oldDir != null)
            {
                sql.append(OUTPUT_DIR_UPDATE_SQL_TEMPLATE);

                final String sqlTemplate =
                    tc.getSqlTemplate(DB_SESSION_DATA_TABLE_NAME_ABBREV);

                if (sqlTemplate != null && ! sqlTemplate.isEmpty())
                {
                    sql.append(" AND ").append(sqlTemplate);
                }
            } else {
                sql.append(OUTPUT_DIR_UPDATE_SQL_TEMPLATE_ANY_DIR);

                final String sqlTemplate =
                    tc.getSqlTemplate(DB_SESSION_DATA_TABLE_NAME_ABBREV);

                if (sqlTemplate != null && ! sqlTemplate.isEmpty())
                {
                    sql.append(" WHERE ").append(sqlTemplate);
                }
            }

            int i = 1;
            preparedStatement = null;
            try {
                preparedStatement = getPreparedStatement(sql.toString());

                if (oldDir != null) {
                    i = setString(i, oldDir);
                }
                i = setString(i, newDir);
                if (oldDir != null) {
                    i = setString(i, oldDir + "%");
                }
                dbSessionInfoFactory.convertProviderToUpdater(tc).fillInSqlTemplate(i, preparedStatement);

               preparedStatement.executeUpdate();

                preparedStatement.close();
            } catch (final SQLException e) {
                throw new DatabaseException("Error updating output directory for " +
                                           DB_SESSION_DATA_TABLE_NAME                      +
                                           ": "                               +
                                           e.getMessage(),
                                       e);
            }
        }
    }

    /**
     * Set up connection for a transaction. We set the safest transaction
     * level. Then we start the transaction by turning off auto-commit. Then
     * we lock the Session table. At that point we exit and we are ready to
     * perform the insertion.
     *
     * We let the caller reset everything.
     *
     * Suppress is for false positive PMD warning.
     *
     * If sessionId is less than one, we need a new sessionId. Otherwise we
     * need a new sessionFragment for that sessionId.
     *
     * @param connection Connection
     * @param statement  Statement
     * @param sessionId  If > 0, a new fragment of this sessionId is needed
     *
     * @return Session fragment holder
     *
     * @throws DatabaseException On any error
     */
    private static SessionFragmentHolder issuePreInsertStatements(
                                             final Connection connection,
                                             final Statement  statement,
                                             final long       sessionId)
        throws DatabaseException
    {
        trace.debug("SessionStore.issuePreInsertStatements. Setting up transaction");

        try {
            try
            {
                connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            }
            finally
            {
                printSqlWarnings(connection);
            }

            try
            {
                connection.setAutoCommit(false);
            }
            finally
            {
                printSqlWarnings(connection);
            }

            try
            {
                final int count = statement.executeUpdate("LOCK TABLES " + DB_SESSION_DATA_TABLE_NAME + " WRITE");

                if (count != 0) {
                    trace.warn("SessionStore.issuePreInsertStatements " + "LOCK TABLES returned " + count
                            + " instead of 0");
                }
            }
            finally
            {
                printSqlWarnings(statement);
            }

            if (sessionId <= 0L)
            {
                return SessionFragmentHolder.MINIMUM;
            }

            // Get the maximum fragment for the sessionId

            ResultSet results = null;
            SessionFragmentHolder fragment = SessionFragmentHolder.MINIMUM;

            try
            {
                try
                {
                    results = statement.executeQuery("SELECT " + "MAX(" + SESSION_FRAGMENT + ") AS maxSf " + "FROM "
                            + DB_SESSION_DATA_TABLE_NAME + " WHERE (sessionId=" + sessionId + ")");
                }
                finally {
                    printSqlWarnings(statement);
                }

                try
                {
                    if (!results.next()) {
                        throw new DatabaseException(DB_SESSION_DATA_TABLE_NAME + " did not return a result");
                    }
                }
                finally
                {
                    printSqlWarnings(results);
                }

                /** MPCS-7430 Handle warnings properly */

                final List<SQLWarning> warnings = new ArrayList<SQLWarning>();

                try {
                    fragment = SessionFragmentHolder.getFromDbRethrow(results, "maxSf", warnings);
                }
                catch (final SQLException se) {
                    // Expect a NULL from database if sessionId does not exist.

                    throw new DatabaseException(SESSION_ID + " " + sessionId + " does not exist", se);
                }
                finally {
                    printSqlWarnings(warnings);

                    warnings.clear();
                }

                // We will store into the next value

                final int nextFragment = fragment.getValue() + 1;

                try {
                    fragment = SessionFragmentHolder.valueOf(nextFragment);
                }
                catch (final HolderException he) {
                    throw new DatabaseException("Next " + SESSION_FRAGMENT + " of " + nextFragment + " exceeds maximum",
                                                he);
                }
            }
            finally {
                if (results != null) {
                    try {
                        results.close();
                    }
                    catch (final SQLException sse) {
                        // No action
                    }
                    finally {
                        printSqlWarnings(results);
                    }
                }
            }
            return fragment;
        }
        catch (final SQLException se) {
            throw new DatabaseException(se);
        }
    }


    /**
     * After the insertion we need to get the id and fragment of the last row
     * inserted on this connection. If it is OK, we commit the transaction.
     *
     * The caller handles the errors and the shutdown and resetting.
     *
     * If everything is OK we return the new sessionId and sessionFragment.
     *
     * issuePreInsertStatements was called before the insertion to set up the
     * transaction.
     *
     * NB: LAST_INSERT_ID returns the last auto-increment for the Connection,
     * so it is safe from race conditions.
     *
     * @param connection Connection
     * @param statement  Statement
     * @param sessionId  If >0, a new fragment is needed
     *
     * @return Inserted session id
     *
     * @throws DatabaseException On any error
     */
    private static long issuePostInsertStatements(final Connection connection,
                                                  final Statement  statement,
                                                  final long       sessionId)
        throws DatabaseException
    {
        trace.debug("SessionStore.issuePostInsertStatements. Getting results and committing transaction");

        long      id       = 0L;
        int       fragment = 0;
        ResultSet results  = null;

        try {
            try
            {
                try {
                    if (sessionId <= 0L) {
                        // Expect a new sessionId and sessionFragment one
                        results = statement.executeQuery("SELECT " + SESSION_ID + "," + SESSION_FRAGMENT + " AS sf "
                                + "FROM " + DB_SESSION_DATA_TABLE_NAME + " WHERE (" + SESSION_ID + "="
                                + "LAST_INSERT_ID()" + ")");
                    }
                    else {
                        // Expect a specific sessionId and a new sessionFragment
                        results = statement.executeQuery("SELECT " + SESSION_ID + "," + "MAX(" + SESSION_FRAGMENT
                                + ") AS sf " + "FROM " + DB_SESSION_DATA_TABLE_NAME + " WHERE (" + SESSION_ID + "="
                                + sessionId + ") " + "GROUP BY " + SESSION_ID);
                    }
                }
                finally {
                    printSqlWarnings(statement);
                }

                try {
                    if (!results.next()) {
                        throw new DatabaseException(DB_SESSION_DATA_TABLE_NAME + " row not created");
                    }
                }
                finally {
                    printSqlWarnings(results);
                }

                try
                {
                    id = results.getLong(SESSION_ID);
                }
                finally
                {
                    printSqlWarnings(results);
                }

                try
                {
                    if (results.wasNull()) {
                        throw new DatabaseException(SESSION_ID + " cannot be NULL");
                    }
                }
                finally
                {
                    printSqlWarnings(results);
                }

                if (id < 1L)
                {
                    throw new DatabaseException(SESSION_ID + " must be greater than zero");
                }

                try
                {
                    fragment = results.getInt("sf");
                }
                finally
                {
                    printSqlWarnings(results);
                }

                try
                {
                    if (results.wasNull()) {
                        throw new DatabaseException(SESSION_FRAGMENT + " cannot be NULL");
                    }
                }
                finally {
                    printSqlWarnings(results);
                }

                if (sessionId <= 0L) {
                    // We wanted a new session

                    if (fragment != 1) {
                        throw new DatabaseException(SESSION_FRAGMENT + " must be one");
                    }
                }
                else if (fragment <= 1) {
                    // We wanted a new fragment

                    throw new DatabaseException(SESSION_FRAGMENT + " must be greater than one");
                }

                try
                {
                    if (results.next()) {
                        throw new DatabaseException("More than one new " + DB_SESSION_DATA_TABLE_NAME
                                + " row returned");
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

            trace.debug("SessionStore.issuePostInsertStatements ", "Created ", SESSION_ID, " ", id, " with ",
                        SESSION_FRAGMENT, " ", fragment);
        }
        catch (final SQLException se) {
            throw new DatabaseException(se);
        }
        return id;
    }
}
