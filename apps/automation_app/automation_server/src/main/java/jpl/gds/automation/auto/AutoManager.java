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
package jpl.gds.automation.auto;

import jpl.gds.automation.common.UplinkRequestManager;
import jpl.gds.common.config.connection.IConnectionMap;
import jpl.gds.common.config.gdsdb.IDatabaseProperties;
import jpl.gds.common.config.security.AccessControlParameters;
import jpl.gds.common.config.security.SecurityProperties;
import jpl.gds.common.config.types.CommandUserRole;
import jpl.gds.common.config.types.LoginEnum;
import jpl.gds.common.config.types.UplinkConnectionType;
import jpl.gds.common.spring.bootstrap.CommonSpringBootstrap;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.IDbSqlArchiveController;
import jpl.gds.db.api.sql.fetch.IDbSqlFetch;
import jpl.gds.db.api.sql.fetch.IDbSqlFetchFactory;
import jpl.gds.db.api.sql.order.IOrderByTypeFactory;
import jpl.gds.db.api.sql.order.ISessionOrderByType;
import jpl.gds.db.api.sql.order.OrderByType;
import jpl.gds.db.api.sql.store.IEndSessionStore;
import jpl.gds.db.api.types.*;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.client.FlightDictionaryLoadingStrategy;
import jpl.gds.dictionary.api.client.command.ICommandUtilityDictionaryManager;
import jpl.gds.dictionary.api.command.ICommandDefinitionProvider;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.security.cam.AccessControl;
import jpl.gds.security.cam.AccessControlException;
import jpl.gds.session.config.SessionConfiguration;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.*;
import jpl.gds.shared.metadata.InvalidMetadataException;
import jpl.gds.tc.api.IScmf;
import jpl.gds.tc.api.IScmfFactory;
import jpl.gds.tc.api.UplinkLogger;
import jpl.gds.tc.api.config.ScmfProperties;
import jpl.gds.tc.api.exception.*;
import jpl.gds.tc.api.icmd.exception.AuthenticationException;
import jpl.gds.tc.api.output.IRawOutputAdapter;
import jpl.gds.tc.api.output.IRawOutputAdapterFactory;
import org.slf4j.MDC;
import org.springframework.context.ApplicationContext;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeoutException;

/**
 * AutoManager is the AUTO "representative" in AMPCS. It fulfills all AUTO
 * functions that require AMPCS. It's actions are dictated by the
 * AutoUplinkServerApp, which acts as a proxy between AUTO and AutoManager.
 */
public final class AutoManager {

    /**
     * 0 to this number will define the size of the random number pool that
     * prefixes for IDs to use for "waitForRadiation" will come from. Should be
     * large enough so that identical SCMFs sent within a millisecond of each
     * other will still have unique IDs for the duration that they are in the
     * CPD radiation queue.
     */
    private static final int MAX_RANDOM_ID_PREFIX = 99999;

    private Tracer                        trace;

    private static final int DEFAULT_SESSION_FETCH_BATCH_SIZE = 50;

    /**
     * The session configuration object representing the AMPCS session AUTO is
     * currently attached to
     */
    private IContextConfiguration    sessionConfig;

    /** The logger to use for logging. */
    private UplinkLogger logger;

    private final String keytabFile;
    private final CommandUserRole userRole;
    private String username;

    private boolean accessControlInitialized;

    private UplinkRequestManager requestManager;
    private final Random generator;
    
    private final ApplicationContext appContext;
    
    /** The database store controller provided by dbService */
    private final IDbSqlArchiveController archiveController;
    
    /** The database fetch instance factory */
    private final IDbSqlFetchFactory fetchFactory;

    private final IDbSessionFactory        dbSessionFactory;
    private final IDbSessionInfoFactory    dbSessionInfoFactory;
    private final IOrderByTypeFactory      orderByTypeFactory;
    private boolean                 logToDb                          = true;


