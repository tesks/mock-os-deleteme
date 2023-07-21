/*
 * Copyright 2006-2019. California Institute of Technology.
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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.cli.ParseException;

import jpl.gds.shared.cli.CliUtility;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.sys.SystemUtilities;

/**
 * 
 * This class is a utility class for loading Java properties files using the
 * AMPCS hierarchical search: System configuration directory, then project
 * configuration directory, then user configuration directory. The properties in
 * each will override any properties with the same name that were defined by a
 * previous file.
 * 
 *
 * 
 */
public class GdsHierarchicalProperties implements IGdsConfiguration {
    
    /**
     * PropertySet enum is utilized to determine which portion of the properties
     * will be stored in this GdsHierarchicalProperties object. A property is a
     * value that is actually utilized by AMPCS to configure something. A
     * descriptive property is not utilized to configure anything, but gives
     * additional context to the user regarding how the property it is
     * associated with is utilized. This includes "description",
     * "behavioralNotes", "validValues", "formatHint", and "example" properties.
     * 
     *
     */
	public enum PropertySet {
	    /** Indicates to only load properties, no descriptive properties */
		NO_DESCRIPTIVES,
		/** Indicates that all properties are to be loaded */
        INCLUDE_DESCRIPTIVES,
        /** Indicates to only load descriptive properties, no usable properties.
         * This should only utilized by certain tools that will be consolidating
         * and outputting the descriptions, not for normal AMPCS use.
         */
		DESCRIPTIVES_ONLY;
	} 
    
    /** Tracer for use during file loading 
     */
    protected static Tracer     log                         = TraceManager.getTracer(Loggers.CONFIG);
	
	/** Properties object that will hold all loaded properties. */
	protected final Properties properties = new Properties();
	
	/** Static map of loaded override properties.*/
	protected static final Map<String, Properties> cachedProperties = new ConcurrentHashMap<>();
	
	/** Static flag controlling load of the master property override file.  */
	protected static AtomicBoolean overridesLoaded = new AtomicBoolean(false);

	/** The base name (no path) of the properties file. */
	private final String propertyFileName;
	
	/*
     * Adding an override properties object, and two constants defining the
     * override file's name and the user config override property.
     */
    /** Properties from override file. Made static */
    protected static final Properties propertyOverrides = new Properties();
    
    /** Flag indicating if a validation error occurred when loading properties */
	protected boolean errorLogged;
	
	/** The subset of properties to be loaded. See the PropertySet enum for additional information */
    private static PropertySet propertiesToLoad = PropertySet.NO_DESCRIPTIVES;
    
    private List<String> whitelistedDirectories;
	
	/** Filename to be utilized by all property files that are not at the system level. */
	public static final String CONSOLIDATED_PROPERTY_FILE_NAME = "ampcs.properties";
	
	private static final String UNDEFINED_PROPERTY_PREFIX = "UNDEFINED.";
	
    /** Internal property prefix */
    protected static final String INTERNAL_PROPERTY_PREFIX         = "internal";
	
    /**
     * GDS configuration property file that controls property loading
     */
    private static final String PROPERTY_OVERRIDES_FILENAME = "property.override.properties";
    /**
     * Property that allows or disallows use of user configurations.
     */
    private static final String USER_CONFIG_OVERRIDE = "property.override.user.config.enable";
    /**
     * Property that specifies which directories outside the default system and project directories
     * are allowed to load internal properties.
     */
    private static final String INTERNAL_PROPERTY_WHITELIST_DIRS = "property.override.internal.whitelist";

    /** The SSE context flag */
    protected final boolean                        sseFlag;
	
    /**
	 * Creates a GdsHierarchicalProperties instance for the properties file
	 * with the given base name.  
	 * 
	 * @param baseFileName the name of the properties file, no directory path
	 * @param init true to automatically search for and load property files; 
	 *        false to leave this to the subclass or do it later
	 */
	public GdsHierarchicalProperties(final String baseFileName, final boolean init) {
        this(baseFileName, init, new SseContextFlag());
	}

    /**
     * Creates a GdsHierarchicalProperties instance for the properties file
     * with the given base name.
     * 
     * @param baseFileName
     *            the name of the properties file, no directory path
     *            false to leave this to the subclass or do it later
     * @param sseFlag
     *            The SSE Context Flag - Whether or not the application is SSE
     */
    public GdsHierarchicalProperties(final String baseFileName, final SseContextFlag sseFlag) {
        this(baseFileName, true, sseFlag);
    }
	
