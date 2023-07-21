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

import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.station.api.InvalidFrameCode;

public interface IDbFrameUpdater extends IDbFrameProvider {

    /**
     * Set type.
     *
     * @param type
     *            Type
     */
    void setType(String type);

    /**
     * Set ERT.
     *
     * @param ert ERT
     */
    void setErt(IAccurateDateTime ert);

    /**
     * Set RCT.
     *
     * @param rct RCT
     */
    void setRct(IAccurateDateTime rct);

    /**
     * Set fill frame.
     *
     * @param fillFrame fill frame
     */
    void setFillFrame(boolean fillFrame);

    /**
     * Set relay S/C id.
     *
     * @param relaySpacecraftId Relay S/C id
     */
    void setRelaySpacecraftId(Integer relaySpacecraftId);

    /**
     * Set VCID.
     *
     * @param vcid VCID
     */
    void setVcid(Integer vcid);

    /**
     * Set VCFC.
     *
     * @param vcfc VCFC
     */
    void setVcfc(Integer vcfc);

    /**
     * Set bit rate.
     *
     * @param bitRate Bit rate
     */
    void setBitRate(Double bitRate);

    /**
     * Set reason frame is bad.
     *
     * @param badReason Bad reason
     */
    void setBadReason(InvalidFrameCode badReason);

    /**
     * Set id.
     *
     * @param id Id
     */
    void setId(Long id);

    /**
     * Set SLE metadata
     * @param sleMetadata SLE metadata
     */
    void setSleMetadata(String sleMetadata);

}