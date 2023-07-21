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
package jpl.gds.tm.service.impl.packet;

import jpl.gds.ccsds.api.packet.ISpacePacketHeader;
import jpl.gds.ccsds.api.tm.packet.ISecondaryPacketHeader;
import jpl.gds.dictionary.api.apid.IApidDefinitionProvider;
import jpl.gds.tm.service.api.packet.ITelemetryPacketInfo;
import jpl.gds.tm.service.api.packet.ITelemetryPacketInfoFactory;

/**
 * PacketInfoFactory is used to create mission-specific instances of classes
 * that implement IPacketInfo.
 * <p>
 * <b>MULTI-MISSION CORE ADAPTATION CLASS
 * <p>
 * This is a controlled class. It may not be updated without applicable change
 * requests being filed, and approval of project management. A new version tag
 * must be added below with each revision, and both ECR number and author must
 * be included with the version number.</b>
 * <p>
 * Instances of IPacketInfo are used by the telemetry processing system to
 * parse and access packet header information. An appropriate IPacketInfo
 * object must be used for each mission, since packet formats may differ.
 * IPacketInfo objects should only be created via the PacketInfoFactory.
 * Direct creation of an IPacketInfo object is a violation of multi-mission
 * development standards.
 * <p>
 * 
 * @version 1.0 - Initial Implementation
 * @version 2.0 - Now caches objects created by reflection for performance (MPCS-7215)
 *
 * @see ISpacePacketHeader
 */
final public class TelemetryPacketInfoFactory implements ITelemetryPacketInfoFactory {
    
    private final IApidDefinitionProvider apidDefs;
    
    /**
     * Constructor.
     * 
     * @param apidDefs the APID dictionary instance, needed to set APID name in packet info
     * objects.
     */
    public TelemetryPacketInfoFactory(final IApidDefinitionProvider apidDefs) {
    	this.apidDefs = apidDefs;
    }
     
    /**
	 * {@inheritDoc}
	 * @see jpl.gds.tm.service.api.packet.ITelemetryPacketInfoFactory#create()
	 */
    @Override
	public ITelemetryPacketInfo create() {
        ITelemetryPacketInfo info = null;
        info = new TelemetryPacketInfo();
        info.setApidName(!isApidDefsAvailable() ? null : apidDefs.getApidName(info.getApid()));
		return info;
    }
    
    /**
	 * {@inheritDoc}
	 * @see jpl.gds.tm.service.api.packet.ITelemetryPacketInfoFactory#create(jpl.gds.ccsds.api.packet.ISpacePacketHeader, int)
	 */
    @Override
	public ITelemetryPacketInfo create(final ISpacePacketHeader header, final int entirePacketLength) {
        ITelemetryPacketInfo info = null;
        info = new TelemetryPacketInfo(header, entirePacketLength);
        info.setApidName(!isApidDefsAvailable() ? null : apidDefs.getApidName(info.getApid()));
		return info;
    }

    /**
	 * {@inheritDoc}
	 * @see jpl.gds.tm.service.api.packet.ITelemetryPacketInfoFactory#create(jpl.gds.ccsds.api.packet.ISpacePacketHeader, int, jpl.gds.ccsds.api.tm.packet.ISecondaryPacketHeader)
	 */
    @Override
	public ITelemetryPacketInfo create(final ISpacePacketHeader primaryHeader, final int entirePacketLength, final ISecondaryPacketHeader secondaryHeader ) {
        final ITelemetryPacketInfo info = create(primaryHeader, entirePacketLength);
        info.setSclk(secondaryHeader.getSclk());
        info.setSecondaryHeaderLength(secondaryHeader.getSecondaryHeaderLength());
        info.setApidName(!isApidDefsAvailable() ? null : apidDefs.getApidName(info.getApid()));
        return info;
    }
    
    private boolean isApidDefsAvailable() {
        return apidDefs != null && apidDefs.isLoaded();
    }

}
