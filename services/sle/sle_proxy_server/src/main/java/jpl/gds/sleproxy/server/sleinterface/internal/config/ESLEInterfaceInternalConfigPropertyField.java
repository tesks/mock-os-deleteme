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
package jpl.gds.sleproxy.server.sleinterface.internal.config;

/**
 * Enumerates all of the configuration properties that are definable for the SLE
 * interface.
 * 
 */
public enum ESLEInterfaceInternalConfigPropertyField {

	/**
	 * Version of the SLE standard to use for the forward service.
	 */
	FORWARD_SERVICE_VERSION,

	/**
	 * Timeout threshold for the SLE forward service BIND and UNBIND operations.
	 */
	FORWARD_BIND_UNBIND_TIMEOUT_MILLIS,

	/**
	 * Timeout threshold for the SLE forward service START and STOP operations.
	 */
	FORWARD_START_STOP_TIMEOUT_MILLIS,

	/**
	 * Timeout threshold for the SLE forward service GET PARAMETER operation.
	 */
	FORWARD_GET_PARAMETER_TIMEOUT_MILLIS,

	/**
	 * Timeout threshold for the SLE forward service PEER ABORT operation.
	 */
	FORWARD_PEER_ABORT_TIMEOUT_MILLIS,

	/**
	 * Timeout threshold for the SLE forward service THROW EVENT operation.
	 */
	FORWARD_THROW_EVENT_TIMEOUT_MILLIS,

	/**
	 * Timeout threshold for the SLE forward service THROW EVENT clearance at
	 * the provider.
	 */
	FORWARD_THROW_EVENT_CLEARANCE_TIMEOUT_SECONDS,

	/**
	 * THROW EVENT scheme to use.
	 */
	FORWARD_THROW_EVENT_SCHEME,

	/**
	 * Allowable bit rates for the THROW EVENT.
	 */
	FORWARD_THROW_EVENT_ALLOWABLE_BITRATES,

	/**
	 * Default bit rate to set the forward service to.
	 */
	FORWARD_DEFAULT_BITRATE,

	/**
	 * Allowable mod-index range for the THROW EVENT.
	 */
	FORWARD_THROW_EVENT_ALLOWABLE_MODINDEX_RANGE,

	/**
	 * Default mod-index to set the forward service to.
	 */
	FORWARD_DEFAULT_MODINDEX,

	/**
	 * Default command modulation state to set the forward service to.
	 */
	FORWARD_DEFAULT_COMMAND_MOD_STATE,

	/**
	 * Boolean flag to enable/disable throw events for range modulation.
	 */
	FORWARD_THROW_EVENT_RANGE_MOD_ENABLE,

	/**
	 * Default range modulation state to set the forward service to.
	 */
	FORWARD_DEFAULT_RANGE_MOD_STATE,

	/**
	 * Value for the "report" flag, for SLE forward service transfer data
	 * operation.
	 */
	FORWARD_TRANSFER_DATA_REPORT_FLAG,

	/**
	 * Timeout threshold for the SLE forward service TRANSFER DATA operation.
	 */
	FORWARD_TRANSFER_DATA_TIMEOUT_MILLIS,

	/**
	 * Reporting cycle (in seconds) for the scheduling of SLE forward service
	 * status reports.
	 */
	FORWARD_SCHEDULE_STATUS_REPORT_REPORTING_CYCLE_SECONDS,

	/**
	 * Timeout threshold for the scheduling of status reports from the SLE
	 * forward service.
	 */
	FORWARD_SCHEDULE_STATUS_REPORT_TIMEOUT_MILLIS,

	/**
	 * Timeout threshold for the SLE forward service to wait for the remaining
	 * CLTUs to be transferred after the transferring thread has been
	 * interrupted.
	 */
	FORWARD_CLTU_TRANSFERRER_TERMINATION_TIMEOUT_MILLIS,

	/**
	 * Version of the SLE standard to use for the return service.
	 */
	RETURN_SERVICE_VERSION,

	/**
	 * Timeout threshold for the SLE return service BIND and UNBIND operations.
	 */
	RETURN_BIND_UNBIND_TIMEOUT_MILLIS,

	/**
	 * Timeout threshold for the SLE return service START and STOP operations.
	 */
	RETURN_START_STOP_TIMEOUT_MILLIS,

	/**
	 * Timeout threshold for the SLE return service GET PARAMETER operation.
	 */
	RETURN_GET_PARAMETER_TIMEOUT_MILLIS,

	/**
	 * Timeout threshold for the SLE return service PEER ABORT operation.
	 */
	RETURN_PEER_ABORT_TIMEOUT_MILLIS,

	/**
	 * Reporting cycle (in seconds) for the scheduling of SLE return service
	 * status reports.
	 */
	RETURN_SCHEDULE_STATUS_REPORT_REPORTING_CYCLE_SECONDS,

	/**
	 * Timeout threshold for the scheduling of status reports from the SLE
	 * return service.
	 */
	RETURN_SCHEDULE_STATUS_REPORT_TIMEOUT_MILLIS,

	/**
	 * Number of missed heartbeats to consider a connection dead
	 */
	DEAD_FACTOR,

	/**
	 * Close after Peer Abort (CPA) timeout in milliseconds
	 */
	CLOSE_AFTER_PEER_ABORT_TIMEOUT_MILLIS,

	/**
	 * Acceptable connection delay in milliseconds
	 */
	ACCEPTABLE_DELAY_MILLIS,

	/**
	 * Startup timeout period in milliseconds
	 */
	STARTUP_TIMEOUT_PERIOD_MILLIS,

	/**
	 * Hearbeat interval in milliseconds
	 */
	HEARTBEAT_INTERVAL_MILLIS;

}