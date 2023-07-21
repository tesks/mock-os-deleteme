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

import jpl.gds.common.config.types.TelemetryConnectionType;
import jpl.gds.common.config.types.UplinkConnectionType;

/**
 * A factory that creates connection objects for use in an IConnectionMap.
 * 
 * @since R8
 */
public class ConnectionFactory {

	
	/**
	 * Creates a downlink connection object.
	 * 
	 * @param type the type of the connection
	 * 
	 * @return IDownlinkConnection instance
	 */
	public static IDownlinkConnection createDownlinkConfiguration(final TelemetryConnectionType type) {
		switch (type) {
		case CLIENT_SOCKET:
		case NEN_SN_CLIENT:
		case NEN_SN_SERVER:
		case SERVER_SOCKET:
			return new DownlinkNetworkConnection(type);			
		case FILE:
			return new DownlinkFileConnection();
		case TDS:
			return new DownlinkTdsConnection();
		case DATABASE:
			return new DownlinkDatabaseConnection();
		case UNKNOWN:
		default:
			throw new IllegalArgumentException("Unusupported connection type: " + type);
			
		}
	}
	
	/**
	 * Creates an uplink connection object.
	 * 
	 * @param type the type of the connection
	 * 
	 * @return IUplinkConnection instance
	 */
	public static IUplinkConnection createUplinkConfiguration(final UplinkConnectionType type) {
		switch (type) {
		case COMMAND_SERVICE:
		case SOCKET:
			return new UplinkNetworkConnection(type);
		case UNKNOWN:
		default:
			throw new IllegalArgumentException("Unusupported connection type: " + type);
		}
	}
}
