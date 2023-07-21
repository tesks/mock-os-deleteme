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
package jpl.gds.evr.api.config;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.shared.config.GdsHierarchicalProperties;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.types.Triplet;

/**
 * A class the manages configuration properties for the EVR projects.
 * 
 *
 * @since R8
 */
public class EvrProperties extends GdsHierarchicalProperties {
    
    /**
     * Name of the default properties file.
     */
    public static final String PROPERTY_FILE = "evr.properties";
    
    private static final String PROPERTY_PREFIX = "evr.";
    
    
    private static final String SSE_BLOCK = PROPERTY_PREFIX + "sse.";
    private static final String SSE_LEVELS_BLOCK = SSE_BLOCK + "levels.";

    private static final String FLIGHT_BLOCK = PROPERTY_PREFIX + "flight.";
    private static final String FLIGHT_LEVELS_BLOCK = FLIGHT_BLOCK + "levels.";
    
    private static final String FSW_COMMAND_LEVEL_PROPERTY = FLIGHT_LEVELS_BLOCK + "command"; 
    private static final String FSW_FATAL_LEVEL_PROPERTY = FLIGHT_LEVELS_BLOCK + "fatal"; 
    private static final String FSW_ALL_LEVELS_PROPERTY = FLIGHT_LEVELS_BLOCK + "all"; 
    
    private static final String SSE_COMMAND_LEVEL_PROPERTY = SSE_LEVELS_BLOCK + "command"; 
    private static final String SSE_FATAL_LEVEL_PROPERTY = SSE_LEVELS_BLOCK + "fatal"; 
    private static final String SSE_ALL_LEVELS_PROPERTY = SSE_LEVELS_BLOCK + "all"; 
    
    private static final String FOREGROUND_BLOCK = PROPERTY_PREFIX + "foregroundColor.";
    private static final String DEFAULT_FOREGROUND_PROPERTY = FOREGROUND_BLOCK + "default";
    
    private static final String BACKGROUND_BLOCK = PROPERTY_PREFIX + "backgroundColor.";
    private static final String DEFAULT_BACKGROUND_PROPERTY = BACKGROUND_BLOCK + "default";
    
    private static final String SAVE_SOURCES_PROPERTY = PROPERTY_PREFIX + "saveSources";
    
    private static final String LIST_DELIM = ",";
    
    private static final String DEFAULT_LEVELS = "COMMAND,FATAL";
    
    private static final String DEFAULT_BG_COLOR =  "0,0,255";
    private static final String DEFAULT_FG_COLOR =  "255,255,255";
    
    private final MissionProperties missionProps;
    
    
    /**
     * Constructor that loads the default properties file, which will be located using the
     * standard configuration file search.
     * 
     * @param missionProps
     *            the current mission properties object
     * @param sseFlag
     *            The SSE context flag
     */
    public EvrProperties(final MissionProperties missionProps, final SseContextFlag sseFlag) {
        super(PROPERTY_FILE, sseFlag);
        this.missionProps = missionProps;
    }
    
    /**
     * Gets the list of supported EVR levels.
     * 
     * @param forSse true if for SSE, false if for flight
     * @return List of EVR level strings; never null or empty
     */
    public List<String> getEvrLevels(final boolean forSse) {
        return getListProperty(forSse ? SSE_ALL_LEVELS_PROPERTY : FSW_ALL_LEVELS_PROPERTY, DEFAULT_LEVELS, LIST_DELIM);
    }
    
    /**
     * Gets the sorted set of supported EVR levels for both flight and SSE.
     * 
     * @return Set of EVR level strings; never null or empty
     */
    public Set<String> getCombinedEvrLevels() {
        final Set<String> result = new TreeSet<String>(getEvrLevels(false));       
        if (missionProps.missionHasSse()) {
            result.addAll(getEvrLevels(true)); 
        }
        return result;
    }
    
    /**
     * Gets the flag indicating that EVR source file name should be saved in EVR message strings.
     * 
     * @return true if source file should be preserved, false if it should be stripped
     */
    public boolean getSaveSources() {
        return getBooleanProperty(SAVE_SOURCES_PROPERTY, false);
    }
    
    /**
     * Gets the "command" EVR level, if any.
     * 
     * @param forSse true if for SSE, false if for flight
     * @return command EVR level; may be null
     */
    public String getCommandEvrLevel(final boolean forSse) {
        return getProperty(forSse ? FSW_COMMAND_LEVEL_PROPERTY : SSE_COMMAND_LEVEL_PROPERTY, null);
    }
    

    /**
     * Gets the "fatal" EVR level, if any.
     * 
     * @param forSse true if for SSE, false if for flight
     * @return fatal EVR level; may be null
     */
    public String getFatalEvrLevel(final boolean forSse) {
        return getProperty(forSse ? FSW_FATAL_LEVEL_PROPERTY : SSE_FATAL_LEVEL_PROPERTY, null);
    }
    