    /**
     * AUTO Manager class constructor
     * 
     * @param appContext
     *            the current application context
     */
    public AutoManager(final ApplicationContext appContext) {
        this.accessControlInitialized = false;
        this.generator = new Random();

        this.appContext = appContext;
        this.archiveController = this.appContext.getBean(IDbSqlArchiveController.class);
        this.fetchFactory = appContext.getBean(IDbSqlFetchFactory.class);
        this.dbSessionFactory = this.appContext.getBean(IDbSessionFactory.class);
        this.dbSessionInfoFactory = this.appContext.getBean(IDbSessionInfoFactory.class);
        this.orderByTypeFactory = this.appContext.getBean(IOrderByTypeFactory.class);
        this.trace = TraceManager.getTracer(appContext, Loggers.AUTO_UPLINK);
        this.logger = new UplinkLogger(appContext, trace, true);

        final AccessControlParameters acp = appContext.getBean(AccessControlParameters.class);
        this.keytabFile = acp.getKeytabFile();
        this.userRole = acp.getUserRole();
        this.username = acp.getUserId();
    }

    /**
     * Sets the session configuration object
     * 
     * @param config
     *            SessionConfiguration to use
     */
    public void setSessionConfiguration(final IContextConfiguration config) {
        this.sessionConfig = config;
    }

    
	/**
	 * Gets the ApplicationContext used by this instance
	 * 
	 * @TODO R8 Refactor TODO - ScmfResource needs to be definitively tied into
	 *       the ApplicationContext used by the rest of the application. This
	 *       seems to be the only window into this unless autowiring does it
	 *       appropriately.
	 *       
	 * @return the AplicaitonContext used by AutoManager
	 */
    public final ApplicationContext getApplicationContext(){
    	return this.appContext;
    }

    /**
     * Set the log file for AutoManager to log to
     * 
     * @param logFile the log file for AutoManager to log to
     */
    public void setLogFile(final String logFile) {
        MDC.put(LoggingConstants.FILE_APP_LOG_PATH, logFile);
        this.trace = TraceManager.getTracer(appContext, Loggers.AUTO_UPLINK);
        this.logger = new UplinkLogger(appContext, trace, true);
    }

    /**
     * Set whether or not to write logs to the database
     * 
     * @param logToDb true to write logs to the database, false otherwise
     */
    public void setLogToDb(final boolean logToDb) {
        this.logToDb = logToDb;
    }

    private void checkSessionInit() throws AutoProxyException {
        // if a session was not initialized, initialize with defaults
        if (this.sessionConfig == null) {
            this.sessionInit(this.getLatestUplinkSession());
        }
    }

    /**
     * Get the latest session that had uplink from the database. Latest is
     * defined as the largest session ID.
     * 
     * @return a SessionConfiguration object representing the latest session
     * @throws AutoProxyException if no sessions were found in the database or
     *             if there is a problem fetching sessions from the database.
     */
    public IContextConfiguration getLatestUplinkSession()throws AutoProxyException {
        return this.getSessionFromDatabase(dbSessionInfoFactory.createQueryableUpdater());
    }

    /**
     * Get an uplink session from the database given a specific session ID.
     * 
     * @param sessionId the session ID of the session to fetch
     * @param sessionHost the session host of the session to fetch
     * @return a SessionConfiguration object representing the session
     * @throws AutoProxyException If the provided session ID is not an uplink
     *             session
     */
    public IContextConfiguration getUplinkSessionFromDatabase(
            final long sessionId, final String sessionHost)
            throws AutoProxyException {
    	
    	if (appContext == null) {
    		throw new IllegalStateException("ApplicationContext is null in AutoManager");
    	}
        final IDatabaseProperties dbProperties = appContext.getBean(CommonSpringBootstrap.DATABASE_PROPERTIES, IDatabaseProperties.class);
        
        final IDbSessionInfoUpdater dbSessionInfo = dbSessionInfoFactory.createQueryableUpdater();
        dbSessionInfo.setSessionKey(sessionId);
        dbSessionInfo.setHostPattern(sessionHost);
        final IContextConfiguration sessionConfig =
                this.getSessionFromDatabase(dbSessionInfo);

        // Detect session not found and emit proper error
        if (sessionConfig == null) {
            throw new AutoProxyException(
                    String.format(
                            "No session with ID %d and host %s was found on the Life-of-Mission database located at %s:%d",
                            sessionId, sessionHost, dbProperties.getHost(),
                            dbProperties.getPort()));
        }

        return sessionConfig;
    }

