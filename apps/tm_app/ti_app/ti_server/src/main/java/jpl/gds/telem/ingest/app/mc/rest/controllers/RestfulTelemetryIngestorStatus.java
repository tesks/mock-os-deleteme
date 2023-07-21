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
import jpl.gds.db.api.DatabaseException;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.exceptions.RestfulTelemetryException;
import jpl.gds.shared.string.StringResponse;
import jpl.gds.telem.common.ITelemetryWorker;
import jpl.gds.telem.common.app.mc.rest.resources.AbstractRestfulTlmStatus;
import jpl.gds.telem.common.app.mc.rest.resources.DownlinkStatusResource;
import jpl.gds.telem.common.app.mc.rest.resources.WorkerId;
import jpl.gds.telem.ingest.IIngestWorker;
import jpl.gds.telem.ingest.IngestServerManagerApp;
import jpl.gds.telem.ingest.app.mc.rest.resources.TelemetryIngestorSummaryDelegateResource;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import javax.ws.rs.core.MediaType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static jpl.gds.context.cli.app.mc.IRestfulClientCommandLineApp.*;

/**
 * Telemetry Ingestion Status REST API
 *
 * As much as possible, return actual objects from the REST API calls, rather than ResponseEntity
 * Spring will serialize these automatically
 * All APIs should return JSON
 *
 *
 */
@RestController
@ResponseBody
@RequestMapping("/ingest/status")
@Api(value = "status")
@EnableSwagger2
public class RestfulTelemetryIngestorStatus extends AbstractRestfulTlmStatus {
    private final ApplicationContext                                    appContext;

    /**
     * Constructor
     * @param appContext
     *            the Spring Application Context
     * @param manager IngestServerManager object
     */
    public RestfulTelemetryIngestorStatus(final ApplicationContext appContext, final IngestServerManagerApp manager) {
        super(appContext, manager);
        this.appContext = appContext;
    }

    //Telemetry Ingestion Service methods

