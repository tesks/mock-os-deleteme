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

import java.util.Properties;

/**
 * An interface to be implemented by configuration classes that read various
 * formats of config files.  Also contains static methods related to configuration.
 * 
 * @since R8
 */
public interface IGdsConfiguration {
    
    
    /** Last component of a description property */
    public static final String DESCRIPTION_SUFFIX = "description";
    /** Last component of a valid values property */
    public static final String VALID_SUFFIX = "validValues";
    /** Last component of a behavioral notes property */
    public static final String BEHAVIOR_SUFFIX = "behavioralNotes";
    /** Last component of a format hint property */
    public static final String HINT_SUFFIX = "formatHint";
    /** Last component of an example property */
    public static final String EXAMPLE_SUFFIX = "example";
    /** Last component of a block description property */
    public static final String BLOCK_DESCRIPTION_SUFFIX = "blockDescription";
    /** Last component of a category description property */
    public static final String CATEGORY_DESCRIPTION_SUFFIX = "categoryDescription";
    /** Last component of a default value property (used when we have multiple entries sharing the same property
      name, like autoProxy.cfdp.entity.[ID].scid.defaultValue=0) */
    public static final String DEFAULT_SUFFIX = "defaultValue";
    /** Matches all but the last element of any property name */
    public static final String ANY_PREFIX = ".*\\.";
    
    /** Regex to match description properties */
    public static final String DESCRIPTION_PROPERTY_REGEX = ANY_PREFIX + DESCRIPTION_SUFFIX;
    /** Regex to match valid values properties */
    public static final String VALID_VALS_PROPERTY_REGEX = ANY_PREFIX + VALID_SUFFIX;
    /** Regex to match default value properties */
    public static final String DEFAULT_VAL_PROPERTY_REGEX = ANY_PREFIX + DEFAULT_SUFFIX;
    /** Regex to match behavioral notes properties */
    public static final String BEHAVIOR_PROPERTY_REGEX = ANY_PREFIX + BEHAVIOR_SUFFIX;
    /** Regex to match format hint properties */
    public static final String FORMAT_HINT_PROPERTY_REGEX = ANY_PREFIX + HINT_SUFFIX;
    /** Regex to match example properties */
    public static final String EXAMPLE_PROPERTY_REGEX = ANY_PREFIX + EXAMPLE_SUFFIX;
    /** Regex to match block description properties */
    public static final String BLOCK_DESC_PROPERTY_REGEX = ANY_PREFIX + BLOCK_DESCRIPTION_SUFFIX;
    /** Regex to match block description properties */
    public static final String CATEGORY_DESC_PROPERTY_REGEX = ANY_PREFIX + CATEGORY_DESCRIPTION_SUFFIX;

    /** Regex to match any descriptive property */
    public static final String ANY_DESCRIPTIVE_REGEX = DESCRIPTION_PROPERTY_REGEX + "|" +
            VALID_VALS_PROPERTY_REGEX + "|" +
            DEFAULT_VAL_PROPERTY_REGEX + "|" +
            BEHAVIOR_PROPERTY_REGEX + "|" +
            FORMAT_HINT_PROPERTY_REGEX + "|" +
            EXAMPLE_PROPERTY_REGEX + "|" +
            BLOCK_DESC_PROPERTY_REGEX + "|" +
            CATEGORY_DESC_PROPERTY_REGEX;
    
    /** Regex for an internal property */
    public static final String INTERNAL_REGEX = "[^\\.]+\\.internal\\..*";
    
    /**
     * Determines if the given property key is for a descriptive property.
     * 
     * @param key the property key to check
     * @return true if the property name matches a descriptive property
     * pattern, false if not
     */
    public static boolean isDescriptiveProperty(final String key) {
        return key.matches(ANY_DESCRIPTIVE_REGEX);
    }
    
    /**
     * Determines if the given property key is for an internal property.
     * 
     * @param key the property key to check
     * @return true if the property name matches an internal property
     * pattern, false if not
     */
    public static boolean isInternalProperty(final String key) {
        return key.matches(INTERNAL_REGEX);
    }

    /**
     * Returns the configuration properties as a Properties object.  Will return
     * null if supportsFlatProperties() returns false.
     * 
     * @return properties object, may be null
     */
    public Properties asProperties();
    
    /**
     * Gets the unique property name prefix for this configuration object. This value
     * is expected to end with a dot (.).
     * 
     * @return prefix
     */
    public String getPropertyPrefix();
    
    /**
     * Indicates is this object can return a Properties object.
     * 
     * @return true or false
     */
    public boolean supportsFlatProperties();
    
    /**
     * Gets the base configuration file name (no path).
     * @return base file name
     */
    public String getBaseFilename();
}
