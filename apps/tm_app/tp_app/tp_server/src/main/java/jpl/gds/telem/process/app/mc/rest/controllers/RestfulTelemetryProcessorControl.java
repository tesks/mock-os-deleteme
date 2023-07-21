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
package jpl.gds.telem.process.app.mc.rest.controllers;

import io.swagger.annotations.*;
import jpl.gds.common.config.TimeComparisonStrategy;
import jpl.gds.common.config.bootstrap.options.ChannelLadBootstrapCommandOptions;
import jpl.gds.common.config.dictionary.options.DictionaryCommandOptions;
import jpl.gds.context.api.options.ContextCommandOptions;
import jpl.gds.context.api.options.ContextCommandOptionsCliHelper;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.shared.exceptions.ApplicationException;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.exceptions.RestfulTelemetryException;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.metadata.context.IContextKey;
import jpl.gds.shared.string.StringResponse;
import jpl.gds.telem.common.ITelemetryWorker;
import jpl.gds.telem.common.app.mc.rest.controllers.AbstractRestfulTlmController;
import jpl.gds.telem.common.app.mc.rest.resources.WorkerStateChangeResponse;
import jpl.gds.telem.common.state.WorkerStateChangeAction;
import jpl.gds.telem.process.IProcessWorker;
import jpl.gds.telem.process.ProcessServerManagerApp;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import javax.annotation.PostConstruct;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static jpl.gds.context.cli.app.mc.IRestfulClientCommandLineApp.*;

/**
 * Telemetry Processor Control REST API
 *
 * As much as possible, return actual objects from the REST API calls, rather than ResponseEntity
 * Spring will serialize these automatically
 * All APIs should return JSON
 *
 */
@RestController
@ResponseBody
@RequestMapping("/control")
@Api(value = "control")
@EnableSwagger2
public class RestfulTelemetryProcessorControl extends AbstractRestfulTlmController {

    private static final String LAD_CLEARED = "LAD CLEARED";
    private static final String LAD_NOT_CLEARED        = "LAD NOT CLEARED";
    private static final String LAD_SAVED              = "LAD SAVED";
    private static final String LAD_NOT_SAVED          = "LAD NOT SAVED";

    private static final String CLEARLAD_CMD = "clearlad";
    private static final String SAVELAD_CMD = "savelad";
    private static final String DICT_CMD = "dictionary";

    private static final String PARAM_FILENAME = "the fully qualified file name on the downlink's file system to "
            + "which to save the current contents of the downlink local channel LAD";

    private static final String PARAM_DICT_DIR= "directory to look for dictionary";
    protected static final String REGEX_NUMERIC_CSV = "^\\d+(,\\d+)*";

    private final ProcessServerManagerApp manager;

    protected DictionaryProperties dictionaryProps;

    /**
     * Telemetry Processor REST controller constructor
     *
     * @param applicationContext the current application context
     * @param processManager Telemetry Processor Server Manager application
     */
    public RestfulTelemetryProcessorControl(final ApplicationContext applicationContext,
                                            final ProcessServerManagerApp processManager) {
        super(applicationContext);
        this.manager = processManager;
        this.dictionaryProps = applicationContext.getBean(DictionaryProperties.class);
    }

    @PostConstruct
    public void init() {
        log = TraceManager.getTracer(appContext, Loggers.PROCESSOR);
    }