    /**
     * Gets the default background color for all EVR levels as a CSV string.
     * 
     * @return default R,G,B string
     */
    public String getDefaultBackgroundColor() {
        return rgbToString(getRgbProperty(DEFAULT_BACKGROUND_PROPERTY, DEFAULT_BG_COLOR));
        
    }
    
    /**
     * Gets the default background color for all EVR levels as an RGB triplet.
     * 
     * @return R,G,B triplet
     */
    public Triplet<Integer, Integer, Integer> getDefaultBackgroundColorAsRgb() {
        return getRgbProperty(DEFAULT_BACKGROUND_PROPERTY, DEFAULT_BG_COLOR);    
    }
    
    /**
     * Gets the default background color for the specified EVR level as an RGB triplet.
     * 
     * @param level the EVR level to get color for
     * @return R,G,B triplet
     */
    public Triplet<Integer, Integer, Integer> getBackgroundColorAsRgb(final String level) {
        return getRgbProperty(BACKGROUND_BLOCK + level, getDefaultBackgroundColor());
        
    }
    
    /**
     * Gets the default background color for the specified EVR level as a CSV string.
     * 
     * @param level the EVR level to get color for
     * @return R,G,B string
     */
    public String getBackgroundColor(final String level) {
        return rgbToString(getRgbProperty(BACKGROUND_BLOCK + level, getDefaultBackgroundColor()));
        
    }
        
    /**
     * Gets the default foreground color for all EVR levels as a CSV string.
     * 
     * @return default R,G,B string
     */
    public String getDefaultForegroundColor() {
        return rgbToString(getRgbProperty(DEFAULT_FOREGROUND_PROPERTY, DEFAULT_FG_COLOR));
        
    }
    
    /**
     * Gets the default foreground color for all EVR levels as an RGB triplet.
     * 
     * @return default R,G,B triplet
     */
    public Triplet<Integer, Integer, Integer> getDefaultForegroundColorAsRgb() {
        return getRgbProperty(DEFAULT_FOREGROUND_PROPERTY, DEFAULT_FG_COLOR);    
    }
    
    /**
     * Gets the default foreground color for the specified EVR level as an RGB triplet.
     * 
     * @param level the EVR level to get color for
     * @return R,G,B triplet
     */
    public Triplet<Integer, Integer, Integer> getForegroundColorAsRgb(final String level) {
        return getRgbProperty(FOREGROUND_BLOCK + level, getDefaultForegroundColor());
        
    }
    
    /**
     * Gets the default foreground color for the specified EVR level as a CSV string.
     * 
     * @param level the EVR level to get color for
     * @return R,G,B string
     */
    public String getForegroundColor(final String level) {
        return rgbToString(getRgbProperty(FOREGROUND_BLOCK + level, getDefaultForegroundColor()));
        
    }
    
    private Triplet<Integer, Integer, Integer> getRgbProperty(final String propertyName, final String defaultVal) {
        final List<String> val = getListProperty(propertyName, defaultVal, LIST_DELIM);
        if (val == null || val.isEmpty()) {
            return null;
        }
        if (val.size() != 3) {
            log.error("Value for property " , propertyName , " in file " , PROPERTY_FILE , " is not a 3-element RGB string, defaulting to " , defaultVal);
            return stringToRgb(defaultVal);
        }
        
        try {
            final Integer r = GDR.parse_int(val.get(0));
            final Integer g = GDR.parse_int(val.get(1));
            final Integer b = GDR.parse_int(val.get(2));
            if (validateColorSetting(propertyName, r) && validateColorSetting(propertyName, g) && validateColorSetting(propertyName, b)) {
                return new Triplet<Integer, Integer, Integer>(r,g,b);
            } else {
                return stringToRgb(defaultVal);
            }
        } catch (final NumberFormatException e) {
        	log.error("Value for property " , propertyName , " in file " , PROPERTY_FILE , " contains a non-integer RGB value, defaulting to " , defaultVal, ExceptionTools.getMessage(e), e);
            return stringToRgb(defaultVal);
        }
    }
    
    private String rgbToString(final Triplet<Integer, Integer, Integer> rgb) {
        if (rgb == null) {
            return null;
        }
        return rgb.getOne() + LIST_DELIM + rgb.getTwo() + LIST_DELIM + rgb.getThree();       
    }
    
    private Triplet<Integer, Integer, Integer> stringToRgb(final String val) {
        if (val == null) {
            return null;
        }
        final String[] pieces = val.split(LIST_DELIM);
        final Integer r = GDR.parse_int(pieces[0]);
        final Integer g = GDR.parse_int(pieces[1]);
        final Integer b = GDR.parse_int(pieces[2]);
        return new Triplet<Integer, Integer, Integer>(r,g,b);       
    }
    
    private boolean validateColorSetting(final String propertyName, final int color) {
        if (color < 0 || color > 255) {
            log.error("Value for property " , propertyName ," in file " ,PROPERTY_FILE , " contains an RGB value not in range 0 to 255");
            return false;
        }
        return true;
    }
    
    @Override
    public String getPropertyPrefix() {
        return PROPERTY_PREFIX;
    }
}
