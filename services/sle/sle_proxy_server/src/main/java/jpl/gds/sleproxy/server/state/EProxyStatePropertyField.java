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
package jpl.gds.sleproxy.server.state;

/**
 * Enumeration of property fields defined for the proxy state, used for
 * persisting and loading the individual state items.
 * 
 */
public enum EProxyStatePropertyField {

	/**
	 * The last/current chill interface for uplink state set to.
	 */
	CHILL_INTERFACE_UPLINK_STATE,

	/**
	 * The last connection number used for the SLE forward service.
	 */
	LAST_SLE_FORWARD_SERVICE_CONNECTION_NUMBER,

	/**
	 * The last connection number used for the SLE return service.
	 */
	LAST_SLE_RETURN_SERVICE_CONNECTION_NUMBER,

	/**
	 * The last SLE forward service profile that the user attempted to BIND with.
	 */
	LAST_SLE_FORWARD_SERVICE_PROFILE_NAME,
	
	/**
	 * The last SLE return service profile that the user attempted to BIND with.
	 */
	LAST_SLE_RETURN_SERVICE_PROFILE_NAME;

}
