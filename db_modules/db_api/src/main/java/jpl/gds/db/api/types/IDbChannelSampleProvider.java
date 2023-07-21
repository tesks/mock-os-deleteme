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
import jpl.gds.shared.template.FullyTemplatable;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.ILocalSolarTime;

public interface IDbChannelSampleProvider extends IDbQueryable, FullyTemplatable {

    /**
     * Get VCID.
     *
     * @return the VCID
     */
    Integer getVcid();

    /**
     * Get SCLK
     *
     * @return Returns the ert.
     */
    ISclk getSclk();

    /**
     * Get ERT
     *
     * @return Returns the ert.
     */
    IAccurateDateTime getErt();

    /**
     * Get SCET
     *
     * @return Returns the scet.
     */
    IAccurateDateTime getScet();

    /**
     * Get LST.
     *
     * @return Returns the LST.
     */
    ILocalSolarTime getLst();

    /**
     * Get value.
     *
     * @return Returns the value as as Object.
     */
    Object getValue();

    /**
     * Get module.
     *
     * @return Returns the module name.
     */
    String getModule();

    /**
     * Get channel id.
     *
     * @return Returns the channelId.
     */
    String getChannelId();

    /**
     * Get channel index.
     *
     * @return Returns the channelIndex.
     */
    Long getChannelIndex();

    /**
     * Get channel type.
     *
     * @return Returns the channelType.
     */
    ChannelType getChannelType();

    /**
     * Get EU.
     *
     * @return Returns the EU value, or null if none defined.
     */
    Double getEu();

    /**
     * Set from-sse state.
     *
     * @return Returns the Sse value, or null if none defined.
     */
    Boolean getFromSse();

    /**
     * Get delta value
     *
     * @return Returns the deltaValue.
     */
    Object getDeltaValue();

    /**
     * Get EU alarm state.
     *
     * @return Returns the dnAlarmState.
     */
    String getDnAlarmState();

    /**
     * Get EU alarm state.
     *
     * @return Returns the euAlarmState.
     */
    String getEuAlarmState();

    /**
     * Get status.
     *
     * @return Returns the status.
     */
    String getStatus();

    /**
     * Get spacecraft id.
     *
     * @return Integer
     */
    Integer getSpacecraftId();

    /**
     * Get name.
     *
     * @return String
     */
    String getName();

    /**
     * Get is-realtime status.
     *
     * @return Boolean
     */
    Boolean getIsRealtime();

    /**
     * Get MTAK field count.
     *
     * @return int
     */
    @Override
    int getMtakFieldCount();

    /**
     * Get previous value.
     *
     * @return Object
     */
    Object getPreviousValue();

    /**
     * Get RCT
     *
     * @return RCT
     */
    IAccurateDateTime getRct();

    /**
     * Get packet id
     *
     * @return Packet id
     */
    PacketIdHolder getPacketId();

    /**
     * Get frame id
     *
     * @return Frame id
     */
    Long getFrameId();

    /**
     * Set frame id
     *
     * @param frameId Frame id
     */
    void getFrameId(Long frameId);

    /**
     * Used by GetEverythingApp. Returns a map of data to be displayed to
     * various output files.
     *
     * @param NO_DATA is the string to be used to represent no data.
     *
     * @return Map<String, String>
     */
    @Override
    Map<String, String> getFileData(String NO_DATA);

    /**
     * @return the dn format
     */
    String getDnFormat();

    /**
     * @return the eu format
     */
    String getEuFormat();

    /**
     * Get APID.
     *
     * @return APID or null
     */
    ApidHolder getApid();

    /**
     * Get APID name.
     *
     * @return APID name or null
     */
    ApidNameHolder getApidName();

    /**
     * Get SPSC.
     *
     * @return SPSC or null
     */
    SpscHolder getSpsc();

    /**
     * Get Packet RCT
     *
     * @return Packet RCT or null
     */
    IAccurateDateTime getPacketRCT();

    /**
     * Get VCFC.
     *
     * @return VCFC or null
     */
    VcfcHolder getVcfc();

    /**
     * Get category.
     *
     * @return Category
     */
    ChannelCategoryEnum getCategory();
}