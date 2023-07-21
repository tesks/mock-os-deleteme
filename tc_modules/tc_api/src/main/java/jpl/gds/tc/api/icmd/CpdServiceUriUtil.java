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

import org.springframework.context.ApplicationContext;

import gov.nasa.jpl.icmd.schema.AggregationMethod;
import gov.nasa.jpl.icmd.schema.ExecutionMethod;
import gov.nasa.jpl.icmd.schema.ExecutionMode;
import gov.nasa.jpl.icmd.schema.ExecutionStateRequest;
import gov.nasa.jpl.icmd.schema.ListPreparationStateEnum;
import jpl.gds.common.config.connection.IConnectionMap;
import jpl.gds.common.config.types.CommandUserRole;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.tc.api.icmd.config.CpdService;
import jpl.gds.tc.api.icmd.config.CpdServiceUriParameter;
import jpl.gds.tc.api.icmd.config.IntegratedCommandProperties;
import jpl.gds.tc.api.icmd.exception.ICmdException;

/**
 * This is a utility class that builds URI for REST services provided by CPD
 *
 */
public class CpdServiceUriUtil {
//	private static CpdServiceUriUtil instance;
	private final int scid;
	private final String serverUrl;
	private final String scidParameter;
	private final String roleParameter;
	private final String requestIdParameter;
	private final String dssIdParameter;
	private final String execModeParameter;
	private final String execStateParameter;
	private final String execMethodParameter;
	private final String aggMethodParameter;
	private final String prepStateParameter;
	private final String whenParameter;
	private final String purgedbParameter;
	private final String bitrateParameter;
	private final String timeParameter;
	private final String msgNumParameter;
	private final IntegratedCommandProperties integratedCmdProps;

	public CpdServiceUriUtil(final ApplicationContext appContext) {
		//final SessionConfiguration config = SessionConfiguration.getGlobalInstance();
		this.scid = appContext.getBean(IContextIdentification.class).getSpacecraftId();
		
		integratedCmdProps = appContext.getBean(IntegratedCommandProperties.class);

		final String domain = integratedCmdProps.getCpdHostDomain();

		this.serverUrl = integratedCmdProps.getCpdUrlProtocol()
				+ "://"
				+ appContext.getBean(IConnectionMap.class).getFswUplinkConnection().getHost()
				+ (domain == null || domain.isEmpty() ? "" : "." + domain)
				+ ":" + appContext.getBean(IConnectionMap.class).getFswUplinkConnection().getPort();
		this.scidParameter = integratedCmdProps.getUriParameter(CpdServiceUriParameter.SCID);
		this.roleParameter = integratedCmdProps.getUriParameter(CpdServiceUriParameter.ROLE_ID);
		this.requestIdParameter = integratedCmdProps.getUriParameter(CpdServiceUriParameter.REQUEST_ID);
		this.dssIdParameter = integratedCmdProps
				.getUriParameter(CpdServiceUriParameter.DSS_ID);
		this.execModeParameter = integratedCmdProps
				.getUriParameter(CpdServiceUriParameter.EXEC_MODE);
		this.execStateParameter = integratedCmdProps
				.getUriParameter(CpdServiceUriParameter.EXEC_STATE);
		this.execMethodParameter = integratedCmdProps
				.getUriParameter(CpdServiceUriParameter.EXEC_METHOD);
		this.aggMethodParameter = integratedCmdProps
				.getUriParameter(CpdServiceUriParameter.AGGREGATION_METHOD);
		this.prepStateParameter = integratedCmdProps
				.getUriParameter(CpdServiceUriParameter.PREP_STATE);
		this.whenParameter = integratedCmdProps.getUriParameter(CpdServiceUriParameter.WHEN);
		this.purgedbParameter = integratedCmdProps.getUriParameter(CpdServiceUriParameter.PURGEDB);
		this.bitrateParameter = integratedCmdProps.getUriParameter(CpdServiceUriParameter.BITRATE);
		this.timeParameter = integratedCmdProps.getUriParameter(CpdServiceUriParameter.TIME);
		this.msgNumParameter = integratedCmdProps.getUriParameter(CpdServiceUriParameter.MSG_NUM);
	}
//
//	/**
//	 * Retrieve the singleton instance
//	 *
//	 * @return the singleton instance
//	 */
//	public static synchronized CpdServiceUriUtil getInstance() {
//		if (instance == null) {
//			instance = new CpdServiceUriUtil();
//		}
//
//		return instance;
//	}