	/**
     * Creates a GdsHierarchicalProperties instance for the properties file
     * with the given base name.
     * 
     * @param baseFileName
     *            the name of the properties file, no directory path
     * @param init
     *            true to automatically search for and load property files;
     *            false to leave this to the subclass or do it later
     * @param sseFlag
     *            The SSE Context Flag - Whether or not the application is SSE
     */
    public GdsHierarchicalProperties(final String baseFileName, final boolean init, final SseContextFlag sseFlag) {
	    assert baseFileName != null: "Base property file name cannot be null";
	    this.propertyFileName = baseFileName;
        this.sseFlag = sseFlag.isApplicationSse();
	    
	    
	    /*
         * Loading the property overrides property file
         */
        loadMasterOverrideProperties();
	    
	    if (init) {
	        loadProperties();
	    }
	}
	
	/**
	 * Clears all static members, including cached properties in shared files.
	 * 
	 */
	public synchronized static void reset() {
	    propertyOverrides.clear();
	    overridesLoaded.set(false);
	    cachedProperties.clear();	    
	}

    /**
     * Loads the properties files into the local property table. Clears the
     * property table. Searches first the class path, starting "config/", then
     * system directory, then project directory, then user directory, and loads
     * the files found there in that order.
     * 
     * 07/27/17 - Updated the order in which directories
     *          are checked. Moved repeated code to separate functions. Changed
     *          loading so filtering is done on the set of properties from a single
     *          file prior to its inclusion in the full set.
     * 08/01/17 - Updated to use the getFullConfigPathList,
     *          which potentially uses either the fixed or flex paths instead of
     *          being a fixed list that is created in the method
     */
	protected void loadProperties() {
		this.properties.clear();
		
        final List<String> dirs = GdsSystemProperties.getFullConfigPathList(sseFlag);

		//system and overrides are different names, all others share a name.
		final String systemConfigFile   = dirs.get(0) + File.separator + this.propertyFileName;
		
		final boolean loadUserProps = Boolean.parseBoolean(propertyOverrides.getProperty(USER_CONFIG_OVERRIDE, "true"));
		

		if(!loadProperties(systemConfigFile, true)){
			log.debug("No ", this.propertyFileName, " resides in ", GdsSystemProperties.getSystemConfigDir());
		}
		
		for(int i = 1 ; i < dirs.size() ; i++){
            final boolean whitelisted = isDirWhitelisted(dirs.get(i));
		    if(!loadUserProps && !whitelisted){
		        continue;
		    }
		    final String filePath = dirs.get(i) + File.separator + CONSOLIDATED_PROPERTY_FILE_NAME;
		    
		    /* 
		     * Load the override file into the static cache.  If it is already cached,
		     *  this will just return true. Then copy the filtered override properties into the property object 
		     *  for this instance.
		     */
		    if (loadPropertyCache(filePath, whitelisted, getPropertyPrefixRegex(), getInternalPropertyPrefixRegex())) {
		        final Properties p = new Properties();
		        p.putAll(cachedProperties.get(filePath));
		        filterProperties(p, whitelisted, getPropertyPrefixRegex(), getInternalPropertyPrefixRegex());
		        this.properties.putAll(p);
		    }
		}
		
		/*
         *  removed addition of master override properties
         * to the instance properties. Turns out, all these were filtered out anyway.
         */
		
		log.trace("Contents of properties file ", this.propertyFileName,": ", this.properties);
		
		validate();
	}
	
	/**
	 * Validates the just-loaded configuration. Subclasses should override.
	 */
	protected void validate() {
	    SystemUtilities.doNothing();
	}
	

	/**
	 * Properties from the supplied filepath are added to the property set for this instance.
	 * @param propFilePath the filepath (relative or absolute) to the file
	 *        containing properties to be loaded.
	 * @return true if the file exists, false is not
	 */
	private boolean loadProperties(final String propFilePath, final boolean includeInternal){
	    final File propFile = new File(propFilePath);
	    if (propFile.exists()) {
	        final Properties tempProperties = new Properties();
	        try {
	            tempProperties.load(new FileReader(propFile));
	            filterProperties(tempProperties, includeInternal, getPropertyPrefixRegex(), getInternalPropertyPrefixRegex());
	            this.properties.putAll(tempProperties);
	            tempProperties.clear();
	            log.debug("Loaded properties from ", propFile.getPath());
	        } catch (final IOException e) {
	            log.error("I/O error loading " + propFile.getPath());
	        }
	        return true;
	    } else {
	        return false;
	    }
	}
	
