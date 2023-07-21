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
package jpl.gds.common.config.mission;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import jpl.gds.common.config.mission.RealtimeRecordedConfiguration.StrategyEnum;
import jpl.gds.common.config.types.DownlinkStreamType;
import jpl.gds.common.config.types.VenueType;
import jpl.gds.common.types.RecordedBool;
import jpl.gds.shared.config.GdsHierarchicalProperties;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.string.StringUtil;
import jpl.gds.shared.types.AsStringComparator;

/**
 * MissionProperties manages the main configuration properties that describe a
 * mission. This includes things like the mission name and ID, spacecraft names,
 * station definitions, virtual channel definitions, etc. By default the
 * configuration is stored in the properties file with the name defined by the
 * PROPERTY_FILE constant. This file is loaded using a standard AMPCS
 * configuration file search, which means that user values override project
 * values, and project values override system values.
 * 
 * 
 */
public class MissionProperties extends GdsHierarchicalProperties {
    /**
     * Name of the default properties file.
     */
    public static final String PROPERTY_FILE = "mission.properties";

    /**
     * Constant that specifies an unknown ID of any type.
     */
    public static final int UNKNOWN_ID = -1;

    /**
     * Constant string which specifies an unknown name of any type.
     */
    public static final String UNKNOWN_NAME = "UNKNOWN";

    /**
     * Value of the searchPath property indicating to search config directories.
     */
    public static final String CONFIG_PATH = "config";

    private static final String LIST_DELIM = ",";
    private static final String ALLOWED = "allowed";
    private static final String DEFAULT = "default";
    private static final int DEFAULT_VC = 0;
    private static final VenueType DEFAULT_VENUE = VenueType.TESTSET;
    
    /**
     * The default testbed name.
     */
    public static final String DEFAULT_TESTBED_NAME = "None";
    
    private static final String DEFAULT_VCID_COLUMN_NAME = "vcidName";

    private static final String PROPERTY_PREFIX = "mission.";
    private static final String SPACECRAFT_BLOCK = PROPERTY_PREFIX
            + "spacecraft.";
    private static final String STATIONS_BLOCK = PROPERTY_PREFIX + "stations.";
    private static final String DL_BLOCK = PROPERTY_PREFIX + "downlink.";
    private static final String UL_BLOCK = PROPERTY_PREFIX + "uplink.";
    
    private static final String DL_VC_BLOCK = DL_BLOCK + "virtualChannels.";

    private static final String MISSION_ID_PROPERTY = PROPERTY_PREFIX + "id";
    private static final String MISSION_LONG_NAME_PROPERTY = PROPERTY_PREFIX
            + "name";
    private static final String MISSION_NEEDS_SSE_PROPERTY = PROPERTY_PREFIX
            + "needsSse";
    private static final String       JPL_SSE_PROPERTY                     = PROPERTY_PREFIX + "sseIsJplStyle";
    private static final String MISSION_DISALLOW_INTEGRATED_PROPERTY = PROPERTY_PREFIX
            + "disallowIntegratedSse";
    
    private static final String ENABLE_SCID_CHECKS_PROPERTY = PROPERTY_PREFIX
            + "enableScidChecks";

    private static final String SOL_VENUES_PROPERTY = PROPERTY_PREFIX + "solVenues";
    
    private static final String STATION_MAP_SEARCH_PROPERTY = STATIONS_BLOCK
            + "mappingFile.searchPath";
    private static final String STATION_MAP_FILENAME_PROPERTY = STATIONS_BLOCK
            + "mappingFile.fileName";

    private static final String SPACECRAFT_IDS_PROPERTY = SPACECRAFT_BLOCK
            + "ids";
    private static final String SPACECRAFT_MNEMONICS_PROPERTY = SPACECRAFT_BLOCK
            + "mnemonics";
    private static final String SPACECRAFT_NAMES_PROPERTY = SPACECRAFT_BLOCK
            + "names";
    private static final String DEFAULT_SPACECRAFT_PROPERTY = SPACECRAFT_IDS_PROPERTY
            + "." + DEFAULT;

    private static final String VCID_MAPPING_ENABLE_PROPERTY = DL_VC_BLOCK
            + "enableQueryMapping";
    private static final String VCID_QUERY_COLUMN_PROPERTY = DL_VC_BLOCK
            + "queryColumn";
    private static final String VCIDS_PROPERTY = DL_VC_BLOCK + "ids";
    private static final String VCID_NAMES_PROPERTY = DL_VC_BLOCK + "names";
    private static final String IDLE_VCIDS_PROPERTY = DL_VC_BLOCK + "ids.idle";
    private static final String PACKET_EXTRACT_VCIDS_PROPERTY = DL_VC_BLOCK + "ids.enablePacketExtraction";
    private static final String       CFDP_PDU_EXTRACT_VCIDS_PROPERTY      = DL_VC_BLOCK
            + "ids.enableCfdpPduExtraction";