	/**
	 * Get the URL of the CPD server, formatted as: [protocol]://[CPD
	 * host].[domain]:[CPD port]
	 *
	 * @return the URL of the CPD server
	 */
	public String getCpdServerUrl() {
		return this.serverUrl;
	}

	/*
	 * MPCS-5934 - Josh Choi - 3/27/2015: New method for generating the URI for
	 * DMS broadcast status messages poll.
	 */
	/**
	 * Retrieve the <code>CpdService.GET_DMS_BROADCAST_STATUS_MESSAGES</code>
	 * service URL
	 *
	 * @return the <code>CpdService.GET_DMS_BROADCAST_STATUS_MESSAGES</code>
	 *         service URI
	 * @throws ICmdException
	 *             if the service name cannot be obtained from GDS
	 *             configuration.
	 * @see jpl.gds.tc.api.icmd.config.CpdService
	 */
	public String getDmsBroadcastStatusMessagesServiceUri(final String time,
			final long msgNum) throws ICmdException {
		if (this.serverUrl == null) {
			throw new ICmdException("No CPD server URL configured.");
		}
		final StringBuilder sb = new StringBuilder();
		sb.append(this.serverUrl);

		final String serviceUri = integratedCmdProps
				.getServiceUri(CpdService.GET_DMS_BROADCAST_STATUS_MESSAGES);

		if (serviceUri == null) {
			throw new ICmdException("Unable to obtain service name for \""
					+ CpdService.GET_DMS_BROADCAST_STATUS_MESSAGES
					+ "\" service.");
		}
		sb.append("/");
		sb.append(serviceUri);

		String finalUri = sb.toString().replace(this.scidParameter,
				Integer.toString(scid));
		finalUri = finalUri.replace(this.timeParameter, time);
		finalUri = finalUri
				.replace(this.msgNumParameter, Long.toString(msgNum));

		return finalUri;
	}

	/*
	 * MPCS-5934 - Josh Choi - 3/27/2015: Removed getRadiationListServiceUri
	 * (replaced by getDmsBroadcastStatusMessagesServiceUri).
	 */

	/**
	 * Retrieve the <code>CpdService.GET_RADIATION_REQUESTS</code> service URI
	 *
	 * @return the <code>CpdService.GET_RADIATION_REQUESTS</code> service URI
	 * @throws ICmdException if the service name cannot be obtained from GDS
	 *             configuration.
	 * @see jpl.gds.tc.api.icmd.config.CpdService
	 */
	public String getRadiationRequestsServiceUri() throws ICmdException {
		if (this.serverUrl == null) {
			throw new ICmdException("No CPD server URL configured.");
		}
		final StringBuilder sb = new StringBuilder();

		sb.append(this.serverUrl);

		final String serviceUri = integratedCmdProps.getServiceUri(CpdService.GET_RADIATION_REQUESTS);

		if (serviceUri == null) {
			throw new ICmdException("Unable to obtain service name for \""
					+ CpdService.GET_RADIATION_REQUESTS + "\" service.");
		}
		sb.append("/");
		sb.append(serviceUri);

		final String finalUri = sb.toString().replace(this.scidParameter,
				Integer.toString(scid));

		return finalUri;
	}

	/**
	 * Retrieve the <code>CpdService.INSERT_SCMF</code> service URL
	 *
	 * @return the <code>CpdService.INSERT_SCMF</code> service URI
	 * @throws ICmdException if the service name cannot be obtained from GDS
	 *             configuration.
	 * @see jpl.gds.tc.api.icmd.config.CpdService
	 */
	public String getInsertScmfServiceUri(final CommandUserRole userRole)
			throws ICmdException {
		if (this.serverUrl == null) {
			throw new ICmdException("No CPD server URL configured.");
		}
		final StringBuilder sb = new StringBuilder();

		sb.append(this.serverUrl);

		final String serviceUri = integratedCmdProps.getServiceUri(CpdService.INSERT_SCMF);

		if (serviceUri == null) {
			throw new ICmdException("Unable to obtain service name for \""
					+ CpdService.INSERT_SCMF + "\" service.");
		}
		sb.append("/");
		sb.append(serviceUri);

		String finalUri = sb.toString().replace(this.scidParameter,
				Integer.toString(scid));
		finalUri = finalUri.replace(this.roleParameter, userRole.toString());

		return finalUri;
	}