	/**
     * Properties from the supplied filepath are loaded and cached in an individual property 
     * object in the static map for override property files.
     * @param propFilePath the filepath (relative or absolute) to the file
     *        containing properties to be loaded.
     * @param includeInternal true to leave internal properties intact, false to exclude
     * @param propertyPrefix the regular expression matching the properties to be kept; others will be filtered out
     * @param internalPropertyPrefix the regular expression matching the internal properties to be kept; others will be filtered out
     * @return true if the file exists, false is not
     */
    private synchronized static boolean loadPropertyCache(final String propFilePath, final boolean includeInternal, final String propertyPrefix, final String internalPropertyPrefix){
        final File propFile = new File(propFilePath);
        if (cachedProperties.get(propFilePath) != null)  {
            return true;
        }
        if (propFile.exists()) {
            final Properties tempProperties = new Properties();
            try {
                tempProperties.load(new FileReader(propFile));
                cachedProperties.put(propFilePath, tempProperties);
                log.debug("Loaded properties from ", propFile.getPath());
            } catch (final IOException e) {
                log.error("I/O error loading " + propFile.getPath());
            }
            return true;
        } else {
            return false;
        }
    }
	
	/**
	 * Filters the supplied set of properties as per previously configured
	 * parameters.
	 * @param props the Properties to be filtered
	 * @param includeInternal true to leave internal properties intact, false to exclude
	 * @param propertyPrefix prefix the property name prefix for this instance
	 * @param internalPropertyPrefix property regex for this instance
	 */
	private static void filterProperties(final Properties props, final boolean includeInternal, final String propertyPrefix, final String internalPropertyPrefix){
        
        /**
         * Updated this filter - now will filter out
         * all properties that don't begin with the prefix and aren't of the
         * appropriate category.
         */
        for (final String key : props.stringPropertyNames()) {
            boolean keepProperty = false;
            keepProperty = key.matches(propertyPrefix);
            
            if(!includeInternal){
                keepProperty = keepProperty && !key.matches(internalPropertyPrefix);
            }

            switch (propertiesToLoad) {
                case NO_DESCRIPTIVES:
                    keepProperty = keepProperty && !key.matches(ANY_DESCRIPTIVE_REGEX);
                    break;
                case DESCRIPTIVES_ONLY:
                    keepProperty = keepProperty && key.matches(ANY_DESCRIPTIVE_REGEX);
                    break;
                case INCLUDE_DESCRIPTIVES:
                default:
            }

            if (!keepProperty) {
                props.remove(key);
            }
        }
	}
	
	/**
	 * Gets a property value, defaulting to supplied value if not found.
	 * 
	 * @param propertyName name of the property to get
	 * @param defaultVal default value
	 * @return property value, or the default
	 */
	public String getProperty(final String propertyName, final String defaultVal) {
	    String val = this.properties.getProperty(propertyName, defaultVal);
	    if (val != null) {
	        val = val.trim();
	    }
	    /* An empty string will be considered a null property */
	    if (val != null && val.isEmpty()) {
	        val = defaultVal;
	    }
	    return val;
	}
	
    /**
     * Retrieve a Properties object which is a copy of the internal Properties
     * 
     * @return a Properties object which is a copy of the internal Properties
     */
    public Properties getProperties() {
        final Properties p = new Properties();
        p.putAll(this.properties);
        return p;
    }

	/**
	 * Gets a property value.
	 * 
	 * @param propertyName name of the property to get
	 * @return property value, or null if not found
	 */
	public String getProperty(final String propertyName) {

        String val = this.properties.getProperty(propertyName);
        if (val != null) {
            val = val.trim();
        }
        /*
         *  An empty string will be considered a null property
         */
        if (val != null && val.isEmpty()) {
            val = null;
        }

        return val;   
	}
	
	/**
	 * Gets a property value as an integer.
	 * 
	 * @param propertyName name of the property to get
	 * @param defaultVal default value if not defined or in error
	 * @return integer property value
	 * 
	 */
	public int getIntProperty(final String propertyName, final int defaultVal) {
	    final String temp = getProperty(propertyName);
	    if (temp == null) {
	        return defaultVal;
	    }
	    try {
	        return Integer.parseInt(temp);
	    } catch (final NumberFormatException e) {
	        log.error("Value for " + propertyName + " in " + this.propertyFileName + " is not an integer");
	        
	    }
	    return defaultVal;
	}
	
