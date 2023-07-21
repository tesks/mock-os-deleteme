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
package jpl.gds.tc.api.icmd;

import java.util.List;

import gov.nasa.jpl.icmd.schema.AggregationMethod;
import gov.nasa.jpl.icmd.schema.ExecutionMethod;
import gov.nasa.jpl.icmd.schema.ExecutionMode;
import gov.nasa.jpl.icmd.schema.ExecutionStateRequest;
import gov.nasa.jpl.icmd.schema.InsertResponseType;
import gov.nasa.jpl.icmd.schema.ListPreparationStateEnum;
import gov.nasa.jpl.icmd.schema.UplinkRequest;
import jpl.gds.common.config.types.CommandUserRole;
import jpl.gds.tc.api.IScmf;
import jpl.gds.tc.api.UplinkLogger;
import jpl.gds.tc.api.exception.ScmfWrapUnwrapException;
import jpl.gds.tc.api.icmd.datastructures.CpdConfiguration;
import jpl.gds.tc.api.icmd.datastructures.CpdConnectionStatus;
import jpl.gds.tc.api.icmd.datastructures.CpdDmsBroadcastStatusMessages;
import jpl.gds.tc.api.icmd.exception.AuthenticationException;
import jpl.gds.tc.api.icmd.exception.AuthorizationException;
import jpl.gds.tc.api.icmd.exception.ICmdException;

/**
 * The CpdClient interface
 * 
 *
 */
public interface ICpdClient {

    /**
     * Set the uplink logger to use to log. Subsequent logs after setting the
     * logger will use the new logger.
     * 
     * @param logger
     *            the uplink logger to use to log
     */
    void setLogger(UplinkLogger logger);


    /**
     * Ping the command service to see if it is alive
     *
     * @return true if command service responded, false otherwise
     * @throws ICmdException if the command service URI is not properly
     *             configured
     */
    boolean pingCommandService() throws ICmdException;

    /**
     * Send an SCMF to CPD
     *
     * @param scmf the SCMF to send
     *
     * @return an <code>InsertResponseType</code> object containing CPD's
     *         response to the SCMF insertion
     *
     * @throws ICmdException if there is an error encountered while preparing
     *             the SCMF to send to CPD
     * @throws AuthorizationException if the user is not authorized to perform
     *             this action as the selected role
     * @throws AuthenticationException if the user is not authenticated
     * @throws ScmfWrapUnwrapException if SCMF exceeds max file size
     */
    InsertResponseType sendScmf(IScmf scmf)
            throws ICmdException, AuthenticationException, AuthorizationException, ScmfWrapUnwrapException;

    /**
     * MPCS-5934 - Josh Choi - 3/27/2015: New getDmsBroadcastStatusMessages
     * method.
     *
     * Polls the CPD for DMS broadcast status messages.
     *
     * The returned CpdDmsBroadcastStatusMessages object contains 5 things:
     *
     * 1. List of active requests from CPD
     *
     * 2. Radiation requests that were received by CPD. This includes active
     * (those found in the radiation list) and inactive (those that have been
     * radiated or aborted) requests. CPD periodically deletes old requests that
     * have been processed based on a configured tie period, so this should not
     * be used as a history of all requests received by CPD.
     *
     * 3. CPD configuration
     *
     * 4. Connection status
     *
     * 5. Bit-rate and mod-index
     *
     * @return a list of active requests on the CPD server
     *
     * @throws ICmdException
     *             if the response type from CPD server does not match what is
     *             expected or if the service URI is not properly configured
     * @throws AuthorizationException
     *             if the user is not authorized to perform this action as the
     *             selected role
     * @throws AuthenticationException
     *             if the user is not authenticated
     */
    CpdDmsBroadcastStatusMessages getDmsBroadcastStatusMessages()
            throws ICmdException, AuthenticationException, AuthorizationException;

    /**
     * MPCS-5934 - Josh Choi - 3/27/2015: Removed getRadiationList method.
     *
     * Get radiation requests that were received by CPD. This includes active
     * (those found in the radiation list) and inactive (those that have been
     * radiated or aborted) requests. CPD periodically deletes old requests that
     * have been processed based on a configured time period, so this should not
     * be used as a history of all requests received by CPD.
     *
     * @return a list radiation requests received by CPD
     * @throws ICmdException if the response type from CPD server does not match
     *             what is expected or if the service URI is not properly
     *             configured
     * @throws AuthorizationException if the user is not authorized to perform
     *             this action as the selected role
     * @throws AuthenticationException if the user is not authenticated
     */
    List<UplinkRequest> getAllRadiationRequests() throws ICmdException, AuthenticationException, AuthorizationException;

