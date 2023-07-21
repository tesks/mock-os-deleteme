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

import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.station.api.dsn.chdo.ChdoConfigurationException;
import jpl.gds.station.api.dsn.chdo.IChdoSfdu;

/**
 * An interface to be implemented by station header object factories.
 * 
 *
 * @since R8
 */
public interface IStationHeaderFactory {

    /**
     * Creates a LEOT station header with the given data size and earth receive time.
     * 
     * @param dataSize size of the LEOT-encapsulated data
     * @param ert the earth received time
     * @return new LEOT station header instance
     */
    IStationTelemHeaderUpdater createLeotHeader(int dataSize, IAccurateDateTime ert);

    /**
     * Creates a empty LEOT station header.
     * 
     * @return new LEOT station header instance
     */
    IStationTelemHeaderUpdater createLeotHeader();
    
    /**
     * Creates an empty CHDO SFDU station header. Actually, the resulting object can contain
     * the attached data too.
     * 
     * @return new CHDO SFDU station header instance
     * @throws ChdoConfigurationException if there is an error loading the CHDO configuration
     */
    IChdoSfdu createChdoSfdu() throws ChdoConfigurationException;

    /**
     * Creates an empty SLE station header.
     *
     * @return new SLE station header instance
     */
    IStationTelemHeader createSleHeader();

}