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

/**
 * An interface defining beans in the Spring configuration for the station modules.
 * 
 *
 * @since R8
 */
public interface StationApiBeans {
    /** Bean name for the station info object factory */
    String STATION_INFO_FACTORY = "STATION_INFO_FACTORY";
    /** Bean name for the station message factory */
    String STATION_MESSAGE_FACTORY = "STATION_MESSAGE_FACTORY";
    /** Bean name for the station header factory. */
    String STATION_HEADER_FACTORY = "STATION_HEADER_FACTORY";
    /** Bean name for the CHDO SFDU configuration */
    String CHDO_CONFIGURATION = "CHDO_CONFIGURATION";
    /** Bean name for the SLE Private Annotation parser */
    String SLE_PRIVATE_ANNOTATION = "SLE_PRIVATE_ANNOTATION";

}
