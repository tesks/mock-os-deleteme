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
 * This enum defines URI parameters that must be replaced with values before the
 * URI is valid
 *
 * @since AMPCS R3
 */
public enum CpdServiceUriParameter {

	/**
	 * The spacecraft ID
	 */
	SCID,

	/**
	 * The user role.
	 */
	ROLE_ID,

	/**
	 * The uplink request ID
	 */
	REQUEST_ID,

	/**
	 * The station ID
	 */
	DSS_ID,

	/**
	 * The execution mode.
	 */
	EXEC_MODE,

	/**
	 * The execution state.
	 */
	EXEC_STATE,

	/**
	 * The execution method.
	 */
	EXEC_METHOD,

	/**
	 * The aggregation method
	 */
	AGGREGATION_METHOD,

	/**
	 * The preparation state
	 */
	PREP_STATE,

	/**
	 * The CPD event that should trigger the change. Used with setting CPD
	 * directives.
	 */
	WHEN,

	/**
	 * Purge database flag
	 */
	PURGEDB,

	/**
	 * Bitrate value
	 */
	BITRATE,

	/**
	 * Time
	 */
	TIME,

	/**
	 * Message number
	 */
	MSG_NUM
}
