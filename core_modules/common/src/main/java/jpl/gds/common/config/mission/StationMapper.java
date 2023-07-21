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

import java.util.Collections;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import jpl.gds.shared.config.GdsHierarchicalProperties;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.config.IGdsConfiguration;
import jpl.gds.shared.holders.StationIdHolder;
import jpl.gds.shared.spring.context.flag.SseContextFlag;

/**
 * This class maps station names to numeric AMPCS station ID equivalents 
 * for storage in the database. The Mapping file is a two-column CSV file
 * consisting of one line for each station in the form: Station Name, Numeric ID.
 * The current instance of the StationMapper should be obtained via the 
 * MissionProperties.
 * 
 * Blank lines in the map file will be skipped. Any other abnormalities will result in
 * a failure to load the mapping file. If the load fails, all subsequent
 * calls to getStationId() will return 0 (StationIdHolder.UNSPECIFIED)
 * and calls to getStationString() will return null.
 * 
 * The map file location is controlled by two configuration variables. The
 * first indicates whether to search for a file called StationMap.csv using a
 * standard AMPCS configuration file search. If that property is false, then
 * a second configuration property specified the full path to the station map
 * file. 
 * 
 * This class keep a single global instance that is crated using the global
 * instance of MissionProperties. Unique instances may also be created.
 *
 *
 *
 */
public class StationMapper extends GdsHierarchicalProperties implements Cloneable {
    
    private static final String PROPERTY_FILE = "station_map.properties";
    
    private static final String PROPERTY_PREFIX = "stationMap.";
    private static final String STATION_ID_BLOCK = PROPERTY_PREFIX + "id.";
    
    /**
     * Minimum real station (not unspecified).
     */
    public static final int MINIMUM_REAL_DSSID = 1;

    private final SortedMap<String, Integer> stringToNumMap     = new TreeMap<>();
    private final SortedMap<Integer, String> numToStringMap     = new TreeMap<>();
    
    // This set is cached because it is used in telemetry processing and speed is important
    private SortedSet<Integer> cachedIds = Collections.unmodifiableSortedSet(new TreeSet<Integer>());
    
