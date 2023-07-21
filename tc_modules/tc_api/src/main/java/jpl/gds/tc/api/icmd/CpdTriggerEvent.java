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

/**
 * This enum defines valid CPD events that may put into effect a request to set
 * a CPD directive.
 * 
 * @since AMPCS R3
 */
public enum CpdTriggerEvent {
	/**
	 * Change is effective immediately
	 */
	IMMEDIATELY,

	/**
	 * Change is effective after current request on CPD server is completed
	 */
	AFTER_CURRENT_REQUEST,

	/**
	 * Change is effective after the current connection session on CPD server
	 * ends. Only applicable in auto radiation mode.
	 */
	AFTER_CURRENT_SESSION
}
