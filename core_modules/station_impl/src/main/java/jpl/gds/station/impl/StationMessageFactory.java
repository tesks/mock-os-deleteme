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
package jpl.gds.station.impl;

import jpl.gds.station.api.IStationMessageFactory;
import jpl.gds.station.api.IStationTelemHeader;
import jpl.gds.station.api.IStationTelemInfo;
import jpl.gds.station.api.dsn.chdo.IChdoSfdu;
import jpl.gds.station.api.dsn.message.IDsnMonitorMessage;
import jpl.gds.station.api.earth.message.INenMonitorMessage;
import jpl.gds.station.impl.dsn.message.DsnMonitorMessage;
import jpl.gds.station.impl.earth.message.NenMonitorMessage;

/**
 * A factory for creation messages for the station modules.
 * 
 *
 * @since R8
 */
public class StationMessageFactory implements IStationMessageFactory {
   
    /**
     * {@inheritDoc}
     */
    @Override
    public IDsnMonitorMessage createDsnMonitorMessage(final IChdoSfdu chdo) {
        return new DsnMonitorMessage( chdo );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public INenMonitorMessage createNenMonitorMessage(final IStationTelemInfo info, final IStationTelemHeader header, final byte[] data) {
        return new NenMonitorMessage(info, header, data);
    }
  
}