	/**
	 * Retrieve the <code>CpdService.GET_REQUEST_STATE</code> service URI
	 *
	 * @param requestId the request ID
	 * @return the <code>CpdService.GET_REQUEST_STATE</code> service URL
	 * @throws ICmdException if the service name cannot be obtained from GDS
	 *             configuration.
	 * @see jpl.gds.tc.api.icmd.config.CpdService
	 */
	public String getRequestStateServiceUri(final String requestId,
			final CommandUserRole userRole) throws ICmdException {
		if (this.serverUrl == null) {
			throw new ICmdException("No CPD server URL configured.");
		}
		final StringBuilder sb = new StringBuilder();

		sb.append(this.serverUrl);

		final String serviceUri = integratedCmdProps.getServiceUri(CpdService.GET_REQUEST_STATE);

		if (serviceUri == null) {
			throw new ICmdException("Unable to obtain service name for \""
					+ CpdService.GET_REQUEST_STATE + "\" service.");
		}
		sb.append("/");
		sb.append(serviceUri);

		String finalUri = sb.toString().replace(this.scidParameter,
				Integer.toString(scid));
		finalUri = finalUri.replace(this.roleParameter, userRole.toString());
		finalUri = finalUri.replace(this.requestIdParameter, requestId);

		return finalUri;
	}

	/*
	 * MPCS-5934 - Josh Choi - 3/27/2015: Leaving in
	 * getConnectionStateServiceUri although its function is replaced by
	 * getDmsBroadcastStatusMessagesServiceUri. Reason is that this URI is used
	 * to ping the CPD server. Also, the user can issue directives to manually
	 * query the state.
	 */
	/**
	 * Retrieve the <code>CpdService.GET_CONNECTION_STATE</code> service URI
	 *
	 * @return the <code>CpdService.GET_CONNECTION_STATE</code> service URL
	 * @throws ICmdException if the service name cannot be obtained from GDS
	 *             configuration.
	 * @see jpl.gds.tc.api.icmd.config.CpdService
	 */
	public String getConnectionStateServiceUri() throws ICmdException {
		if (this.serverUrl == null) {
			throw new ICmdException("No CPD server URL configured.");
		}
		final StringBuilder sb = new StringBuilder();

		sb.append(this.serverUrl);

		final String serviceUri = integratedCmdProps.getServiceUri(CpdService.GET_CONNECTION_STATE);

		if (serviceUri == null) {
			throw new ICmdException("Unable to obtain service name for \""
					+ CpdService.GET_CONNECTION_STATE + "\" service.");
		}
		sb.append("/");
		sb.append(serviceUri);

		final String finalUri = sb.toString().replace(this.scidParameter,
				Integer.toString(scid));

		return finalUri;
	}

	/**
	 * Retrieve the <code>CpdService.CONNECT_TO_STATION</code> service URI
	 *
	 * @return the <code>CpdService.CONNECT_TO_STATION</code> service URL
	 * @throws ICmdException if the service name cannot be obtained from GDS
	 *             configuration.
	 * @see jpl.gds.tc.api.icmd.config.CpdService
	 */
	public String getConnectToStationServiceUri(final String dssId)
			throws ICmdException {
		if (this.serverUrl == null) {
			throw new ICmdException("No CPD server URL configured.");
		}
		final StringBuilder sb = new StringBuilder();

		sb.append(this.serverUrl);

		final String serviceUri = integratedCmdProps.getServiceUri(CpdService.CONNECT_TO_STATION);

		if (serviceUri == null) {
			throw new ICmdException("Unable to obtain service name for \""
					+ CpdService.CONNECT_TO_STATION + "\" service.");
		}
		sb.append("/");
		sb.append(serviceUri);

		String finalUri = sb.toString().replace(this.scidParameter,
				Integer.toString(scid));
		finalUri = finalUri.replace(this.dssIdParameter, dssId);

		return finalUri;
	}

