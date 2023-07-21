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

import jpl.gds.serialization.station.Proto3StationTelemInfo;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.station.api.IStationInfoFactory;
import jpl.gds.station.api.IStationTelemInfo;

import java.util.Map;

/**
 * A factory that creates station information objects.
 * 
 *
 * @since R8
 */
public class StationInfoFactory implements IStationInfoFactory {

    @Override
    public IStationTelemInfo create(final double bitRate, final int numBits,
                                    final IAccurateDateTime ert, final int dssId){
        final IStationTelemInfo info = create();
        if (info == null) {
            return null;
        }

        info.setBitRate(bitRate);
        info.setDssId(dssId);
        info.setErt(ert);
        info.setNumBitsReceived(numBits);
        return info;
    }

    @Override
    public IStationTelemInfo create(final double bitRate, final int numBits,
                                    final IAccurateDateTime ert, final int dssId,
                                    final Map<String, String> sleMetadata){
        final IStationTelemInfo info = create(bitRate, numBits, ert, dssId);
        if (info == null) {
            return null;
        }

        info.setSleMetadata(sleMetadata);
        return info;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public IStationTelemInfo create() {
        return new StationTelemInfo();  
    }
    
    public IStationTelemInfo create(Proto3StationTelemInfo msg) {
    	final IStationTelemInfo info = create();
    	if(msg == null) {
    		return null;
    	}
    	info.load(msg);
    	
    	return info;
    }
    
}
