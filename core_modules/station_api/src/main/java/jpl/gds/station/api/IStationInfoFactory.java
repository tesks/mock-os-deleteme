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

import jpl.gds.serialization.station.Proto3StationTelemInfo;
import jpl.gds.shared.time.IAccurateDateTime;

import java.util.Map;

/**
 * An interface to be implemented by factories that create station information
 * objects.
 * 
 *
 * @since R8
 */
public interface IStationInfoFactory {

    /**
     * Creates an IStationTelemInfo object.
     * 
     * @param bitRate data receipt bitrate
     * @param numBits number of bits received
     * @param ert earth receive time for the station
     * @param dssId  receiving station ID
     * 
     * @return  IStationTelemInfo object
     */
    public IStationTelemInfo create(double bitRate, int numBits, IAccurateDateTime ert, int dssId);

	/**
	 * Creates an IStationTelemInfo object.
	 *
	 * @param bitRate data receipt bitrate
	 * @param numBits number of bits received
	 * @param ert earth receive time for the station
	 * @param dssId  receiving station ID
	 * @param sleMetadata SLE metadata
	 *
	 * @return  IStationTelemInfo object
	 */
	public IStationTelemInfo create(double bitRate, int numBits, IAccurateDateTime ert, int dssId,
	                                Map<String, String> sleMetadata);

    /**
     * Creates an empty IStationTelemInfo object.
     * 
     * @return  IStationTelemInfo object
     */
    public IStationTelemInfo create();
    
	/**
	 * Recreates an IStationTelemInfo object from a Protobuf message
	 * 
	 * @param msg
	 *            the Protobuf message with all station telem info
	 * @return IStationTelemInfoObject
	 */
    public IStationTelemInfo create(Proto3StationTelemInfo msg);

}