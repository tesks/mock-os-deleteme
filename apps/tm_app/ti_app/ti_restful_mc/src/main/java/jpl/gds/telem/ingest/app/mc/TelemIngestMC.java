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

package jpl.gds.telem.ingest.app.mc;

import jpl.gds.common.config.connection.options.ConnectionCommandOptions;
import jpl.gds.common.config.dictionary.options.DictionaryCommandOptions;
import jpl.gds.common.config.types.TelemetryConnectionType;
import jpl.gds.common.config.types.TelemetryInputType;
import jpl.gds.common.config.types.VenueType;
import jpl.gds.common.options.*;
import jpl.gds.context.api.options.ContextCommandOptions;
import jpl.gds.security.cli.rest.AbstractRestfulClientCommandLineApp;
import jpl.gds.session.config.options.SessionCommandOptions;
import jpl.gds.shared.annotation.Operation;
import jpl.gds.shared.cli.app.mc.UnsafeOkHttpClient;
import jpl.gds.telem.ingest.app.mc.rest.api.*;
import jpl.gds.telem.ingest.app.mc.rest.invoker.ApiClient;
import jpl.gds.telem.ingest.app.mc.rest.invoker.ApiException;
import jpl.gds.telem.ingest.app.mc.rest.invoker.Configuration;

import java.io.IOException;
import java.net.ConnectException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 *
 * Telemetry Ingestion MC (RESTful Client)
 *
 */
public class TelemIngestMC extends AbstractRestfulClientCommandLineApp {

    private final ApiClient         apiClient;
    private final CmdlineApi        cmdlineApi;
    private final ShutdownApi       shutdownApi;
    private final PerfApi           perfApi;
    private final QueryApi          queryApi;
    private final SessionApi        sessionApi;
    private final ContextApi        contextApi;
    private final SessioncreateApi  createApi;
    private final AttachApi         attachApi;
    private final StartApi          startApi;
    private final StopApi           stopApi;
    private final AbortApi          abortApi;
    private final ReleaseApi        releaseApi;
    private final TelemApi          telemApi;
    private final StateApi          stateApi;
    private final StatusApi         statusApi;
    private final WorkersApi        workersApi;


    /**
     * @param args
     *            command line arguments
     */
    public static void main(final String[] args) {
        new TelemIngestMC().process(args).exit();
    }

    /**
     * Constructor for command line applications. This creates a SIGTERM handler
     * that implements the IQuitSignalHandler interface
     */
    public TelemIngestMC() {
        this(true, false);
    }

    /**
     * Constructor for command line applications. This creates a SIGTERM handler
     * that implements the IQuitSignalHandler interface
     *
     * @param addHook
     *            whether or not to add the shutdown hook
     * @param attemptInsecureRetries
     *            if true, will attempt to retry in the event of an invalid SSL/TLS certificate or protocol error.
     *            if false, will not attempt this retry.
     */
    public TelemIngestMC(final boolean addHook, final boolean attemptInsecureRetries) {
        super(addHook, attemptInsecureRetries);
        apiClient = Configuration.getDefaultApiClient();
        // TP stop times out when product assembly has not finished
        apiClient.getHttpClient().setConnectTimeout(MC_CLIENT_HTTP_CONNECTION_TIMEOUT_MINUTES, TimeUnit.MINUTES);
        apiClient.getHttpClient().setReadTimeout(MC_CLIENT_HTTP_CONNECTION_TIMEOUT_MINUTES, TimeUnit.MINUTES);
        apiClient.getHttpClient().setWriteTimeout(MC_CLIENT_HTTP_CONNECTION_TIMEOUT_MINUTES, TimeUnit.MINUTES);

        abortApi = new AbortApi(apiClient);
        cmdlineApi = new CmdlineApi(apiClient);
        shutdownApi = new ShutdownApi(apiClient);
        perfApi = new PerfApi(apiClient);
        queryApi = new QueryApi(apiClient);
        sessionApi = new SessionApi(apiClient);
        contextApi = new ContextApi(apiClient);
        createApi = new SessioncreateApi(apiClient);
        attachApi = new AttachApi(apiClient);
        startApi = new StartApi(apiClient);
        stopApi = new StopApi(apiClient);
        releaseApi = new ReleaseApi(apiClient);
        telemApi = new TelemApi(apiClient);
        stateApi = new StateApi(apiClient);
        statusApi = new StatusApi(apiClient);
        workersApi = new WorkersApi(apiClient);
    }

