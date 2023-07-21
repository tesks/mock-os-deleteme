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
package jpl.gds.telem.ingest.app.mc.rest.controllers;

import io.swagger.annotations.*;
import jpl.gds.common.config.connection.options.ConnectionCommandOptions;
import jpl.gds.common.config.dictionary.options.DictionaryCommandOptions;
import jpl.gds.common.config.types.DownlinkStreamType;
import jpl.gds.common.config.types.TelemetryConnectionType;
import jpl.gds.common.config.types.TelemetryInputType;
import jpl.gds.common.config.types.VenueType;
import jpl.gds.common.options.*;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.context.api.options.ContextCommandOptions;
import jpl.gds.context.api.options.ContextCommandOptionsCliHelper;
import jpl.gds.context.cli.app.mc.IRestfulClientCommandLineApp;
import jpl.gds.session.config.options.SessionCommandOptions;
import jpl.gds.shared.exceptions.ApplicationException;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.exceptions.RestfulTelemetryException;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.metadata.context.IContextKey;
import jpl.gds.shared.string.StringResponse;
import jpl.gds.telem.common.ITelemetryWorker;
import jpl.gds.telem.common.app.mc.rest.controllers.AbstractRestfulTlmController;
import jpl.gds.telem.common.app.mc.rest.resources.SessionCreateResponse;
import jpl.gds.telem.common.app.mc.rest.resources.WorkerStateChangeResponse;
import jpl.gds.telem.common.state.WorkerStateChangeAction;
import jpl.gds.telem.ingest.IngestServerManagerApp;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import javax.annotation.PostConstruct;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;

import static jpl.gds.context.cli.app.mc.IRestfulClientCommandLineApp.*;

/**
 * Telem Ingest Control REST API
 *
 * As much as possible, return actual objects from the REST API calls, rather than ResponseEntity
 * Spring will serialize these automatically
 *
 * All APIs should return JSON
 *
 */
@RestController
@ResponseBody
@RequestMapping("/ingest/control")
@Api(value = "control")
@EnableSwagger2
public class RestfulTelemetryIngestorControl extends AbstractRestfulTlmController {

    private static final String SESSION_CREATE_URL     = IRestfulClientCommandLineApp.SESSION_CMD
            + "/" + IRestfulClientCommandLineApp.CREATE_CMD;

    private final IngestServerManagerApp manager;

    /**
     * Telemetry Ingestor REST controller contstructor
     *
     * @param ingestApp
     *            <IIngestApp> Telemetry Ingestor application
     */
    public RestfulTelemetryIngestorControl(final ApplicationContext applicationContext,
                                           final IngestServerManagerApp ingestApp) {
        super(applicationContext);
        this.manager = ingestApp;
    }

    @PostConstruct
    public void init() {
        log = TraceManager.getTracer(appContext, Loggers.INGEST);
    }

