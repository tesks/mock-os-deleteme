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

import jpl.gds.evr.api.EvrMetadata;
import jpl.gds.shared.holders.PacketIdHolder;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ILocalSolarTime;
import jpl.gds.shared.time.ISclk;

public interface IDbEvrProvider extends IDbQueryable {

    /**
     * Get id.
     *
     * @return Id
     */
    Long getId();

    /**
     * Get name.
     *
     * @return Name
     */
    String getName();

    /**
     * Get event id.
     *
     * @return Event id
     */
    Long getEventId();

    /**
     * Get ERT.
     *
     * @return ERT
     */
    IAccurateDateTime getErt();

    /**
     * Get SCET.
     *
     * @return SCET
     */
    IAccurateDateTime getScet();

    /**
     * Get LST.
     *
     * @return LST
     */
    ILocalSolarTime getLst();

    /**
     * Get RCT.
     *
     * @return RCT
     */
    IAccurateDateTime getRct();

    /**
     * Get SCLK.
     *
     * @return SCLK
     */
    ISclk getSclk();

    /**
     * Get level.
     *
     * @return Level
     */
    String getLevel();

    /**
     * Get module.
     *
     * @return Module
     */
    String getModule();

    /**
     * Get message.
     *
     * @return Message
     */
    String getMessage();

    /**
     * Get from-SSE state.
     *
     * @return From-SSE state
     */
    Boolean getFromSse();

    /**
     * Get real-time state.
     *
     * @return Real-time state
     */
    Boolean getIsRealtime();

    /**
     * Get VCID.
     *
     * @return VCID
     */
    Integer getVcid();

    /**
     * Get packet id.
     *
     * @return packet id
     * @version MPCS-5935 Use holder
     */
    PacketIdHolder getPacketId();

    /**
     * Get metadata.
     *
     * @return Metadata
     */
    EvrMetadata getMetadata();

}