    @Override
    public void setApiClientBaseURI(final String uri, final UnsafeOkHttpClient client) {
        /* Rest the URI components for the ApiClient instance */
        if (null != client) {
            this.apiClient.setHttpClient(client);
        }
        this.apiClient.setBasePath(getBaseURI());
    }

    @Override
    public void setApiClientHeader(final String key, final String value) {
        this.apiClient.addDefaultHeader(key,value);
    }

    @Override
    public String getApiClientBaseURI() {
        return this.apiClient.getBasePath();
    }

    /**
     * @return the results of the operation
     * @throws ApiException
     *             if an error occurs
     */
    @Operation(subCmd = CMDLINE_CMD, subCmdDesc = "Display the application's invoking command line in JSON format")
    public Object getCmdLine() throws ApiException {
        return cmdlineApi.getCommandLine();
    }

    /**
     * @param regex
     *            a regular expression with which to filter results
     * @param includeDescriptions
     *            if true, display descriptions, if false do not display discriptions
     * @param includeSystem
     *            if true, display System Properties, if false do not display System Properties
     * @param includeTemplateDirs
     *            if true, display Template Directories, if false do not display Template Directories
     * @return the results of the operation
     * @throws ApiException
     *             if an error occurs
     */
    @Operation(subCmd = QUERY_CMD, subCmdDesc = "Query downlink configuration in JSON format",
            optNames = { PROP_FILTER_OPT, "includeDescriptions", "includeSystem", "includeTemplateDirs" },
            parmNames = { "regex", "includeDescriptions", "includeSystem", "includeTemplateDirs" },
            parmTypes = { "STRING", "BOOLEAN", "BOOLEAN", "BOOLEAN" },
            parmDesc  = { "A regular expression with which to filter results",
                    "If present, display descriptions", "If present, display System Properties",
                    "If present, display Template Directories" })
    public Object queryConfig(final String regex, final Boolean includeDescriptions, final Boolean includeSystem,
                              final Boolean includeTemplateDirs)
            throws ApiException {
        return queryApi.queryConfiguration(regex, includeDescriptions, includeSystem, includeTemplateDirs);
    }

    /**
     * Calls service to get performance data for a session
     *
     * @param key Session key
     * @param host Session host
     * @param fragment Session fragment
     *
     * @return the results of the operation
     * @throws ApiException
     *             if an error occurs
     */
    @Operation(subCmd = PERF_CMD, subCmdDesc = "Query downlink performance status in JSON format",
            optNames = { KEY_PARAM, HOST_PARAM, FRAGMENT_PARAM},
            parmNames = { KEY_PARAM, HOST_PARAM, FRAGMENT_PARAM },
            parmTypes = { "LONG", "STRING", "INT" },
            parmDesc = { KEY_PARAM_DESC, HOST_PARAM_DESC, FRAGMENT_PARAM_DESC})
    public Object getPerf(final Long key, final String host, final Integer fragment) throws ApiException {
        return perfApi.getPerformanceStatus(key, host, fragment);
    }

    /**
     * Calls service to get telemetry data for a session
     *
     * @param key Session key
     * @param host Session host
     * @param fragment Session fragment
     *
     * @return the results of the operation
     * @throws ApiException
     *             if an error occurs
     */
    @Operation(subCmd = TELEM_CMD, subCmdDesc = "Query telemetry processing statistics in JSON format",
            optNames = { KEY_PARAM, HOST_PARAM, FRAGMENT_PARAM},
            parmNames = { KEY_PARAM, HOST_PARAM, FRAGMENT_PARAM },
            parmTypes = { "LONG", "STRING", "INT" },
            parmDesc = { KEY_PARAM_DESC, HOST_PARAM_DESC, FRAGMENT_PARAM_DESC})
    public Object getTelem(final Long key, final String host, final Integer fragment) throws ApiException {
        return telemApi.getTelemetryStatus(key, host, fragment);
    }

    /**
     * Calls service to start a previously created TI worker with the specified key, host and fragment
     *
     * @param key Session key
     * @param host Session host
     * @param fragment Session fragment
     *
     * @return the results of the operation
     * @throws ApiException
     *             if an error occurs
     */
    @Operation(subCmd = START_CMD, subCmdDesc = "Start worker with specified key, host and fragment",
            optNames = { KEY_PARAM, HOST_PARAM, FRAGMENT_PARAM},
            parmNames = { KEY_PARAM, HOST_PARAM, FRAGMENT_PARAM },
            parmTypes = { "LONG", "STRING", "INT" },
            parmDesc = { KEY_PARAM_DESC, HOST_PARAM_DESC, FRAGMENT_PARAM_DESC})
    public Object start(final Long key, final String host, final Integer fragment) throws ApiException {
        return startApi.start(key, host, fragment);
    }