    /**
     * Create new Session. This operation inserts a new session entry in the database
     * using the default mission configuration and returns the new session
     * identification (session key and host). Basically provides a way to obtain a new
     * session number.
     *
     * We made a decision to split session creation
     * from TI worker creation. This work was done with the intention of eventually moving
     * this capability into its own service (Session Service).
     *
     * @return the new session identification
     *
     */
    @PostMapping(value = SESSION_CREATE_URL, produces = MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Creates a new session.", tags = SESSION_CREATE_URL)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "New Session Created Successfully"),
            @ApiResponse(code = 500, message = INTERNAL_SERVER_ERROR) })
    public SessionCreateResponse createSession() {

        final IContextIdentification sessionContext = manager.obtainNewSessionNumber();
        return new SessionCreateResponse("SESSION CREATED", sessionContext.getContextKey().getNumber(), sessionContext.getContextKey().getHost());
    }

    /**
     * Create a TI worker and attach it to the specified session
     *
     * @param sessionKey             the unique numeric identifier for a session
     * @param sessionHost            the name of the host computing system executing the session
     * @param venueType              Operational or test venue to use
     * @param testbedName            Optional testbed name to specify
     * @param downlinkConnectionType The connection type for telemetry input
     * @param inputFormat            ource format of telemetry input; defaults based upon
     * @param downlinkStreamId       downlink stream ID for TESTBED or ATLO
     * @param fswDictionaryDir       FSW dictionary directory (may be command, telemetry or some other entity)
     * @param fswVersion             flight software version
     * @param sseDictionaryDir       SSE dictionary directory (may be command, telemetry or some other entity)
     * @param sseVersion             simulation & support equipment software dictionary version
     * @param inputFile              FSW downlink data input file or TDS PVL query file
     * @param fswDownlinkHost        the host (source) computing system for flight software downlink
     * @param fswDownlinkPort        network port to use for flight software downlink
     * @param sseHost                host machine for system support or simulation equipment
     * @param sseDownlinkPort        network port to use for downlinking from system support equipment software
     * @param dbSourceHost           the name of the host for a database session to be used as telemetry data source
     * @param dbSourceKey            the unique numeric identifier for a database session to be used as telemetry data source
     * @param spacecraftID           spacecraft id; must be numeric
     * @param sessionDssId           station identifier
     * @param sessionVcid            input virtual channel ID
     * @param sessionName            the name of the session
     * @param sessionUser            the name of the user/entity executing the session
     * @param sessionDescription     a description of the session
     * @param sessionType            the type of the session
     * @param outputDir              directory for saving session output files
     * @param subtopic               name of the session realtime publication subtopic for OPS venues
     * @return result of create operation
     */
    @PostMapping(value = ATTACH_CMD, produces = MediaType.APPLICATION_JSON) @ApiOperation(value = "Creates a TI Worker.", tags = ATTACH_CMD)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "TI Worker Created Successfully"),
            @ApiResponse(code = 500, message = INTERNAL_SERVER_ERROR) })
    public WorkerStateChangeResponse attach(
            @RequestParam(value = SessionCommandOptions.SESSION_KEY_LONG)
            @ApiParam(value = SessionCommandOptions.SESSION_KEY_DESC) final long sessionKey,
            @RequestParam(value = SessionCommandOptions.SESSION_HOST_LONG)
            @ApiParam(value = SessionCommandOptions.SESSION_HOST_DESC) final String sessionHost,
            @RequestParam(value = VenueTypeOption.LONG_OPTION, required = false)
            @ApiParam(value = VenueTypeOption.DESCRIPTION) final VenueType venueType,
            @RequestParam(value = TestbedNameOption.LONG_OPTION, required = false)
            @ApiParam(value = TestbedNameOption.DESCRIPTION) final String testbedName,
            @RequestParam(value = ConnectionCommandOptions.DOWNLINK_CONNECTION_LONG, required = false)
            @ApiParam(value = ConnectionCommandOptions.DOWNLINK_CONNECTION_DESC) final TelemetryConnectionType downlinkConnectionType,
            @RequestParam(value = ConnectionCommandOptions.INPUT_TYPE_LONG, required = false)
            @ApiParam(value = ConnectionCommandOptions.DOWNLINK_INPUT_DESC, required = false) final TelemetryInputType inputFormat,
            @RequestParam(value = DownlinkStreamTypeOption.LONG_OPTION, required = false)
            @ApiParam(value = DownlinkStreamTypeOption.DESCRIPTION, required = false) final String downlinkStreamId,
            @RequestParam(value = DictionaryCommandOptions.FSW_DIR_LONG_OPT, required = false)
            @ApiParam(value = DictionaryCommandOptions.FSW_DIR_DESC) final String fswDictionaryDir,
            @RequestParam(value = DictionaryCommandOptions.FSW_VERSION_LONG_OPT, required = false)
            @ApiParam(value = DictionaryCommandOptions.FSW_VER_DESC) final String fswVersion,
            @RequestParam(value = DictionaryCommandOptions.SSE_DIR_LONG_OPT, required = false)
            @ApiParam(value = DictionaryCommandOptions.SSE_DIR_DESC) final String sseDictionaryDir,
            @RequestParam(value = DictionaryCommandOptions.SSE_VERSION_LONG_OPT, required = false)
            @ApiParam(value = DictionaryCommandOptions.SSE_VERSION_DESC) final String sseVersion,
            @RequestParam(value = ConnectionCommandOptions.INPUT_FILE_LONG, required = false)
            @ApiParam(value = ConnectionCommandOptions.DOWNLINK_INPUT_DESC) final String inputFile,
            @RequestParam(value = ConnectionCommandOptions.FSW_DOWNLINK_HOST_LONG, required = false)
            @ApiParam(value = ConnectionCommandOptions.FSW_DOWNLINK_HOST_DESC) final String fswDownlinkHost,
            @RequestParam(value = ConnectionCommandOptions.FSW_DOWNLINK_PORT_LONG, required = false)
            @ApiParam(value = ConnectionCommandOptions.FSW_DOWNLINK_PORT_DESC) final Integer fswDownlinkPort,
            @RequestParam(value = ConnectionCommandOptions.SSE_HOST_LONG, required = false)
            @ApiParam(value = ConnectionCommandOptions.SSE_HOST_DESC) final String sseHost,
            @RequestParam(value = ConnectionCommandOptions.SSE_DOWNLINK_PORT_LONG, required = false)
            @ApiParam(value = ConnectionCommandOptions.SSE_DOWNLINK_PORT_DESC) final Integer sseDownlinkPort,
            @RequestParam(value = ConnectionCommandOptions.DB_SOURCE_HOST_LONG, required = false)
            @ApiParam(value = ConnectionCommandOptions.DB_SOURCE_HOST_DESC) final String dbSourceHost,
            @RequestParam(value = ConnectionCommandOptions.DB_SOURCE_KEY_LONG, required = false)
            @ApiParam(value = ConnectionCommandOptions.DB_SOURCE_KEY_DESC) final Integer dbSourceKey,
            @RequestParam(value = SpacecraftIdOption.LONG_OPTION, required = false)
            @ApiParam(value = SpacecraftIdOption.DESCRIPTION) final Integer spacecraftID,
            @RequestParam(value = SessionCommandOptions.SESSION_DSSID_LONG, required = false)
            @ApiParam(value = SessionCommandOptions.SESSION_DSSID_DESC) final Integer sessionDssId,
            @RequestParam(value = SessionCommandOptions.SESSION_VCID_LONG, required = false)
            @ApiParam(value = SessionCommandOptions.SESSION_VCID_DESC) final Integer sessionVcid,
            @RequestParam(value = SessionCommandOptions.SESSION_NAME_LONG, required = false)
            @ApiParam(value = SessionCommandOptions.SESSION_NAME_DESC) final String sessionName,
            @RequestParam(value = SessionCommandOptions.SESSION_USER_LONG, required = false)
            @ApiParam(value = SessionCommandOptions.SESSION_USER_DESC) final String sessionUser,
            @RequestParam(value = SessionCommandOptions.SESSION_DESC_LONG, required = false)
            @ApiParam(value = SessionCommandOptions.SESSION_DESC_DESC) final String sessionDescription,
            @RequestParam(value = SessionCommandOptions.SESSION_TYPE_LONG, required = false)
            @ApiParam(value = SessionCommandOptions.SESSION_TYPE_DESC) final String sessionType,
            @RequestParam(value = SessionCommandOptions.OUTPUT_DIRECTORY_LONG, required = false)
            @ApiParam(value = SessionCommandOptions.OUTPUT_DIRECTORY_DESC) final String outputDir,
            @RequestParam(value = SubtopicOption.LONG_OPTION, required = false)
            @ApiParam(value = SubtopicOption.DESCRIPTION) final String subtopic,
            @RequestParam(value = ContextCommandOptions.PUBLISH_TOPIC_PARAM_LONG, required = false)
            @ApiParam(value = ContextCommandOptions.PUBLISH_TOPIC_DESC) final String topic) {

        final List<String> argList = new ArrayList<>();

        //we need a new session to configure
        argList.addAll(DictionaryCommandOptions
                               .buildDictionaryCliFromArgs(fswDictionaryDir, fswVersion, sseDictionaryDir, sseVersion));

        argList.addAll(ConnectionCommandOptions
                               .buildConnectionCliFromArgs(downlinkConnectionType, inputFormat, inputFile,
                                                           fswDownlinkHost, fswDownlinkPort, sseHost, sseDownlinkPort,
                                                           dbSourceHost, dbSourceKey));

        argList.addAll(SessionCommandOptions
                               .buildSessionOptionsFromCli(sessionHost, sessionName, sessionUser, sessionDescription,
                                                           sessionType, outputDir, sessionDssId, sessionVcid));

        argList.addAll(CommonCommandLineOptionsCliHelper
                               .buildMiscOptionsFromCli(venueType,
                                                        downlinkStreamId != null ? DownlinkStreamType.convert(downlinkStreamId) : null,
                                                        spacecraftID, subtopic, testbedName));

        argList.addAll(ContextCommandOptionsCliHelper.buildMiscOptionsFromCli(topic));

        IContextKey contextKey;
        try {
            contextKey = manager
                    .attachWorkerToSession(sessionKey, sessionHost, argList.stream().toArray(String[]::new));
        }
        catch (ApplicationException e) {
            throw new RestfulTelemetryException(HttpStatus.BAD_REQUEST, ExceptionTools.getMessage(e));
        }
        final ITelemetryWorker worker = manager
                .getWorker(contextKey.getNumber(), contextKey.getHost(), contextKey.getFragment());
        return new WorkerStateChangeResponse("INGESTION WORKER ATTACHED", contextKey, null, worker.getState());

    }

    /**
     * Stop TI worker
     *
     * @param key a valid session key
     * @param host a valid session host
     * @param fragment a valid session fragment
     *
     * @return stop status
     */
    @PostMapping(value = STOP_CMD, produces = MediaType.APPLICATION_JSON)
    @ApiOperation(value = "If currently processing telemetry, causes the current downlink process to stop processing telemetry.", tags = STOP_CMD)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "TI Worker Successfully Stopped"),
            @ApiResponse(code = 412, message = WORKER_NOT_RUNNING),
            @ApiResponse(code = 500, message = INTERNAL_SERVER_ERROR) })
    public WorkerStateChangeResponse stop(
            @RequestParam(value = KEY_PARAM) @ApiParam(value = PARAM_SESSION_KEY) final long key,
            @RequestParam(value = HOST_PARAM) @ApiParam(value = PARAM_SESSION_HOST) final String host,
            @RequestParam(value = FRAGMENT_PARAM) @ApiParam(value = PARAM_SESSION_FRAGMENT) final int fragment) {

        return changeState(manager, key, host, fragment, WorkerStateChangeAction.STOP);
    }

    /**
     * Abort TI worker
     *
     * @param key a valid session key
     * @param host a valid session host
     * @param fragment a valid session fragment
     *
     * @return abort status
     */
    @PostMapping(value = ABORT_CMD, produces = MediaType.APPLICATION_JSON)
    @ApiOperation(value = "If currently processing telemetry, causes the current downlink process to abort the processing of telemetry.", tags = ABORT_CMD)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "TI Worker Successfully Aborted"),
            @ApiResponse(code = 412, message = WORKER_NOT_RUNNING),
            @ApiResponse(code = 500, message =  INTERNAL_SERVER_ERROR) })
    public WorkerStateChangeResponse abort(
            @RequestParam(value = KEY_PARAM) @ApiParam(value = PARAM_SESSION_KEY) final long key,
            @RequestParam(value = HOST_PARAM) @ApiParam(value = PARAM_SESSION_HOST) final String host,
            @RequestParam(value = FRAGMENT_PARAM) @ApiParam(value = PARAM_SESSION_FRAGMENT) final int fragment) {

        return changeState(manager, key, host, fragment, WorkerStateChangeAction.ABORT);
    }

    /**
     * Shutdown TI server
     *
     * Note: naming this method shutdown() has special meaning to Spring, and it will get called automatically
     * on app unload. We want it only called via REST call
     *
     * @return exit status
     */
    @PostMapping(value = SHUTDOWN_CMD, produces = MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Causes the Ingest server process stop processing and shut down. An attempt is made to shut down cleanly, but the exit is guaranteed even if an orderly shutdown is not possible.", tags = SHUTDOWN_CMD)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "TI Server Successfully Stopped -- Exiting..."),
            @ApiResponse(code = 500, message =  INTERNAL_SERVER_ERROR) })
    public StringResponse execShutdown(){
        return execShutdown(manager);
    }


    /**
     * Start TI worker
     *
     * @param key a valid session key
     * @param host a valid session host
     * @param fragment a valid session fragment
     *
     * @return worker start status
     */
    @PostMapping(value = START_CMD, produces = MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Start TI worker.", tags = START_CMD)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "TI Worker started successfully"),
            @ApiResponse(code = 500, message = WORKER_NOT_STARTED)})
    public WorkerStateChangeResponse start(
            @RequestParam(value = KEY_PARAM) @ApiParam(value = PARAM_SESSION_KEY)  final long key,
            @RequestParam(value = HOST_PARAM) @ApiParam(value = PARAM_SESSION_HOST) final String host,
            @RequestParam(value = FRAGMENT_PARAM) @ApiParam(value = PARAM_SESSION_FRAGMENT) final int fragment) {

        return changeState(manager, key, host, fragment, WorkerStateChangeAction.START);
    }

    /**
     * Release TI worker
     *
     * @param key a valid session key
     * @param host a valid session host
     * @param fragment a valid session fragment
     *
     * @return worker release status
     */
    @PostMapping(value = RELEASE_CMD, produces = MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Release TI worker.", tags = RELEASE_CMD)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "TI Worker released successfully"),
            @ApiResponse(code = 500, message = WORKER_NOT_RELEASED)})
    public WorkerStateChangeResponse release(
            @RequestParam(value = KEY_PARAM) @ApiParam(value = PARAM_SESSION_KEY)  final long key,
            @RequestParam(value = HOST_PARAM) @ApiParam(value = PARAM_SESSION_HOST) final String host,
            @RequestParam(value = FRAGMENT_PARAM) @ApiParam(value = PARAM_SESSION_FRAGMENT) final int fragment) {

        return changeState(manager, key, host, fragment, WorkerStateChangeAction.RELEASE);
    }


    @ApiOperation(value = "Logs a message to be associated with the server or processor ",
            notes="If key, host, and fragment are specified the log will be associated with an existing processor."
                    + " Otherwise it will be logged at the server level", tags = LOG_CMD)
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid log message request"),
            @ApiResponse(code = 200, message = "Successfully logged message"),
            @ApiResponse(code = 400, message = "Invalid parameters specified") })
    @PostMapping(value = LOG_CMD, produces = MediaType.APPLICATION_JSON)
    public StringResponse logMessage(@RequestParam(value = "level")
                                     @ApiParam(value = "The log message severity", required = true)
                                     final String level,
                                     @RequestParam(value = "message")
                                     @ApiParam(value = "The message to log", required = true)
                                     final String message,
                                     @RequestParam(value = KEY_PARAM, required = false)
                                     @ApiParam(value = PARAM_SESSION_KEY)
                                     final Long key,
                                     @RequestParam(value = HOST_PARAM, required = false)
                                     @ApiParam(value = PARAM_SESSION_HOST)
                                     final String host,
                                     @RequestParam(value = FRAGMENT_PARAM, required = false)
                                     @ApiParam(value = PARAM_SESSION_FRAGMENT)
                                     final Integer fragment) {
        if (key != null && key != 0L && host != null && !host.isEmpty() && fragment != null && fragment != 0) {
            final ITelemetryWorker worker = manager.getWorker(key, host, fragment);

            // Make sure worker exists
            if (worker == null) {
                throw new RestfulTelemetryException(HttpStatus.PRECONDITION_FAILED,
                                                    "Worker with ID: " + key + "/" + host + "/" + fragment + " does not exist");
            }
            try {
                return logHelper.logMessage(level, message, worker.getTracer());
            } catch(Exception e) {
                throw new RestfulTelemetryException(HttpStatus.BAD_REQUEST, ExceptionTools.getMessage(e));
            }
        } else {
            if ( (key != null && key != 0L) || (host != null && host.isEmpty()) || (fragment != null && fragment != 0) ) {
                throw new RestfulTelemetryException(HttpStatus.BAD_REQUEST, "Error processing request! Key, host, and"
                        + " fragment must all be specified together, or not at all.");
            }

            // No key, host and fragment have been specified, so log to the server.
            try {
                return logHelper.logMessage(level, message, log);
            } catch(Exception e) {
                throw new RestfulTelemetryException(HttpStatus.BAD_REQUEST, ExceptionTools.getMessage(e));
            }
        }

    }

}
