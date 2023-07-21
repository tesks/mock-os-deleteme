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
package jpl.gds.station.api.earth.message;

import jpl.gds.shared.message.IMessage;
import jpl.gds.station.api.IStationTelemHeader;
import jpl.gds.station.api.IStationTelemInfo;

/**
 * An interface to be implemented by messages classes that carry Near-Earth_Network (NEN)
 * status packets.
 * 
 *
 * @since R8
 */
public interface INenMonitorMessage extends IMessage {

    /**
     * Gets the data from the NEN status packet.
     * 
     * @return byte array
     */
    public byte[] getData();

    /**
     * Gets the station info associated with receipt of the status packet.
     * 
     * @return station telemetry info object
     */
    public IStationTelemInfo getStationInfo();

    /**
     * Gets the station header associated with receipt of the status packet.
     * 
     * @return station telemetry header object, e.g., LEOT header
     */
    public IStationTelemHeader getStationHeader();

}