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

import java.util.List;

import jpl.gds.shared.holders.PacketIdHolder;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ILocalSolarTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.station.api.InvalidFrameCode;

public interface IDbPacketProvider extends IDbQueryable, IDbRawData {

    /**
     * Get id.
     *
     * @return Id
     */
    @Override
    PacketIdHolder getPacketId();

    /**
     * Get SCET.
     *
     * @return SCET
     */
    IAccurateDateTime getScet();

    /**
     * Get ERT.
     *
     * @return ERT
     */
    @Override
    IAccurateDateTime getErt();

    /**
     * Get SCLK.
     *
     * @return SCLK
     */
    ISclk getSclk();

    /**
     * Get VCID.
     *
     * @return VCID
     */
    Integer getVcid();

    /**
     * Get frame id.
     *
     * @return Frame id
     */
    @Override
    Long getFrameId();

    /**
     * Get VCFCS.
     *
     * @return VCFCS
     */
    List<Long> getVcfcs();

    /**
     * Dummy routine to satisfy interface.
     * Bad reason not implemented for Packet anyway.
     *
     * @return Null always
     */
    @Override
    InvalidFrameCode getBadReason();

    @Override
    Double getBitRate();

    /* 
     * MPCS-6349 : DSS ID not set properly
     * Removed dssId. Parent class has been updated with 
     * protected fields sessionDssId and recordDssId with get/set 
     * methods for both.
     */
    @Override
    Integer getRecordDssIdAsInt();

    /**
     * Get SPSC.
     *
     * @return SPSC
     */
    Integer getSpsc();

    /**
     * Get RCT.
     *
     * @return RCT
     */
    IAccurateDateTime getRct();

    /**
     * Get LST.
     *
     * @return LST
     */
    ILocalSolarTime getLst();

    /**
     * Get APID.
     *
     * @return APID
     */
    Integer getApid();

    /**
     * Get APID name.
     *
     * @return APID name
     */
    String getApidName();

    /**
     * Get from-SSE state.
     *
     * @return From-SSE state
     */
    Boolean getFromSse();

    /**
     * Get fill state.
     *
     * @return Fill state
     */
    boolean getFillFlag();

}