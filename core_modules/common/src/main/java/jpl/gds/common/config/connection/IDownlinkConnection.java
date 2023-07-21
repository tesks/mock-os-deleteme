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
import jpl.gds.common.config.types.TelemetryInputType;

/**
 * An interface to be implemented by telemetry (downlink) connection configurations.
 * 
 * @since R8
 */
public interface IDownlinkConnection extends IConnection {

	/**
	 * Gets the connection type associated with this connection.
	 * 
	 * @return DownlinkConnectionType; never null
	 */
	public TelemetryConnectionType getDownlinkConnectionType();
	
	/**
	 * Gets the telemetry input type (raw input type) associated
	 * with this downlink connection.
	 * 
	 * @return TelemetryInputType; never null
	 */
	public TelemetryInputType getInputType();
	
	/**
	 * Sets the telemetry input type (raw input type) associated
	 * with this downlink connection.
	 * 
	 * @param type TelemetryInputType to set; may not be null
	 */
	public void setInputType(TelemetryInputType type);
}
