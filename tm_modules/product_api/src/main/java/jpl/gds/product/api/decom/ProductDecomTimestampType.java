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
package jpl.gds.product.api.decom;
/**
 * ProductDecomTimestampType is an enumeration that defines all the valid types of
 * time stamp fields that can be used for timestamping channels generated
 * during decommutation of a data stream.
 * <p>
 * <b>MULTI-MISSION CORE ADAPTATION CLASS
 * <p>
 * This is a controlled class. It may not be updated without applicable change
 * requests being filed, and approval of project management. A new version tag
 * must be added below with each revision, and both ECR number and author must
 * be included with the version number.</b>
 * <p>
 * TimestampType is an enumeration that defines all the valid types of
 * time stamp fields that can be used for timestamping channels generated
 * during decommutation of a data stream. Any data field used for timestamping
 * channels must have an associated TimestampType.
 * 
 *
 * @see IChannelTimestampSupport
 */
public enum ProductDecomTimestampType {
    /**
     * Field is an absolute time stamp. 
     */
    ABSOLUTE,
    /**
     * Field is a base timestamp, which may be used by itself or be subsequently
     * adjusted by DELTA time values.
     */
    BASE,
    /**
     * Field is a delta to the previous BASE timestamp.
     */
    DELTA;
}
