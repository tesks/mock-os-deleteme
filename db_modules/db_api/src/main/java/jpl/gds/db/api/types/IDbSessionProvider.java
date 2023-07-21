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
import java.util.Map;

import jpl.gds.common.config.types.DownlinkStreamType;
import jpl.gds.common.config.types.TelemetryConnectionType;
import jpl.gds.common.config.types.TelemetryInputType;
import jpl.gds.common.config.types.UplinkConnectionType;
import jpl.gds.common.config.types.VenueType;
import jpl.gds.shared.time.IAccurateDateTime;

public interface IDbSessionProvider extends IDbQueryable {

    /**
     * Get name
     *
     * @return Returns the name.
     */
    String getName();

    /**
     * Get type
     *
     * @return Returns the type.
     */
    String getType();

    /**
     * Get description
     *
     * @return Returns the description.
     */
    String getDescription();

    /**
     * Get full name
     *
     * @return Returns the fullName.
     */
    String getFullName();

    /**
     * Get user
     *
     * @return Returns the user.
     */
    String getUser();

    /**
     * Get connection type
     *
     * @return Returns the connection type.
     */
    TelemetryConnectionType getConnectionType();

    /**
     * Get uplink connection type
     *
     * @return Returns the connection type.
     */
    UplinkConnectionType getUplinkConnectionType();

    /**
     * Get output directory
     *
     * @return Returns the outputDirectory.
     */
    String getOutputDirectory();

    /**
     * Get FSW dictionary directory
     *
     * @return Returns the fswDictionaryDir.
     */
    String getFswDictionaryDir();

    /**
     * Get SSE dictionary directory
     *
     * @return Returns the sseDictionaryDir.
     */
    String getSseDictionaryDir();

    /**
     * Get SSE version
     *
     * @return Returns the sseVersion.
     */
    String getSseVersion();

    /**
     * Get FSW version
     *
     * @return Returns the fswVersion.
     */
    String getFswVersion();

    /**
     * Get venue type
     *
     * @return Returns the venueType.
     */
    VenueType getVenueType();

    /**
     * Get testbed name
     *
     * @return Returns the testbedName.
     */
    String getTestbedName();

    /**
     * Get raw input type
     *
     * @return Returns the rawInputType.
     */
    TelemetryInputType getRawInputType();

    /**
     * Get start time
     *
     * @return Returns the startTime.
     */
    IAccurateDateTime getStartTime();

    /**
     * Get end time
     *
     * @return Returns the endTime.
     */
    IAccurateDateTime getEndTime();

    /**
     * Get S/C id
     *
     * @return Returns the spacecraftId.
     */
    Integer getSpacecraftId();

    /**
     * Get downlink stream id
     *
     * @return Returns the downlinkStreamId.
     */
    DownlinkStreamType getDownlinkStreamId();

    /**
     * Get MPCS version
     *
     * @return Returns the mpcsVersion.
     */
    String getMpcsVersion();

    /**
     * Get FSW downlink host
     *
     * @return Returns the fswDownlinkHost.
     */
    String getFswDownlinkHost();

    /**
     * Get FSW uplink host
     *
     * @return Returns the fswUplinkHost.
     */
    String getFswUplinkHost();

    /**
     * Get FSW uplink port
     *
     * @return Returns the fswUplinkPort.
     */
    Integer getFswUplinkPort();

    /**
     * Get FSW downlink port
     *
     * @return Returns the fswDownlinkPort.
     */
    Integer getFswDownlinkPort();

    /**
     * Get SSE host
     *
     * @return Returns the sseHost.
     */
    String getSseHost();

    /**
     * Get SSE uplink port
     *
     * @return Returns the sseUplinkPort.
     */
    Integer getSseUplinkPort();

    /**
     * Get SSE downlink port
     *
     * @return Returns the sseDownlinkPort.
     */
    Integer getSseDownlinkPort();

    /**
     * Get input file
     *
     * @return Input file
     */
    String getInputFile();

    /**
     * Get topic.
     *
     * @return Topic
     */
    String getTopic();

    /**
     * Get the outputDirectoryOverride state.
     *
     * @return State
     */
    Boolean getOutputDirectoryOverride();

    /**
     * Get the subtopic.
     *
     * @return subtopic
     */
    String getSubtopic();

    /**
     * Get the vcid.
     *
     * @return vcid
     */
    Long getVcid();

    /**
     * Get the fswDownlinkFlag.
     *
     * @return fswDownlinkFlag
     */
    Boolean getFswDownlinkFlag();

    /**
     * Get the sseDownlinkFlag.
     *
     * @return sseDownlinkFlag
     */
    Boolean getSseDownlinkFlag();

    /**
     * Get the uplinkFlag.
     *
     * @return uplinkFlag
     */
    Boolean getUplinkFlag();

    /**
     * Get the databaseSessionId.
     *
     * @return databaseSessionId
     */
    Long getDatabaseSessionId();

    /**
     * Get the databaseHost.
     *
     * @return databaseHost
     */
    String getDatabaseHost();

    /**
     * {@inheritDoc}
     * 
     * NOTE: This method is NOT threadsafe!!!! It utilizes unsynchronized
     * static variables to do its work for performance reasons.
     *
     * @version MPCS-6808 Massive rewrite
     */
    @Override
    String toCsv(List<String> csvColumns);

    /**
     * Returns a map of data to be displayed to various output files.
     *
     * @param NO_DATA is the string to be used to represent no data
     *
     * @return null (empty map)
     */
    @Override
    Map<String, String> getFileData(String NO_DATA);

}