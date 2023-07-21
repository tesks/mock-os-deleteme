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


public interface IDbPacketUpdater extends IDbPacketProvider {

    /**
     * Set id.
     *
     * @param id Id
     */
    void setPacketId(PacketIdHolder id);

    /**
     * Set RCT.
     *
     * @param rct RCT
     */
    void setRct(IAccurateDateTime rct);

    /**
     * Set SCET.
     *
     * @param scet SCET
     */
    void setScet(IAccurateDateTime scet);

    /**
     * Set LST.
     *
     * @param sol LST
     */
    void setLst(ILocalSolarTime sol);

    /**
     * Set ERT.
     *
     * @param ert ERT
     */
    void setErt(IAccurateDateTime ert);

    /**
     * Set SCLK.
     *
     * @param sclk SCLK
     */
    void setSclk(ISclk sclk);

    /**
     * Set APID.
     *
     * @param apid APID
     */
    void setApid(Integer apid);

    /**
     * Set APID name.
     *
     * @param apidName APID name
     */
    void setApidName(String apidName);

    /**
     * Set VCID.
     *
     * @param vcid VCID
     */
    void setVcid(Integer vcid);

    /**
     * Set SPSC.
     *
     * @param spsc SPSC
     */
    void setSpsc(Integer spsc);

    /**
     * Set from-SSE state
     *
     * @param fromSse From-SSE state
     */
    void setFromSse(Boolean fromSse);

    /**
     * Set fill flag
     *
     * @param fill Fill flag
     */
    void setFillFlag(boolean fill);

    /**
     * Set frame id.
     *
     * @param frameId Frame id
     */
    void setFrameId(Long frameId);

    /**
     * Set source VCFCS.
     *
     * @param sourceVcfcs Source VCFCS
     */
    void setSourceVcfcs(List<Long> sourceVcfcs);
}