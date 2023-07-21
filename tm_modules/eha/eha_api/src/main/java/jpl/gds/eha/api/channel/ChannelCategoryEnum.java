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
package jpl.gds.eha.api.channel;

/**
 * Categories of channel values. These correspond to the classes ChannelValue,
 * SseChannelValue, MonitorChannelValue, and HeaderChannelValue, except that
 * header channels are further broken down into their parent classes.
 *
 */
public enum ChannelCategoryEnum {
    /** FSW channel value */
    FSW,

    /** SSE channel value */
    SSE,

    /** Monitor channel value */
    MONITOR,

    /** Frame header channel value */
    FRAME_HEADER,

    /** Packet header channel value */
    PACKET_HEADER,

    /** SSE packet header channel value */
    SSEPACKET_HEADER,

    /** Lost header channel value (old data only) */
    LOST_HEADER;
}