    /**
     * Calls service to get a new session. Service inserts a new session entry in the
     * database using default mission configuration and returns the new session
     * identification (key and host)
     *
     *
     * @return the results of the operation
     * @throws ApiException
     *             if an error occurs
     */
    @Operation(subCmd = CREATE_SESSION_CMD, subCmdDesc = "Create new session using default mission configuration")
    public Object getNewSession() throws ApiException {
        return createApi.createSession();
    }

    /**
     * Calls service to get session configuration for a session
     *
     * @param key Session key
     * @param host Session host
     * @param fragment Session fragment
     *
     * @return the results of the operation
     * @throws ApiException
     *             if an error occurs
     */
    @Operation(subCmd = SESSION_CMD, subCmdDesc = "Query session in JSON format",
            optNames = { KEY_PARAM, HOST_PARAM, FRAGMENT_PARAM },
            parmNames = { KEY_PARAM, HOST_PARAM, FRAGMENT_PARAM },
            parmTypes = { "LONG", "STRING", "INT" },
            parmDesc = { KEY_PARAM_DESC, HOST_PARAM_DESC, FRAGMENT_PARAM_DESC})
    public List<Map<String, String>> getSessionConfig(final Long key, final String host, final Integer fragment) throws ApiException {
        return sessionApi.getSessionConfiguration(key, host, fragment);
    }

    /**
     * Calls service to get context configuration for Telemetry Ingestion Service
     *
     * @return the results of the operation
     * @throws ApiException
     *             if an error occurs
     */
    @Operation(subCmd = CONTEXT_CMD, subCmdDesc = "Query server context configuration in JSON format")
    public Map<String, String> getContextConfig() throws ApiException {
        return contextApi.getServerContextConfiguration();
    }

    /**
     * Calls service to get abort TI worker
     *
     * @param key Session key
     * @param host Session host
     * @param fragment Session fragment
     *
     * @return the results of the operation
     * @throws ApiException
     *             if an error occurs
     */
    @Operation(subCmd = ABORT_CMD, subCmdDesc = "Abort telemetry processing on a specified worker",
            optNames = { KEY_PARAM, HOST_PARAM, FRAGMENT_PARAM },
            parmNames = { KEY_PARAM, HOST_PARAM, FRAGMENT_PARAM },
            parmTypes = { "LONG", "STRING", "INT" },
            parmDesc = { KEY_PARAM_DESC, HOST_PARAM_DESC, FRAGMENT_PARAM_DESC})
    public Object abort(final Long key, final String host, final Integer fragment) throws ApiException {
        return abortApi.abort(key, host, fragment);
    }

    /**
     * Calls service to create a new TI worker and attach it to an existing session
     *
     * @param sessionKey the unique numeric identifier for a session
     * @param sessionHost the name of the host computing system executing the session
     * @param venueType Operational or test venue to use
     * @param downlinkConnectionType The connection type for telemetry input
     * @param inputFormat ource format of telemetry input; defaults based upon
     * @param downlinkStreamId downlink stream ID for TESTBED or ATLO
     * @param fswDictionaryDir FSW dictionary directory (may be command, telemetry or some other entity)
     * @param fswVersion flight software version
     * @param sseDictionaryDir SSE dictionary directory (may be command, telemetry or some other entity)
     * @param sseVersion simulation & support equipment software dictionary version
     * @param inputFile FSW downlink data input file or TDS PVL query file
     * @param fswDownlinkHost the host (source) computing system for flight software downlink
     * @param fswDownlinkPort network port to use for flight software downlink
     * @param sseHost host machine for system support or simulation equipment
     * @param sseDownlinkPort network port to use for downlinking from system support equipment software
     * @param dbSourceHost the name of the host for a database session to be used as telemetry data source
     * @param dbSourceKey the unique numeric identifier for a database session to be used as telemetry data source
     * @param spacecraftID spacecraft id; must be numeric
     * @param sessionDssId station identifier
     * @param sessionVcid input virtual channel ID
     * @param sessionName the name of the session
     * @param sessionUser the name of the user/entity executing the session
     * @param sessionDescription a description of the session
     * @param sessionType the type of the session
     * @param outputDir directory for saving session output files
     * @param subtopic name of the session realtime publication subtopic for OPS venues
     * @param topic the name of the topic for publication
     *
     * @return the results of the operation
     * @throws ApiException
     *             if an error occurs
     */

