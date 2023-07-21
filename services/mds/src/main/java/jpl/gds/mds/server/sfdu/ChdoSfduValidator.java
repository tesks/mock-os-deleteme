/*
 * Copyright 2006-2021. California Institute of Technology.
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

package jpl.gds.mds.server.sfdu;

import jpl.gds.station.api.IStationHeaderFactory;
import jpl.gds.station.api.dsn.chdo.IChdoSfdu;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

/**
 * SFDU Validator using station_api and station_impl.
 */
public class ChdoSfduValidator implements ISfduValidator {

    private final IStationHeaderFactory stationHeaderFactory;

    /**
     * Constructor
     *
     * @param stationHeaderFactory station header factory for creation ChdoSfdu objects
     */
    public ChdoSfduValidator(IStationHeaderFactory stationHeaderFactory) {
        this.stationHeaderFactory = stationHeaderFactory;
    }

    @Override
    public boolean validate(byte[] data) {
        try {
            IChdoSfdu sfdu = stationHeaderFactory.createChdoSfdu();
            sfdu.readSfdu(new DataInputStream(new ByteArrayInputStream(data)));
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