	/**
	 * Retrieve the <code>CpdService.DISCONNECT_FROM_STATION</code> service URI
	 *
	 * @return the <code>CpdService.DISCONNECT_FROM_STATION</code> service URI
	 * @throws ICmdException if the service name cannot be obtained from GDS
	 *             configuration.
	 * @see jpl.gds.tc.api.icmd.config.CpdService
	 */
	public String getDisconnectFromStationServiceUri() throws ICmdException {
		if (this.serverUrl == null) {
			throw new ICmdException("No CPD server URL configured.");
		}
		final StringBuilder sb = new StringBuilder();

		sb.append(this.serverUrl);

		final String serviceUri = integratedCmdProps.getServiceUri(CpdService.DISCONNECT_FROM_STATION);

		if (serviceUri == null) {
			throw new ICmdException("Unable to obtain service name for \""
					+ CpdService.DISCONNECT_FROM_STATION + "\" service.");
		}
		sb.append("/");
		sb.append(serviceUri);

		final String finalUri = sb.toString().replace(this.scidParameter,
				Integer.toString(scid));

		return finalUri;
	}

	/**
	 * Retrieve the <code>CpdService.SET_EXECUTION_MODE</code> service URI
	 *
	 * @return the <code>CpdService.SET_EXECUTION_MODE</code> service URL
	 * @throws ICmdException if the service name cannot be obtained from GDS
	 *             configuration.
	 * @see jpl.gds.tc.api.icmd.config.CpdService
	 */
	public String getSetExecutionModeServiceUri(final ExecutionMode mode,
			final CpdTriggerEvent when) throws ICmdException {
		if (this.serverUrl == null) {
			throw new ICmdException("No CPD server URL configured.");
		}
		final StringBuilder sb = new StringBuilder();

		sb.append(this.serverUrl);

		final String serviceUri = integratedCmdProps.getServiceUri(CpdService.SET_EXECUTION_MODE);

		if (serviceUri == null) {
			throw new ICmdException("Unable to obtain service name for \""
					+ CpdService.SET_EXECUTION_MODE + "\" service.");
		}
		sb.append("/");
		sb.append(serviceUri);

		String finalUri = sb.toString().replace(this.scidParameter,
				Integer.toString(scid));
		finalUri = finalUri.replace(this.execModeParameter, mode.toString());
		finalUri = finalUri.replace(this.whenParameter, when.toString());

		return finalUri;
	}

	/**
	 * Retrieve the <code>CpdService.SET_EXECUTION_STATE</code> service URI
	 *
	 * @return the <code>CpdService.SET_EXECUTION_STATE</code> service URL
	 * @throws ICmdException if the service name cannot be obtained from GDS
	 *             configuration.
	 * @see jpl.gds.tc.api.icmd.config.CpdService
	 */
	public String getSetExecutionStateServiceUri(final ExecutionStateRequest state,
			final CpdTriggerEvent when) throws ICmdException {
		if (this.serverUrl == null) {
			throw new ICmdException("No CPD server URL configured.");
		}
		final StringBuilder sb = new StringBuilder();

		sb.append(this.serverUrl);

		final String serviceUri = integratedCmdProps.getServiceUri(CpdService.SET_EXECUTION_STATE);

		if (serviceUri == null) {
			throw new ICmdException("Unable to obtain service name for \""
					+ CpdService.SET_EXECUTION_STATE + "\" service.");
		}
		sb.append("/");
		sb.append(serviceUri);

		String finalUri = sb.toString().replace(this.scidParameter,
				Integer.toString(scid));
		finalUri = finalUri.replace(this.execStateParameter, state.toString());
		finalUri = finalUri.replace(this.whenParameter, when.toString());

		return finalUri;
	}

	/**
	 * Retrieve the <code>CpdService.SET_EXECUTION_METHOD</code> service URI
	 *
	 * @return the <code>CpdService.SET_EXECUTION_METHOD</code> service URL
	 * @throws ICmdException if the service name cannot be obtained from GDS
	 *             configuration.
	 * @see jpl.gds.tc.api.icmd.config.CpdService
	 */
	public String getSetExecutionMethodServiceUri(final ExecutionMethod method,
			final CpdTriggerEvent when) throws ICmdException {
		if (this.serverUrl == null) {
			throw new ICmdException("No CPD server URL configured.");
		}
		final StringBuilder sb = new StringBuilder();

		sb.append(this.serverUrl);

		final String serviceUri = integratedCmdProps.getServiceUri(CpdService.SET_EXECUTION_METHOD);

		if (serviceUri == null) {
			throw new ICmdException("Unable to obtain service name for \""
					+ CpdService.SET_EXECUTION_METHOD + "\" service.");
		}
		sb.append("/");
		sb.append(serviceUri);

		String finalUri = sb.toString().replace(this.scidParameter,
				Integer.toString(scid));
		finalUri = finalUri
				.replace(this.execMethodParameter, method.toString());
		finalUri = finalUri.replace(this.whenParameter, when.toString());

		return finalUri;
	}