    /**
     * Retrieve a specific request from the CPD server
     *
     * @param requestId the request ID of the request to retrieve
     * @param requestRole the role of the user that originally issued the
     *            request
     * @return the <code>UplinkRequest</code> object that contains the data of
     *         the retrieved request or null if no requests matched the provided
     *         request ID
     * @throws ICmdException if the response type from CPD server does not match
     *             what is expected or if the service URI is not properly
     *             configured
     * @throws AuthorizationException if the user is not authorized to perform
     *             this action as the selected role
     * @throws AuthenticationException if the user is not authenticated
     */
    UplinkRequest getRequest(String requestId, CommandUserRole requestRole)
            throws ICmdException, AuthenticationException, AuthorizationException;

    /**
     * Set the CPD server list execution mode
     *
     * @param execMode the execution Mode to set the server to
     * @param when when the change should take effect
     * @return the CPD response to the set execution mode action
     * @throws ICmdException if the response type from CPD server does not match
     *             what is expected or if the service URI is not properly
     *             configured
     * @throws AuthorizationException if the user is not authorized to perform
     *             this action as the selected role
     * @throws AuthenticationException if the user is not authenticated
     */
    CpdResponse setExecutionMode(ExecutionMode execMode, CpdTriggerEvent when)
            throws ICmdException, AuthenticationException, AuthorizationException;

    /**
     * Set the CPD server list execution state
     *
     * @param execState the execution state to set the server to
     * @param when when the change should take effect
     * @return the CPD response to the set execution state action
     * @throws ICmdException if the response type from CPD server does not match
     *             what is expected or if the service URI is not properly
     *             configured
     * @throws AuthorizationException if the user is not authorized to perform
     *             this action as the selected role
     * @throws AuthenticationException if the user is not authenticated
     */
    CpdResponse setExecutionState(ExecutionStateRequest execState, CpdTriggerEvent when)
            throws ICmdException, AuthenticationException, AuthorizationException;

    /**
     * Set the CPD server list execution method
     *
     * @param execMethod the execution method to set the server to
     * @param when when the change should take effect
     * @return the CPD response to the set execution method action
     * @throws ICmdException if the response type from CPD server does not match
     *             what is expected or if the service URI is not properly
     *             configured
     * @throws AuthorizationException if the user is not authorized to perform
     *             this action as the selected role
     * @throws AuthenticationException if the user is not authenticated
     */
    CpdResponse setExecutionMethod(ExecutionMethod execMethod, CpdTriggerEvent when)
            throws ICmdException, AuthenticationException, AuthorizationException;

    /**
     * Set the CPD server's aggregation method
     *
     * @param aggMethod the Aggregation Method to set the server to
     * @return the CPD response to the set aggregation method action
     * @throws ICmdException if the response type from CPD server does not match
     *             what is expected or if the service URI is not properly
     *             configured
     * @throws AuthorizationException if the user is not authorized to perform
     *             this action as the selected role
     * @throws AuthenticationException if the user is not authenticated
     */
    CpdResponse setAggregationMethod(AggregationMethod aggMethod)
            throws ICmdException, AuthenticationException, AuthorizationException;

    /**
     * Set the CPD server list preparation state mode
     *
     * @param prepState the preparation state to set the server to
     * @return the CPD response to the set execution mode action
     * @throws ICmdException if the response type from CPD server does not match
     *             what is expected or if the service URI is not properly
     *             configured
     * @throws AuthorizationException if the user is not authorized to perform
     *             this action as the selected role
     * @throws AuthenticationException if the user is not authenticated
     */
    CpdResponse setPreparationState(ListPreparationStateEnum prepState)
            throws ICmdException, AuthenticationException, AuthorizationException;

    /**
     * Set the CPD server execution state
     *
     * @return the ExecutionMode set on the CPD server
     * @throws ICmdException if the response type from CPD server does not match
     *             what is expected or if the service URI is not properly
     *             configured
     * @throws AuthorizationException if the user is not authorized to perform
     *             this action as the selected role
     * @throws AuthenticationException if the user is not authenticated
     */
    CpdConfiguration getCpdConfiguration() throws ICmdException, AuthenticationException, AuthorizationException;

