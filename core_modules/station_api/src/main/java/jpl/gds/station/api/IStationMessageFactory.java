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
package jpl.gds.station.api;

import jpl.gds.station.api.dsn.chdo.IChdoSfdu;
import jpl.gds.station.api.dsn.message.IDsnMonitorMessage;
import jpl.gds.station.api.earth.message.INenMonitorMessage;

/**
 * An interface to be implemented by factories that create messages for the station modules.
 * 
 *
 * @since R8
 */
public interface IStationMessageFactory {

    /**
     * Creates a DSN station monitor packet message.
     * 
     * @param chdo the MON-0158 CHDO SFDU packet received from the station
     * @return new message instance
     */
    public IDsnMonitorMessage createDsnMonitorMessage(IChdoSfdu chdo);

    /**
     * Creates a NEN station monitor packet message.
     * 
     * @param info the station telemetry info object received with the NEN status packet
     * @param header the station header object received with the NEN status packet
     * @param data the NEN status packet data
     * @return new message instance
     */
    public INenMonitorMessage createNenMonitorMessage(IStationTelemInfo info, IStationTelemHeader header, byte[] data);

}