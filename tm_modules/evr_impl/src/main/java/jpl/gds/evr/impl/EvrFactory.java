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
/**
 * 
 */
package jpl.gds.evr.impl;

import jpl.gds.dictionary.api.evr.IEvrDefinition;
import jpl.gds.evr.api.EvrMetadata;
import jpl.gds.evr.api.IEvr;
import jpl.gds.evr.api.IEvrFactory;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ILocalSolarTime;
import jpl.gds.shared.time.ISclk;

/**
 * <code>EvrFactory</code> is used to create <code>IEvr</code> objects.
 * <p>
 * <b>MULTI-MISSION CORE ADAPTATION CLASS
 * <p>
 * This is a controlled class. It may not be updated without applicable change
 * requests being filed, and approval of project management. A new version tag
 * must be added below with each revision, and both ECR number and author must
 * be included with the version number.</b>
 * <p>
 * An <code>IEvr</code> object is the multi-mission representation of an EVent
 * Record. EVR extraction adapter implementations must
 * create <code>IEvr</code> objects via this factory.
 * <p>
 * This class contains only static methods. Once the <code>IEvr</code> object is
 * returned by this factory, its additional members can be set through the
 * methods in the <code>IEvr</code> interface.
 * 
 *
 * @see IEvr
 * @see IEvrDefinition
 */
public class EvrFactory implements IEvrFactory {
    
	/**
	 * Creates an EVR object.
	 * 
	 * @return the new <code>IEvr</code> object
	 */
	@Override
    public IEvr createEvr() {
		return new Evr();
	}
	
	@Override
    public IEvr createEvr(final IEvrDefinition evrDef, final IAccurateDateTime scet,
                          final IAccurateDateTime ert, final IAccurateDateTime rct, final ISclk sclk,
            final ILocalSolarTime sol, final String message,
            final EvrMetadata metadata, final boolean fromSse,
            final byte    dssId,
            final Integer vcid) {
	    return new Evr(evrDef, scet, ert, rct, sclk,
	            sol, message, metadata, fromSse, dssId, vcid);
	}

}