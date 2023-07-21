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
package jpl.gds.telem.common.app.mc.rest.resources;

import jpl.gds.common.config.ConfigurationDumpUtility;
import jpl.gds.context.api.ISimpleContextConfiguration;
import jpl.gds.db.api.rest.IRestfulDbHelper;
import jpl.gds.shared.config.GdsHierarchicalProperties;
import jpl.gds.shared.config.PerformanceProperties;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.exceptions.RestfulTelemetryException;
import jpl.gds.shared.performance.HealthStatus;
import jpl.gds.shared.performance.HeapPerformanceData;
import jpl.gds.shared.performance.PerformanceSummaryMessage;
import jpl.gds.shared.performance.ProviderPerformanceSummary;
import jpl.gds.shared.string.StringResponse;
import jpl.gds.telem.common.ITelemetryServer;
import jpl.gds.telem.common.ITelemetryWorker;
import jpl.gds.telem.common.state.WorkerState;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpStatus;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Abstract class for RESTful telemetry status
 *
 *  As much as possible, return actual objects from the REST API calls, rather than ResponseEntity
 *  Spring will serialize these automatically
 *
 *  All APIs should return JSON
 *
 */
public class AbstractRestfulTlmStatus {

    /** Regex filter parameter description */
    protected static final String PARAM_REGEX = "a regular expression with which to filter results, e.g.:\n"
            + " - query two properties: /status/properties?filter=mission.spacecraft.ids|time.date.useDoyOutputFormat\n"
            + " - query station properties: /status/properties?filter=stationMap.id.*";

    /** Include descriptions parameter description */
    protected static final String PARAM_INCLUDE_DESC = "if true, show descriptions, if false or null, do not show descriptions";

    /** Include system parameter description */
    protected static final String PARAM_INCLUDE_SYS = "if true, show System properties, if false or null, do not show System properties";

    /** Include template parameter description */
    protected static final String PARAM_INCLUDE_TEMPLATE = "if true, show Template Directories, if false or null, do not show Template Directories";

    /** Session key description */
    protected static final String PARAM_SESSION_KEY = "the key of a session; unsigned long";

    /** Session host description */
    protected static final String PARAM_SESSION_HOST = "session hostname";

    /** Session fragment description */
    protected static final String PARAM_SESSION_FRAGMENT = "session fragment; unsigned integer";

    /** Downlink Processing State string */
    protected static final String DOWNLINK_PROCESSING_STATE = "DOWNLINK PROCESSING STATE";

    /** Session retrieved string */
    protected static final String WORKERS_RETRIEVED = "Worker IDs retrieved succesfully";

    /** Config query retrieved string */
    protected static final String QUERY_RETRIEVED = "Config query succesful";

    /** Config query retrieved string */
    protected static final String CMDLINE_RETRIEVED = "Command line retrieved successfully";

    /** Performance retrieved string */
    protected static final String PERF_RETRIEVED = "Performance data retrieved successfully";

    /** internal server error */
    protected static final String INTERNAL_SERVER_ERROR = "Internal Server Error";

    /** internal server error */
    protected static final String SESSION_NOT_FOUND = "Session Not Found";

    /** all session parameters */
    private static  final String KEY_REQUIRED = "You have to supply session key";

    /** Dictionary parameters and description */
    protected static final String DICTIONARIES_RETRIEVED = "DICTIONARIES RETRIEVED";
    protected static final String PARAM_DICT = "directory to look for dictionary, URL encoded";

    protected static final String LOG_LIMIT_PARAM = "limit";
    protected static final String PARAM_LOG_LIMIT = "log results limit, default is unlimited; positive integer. 0 or negative indicates unlimited.";

    protected final IRestfulDbHelper logHelper;

    protected final ITelemetryServer manager;


    /**
     * Abstract REST status Constructor
     * @param appContext Spring application context
     * @param manager the telemetry server manager
     */
    public AbstractRestfulTlmStatus(final ApplicationContext appContext, ITelemetryServer manager) {
        logHelper = appContext.getBean(IRestfulDbHelper.class);
        this.manager = manager;
    }



    /**
     * Get session configuration
     *
     * - If all key, key/host, key/host/fragment are specified - return specific session
     * - If no parameters, return all sessions
     *
     * @param key a valid session key
     * @param host a valid session host
     * @param fragment a valid session fragment
     *
     * @return Set of session configuration maps, in order to contain unique entries
     */
    protected Set<Map<String, String>> getSessionConfig(final Long key, final String host, final Integer fragment) {

        //if no session parameters - return all workers
        if(key == null && host == null && fragment == null) {
            Set<Map<String, String>> workers = new LinkedHashSet<>();
            for (ITelemetryWorker worker : manager.getAllWorkers()) {
                workers.add(getConfigMap(worker.getContextConfiguration()));
            }
            return workers;
        }

        //some parameters present - key is required
        if (key != null) {
            Set<Map<String, String>> workers = new LinkedHashSet<>();
            List<WorkerId> workerIds = manager.getWorkers();
            //match a worker by key, key/host, or key/host/fragment
            for(WorkerId workerId : workerIds){
                if(key == workerId.getKey()){
                    //may return null if not found
                    ITelemetryWorker worker = manager.getWorker(key, host != null ? host : workerId.getHost(),
                                                                fragment != null? fragment : workerId.getFragment());
                    if(worker != null) {
                        workers.add(getConfigMap(worker.getContextConfiguration()));
                    }
                    else{
                        throw new RestfulTelemetryException(HttpStatus.NOT_FOUND, SESSION_NOT_FOUND);
                    }
                }
            }
            // no workers, return not found
            if(workers.isEmpty()){
                throw new RestfulTelemetryException(HttpStatus.NOT_FOUND, SESSION_NOT_FOUND);
            }
            return workers;
        }
        else {
            throw new RestfulTelemetryException(HttpStatus.BAD_REQUEST, KEY_REQUIRED);
        }
    }