    @SuppressWarnings("unchecked")
    private IContextConfiguration getSessionFromDatabase(final IDbSessionInfoProvider dbSessionInfo)
            throws AutoProxyException {
        // get the latest session

        final IDbSqlFetch sessionFetch = fetchFactory.getSessionFetch();
        final ISessionOrderByType startTimeOrderDesc = (ISessionOrderByType) orderByTypeFactory.getOrderByType(OrderByType.SESSION_ORDER_BY,
                                                                                                               ISessionOrderByType.ID_DESC_TYPE);

        List<IDbSessionProvider> databaseSessions = null;
        try {
            databaseSessions = (List<IDbSessionProvider>) sessionFetch.get(dbSessionInfo, null, DEFAULT_SESSION_FETCH_BATCH_SIZE,
                    startTimeOrderDesc);
        }
        catch (final DatabaseException e) {
            sessionFetch.close();
            throw new AutoProxyException("Encountered error while fetching sessions from database: " + e.getMessage());
        }

        if (databaseSessions.isEmpty()) {
            trace.debug("No sessions found in the database");
        }
        else {
            trace.debug("Returned ", databaseSessions.size(), " session records ");
        }

        IDbSessionProvider matchingSession = null;

        /// Iterate through all results.
        // Must call getNextResultBatch to process results after first batch
        outer: while (databaseSessions != null && !databaseSessions.isEmpty()) {
            try {
                for (final IDbSessionProvider ds : databaseSessions) {
                    final UplinkConnectionType upConnType = ds.getUplinkConnectionType();
                    if ((upConnType != null) && !upConnType.equals(UplinkConnectionType.UNKNOWN)) {
                        matchingSession = ds;
                        break outer;
                    }
                }
                databaseSessions = (List<IDbSessionProvider>) sessionFetch.getNextResultBatch();

                if (databaseSessions.isEmpty()) {
                    trace.debug("No more sessions found in the database");
                }
                else {
                    trace.debug("Returned ", databaseSessions.size(), " additional session records ");
                }

            }
            catch (final DatabaseException e) {
                sessionFetch.close();
                trace.warn(Markers.SUPPRESS, ExceptionTools.getMessage(e), e);
                throw new AutoProxyException("Encountered error while fetching sessions from database: " + ExceptionTools.getMessage(e));
            }
        }

        if (matchingSession == null) {
            sessionFetch.close();
            throw new AutoProxyException("No sessions found in the database. "
                    + "Provide a valid session configuration file " + "to create a new session.");
        }

        // Can no longer create session straight from database object. Replace with multiple steps.
        final SessionConfiguration config = (SessionConfiguration) appContext.getBean(IContextConfiguration.class);
        dbSessionFactory.convertProviderToUpdater(matchingSession).setIntoContextConfiguration(config);

        // set css configuration since it is not stored in the database
        final AccessControlParameters acp = appContext.getBean(AccessControlParameters.class);
        acp.setKeytabFile(this.keytabFile);
        acp.setUserRole(this.userRole);

        // Abort query and close the SessionFetch
        sessionFetch.abortQuery();
        sessionFetch.close();
        return config;
    }

    /**
     * Sets the user and requests an SSO token
     *
     * @throws AuthenticationException if unable to authenticate the user
     */
    public void initAccessControl() throws AuthenticationException {
    	
    	if (appContext == null) {
    		throw new IllegalStateException("ApplicationContext is null in AutoManager");
    	}
        if (!this.accessControlInitialized) {
        	final AccessControlParameters acp = appContext.getBean(AccessControlParameters.class);
            if (appContext.getBean(SecurityProperties.class).getEnabled()) {
                AccessControl ac = null;
                try {
                    // Initially guess the security user is the login user

                    ac =
                            AccessControl.createAccessControl(
                            		appContext.getBean(SecurityProperties.class),
                                    acp.getUserId(),
                                    acp.getUserRole(),
                                    LoginEnum.KEYTAB_FILE,
                                    acp.getKeytabFile(), false,
                                    null, trace);
                } catch (final AccessControlException ace) {
                    throw new AuthenticationException(
                            "Error encountered while attempting to authenticating user",
                            ace);
                }

                try {
                    ac.requestSsoToken();

                    // Now get the real user

                    acp.setUserId(ac.getUser());
                    this.username = ac.getUser();
                } catch (final AccessControlException ace) {
                    throw new AuthenticationException(
                            "Unable to authenticate user", ace);
                }

            } else {
                this.logger.info("Access control is disabled");
            }
            this.accessControlInitialized = true;
        }
    }