    private static final String ENABLE_UPLINK_PROPERTY = UL_BLOCK + "enable";
    private static final String       ENABLE_TC_PACKET_PROPERTY            = UL_BLOCK + "enableTcPackets";
    private static final String UPLINK_RATES_PROPERTY = UL_BLOCK + "allowedBitrates";
    
    private static final String DEFAULT_UPLINK_BITRATE = "ANY";
    
    private static final String ENABLE_EVRS_PROPERTY = PROPERTY_PREFIX + "evrs.enable";
    private static final String ENABLE_EHA_PROPERTY = PROPERTY_PREFIX + "eha.enable";
    private static final String ENABLE_PRODUCTS_PROPERTY = PROPERTY_PREFIX + "products.enable";
    
    private static final String MARKING_PREFIX        = DL_BLOCK + "telemetryMarking.";
    private static final String MARKING_STRATEGY_PROPERTY = MARKING_PREFIX + "strategy";
    private static final String MARKING_UNCONDITIONAL_PROPERTY = MARKING_PREFIX + "unconditionalValue";
    private static final String MARKING_FALLBACK_PROPERTY = MARKING_PREFIX + "fallbackValue";

    private static final String MARKING_VCIDS_PROPERTY  = MARKING_PREFIX + "recordedVcids";

    private static final StrategyEnum DEFAULT_STRATEGY = StrategyEnum.BY_APID;
    private static final RecordedBool DEFAULT_BOOL     = RecordedBool.REALTIME;

    private static final String VENUE_BLOCK = PROPERTY_PREFIX + "venueType.";

    private static final String TESTBED_NAMES_BLOCK = PROPERTY_PREFIX + "testbedNames.";
    private static final String SUBTOPICS_BLOCK = PROPERTY_PREFIX + "subtopics."; 
    private static final String STREAM_IDS_BLOCK = DL_BLOCK + "streamIds.";

    private static final String DEFAULT_VENUE_PROPERTY = VENUE_BLOCK
            + DEFAULT;
    private static final String ALLOWED_VENUES_PROPERTY = VENUE_BLOCK
            + ALLOWED;
    
    private static final String ALLOWED_TESTBED_NAMES_PROPERTY = TESTBED_NAMES_BLOCK + ALLOWED + ".";
    private static final String DEFAULT_TESTBED_NAME_PROPERTY = TESTBED_NAMES_BLOCK + DEFAULT + ".";
    
    private static final String ALLOWED_SUBTOPICS_PROPERTY = SUBTOPICS_BLOCK + ALLOWED;
    private static final String DEFAULT_SUBTOPIC_PROPERTY = SUBTOPICS_BLOCK + DEFAULT;
    
    private static final String ALLOWED_STREAM_IDS_PROPERTY = STREAM_IDS_BLOCK + ALLOWED + ".";   


    /**
     * Override flag for the "mission needs SSE" property.
     */
    private Boolean enableSse;

    private final StationMapper stationMapper;

    private final SseContextFlag      sseFlag;

    /**
     * Test Constructor for a unique instance. Loads the default properties file.
     */
    public MissionProperties() {
        this(new SseContextFlag());
    }

    /**
     * Constructor for a unique instance with SSE Context Flag. Loads the default properties file.
     * 
     * @param sseFlag
     *            The SSE context flag
     */
    public MissionProperties(final SseContextFlag sseFlag) {
        super(PROPERTY_FILE, sseFlag);
        stationMapper = new StationMapper(this, sseFlag);
        this.sseFlag = sseFlag;
    }