    /**
     * Return server context configuration
     * @param config
     *            ISimpleContextConfiguration to create a map for
     * @return Key/values map with server config (as strings)
     *
     * Use templateContext map
     */
    protected Map<String, String> getConfigMap(final ISimpleContextConfiguration config) {
        //use linked hash map to always return response in the same order
        final Map<String, String> map = new LinkedHashMap<>();

        Map<String, Object> templateContext = new HashMap<>();
        config.setTemplateContext(templateContext);

        //transform to Map<String,String>
        for(Map.Entry<String, Object> entry : templateContext.entrySet()){
            map.put(entry.getKey(), entry.getValue().toString());
        }

        return map;
    }

    /**
     * Builds the command-line
     * 
     * @param arguments
     *            command-line arguments
     * @param appName
     *            the application name
     *
     * @return String representation of the provided command-line
     */
    protected StringResponse getCommandLine(final String[] arguments,
                                            final String appName){
        final StringBuilder sb = new StringBuilder();

        //if SSE enabled, it will be visible in the arguments
        sb.append(appName);
        for (final String arg : arguments) {
            sb.append(' ').append(arg);
        }

        return new StringResponse(sb.toString());
    }

    /**
     * Gets the WorkerState for a particular session from an <ITelemetryServer>
     *
     * @param key a valid session key
     * @param host a valid session host
     * @param fragment a valid session fragment
     *
     * @return <ResponseEntity> containing the WorkerState for a session
     */
    public StringResponse getProcessingState(final long key, final String host, final int fragment) {
        final AtomicBoolean successful = new AtomicBoolean(false);
        final ITelemetryWorker app = manager.getWorker(key, host, fragment);
            if (app != null) {
                WorkerState currentState;
                currentState = app.getState();
                successful.set(true);

                if (successful.get()) {
                    return new StringResponse(currentState.name());
                } else {
                    throw new RestfulTelemetryException(HttpStatus.PRECONDITION_FAILED, "ERROR retrieving " + DOWNLINK_PROCESSING_STATE);
                }
            }
            else {
                // Return 404 when session was not found
                throw new RestfulTelemetryException(HttpStatus.NOT_FOUND, "ERROR retrieving " + DOWNLINK_PROCESSING_STATE);
        }
    }

    /**
     * Gets a property map of AMPCS configuration
     * 
     * @param regExOrNull
     *            regex
     * @param includeDescriptionsOrNull
     *            descriptions
     * @param includeSystemOrNull
     *            system properties
     * @param includeTemplateDirsOrNull
     *            template directories
     * @param appContext
     *            spring application context
     * @return properties map
     */
    protected Map<String, String> queryConfiguration(final String regExOrNull,
                                                     final Boolean includeDescriptionsOrNull,
                                                     final Boolean includeSystemOrNull,
                                                     final Boolean includeTemplateDirsOrNull,
                                                     final ApplicationContext appContext) {

        final boolean includeDescriptions = (includeDescriptionsOrNull != null) && includeDescriptionsOrNull;
        final boolean includeSystem = (includeSystemOrNull != null) && includeSystemOrNull;
        final boolean includeTemplateDirs = (includeTemplateDirsOrNull != null) && includeTemplateDirsOrNull;

        return new ConfigurationDumpUtility(appContext).collectProperties(regExOrNull, includeSystem, includeTemplateDirs,
          includeDescriptions ? GdsHierarchicalProperties.PropertySet.INCLUDE_DESCRIPTIVES :
                  GdsHierarchicalProperties.PropertySet.NO_DESCRIPTIVES);
    }

    /**
     * Returns OK if server is up
     *
     * @return Server status
     * */
    protected StringResponse getStatus() {
        return new StringResponse(HttpStatus.OK.getReasonPhrase());
    }

    /**
     * Method used to retrieve available dictionaries from a telemetry server
     * @param dir The dir to check; may be null
     * @return a list of available dictionaries; may be empty
     */
    protected List<String> getServerDictionaries(final String dir) {
        final List<String> dicts = new ArrayList<>();
        final String directory;
        if (dir != null && !dir.isEmpty()) {
            try {
                // Use URL encoded strings instead of Base64
                directory = URLDecoder.decode(dir, StandardCharsets.UTF_8.name());
            }
            catch (final UnsupportedEncodingException e) {
                throw new RestfulTelemetryException(HttpStatus.INTERNAL_SERVER_ERROR, "Error decoding URL (" + ExceptionTools
                        .getMessage(e) + ")");
            }
            dicts.addAll(manager.getAvailableDictionaries(directory));
        }
        else {
            dicts.addAll(manager.getAvailableDictionaries(null));
        }

        return dicts;
    }

}
