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
package jpl.gds.dictionary.api.config;

import jpl.gds.dictionary.api.frame.ITransferFrameDefinition;

/**
 * FrameErrorControlType is a enumeration class used to identify built-in
 * calculations for the Frame Error Control Field (FECF) of a transfer frame. <br>
 * <br>
 * <b>THIS IS A MULTI-MISSION CORE ADAPTATION INTERFACE</b><br>
 * <br>
 * <b>This is a controlled interface. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b><br>
 * <br>
 * 
 *
 *
 * @see ITransferFrameDefinition
 */
public enum FrameErrorControlType {

    /**
     * The CCSDS CRC-16 computation, as defined by CCSDS 132.0-B-1 or CCSDS
     * 732.0-B-2.
     */
    CCSDS_CRC_16("jpl.gds.shared.checksum.CcsdsCrc16ChecksumAdaptor");

    private String defaultFrameErrorClass;

    /**
     * Constructor. The supplied class must implement the
     * IFrameChecksumComputation interface.
     * 
     * @param frameErrorClass
     *            full Java package and class name of the FECF computation class
     */
    private FrameErrorControlType(String frameErrorClass) {
        this.defaultFrameErrorClass = frameErrorClass;
    }

    /**
     * Gets the full Java class name of the FECF computation class. This class
     * must implement the IFrameChecksumComputation interface.
     * 
     * @return class name including package
     */
    public String getDefaultFrameErrorClass() {
        return this.defaultFrameErrorClass;
    }
}