    /**
     * @{inheritDoc
     * @see jpl.gds.shared.config.GdsHierarchicalProperties#validate()
     */
    @Override
    protected void validate() {
        super.validate();

        /* Spacecraft property lists should have the same length. */
        final int numSpacecraft = this.getAllScids().size();
        if (getAllSpacecraftNames().size() != numSpacecraft
                || getAllSpacecraftMnemonics().size() != numSpacecraft) {
            log.error("The spacecraft properties in the "
                    + getBaseFilename()
                    + " configuration file have a disagreeing number of entries.");
            log.error("Something seems seriously wrong with " + getBaseFilename());

        }

        /* The default SCID should be one of the defined scids. */
        if (!getAllScids().contains(getDefaultScid())) {
            log.error("The "
                    + DEFAULT_SPACECRAFT_PROPERTY
                    + " in the "
                    + getBaseFilename()
                    + " configuration file disagrees with the list of allowed spacecraft IDs.");
            log.error("Something seems seriously wrong with " + getBaseFilename());
        }

        /* VC property lists should have the same length */
        final int numVcs = this.getAllDownlinkVcids().size();
        if (getAllDownlinkVcNames().size() != numVcs) {
            log.error("The downlink VC properties in the "
                    + getBaseFilename()
                    + " configuration file have a disagreeing number of entries.");
            log.error("Something seems seriously wrong with " + getBaseFilename());

        }
        
        //warn user if vcid query column (named) and numbered names will conflict.
        if(shouldMapQueryOutputVcid() && getVcidColumnName().toUpperCase().equals("VCID")){
        	log.warn("Named VCID column name is invalid - matches numbered VCID column name. This will cause a conflict if both columns are used and not corrected.");
        }

    }

    /**
     * Gets the numeric CCSDS or agency-defined ID for the current mission.
     * 
     * @return mission ID, or UNKNOWN_ID if not defined
     */
    public int getMissionId() {
        return this.getIntProperty(MISSION_ID_PROPERTY, UNKNOWN_ID);
    }

    /**
     * Gets the long descriptive name for the mission.
     * 
     * @return mission long name, or UNKNOWN_NAME if not defined
     */
    public String getMissionLongName() {
        return getProperty(MISSION_LONG_NAME_PROPERTY, UNKNOWN_NAME);
    }
    
    /**
     * Gets the StationMapper instance for the current mission.
     * 
     * @return StationMapper
     */
    public StationMapper getStationMapper() {
    	return this.stationMapper;
    }

    /**
     * Indicates whether the current mission has an SSE configuration. Takes two
     * other factors into account. The first is that it always returns true if
     * sseFlag.isApplicationSse() is true regardless of the
     * configured property value. The second is that the value can be overridden
     * by calling enableMissionSse(). If that has been invoked, then the value
     * set by that call is always returned, overriding both the application flag
     * and the configuration property.
     * 
     * @return true if there is an SSE in the current configuration, false if
     *         not
     */
    public boolean missionHasSse() {
        if (enableSse != null) {
            return enableSse;
        }
        return sseFlag.isApplicationSse()
                || getBooleanProperty(MISSION_NEEDS_SSE_PROPERTY, false);
    }

    /**
     * Indicates whether the SSE software in use is the JPL/FSW Core SSE.
     * 
     * @return true if JPL SSE, false if not
     */
    public boolean sseIsJplStyle() {
        return getBooleanProperty(JPL_SSE_PROPERTY, false);
    }

    /**
     * Overrides the return value of missionHasSse() to the specified boolean
     * value. SHOULD BE USED ONLY BY UNIT TESTS.
     * 
     * @param enable
     *            true to enable SSE in the current configuration, false to
     *            disable.
     */
    public void enableMissionSse(final boolean enable) {
        enableSse = Boolean.valueOf(enable);
    }

    /**
     * Gets the property indicating whether to disallow integrated flight and
     * SSE configuration, even though the mission configuration supports SSE.
     * Takes two other factors into account. The first is that it always returns
     * true if sseFlag.isApplicationSse() is true regardless of the
     * configured property value. The second is that it will only return true if
     * the MISSION_NEEDS_SSE property is also true.
     * 
     * @return true to disallow integrated configuration, false to leave it
     *         enabled
     */
    public boolean disallowIntegratedSse() {
        return (!sseFlag.isApplicationSse()
                && getBooleanProperty(MISSION_NEEDS_SSE_PROPERTY, false) && getBooleanProperty(
                        MISSION_DISALLOW_INTEGRATED_PROPERTY, false));
    }

    /**
     * Gets the list of all configured spacecraft IDs. If none are found,
     * UNKNOWN_ID is returned as the only list entry.
     * 
     * @return List of Integer scids, never null or empty.
     */
    public List<Integer> getAllScids() {
        final List<String> listAsStrings = this
                .getListProperty(SPACECRAFT_IDS_PROPERTY,
                        String.valueOf(UNKNOWN_ID), LIST_DELIM);
        return toIntegerList(listAsStrings, SPACECRAFT_IDS_PROPERTY,
                Integer.valueOf(UNKNOWN_ID));
    }