    /**
     * Stop TP worker
     *
     * @param key a valid session key
     * @param host a valid session host
     * @param fragment a valid session fragment
     *
     * @return stop status
     */
    @PostMapping(value = STOP_CMD, produces = MediaType.APPLICATION_JSON)
    @ApiOperation(value = "If currently processing telemetry, causes the current TP worker to stop processing telemetry.", tags = STOP_CMD)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "TP worker stopped successfully"),
            @ApiResponse(code = 412, message = WORKER_NOT_RUNNING),
            @ApiResponse(code = 500, message = INTERNAL_SERVER_ERROR)})
    public WorkerStateChangeResponse stop(@RequestParam(value = KEY_PARAM) @ApiParam(value = PARAM_SESSION_KEY)  final long key,
                                          @RequestParam(value = HOST_PARAM) @ApiParam(value = PARAM_SESSION_HOST) final String host,
                                          @RequestParam(value = FRAGMENT_PARAM) @ApiParam(value = PARAM_SESSION_FRAGMENT) final int fragment) {

        return changeState(manager, key, host, fragment, WorkerStateChangeAction.STOP);
    }

    /**
     * Abort TP worker
     *
     * @param key a valid session key
     * @param host a valid session host
     * @param fragment a valid session fragment
     *
     * @return abort status
     */
    @PostMapping(value = ABORT_CMD, produces = MediaType.APPLICATION_JSON)
    @ApiOperation(value = "If currently processing telemetry, causes the current TP worker to abort the processing of telemetry.", tags = ABORT_CMD)
    @ApiResponses(value = { @ApiResponse(code = 200, message = "TP worker Aborted Successfully"),
            @ApiResponse(code = 412, message = WORKER_NOT_RUNNING),
            @ApiResponse(code = 500, message =  INTERNAL_SERVER_ERROR) })
    public WorkerStateChangeResponse abort(
            @RequestParam(value = KEY_PARAM) @ApiParam(value = PARAM_SESSION_KEY) final long key,
            @RequestParam(value = HOST_PARAM) @ApiParam(value = PARAM_SESSION_HOST) final String host,
            @RequestParam(value = FRAGMENT_PARAM) @ApiParam(value = PARAM_SESSION_FRAGMENT) final int fragment) {

        return changeState(manager, key, host, fragment, WorkerStateChangeAction.ABORT);
    }

    /**
     * Shutdown TP Service
     *
     * Note: naming this method shutdown() has special meaning to Spring, and it will get called automatically
     * on app unload. We want it only called via REST call
     *
     * @return exit status
     */
    @PostMapping(value = SHUTDOWN_CMD, produces = MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Causes the Processor manager to stop all processing and shut down. An attempt is made to shut down cleanly, but the exit is guaranteed even if an orderly shutdown is not possible.", tags = SHUTDOWN_CMD)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "TP server stopped successfully -- exiting..."),
            @ApiResponse(code = 500, message = INTERNAL_SERVER_ERROR) })
    public StringResponse execShutdown(){
        return execShutdown(manager);
    }

    /**
     * Saves the current contents of the downlink local channel LAD to a specified, fully qualified file on the
     * downlink's file system
     * @param key a valid session key
     * @param host a valid session host
     * @param fragment a valid session fragment
     *
     * @param filename
     *            the fully qualified file name on the downlink's file system to which to save the current contents
     *            of the downlink local channel LAD
     *
     * @return savelad status
     */
    @PostMapping(value = SAVELAD_CMD, produces = MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Saves the current contents of the downlink local channel LAD to a specified, fully "
            + "qualified file on the downlink's file system.", tags = SAVELAD_CMD)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Global LAD Successfully Saved"),
            @ApiResponse(code = 500, message = LAD_NOT_SAVED)})
    public StringResponse saveLad(@RequestParam(value = KEY_PARAM) @ApiParam(value = PARAM_SESSION_KEY)  final long key,
                                  @RequestParam(value = HOST_PARAM) @ApiParam(value = PARAM_SESSION_HOST) final String host,
                                  @RequestParam(value = FRAGMENT_PARAM) @ApiParam(value = PARAM_SESSION_FRAGMENT) final int fragment,
                                  @RequestParam(value = "filename") @ApiParam(value = PARAM_FILENAME)  final String filename) {
        try {
            final boolean status = manager.saveLadToFile(key, host, fragment, filename);
            return new StringResponse(status ? LAD_SAVED : LAD_NOT_SAVED);
        } catch (final Exception e) {
            throw new RestfulTelemetryException(HttpStatus.INTERNAL_SERVER_ERROR,
                                                LAD_NOT_SAVED + ": " + ExceptionTools.getMessage(e));
        }
    }

    /**
     * Clears the contents of the current downlink LAD
     * @param key a valid session key
     * @param host a valid session host
     * @param fragment a valid session fragment
     *
     * @return clearlad status
     */
    @PostMapping(value = CLEARLAD_CMD, produces = MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Clears the contents of the current downlink LAD.", tags = CLEARLAD_CMD)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Global LAD Successfully Cleared"),
            @ApiResponse(code = 500, message = LAD_NOT_CLEARED)})
    public StringResponse clearLad(@RequestParam(value = KEY_PARAM) @ApiParam(value = PARAM_SESSION_KEY)  final long key,
                                   @RequestParam(value = HOST_PARAM) @ApiParam(value = PARAM_SESSION_HOST) final String host,
                                   @RequestParam(value = FRAGMENT_PARAM) @ApiParam(value = PARAM_SESSION_FRAGMENT) final int fragment) {
        try {
            final boolean status = manager.clearChannelState(key, host, fragment);
            return new StringResponse(status ? LAD_CLEARED : LAD_NOT_CLEARED);
        } catch (final Exception e) {
            throw new RestfulTelemetryException(HttpStatus.INTERNAL_SERVER_ERROR,
                                                LAD_NOT_CLEARED + ": " + ExceptionTools.getMessage(e));
        }
    }

    /**
     * Caches a specified dictionary
     *
     * @param name dictionary name
     * @param directory dictionary directory to look in
     *
     * @return success if dictionary is cached
     */
    @PostMapping(value = DICT_CMD, consumes = MediaType.APPLICATION_JSON)
    @ApiOperation(value = "", tags = DICT_CMD)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Dictionary cache request received.")})
    public StringResponse cacheDictionary(@RequestParam(value = "name") @ApiParam(value = PARAM_DICT_NAME)  final String name,
                                          @RequestParam(value = "dir", required = false) @ApiParam(value = PARAM_DICT_DIR) final String directory) {

        // Check if provided name and directory exist before caching dictionary
        if (directory == null || directory.isEmpty()) {
            if (!dictionaryProps.getAvailableFswVersions().contains(name)) {
                throw new RestfulTelemetryException(HttpStatus.BAD_REQUEST,
                                                    "Dictionary specified does not exist.");
            }
            manager.cacheDictionary(name, null);
            return new StringResponse("Cached " + dictionaryProps.getFswDictionaryDir() + File.separator + name);
        }
        if (!dictionaryProps.getAvailableFswVersions(directory).contains(name) || !manager.directoryExists(directory)) {
            throw new RestfulTelemetryException(HttpStatus.BAD_REQUEST,
                                                "Dictionary specified does not exist.");
        }
        manager.cacheDictionary(name, directory);
        return new StringResponse("Cached " + directory + File.separator + name);
    }

    /**
     * Create a TP worker and attach it to an existing session
     *
     * @param key a valid session key
     * @param host a valid session host
     *
     * @return worker attach status
     */
    @PostMapping(value = ATTACH_CMD, produces = MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Creates and attaches a new TP worker to an existing session.", tags = ATTACH_CMD)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "TP worker attached to session successfully"),
            @ApiResponse(code = 500, message = WORKER_NOT_ATTACHED)})
    public WorkerStateChangeResponse attach(
            @RequestParam(value = KEY_PARAM)
            @ApiParam(value = PARAM_SESSION_KEY)  final long key,
            @RequestParam(value = HOST_PARAM)
            @ApiParam(value = PARAM_SESSION_HOST) final String host,
            @RequestParam(value = DictionaryCommandOptions.FSW_DIR_LONG_OPT, required = false)
            @ApiParam(value = DictionaryCommandOptions.FSW_DIR_DESC) final String fswDictionaryDir,
            @RequestParam(value = DictionaryCommandOptions.FSW_VERSION_LONG_OPT, required = false)
            @ApiParam(value = DictionaryCommandOptions.FSW_VER_DESC) final String fswVersion,
            @RequestParam(value = DictionaryCommandOptions.SSE_DIR_LONG_OPT, required = false)
            @ApiParam(value = DictionaryCommandOptions.SSE_DIR_DESC) final String sseDictionaryDir,
            @RequestParam(value = DictionaryCommandOptions.SSE_VERSION_LONG_OPT, required = false)
            @ApiParam(value = DictionaryCommandOptions.SSE_VERSION_DESC) final String sseVersion,
            @RequestParam(value = ChannelLadBootstrapCommandOptions.BOOTSTRAP_CHANNEL_LAD_LONG, required = false)
            @ApiParam(value = ChannelLadBootstrapCommandOptions.BOOTSTRAP_CHANNEL_LAD_DESC) final boolean bootstrapLad,
            @RequestParam(value = ChannelLadBootstrapCommandOptions.BOOTSTRAP_CHANNEL_LAD_IDS_LONG, required = false)
            @ApiParam(value = ChannelLadBootstrapCommandOptions.BOOTSTRAP_CHANNEL_LAD_IDS_DESC) final String bootstrapIds,
            @RequestParam(value = ContextCommandOptions.PUBLISH_TOPIC_PARAM_LONG, required = false)
            @ApiParam(value = ContextCommandOptions.PUBLISH_TOPIC_DESC) final String topic) {

        final List<String> argList = new ArrayList<>();

        argList.addAll(DictionaryCommandOptions.buildDictionaryCliFromArgs(fswDictionaryDir, fswVersion,
                                                                           sseDictionaryDir, sseVersion));
        if (bootstrapLad) {
            argList.add("--" + ChannelLadBootstrapCommandOptions.BOOTSTRAP_CHANNEL_LAD_LONG);
            log.info("Bootstrap channel LAD is enabled for new TP worker ", key, ".");
        }

        if (bootstrapIds != null) {
            if (bootstrapIds.matches(REGEX_NUMERIC_CSV)) {
                argList.add("--" + ChannelLadBootstrapCommandOptions.BOOTSTRAP_CHANNEL_LAD_IDS_LONG);
                argList.add(bootstrapIds);
                log.info("Bootstrap LAD session IDs for new TP worker ", key, ": ", bootstrapIds);
            } else {
                log.warn("Bootstrap LAD session IDs for new TP worker ", key, " were not provided in numeric CSV form. Discarding and ignoring.");
            }

        }

        argList.addAll(ContextCommandOptionsCliHelper.buildMiscOptionsFromCli(topic));

        IContextKey contextKey;
        try{
            contextKey = manager.attachWorkerToSession(key, host, argList.stream().toArray(String[]::new));
        }
        catch (ApplicationException e){
            throw new RestfulTelemetryException(HttpStatus.BAD_REQUEST, ExceptionTools.getMessage(e));
        }

        final ITelemetryWorker worker = manager.getWorker(contextKey.getNumber(), contextKey.getHost(), contextKey.getFragment());

        return new WorkerStateChangeResponse("TP WORKER ATTACHED TO EXISTING SESSION", contextKey, null, worker.getState());
    }


    /**
     * Start TP worker
     *
     * @param key a valid session key
     * @param host a valid session host
     * @param fragment a valid session fragment
     *
     * @return worker start status
     */
    @PostMapping(value = START_CMD, produces = MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Start TP worker.", tags = START_CMD)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "TP worker started successfully"),
            @ApiResponse(code = 500, message = WORKER_NOT_STARTED)})
    public WorkerStateChangeResponse start(
            @RequestParam(value = KEY_PARAM) @ApiParam(value = PARAM_SESSION_KEY)  final long key,
            @RequestParam(value = HOST_PARAM) @ApiParam(value = PARAM_SESSION_HOST) final String host,
            @RequestParam(value = FRAGMENT_PARAM) @ApiParam(value = PARAM_SESSION_FRAGMENT) final int fragment) {

        return changeState(manager, key, host, fragment, WorkerStateChangeAction.START);
    }

    /**
     * Release TP worker
     *
     * @param key a valid session key
     * @param host a valid session host
     * @param fragment a valid session fragment
     *
     * @return worker release status
     */
    @PostMapping(value = RELEASE_CMD, produces = MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Release TP worker.", tags = RELEASE_CMD)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "TP Worker released successfully"),
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


    @PostMapping(value = TIME_COMP_CMD, produces = MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Sets the Time Comparison Strategy on a specified worker ", tags = TIME_COMP_CMD)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Successfully set the Time Comparison strategy"),
            @ApiResponse(code = 500, message = "Unable to set the time comparison strategy")})
    public StringResponse setTimeComp(
            @RequestParam(value = KEY_PARAM)
            @ApiParam(value = PARAM_SESSION_KEY)  final long key,
            @RequestParam(value = HOST_PARAM)
            @ApiParam(value = PARAM_SESSION_HOST) final String host,
            @RequestParam(value = FRAGMENT_PARAM)
            @ApiParam(value = PARAM_SESSION_FRAGMENT) final int fragment,
            @RequestParam(value = TIME_COMP_PARAM)
            @ApiParam(allowableValues = "LAST_RECEIVED,SCLK,SCET,ERT") final String strategy) {
        IProcessWorker worker = manager.getWorker(key, host, fragment);

        if (worker == null) {
            throw new RestfulTelemetryException(HttpStatus.EXPECTATION_FAILED, "Processor with ID: "
                    + key + "/" + host + "/" + fragment + " does not exist");
        } else {
            try {
                worker.setTimeComparisonStrategy(TimeComparisonStrategy.valueOf(strategy.toUpperCase()));
            } catch(final Exception e) {
                throw new RestfulTelemetryException(HttpStatus.INTERNAL_SERVER_ERROR,
                                                    "Unable to set " + strategy + ": " + ExceptionTools.getMessage(e));
            }
        }
        return new StringResponse("Set strategy " + strategy + " on Processor " + key + "/" + host + "/" + fragment);
    }


    //TODO new method: suspect-channels (key, host, fragment, channelID array)
}