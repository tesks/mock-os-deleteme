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
package jpl.gds.telem.process.app.mc;


import jpl.gds.common.config.bootstrap.options.ChannelLadBootstrapCommandOptions;
import jpl.gds.common.config.dictionary.options.DictionaryCommandOptions;
import jpl.gds.context.api.options.ContextCommandOptions;
import jpl.gds.context.cli.app.mc.IRestfulClientCommandLineApp;
import jpl.gds.security.cli.rest.AbstractRestfulClientCommandLineApp;
import jpl.gds.shared.annotation.Operation;
import jpl.gds.shared.cli.app.mc.UnsafeOkHttpClient;
import jpl.gds.telem.process.app.mc.rest.api.*;
import jpl.gds.telem.process.app.mc.rest.invoker.ApiClient;
import jpl.gds.telem.process.app.mc.rest.invoker.ApiException;
import jpl.gds.telem.process.app.mc.rest.invoker.Configuration;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.ConnectException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Telemetry Processor MC (RESTful Client)
 *
 */
public class TelemProcessMC extends AbstractRestfulClientCommandLineApp {
    /** clear LAD sub-command */
    static final String        CLRLAD_CMD         = "clrlad";
    /** dump LAD sub-command */
    static final String        DMPLAD_CMD         = "dmplad";
    /** save LAD sub-command */
    static final String        SAVLAD_CMD         = "savlad";
    /** Time Comparison strategy sub-command */
    static final String        GET_TC_CMD         = "tmcomp";
    /** dictionaries sub-command */
    static final String        DICTS_CMD          = "dictionaries";
    /** dictionary sub-command */
    static final String        DICT_CMD           = "dictionary";
    /** output file sub-command */
    static final String        SAVLAD_OUTFILE_OPT = "outfile";
    /** attach to existing session sub-command */
    static final String        ATTACH_CMD         = "attach";
    /** tmcomp strategy option */
    public static final String SET_TC_CMD         = "strategy";

    private final ApiClient           apiClient;
    private final ClearladApi         clearladApi;
    private final CmdlineApi          cmdlineApi;
    private final DumpladApi          dumpladApi;
    private final ShutdownApi         shutdownApi;
    private final PerfApi             perfApi;
    private final QueryApi            queryApi;
    private final SaveladApi          saveladApi;
    private final SessionApi          sessionApi;
    private final ContextApi          contextApi;
    private final AttachApi           attachApi;
    private final StartApi            startApi;
    private final StopApi             stopApi;
    private final AbortApi            abortApi;
    private final ReleaseApi          releaseApi;
    private final TelemApi            telemApi;
    private final TimeComparisonStrategyApi timeCompStrategyApi;
    private final DictionaryApi       dictionaryApi;
    private final DictionariesApi     dictionariesApi;
    private final WorkersApi          workersApi;
    private final StatusApi           statusApi;
    private final StateApi            stateApi;
    private final TmcompApi           tmcompApi;
    /**
     * Constructor for command line applications. This creates a SIGTERM handler that implements the IQuitSignalHandler
     * interface
     */
    public TelemProcessMC() {
        this(true, false);
    }

    /**
     * Constructor for command line applications. This creates a SIGTERM handler that implements the IQuitSignalHandler
     * interface
     *
     * @param addHook                whether or not to add the shutdown hook
     * @param attemptInsecureRetries if true, will attempt to retry in the event of an invalid SSL/TLS certificate or
     *                               protocol error. if false, will not attempt this retry.
     */
    public TelemProcessMC(final boolean addHook, final boolean attemptInsecureRetries) {
        super(addHook, attemptInsecureRetries);
        apiClient = Configuration.getDefaultApiClient();
        // TP stop times out when product assembly has not finished
        apiClient.getHttpClient().setConnectTimeout(MC_CLIENT_HTTP_CONNECTION_TIMEOUT_MINUTES, TimeUnit.MINUTES);
        apiClient.getHttpClient().setReadTimeout(MC_CLIENT_HTTP_CONNECTION_TIMEOUT_MINUTES, TimeUnit.MINUTES);
        apiClient.getHttpClient().setWriteTimeout(MC_CLIENT_HTTP_CONNECTION_TIMEOUT_MINUTES, TimeUnit.MINUTES);

        clearladApi = new ClearladApi(apiClient);
        cmdlineApi = new CmdlineApi(apiClient);
        dumpladApi = new DumpladApi(apiClient);
        shutdownApi = new ShutdownApi(apiClient);
        perfApi = new PerfApi(apiClient);
        queryApi = new QueryApi(apiClient);
        saveladApi = new SaveladApi(apiClient);
        sessionApi = new SessionApi(apiClient);
        contextApi = new ContextApi(apiClient);
        attachApi = new AttachApi(apiClient);
        startApi = new StartApi(apiClient);
        stopApi = new StopApi(apiClient);
        abortApi = new AbortApi(apiClient);
        releaseApi = new ReleaseApi(apiClient);
        telemApi = new TelemApi(apiClient);
        dictionaryApi = new DictionaryApi(apiClient);
        dictionariesApi = new DictionariesApi(apiClient);
        workersApi = new WorkersApi(apiClient);
        statusApi = new StatusApi(apiClient);
        stateApi = new StateApi(apiClient);
        timeCompStrategyApi = new TimeComparisonStrategyApi(apiClient); // used for GETTING TC Strategy
        tmcompApi = new TmcompApi(apiClient); // used for SETTING TC Strategy
    }