    /**
     * Gets the list of all configured spacecraft mnemonics. If none are found,
     * UNKNOWN_NAME is returned as the only list entry.
     * 
     * @return List of Strings, never null or empty.
     */
    public List<String> getAllSpacecraftMnemonics() {
        return getListProperty(SPACECRAFT_MNEMONICS_PROPERTY, UNKNOWN_NAME,
                LIST_DELIM);
    }

    /**
     * Gets the list of all configured spacecraft long names. If none are found,
     * UNKNOWN_NAME is returned as the only list entry.
     * 
     * @return List of Strings, never null or empty.
     */
    public List<String> getAllSpacecraftNames() {
        return getListProperty(SPACECRAFT_NAMES_PROPERTY, UNKNOWN_NAME,
                LIST_DELIM);
    }

    /**
     * Gets the configured default spacecraft ID.
     * 
     * @return scid value, or UNKNOWN_ID if not configured.
     */
    public int getDefaultScid() {
        return this.getIntProperty(DEFAULT_SPACECRAFT_PROPERTY, UNKNOWN_ID);
    }

    /**
     * Indicates whether the supplied spacecraft ID is among the configured
     * spacecraft IDs.
     * 
     * @param scid
     *            the spacecraft ID to check
     * 
     * @return true if the supplied scid is valid, false if not
     */
    public boolean isScidValid(final int scid) {
        return getAllScids().contains(scid);
    }

    /**
     * Maps the supplied spacecraft ID to a spacecraft mnemonic.
     * 
     * @param scid
     *            the spacecraft ID to map
     * @return the mnemonic it maps to, or UNKNOWN_NAME if no match can be made
     */
    public String mapScidToMnemonic(final int scid) {
        final List<Integer> allScids = getAllScids();
        final int index = allScids.indexOf(scid);
        if (index == -1) {
            return UNKNOWN_NAME;
        }
        final List<String> allMnemonics = getAllSpacecraftMnemonics();
        if (allMnemonics.size() - 1 < index) {
            return UNKNOWN_NAME;
        }
        return allMnemonics.get(index);

    }

    /**
     * Maps the supplied spacecraft mnemonic to a spacecraft ID.
     * 
     * @param mnemonic
     *            the spacecraft mnemonic to map
     * @return the spacecraft ID it maps to, or UNKNOWN_ID if no match can be
     *         made
     */
    public int mapMnemonicToScid(final String mnemonic) {
        final List<String> allMnemonics = getAllSpacecraftMnemonics();
        final int index = allMnemonics.indexOf(mnemonic);
        if (index == -1) {
            return UNKNOWN_ID;
        }
        final List<Integer> allScids = getAllScids();
        if (allScids.size() - 1 < index) {
            return UNKNOWN_ID;
        }
        return allScids.get(index);
    }

    /**
     * Maps the supplied spacecraft ID to a spacecraft long name.
     * 
     * @param scid
     *            the spacecraft ID to map
     * @return the name it maps to, or UNKNOWN_NAME if no match can be made
     */
    public String mapScidToName(final int scid) {
        final List<Integer> allScids = getAllScids();
        final int index = allScids.indexOf(scid);
        if (index == -1) {
            return UNKNOWN_NAME;
        }
        final List<String> allNames = getAllSpacecraftNames();
        if (allNames.size() - 1 < index) {
            return UNKNOWN_NAME;
        }
        return allNames.get(index);

    }

    /**
     * Gets the path, excluding the file name, to the station map file.
     * 
     * @return station map file path, or null if none found
     */
    public String getStationMapFilePath() {
        return getProperty(STATION_MAP_SEARCH_PROPERTY);
    }
    
    /**
     * Gets the system level file name for the station map file.
     * 
     * @return station map file name, or null if none found
     */
    public String getStationMapFileName(){
        return getProperty(STATION_MAP_FILENAME_PROPERTY);
    }

    /**
     * Gets the property indicating whether the VCID column in query output
     * should be mapped to another column and the VCID converted to VC Name in
     * the query records.
     * 
     * @return true to enable VCID column mapping, false to disable
     */
    public boolean shouldMapQueryOutputVcid() {
        return getBooleanProperty(VCID_MAPPING_ENABLE_PROPERTY, false);
    }

    /**
     * Gets the property indicating the column name to which the VCID column
     * should be mapped if query output VCID mapping is enabled.
     * 
     * @return VCID column name
     */
    public String getVcidColumnName() {
        return getProperty(VCID_QUERY_COLUMN_PROPERTY, DEFAULT_VCID_COLUMN_NAME);
    }