    /**
     *
     * Query telemetry ingestion service properties
     *
     * @param regExOrNull a regular expression with which to filter results, e.g.:
     *        - query two properties: /status/properties?filter=mission.spacecraft.ids|time.date.useDoyOutputFormat
     *        - query station properties: /status/properties?filter=stationMap.id.*
     * @param includeDescriptionsOrNull
     *            if true, show descriptions, if false or null, do not show descriptions
     * @param includeSystemOrNull
     *            if true, show System properties, if false or null, do not show System properties
     * @param includeTemplateDirsOrNull
     *            if true, show Template Directories, if false or null, do not show Template Directories
     * @return a Map of property objects that satisfy the request
     */
    @GetMapping(value = QUERY_CMD, produces = MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Displays an optionally filtered list of currently active ingest properties.", tags = QUERY_CMD)
    @ApiResponse(code = 200, message = QUERY_RETRIEVED)
    public Map<String, String> queryConfiguration(
            @RequestParam(value = "filter", required = false) @ApiParam(value = PARAM_REGEX) final String regExOrNull,
            @RequestParam(value = "includeDescriptions", required = false) @ApiParam(value = PARAM_INCLUDE_DESC) final Boolean includeDescriptionsOrNull,
            @RequestParam(value = "includeSystem", required = false) final @ApiParam(value = PARAM_INCLUDE_SYS) Boolean includeSystemOrNull,
            @RequestParam(value = "includeTemplateDirs", required = false) @ApiParam(value = PARAM_INCLUDE_TEMPLATE) final Boolean includeTemplateDirsOrNull) {

        return super.queryConfiguration(regExOrNull, includeDescriptionsOrNull, includeSystemOrNull,
                                        includeTemplateDirsOrNull, appContext);
    }

    /**
     * Gets command line of the Telemetry Ingestion Service
     *
     * @return command line of the Telemetry Ingestion Service
     */
    @GetMapping(value = CMDLINE_CMD, produces = MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Displays the command line used to invoke the currently running downlink process.", tags = CMDLINE_CMD)
    @ApiResponse(code = 200, message = CMDLINE_RETRIEVED)
    public StringResponse getCommandLine(){
        return super.getCommandLine(appContext.getBean(ApplicationArguments.class).getSourceArgs(),
                                    ApplicationConfiguration.getApplicationName());
    }

    /**
     * Get Performance status of the Telemetry Ingestion Service
     * @return the Current Downlink Status POJO
     */
    @GetMapping(value = PERF_CMD, produces = MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Displays a summary of performance and memory usage data, along with warnings when low "
            + "resources threaten to cause failure.", tags = PERF_CMD)
    @ApiResponse(code = 200, message = PERF_RETRIEVED)
    public DownlinkStatusResource getPerformanceStatus(
            @RequestParam(value = KEY_PARAM) @ApiParam(value = PARAM_SESSION_KEY)  final long key,
            @RequestParam(value = HOST_PARAM) @ApiParam(value = PARAM_SESSION_HOST)  final String host,
            @RequestParam(value = FRAGMENT_PARAM) @ApiParam(value = PARAM_SESSION_FRAGMENT)  final int fragment) {

        final IIngestWorker worker = ((IngestServerManagerApp)manager).getWorker(key, host, fragment);
        if (worker != null) {
            return  worker.getPerfStatus();
        } else {
            throw new RestfulTelemetryException(HttpStatus.NOT_FOUND, SESSION_NOT_FOUND);
        }
    }

    //Worker methods

    /**
     * Get current state for a Telemetry Ingestor (worker)
     *
     * @param key a valid session key
     * @param host a valid session host
     * @param fragment a valid session fragment
     *
     * @return Downlink Processing State
     */
    @GetMapping(value = STATE_CMD, produces = MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Returns the current telemetry ingestion state", tags = STATE_CMD)
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Telemetry ingestion state successfully returned"),
            @ApiResponse(code = 404, message = "ERROR retrieving " + DOWNLINK_PROCESSING_STATE),
            @ApiResponse(code = 500, message = INTERNAL_SERVER_ERROR)})
    public StringResponse getDownlinkProcessingState(
            @RequestParam(value = KEY_PARAM) @ApiParam(value = PARAM_SESSION_KEY) final long key,
            @RequestParam(value = HOST_PARAM) @ApiParam(value = PARAM_SESSION_HOST)  final String host,
            @RequestParam(value = FRAGMENT_PARAM) @ApiParam(value = PARAM_SESSION_FRAGMENT)  final int fragment) {
        return getProcessingState(key, host, fragment);
    }

    /**
     * Get telemetry summary for a given session
     *
     * @param key a valid session key
     * @param host a valid session host
     * @param fragment a valid session fragment
     *
     * @return the Current Telemetry Summary POJO
     */
    @GetMapping(value = TELEM_CMD, produces = MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Displays a summary of telemetry processing statistics.", tags = TELEM_CMD)
    @ApiResponses(
            value = {@ApiResponse(code = 200, message = "Telemetry summary  successfully returned"),
            @ApiResponse(code = 404, message = SESSION_NOT_FOUND),
            })
    public TelemetryIngestorSummaryDelegateResource getTelemetryStatus(
            @RequestParam(value = KEY_PARAM) @ApiParam(value = PARAM_SESSION_KEY)  final long key,
            @RequestParam(value = HOST_PARAM) @ApiParam(value = PARAM_SESSION_HOST)  final String host,
            @RequestParam(value = FRAGMENT_PARAM) @ApiParam(value = PARAM_SESSION_FRAGMENT)  final int fragment) {

        final IIngestWorker worker = ((IngestServerManagerApp)manager).getWorker(key, host, fragment);
        if (worker != null) {
            TelemetryIngestorSummaryDelegateResource telemStatus = (TelemetryIngestorSummaryDelegateResource) worker.getTelemStatus();
            telemStatus.setTelemetrySummary(worker.getSessionSummary());
            return telemStatus;
        } else {
            throw new RestfulTelemetryException(HttpStatus.NOT_FOUND, SESSION_NOT_FOUND);
        }
    }

    /**
     * Get session configuration for specific TI worker (if session data present) or all workers
     *
     * @param key a valid session key
     * @param host a valid session host
     * @param fragment a valid session fragment
     *
     * @return the Current Context Configuration list
     */
    @GetMapping(value = SESSION_CMD, produces = MediaType.APPLICATION_JSON)
    @ApiOperation(value = "If a session is specified, return session configuration for worker, all workers ",
            tags = SESSION_CMD)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Session configuration successfully returned"),
            @ApiResponse(code = 404, message = SESSION_NOT_FOUND)
    })
    public List<Map<String, String>> getSessionConfiguration(
            @RequestParam(value = KEY_PARAM, required = false) @ApiParam(value = PARAM_SESSION_KEY) final Long key,
            @RequestParam(value = HOST_PARAM, required = false) @ApiParam(value = PARAM_SESSION_HOST) final String host,
            @RequestParam(value = FRAGMENT_PARAM, required = false) @ApiParam(value = PARAM_SESSION_FRAGMENT) final Integer fragment) {
        return new ArrayList<>(getSessionConfig(key, host, fragment));
    }

