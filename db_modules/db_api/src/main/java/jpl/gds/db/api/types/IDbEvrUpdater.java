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
import jpl.gds.evr.api.EvrMetadataKeywordEnum;
import jpl.gds.shared.holders.PacketIdHolder;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ILocalSolarTime;
import jpl.gds.shared.time.ISclk;

public interface IDbEvrUpdater extends IDbEvrProvider {

    /**
     * Set id.
     *
     * @param id Id
     */
    void setId(Long id);

    /**
     * Set name.
     *
     * @param name Name
     */
    void setName(String name);

    /**
     * Set event id.
     *
     * @param eventId Event id
     */
    void setEventId(Long eventId);

    /**
     * Set ERT.
     *
     * @param ert ERT
     */
    void setErt(IAccurateDateTime ert);

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
     * Set RCT.
     *
     * @param rct RCT
     */
    void setRct(IAccurateDateTime rct);

    /**
     * Set SCLK.
     *
     * @param sclk SCLK
     */
    void setSclk(ISclk sclk);

    /**
     * Set level.
     *
     * @param level Level
     */
    void setLevel(String level);

    /**
     * Set module.
     *
     * @param module Module
     */
    void setModule(String module);

    /**
     * Set message.
     *
     * @param message Message
     */
    void setMessage(String message);

    /**
     * Set from-SSE state.
     *
     * @param fromSse From-SSE state
     */
    void setFromSse(Boolean fromSse);

    /**
     * Set real-time state.
     *
     * @param isRealtime Real-time state
     */
    void setIsRealtime(Boolean isRealtime);

    /**
     * Set VCID.
     *
     * @param vcid VCID
     */
    void setVcid(Integer vcid);

    /**
     * Set packet id.
     *
     * @param packetId packet id
     * @version MPCS-5935 Use holder
     */
    void setPacketId(PacketIdHolder packetId);

    /**
     * Set metadata.
     *
     * @param em Metadata
     */
    void setMetadata(EvrMetadata em);

    /**
     * Add new key/value pair.
     *
     * @param key   Key
     * @param value Value
     */
    void addKeyValue(EvrMetadataKeywordEnum key, String value);

    /**
     * Add new key/value pair.
     *
     * @param key   Key
     * @param value Value
     */
    void addKeyValue(String key, String value);
}