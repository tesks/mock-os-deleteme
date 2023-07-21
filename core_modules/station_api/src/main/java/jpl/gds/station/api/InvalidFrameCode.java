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
package jpl.gds.station.api;


/**
 * The InvalidFrameCode class is used to tag invalid frames with a reason for their invalid status.

 *
 */
public enum InvalidFrameCode {
    
     /**
     * Invalid frame reason: RS error
     */
    RS_ERROR,
    /**
     * Invalid frame reason: CRC error
     */
    CRC_ERROR,
    /**
     * Invalid frame reason: bad version
     */
    BAD_VERSION,
    /**
     * Invalid frame reason: bad scid
     */
    BAD_SCID,
    /**
     * Invalid frame reason: bad header
     */
     BAD_HEADER,
     /**
      * Invalid frame reason: unknown
      */
     UNKNOWN,
     /**
      * Invalid frame reason: bad vcid
      */
     BAD_VCID,
     /**
      * Invalid frame reason: bad packet pointer
      */
     BAD_PKT_POINTER,
     /**
      * Invalid frame reason: turbo error
      */
     TURBO_ERROR
}