    /**
     * Get context configuration for TI Server
     *
     * @return the Current Context Configuration map
     */
    @GetMapping(value = CONTEXT_CMD, produces = MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Get Telemetry Ingestion Server context configuration ", tags = CONTEXT_CMD)
    @ApiResponse(code = 200, message = "Context configuration successfully returned")
    public Map<String, String> getServerContextConfiguration(){
        return super.getConfigMap(manager.getContextConfiguration());
    }

    /**
     * Gets the "up" status for the TI Service. Returns OK if the service is up
     * No value for GetMapping(), use class default
     *
     * @return Service status
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON)
    @ApiResponse(code = 200, message = "Service is up")
    @ApiOperation(value = "Check if the Telemetry Ingestor Service has started", notes = "No response if the service has not started", tags = STATUS_CMD)
    public StringResponse status() {
        return getStatus();
    }

    /**
     * Retrieves a list of worksrs (sessions) TP is aware of
     *
     * @return Map of sessions
     */
    @GetMapping(value = WORKERS_CMD, produces = MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Retrieves the list of active workers (sessions)", tags = WORKERS_CMD)
    @ApiResponses(value = {@ApiResponse(code = 200, message = WORKERS_RETRIEVED)})
    public List<WorkerId> getWorkers() {
        return manager.getWorkers();
    }



    /**
     * Gets logs from a particular telemetry server
     * @param key the key identifier
     * @param host the host identifier
     * @param fragment the fragment identifier
     * @return logs queried; may be empty
     */
    @ApiOperation(value = "Query for log message entries in the database", notes="If key, host, and fragment are specified "
            + "the logs returned will be for a specific ingestor. Otherwise the logs returned will be from the server",tags = LOGS_CMD)
    @ApiResponses(value = {
            @ApiResponse(code = 412, message = "Invalid request"),
            @ApiResponse(code = 200, message = "Successfully retrieved log messages"),
            @ApiResponse(code = 400, message = "Invalid parameters specified")})
    @GetMapping(value = { LOGS_CMD } , produces = MediaType.APPLICATION_JSON)
    public ResponseEntity getLogs(@RequestParam(value = KEY_PARAM, required = false)
                                            @ApiParam(value = PARAM_SESSION_KEY)
                                            final Long key,
                                  @RequestParam(value = HOST_PARAM, required = false)
                                            @ApiParam(value = PARAM_SESSION_HOST)
                                            final String host,
                                  @RequestParam(value = FRAGMENT_PARAM, required = false)
                                            @ApiParam(value = PARAM_SESSION_FRAGMENT)
                                            final Integer fragment,
                                  @RequestParam(value = LOG_LIMIT_PARAM, required = false, defaultValue = "-1")
                                      @ApiParam(value = PARAM_LOG_LIMIT)
                                      final int limit) {

        if (key != null && key != 0L && host != null && !host.isEmpty() && fragment != null && fragment != 0) {
            final ITelemetryWorker worker = manager.getWorker(key, host, fragment);

            // Make sure worker exists
            if (worker == null) {
                throw new RestfulTelemetryException(HttpStatus.PRECONDITION_FAILED,
                                                    "Worker with ID: " + key + "/" + host + "/" + fragment + " does not exist");
            }
            try {
                return ResponseEntity.ok(logHelper.getLogsFromSession(key, host, limit));
            } catch(final DatabaseException e) {
                throw new RestfulTelemetryException(HttpStatus.INTERNAL_SERVER_ERROR, ExceptionTools.getMessage(e));
            }
        } else {
            if ( (key != null && key != 0L) || (host != null && host.isEmpty()) || (fragment != null && fragment != 0) ) {
                throw new RestfulTelemetryException(HttpStatus.BAD_REQUEST, "Error processing request! Key, host, and"
                        + " fragment must all be specified together, or not at all.");
            }

            // No key, host and fragment have been specified, so get logs from the server.
            try {
                return ResponseEntity.ok(logHelper.getLogsFromContext(manager.getContextConfiguration().getContextId().getNumber(),
                                                    manager.getContextConfiguration().getContextId().getHost(), limit));
            } catch (final DatabaseException e) {
                throw new RestfulTelemetryException(HttpStatus.INTERNAL_SERVER_ERROR, ExceptionTools.getMessage(e));
            }
        }

    }


    /**
     * Gets available dictionaries
     *
     * @param dir Directory to look for dictionaries, URL encoded
     *
     * @return Available dictionaries list
     */
    @GetMapping(value = "dictionaries", produces = MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Retrieves the list of available dictionaries.", tags = "dictionaries")
    @ApiResponses(value = {@ApiResponse(code = 200, message = DICTIONARIES_RETRIEVED),
            @ApiResponse(code = 500, message = "Error returning dictionaries")})
    public List<String> getAvailableDictionaries(
            @RequestParam(value = "dir", required = false)
                @ApiParam(value = PARAM_DICT)  final String dir) {
        return getServerDictionaries(dir);
    }
}
