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
package jpl.gds.shared.config;

/**
 * Loads and provides information from the shared_gui.properties file to GUI
 * components that need it. The properties file is searched for using the
 * standard AMPCS configuration file search. Uses a singleton pattern so there
 * is only one instance.
 * 
 *
 * @TODO - R8 Refactor TODO - Decide where this config object needs to be in the context
 */
public class SharedGuiProperties extends GdsHierarchicalProperties {
    
    /** The property file */
    private static final String PROPERTY_FILE = "shared_gui.properties";
    
    private static final String PROPERTY_PREFIX = "sharedGui.";
    
    private static final String FONT_BLOCK = PROPERTY_PREFIX + "font.";
    
    // Property Names
    private static final String DEFAULT_VARIABLE_FACE = FONT_BLOCK + "defaultVariableFace";
    private static final String DEFAULT_MONOSPACE_FACE = FONT_BLOCK + "defaultMonospaceFace";
    private static final String MINISCULE_SIZE = FONT_BLOCK + "minisculeSize";
    private static final String MINI_SIZE = FONT_BLOCK + "miniSize";
    private static final String TINY_SIZE = FONT_BLOCK + "tinySize";
    private static final String SMALL_SIZE = FONT_BLOCK + "smallSize";
    private static final String MEDIUM_SIZE = FONT_BLOCK + "mediumSize";
    private static final String LARGE_SIZE = FONT_BLOCK + "largeSize";
    private static final String SUPER_LARGE_SIZE = FONT_BLOCK + "superLargeSize";    
 
    
    /** The one static instance of this class */
    private static SharedGuiProperties instance;
    
    /**
     * Creates and loads the object.  Private to enforce singleton nature.
     */
    private SharedGuiProperties() {
        super(PROPERTY_FILE, true);
    }
    
    /**
     * Get the one instance of this class.
     * 
     * @return SharedGuiProperties
     */
    public static synchronized SharedGuiProperties getInstance() {
        if (instance == null) {
            instance = new SharedGuiProperties();   
        }
        return instance;
    }
    
    /**
     * Resets the instance, such that a new one will be created and loaded
     * the next time getInstance() is called.
     */
    public synchronized static void reset() {
        instance = null;       
    }
    
    /**
     * Gets the name of the default variable font face,  primarily for ChillFont.
     * 
     * @return font name
     */
    public String getDefaultVariableFace() {
    	return getProperty(DEFAULT_VARIABLE_FACE, "Liberation Sans");
    }
    
    /**
     * Gets the name of the default monospace font face,  primarily for ChillFont.
     * 
     * @return font name
     */
    public String getDefaultMonospaceFace() {
        return getProperty(DEFAULT_MONOSPACE_FACE, "Courier");
    }
    
    /**
     * Gets the size of MINISCULE font, as defined by ChillFont.
     * 
     * @return font point size
     */
    public int getMinisculeFontSize() {
        return getFontSizeProperty(MINISCULE_SIZE, 6);
    }
    
    /**
     * Gets the size of MINI font, as defined by ChillFont.
     * 
     * @return font point size
     */
    public int getMiniFontSize() {
        return getFontSizeProperty(MINI_SIZE, 7);
    }
    
    /**
     * Gets the size of TINY font, as defined by ChillFont.
     * 
     * @return font point size
     */
    public int getTinyFontSize() {
        return getFontSizeProperty(TINY_SIZE, 8);
    }
    
    /**
     * Gets the size of SMALL font, as defined by ChillFont.
     * 
     * @return font point size
     */
    public int getSmallFontSize() {
        return getFontSizeProperty(SMALL_SIZE, 10);
    }
      
    /**
     * Gets the size of MEDIUM font, as defined by ChillFont.
     * 
     * @return font point size
     */
    public int getMediumFontSize() {
        return getFontSizeProperty(MEDIUM_SIZE, 12);
    }
    
    /**
     * Gets the size of LARGE font, as defined by ChillFont.
     * 
     * @return font point size
     */
    public int getLargeFontSize() {
        return getFontSizeProperty(LARGE_SIZE, 14);
    }
    
    /**
     * Gets the size of SUPER LARGE font, as defined by ChillFont.
     * 
     * @return font point size
     */
    public int getSuperLargeFontSize() {
        return getFontSizeProperty(SUPER_LARGE_SIZE, 18);
    }
    
    /**
     * General method to parse a font size property.
     * 
     * @param propertyName name of the font property
     * @param defaultVal default value for the point size if not defined or in error
     * @return font point size
     */
    private int getFontSizeProperty(final String propertyName, final int defaultVal) {
        
        int temp = getIntProperty(propertyName, defaultVal);
        if (temp <= 0) {
             log.warn(propertyName + " property is not a positive integer in " + 
                 PROPERTY_FILE + ". Using default value.");
             temp = defaultVal;
        }
        
        return temp;
        
    }
    
    @Override
	  public String getPropertyPrefix() {
        return PROPERTY_PREFIX;
    }

}
