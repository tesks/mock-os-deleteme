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
package jpl.gds.telem.down.app.mc;

import jpl.gds.common.config.TimeComparisonStrategy;
import jpl.gds.security.cli.rest.AbstractRestfulClientCommandLineApp;
import jpl.gds.shared.annotation.Operation;
import jpl.gds.shared.cli.app.mc.UnsafeOkHttpClient;
import jpl.gds.telem.down.app.mc.rest.api.AbortApi;
import jpl.gds.telem.down.app.mc.rest.api.BindApi;
import jpl.gds.telem.down.app.mc.rest.api.ClearladApi;
import jpl.gds.telem.down.app.mc.rest.api.CmdlineApi;
import jpl.gds.telem.down.app.mc.rest.api.DumpladApi;
import jpl.gds.telem.down.app.mc.rest.api.ExitApi;
import jpl.gds.telem.down.app.mc.rest.api.PauseApi;
import jpl.gds.telem.down.app.mc.rest.api.PerfApi;
import jpl.gds.telem.down.app.mc.rest.api.PropertiesApi;
import jpl.gds.telem.down.app.mc.rest.api.ResumeApi;
import jpl.gds.telem.down.app.mc.rest.api.SaveladApi;
import jpl.gds.telem.down.app.mc.rest.api.SessionApi;
import jpl.gds.telem.down.app.mc.rest.api.StartApi;
import jpl.gds.telem.down.app.mc.rest.api.StopApi;
import jpl.gds.telem.down.app.mc.rest.api.TelemApi;
import jpl.gds.telem.down.app.mc.rest.api.TimeCompStrategyApi;
import jpl.gds.telem.down.app.mc.rest.api.UnbindApi;
import jpl.gds.telem.down.app.mc.rest.invoker.ApiClient;
import jpl.gds.telem.down.app.mc.rest.invoker.ApiException;
import jpl.gds.telem.down.app.mc.rest.invoker.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Downlink Monitor and Control
 */
public class DownlinkMC extends AbstractRestfulClientCommandLineApp {
    /** clrlad sub-command */
    public static final String        CLRLAD_CMD = "clrlad";
    /** dmplad sub-command */
    public static final String        DMPLAD_CMD  = "dmplad";
    /** save lad content sub-command */
    public static final String        SAVLAD_CMD  = "savlad";
    /** tmcomp sub-command */
    public static final String        TMPOMP_CMD  = "tmcomp";

    /** savlad outfile option */
    public static final String        SAVLAD_OUTFILE_OPT  = "outfile";
    /** tmcomp strategy option */
    public static final String        TMCOMP_STRATEGY_OPT = "strategy";


    private final ApiClient           apiClient;
    private final AbortApi            abortApi;
    private final BindApi             bindApi;
    private final ClearladApi         clearladApi;
    private final CmdlineApi          cmdlineApi;
    private final DumpladApi          dumpladApi;
    private final ExitApi             exitApi;
    private final PauseApi            pauseApi;
    private final PerfApi             perfApi;
    private final PropertiesApi       propertiesApi;
    private final ResumeApi           resumeApi;
    private final SaveladApi          saveladApi;
    private final SessionApi          sessionApi;
    private final StartApi            startApi;
    private final StopApi             stopApi;
    private final TelemApi            telemApi;
    private final TimeCompStrategyApi timeCompStrategyApi;
    private final UnbindApi           unbindApi;