	/**
	 * Gets a property value as an double.
	 * 
	 * @param propertyName name of the property to get
	 * @param defaultVal default value if not defined or in error
	 * @return double property value
	 * 
	 */
	public double getDoubleProperty(final String propertyName, final double defaultVal) {
	    final String temp = getProperty(propertyName);
	    if (temp == null) {
	        return defaultVal;
	    }
	    try {
	        return Double.parseDouble(temp);
	    } catch (final NumberFormatException e) {
	        log.error("Value for " + propertyName + " in " + this.propertyFileName + " is not an double");
	        
	    }
	    return defaultVal;
	}
	
	/**
     * Gets a property value as a long.
     * 
     * @param propertyName name of the property to get
     * @param defaultVal default value if not defined or in error
     * @return integer property value
     * 
     */
	public long getLongProperty(final String propertyName, final long defaultVal) {
        final String temp = getProperty(propertyName);
        if (temp == null) {
            return defaultVal;
        }
        try {
            return Long.parseLong(temp);
        } catch (final NumberFormatException e) {
            log.error("Value for " + propertyName + " in " + this.propertyFileName + " is not a long integer");
            
        }
        return defaultVal;
    }
	
	/**
     * Gets a property value as a boolean.
     * 
     * @param propertyName name of the property to get
     * @param defaultVal default value if not defined or in error
     * @return integer property value
     * 
     */
    public boolean getBooleanProperty(final String propertyName, final boolean defaultVal) {
        final String temp = getProperty(propertyName);
        if (temp == null) {
            return defaultVal;
        }
        return Boolean.valueOf(temp).booleanValue();
    }

    /**
     * Gets the value of a list property (e.g, a property whose value is in CSV
     * format) as a List of Strings.
     * 
     * @param propertyName
     *            name of the property to get
     * @param defaultValue
     *            default value for the property (may be null)
     * @param delim
     *            delimiter string for the elements in the list
     * @return List of Strings. Never null.
     * 
     */
    public List<String> getListProperty(final String propertyName,
            final String defaultValue, final String delim) {
        String raw = getProperty(propertyName, defaultValue);

        final List<String> result = new LinkedList<String>();
        if (raw == null) {
            return result;
        } else if (raw.isEmpty()) {
            raw = defaultValue;
        }
        final String[] pieces = raw.split(delim);
        for (final String piece : pieces) {
            result.add(piece.trim());
        }
        return result;
    }
    
    /**
     * Gets the value of a list property (e.g, a property wose value is in CSV format) as a List of Integers.
     * 
     * @param propertyName
     *            name of the property to get
     * @param defaultValue
     *            default value for the property (may be null)
     * @param delim
     *            delimiter string for the elements in the list
     * @return List of Integers. Never null
     * @throws NumberFormatException
     *             if the property cannot be interpreted as an Integer or an int
     */
    public List<Integer> getIntListProperty(final String propertyName, final String defaultValue, final String delim)
            throws NumberFormatException {
        final List<String> stringList = getListProperty(propertyName, defaultValue, delim);
        final List<Integer> intList = new LinkedList<>();
        if (!stringList.isEmpty()) {
            for (final String s : stringList) {
                if (!s.isEmpty()) {
                    intList.add(Integer.valueOf(s));
                }
            }
        }
        return intList;
    }

    /**
     * Gets the value of a list property (e.g, a property wose value is in CSV format) as a List of Integers.
     * 
     * @param propertyName
     *            name of the property to get
     * @param defaultValue
     *            default value for the property (may be null)
     * @param delim
     *            delimiter string for the elements in the list
     * @return List of Integers. Never null
     */
    public List<Long> getLongListProperty(final String propertyName, final String defaultValue, final String delim) {
        final List<String> stringList = getListProperty(propertyName, defaultValue, delim);
        final List<Long> longList = new LinkedList<>();

        if (!stringList.isEmpty()) {
            for (final String s : stringList) {
                if (!s.isEmpty()) {
                    longList.add(Long.valueOf(s));
                }
            }
        }
        return longList;
    }