    /**
     * Private constructor to enforce static nature.
     * 
     * @param mp the MissionProperties object containing mission configuration
     */
    /* package */ StationMapper(final MissionProperties mp, final SseContextFlag sseFlag) {
        super(mp.getStationMapFileName() != null ? mp.getStationMapFileName() : PROPERTY_FILE, false, sseFlag);
        load(mp);
    }
    
    
    /**
     * Loads the station mappings from the mapping file. Will clear and reload the
     * current instance if invoked after construction. 
     * 
     * Package protected to support unit test.
     * 
     * @param MissionProperties the MissionProperties object to use when locating
     *        the station map file.
     * 
     *                            Completely overhauled method. Previously
     *                            station map "files" were separate CSV files where
     *                            each line was a pair consisting of a station number
     *                            and the string station name. However, as part of the
     *                            refactoring of R8, the station map CSV was converted
     *                            into a properties file and subsequently folded into
     *                            the conglomerate properties files. This presented an
     *                            issue; either the station map properties would remain
     *                            a separate property file, which was not desired since
     *                            there should not be any non-conforming property
     *                            files, or rewrite this method. It was decided to do the redesign of
     *                            this function. Currently it still has to follow the
     *                            same system/project/user loading that's required
     *                            by all classes that extend GdsHierarchicalProperties.
     *
     */
    private synchronized void load(final MissionProperties props) {
        
        this.stringToNumMap.clear();
        this.numToStringMap.clear();

        final String filepath = props.getStationMapFilePath();
        
        final String oldUserConfigDir = GdsSystemProperties.getUserConfigDir();
        
        /*
         * Config was/is the flag to say to use the standard loading schema. If it's something else,
         * then use that as the path to the config file.
         * At the moment, however, we can't use just the single path. Use that as the user config
         * directory, load the property file there, and then put it back
         */
        
        if(filepath != null && !filepath.isEmpty() && !filepath.equalsIgnoreCase("config")){
            GdsSystemProperties.setSystemProperty(GdsSystemProperties.USER_DIR_PROPERTY, filepath);
        }
        
        loadProperties();
        
        if(oldUserConfigDir == null){
            GdsSystemProperties.clearProperty(GdsSystemProperties.USER_DIR_PROPERTY);
        }
        else{
            GdsSystemProperties.setSystemProperty(GdsSystemProperties.USER_DIR_PROPERTY, oldUserConfigDir);
        }
        
        /*
         * All of the loaded properties have the "stationMap." prefix, anything else got filtered out already. This is where
         * most of the redesign is done. Now, instead of failing out on a bad line, just warn the user something was wrong
         * and move on. There are potentially some valid property values.
         */
        
        final Set<String> propNames = this.properties.stringPropertyNames();
        
        for (final String propName : propNames) {
            
            
            if (IGdsConfiguration.isDescriptiveProperty(propName)) {
                continue;
            }

            if(!propName.startsWith(STATION_ID_BLOCK)){
                log.debug("Station map property " , propName , " is not named properly and will be discarded");
                continue;
            }
            
            final String cpdName = getProperty(propName);
            if (cpdName == null || cpdName.isEmpty()) {
                log.debug("Missing numeric ID for property " , propName , ". Property will be discarded.");
                continue;
            }
            
            //The ID should be the third portion of the key
            final String[] keyPieces = propName.trim().split("\\.");
            if (keyPieces.length != 3) {
                log.error("Error parsing station ID from station map property " , propName , ". Property will be discarded.");
                continue;
            }
            
            final String id = keyPieces[2].trim();
            
            if (id.isEmpty()) {
                log.error("Missing station name for property " , propName , "=" , cpdName , ". Property will be discarded.");
                continue;
            }
            
            // Convert numeric ID to an int. Stop if this fails.
            int numericId = 0;
            try {
                numericId = Integer.valueOf(id);
            } catch (final NumberFormatException e) {
                log.error("Invalid numeric station ID for property " , propName , "=" , cpdName , ". Property will be discarded.");
                continue;
            }
            
            if ((numericId < MINIMUM_REAL_DSSID) ||
                    (numericId > StationIdHolder.MAX_VALUE)) {
                log.error("Out of range station ID " , numericId , " in station map file for property " , propName , ". Property will be discarded.");                       
            } else {
               // Everything seems fine. Add the mapping to the station map.
                stringToNumMap.put(cpdName, numericId);
                numToStringMap.put(numericId, cpdName);
            }

        }
        
        cachedIds = Collections.unmodifiableSortedSet(new TreeSet<Integer>(this.numToStringMap.keySet()));
    }
    
    /**
     * Gets the numeric station ID associated with the given station name
     * (assumed to have come from DSN CPD).
     * 
     * @param stationName
     *            station name string
     * @return numeric station ID, or SessionConfiguration.UNSPECIFIED_DSSID if
     *         either of 1) map file was not loaded, or 2) no mapping for
     *         the station name is found
     */
	public synchronized int getStationId(final String stationName) {
        final Integer getVal = stringToNumMap.get(stationName);
        if (getVal == null) {
            return StationIdHolder.UNSPECIFIED_VALUE;
        } else {
            return getVal;
        }
    }
    
    /**
     * Gets the station name string associated with the given station number/ID.
     * 
     * @param stationId
     *            numeric station ID
     * @return station name string, or null if
     *         either of 1) map file was not loaded, or 2) no mapping for
     *         the station number is found
     */
	public synchronized String getStationString(final int stationId) {
        return numToStringMap.get(stationId);
    }
    
    /**
     * Get the sorted list of all station name strings.
     * 
     * @return array of names; will be the empty array if none configured or
     *         loaded
     */
    public synchronized String[] getStationStrings() {
        final Set<String> strings = this.stringToNumMap.keySet();
        return strings.toArray(new String[strings.size()]);
    }
    

    /**
     * Get the sorted list of all station IDs.
     * 
     * @return array of station numbers; will be the empty array if none
     *         configured or loaded
     */
    public synchronized Integer[] getStationIds() {
        return cachedIds.toArray(new Integer[cachedIds.size()]);
    }
    

    /**
     * Get the sorted list of all station IDs.
     * 
     * @return array of station numbers; will be the empty array if none
     *         configured or loaded
     */
    public synchronized Set<Integer> getStationIdsAsSet() {
        return cachedIds;
    }
    
    @Override
    public String getPropertyPrefix(){
        return PROPERTY_PREFIX;
    }
}