	/**
	 * Retrieve the <code>CpdService.SET_AGGREGATION_METHOD</code> service URI
	 *
	 * @return the <code>CpdService.SET_AGGREGATION_METHOD</code> service URL
	 * @throws ICmdException if the service name cannot be obtained from GDS
	 *             configuration.
	 * @see jpl.gds.tc.api.icmd.config.CpdService
	 */
	public String getSetAggregationMethodServiceUri(final AggregationMethod method)
			throws ICmdException {
		if (this.serverUrl == null) {
			throw new ICmdException("No CPD server URL configured.");
		}
		final StringBuilder sb = new StringBuilder();

		sb.append(this.serverUrl);

		final String serviceUri = integratedCmdProps.getServiceUri(CpdService.SET_AGGREGATION_METHOD);

		if (serviceUri == null) {
			throw new ICmdException("Unable to obtain service name for \""
					+ CpdService.SET_AGGREGATION_METHOD + "\" service.");
		}
		sb.append("/");
		sb.append(serviceUri);

		String finalUri = sb.toString().replace(this.scidParameter,
				Integer.toString(scid));
		finalUri = finalUri.replace(this.aggMethodParameter, method.toString());

		return finalUri;
	}

	/**
	 * Retrieve the <code>CpdService.SET_PREPARTION_STATE</code> service URI
	 *
	 * @return the <code>CpdService.SET_PREPARTION_STATE</code> service URL
	 * @throws ICmdException if the service name cannot be obtained from GDS
	 *             configuration.
	 * @see jpl.gds.tc.api.icmd.config.CpdService
	 */
	public String getSetPreparationStateServiceUri(
			final ListPreparationStateEnum state) throws ICmdException {
		if (this.serverUrl == null) {
			throw new ICmdException("No CPD server URL configured.");
		}
		final StringBuilder sb = new StringBuilder();

		sb.append(this.serverUrl);

		final String serviceUri = integratedCmdProps.getServiceUri(CpdService.SET_PREPARTION_STATE);

		if (serviceUri == null) {
			throw new ICmdException("Unable to obtain service name for \""
					+ CpdService.SET_PREPARTION_STATE + "\" service.");
		}
		sb.append("/");
		sb.append(serviceUri);

		String finalUri = sb.toString().replace(this.scidParameter,
				Integer.toString(scid));
		finalUri = finalUri.replace(this.prepStateParameter, state.toString());

		return finalUri;
	}

	/*
	 * MPCS-5934 - Josh Choi - 3/27/2015: Although CPD configuration is now
	 * queried via the DMS broadcast status messages polling, user can issue
	 * directives to manually query this configuration.
	 */
	/**
	 * Retrieve the <code>CpdService.GET_CONFIGURATION</code> service URI
	 *
	 * @return the <code>CpdService.GET_CONFIGURATION</code> service URL
	 * @throws ICmdException if the service name cannot be obtained from GDS
	 *             configuration.
	 * @see jpl.gds.tc.api.icmd.config.CpdService
	 */
	public String getCpdConfigurationServiceUrl() throws ICmdException {
		if (this.serverUrl == null) {
			throw new ICmdException("No CPD server URL configured.");
		}
		final StringBuilder sb = new StringBuilder();

		sb.append(this.serverUrl);

		final String serviceUri = integratedCmdProps.getServiceUri(CpdService.GET_CONFIGURATION);

		if (serviceUri == null) {
			throw new ICmdException("Unable to obtain service name for \""
					+ CpdService.GET_CONFIGURATION + "\" service.");
		}
		sb.append("/");
		sb.append(serviceUri);

		final String finalUri = sb.toString().replace(this.scidParameter,
				Integer.toString(scid));

		return finalUri;
	}