    @Operation(subCmd = ATTACH_CMD, subCmdDesc = "Create new TI worker",
            optNames = {
                    SessionCommandOptions.SESSION_KEY_LONG,
                    SessionCommandOptions.SESSION_HOST_LONG,
                    VenueTypeOption.LONG_OPTION,
                    TestbedNameOption.LONG_OPTION,
                    ConnectionCommandOptions.DOWNLINK_CONNECTION_LONG,
                    ConnectionCommandOptions.INPUT_TYPE_LONG,
                    DownlinkStreamTypeOption.LONG_OPTION,
                    DictionaryCommandOptions.FSW_DIR_LONG_OPT,
                    DictionaryCommandOptions.FSW_VERSION_LONG_OPT,
                    DictionaryCommandOptions.SSE_DIR_LONG_OPT,
                    DictionaryCommandOptions.SSE_VERSION_LONG_OPT,
                    ConnectionCommandOptions.INPUT_FILE_LONG,
                    ConnectionCommandOptions.FSW_DOWNLINK_HOST_LONG,
                    ConnectionCommandOptions.FSW_DOWNLINK_PORT_LONG,
                    ConnectionCommandOptions.SSE_HOST_LONG,
                    ConnectionCommandOptions.SSE_DOWNLINK_PORT_LONG,
                    ConnectionCommandOptions.DB_SOURCE_HOST_LONG,
                    ConnectionCommandOptions.DB_SOURCE_KEY_LONG,
                    SpacecraftIdOption.LONG_OPTION,
                    SessionCommandOptions.SESSION_DSSID_LONG,
                    SessionCommandOptions.SESSION_VCID_LONG,
                    SessionCommandOptions.SESSION_NAME_LONG,
                    SessionCommandOptions.SESSION_USER_LONG,
                    SessionCommandOptions.SESSION_DESC_LONG,
                    SessionCommandOptions.SESSION_TYPE_LONG,
                    SessionCommandOptions.OUTPUT_DIRECTORY_LONG,
                    SubtopicOption.LONG_OPTION,
                    ContextCommandOptions.PUBLISH_TOPIC_PARAM_LONG
            },
            parmNames = {
                    SessionCommandOptions.SESSION_KEY_LONG,
                    SessionCommandOptions.SESSION_HOST_LONG,
                    VenueTypeOption.LONG_OPTION,
                    TestbedNameOption.LONG_OPTION,
                    ConnectionCommandOptions.DOWNLINK_CONNECTION_LONG,
                    ConnectionCommandOptions.INPUT_TYPE_LONG,
                    DownlinkStreamTypeOption.LONG_OPTION,
                    DictionaryCommandOptions.FSW_DIR_LONG_OPT,
                    DictionaryCommandOptions.FSW_VERSION_LONG_OPT,
                    DictionaryCommandOptions.SSE_DIR_LONG_OPT,
                    DictionaryCommandOptions.SSE_VERSION_LONG_OPT,
                    ConnectionCommandOptions.INPUT_FILE_LONG,
                    ConnectionCommandOptions.FSW_DOWNLINK_HOST_LONG,
                    ConnectionCommandOptions.FSW_DOWNLINK_PORT_LONG,
                    ConnectionCommandOptions.SSE_HOST_LONG,
                    ConnectionCommandOptions.SSE_DOWNLINK_PORT_LONG,
                    ConnectionCommandOptions.DB_SOURCE_HOST_LONG,
                    ConnectionCommandOptions.DB_SOURCE_KEY_LONG,
                    SpacecraftIdOption.LONG_OPTION,
                    SessionCommandOptions.SESSION_DSSID_LONG,
                    SessionCommandOptions.SESSION_VCID_LONG,
                    SessionCommandOptions.SESSION_NAME_LONG,
                    SessionCommandOptions.SESSION_USER_LONG,
                    SessionCommandOptions.SESSION_DESC_LONG,
                    SessionCommandOptions.SESSION_TYPE_LONG,
                    SessionCommandOptions.OUTPUT_DIRECTORY_LONG,
                    SubtopicOption.LONG_OPTION,
                    ContextCommandOptions.PUBLISH_TOPIC_PARAM_LONG
            },
            parmTypes = {
                    "LONG",
                    "STRING",
                    "ENUM",
                    "STRING",
                    "ENUM",
                    "DYNAMIC_ENUM",
                    "ENUM",
                    "STRING",
                    "STRING",
                    "STRING",
                    "STRING",
                    "STRING",
                    "STRING",
                    "INT",
                    "STRING",
                    "INT",
                    "STRING",
                    "INT",
                    "INT",
                    "INT",
                    "INT",
                    "STRING",
                    "STRING",
                    "STRING",
                    "STRING",
                    "STRING",
                    "STRING",
                    "STRING"
            },
            enumClassName = {
                    "",
                    "",
                    "jpl.gds.common.config.types.VenueType",
                    "",
                    "jpl.gds.common.config.types.TelemetryConnectionType",
                    "jpl.gds.common.config.types.TelemetryInputType",
                    "jpl.gds.common.config.types.DownlinkStreamType",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    ""
            },
            parmAllowedValues = {
                    "",
                    "",
                    "ATLO,CRUISE,OPS,ORBIT,TESTBED,TESTSET,SURFACE",
                    "",
                    "CLIENT_SOCKET,DATABASE,FILE,NEN_SN_CLIENT,NEN_SN_SERVER,SERVER_SOCKET,TDS",
                    "LEOT_TF,RAW_PKT,RAW_TF,SFDU_TF,SFDU_PKT,CMD_ECHO,SLE_TF",
                    "TZ,LV,SELECTED_DL,SA,COMMAND_ECHO",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    "",
                    ""
            },
            parmDesc = {
                    SessionCommandOptions.SESSION_KEY_DESC,
                    SessionCommandOptions.SESSION_HOST_DESC,
                    VenueTypeOption.DESCRIPTION,
                    TestbedNameOption.DESCRIPTION,
                    ConnectionCommandOptions.DOWNLINK_CONNECTION_DESC,
                    ConnectionCommandOptions.DOWNLINK_INPUT_DESC,
                    DownlinkStreamTypeOption.DESCRIPTION,
                    DictionaryCommandOptions.FSW_DIR_DESC,
                    DictionaryCommandOptions.FSW_VER_DESC,
                    DictionaryCommandOptions.SSE_DIR_DESC,
                    DictionaryCommandOptions.SSE_VERSION_DESC,
                    ConnectionCommandOptions.DOWNLINK_INPUT_DESC,
                    ConnectionCommandOptions.FSW_DOWNLINK_HOST_DESC,
                    ConnectionCommandOptions.FSW_DOWNLINK_PORT_DESC,
                    ConnectionCommandOptions.SSE_HOST_DESC,
                    ConnectionCommandOptions.SSE_DOWNLINK_PORT_DESC,
                    ConnectionCommandOptions.DB_SOURCE_HOST_DESC,
                    ConnectionCommandOptions.DB_SOURCE_KEY_LONG,
                    SpacecraftIdOption.DESCRIPTION,
                    SessionCommandOptions.SESSION_DSSID_DESC,
                    SessionCommandOptions.SESSION_VCID_DESC,
                    SessionCommandOptions.SESSION_NAME_DESC,
                    SessionCommandOptions.SESSION_USER_DESC,
                    SessionCommandOptions.SESSION_DESC_DESC,
                    SessionCommandOptions.SESSION_TYPE_DESC,
                    SessionCommandOptions.OUTPUT_DIRECTORY_DESC,
                    SubtopicOption.DESCRIPTION,
                    ContextCommandOptions.PUBLISH_TOPIC_DESC
            })
    public Object attach(
            final Long sessionKey,
            final String sessionHost,
            final VenueType venueType,
            final String testbedName,
            final TelemetryConnectionType downlinkConnectionType,
            final TelemetryInputType inputFormat,
            final String downlinkStreamId,
            final String fswDictionaryDir,
            final String fswVersion,
            final String sseDictionaryDir,
            final String sseVersion,
            final String inputFile,
            final String fswDownlinkHost,
            final Integer fswDownlinkPort,
            final String sseHost,
            final Integer sseDownlinkPort,
            final String dbSourceHost,
            final Integer dbSourceKey,
            final Integer spacecraftID,
            final Integer sessionDssId,
            final Integer sessionVcid,
            final String sessionName,
            final String sessionUser,
            final String sessionDescription,
            final String sessionType,
            final String outputDir,
            final String subtopic,
            final String topic) throws ApiException {
        return attachApi.attach(sessionKey, sessionHost,
                                venueType != null ? venueType.name() : null,
                                testbedName,
                                downlinkConnectionType != null ? downlinkConnectionType.name() : null,
                                inputFormat != null ? inputFormat.name() : null,
                                downlinkStreamId,
                                fswDictionaryDir, fswVersion, sseDictionaryDir, sseVersion, inputFile, fswDownlinkHost,
                                fswDownlinkPort, sseHost, sseDownlinkPort,
                                dbSourceHost, dbSourceKey, spacecraftID, sessionDssId, sessionVcid, sessionName,
                                sessionUser, sessionDescription, sessionType, outputDir, subtopic, topic);
    }