    /**
     * Request the CPD server to connect to a station. This is used in manual
     * radiation mode to create a connection to the station.
     *
     * @param stationId the station ID to connection
     * @return the CPD response to the create connection session action
     * @throws ICmdException if the response type from CPD server does not match
     *             what is expected or if the service URI is not properly
     *             configured
     * @throws AuthorizationException if the user is not authorized to perform
     *             this action as the selected role
     * @throws AuthenticationException if the user is not authenticated
     */
    CpdResponse connectToStation(String stationId)
            throws ICmdException, AuthenticationException, AuthorizationException;

    /**
     * Get the connection status
     *
     * @return a CpdConnectionStatus object indicating CPD's connection status
     * @throws ICmdException if there is an error retrieving the connection
     *             status from CPD
     * @throws AuthorizationException if the user is not authorized to perform
     *             this action as the selected role
     * @throws AuthenticationException if the user is not authenticated
     */
    CpdConnectionStatus getConnectionStatus() throws ICmdException, AuthenticationException, AuthorizationException;

    /**
     * Request the CPD server to disconnect from a station. This is used in
     * manual radiation mode to disconnect from the connected station. It does
     * nothing if CPD is currently not connected to a station.
     *
     * @return the CPD response to the disconnection action
     * @throws ICmdException if the response type from CPD server does not match
     *             what is expected or if the service URI is not properly
     *             configured
     * @throws AuthorizationException if the user is not authorized to perform
     *             this action as the selected role
     * @throws AuthenticationException if the user is not authenticated
     */
    CpdResponse disconnectFromStation() throws ICmdException, AuthenticationException, AuthorizationException;

    /**
     * Request the CPD server to delete a radiation request.
     *
     * @param requestId ID of the radiation request to delete
     * @param requestRole the role of the user that originally issued the
     *            request
     * @param purgeDb whether or not the record should be purged from database
     * @return the CPD response to the delete radiation request action
     * @throws ICmdException if the response type from CPD server does not match
     *             what is expected or if the service URI is not properly
     *             configured
     * @throws AuthorizationException if the user is not authorized to perform
     *             this action as the selected role
     * @throws AuthenticationException if the user is not authenticated
     */
    CpdResponse deleteRadiationRequest(String requestId, CommandUserRole requestRole, boolean purgeDb)
            throws ICmdException, AuthenticationException, AuthorizationException;

    /**
     * Request the CPD server to flush all radiation requests.
     *
     * @param purgeDb whether or not the records should be purged from database
     * @param rolePool the role's request pool to flush
     * @return the CPD response to the flush radiation requests action
     * @throws ICmdException if the response type from CPD server does not match
     *             what is expected or if the service URI is not properly
     *             configured
     * @throws AuthorizationException if the user is not authorized to perform
     *             this action as the selected role
     * @throws AuthenticationException if the user is not authenticated
     */
    CpdResponse flushRequests(CommandUserRole rolePool, boolean purgeDb)
            throws ICmdException, AuthenticationException, AuthorizationException;

    /**
     * Requests the CPD server to set a new bit-rate value
     *
     * @param rate bitrate to set
     * @return the CPD response to the set bit-rate request action
     * @throws ICmdException if the response type from CPD server does not match
     *             what is expected or if the service URI is not properly
     *             configured
     * @throws AuthorizationException if the user is not authorized to perform
     *             this action as the selected role
     * @throws AuthenticationException if the user is not authenticated
     */
    CpdResponse setBitRate(double rate) throws ICmdException, AuthenticationException, AuthorizationException;

    /**
     * Issues a directive to CPD
     *
     * @param directive the directive to send to CPD
     * @return the response from CPD after processing the directive
     * @throws AuthenticationException if there was an error authenticating the
     *             user
     * @throws AuthorizationException if the user/role trying to issue the
     *             directive does not have permission to do so
     * @throws ICmdException if there is an error encountered while processing
     *             the directive to send to CPD
     * 
     * MPCS-5934- 3/27/2015: Removed getBitRateModIndex method.
     */
    CpdResponse issueDirective(CpdDirective directive)
            throws AuthenticationException, AuthorizationException, ICmdException;

}