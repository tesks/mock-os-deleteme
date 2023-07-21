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
package jpl.gds.db.api.sql.order;

/**
 * OrderByType is an enumeration that defines all the valid types of
 * product messages
 * <p>
 * <b>MULTI-MISSION CORE ADAPTATION CLASS
 * <p>
 * This is a controlled class. It may not be updated without applicable change
 * requests being filed, and approval of project management. A new version tag
 * must be added below with each revision, and both ECR number and author must
 * be included with the version number.</b>
 * <p>
 * OrderByType is an enumeration that defines all the valid types of
 * data fields that can be decommutated. The OrderByType is set when the
 * IDbOrderByType it applies to is created via the ProductDecomFieldFactory.
 * 
 * @version 1.0 - Initial Implementation
 */
public enum OrderByType {
    /** Channel Value OrderBy */
    CHANNEL_VALUE_ORDER_BY,
    /** Command OrderBy */
    COMMAND_ORDER_BY,
    /** ECDR Monitor OrderBy */
    ECDR_MONITOR_ORDER_BY,
    /** ECDR OrderBy */
    ECDR_ORDER_BY,
    /** EVR OrderBy */
    EVR_ORDER_BY,
    /** Frame OrderBy */
    FRAME_ORDER_BY,
    /** Log OrderBy */
    LOG_ORDER_BY,
    /** Packet OrderBy */
    PACKET_ORDER_BY,
    /** Product OrderBy */
    PRODUCT_ORDER_BY,
    /** Session OrderBy */
    SESSION_ORDER_BY,
    /** Channel Aggregate OrderBy */
    CHANNEL_AGGREGATE_ORDER_BY,
    
}