	/**
	 * Retrieve the <code>CpdService.DELETE_RADIATION_REQUEST</code> service URI
	 *
	 * @return the <code>CpdService.DELETE_RADIATION_REQUEST</code> service URI
	 * @throws ICmdException if the service name cannot be obtained from GDS
	 *             configuration.
	 * @see jpl.gds.tc.api.icmd.config.CpdService
	 */
	public String getDeleteRadiationRequestServiceUri(final String requestId,
			final CommandUserRole userRole, final boolean purgeDb) throws ICmdException {
		if (this.serverUrl == null) {
			throw new ICmdException("No CPD server URL configured.");
		}
		final StringBuilder sb = new StringBuilder();

		sb.append(this.serverUrl);

		final String serviceUri = integratedCmdProps.getServiceUri(CpdService.DELETE_RADIATION_REQUEST);

		if (serviceUri == null) {
			throw new ICmdException("Unable to obtain service name for \""
					+ CpdService.DELETE_RADIATION_REQUEST + "\" service.");
		}
		sb.append("/");
		sb.append(serviceUri);

		String finalUri = sb.toString().replace(this.scidParameter,
				Integer.toString(scid));
		finalUri = finalUri.replace(this.roleParameter, userRole.toString());
		finalUri = finalUri.replace(this.requestIdParameter, requestId);
		finalUri = finalUri.replace(this.purgedbParameter,
				Boolean.toString(purgeDb));
		return finalUri;
	}

	/**
	 * Retrieve the <code>CpdService.FLUSH_REQUESTS</code> service URI
	 *
	 * @param rolePool the role pool to flush
	 * @param purgeDb whether or not to purge the request data from CPD's
	 *            database
	 * @return the <code>CpdService.FLUSH_REQUESTS</code> service URI
	 * @throws ICmdException if the service name cannot be obtained from GDS
	 *             configuration.
	 * @see jpl.gds.tc.api.icmd.config.CpdService
	 */
	public String getFlushRequestsServiceUri(final CommandUserRole rolePool,
			final boolean purgeDb) throws ICmdException {
		if (this.serverUrl == null) {
			throw new ICmdException("No CPD server URL configured.");
		}
		final StringBuilder sb = new StringBuilder();

		sb.append(this.serverUrl);

		final String serviceUri = integratedCmdProps.getServiceUri(CpdService.FLUSH_REQUESTS);

		if (serviceUri == null) {
			throw new ICmdException("Unable to obtain service name for \""
					+ CpdService.FLUSH_REQUESTS + "\" service.");
		}
		sb.append("/");
		sb.append(serviceUri);

		String finalUri = sb.toString().replace(this.scidParameter,
				Integer.toString(scid));

		if (rolePool == null) {
			finalUri = finalUri.replace("/" + this.roleParameter, "");
		} else {
			finalUri = finalUri
					.replace(this.roleParameter, rolePool.toString());
		}

		finalUri = finalUri.replace(this.purgedbParameter,
				Boolean.toString(purgeDb));
		return finalUri;
	}

	/**
	 * Retrieve the <code>CpdService.SET_BITRATE</code> service URI
	 *
	 * @param rate bitrate to set
	 * @return the <code>CpdService.SET_BITRATE</code> service URI
	 * @throws ICmdException if the service name cannot be obtained from GDS
	 *             configuration.
	 * @see jpl.gds.tc.api.icmd.config.CpdService
	 */
	public String getSetBitRateServiceUri(final double rate) throws ICmdException {
		if (this.serverUrl == null) {
			throw new ICmdException("No CPD server URL configured.");
		}
		final StringBuilder sb = new StringBuilder();

		sb.append(this.serverUrl);

		final String serviceUri = integratedCmdProps.getServiceUri(CpdService.SET_BITRATE);

		if (serviceUri == null) {
			throw new ICmdException("Unable to obtain service name for \""
					+ CpdService.SET_BITRATE + "\" service.");
		}
		sb.append("/");
		sb.append(serviceUri);

		String finalUri = sb.toString().replace(this.scidParameter,
				Integer.toString(scid));
		finalUri = finalUri.replace(this.bitrateParameter,
				Double.toString(rate));
		return finalUri;
	}

	/*
	 * MPCS-5934 - Josh Choi - 3/27/2015: Removed getBitRateModIndexServiceUri
	 * (replaced by getDmsBroadcastStatusMessagesServiceUri).
	 */

}
