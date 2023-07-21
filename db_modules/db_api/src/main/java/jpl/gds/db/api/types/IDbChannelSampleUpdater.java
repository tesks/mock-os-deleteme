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

import java.util.Map;

import jpl.gds.dictionary.api.channel.ChannelType;
import jpl.gds.eha.api.channel.ChannelCategoryEnum;
import jpl.gds.shared.holders.ApidHolder;
import jpl.gds.shared.holders.ApidNameHolder;
import jpl.gds.shared.holders.PacketIdHolder;
import jpl.gds.shared.holders.SpscHolder;
import jpl.gds.shared.holders.VcfcHolder;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ILocalSolarTime;
import jpl.gds.shared.time.ISclk;

public interface IDbChannelSampleUpdater extends IDbChannelSampleProvider {
    /**
     * Set additional columns from Packet. Not NULL.
     *
     * @param pktApid
     *            APID
     * @param pktApidName
     *            APID name
     * @param pktSpsc
     *            SPSC
     * @param pktRct
     *            Packet RCT
     * @param pktVcfc
     *            Source VCFC
     */
    void setPacketInfo(final ApidHolder pktApid,
                              final ApidNameHolder pktApidName,
                              final SpscHolder pktSpsc,
                              final IAccurateDateTime pktRct,
                              final VcfcHolder pktVcfc);

    /**
     * XML root name
     */
    String XML_ROOT_NAME = "EhaChannelValue";

    /**
     * Set VCID.
     *
     * @param vcid the VCID to set
     */
    void setVcid(Integer vcid);

    /**
     * Sets the deltaValue
     *
     * @param deltaValue The deltaValue to set.
     */
    void setDeltaValue(Object deltaValue);

    /**
     * Set previous value.
     *
     * @param previousValue Previous value
     */
    void setPreviousValue(Object previousValue);

    /**
     * Sets the S/C id.
     *
     * @param id The value to set
     */
    void setSpacecraftId(Integer id);

    /**
     * Sets the name
     *
     * @param name The value to set
     */
    void setName(String name);

    /**
     * Sets the status
     *
     * @param status The status to set.
     */
    void setStatus(String status);

    /**
     * Set from-SSE state.
     *
     * @param fromSse Value to set
     */
    void setFromSse(Boolean fromSse);

    /**
     * Set is-realtime state.
     *
     * @param isRealtime Value to set
     */
    void setIsRealtime(Boolean isRealtime);

    /**
     * Set SCLK.
     *
     * @param sclk Value to set
     */
    void setSclk(ISclk sclk);

    /**
     * Set SCET
     *
     * @param scet Value to set
     */
    void setScet(IAccurateDateTime scet);

    /**
     * Set LST.
     *
     * @param sol Value to set
     */
    void setLst(ILocalSolarTime sol);

    /**
     * Set ERT.
     *
     * @param ert Value to set
     */
    void setErt(IAccurateDateTime ert);

    /**
     * Set data value.
     *
     * @param value Value to set
     */
    void setValue(Object value);

    /**
     * Set channel type.
     *
     * @param channelType Value to set
     */
    void setChannelType(ChannelType channelType);

    /**
     * Set channel id.
     *
     * @param channelId Value to set
     */
    void setChannelId(String channelId);

    /**
     * Set channel index.
     *
     * @param channelIndex Value to set
     */
    void setChannelIndex(Long channelIndex);

    /**
     * Set module.
     *
     * @param module Value to set
     */
    void setModule(String module);

    /**
     * Set EU value.
     *
     * @param eu Value to set
     */
    void setEu(Double eu);

    /**
     * Set DN alarm state.
     *
     * @param dnAlarmState Value to set
     */
    void setDnAlarmState(String dnAlarmState);

    /**
     * Set EU alarm state.
     *
     * @param euAlarmState Value to set
     */
    void setEuAlarmState(String euAlarmState);

    /**
     * Set RCT
     *
     * @param rct RCT
     */
    void setRct(IAccurateDateTime rct);

    /**
     * Set packet id
     *
     * @param packetId Packet id
     */
    void setPacketId(PacketIdHolder packetId);

    /**
     * Used by GetEverythingApp to calculate the delta value. Adds
     * <"delta",calculated delta value> to the passed map. 
     *
     * @param prevVal Previous value
     * @param map     Map to augment
     * @param NO_DATA Value used when no data
     */
    void calculateDelta(Object prevVal, Map<String, String> map, String NO_DATA);

    /**
     * @param format
     *            the format to use when formatting the dn value
     */
    void setDnFormat(String format);

    /**
     * @param format
     *            the format to use when formatting the eu value
     */
    void setEuFormat(String format);

    /**
     * Set APID.
     * 
     * @param ap
     *            APID or null
     */
    void setApid(ApidHolder ap);

    /**
     * Set SPSC.
     *
     * @param s SPSC
     */
    void setSpsc(SpscHolder s);

    /**
     * Set packet RCT
     *
     * @param rct RCT
     */
    void setPacketRct(IAccurateDateTime rct);

    /**
     * Set VCFC.
     *
     * @param v
     *            the frame's VCFC (in a holder)
     */
    void setVcfc(VcfcHolder v);

    /**
     * Set category.
     *
     * @param cce Category
     */
    void setCategory(ChannelCategoryEnum cce);
}