    /**
     * Gets the list of configured downlink VCIDs. If none are found, 
     * the DEFAULT_VC is returned as the only list entry.
     * 
     * @return List of Integer vcids, never null or empty
     */
    public List<Integer> getAllDownlinkVcids() {
        final List<String> listAsStrings = this.getListProperty(VCIDS_PROPERTY,
                String.valueOf(DEFAULT_VC), LIST_DELIM);
        return toIntegerList(listAsStrings, VCIDS_PROPERTY,
                Integer.valueOf(DEFAULT_VC));
    }

    /**
     * Gets the list of configured downlink VC names. If none are found,
     * UNKNOWN_NAME is returned as the only list entry.
     * 
     * @return List of Strings, never null or empty
     */
    public List<String> getAllDownlinkVcNames() {
        return getListProperty(VCID_NAMES_PROPERTY, UNKNOWN_NAME, LIST_DELIM);
    }

    /**
     * Maps the supplied downlink VCID to its configured name.
     * 
     * @param vcid
     *            VCID to map
     * @return VC name, or UNKNOWN_NAME if no match can be made
     */
    public String mapDownlinkVcidToName(final int vcid) {
        final List<Integer> allVcids = getAllDownlinkVcids();
        final int index = allVcids.indexOf(vcid);
        if (index == -1) {
            return UNKNOWN_NAME;
        }
        final List<String> allNames = getAllDownlinkVcNames();
        if (allNames.size() - 1 < index) {
            return UNKNOWN_NAME;
        }
        return allNames.get(index);
    }

    /**
     * Maps the supplied VC name to a downlink VCID.
     * 
     * @param name
     *            the VC name to map
     * @return VCID, or UNKNOWN_ID if no match could be made
     */
    public int mapNameToDownlinkVcid(final String name) {
        final List<String> allNames = getAllDownlinkVcNames();
        final int index = allNames.indexOf(name);
        if (index == -1) {
            return UNKNOWN_ID;
        }
        final List<Integer> allVcids = getAllDownlinkVcids();
        if (allVcids.size() - 1 < index) {
            return UNKNOWN_ID;
        }
        return allVcids.get(index);

    }

    /**
     * Gets the list of downlink VCIDs that identify idle transfer frames.
     * 
     * @return List of Integer; may be empty if there are no idle VCIDs,
     *         but never null
     */
    public List<Integer> getIdleVcids() {
        final List<String> tempList = getListProperty(IDLE_VCIDS_PROPERTY, null,
                LIST_DELIM);
        return toIntegerList(tempList, IDLE_VCIDS_PROPERTY, null);

    }
    
    /**
     * Gets the list of downlink VCIDs for which packet extraction is required.
     * 
     * @return list of VCIDs, may be empty but never null
     */
    public List<Integer> getPacketExtractVcids() {
        final List<String> tempList = getListProperty(PACKET_EXTRACT_VCIDS_PROPERTY, null,
                LIST_DELIM);
        return toIntegerList(tempList, PACKET_EXTRACT_VCIDS_PROPERTY, null);

    }
    
    /**
     * Gets the list of ownlink VCIDs for which CFDP PDU extraction is required.
     * 
     * @return list of VCIDs, may be empty but never null
     */
    public List<Integer> getCfdpPduExtractVcids() {
        final List<String> tempList = getListProperty(CFDP_PDU_EXTRACT_VCIDS_PROPERTY, null, LIST_DELIM);
        return toIntegerList(tempList, CFDP_PDU_EXTRACT_VCIDS_PROPERTY, null);
    }

    /**
     * Indicates whether uplink/command capability is enabled for the current
     * mission.
     * 
     * @return true if uplink enabled, false if not
     * 
     */
    public boolean isUplinkEnabled() {
        return getBooleanProperty(ENABLE_UPLINK_PROPERTY, true);
    }
    
    /**
     * Indicates whether uplink capabilities should include a Telecommand Packet
     * 
     * @return true if TC packets enabled, false if not
     */
    public boolean isUsingTcPackets() {
        return getBooleanProperty(ENABLE_TC_PACKET_PROPERTY, false);
    }

    /**
     * Indicates whether EVR capability is enabled for the current
     * mission.
     * 
     * @return true if EVR handling enabled, false if not
     * 
     */
    public boolean areEvrsEnabled() {
        return getBooleanProperty(ENABLE_EVRS_PROPERTY, true);
    }
    