    /**
     * Calls service to stop TI worker
     *
     * @param key Session key
     * @param host Session host
     * @param fragment Session fragment
     *
     * @return the results of the operation
     * @throws ApiException
     *             if an error occurs
     */
    @Operation(subCmd = STOP_CMD, subCmdDesc = "Stop processing telemetry on a specified session",
            optNames = { KEY_PARAM, HOST_PARAM, FRAGMENT_PARAM },
            parmNames = { KEY_PARAM, HOST_PARAM, FRAGMENT_PARAM },
            parmTypes = { "LONG", "STRING", "INT" },
            parmDesc = { KEY_PARAM_DESC, HOST_PARAM_DESC, FRAGMENT_PARAM_DESC})
    public Object stop(final Long key, final String host, final Integer fragment) throws ApiException {
        return stopApi.stop(key, host, fragment);
    }

    /**
     * Calls service to release TI worker
     *
     * @param key Session key
     * @param host Session host
     * @param fragment Session fragment
     *
     * @return the results of the operation
     * @throws ApiException
     *             if an error occurs
     */
    @Operation(subCmd = RELEASE_CMD,
            optNames = { KEY_PARAM, HOST_PARAM, FRAGMENT_PARAM},
            parmNames = { KEY_PARAM, HOST_PARAM, FRAGMENT_PARAM },
            parmTypes = { "LONG", "STRING", "INT" },
            parmDesc = { KEY_PARAM_DESC, HOST_PARAM_DESC, FRAGMENT_PARAM_DESC},
            subCmdDesc = "Release TI worker")
    public Object release(final Long key, final String host, final Integer fragment) throws ApiException {
        return releaseApi.release(key, host, fragment);
    }