    /**
     * Gets the value of a Number list property and expands it
     * 
     * @param propertyName
     *            The property name
     * @return Collection<Long> of the values
     * @throws ParseException if the range is improper
     */
    public List<String> getNumberRangeProperty(final String propertyName) throws ParseException {
        final String rangeStr = getProperty(propertyName);
        if (rangeStr == null) {
            return Collections.emptyList();
        } else {
        		return CliUtility.expandRange(rangeStr);
        }
    }

    /**
     * {@inheritDoc}
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return properties.toString();
    }
    
    /**
     * Gets a copy of the complete property map. Since it is a copy, the caller is free
     * to modify it.
     * 
     * @return Properties object
     * 
     */
    @Override
    public Properties asProperties() {
        final Properties p = new Properties();
        p.putAll(this.properties);
        return p;   
    }
    
    /**
     * Gets the base property file name (no path).
     * 
     * @return property file name
     * 
     */
    @Override
    public String getBaseFilename() {
        return this.propertyFileName;
    }

    /**
     * Logs an error message indicating that the specified property has an
     * invalid value.
     * 
     * @param propertyName
     *            name of the property
     * @param errVal
     *            value that was in error
     * @param defaultVal
     *            default value; may be null
     */
    protected void reportError(final String propertyName, final String errVal,
            final String defaultVal) {
        log.error("Invalid value (" + errVal + ") for property " + propertyName
                + " in property file " + getBaseFilename());
        if (defaultVal == null) {
            log.error("There is no default for this property");
        } else {
            log.error("Property value will default to " + defaultVal);
        }

    }
    
    /*
     * Adding method to load override properties
     */
    /**
     * Loads the master override properties.
     * 
     */
    private static synchronized void loadMasterOverrideProperties() {
        
        /** Only want to load the master override properties once. */
        if (overridesLoaded.getAndSet(true)) {
            return;
        }
        
        final String systemConfigDir = GdsSystemProperties.getSystemConfigDir();
        final String propertyOverridesPath = systemConfigDir + PROPERTY_OVERRIDES_FILENAME;
        final File propertyOverridesFile = new File(propertyOverridesPath);
        if(propertyOverridesFile.exists()) {
            try {
                propertyOverrides.load(new FileReader(propertyOverridesFile));
                log.debug("Loaded properties from ", propertyOverridesFile.getPath());
            } catch (final IOException e) {
                log.error("I/O error loading " + propertyOverridesFile.getPath());
            }
        }
    }
    
    /**
     * Gets the unique prefix for properties loaded by this properties object. This method should be overridden
     * by subclasses so that the different objects can all be placed into a hash table or
     * set. The hashCode is computed from the lookup key.
     * 
     * @return lookup key string
     * 
     */
    @Override
    public String getPropertyPrefix() {
        return UNDEFINED_PROPERTY_PREFIX;
    }
    
    /**
     * Get the property prefix as a regex.
     * @return the property prefix regex
     */
    private String getPropertyPrefixRegex(){
        final StringBuilder regex = new StringBuilder("^");
        final String tempStr = getPropertyPrefix();
        regex.append(tempStr.endsWith(".") ? tempStr.substring(0, tempStr.length()-1) : tempStr);
        regex.append("\\..+");
        
        return regex.toString();
    }
    
    /**
     * Get the property prefix with the internal prefix as a regex.
     * @return get the property and internal prefixes as a regex
     */
    private String getInternalPropertyPrefixRegex(){
        final StringBuilder regex = new StringBuilder("^");
        final String tempStr = getPropertyPrefix();
        regex.append(tempStr.endsWith(".") ? tempStr.substring(0, tempStr.length()-1) : tempStr);
        regex.append("\\.");
        regex.append(INTERNAL_PROPERTY_PREFIX + "\\..+");
        
        return regex.toString();
    }
    