    /**
     * Indicates whether EHA capability is enabled for the current
     * mission.
     * 
     * @return true if EHA handling enabled, false if not
     * 
     */
    public boolean isEhaEnabled() {
        return getBooleanProperty(ENABLE_EHA_PROPERTY, true);
    }
    
    /**
     * Indicates whether data product capability is enabled for the current
     * mission.
     * 
     * @return true if data product handling enabled, false if not
     * 
     */
    public boolean areProductsEnabled() {
        return getBooleanProperty(ENABLE_PRODUCTS_PROPERTY, true);
    }
    
    /**
     * Indicates whether the system should generate and display SOL (LST) times
     * in the specified venue.
     * 
     * @param vt
     *            VenueType to check
     * @return true if SOL/LST times should be used, false if not
     */
    public boolean getVenueUsesSol(final VenueType vt) {
        final List<String> solVenues = getListProperty(SOL_VENUES_PROPERTY, null,
                LIST_DELIM);
        return solVenues.contains(vt.toString());
    }
   
    
	/**
	 * Returns the list of allowed uplink bitrates.
	 * 
	 * @return list of allowed rates (as Strings); will be empty if uplink not
	 *         enabled for the current mission
	 */
    public List<String> getAllowedUplinkBitrates() {
        if (!isUplinkEnabled()) {
            return new LinkedList<String>();
        }
        return getListProperty(UPLINK_RATES_PROPERTY, DEFAULT_UPLINK_BITRATE, LIST_DELIM);
    }
    
    /**
     * Gets the configured telemetry marking strategy (for realtime/recorded marking)
     * 
     * @return StrategyEnum value
     */
    public StrategyEnum getTelemetryMarkingStrategy() {
        final String value =
                StringUtil.safeTrimAndUppercase(getProperty(MARKING_STRATEGY_PROPERTY, DEFAULT_STRATEGY.toString()));

            try {
                return Enum.valueOf(StrategyEnum.class, value);
            }
            catch (final IllegalArgumentException iae)
            {
                log.error(MARKING_STRATEGY_PROPERTY +
                             " is set to an invalid value (" +
                             value +
                             ") in " + PROPERTY_FILE + ". Setting it to " + DEFAULT_STRATEGY);
            }

            return DEFAULT_STRATEGY;
    }
    
    /**
     * Gets the configured telemetry marking fallback strategy (for realtime/recorded marking)
     * 
     * @return RecordedBool - true if the fallback is recorded, false if realtime
     */
    public RecordedBool getTelemetryMarkingFallback() {
        return getRecordedBool(MARKING_FALLBACK_PROPERTY, DEFAULT_BOOL);
    }
    
    /**
     * Gets the configured telemetry marking unconditional strategy (for realtime/recorded marking)
     * 
     * @return RecordedBool - true if the unconditional is recorded, false if realtime
     */
    public RecordedBool getUnconditionalTelemetryMarking() {
        return getRecordedBool(MARKING_UNCONDITIONAL_PROPERTY, DEFAULT_BOOL);
    }
    
    /**
     * Gets the list of downlink VCIDs that signal recorded data content. Valid only
     * if the marking strategy if BY_VCID.
     * 
     * @return Set of VCIDs as Longs, may be empty but never null
     */
    public Set<Long> getRecordedVcids() {
        final List<String> configuredVcids = getListProperty(MARKING_VCIDS_PROPERTY, null, LIST_DELIM);
        
        
        final List<Integer> known = getAllDownlinkVcids();
        final Set<Long> result = new TreeSet<Long>();

        for (final String s : configuredVcids)
        {
            Integer vcid = null;
            // NumberFormatException from Integer.valueOf().
            try
            {
                vcid = Integer.valueOf(s);
                if (known.contains(vcid))
                {
                    result.add(Long.valueOf(vcid));
                }
                else
                {
                    log.error(MARKING_VCIDS_PROPERTY +
                            " contains recorded VCID " +
                            vcid +
                            " not in configured allowed VCIDs in " + PROPERTY_FILE + "; skipped");
                }
            } catch (final NumberFormatException nfe) {
                log.error(MARKING_VCIDS_PROPERTY +
                        " in " + PROPERTY_FILE + " contains a non-integer value: " + vcid + "; skipped");

            }
        }
        
        return result;
    }
    
    /**
     * Get a RecordedBool from the configuration.
     *
     * @param property Property string
     *
     * @return Recorded choice
     */
    private RecordedBool getRecordedBool(final String property, final RecordedBool defValue)
    {
        final String value =
            StringUtil.safeTrim(getProperty(property, defValue.toString()));

        return (! value.isEmpty() ? RecordedBool.valueOf(value) : null);
    }