    /**
     * Logs a message
     * 
     * @param logMessage the message to log
     * @param severity the severity level to log the message at
     * @throws AutoProxyException if a session has not been initialized and
     *             there is a problem initializing a session
     */
    public void log(final String logMessage, final TraceSeverity severity)
            throws AutoProxyException {
        this.checkSessionInit();

        this.logger.publishLogMessage(logMessage, severity);
    }

    /**
     * Creates a new session using the provided SessionConfiguration object and
     * initializes the session. All actions that are associated with session
     * will be associated with the newly created session after this call.
     * 
     * @param sessionConfig the SessionConfiguration object describing the
     *            session to be created.
     * @throws AutoProxyException if the SessionConfiguration object, session
     *             ID, or session Host is null
     */
    public void sessionInit(final IContextConfiguration sessionConfig)
            throws AutoProxyException {
        if (sessionConfig == null) {
            throw new AutoProxyException("Cannot initialize null session");
        }

        this.sessionConfig.copyValuesFrom(sessionConfig);
        this.archiveController.restartArchiveWithNewContext(this.sessionConfig);
        
        final AccessControlParameters acp = appContext.getBean(AccessControlParameters.class);


        acp.setKeytabFile(this.keytabFile);
        acp.setUserRole(this.userRole);
        acp.setUserId(this.username);

        if (this.sessionConfig.getContextId().getNumber() == null) {
            throw new AutoProxyException("Unable to determine session key");
        }

        if (this.sessionConfig.getContextId().getHost() == null) {
            throw new AutoProxyException("Unable to determine session host");
        }

        /*
         * Discovered when addressing this JIRA. Load
         * dictionaries explcitly since loading implicitly only works if we
         * don't reload.
         *
         * Adjusted command loading strategy for R8 changes.
         * Added calls to FlightDictionaryLoadingStrategy and ICommandUtilityDictionaryManager.
         * These are needed for AUTO send scmf endpoint when it reverses the sent scmf for logs
         */

        try {
            appContext.getBean(FlightDictionaryLoadingStrategy.class).enableCommand().loadAllEnabled(appContext, false, false);

            final ICommandUtilityDictionaryManager cm = appContext.getBean(ICommandUtilityDictionaryManager.class);
            if (!cm.isLoaded()) {
                cm.load(false);
            }
            appContext.getBean(ICommandDefinitionProvider.class);
          
        } catch (final Exception e) {
            throw new AutoProxyException(e);
        }
    }

    /**
     * Create a new AMPCS session
     * 
     * @param pathToSessionConfigXml the path to the AMPCS session configuration
     *            file
     * @return a SessionConfiguration object representing the newly created
     *         session
     * @throws AutoProxyException if session store cannot be started or an error
     *             occured while writing the session to the database
     */
    public IContextConfiguration createNewSession(
            final String pathToSessionConfigXml) throws AutoProxyException {

        final boolean validConfig = sessionConfig.load(pathToSessionConfigXml);
        // Load config into global SessionConfiguration object
        // instead of trying to create a new one, then load into globl.
        // start with blank session config

        if (!validConfig) {
            throw new AutoProxyException(
                    "AMPCS could not parse the Session Configuration file: "
                            + pathToSessionConfigXml);
        }

        // Access control params are already set at this
        // point. Getting them again from the context does nothing

        // Removed direct database access. Now
        // handled internally by IDbSqlArchiveController.

        sessionConfig.clearFieldsForNewConfiguration();
        return sessionConfig;
    }

