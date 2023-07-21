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

import jpl.gds.common.config.types.DownlinkStreamType;
import jpl.gds.common.config.types.TelemetryConnectionType;
import jpl.gds.common.config.types.TelemetryInputType;
import jpl.gds.common.config.types.UplinkConnectionType;
import jpl.gds.common.config.types.VenueType;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.shared.time.IAccurateDateTime;

public interface IDbSessionUpdater extends IDbSessionProvider {

    /**
     * Sets the name
     *
     * @param name The name to set.
     */
    void setName(String name);

    /**
     * Sets the type
     *
     * @param type The type to set.
     */
    void setType(String type);

    /**
     * Sets the description
     *
     * @param description The description to set.
     */
    void setDescription(String description);

    /**
     * Sets the fullName
     *
     * @param fullName The fullName to set.
     */
    void setFullName(String fullName);

    /**
     * Sets the user
     *
     * @param user The user to set.
     */
    void setUser(String user);

    /**
     * Sets the connection type
     *
     * @param ct The connection type to set.
     */
    void setConnectionType(TelemetryConnectionType ct);

    /**
     * Sets the uplink connection type
     *
     * @param ct The connection type to set.
     */
    void setUplinkConnectionType(UplinkConnectionType ct);

    /**
     * Sets the outputDirectory
     *
     * @param outputDirectory The outputDirectory to set.
     */
    void setOutputDirectory(String outputDirectory);

    /**
     * Sets the fswDictionaryDir
     *
     * @param fswDictionaryDir The fswDictionaryDir to set.
     */
    void setFswDictionaryDir(String fswDictionaryDir);

    /**
     * Sets the sseDictionaryDir
     *
     * @param sseDictionaryDir The sseDictionaryDir to set.
     */
    void setSseDictionaryDir(String sseDictionaryDir);

    /**
     * Sets the sseVersion
     *
     * @param sseVersion The sseVersion to set.
     */
    void setSseVersion(String sseVersion);

    /**
     * Sets the fswVersion
     *
     * @param fswVersion The fswVersion to set.
     */
    void setFswVersion(String fswVersion);

    /**
     * Sets the venueType
     *
     * @param venueType The venueType to set.
     */
    void setVenueType(VenueType venueType);

    /**
     * Sets the testbedName
     *
     * @param testbedName The testbedName to set.
     */
    void setTestbedName(String testbedName);

    /**
     * Sets the rawInputType
     *
     * @param rawInputType The rawInputType to set.
     */
    void setRawInputType(TelemetryInputType rawInputType);

    /**
     * Sets the startTime
     *
     * @param startTime The startTime to set.
     */
    void setStartTime(IAccurateDateTime startTime);

    /**
     * Sets the endTime
     *
     * @param endTime The endTime to set.
     */
    void setEndTime(IAccurateDateTime endTime);

    /**
     * Sets the spacecraftId
     *
     * @param spacecraftId The spacecraftId to set.
     */
    void setSpacecraftId(Integer spacecraftId);

    /**
     * Sets the downlinkStreamId
     *
     * @param downlinkStreamId The downlinkStreamId to set.
     */
    void setDownlinkStreamId(DownlinkStreamType downlinkStreamId);

    /**
     * Sets the mpcsVersion
     *
     * @param mpcsVersion The mpcsVersion to set.
     */
    void setMpcsVersion(String mpcsVersion);

    /**
     * Sets the fswDownlinkHost
     *
     * @param fswDownlinkHost The fswDownlinkHost to set.
     */
    void setFswDownlinkHost(String fswDownlinkHost);

    /**
     * Sets the fswUplinkHost
     *
     * @param fswUplinkHost The fswUplinkHost to set.
     */
    void setFswUplinkHost(String fswUplinkHost);

    /**
     * Sets the fswUplinkPort
     *
     * @param fswUplinkPort The fswUplinkPort to set.
     */
    void setFswUplinkPort(Integer fswUplinkPort);

    /**
     * Sets the fswDownlinkPort
     *
     * @param fswDownlinkPort The fswDownlinkPort to set.
     */
    void setFswDownlinkPort(Integer fswDownlinkPort);

    /**
     * Sets the sseHost
     *
     * @param sseHost The sseHost to set.
     */
    void setSseHost(String sseHost);

    /**
     * Sets the sseUplinkPort
     *
     * @param sseUplinkPort The sseUplinkPort to set.
     */
    void setSseUplinkPort(Integer sseUplinkPort);

    /**
     * Sets the sseDownlinkPort
     *
     * @param sseDownlinkPort The sseDownlinkPort to set.
     */
    void setSseDownlinkPort(Integer sseDownlinkPort);

    /**
     * Set input file
     *
     * @param inputFile Input file
     */
    void setInputFile(String inputFile);

    /**
     * Set topic.
     *
     * @param topic Topic
     */
    void setTopic(String topic);

    /**
     * Set the outputDirectoryOverride state.
     *
     * @param state State to set
     */
    void setOutputDirectoryOverride(Boolean state);

    /**
     * Set the subtopic.
     *
     * @param state Subtopic
     */
    void setSubtopic(String state);

    /**
     * Set the vcid.
     *
     * @param state Vcid
     */
    void setVcid(Long state);

    /**
     * Set the fswDownlinkFlag.
     *
     * @param state FswDownlinkFlag
     */
    void setFswDownlinkFlag(Boolean state);

    /**
     * Set the sseDownlinkFlag.
     *
     * @param state SseDownlinkFlag
     */
    void setSseDownlinkFlag(Boolean state);

    /**
     * Set the uplinkFlag.
     *
     * @param state UplinkFlag
     */
    void setUplinkFlag(Boolean state);

    /**
     * Set the databaseSessionId.
     *
     * @param state DatabaseSessionId
     */
    void setDatabaseSessionId(Long state);

    /**
     * Set the databaseHost.
     *
     * @param state DatabaseHost
     */
    void setDatabaseHost(String state);

//    /**
//     * Sets the HostConfiguration information based on the information 
//     * in this object.
//     * 
//     * @param hc HostConfiguration to set values into
//     * 
//     * @version MPCS-7677 - Moved here from HostConfiguration
//     */
//    void setIntoHostConfiguration(ConnectionConfiguration hc);

    /**
     * Sets database session information into a ContextIdentification object.
     * 
     * @param si SessionIdentification to set values into
     *            
     * @version MPCS-7677 - Moved here from SessionIdentification.
     */
    void setIntoContextIdentification(IContextIdentification si);

    /**
     * Sets the members of the given ContextConfiguration object from this database
     * session object.
     * 
     * @param cc ContextConfiguration to set values into
     *        
     * @version MPCS-7677 - Moved here from SessionConfiguration.
     *        
     */
    void setIntoContextConfiguration(IContextConfiguration cc);

    /**
     * Sets the members of this object into the given dictionary
     * configuration object.
     * 
     * @param dc DictionaryConfiguration to set values into
     *            database session
     * @version MPCS-5079. Added method.
     * @version MPCS-6085. Moved here from DictionaryConfiguration,
     *          which can no longer use database classes.
     * @version MPCS-7677 - Moved here from SessionConfiguration.
     */
    void setIntoDictionaryConfiguration(DictionaryProperties dc);

}