    /**
     * Convenience method to convert list of strings to list of integers. Will
     * discard any list entry that is not an integer. If the resulting list is
     * empty, the defaultElement, if non-null, will be added to the list as its
     * one element.
     * 
     * @param toConvert
     *            List of String to convert
     * @param propertyName
     *            property name (for log messages)
     * @param defaultElement
     *            element to add if the list is empty; may be null
     * @return List of Integer
     */
    private List<Integer> toIntegerList(final List<String> toConvert,
            final String propertyName, final Integer defaultElement) {
        final List<Integer> result = new ArrayList<Integer>(toConvert.size());
        for (final String s : toConvert) {
            try {
                result.add(Integer.parseInt(s));
            } catch (final NumberFormatException e) {
                log.warn("Invalid integer value " + s
                        + " found in configuration file " + getBaseFilename()
                        + " for property " + propertyName);
                log.warn("This value will be skipped");
            }
        }
        if (result.isEmpty() && defaultElement != null) {
            result.add(defaultElement);
        }
        return result;
    }
    
    /**
     * Gets the default VenueType for the current configuration.
     * 
     * @return VenueType, never null
     */
    public VenueType getDefaultVenueType() {
        final String vtStr = getProperty(DEFAULT_VENUE_PROPERTY,
                DEFAULT_VENUE.toString());
        try {
            final VenueType vr = VenueType.valueOf(vtStr);
            return vr;
        } catch (final IllegalArgumentException e) {
            reportError(DEFAULT_VENUE_PROPERTY, vtStr, DEFAULT_VENUE.toString());
            return DEFAULT_VENUE;
        }

    }

    /**
     * Gets a sorted set of the allowed venue types in the current
     * configuration.
     * 
     * @return Set of VenueType, never null or empty
     */
    public Set<VenueType> getAllowedVenueTypes() {
        final List<String> temp = getListProperty(ALLOWED_VENUES_PROPERTY,
                DEFAULT_VENUE.toString(), LIST_DELIM);
        final SortedSet<VenueType> result = new TreeSet<VenueType>(
                new AsStringComparator<VenueType>());
        for (final String vtStr : temp) {
            try {
                final VenueType vt = VenueType.valueOf(vtStr);
                result.add(vt);
            } catch (final IllegalArgumentException e) {
                reportError(DEFAULT_VENUE_PROPERTY, vtStr, null);
                log.error("Value will be omitted from the configured list");
            }

        }
        if (result.isEmpty()) {
            result.add(DEFAULT_VENUE);
        }

        return result;
    }

    /**
     * Gets a sorted set of the allowed venue types in the current
     * configuration, as Strings.
     * 
     * @return Set of String, never null or empty
     */
    public Set<String> getAllowedVenueTypesAsStrings() {
        final Set<VenueType> temp = getAllowedVenueTypes();
        return toSortedStringSet(temp);
    }

  
    /**
     * Gets the list of allowed testbed names for the specified venue. If the
     * list is empty, the DEFAULT_TESTBED_NAME is added to the list.
     * 
     * @param vt
     *            VenueType.TESTBED or VenueType.ATLO; any other value will
     *            result in a null return value.
     * @return List of String testbed names, never empty or null unless the
     *         wrong venue type is specified, in which case null.
     * 
     */
    public List<String> getAllowedTestbedNames(final VenueType vt) {
        if (!vt.hasTestbedName()) {
            return null;
        }
        return getListProperty(ALLOWED_TESTBED_NAMES_PROPERTY + vt.toString(),
                DEFAULT_TESTBED_NAME, LIST_DELIM);
    }

    /**
     * Gets the default testbed name for the specified venue. If the hostname is
     * null, the return value will be the configured value. If the hostname is
     * not null, but contains any allowed testbed name, then the value returned
     * will be the testbed name that matched the host name. The comparison is
     * not sensitive to case.
     * 
     * @param vt
     *            VenueType.TESTBED or VenueType.ATLO; any other value will
     *            result in a null return value.
     * @param currentHost
     *            the name of the current host, if the default testbed should be
     *            determined based upon host name, or null if hostname should
     *            not be taken into account
     * @return default testbed name; may be null
     * 
     */
    public String getDefaultTestbedName(final VenueType vt, final String currentHost) {
        if (!vt.hasTestbedName()) {
            return null;
        }
        final String defval = getProperty(DEFAULT_TESTBED_NAME_PROPERTY
                + vt.toString());
        if (currentHost == null) {
            return defval;
        }
        final List<String> allVenueNames = getAllowedTestbedNames(vt);

        final String upperHost = currentHost.toUpperCase();
        for (final String vn : allVenueNames) {
            final String upperVn = vn.toUpperCase();
            if (upperHost.indexOf(upperVn) != -1) {
                return vn;
            }
        }
        return defval;
    }

 
    
