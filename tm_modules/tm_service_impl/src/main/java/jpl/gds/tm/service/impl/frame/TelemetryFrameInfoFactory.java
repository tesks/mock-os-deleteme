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
package jpl.gds.tm.service.impl.frame;

import jpl.gds.ccsds.api.tm.frame.ITelemetryFrameHeader;
import jpl.gds.dictionary.api.frame.ITransferFrameDefinition;
import jpl.gds.tm.service.api.frame.ITelemetryFrameInfo;
import jpl.gds.tm.service.api.frame.ITelemetryFrameInfoFactory;

/**
 * FrameInfoFactory is used to create mission-specific instances of classes
 * that implement IFrameInfo.
 * <p>
 * <b>MULTI-MISSION CORE ADAPTATION CLASS
 * <p>
 * This is a controlled class. It may not be updated without applicable change
 * requests being filed, and approval of project management. A new version tag
 * must be added below with each revision, and both ECR number and author must
 * be included with the version number.</b>
 * <p>
 * Instances of IFrameInfo are used by the telemetry processing system to
 * parse and access frame header information. An appropriate IFrameInfo
 * object must be used for each mission, since frame formats may differ.
 * IFrameInfo objects should only be created via the FrameInfoFactory.
 * Direct creation of an IFrameInfo object is a violation of multi-mission
 * development standards.
 * <p>
 * 
 * @version 1.0 - Initial Implementation
 * @version 1.1 - Added create method that takes header and format objects
 *                (MPCS-3923 - 11/4/14)
 * @version 2.0 - Now caches objects created by reflection for performance.
 *                (MPCS-7215)    
 *
 * @see ITelemetryFrameHeader
 */
final public class TelemetryFrameInfoFactory implements ITelemetryFrameInfoFactory {

 
    /**
     * {@inheritDoc}
     */
    @Override
    public ITelemetryFrameInfo create() {
        return new TelemetryFrameInfo();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ITelemetryFrameInfo create(final int scid, final int vc, final int seq) {
       return new TelemetryFrameInfo(scid, vc, seq);  
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ITelemetryFrameInfo create(final ITelemetryFrameHeader header, final ITransferFrameDefinition format) {
        return new TelemetryFrameInfo(header, format);  
    }

}
