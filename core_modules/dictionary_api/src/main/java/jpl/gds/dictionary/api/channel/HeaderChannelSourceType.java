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
package jpl.gds.dictionary.api.channel;

/**
 * HeaderChannelSourceType is an enumeration that defines all the valid
 * telemetry header types from which header channels can be extracted.
 * <br>
 * <br><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br><br>
 * 
 * HeaderChannelSourceType is an enumeration of supported telemetry header
 * channel sources. Every HeaderChannelDefinition object has at least one
 * associated header field.  It is possible for it to have both a packet
 * source and an SFDU source, or a frame source and an SFDU source, to allow
 * extraction of the fields when multiple telemetry formats are used,
 * but there should really be no more than two.
 *
 */
public enum HeaderChannelSourceType {
    /** Channel matches a field in the primary or secondary space packet header. */
    PACKET_SOURCE,
    
    /** Channel matches a field in the primary transfer frame header. */
    FRAME_SOURCE,
    
    /** Channel matches a field in a CHDO SFDU header, either frame or packet */
    SFDU_SOURCE
}