    /**
     * Gets the list of allowed messaging subtopics.
     * 
     * @return List of String subtopics; may be null
     * 
     */
    public List<String> getAllowedSubtopics() {
        return getListProperty(ALLOWED_SUBTOPICS_PROPERTY, null, LIST_DELIM);
    }

    /**
     * Gets the default messaging subtopic.
     * 
     * @return default subtopic; may be null
     * 
     */
    public String getDefaultSubtopic() {
        return getProperty(DEFAULT_SUBTOPIC_PROPERTY);
    }

  

    /**
     * Tells whether this venue is a supported venue for the current mission.
     * 
     * @param vt the VenueType
     * 
     * @return true if this venue is a supported venue for the current mission
     */
    public boolean isVenueForMission(final VenueType vt) {
        if (vt == null) {
            return false;
        }
        return getAllowedVenueTypes().contains(vt);

    }
    
    /**
     * Gets the list of allowed downlink stream IDs in the specified venue. If
     * the list is empty, the NOT_APPLICABLE stream will be added.
     * <p>
     * Unlike the other methods that get the allowed list of things, this
     * returns List rather than a Sorted Set. This is because we seem to 
     * want the NOT_APPLICABLE value, which is sometimes added to the list
     * by the caller, to always be last.
     * 
     * 
     * @param vt
     *            VenueType.TESTBED or VenueType.ATLO; any other value will
     *            result in a null return value.
     * @return List of DownlinkStreamType, may be null if the wrong venue type
     *         is specified. Otherwise never empty or null
     * 
     */
    public List<DownlinkStreamType> getAllowedDownlinkStreamIds(final VenueType vt) {
        if (!vt.hasStreams()) {
            return null;
        }
        final List<String> idList = getListProperty(
                ALLOWED_STREAM_IDS_PROPERTY + vt.toString(),
                DownlinkStreamType.NOT_APPLICABLE.toString(), LIST_DELIM);
        return convertStreamTypeList(idList,
                ALLOWED_STREAM_IDS_PROPERTY + vt.toString());
    }

    /**
     * Gets the allowed downlink stream IDs in the specified venue as Strings.
     * <p>
     * Unlike the other methods that get the allowed list of things, this
     * returns List rather than a Sorted Set. This is because we seem to 
     * want the NOT_APPLICABLE value, which is sometimes added to the list
     * by the caller, to always be last.
     * 
     * 
     * @param vt
     *            VenueType.TESTBED or VenueType.ATLO; any other value will
     *            result in a null return value.
     * @return List of Strings, may be null if the wrong venue type is
     *         specified. Otherwise never null or empty
     * 
     */
    public List<String> getAllowedDownlinkStreamIdsAsStrings(final VenueType vt) {
        if (!vt.hasStreams()) {
            return null;
        }
        final List<DownlinkStreamType> idList = getAllowedDownlinkStreamIds(vt);
        return toStringList(idList);
    }

 
    /**
     * Gets the flag indicating if SCID checks of command and telemetry structures is enabled.
     * @return true if scid checking enabled, false if not
     */
    public boolean getScidChecksEnabled() {
        return getBooleanProperty(ENABLE_SCID_CHECKS_PROPERTY, false);
    }
    
    /**
     * Converts list of strings to a list of downlink stream types.
     * 
     * @param toConvert
     *            list of Strings to convert
     * @param propertyName
     *            property name the conversion is for
     * @return List of DownlinkStreamType
     * 
     */
    private List<DownlinkStreamType> convertStreamTypeList(
            final List<String> toConvert, final String propertyName) {
        final List<DownlinkStreamType> result = new LinkedList<DownlinkStreamType>();
        for (final String vtStr : toConvert) {
            try {
                final DownlinkStreamType dst = DownlinkStreamType.convert(vtStr
                        .trim().toUpperCase());
                if (!result.contains(dst)) {
                    result.add(dst);
                }
            } catch (final IllegalArgumentException e) {
                reportError(propertyName, vtStr, null);
                log.error("Value will be omitted from the configured list");
            }
        }
        return result;

    }
    
    @Override
    public String getPropertyPrefix() {
        return PROPERTY_PREFIX;
    }
}
