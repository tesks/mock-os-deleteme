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
 * Release is used to access properties in the Release properties file. That
 * file contains values used to label and report application and jar versions.
 *
 */
public final class ReleaseProperties extends GdsHierarchicalProperties {

    private static final String RELEASE_FILE = "release.properties";
    
    private static final String PROPERTY_PREFIX = "release.";
    private static final String CORE_VERSION_PROPERTY = PROPERTY_PREFIX + "internal.coreVersion";
    private static final String MISSION_VERSION_PROPERTY = PROPERTY_PREFIX + "missionVersion";
    private static final String PRODUCT_LINE_PROPERTY = PROPERTY_PREFIX + "internal.productLine";
    
    private static String releaseFile = RELEASE_FILE;
    private static ReleaseProperties instance = null;
    private final String coreVersion;
    private final String projVersion;
    private final String coreVersionShort;
    private final String projVersionShort;
    private final String productLine;
    private static final char AMPCS_VERSION_MARKER = '_';
	private static final String SNAPSHOT_SUFFIX = "-SNAPSHOT";
	private static final int SNAPSHOT_SUFFIX_LENGTH = SNAPSHOT_SUFFIX.length();
	private static final String UNDEFINED = "[undefined]";
	private static final String UNDEFINED_SHORT = "undef";
    private static final String MM_PREVIEW_DELIMITER = "m";

    /**
     * Gets the release version string.
     * @return the version
     */
    public static synchronized String getVersion() {
        if (instance == null) {
            instance = new ReleaseProperties();
        }
        final String mission = GdsSystemProperties.getSystemMission().toUpperCase();
        
        /* Printing short version instead of long version.  Previously, the difference
         * was only if release.properties was not present, [undefined] would print whereas the short version was [undef]. 
         * Now, we are lopping off the AMPCS version that is appended to the adaptation version, so the short version is much more clear.
         */
        // No longer displays project version if undefined
        String versionString = "Multi-Mission Core Version: " + instance.coreVersionShort;
        if(!instance.projVersionShort.equals(UNDEFINED_SHORT)){
        	versionString += ", " + mission + " Adaptation Version: " + instance.projVersionShort;
        }
        return versionString;
    }


    /**
     * Gets the shorter release version string.
     *
     * @return the version
     */
    public static synchronized String getShortVersion()
    {
        if (instance == null)
        {
            instance = new ReleaseProperties();
        }

        //  No longer displays project version if undefined
        String shortVersionString = instance.coreVersionShort;
        if(!instance.projVersionShort.equals(UNDEFINED_SHORT)){
        	shortVersionString += "/" + instance.projVersionShort;
        }
        return shortVersionString;
    }
    
    /**
     * Gets the shorter release version string for the multimission core only.
     *
     * @return the version
     */
    public static synchronized String getShortCoreVersion()
    {
        if (instance == null)
        {
            instance = new ReleaseProperties();
        }

        return instance.coreVersionShort;
    }



    /**
     * Gets the name of the GDS product line.
     * @return the product set name
     */
    public synchronized static String getProductLine() {
        if (instance == null) {
            instance = new ReleaseProperties();
        }
        return instance.productLine;
    }

    /**
     * Takes long form of version abbreviates it. If "+" occurs, truncate the substring beginning at that index.
     * Also truncates "-SNAPSHOT" if it exists
     *
     * @param longVersion - long form of AMPCS version. Must be non-null.
     * @return the shortened version of the AMPCS version
     */
    private String shortenVersion(final String longVersion) {
    	
    	String shortVersion = longVersion;
    	
    	if (longVersion.equals(UNDEFINED)) {
    		return UNDEFINED_SHORT;
    	}
    	
        /**
         * Support a multimission version "preview" delimiter
         * Truncate anythign after this occurs.
         */
        int index = shortVersion.indexOf(MM_PREVIEW_DELIMITER);
        if (index != -1) {
            shortVersion = shortVersion.substring(0, index + 1);
        }

    	/** Support new version number scheme for adaptations
    	 * where the version is suffixed by "+<AMPCS_VERSION_NUMBER>"
    	 */
        index = shortVersion.indexOf(AMPCS_VERSION_MARKER);
    	if (index != -1) {
    		shortVersion = shortVersion.substring(0, index);
    	}
    		
    	if (shortVersion.endsWith(SNAPSHOT_SUFFIX) ) {
    		shortVersion = shortVersion.substring(0, shortVersion.length() - SNAPSHOT_SUFFIX_LENGTH );
    	}
    	return shortVersion;
    }
    
    private ReleaseProperties() {
    	/** use super constructor to simplify parsing and construction. */
        super(releaseFile, true);

        coreVersion      = getProperty(CORE_VERSION_PROPERTY, UNDEFINED);
        /** logic for coreVersionShort is no longer just coreVersion */
        coreVersionShort = shortenVersion(coreVersion);
      
        projVersion      = getProperty(MISSION_VERSION_PROPERTY, UNDEFINED);
       	/** logic for projVersionShort is no longer just projVersion */
        projVersionShort = shortenVersion(projVersion);

        productLine = getProperty(PRODUCT_LINE_PROPERTY, UNDEFINED);
    }

    /**
     * Test.
     *
     * @param args Command-line arguments
     */
    public static void main(final String[] args)
    {
        System.out.println("Version = " + getVersion());
        System.exit(0);
    }
    
    /**
     * Resets loaded content and forces creation of a new static instance
     * the next time a property is required. Also resets the release file
     * to the default.
     * 
     */
    public synchronized static void reset() {

        setReleaseFile(RELEASE_FILE);
        instance = null;
    }
    
    /**
     * Overrides the default name of the release file.
     * 
     * @param path
     *            new file name
     *
     * Due to merge setReleaseFile only changes the Release file name, not the path.
     * File is now restricted to being place in GDS system/project/user configuration directory.
     */
    public synchronized static void setReleaseFile(final String path) {

        releaseFile = path;
    }
    
    @Override
    public String getPropertyPrefix() {
        return PROPERTY_PREFIX;
    }
}