    /**
     * Send an SCMF to the uplink host
     * 
     * @param pathToScmfFile
     *            The path to the SCMF file to transmit
     * @param validateScmf
     *            If checksums should be checked, false otherwise
     * @param timeout
     *            the number of seconds to wait before timing out
     * @throws RawOutputException
     *             If the output adapter can't be properly loaded
     * @throws ScmfParseException
     *             If the user input SCMF can't be parsed
     * @throws ScmfWrapUnwrapException
     *             If the SCMF has a formatting error
     * @throws UplinkException
     *             If the SCMF cannot be transmitted over the
     *             network
     * @throws DictionaryException
     *             If the command dictionary cannot be parsed
     * @throws IOException
     *             If the SCMF cannot be read properly
     * @throws AutoProxyException
     *             If session initialization failed
     * @throws AuthenticationException
     *             If user supplied credentials failed
     *             authentication
     * @throws TimeoutException
     *             If wait times out
     * @throws InvalidMetadataException
     *             if the metadata is invalid
     */
    public void sendScmf(final String pathToScmfFile,
            final boolean validateScmf, final int timeout)
            throws RawOutputException, ScmfParseException, ScmfWrapUnwrapException,
            UplinkException, DictionaryException, IOException,
            AutoProxyException, AuthenticationException, TimeoutException, InvalidMetadataException {
        this.checkSessionInit();
        this.initAccessControl();
        
        final ScmfProperties scmfConfig = appContext.getBean(ScmfProperties.class);

        /*
         * The issue that this change addresses was
         * discovered after upgrading to CSS V1.2. Not entirely sure of the
         * sequence of events or what changes caused AUTO to break, but it makes
         * more sense to create the UplinkRequestManager here anyway, since it
         * is only use when sending SCMFs
         */

        if (this.requestManager == null) {
            this.requestManager = new UplinkRequestManager();
        }

        /*
         * Create a unique ID to identify identical SCMFs sent within a
         * millisecond of each other
         */
        final int id =
                (this.generator.nextInt(MAX_RANDOM_ID_PREFIX) + pathToScmfFile + System
                        .currentTimeMillis()).hashCode();
        // temporarily use the input checksum setting
        final boolean oldDisableChecks =
        		scmfConfig.isDisableChecksums();
        scmfConfig.setDisableChecksums(!validateScmf);

        // get the necessary output adapter
        final UplinkConnectionType connectType = appContext.getBean(IConnectionMap.class).
        		getFswUplinkConnection().getUplinkConnectionType();
        final IRawOutputAdapter output = appContext.getBean(IRawOutputAdapterFactory.class).getUplinkOutput(connectType);

        // parse the input SCMF file
        final IScmf scmf = appContext.getBean(IScmfFactory.class).parse(pathToScmfFile); 

        final boolean validateCmdDict = scmfConfig.isDictionaryValidation();
        
        if (validateCmdDict) {
            // If validation is turned on, validate the command dictionary
            // version from the SCMF file (the command dictionary that was used
            // to generate
            // the SCMF) against the current command dictionary version
            final String scmfVersionId = scmf.getCommentField().trim();

            /*
             * Modified the CommandDefinitionTable to return the GDS
             * version ID for build and release IDs if they are null. This used
             * to be the other way around, basically.
             *
             */
            
            final String dictVersionId =
                    appContext.getBean(ICommandDefinitionProvider.class).getBuildVersionId();
            if (!scmfVersionId.equalsIgnoreCase(dictVersionId)
                    && !scmfConfig.isDisableChecksums()) {
                throw new ScmfVersionMismatchException(
                        "The FSW version ID \""
                                + scmfVersionId
                                + "\" ("
                                + scmf.getMacroVersion()
                                + ") in the input SCMF does not match the current command dictionary FSW version ID \""
                                + dictVersionId
                                + "\" ("
                                + appContext.getBean(DictionaryProperties.class)
                                + ").  If you want to transmit this SCMF anyway, you'll need to disable SCMF validity checks.");
            }
        }

        output.sendScmf(scmf, id);
        this.requestManager.waitForRadiation(id, timeout);

        // reset the checksum setting
        scmfConfig.setDisableChecksums(oldDisableChecks);
    }

    /**
     * Clean up opened resources
     * 
     * @throws AutoProxyException if stopping session store encounters errors.
     */
    @PreDestroy
    public void cleanUp() throws AutoProxyException {
        trace.debug("Shutting down AUTO manager ");
    	try {
            if (this.sessionConfig != null && archiveController != null) {
	            // write end session
	            final IEndSessionStore endSessionStore = archiveController.getEndSessionStore();
                if (endSessionStore != null) {
                    try {
                        trace.debug("Storing session ...");
                        endSessionStore.insertEndSession(this.sessionConfig.getContextId());
                        trace.debug("Session successfully stored");
                    } catch (final DatabaseException e) {
                        throw new AutoProxyException("Encountered error while trying to write end session to database: "
                                + ExceptionTools.getMessage(e), e);
                    }
                    trace.debug("Successfully wrote session to database");
                } else {
                    trace.debug("No session store found, unable to insert into archive service");
	            }
            } else {
                trace.debug("No SessionConfiguration or archive service detected. Unable to archive");
	        }
    	}
    	finally {
            if (archiveController != null && archiveController.isUp()) {
                trace.debug("Shutting down archive controller");
                this.archiveController.shutDown();
                trace.debug("Archive controller shut down");
            }
        }
    }
}
