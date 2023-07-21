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
package jpl.gds.sleproxy.server.chillinterface.config;

/**
 * Enumeration of property fields defined for the chill interface configuration.
 * 
 */
public enum EChillInterfaceConfigPropertyField {

	/**
	 * Property field for the chill downlink host name.
	 */
	DOWNLINK_HOST,
	
	/**
	 * Property field for the chill downlink port number.
	 */
	DOWNLINK_PORT,
	
	/**
	 * Property field for the chill uplink listening port number.
	 */
	UPLINK_LISTENING_PORT;

}