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
package jpl.gds.evr.api;

import jpl.gds.dictionary.api.evr.IEvrDefinition;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ILocalSolarTime;
import jpl.gds.shared.time.ISclk;

/**
 * An interface to be implemented by EVR object factories.
 * 
 * @since R8
 */
public interface IEvrFactory {

    /**
     * Create an empty EVR.
     * 
     * @return new EVR object instance
     */
    public IEvr createEvr();
    
    /**
     * Create a populated EVR.
     * 
     * @param evrDef the EVR definition object
     * @param scet the spacecraft event time
     * @param ert the earth received time
     * @param rct the record creation time
     * @param sclk the spacecraft clock time
     * @param sol the local solar time
     * @param message the EVR message string
     * @param metadata the EVR metadata object
     * @param fromSse true if the EVR is from SSE, false if from flight
     * @param dssId the station ID associated with the EVR
     * @param vcid the virtual channel ID associated with the EVR
     * @return new EVR object instance
     */
    public IEvr createEvr(final IEvrDefinition evrDef, final IAccurateDateTime scet,
            final IAccurateDateTime ert, final IAccurateDateTime rct, final ISclk sclk,
            final ILocalSolarTime sol, final String message,
            final EvrMetadata metadata, final boolean fromSse,
            final byte    dssId,
            final Integer vcid);

}
