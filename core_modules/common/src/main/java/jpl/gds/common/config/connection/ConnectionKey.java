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
package jpl.gds.common.config.connection;

/**
 * An enumeration used as the key for an IConnectionMap.
 * 
 * @since R8
 */
public enum ConnectionKey {
	/** FSW Downlink Connection */
	FSW_DOWNLINK,
	/** FSW Uplink Connection */
	FSW_UPLINK,
	/** SSE Downlink Connection */
	SSE_DOWNLINK,
	/** SSE Uplink Connection */
	SSE_UPLINK;
}