    /**
     * @param args command line arguments
     */
    public static void main(final String[] args) {
        new TelemProcessMC().process(args).exit();
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
     * Calls service to create a TP worker and attach it to the session with the specified key and host
     *
     * @param key Session key
     * @param host Session host
     * @return the results of the operation
     * @throws ApiException
     *             if an error occurs
     */
    @Operation(subCmd = ATTACH_CMD,
            optNames = {
                    KEY_PARAM,
                    HOST_PARAM,
                    DictionaryCommandOptions.FSW_DIR_LONG_OPT,
                    DictionaryCommandOptions.FSW_VERSION_LONG_OPT,
                    DictionaryCommandOptions.SSE_DIR_LONG_OPT,
                    DictionaryCommandOptions.SSE_VERSION_LONG_OPT,
                    ChannelLadBootstrapCommandOptions.BOOTSTRAP_CHANNEL_LAD_LONG,
                    ChannelLadBootstrapCommandOptions.BOOTSTRAP_CHANNEL_LAD_IDS_LONG,
                    ContextCommandOptions.PUBLISH_TOPIC_PARAM_LONG },
            parmNames = {
                    KEY_PARAM,
                    HOST_PARAM,
                    DictionaryCommandOptions.FSW_DIR_LONG_OPT,
                    DictionaryCommandOptions.FSW_VERSION_LONG_OPT,
                    DictionaryCommandOptions.SSE_DIR_LONG_OPT,
                    DictionaryCommandOptions.SSE_VERSION_LONG_OPT,
                    ChannelLadBootstrapCommandOptions.BOOTSTRAP_CHANNEL_LAD_LONG,
                    ChannelLadBootstrapCommandOptions.BOOTSTRAP_CHANNEL_LAD_IDS_LONG,
                    ContextCommandOptions.PUBLISH_TOPIC_PARAM_LONG},
            parmTypes = { "LONG", "STRING",  "STRING", "STRING", "STRING", "STRING", "BOOLEAN", "STRING", "STRING"},
            parmDesc = {
                    KEY_PARAM_DESC,
                    HOST_PARAM_DESC,
                    DictionaryCommandOptions.FSW_DIR_DESC,
                    DictionaryCommandOptions.FSW_VER_DESC,
                    DictionaryCommandOptions.SSE_DIR_DESC,
                    DictionaryCommandOptions.SSE_VERSION_DESC,
                    ChannelLadBootstrapCommandOptions.BOOTSTRAP_CHANNEL_LAD_DESC,
                    ChannelLadBootstrapCommandOptions.BOOTSTRAP_CHANNEL_LAD_IDS_DESC,
                    ContextCommandOptions.PUBLISH_TOPIC_DESC},
            subCmdDesc = "Create a new worker and attach it to the specified session")
    public Object attach(final Long key, final String host, final String fswDictionaryDir, final String fswVersion,
                         final String sseDictionaryDir, final String sseVersion,
                         final boolean bootstrapLad, final String bootstrapLadIds, final String topic) throws ApiException {
        return attachApi.attach(key, host, fswDictionaryDir, fswVersion, sseDictionaryDir, sseVersion,
                                bootstrapLad, bootstrapLadIds, topic);
    }

    /**
     * Calls service to tart TP Worker associated with the specified session identifier
     *
     * @param key Session key
     * @param host Session host
     * @param fragment Session fragment
     * @return the results of the operation
     * @throws ApiException
     *             if an error occurs
     */
    @Operation(subCmd = START_CMD,
            optNames = { KEY_PARAM, HOST_PARAM, FRAGMENT_PARAM},
            parmNames = { KEY_PARAM, HOST_PARAM, FRAGMENT_PARAM},
            parmTypes = { "LONG", "STRING", "INT" },
            parmDesc = { KEY_PARAM_DESC, HOST_PARAM_DESC, FRAGMENT_PARAM_DESC},
            subCmdDesc = "Start worker with specified key, host and fragment")
    public Object start(final Long key, final String host, final Integer fragment) throws ApiException {
        return startApi.start(key, host, fragment);
    }

    /**
     * @param key Session key
     * @param host Session host
     * @param fragment Session fragment
     * @return the results of the operation
     * @throws ApiException
     *             if an error occurs
     */
    @Operation(subCmd = DMPLAD_CMD,
            optNames = { KEY_PARAM, HOST_PARAM, FRAGMENT_PARAM},
            parmNames = { KEY_PARAM, HOST_PARAM, FRAGMENT_PARAM },
            parmTypes = { "LONG", "STRING", "INT" },
            subCmdDesc = "Query the contents of a specified session's internal downlink LAD in JSON format",
            parmDesc = { KEY_PARAM_DESC, HOST_PARAM_DESC, FRAGMENT_PARAM_DESC})
    public Object dumpLad(final Long key, final String host, final Integer fragment) throws ApiException {
        return dumpladApi.dumpLad(key, host, fragment);
    }

    /**
     * @return the results of the operation
     * @throws ApiException if an error occurs
     */
    @Operation(subCmd = CMDLINE_CMD, subCmdDesc = "Display the downlink's invoking command line in JSON format")
    public Object getCmdLine() throws ApiException {
        return cmdlineApi.getCommandLine();
    }

    /**
     * @param regex               a regular expression with which to filter results
     * @param includeDescriptions if true, display descriptions, if false do not display discriptions
     * @param includeSystem       if true, display System Properties, if false do not display System Properties
     * @param includeTemplateDirs if true, display Template Directories, if false do not display Template Directories
     * @return the results of the operation
     * @throws ApiException if an error occurs
     */
    @Operation(subCmd = QUERY_CMD, subCmdDesc = "Query downlink configuration in JSON format",
            optNames = {PROP_FILTER_OPT, "includeDescriptions", "includeSystem", "includeTemplateDirs"},
            parmNames = {"regex", "includeDescriptions", "includeSystem", "includeTemplateDirs"},
            parmTypes = {"STRING", "BOOLEAN", "BOOLEAN", "BOOLEAN"},
            parmDesc = {"A regular expression with which to filter results",
                    "If present, display property descriptions", "If present, display System Properties",
                    "If present, display Template Directories" })
    public Object queryConfig(final String regex, final Boolean includeDescriptions, final Boolean includeSystem,
                              final Boolean includeTemplateDirs)
            throws ApiException {
        return queryApi.queryConfiguration(regex, includeDescriptions, includeSystem, includeTemplateDirs);
    }

    /**
     * Calls service to retrieve service performance
     *
     * @param key Session key
     * @param host Session host
     * @param fragment Session fragment
     *
     * @return the results of the operation
     * @throws ApiException if an error occurs
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
     * Calls service to retrieve processing statistics
     *
     * @param key Session key
     * @param host Session host
     * @param fragment Session fragment
     * @return the results of the operation
     * @throws ApiException
     *             if an error occurs
     */
    @Operation(subCmd = TELEM_CMD,
            optNames = { KEY_PARAM, HOST_PARAM, FRAGMENT_PARAM},
            parmNames = { KEY_PARAM, HOST_PARAM, FRAGMENT_PARAM },
            parmTypes = { "LONG", "STRING", "INT" },
            parmDesc = { KEY_PARAM_DESC, HOST_PARAM_DESC, FRAGMENT_PARAM_DESC},
            subCmdDesc = "Query telemetry processing statistics of a specified session in JSON format")
    public Object getTelem(final Long key, final String host, final Integer fragment) throws ApiException {
        return telemApi.getTelemetryStatus(key, host, fragment);
    }

    /**
     * @param key Session key
     * @param host Session host
     * @param fragment Session fragment
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
     * Calls service to get context configuration for Telemetry Processor Service
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
     * @param key Session key
     * @param host Session host
     * @param fragment Session fragment
     *
     * @return the results of the operation
     * @throws ApiException
     *             if an error occurs
     */
    @Operation(subCmd = CLRLAD_CMD,
            optNames = { KEY_PARAM, HOST_PARAM, FRAGMENT_PARAM},
            parmNames = { KEY_PARAM, HOST_PARAM, FRAGMENT_PARAM },
            parmTypes = { "LONG", "STRING", "INT" },
            parmDesc = { KEY_PARAM_DESC, HOST_PARAM_DESC, FRAGMENT_PARAM_DESC},
            subCmdDesc = "Clear the downlink internal LAD for the specified ID")
    public Object clearLad(final Long key, final String host, final Integer fragment) throws ApiException {
        return clearladApi.clearLad(key, host, fragment);
    }

    /**
     * Calls service to save LAD for a given session
     * @param key Session key
     * @param host Session host
     * @param fragment Session fragment
     * @param filename
     *            the filename to which the LAD will be saved
     * @return the results of the operation
     * @throws ApiException
     *             if an error occurs
     */
    @Operation(subCmd = SAVLAD_CMD,
            subCmdDesc = "Save the downlink internal LAD to a file",

            optNames = { KEY_PARAM, HOST_PARAM, FRAGMENT_PARAM, SAVLAD_OUTFILE_OPT},
            parmNames = { KEY_PARAM, HOST_PARAM, FRAGMENT_PARAM, "filename"},
            parmTypes = { "LONG", "STRING", "INT", "WRITABLE_FILE" },
            parmDesc = { KEY_PARAM_DESC, HOST_PARAM_DESC, FRAGMENT_PARAM_DESC,
                    "The filename to which to write the contents of the chill_telem_process internal LAD"})
    public Object saveLad(final Long key, final String host, final Integer fragment, final String filename) throws ApiException {
        return saveladApi.saveLad(key, host, fragment, filename);
    }

    /**
     * Gets the currently configured Time Comparison Strategy
     *
     * @return the results of the operation
     * @throws ApiException if an error occurs
     */
    @Operation(subCmd = GET_TC_CMD, subCmdDesc = "Get the current Time Comparison Strategy for a specified processor",
            optNames = { KEY_PARAM, HOST_PARAM, FRAGMENT_PARAM },
            parmNames = { KEY_PARAM, HOST_PARAM, FRAGMENT_PARAM },
            parmTypes = { "LONG", "STRING", "INT"},
            parmDesc = { KEY_PARAM_DESC, HOST_PARAM_DESC, FRAGMENT_PARAM_DESC })
    public Object getTimeCompStrategy(final Long key, final String host, final Integer fragment) throws ApiException {
        return timeCompStrategyApi.getTimeComparisonStrategy(key, host, fragment);
    }

    /**
     * Sets the Time Comparison Strategy
     *
     * @return the results of the operation
     * @throws ApiException if an error occurs
     */
    @Operation(subCmd = SET_TC_CMD,
            subCmdDesc = "Set the current Time Comparison Strategy on a specified processor",
            optNames = { KEY_PARAM, HOST_PARAM, FRAGMENT_PARAM, GET_TC_CMD },
            parmNames = { KEY_PARAM, HOST_PARAM, FRAGMENT_PARAM, SET_TC_CMD },
            parmTypes = { "LONG", "STRING", "INT", "STRING"},
            parmDesc = { KEY_PARAM_DESC, HOST_PARAM_DESC, FRAGMENT_PARAM_DESC, "LAST_RECEIVED,SCLK,SCET,ERT"})
    public Object setTimeCompStrategy(final Long key, final String host, final Integer fragment,
                                      final String strategy) throws ApiException {
        return tmcompApi.setTimeComp(key, host, fragment, strategy);
    }

    /**
     * Calls service to stop TP Worker
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
            optNames = { KEY_PARAM, HOST_PARAM, FRAGMENT_PARAM},
            parmNames = { KEY_PARAM, HOST_PARAM, FRAGMENT_PARAM },
            parmTypes = { "LONG", "STRING", "INT" },
            parmDesc = { KEY_PARAM_DESC, HOST_PARAM_DESC, FRAGMENT_PARAM_DESC})
    public Object stop(final Long key, final String host, final Integer fragment) throws ApiException {
        return stopApi.stop(key, host, fragment);
    }

    /**
     * Calls service to abort TP worker
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
     * Calls service to release TP worker
     *
     * @param key Session key
     * @param host Session host
     * @param fragment Session fragment
     *
     * @return the results of the operation
     * @throws ApiException
     *             if an error occurs
     */
    @Operation(subCmd = RELEASE_CMD, subCmdDesc = "Release TP worker",
            optNames = { KEY_PARAM, HOST_PARAM, FRAGMENT_PARAM},
            parmNames = { KEY_PARAM, HOST_PARAM, FRAGMENT_PARAM },
            parmTypes = { "LONG", "STRING", "INT" },
            parmDesc = { KEY_PARAM_DESC, HOST_PARAM_DESC, FRAGMENT_PARAM_DESC})
    public Object release(final Long key, final String host, final Integer fragment) throws ApiException {
        return releaseApi.release(key, host, fragment);
    }


    /**
     * Call service to get available dictionaries
     *
     * @param dir Directory to search (optional)
     * @return the results of the operation
     * @throws ApiException If an error occurs
     * @throws UnsupportedEncodingException if dir is encoded with unsupported encoding
     */
    @Operation(subCmd = DICTS_CMD,
            optNames = {"directory"},
            parmNames = {"dir"},
            parmTypes = {"STRING"},
            parmDesc = {"The directory to search for dictionaries"},
            subCmdDesc = "Retrieve a list of available dictionaries, searching a given directory if one is provided.")
    public Object dictionaries(final String dir) throws ApiException, UnsupportedEncodingException {
        // Use URL encoded strings instead of Base64
        if(StringUtils.isNotBlank(dir)) {
            return dictionariesApi.getAvailableDictionaries(URLEncoder.encode(dir, StandardCharsets.UTF_8.name()));
        }
        else{
            return dictionariesApi.getAvailableDictionaries(null);
        }
    }

    /**
     * @param name
     *            Dictionary name
     * @param dir
     *            Dictionary directory
     * @return The result of the operation
     * @throws ApiException
     *             If an error occurs
     */
    @Operation(subCmd = DICT_CMD,
            optNames = {"name", "directory"},
            parmNames = {"name", "dir"},
            parmTypes = {"STRING", "STRING"},
            parmDesc = {"Dictionary name", "Dictionary directory."},
            subCmdDesc = "Instruct the telemetry processing server to cache a particular dictionary.")
    public Object dictionary(final String name, final String dir) throws ApiException {
        return dictionaryApi.cacheDictionary(name, dir);
    }

    /**
     * Calls service to retrieve list of TP workers
     *
     * @return The results of the operation
     * @throws ApiException
     *             if an error occurs
     */
    @Operation(subCmd = WORKERS_CMD,
            subCmdDesc = "Retrieve lists of TP workers")
    public Object workers() throws ApiException {
        return workersApi.getWorkers();
    }

    /**
     * Get the server status
     *
     * @return the result of the operation
     * @throws ApiException
     *             if an error occurs
     */
    @Operation(subCmd = IRestfulClientCommandLineApp.STATUS_CMD, subCmdDesc = "Check if the server is up")
    public Object getStatus() throws ApiException {
        return statusApi.status();
    }

    /**
     * Shutdown Telemetry Processor service
     *
     * @return the results of the operation
     */
    @Operation(subCmd = SHUTDOWN_CMD, subCmdDesc = "Try to shut down server gracefully and terminate the process")
    public Object shutdownServer() {
        try {
            return shutdownApi.execShutdown();
        }
        catch (final ApiException e) {
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
     * Calls service to get TP worker state
     *
     * @param key Session key
     * @param host Session host
     * @param fragment Session fragment
     *
     * @return the results of the operation
     * @throws ApiException
     *             if an error occurs
     */
    @Operation(subCmd = STATE_CMD, subCmdDesc = "Gets the processing state for a specified TP worker",
            optNames = { KEY_PARAM, HOST_PARAM, FRAGMENT_PARAM},
            parmNames = { KEY_PARAM, HOST_PARAM, FRAGMENT_PARAM },
            parmTypes = { "LONG", "STRING", "INT" },
            parmDesc = { KEY_PARAM_DESC, HOST_PARAM_DESC, FRAGMENT_PARAM_DESC})
    public Object getState(final Long key, final String host, final Integer fragment) throws ApiException {
        return stateApi.getDownlinkProcessingState(key, host, fragment);
    }

    //TODO new method: logs (query string)

    //TODO new method: properties (key, host, fragment)

    //TODO new method: suspect-channels (key, host, fragment)

    //TODO new method: log (key, host, fragment, level, text)

    //TODO new method: suspect-channels (key, host, fragment, channelID array)
}