    /**
     * Constructor for command line applications. This creates a SIGTERM handler that implements the IQuitSignalHandler
     * interface
     */
    public DownlinkMC() {
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
    public DownlinkMC(final boolean addHook, final boolean attemptInsecureRetries) {
        super(addHook, attemptInsecureRetries);
        apiClient = Configuration.getDefaultApiClient();
        // TP stop times out when product assembly has not finished
        apiClient.getHttpClient().setConnectTimeout(MC_CLIENT_HTTP_CONNECTION_TIMEOUT_MINUTES, TimeUnit.MINUTES);
        apiClient.getHttpClient().setReadTimeout(MC_CLIENT_HTTP_CONNECTION_TIMEOUT_MINUTES, TimeUnit.MINUTES);
        apiClient.getHttpClient().setWriteTimeout(MC_CLIENT_HTTP_CONNECTION_TIMEOUT_MINUTES, TimeUnit.MINUTES);

        abortApi = new AbortApi(apiClient);
        bindApi = new BindApi(apiClient);
        clearladApi = new ClearladApi(apiClient);
        cmdlineApi = new CmdlineApi(apiClient);
        dumpladApi = new DumpladApi(apiClient);
        exitApi = new ExitApi(apiClient);
        pauseApi = new PauseApi(apiClient);
        perfApi = new PerfApi(apiClient);
        propertiesApi = new PropertiesApi(apiClient);
        resumeApi = new ResumeApi(apiClient);
        saveladApi = new SaveladApi(apiClient);
        sessionApi = new SessionApi(apiClient);
        startApi = new StartApi(apiClient);
        stopApi = new StopApi(apiClient);
        telemApi = new TelemApi(apiClient);
        timeCompStrategyApi = new TimeCompStrategyApi(apiClient);
        unbindApi = new UnbindApi(apiClient);
    }

    /**
     * @param args command line arguments
     */
    public static void main(final String[] args) {
        new DownlinkMC().process(args).exit();
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
     * @throws ApiException if an error occurs
     */
    @Operation(subCmd = DMPLAD_CMD, subCmdDesc = "Query the contents of the internal downlink LAD in JSON format")
    public Object dumpLad() throws ApiException {
        return dumpladApi.dumpLad();
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
    @Operation(subCmd = PROP_CMD, subCmdDesc = "Query downlink configuration in JSON format",
            optNames = {PROP_FILTER_OPT, "includeDescriptions", "includeSystem", "includeTemplateDirs"},
            parmNames = {"regex", "includeDescriptions", "includeSystem", "includeTemplateDirs"},
            parmTypes = {"STRING", "BOOLEAN", "BOOLEAN", "BOOLEAN"},
            parmDesc = {"A regular expression with which to filter results",
                    "If true, display descriptions, if false do not display descriptions",
                    "If true, display System Properties, if false do not display System Properties",
                    "If true, display Template Directories, if false do not display Template Directories"})
    public Object getProperties(final String regex, final Boolean includeDescriptions, final Boolean includeSystem,
                                final Boolean includeTemplateDirs)
            throws ApiException {
        return propertiesApi.getPropertiesStatus(regex, includeDescriptions, includeSystem, includeTemplateDirs);
    }

    /**
     * @return the results of the operation
     * @throws ApiException if an error occurs
     */
    @Operation(subCmd = PERF_CMD, subCmdDesc = "Query downlink performance status in JSON format")
    public Object getPerf() throws ApiException {
        return perfApi.getPerformanceStatus();
    }

    /**
     * @return the results of the operation
     * @throws ApiException if an error occurs
     */
    @Operation(subCmd = TELEM_CMD, subCmdDesc = "Query telemetry processing status in JSON format")
    public Object getTelem() throws ApiException {
        return telemApi.getTelemetryStatus();
    }

    /**
     * @return the results of the operation
     * @throws ApiException if an error occurs
     */
    @Operation(subCmd = SESSION_CMD, subCmdDesc = "Query session configuration metatdata in JSON format")
    public Object sessionConfig() throws ApiException {
        return sessionApi.getSessionConfiguration();
    }

    /**
     * @return the results of the operation
     * @throws ApiException if an error occurs
     */
    @Operation(subCmd = ABORT_CMD, subCmdDesc = "Abort telemetry processing", returnsJSON = false)
    public Object abort() throws ApiException {
        return abortApi.abort();
    }

    /**
     * @return the results of the operation
     * @throws ApiException if an error occurs
     */
    @Operation(subCmd = BIND_CMD, subCmdDesc = "Bind to telemetry input source (UNIMPLEMENTED)", returnsJSON = false)
    public Object bind() throws ApiException {
        return bindApi.bind();
    }

    /**
     * @return the results of the operation
     * @throws ApiException if an error occurs
     */
    @Operation(subCmd = CLRLAD_CMD, subCmdDesc = "Clear the downlink internal LAD", returnsJSON = false)
    public Object clearLad() throws ApiException {
        return clearladApi.clearLad();
    }

    /**
     * @return the results of the operation
     * @throws ApiException if an error occurs
     */
    @Operation(subCmd = EXIT_CMD, subCmdDesc = "Try to shut down gracefully, and then exit", returnsJSON = false)
    public Object exitDownlink() throws ApiException {
        return exitApi.exit();
    }

    /**
     * @return the results of the operation
     * @throws ApiException if an error occurs
     */
    @Operation(subCmd = PAUSE_CMD, subCmdDesc = "Pause processing telemetry", returnsJSON = false)
    public Object pause() throws ApiException {
        return pauseApi.pause();
    }

    /**
     * @return the results of the operation
     * @throws ApiException if an error occurs
     */
    @Operation(subCmd = RESUME_CMD, subCmdDesc = "Resume processing telemetry after pausing", returnsJSON = false)
    public Object resume() throws ApiException {
        return resumeApi.resume();
    }

    /**
     * @param filename the filename to which the LAD will be saved
     * @return the results of the operation
     * @throws ApiException if an error occurs
     */
    @Operation(subCmd = SAVLAD_CMD,
            subCmdDesc = "Save the downlink internal LAD to a file",
            optNames = {SAVLAD_OUTFILE_OPT},
            parmNames = {"filename"},
            parmTypes = {"WRITABLE_FILE"},
            parmDesc = {"The filename to which to write the contents of the chill_down internal LAD"},
            returnsJSON = false)
    public Object saveLad(final String filename) throws ApiException {
        return saveladApi.saveLad(filename);
    }

    /**
     * @param strategy the Time Strategy
     * @return the results of the operation
     * @throws ApiException if an error occurs
     */
    @Operation(subCmd = "tmcomp",
            subCmdDesc = "Set the current Time Comparison Strategy",
            optNames = {TMCOMP_STRATEGY_OPT},
            parmNames = {"strategy"},
            parmTypes = {"ENUM"},
            enumClassName = "jpl.gds.common.config.TimeComparisonStrategy",
            parmAllowedValues = {"SCLK,SCET,ERT,LAST_RECEIVED"},
            parmDesc = {"The new time configuration strategy"},
            returnsJSON = false)
    public Object setTimeCompStrategy(final TimeComparisonStrategy strategy) throws ApiException {
        return timeCompStrategyApi.setTimeComparisonStrategy(strategy.name());
    }

    /**
     * @return the results of the operation
     * @throws ApiException if an error occurs
     */
    @Operation(subCmd = START_CMD, subCmdDesc = "Start processing telemetry", returnsJSON = false)
    public Object start() throws ApiException {
        return startApi.start();
    }

    /**
     * @return the results of the operation
     * @throws ApiException if an error occurs
     */
    @Operation(subCmd = STOP_CMD, subCmdDesc = "Stop processing telemetry", returnsJSON = false)
    public Object stop() throws ApiException {
        return stopApi.stop();
    }

    /**
     * @return the results of the operation
     * @throws ApiException if an error occurs
     */
    @Operation(subCmd = UNBIND_CMD, subCmdDesc = "Unbind from telemetry input source (UNIMPLEMENTED)", returnsJSON = false)
    public Object unbind() throws ApiException {
        return unbindApi.unbind();
    }

}