    /**
     * Calls service to get TI worker state
     *
     * @param key Session key
     * @param host Session host
     * @param fragment Session fragment
     *
     * @return the results of the operation
     * @throws ApiException
     *             if an error occurs
     */
    @Operation(subCmd = STATE_CMD, subCmdDesc = "Gets the downlink telemetry processing state for a specified session",
            optNames = { KEY_PARAM, HOST_PARAM, FRAGMENT_PARAM },
            parmNames = { KEY_PARAM, HOST_PARAM, FRAGMENT_PARAM },
            parmTypes = { "LONG", "STRING", "INT" },
            parmDesc = { KEY_PARAM_DESC, HOST_PARAM_DESC, FRAGMENT_PARAM_DESC})
    public Object getState(final Long key, final String host, final Integer fragment) throws ApiException {
        return stateApi.getDownlinkProcessingState(key, host, fragment);
    }

    /**
     * Shutdown Telemetry Ingestor service
     *
     * @return the results of the operation
     */
    @Operation(subCmd = SHUTDOWN_CMD, subCmdDesc = "Try to shut down server gracefully and terminate the process")
    public Object shutdownServer() {
        try {
            return shutdownApi.execShutdown();
        }
        catch (final ApiException e) {
            // ApiResponse has connection error b/c server has been shutdown. Suppress it.
            if(e.getCause() instanceof ConnectException){
                return "Server is not up";
            }
            // ApiResponse has connection error b/c server has been shutdown. Suppress it.
            if(e.getCause() instanceof IOException){
                return "Shutdown request sent";
            }
            return e.getMessage();
        }
    }

    /**
     * Get the server status
     *
     * @return the result of the operation
     * @throws ApiException
     *             if an error occurs
     */
    @Operation(subCmd = STATUS_CMD, subCmdDesc = "Check if the server is up")
    public Object getStatus() throws ApiException {
        return statusApi.status();
    }

    /**
     * @return The results of the operation
     * @throws ApiException
     *             if an error occurs
     */
    @Operation(subCmd = WORKERS_CMD, subCmdDesc = "Retrieve lists of active worker IDs.")
    public Object workers() throws ApiException {
        return workersApi.getWorkers();
    }

    //TODO new method: logs (query string)

    //TODO new method: log (key, host, fragment, level, text)

}
