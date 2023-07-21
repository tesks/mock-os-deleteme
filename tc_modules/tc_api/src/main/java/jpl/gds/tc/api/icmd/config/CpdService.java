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
package jpl.gds.tc.api.icmd.config;

/**
 * This enum defines the available services offered by CPD
 *
 * @since AMPCS R3
 */
public enum CpdService {
	/**
	 * This service adds an SCMF to the CPD server
	 */
	INSERT_SCMF,
	/**
	 * This service retrieves all requests that were sent to the CPD server and
	 * have not been purged
	 */
	GET_RADIATION_REQUESTS,
	/*
	 * MPCS-5934 - Josh Choi - 3/27/2015: Removed the deprecated
	 * GET_RADIATION_LIST and added GET_DMS_BROADCAST_STATUS_MESSAGES which
	 * replaces it. GET_RADIATION_REQUESTS is left in for backward compatibility
	 * with CommandStatusRequestApp.
	 */
	/**
	 * This service polls the CPD server for the DMS broadcast status messages
	 * for a given spacecraft ID, and thus replaces: (1) retrieving all pending
	 * radiation request, or GET_RADIATION_LIST; (2) retrieving all requests
	 * that were sent to the CPD server and have not been purged, or
	 * GET_RADIATION_REQUESTS; (3) retrieving the CPD configuration, or
	 * GET_CONFIGURATION; (4) retrieving the connection state of CPD server to a
	 * station, or GET_CONNECTION_STATE; and (5) retrieving the
	 * bit-rate/mod-index, or GET_BITRATE_MODINDEX. But GET_CONNECTION_STATE
	 * will remain, since it is also used to simply ping the CPD server.
	 */
	GET_DMS_BROADCAST_STATUS_MESSAGES,
	/**
	 * This service retrieves the detailed state of a given request
	 */
	GET_REQUEST_STATE,
	/**
	 * This service sets the execution mode on the CPD server
	 */
	SET_EXECUTION_MODE,
	/**
	 * This service sets the execution state on the CPD server
	 */
	SET_EXECUTION_STATE,
	/**
	 * This service sets the execution method on the CPD server
	 */
	SET_EXECUTION_METHOD,
	/**
	 * This service sets the aggregation method on the CPD server
	 */
	SET_AGGREGATION_METHOD,
	/**
	 * This service sets the preparation state on the CPD server
	 */
	SET_PREPARTION_STATE,
	/**
	 * This service retrieves the connection state of CPD server to a station
	 */
	GET_CONNECTION_STATE,
	/**
	 * This service makes a request to connect to a station
	 */
	CONNECT_TO_STATION,
	/**
	 * This service makes a request to disconnect from the connected station, if
	 * connected
	 */
	DISCONNECT_FROM_STATION,
	/**
	 * This service retrieves the CPD configuration
	 */
	GET_CONFIGURATION,
	/**
	 * This service deletes a radiation request
	 */
	DELETE_RADIATION_REQUEST,
	/**
	 * This service flushes all radiation requests
	 */
	FLUSH_REQUESTS,
	/**
	 * This service sets the bit-rate/mod-index
	 */
	SET_BITRATE
}
