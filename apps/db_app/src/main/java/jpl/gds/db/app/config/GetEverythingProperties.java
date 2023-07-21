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
package jpl.gds.db.app.config;

import jpl.gds.shared.config.GdsHierarchicalProperties;

/**
 * Loads and provides information from the get_everything.properties file to
 * components that need it. The properties file is searched for using the
 * standard AMPCS configuration file search. Uses a singleton pattern so there
 * is only one instance.
 *
 * @since R8
 */
public class GetEverythingProperties extends GdsHierarchicalProperties {
    
    /** The property file */
    private static final String PROPERTY_FILE = "get_everything.properties";
    
    private static final String PROPERTY_PREFIX = "getEverything.";
    
    private static final String EVERYTHING_EXCEL_BLOCK = PROPERTY_PREFIX + "excel.";
    private static final String EVERYTHING_EXCEL_COLOR_BLOCK = EVERYTHING_EXCEL_BLOCK + "color.";
    
    private static final String BACK_PROPERTY_SUFFIX = ".back";
    private static final String FORE_PROPERTY_SUFFIX = ".fore";
    
    private static final String EVERYTHING_1553_COLOR_PROPERTY = EVERYTHING_EXCEL_COLOR_BLOCK + "1553Log";
    private static final String EVERYTHING_CHANNEL_COLOR_PROPERTY = EVERYTHING_EXCEL_COLOR_BLOCK + "channel";
    private static final String EVERYTHING_COMMAND_COLOR_PROPERTY = EVERYTHING_EXCEL_COLOR_BLOCK + "command";
    private static final String EVERYTHING_EVR_COLOR_PROPERTY = EVERYTHING_EXCEL_COLOR_BLOCK + "evr";
    private static final String EVERYTHING_EVR_USE_COLOR_CODING_PROPERTY = EVERYTHING_EVR_COLOR_PROPERTY + ".useCoding";
    private static final String EVERYTHING_FONT_COLOR_PROPERTY = EVERYTHING_EXCEL_COLOR_BLOCK + "font";
    private static final String EVERYTHING_HEADER_COLOR_PROPERTY = EVERYTHING_EXCEL_COLOR_BLOCK + "header";
    private static final String EVERYTHING_LOG_COLOR_PROPERTY = EVERYTHING_EXCEL_COLOR_BLOCK + "log";
    private static final String EVERYTHING_PRODUCT_COLOR_PROPERTY = EVERYTHING_EXCEL_COLOR_BLOCK + "product";
    
    /**
     * Creates and loads the object from the default property file, which will be searched
     * for using the standard configuration search path.
     */
    public GetEverythingProperties() {
        super(PROPERTY_FILE, true);
    }
    

    @Override
    public String getPropertyPrefix() {
        return PROPERTY_PREFIX;
    }
	
	/**
	 * Get the background and foreground color values (in that order) for 1553
	 * logs in getEverything's excel. The color values are still returned as a
	 * String to allow various GUI elements to handle them appropriately.
	 * 
	 * @return the background and foreground color values for 1553 logs in
	 *         getEverything's excel
	 */
    public String[] getEverything1553LogColors(){
    	return getBackAndForeColors(EVERYTHING_1553_COLOR_PROPERTY);
    }


	/**
	 * Get the background and foreground color values (in that order) for
	 * channels in getEverything's excel. The color values are still returned as
	 * a String to allow various GUI elements to handle them appropriately.
	 * 
	 * @return the background and foreground color values for channels in
	 *         getEverything's excel
	 */
    public String[] getEverythingChannelColors(){
    	return getBackAndForeColors(EVERYTHING_CHANNEL_COLOR_PROPERTY);
    }
    
	/**
	 * Get the background and foreground color values (in that order) for
	 * commands in getEverything's excel. The color values are still returned as
	 * a String to allow various GUI elements to handle them appropriately.
	 * 
	 * @return the background and foreground color values for commands in
	 *         getEverything's excel
	 */
    public String[] getEverythingCommandColors(){
    	return getBackAndForeColors(EVERYTHING_COMMAND_COLOR_PROPERTY);
    }
    
	/**
	 * Get the background and foreground color values (in that order) for
	 * commands in getEverything's excel. The color values are still returned as
	 * a String to allow various GUI elements to handle them appropriately.
	 * 
	 * @return the background and foreground color values for commands in
	 *         getEverything's excel
	 */
    public String[] getEverythingEvrColors(){
    	return getBackAndForeColors(EVERYTHING_EVR_COLOR_PROPERTY);
    }
    
    /**
	 * Get if EVRs will use color coding to separate them by type
	 * 
	 * @return TRUE if color coding is used for EVRs, FALSE if not
	 */
    public boolean isGetEverythingEvrColorCodingUsed(){
    	return this.getBooleanProperty(EVERYTHING_EVR_USE_COLOR_CODING_PROPERTY, false);
    }
    
	/**
	 * Get the default font color value for getEverything's excel. The color
	 * values are still returned as a String to allow various GUI elements to
	 * handle them appropriately.
	 * 
	 * @return the default font color values in getEverything's excel
	 */
    public String getEverythingDefaultFontColor(){
    	return this.getProperty(EVERYTHING_FONT_COLOR_PROPERTY, "255,255,255");
    }
    
	/**
	 * Get the background and foreground color values (in that order) for
	 * headers in getEverything's excel. The color values are still returned as
	 * a String to allow various GUI elements to handle them appropriately.
	 * 
	 * @return the background and foreground color values for headers in
	 *         getEverything's excel
	 */
    public String[] getEverythingHeaderColors(){
    	return getBackAndForeColors(EVERYTHING_HEADER_COLOR_PROPERTY);
    }
    
	/**
	 * Get the background and foreground color values (in that order) for logs
	 * in getEverything's excel. The color values are still returned as a String
	 * to allow various GUI elements to handle them appropriately.
	 * 
	 * @return the background and foreground color values for logs in
	 *         getEverything's excel
	 */
    public String[] getEverythingLogColors(){
    	return getBackAndForeColors(EVERYTHING_LOG_COLOR_PROPERTY);
    }
    
	/**
	 * Get the background and foreground color values (in that order) for
	 * products in getEverything's excel. The color values are still returned as
	 * a String to allow various GUI elements to handle them appropriately.
	 * 
	 * @return the background and foreground color values for logs in
	 *         getEverything's excel
	 */
    public String[] getEverythingProductColors(){
    	return getBackAndForeColors(EVERYTHING_PRODUCT_COLOR_PROPERTY);
    }
    
    /**
     * Gets the background and foreground colors for the specified property
     * @param propertyName the property that has both a background and foreground color
     * @return an array of the two colors
     */
    private String[] getBackAndForeColors(final String propertyName){
    	final String[] retVals = new String[2];
    	
    	retVals[0] = this.getProperty(propertyName + BACK_PROPERTY_SUFFIX, "0,0,0");    	
    	retVals[1] = this.getProperty(propertyName + FORE_PROPERTY_SUFFIX, "255,255,255");

    	return retVals;
    }
}
