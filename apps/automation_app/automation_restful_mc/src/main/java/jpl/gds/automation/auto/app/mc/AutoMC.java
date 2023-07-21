/*
 * Copyright 2006-2019. California Institute of Technology.
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
package jpl.gds.automation.auto.app.mc;

import jpl.gds.automation.auto.cfdp.app.mc.api.SessionApi;
import jpl.gds.automation.auto.cfdp.app.mc.api.ShutdownApi;
import jpl.gds.automation.auto.cfdp.app.mc.api.StatusApi;
import jpl.gds.automation.auto.cfdp.app.mc.invoker.ApiClient;
import jpl.gds.automation.auto.cfdp.app.mc.invoker.ApiException;
import jpl.gds.automation.auto.cfdp.app.mc.invoker.ApiResponse;
import jpl.gds.automation.auto.cfdp.app.mc.invoker.Configuration;
import jpl.gds.context.cli.app.mc.IRestfulClientCommandLineApp;
import jpl.gds.security.cli.rest.AbstractRestfulClientCommandLineApp;
import jpl.gds.shared.annotation.Operation;
import jpl.gds.shared.cli.app.mc.UnsafeOkHttpClient;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.exceptions.ExceptionTools;
import org.apache.commons.cli.ParseException;
import org.springframework.http.HttpStatus;

import java.util.concurrent.TimeUnit;

/**
 * M&C REST client implementation for chill_auto_uplink_server
 *
 * @since R8
 */
public class AutoMC extends AbstractRestfulClientCommandLineApp {

    private final ApiClient   apiClient;
    private final StatusApi   statusApi;
    private final ShutdownApi shutdownApi;
    private final SessionApi  sessionApi;

    /**
     * Constructor for command line applications. This creates a SIGTERM handler
     * that implements the IQuitSignalHandler interface
     */
    public AutoMC() {
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
    public AutoMC(final boolean addHook, final boolean attemptInsecureRetries) {
        super(addHook, attemptInsecureRetries);
        this.apiClient = Configuration.getDefaultApiClient();
        // TP stop times out when product assembly has not finished
        apiClient.getHttpClient().setConnectTimeout(MC_CLIENT_HTTP_CONNECTION_TIMEOUT_MINUTES, TimeUnit.MINUTES);
        apiClient.getHttpClient().setReadTimeout(MC_CLIENT_HTTP_CONNECTION_TIMEOUT_MINUTES, TimeUnit.MINUTES);
        apiClient.getHttpClient().setWriteTimeout(MC_CLIENT_HTTP_CONNECTION_TIMEOUT_MINUTES, TimeUnit.MINUTES);

        this.statusApi = new StatusApi(apiClient);
        this.shutdownApi = new ShutdownApi(apiClient);
        this.sessionApi = new SessionApi(apiClient);
    }

    /**
     * @param args
     *            command line arguments
     */
    public static void main(final String[] args) {
        new AutoMC().process(args).exit();
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
        return apiClient.getBasePath();
    }

    @Override
    public void configure(final ICommandLine commandLine) throws ParseException {
        restOptions.REST_PORT_OPTION.setDefaultValue(8384);
        super.configure(commandLine);
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
        return statusApi.getStatusWithHttpInfo();
    }

    /**
     * Send a shutdown request to the CFDP AUTO Uplink Proxy
     * 
     * @return status of the shutdown request
     * 
     * @throws ApiException
     *             if an error occurs
     */
    @Operation(subCmd = SHUTDOWN_CMD, subCmdDesc = "Shutdown the AUTO Uplink Proxy")
    public Object sendShutdown() throws ApiException {
        ApiResponse<?> statusResponse = null;
        try {
            statusResponse = (ApiResponse<?>) getStatus();
            if (statusResponse.getStatusCode() == HttpStatus.OK.value()) {
                System.out.println("Sending shutdown request"); // Server ready to be shutdown
            }
            else { // Status was successful but not 'OK' (I don't think this can even happen)
                throw new ApiException("Bad server status " + statusResponse.getStatusCode());
            }
        }
        catch (final ApiException e) { // status threw, server likely not up
            System.out.println("Unexpected error checking status: " + ExceptionTools.getMessage(e));
            return e;
        }

        // Does not execute if status was not successful
        try {
            shutdownApi.execShutdown();
        }
        catch (final Exception e) {
            // ApiResponse has connection error b/c server has been shutdown. Suppress it.
            // return headers from good status request
        }
        return new ApiResponse<>(HttpStatus.OK.value(), statusResponse.getHeaders());

    }

    /**
     * Attached the AUTO Uplink server to an existing, active session
     * @param key the session id
     * @param host the session host
     * @return A response
     * @throws ApiException if an error occurs attaching to the session
     */
    @Operation(subCmd = ATTACH_CMD, subCmdDesc = "Attaches the AUTO server to an existing session ",
            optNames = { KEY_PARAM, HOST_PARAM },
            parmNames = { KEY_PARAM, HOST_PARAM },
            parmTypes = { "LONG", "STRING" },
            parmDesc = { KEY_PARAM_DESC, HOST_PARAM_DESC })
    public Object attachToSession(final Long key, final String host) throws ApiException {
        return sessionApi.startSessionWithHttpInfo(null, key, host);
    }

}