    @Override
    public int hashCode() {
        return getPropertyPrefix().hashCode();
    }
    

    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof GdsHierarchicalProperties)) {
            return false;
        }
        return getPropertyPrefix().equals(((GdsHierarchicalProperties)o).getPropertyPrefix());
    }
    
    /**
     * Returns a map of the properties whose names match the supplied regular expression.
     * 
     * @param regexp the regular expression to match
     * 
     * @return Map of property keys and their values
     * 
     */
    public Map<String, String> getMatchingProperties(final String regexp) {
        final Map<String, String> result = new HashMap<String, String>();
        final Enumeration<Object> keys = this.properties.keys();
        while (keys.hasMoreElements()) {
            final String keyStr = (String)keys.nextElement();
            if (keyStr.matches(regexp)) {
                result.put(keyStr, properties.get(keyStr).toString());
            }
        }
        return result;
    }

	/**
	 * Logs a property value length error.
	 * @param prop property name
	 * @param val property value
	 * @param len allowed length of the property
	 */
	protected void logLengthError(final String prop, final String val, final int len) {
	    log.error("The value " +  val  + " for property " + prop + " in file "
	            + getBaseFilename()
	            + " is too long. Length is limited to " + len);
	    errorLogged = true;
	}

	/**
	 * Logs an error for a default property value that is not one of the allowed
	 * property values. Always sets the errorLogged member variable.
	 * 
	 * @param defaultProp
	 *            the default property name
	 * @param allowsProp
	 *            the allowed property name
	 *            
	 */
	protected void logContainmentError(final String defaultProp, final String allowsProp) {
	    log.error("The value for property " + defaultProp + " in file "
	            + getBaseFilename()
	            + " is not one of the allowed values specified by "
	            + allowsProp);
	    errorLogged = true;
	}

	/**
	 * Converts any set of objects to a sorted set of Strings.
	 * 
	 * @param toConvert
	 *            Set to convert
	 * @return Set of String
	 */
	protected Set<String> toSortedStringSet(final Set<? extends Object> toConvert) {
	    final SortedSet<String> result = new TreeSet<String>();
	
	    for (final Object vt : toConvert) {
	        result.add(vt.toString());
	    }
	
	    return result;
	}

	/**
	 * Converts any list of objects to a list of Strings.
	 * 
	 * @param toConvert
	 *            List to convert
	 * @return List of String
	 * 
	 */
	protected List<String> toStringList(final List<? extends Object> toConvert) {
	    final List<String> result = new LinkedList<String>();
	
	    for (final Object vt : toConvert) {
	        result.add(vt.toString());
	    }
	
	    return result;
	}
	
	/**
	 * Set the PropertySet enum that determines if properties, their descriptions, or both,
	 *  are loaded
	 * @param propsToLoad the new value of the PropertySet enum dictating if properties,
	 *        their descriptions, or both, are loaded
	 */
	public static void setPropertiesToLoad(final PropertySet propsToLoad){
	    if(propsToLoad != null){
	        propertiesToLoad = propsToLoad;
	    }
	}
	
	/**
	 * Get the current value of the PropertySet enum that determines if properties, their
	 * descriptions, or both, are loaded
	 * @return the current value of the PropertySet enum that determines if properties,
	 * their descriptions, or both, are loaded
	 */
	public static PropertySet getPropertiesToLoad() {
	    return propertiesToLoad;
	}

    @Override
    public boolean supportsFlatProperties() {
        return true;
    }
    

    /**
     * Get the list of directories that may contain properties files that are allowed to load internal properties.
     * By default this list will include the fixed configuration path directories along with any specified in the
     * override properties
     * @return the List of directories that can load internal properties
     */
    private List<String> getInternalWhitelistDirectories() {
        if(this.whitelistedDirectories == null){
            final String directories = propertyOverrides.getProperty(INTERNAL_PROPERTY_WHITELIST_DIRS);
            final List <String> dirList = new ArrayList<>();
            
            dirList.add(new File(GdsSystemProperties.getSystemConfigDir()).getAbsolutePath());
            dirList.add(new File(GdsSystemProperties.getProjectConfigDir(GdsSystemProperties.getSystemMission())).getAbsolutePath());
            if (sseFlag) {
                dirList.add(new File(GdsSystemProperties.getProjectConfigDir(sseFlag)).getAbsolutePath());
            }
            
            if( directories != null && !directories.isEmpty()){
                dirList.addAll(Arrays.asList(directories.split(File.pathSeparator)));
            }
            
            this.whitelistedDirectories = dirList;
        }
        
        return this.whitelistedDirectories;   
    }
    
    /**
     * Determine if a property files in the specified directory can load internal properties
     * @param checkedDirectory the absolute path to the folder that properties will be loaded from
     * @return TRUE if internal properties may be loaded, FALSE if not
     */
    private boolean isDirWhitelisted(final String checkedDirectory) {
        final List<String> checkDirs = getInternalWhitelistDirectories();
        
        for(final String dir : checkDirs){
            if(checkedDirectory.startsWith(dir)){
                return true;
            }
        }
        return false;
    }
}
