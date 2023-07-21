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
package jpl.gds.db.api.types;

import jpl.gds.db.api.DatabaseException;
import jpl.gds.eha.api.channel.ChannelCategoryEnum;
import jpl.gds.shared.string.StringUtil;


/**
 * Types of header channel parents. Must match HeaderChannelValue.parentType
 * in database schema. In the code we extend this with FSW, SSE, and MONITOR.
 *
 */
public enum ParentTypeEnum
{
    /** Frame header channel value */
    FRAME,

    /** Packet header channel value */
    PACKET,

    /** SSE packet header channel value */
    SSEPACKET,

    /** Lost header channel value (old data only) */
    LOST,

    // Above is the databass enum. Below are extensions we return as aliases.

    /** FSW channel value */
    FSW,

    /** SSE channel value */
    SSE,

    /** MONITOR channel value */
    MONITOR;
    
    /**
     * Convert from extended ParentTypeEnum returned from database.
     *
     * @param s String from parentType column
     *
     * @return Corresponding ChannelCategoryEnum
     *
     * @throws DatabaseException Bad enum received from database
     */
    public static ChannelCategoryEnum convertFromExtendedEnum(final String s)
        throws DatabaseException 
    {
        ParentTypeEnum pte = null;

        try
        {
            pte = ParentTypeEnum.valueOf(StringUtil.safeTrimAndUppercase(s));
        }
        catch (IllegalArgumentException iae)
        {
            throw new DatabaseException("Unexpected parentType: " + pte, iae);
        }

        switch (pte)
        {
            case FRAME:
                return ChannelCategoryEnum.FRAME_HEADER;

            case PACKET:
                return ChannelCategoryEnum.PACKET_HEADER;

            case SSEPACKET:
                return ChannelCategoryEnum.SSEPACKET_HEADER;

            case LOST:
                return ChannelCategoryEnum.LOST_HEADER;

            case FSW:
                return ChannelCategoryEnum.FSW;

            case SSE:
                return ChannelCategoryEnum.SSE;

            case MONITOR:
                return ChannelCategoryEnum.MONITOR;

            default:
                break;
        }

        // Shouldn't happen
        throw new DatabaseException("Unexpected parentType: " + pte);
    }
}

