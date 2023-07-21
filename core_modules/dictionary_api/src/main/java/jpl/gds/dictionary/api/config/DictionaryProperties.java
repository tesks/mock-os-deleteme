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
package jpl.gds.dictionary.api.config;

import jpl.gds.dictionary.api.DictionaryClassContainer;
import jpl.gds.dictionary.api.DictionaryClassContainer.ClassType;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.IBaseDictionary;
import jpl.gds.security.loader.AmpcsUriPluginClassLoader;
import jpl.gds.shared.config.GdsHierarchicalProperties;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Markers;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.metadata.IMetadataHeaderProvider;
import jpl.gds.shared.metadata.ISerializableMetadata;
import jpl.gds.shared.metadata.MetadataKey;
import jpl.gds.shared.metadata.MetadataMap;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.template.Templatable;
import jpl.gds.shared.xml.XmlUtility;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 * This class encapsulates the FSW and SSE dictionary configuration information.
 * It provides methods for locating and defaulting dictionary files, and
 * provides methods for establishing and getting the dictionary parser
 * configuration. As part of the break-out of dictionary capabilities in
 * the AMCPS core, it has been augmented to support dictionary parser
 * configuration using an underlying Java properties file.
 * <p>
 * <br>
 * <b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b>
 * <p>
 * <p>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b>
 * <p>
 * <br>
 * <br>
 * 
 * The properties file (name defined by PROPERTY_FILE constant) includes
 * configuration properties moved here from the XML GDS configuration. The
 * properties files is loaded in the same sequence as other AMPCS configuration:
 * first from the system configuration directory, second from the project
 * configuration directory, and then finally from the user configuration
 * directory. At each stage, values loaded later override values loaded earlier.
 * Once loaded, configuration can be reloaded, and it can be modified in memory,
 * but it cannot be written back to the properties file.
 * <p>
 * This class supports both a static global instance and instantiation of other
 * instances. To get the global instance invoke getGlobalInstance().
 * 
 *
 */
public class DictionaryProperties extends GdsHierarchicalProperties implements Templatable, IMetadataHeaderProvider {

	/**
	 * Name of the default properties file.
	 */
	public static final String PROPERTY_FILE = "dictionary.properties";
	
    /*
     * In order to isolate implementation from
     * interfaces, reflection is used to create many dictionary-related objects,
     * including the dictionary parsers. In order to avoid scattering hard-coded
     * references to these classes everywhere, I have chosen to create constants
     * for the classes most likely created by reflection by classes outside of
     * the interface. This is somewhat ugly, but I can't think of any other way
     * and at least it's in one place.
     * 
     */
	/** Name of the dictionary root package */
	public static final String PACKAGE = "jpl.gds.dictionary.impl.";
	
	private static final String DICTIONARY_FILE = "Dictionary file ";	   
    private static final String ADDING = "Adding ";
    private static final String TO_CLASSPATH = " to classpath";
    private static final String NOT_FOUND = " not found";
   
    /** Value of the searchPath property indicating to search config directories. */
    public static final String CONFIG_PATH = "config";
    
    /** Value of the searchPath property indicating to search the current dictionary directory. */
    public static final String DICTIONARY_PATH = "dictionary";


    private static final String PROPERTY_PREFIX = "dictionary.";
    private static final String INTERNAL_PROPERTY_BLOCK = PROPERTY_PREFIX + "internal.";
    private static final String FLIGHT = "flight.";
    private static final String SIM = "sse.";
    private static final String DICT_ADAPTOR = "dictionaryAdaptorClass";
    private static final String SEARCH_PATH = "searchPath";
    private static final String FILE_NAME = "fileName";

	private static final String PRODUCT_DICT_VERSION_PROPERTY = PROPERTY_PREFIX + 
	        DictionaryType.PRODUCT.getDictionaryName() +  "." + FLIGHT + "isVersioned";
	private static final String DEFAULT_FSW_DICT_PROPERTY = PROPERTY_PREFIX + FLIGHT + "defaultDirectory";
	private static final String DEFAULT_FSW_VERSION_PROPERTY = PROPERTY_PREFIX + FLIGHT + "defaultVersion";
	private static final String DEFAULT_SSE_DICT_PROPERTY = PROPERTY_PREFIX + SIM + "defaultDirectory";
	private static final String DEFAULT_SSE_VERSION_PROPERTY =  PROPERTY_PREFIX + SIM + "defaultVersion";
	
	private static final String ALARM_RELOAD_PROPERTY = PROPERTY_PREFIX + "alarm.reloadInterval";
	private static final int ALARM_RELOAD_DEFAULT = 15;
	
	private static final String DICTIONARY_LIB_DIR_PROPERTY = PROPERTY_PREFIX  + "jarSubdirectory";
	private static final String DEFAULT_LIB_DIR = "lib";
	
	private static final String ENABLE_CHANNEL_FORMATTING_PROPERTY = PROPERTY_PREFIX + "channel.useFormatters";

	/** The default dictionary schema location for validation */
	public static final String DEFAULT_SCHEMA_VALIDATION_LOC  = "schema";
	private static final String SCHEMA_BLOCK                   = PROPERTY_PREFIX + DEFAULT_SCHEMA_VALIDATION_LOC + ".";
	/** The Dictionary schema validation property */
	public static final String  SCHEMA_VALIDATION_PROPERTY     = SCHEMA_BLOCK + "validation";
	private static final String SCHEMA_VALIDATION_DIR_PROPERTY = SCHEMA_VALIDATION_PROPERTY + "." + "directory";
	
	/**
	 * Name of the configuration property for multimission version.
	 */
	private static final String MM_VERSION = "multimissionVersion";

	/**
	 * Name of the configuration property that defines command opcode bit length.
	 */
	private static final String OPCODE_BIT_LENGTH_PROPERTY =
        PROPERTY_PREFIX + DictionaryType.COMMAND.getDictionaryName() + "." + FLIGHT + "opcodeBitLength";
	private static final String OPCODE_BIT_LENGTH_DEFAULT = "16";
   /**
	 * Flag to hide opcodes in log messages or telemetry in which they are known to occur.
	 * Note that this cannot be applied if dictionary information cannot indicate that a given blob
     * is in fact an opcode.
	 */
	private static final String OPCODE_HIDE_PROPERTY = PROPERTY_PREFIX +
			DictionaryType.COMMAND.getDictionaryName() + "." + FLIGHT + "hideOpcodes";
	/**
	 * Name of the configuration property that indicates whether to parse command sequence directives.
	 */
	private static final String PARSE_SEQ_DIRECTIVES_PROPERTY =  PROPERTY_PREFIX + 
	        DictionaryType.COMMAND.getDictionaryName() +  "." + FLIGHT + "parseSequenceDirectives";

	/** Name of the configuration property indicating assumed telemetry transfer frame format for use with 
	 *  old frame dictionaries.
	 *  
	 */
    private static final String ASSUMED_FRAME_FORMAT_PROPERTY = PROPERTY_PREFIX + 
            DictionaryType.FRAME.getDictionaryName() + FLIGHT + "downlink.assumedFrameFormat";
    
	/** 
	 * Default dictionary root directory 
	 */
	private static final String DEFAULT_DICT_ROOT = "/msop/dict";
	
	/**
	 * Name of the mapper property block and filepath property
	 * 
	 */
	private static final String MAPPER_PROPERTY_BLOCK = PROPERTY_PREFIX + "mapper.";
	private static final String FSW_MAPPER_PROPERTY_BLOCK = MAPPER_PROPERTY_BLOCK + FLIGHT;
	private static final String MAPPER_FILEPATH_PROPERTY = FSW_MAPPER_PROPERTY_BLOCK + "filepath";

	/**
	 * The dictionary cache eviction timeout, in hours
	 *
	 */
	private static final int CACHE_EVICTION_TIMEOUT_DEFAULT = 12;
	private static final String CACHE_EVICTION_TIMEOUT_PROPERTY = INTERNAL_PROPERTY_BLOCK + "cache.eviction.timeout";
    
	private String fswVersion;
	private String sseVersion;
	private String fswDictionaryDir;
	private String sseDictionaryDir;
	private boolean hideOpcode;
	
	private final Map<DictionaryType, String> fswOverrideLocations = new HashMap<>();
	private final Map<DictionaryType, String> sseOverrideLocations = new HashMap<>();


	private ISerializableMetadata header;
	private boolean dirty = true;


	/**
     * Creates a new instance of DictionaryConfiguration, but does not scan the
     * file system or initialize the current FSW or SSE dictionary directories
     * or fields. This constructor should be used when only the default properties
     * from the property file are needed, and not current dictionary selection. 
     * For instance, use this constructor in a configuration dump tool.
     */
    public DictionaryProperties()
    {   
        this(false);
    }
    
	/**
	 * Creates a new instance of DictionaryConfiguration. If the initialize
	 * parameter is set to true, the object will be initialized with dictionary
	 * directory and version defaults from the configuration and the
	 * environment. If that flag is false, these fields in the object will be
	 * left completely blank. In either case, the dictionary properties file
	 * will be loaded.
	 * 
	 * @param initialize
	 *            true to initialize all members, false to not
	 */
	public DictionaryProperties(final boolean initialize)
	{   
        this(initialize, new SseContextFlag());
	}

    /**
     * Creates a new instance of DictionaryConfiguration. If the initialize
     * parameter is set to true, the object will be initialized with dictionary
     * directory and version defaults from the configuration and the
     * environment. If that flag is false, these fields in the object will be
     * left completely blank. In either case, the dictionary properties file
     * will be loaded.
     * 
     * @param initialize
     *            true to initialize all members, false to not
     * @param sseFlag
     *            The SSE context flag
     */
    public DictionaryProperties(final boolean initialize, final SseContextFlag sseFlag) {
        super(PROPERTY_FILE, sseFlag);

        if (initialize) {
            initToDefaults();
        }
    }

	/**
	 * Loads the default properties file.
	 * Initialize dictionary directory and version fields to configured defaults.
	 * 
	 */
	public void initToDefaults() {
		super.loadProperties();
		fswDictionaryDir = getDefaultFswDictionaryDir();
		sseDictionaryDir = getDefaultSseDictionaryDir();
		fswVersion = getDefaultFswVersion();
		sseVersion = getDefaultSseVersion();
		hideOpcode = getHideOpcodeProperty();
		dirty = true;
	}
	
	/**
	 * Sets an override path for a specific dictionary. This overrides the default
	 * search path for the dictionary in question. When fetched, the dictionary file
	 * will be located using the override path.
	 * 
	 * @param type DictionaryType
	 * @param path the new path; set to null to cancel a previous override path
	 * @param forSse true if for SSE, false if for flight
	 */
	public void setOverrideLocation(final DictionaryType type, final String path, final boolean forSse) {
		if (path == null) {
			if (forSse) {
				sseOverrideLocations.remove(type);
			} else {
				fswOverrideLocations.remove(type);
			}
		} else {
			if (forSse) {
				sseOverrideLocations.put(type, path);
			} else {
				fswOverrideLocations.put(type, path);
			}
		}
	}
	
	/**
	 * Gets the overridden location (path) for a specific dictionary type.
	 * 
	 * @param type the DictionaryType
	 * @param forSse true if for SSE, false if for FSW
	 * @return path to dictionary if overridden from the default search path, null
	 *         if no override exists
	 */
	public String getOverrideLocation(final DictionaryType type, final boolean forSse) {
		if (forSse) {
			return sseOverrideLocations.get(type);
		} else {
			return fswOverrideLocations.get(type);
		}

	}

	/**
	 * Returns the flight software dictionary directory.
	 * 
	 * @return fsw dictionary directory path
	 */
	public String getFswDictionaryDir() {
		return fswDictionaryDir;
	}

	/**
	 * Sets the flight software dictionary directory.
	 * 
	 * @param dictDir
	 *            the directory to set
	 */
	public void setFswDictionaryDir(final String dictDir) {
		fswDictionaryDir = dictDir;
		dirty = true;
	}

	/**
	 * Returns the SSE dictionary directory.
	 * 
	 * @return sse dictionary directory path
	 */
	public String getSseDictionaryDir() {
		return sseDictionaryDir;
	}

	/**
	 * Sets the SSE dictionary directory.
	 * 
	 * @param dictDir
	 *            the directory to set
	 */
	public void setSseDictionaryDir(final String dictDir) {
		sseDictionaryDir = dictDir;
		dirty = true;
	}

	/** 
	 * Returns the fswVersion.
	 * 
	 * @return fsw version
	 */
	public String getFswVersion() {
		return fswVersion;
	}

	/**
	 * Sets the fswVersion
	 * 
	 * @param fswVersion
	 *            the fswVersion to set
	 */
	public void setFswVersion(final String fswVersion) {
		this.fswVersion = fswVersion;
		dirty = true;
	}

	/**
	 * Returns the sseVersion.
	 * 
	 * @return sse version
	 */
	public String getSseVersion() {
		return sseVersion;
	}

	/**
	 * Sets the sse version.
	 * 
	 * @param sseVersion
	 *            the sseVersion to set
	 */
	public void setSseVersion(final String sseVersion) {
		this.sseVersion = sseVersion;
		  dirty = true;
	}
	
	/**
	 * Returns the file path for the FSW to dictionary mapping file.
	 * 
	 * @return file path for FSW to dictionary mapping file
	 */
	public String getFswToDictMappingFilepath(){
		return getProperty(MAPPER_FILEPATH_PROPERTY);  
	}
	
	/**
	 * Retrieves the default dictionary directory root for the current mission,
	 * regardless of any current dictionary directory or version settings.
	 * 
	 * @return the default dictionary directory path
	 */
	public String getDefaultFswDictionaryDir() {
		// This is to allow unit tests to override the dictionary directory
        String dir = GdsSystemProperties.getSystemProperty(DEFAULT_FSW_DICT_PROPERTY);
		if (dir == null) {
		    dir = getProperty(
		            DEFAULT_FSW_DICT_PROPERTY, DEFAULT_DICT_ROOT);
		}
		return dir;
	}

	/**
	 * Retrieves the default dictionary directory root for the SSE for the
	 * current mission, regardless of any current dictionary directory or
	 * version settings.
	 * 
	 * @return the default dictionary directory path
	 */
	public String getDefaultSseDictionaryDir() {
		// This is to allow unit tests to override the dictionary directory
        String dir = GdsSystemProperties.getSystemProperty(DEFAULT_SSE_DICT_PROPERTY);
	    if (dir == null) {
	        dir = getProperty(
	                DEFAULT_SSE_DICT_PROPERTY, DEFAULT_DICT_ROOT);
	    }
	    return dir;
	}


	/**
	 * Returns the full path of the telemetry dictionary file with the given
	 * name for the given mission, based upon current dictionary directory and
	 * version settings. This method will return null if the dictionary file
	 * does not exist in the configured location.
	 * 
	 * @param filename
	 *            the name (only, not path) of the dictionary file to find
	 * @param mission
	 *            the mission ID string (lower case)
	 * @return the full path to the file, if found; otherwise null
	 * @throws DictionaryException
	 *             thrown if dictionary does not exist
	 */
	private String getDictionaryFile(final String filename, final String mission)
			throws DictionaryException {
		String defaultVer = null;
		String defaultDir = null;

		if (mission.toLowerCase().endsWith(GdsSystemProperties.SSE_MISSION_SUFFIX)) {
			defaultVer = sseVersion;
			defaultDir = sseDictionaryDir;
		} else {
			defaultVer = fswVersion;
			defaultDir = fswDictionaryDir;
		}

		// check the dictionary version directory first
		final String versionDirFile = defaultDir + File.separator + mission
				+ File.separator + defaultVer + File.separator + filename;
        log.debug("Version Dir File = ", new File(versionDirFile).getAbsolutePath());
		File f = new File(versionDirFile);
		if (f.exists()) {
			return (f.getAbsolutePath());
		} else {
			// if there's no dictionary in the version directory, check the
			// mission directory just above it
			final String missionDirFile = defaultDir + File.separator + mission
					+ File.separator + filename;
			log.debug("Mission Dir File = " , missionDirFile);
			f = new File(missionDirFile);
			if (f.exists()) {
				return (f.getAbsolutePath());
			}
		}
        /* Report the file name people expect (in the version dir,
         * not the one in the level above). 
         */
        throw new DictionaryException(DICTIONARY_FILE + new File(versionDirFile).getAbsolutePath() + NOT_FOUND);
	}

	/**
	 * Returns the full path of the telemetry dictionary file with the given
	 * name for the current mission, based upon current dictionary directory and
	 * version settings. This method will return null if the dictionary file
	 * does not exist in the configured location.
	 * <p>
	 * CAVEAT: This method is NOT the way to load dictionary files that
	 * are included in the system-level dictionary.properties file, because
	 * it does not utilize the configured search method.  It should be
	 * used ONLY to load non-dictionary files that happen to be located
	 * in the dictionary structure, or dictionary files for which there
	 * is as yet no core_dictionary implementation so they cannot be loaded
	 * any other way.
	 * 
	 * @param filename
	 *            the name (only, not path) of the dictionary file to find
	 * @return the full path to the file, if found; otherwise null
	 * @throws DictionaryException
	 *             thrown if encountered    
	 */
	public String getDictionaryFile(final String filename)
			throws DictionaryException {
        return getDictionaryFile(filename, GdsSystemProperties.getSystemMissionIncludingSse(sseFlag));
		
	}
	
	/**
	 * Returns the full path of the dictionary file of the given type for the
	 * current mission, based upon current configuration. Where is file is
	 * looked for depends upon the configured search path and file name for the
	 * supplied dictionary type. This method will find SSE dictionaries if
	 * GdsSystemProperties.applicationIsSse() is true. Otherwise, you must use
	 * findSseFileForSystemMission(). This method will return null if the
	 * dictionary file does not exist in the configured location.
	 * 
	 * @param dictType
	 *            the type indicator for the dictionary to load
	 * @return the full path to the file, if found; otherwise null
	 * @throws DictionaryException
	 *             thrown if encountered
	 */
	public String findFileForSystemMission(final DictionaryType dictType)
	        throws DictionaryException {
		
        String fullPath = getOverrideLocation(dictType, sseFlag);
		
        if (fullPath != null) {
        	if (new File(fullPath).exists()) {
	            return fullPath;
	        } else {
	        	throw new DictionaryException(DICTIONARY_FILE + fullPath
						+ NOT_FOUND);
			}
        }
        final String searchPath = sseFlag ? getSseDictionarySearchPath(dictType)
	            : getDictionarySearchPath(dictType);
        final String filename = sseFlag ? getSseDictionaryFileName(dictType)
	            : getDictionaryFileName(dictType);
	    if (searchPath.equalsIgnoreCase(DICTIONARY_PATH)) {
            return getDictionaryFile(filename, GdsSystemProperties.getSystemMissionIncludingSse(sseFlag));
	    } else if (searchPath.equalsIgnoreCase(CONFIG_PATH)) {
	    	/** Throw exception if path returns null.
	    	 * Makes behavior consistent with the dictionary path behavior.
	    	 */
            fullPath = GdsSystemProperties.getMostLocalPath(filename, sseFlag);
	    	if (fullPath == null) {
		 		throw new DictionaryException(DICTIONARY_FILE + filename
						+ NOT_FOUND);
			}
	    	return fullPath;
	    } else {
	        final String wholePath = searchPath + File.separator + filename;
	        if (new File(wholePath).exists()) {
	            return wholePath;
	        }
	    }
	    return null;
	}

	/**
	 * Returns the full path of the SSE/GSE dictionary file of the given type
	 * for the current mission, based upon current configuration. Where is file
	 * is looked for depends upon the configured search path and file name for
	 * the supplied dictionary type. This method will return null if the
	 * dictionary file does not exist in the configured location.
	 * 
	 * @param dictType
	 *            the type indicator for the dictionary to load
	 * @return the full path to the file, if found; otherwise null
	 * @throws DictionaryException
	 *             thrown if encountered
	 */
	public String findSseFileForSystemMission(final DictionaryType dictType)
	        throws DictionaryException {

        final String fullPath = getOverrideLocation(dictType, true);
		
        if (fullPath != null) {
        	if (new File(fullPath).exists()) {
	            return fullPath;
	        } else {
	        	throw new DictionaryException(DICTIONARY_FILE + fullPath
						+ NOT_FOUND);
			}
        }
         
	    final String searchPath = getSseDictionarySearchPath(dictType);
	    final String filename = getSseDictionaryFileName(dictType);
	    if (searchPath.equalsIgnoreCase(DICTIONARY_PATH)) {
	        return getDictionaryFile(filename,
	                GdsSystemProperties.getSseNameForSystemMission());
	    } else if (searchPath.equalsIgnoreCase(CONFIG_PATH)) {
            return GdsSystemProperties.getMostLocalPath(filename, sseFlag);
	    } else {
	        final String wholePath = searchPath + File.separator + filename;
	        if (new File(wholePath).exists()) {
	            return wholePath;
	        }
	    }
	    return null;
	}
    
	/**
	 * Indicates whether the product dictionary is configured to be versioned
	 * along with the flight dictionary.
	 * 
	 * @return true if product dictionary is versioned, false if not
	 * 
	 */
	public boolean getProductDictionaryIsVersioned() {
	    return Boolean.valueOf(getProperty(
	            PRODUCT_DICT_VERSION_PROPERTY, "false"));
	}

	/**
	 * Sets the flag indicating whether the product dictionary is versioned
	 * along with the flight dictionary.
	 * 
	 * @param isVersioned
	 *            true if product dictionary is versioned, false if not
	 * 
	 */
	public void setProductDictionaryIsVersioned(final boolean isVersioned) {
	    properties.setProperty(PRODUCT_DICT_VERSION_PROPERTY,
	            String.valueOf(isVersioned));
	}

	/**
	 * Returns the full path of the product dictionary directory with the
	 * default name for the current mission, based upon current dictionary
	 * directory setting. This method will return null if the dictionary
	 * directory does not exist in the configured location.
	 * 
	 * @return the full path to the file, if found; otherwise null
	 */
	public String getProductDictionaryDir()  {

	    /* Versioned product dictionary can ONLY be used with a 'dictionary' search path */
	    if (getProductDictionaryIsVersioned() && !getDictionarySearchPath(DictionaryType.PRODUCT).equals(DICTIONARY_PATH)) {
	        log.error("Versioned product dictionary is mis-configured");
	        log.error("Versioned product dictionaries can only be configured with 'dictionary' search path; overriding");
	        setDictionarySearchPath(DictionaryType.PRODUCT, DICTIONARY_PATH);
	    }
	    try {
	        return findFileForSystemMission(DictionaryType.PRODUCT);
	    } catch (final DictionaryException e) {
	        return null;
	    }

	}

	/**
	 * Gets a list of available flight software dictionary versions by
	 * assembling the list of version sub-directories in the FSW dictionary
	 * directory, using the current dictionary directory setting.
	 * 
	 * @return a List of version directory names
	 */
	public List<String> getAvailableFswVersions() {
		return (getAvailableFswVersions(fswDictionaryDir));
	}

	/**
	 * Gets a list of available flight software dictionary versions by
	 * assembling the list of version sub-directories in the FSW dictionary
	 * directory, using the given root dictionary directory.
	 * 
	 * @param inDictDir
	 *            the root dictionary directory path to use
	 * @return a List of version directory names
	 */
	public List<String> getAvailableFswVersions(final String inDictDir) {
		final List<String> result = new ArrayList<>();

		final String dictDir = inDictDir
				+ File.separator
				+ GdsSystemProperties.getSystemMission();

		final File dirFile = new File(dictDir);
		if (!dirFile.exists()) {
			return (result);
		}

		final String defaultVersionName = getProperty(
				DEFAULT_FSW_VERSION_PROPERTY, null);

		final File[] list = dirFile.listFiles(new DictDirOnlyFilter());
		final List<File> sortedList = sortByLastModifiedTime(list);
		for (final File version : sortedList) {
			String versionString = version.getName();
			if (versionString.equals(defaultVersionName)) {
				try {
					versionString = version.getCanonicalFile().getName();
				} catch (final IOException ioe) {
					// don't care
				}

				if (result.remove(versionString) 
						|| !result.contains(versionString)) {
					result.add(0, versionString);
				}
			} else {
				if (!result.contains(versionString)) {
					result.add(versionString);
				}
			}
		}

		return (result);
	}

	/**
	 * Gets the default FSW version. Unfortunately, there is no algorithm for
	 * determining this. So here we just take the first one we find. Someone
	 * will complain, no doubt, and perhaps that person will have a better
	 * suggestion.
	 * 
	 * @return the version directory name, or null if no versions found
	 */
	public String getDefaultFswVersion() {
		final List<String> list = getAvailableFswVersions();
		if (list.isEmpty()) {
			return null;
		}
		return list.get(0);
	}

	/**
	 * Gets a list of available SSE dictionary versions by assembling the list
	 * of version sub-directories in the SSE dictionary directory, using the
	 * current dictionary directory setting.
	 * 
	 * @return a List of version directory names
	 */
	public List<String> getAvailableSseVersions() {
		return (getAvailableSseVersions(sseDictionaryDir));
	}

	/**
	 * Gets a list of available SSE dictionary versions by assembling the list
	 * of version sub-directories in the SSE dictionary directory, using the
	 * given root dictionary directory. If the current mission has no associated
	 * SSE, returns an empty list.
	 * 
	 * @param dictDirRoot
	 *            the root dictionary directory path to use
	 * @return an List of version directory names
	 */
	public List<String> getAvailableSseVersions(final String dictDirRoot) {
		final List<String> result = new ArrayList<>();
		if (dictDirRoot == null) {
			return result;
		}

		final StringBuilder dictDir = new StringBuilder(dictDirRoot);
		dictDir.append(File.separator); 
        dictDir.append(GdsSystemProperties.getSseNameForSystemMission());

		final File dirFile = new File(dictDir.toString());
		if (!dirFile.exists()) {
			return (result);
		}

		final String defaultVersionName = getProperty(
				DEFAULT_SSE_VERSION_PROPERTY, null);

		final File[] list = dirFile.listFiles(new DictDirOnlyFilter());
		final List<File> sortedList = sortByLastModifiedTime(list);
		for (final File version : sortedList) {
			String versionString = version.getName();
			if (versionString.equals(defaultVersionName)) {
				try {
					versionString = version.getCanonicalFile().getName();
				} catch (final IOException ioe) {
					// don't care
				}

				if (result.remove(versionString)
						|| !result.contains(versionString)) {
					result.add(0, versionString);
				}
			} else {
				if (!result.contains(versionString)) {
					result.add(versionString);
				}
			}
		}

		return (result);
	}

	/**
	 * Sorts a list of files by last modified time, descending.
	 * 
	 * @param fileList list of File objects
	 * @return sorted List of File objects
	 */
	private List<File> sortByLastModifiedTime(final File[] fileList) {
		final List<File> result = new ArrayList<>();
		if (fileList != null) {
			for (int i = 0; i < fileList.length; i++) {
				final long lastMod = fileList[i].lastModified();
				if (result.isEmpty()) {
					result.add(fileList[i]);
				} else {
					int addIndex = 0;
					for (int j = 0; j < result.size(); j++) {
						final File curFile = result.get(j);
						if (lastMod < curFile.lastModified()) {
							addIndex++;
						} else {
							break;
						}
					}
					result.add(addIndex, fileList[i]);
				}
			}
		}
		return result;
	}

	/**
	 * Gets the default SSE version. Unfortunately, there is no algorithm for
	 * determining this. So here we just take the first one we find. Someone
	 * will complain, no doubt, and perhaps that person will have a better
	 * suggestion.
	 * 
	 * @return the version directory name, or null if no versions found
	 */
	public String getDefaultSseVersion() {
		final List<String> list = getAvailableSseVersions();
		if (list.isEmpty()) {
			return null;
		}
		return list.get(0);
	}



	/**
     * Initializes this configuration from values in the given configuration
     * object.
     * 
     * @param dc
     *            the DictionaryConfiguration to get values from
     */
	public void copyValuesFrom(final DictionaryProperties dc) {
        if (this == dc) {
            return;
        }
		setFswDictionaryDir(dc.getFswDictionaryDir());
		setFswVersion(dc.getFswVersion());
		setSseDictionaryDir(dc.getSseDictionaryDir());
		setSseVersion(dc.getSseVersion());
		setHideOpcode(dc.getHideOpcode());
		this.properties.clear();
		if(dc.properties != null) {
			this.properties.putAll(dc.properties);
		}
	}



	/**
	 * Returns the command opcode bit length for opcodes in the command dictionary.
	 * 
	 * @return bit length
	 * 
	 */
	public int getOpcodeBitLength()
    {
        final String raw   = getProperty(OPCODE_BIT_LENGTH_PROPERTY,
                                         OPCODE_BIT_LENGTH_DEFAULT);
        int          value = 0;
        boolean      error = false;

        try
        {
            value = Integer.parseInt(raw);
            error = ((value < 1) || (value > Integer.SIZE));
		}
        catch (final NumberFormatException nfe)
        {
            error = true;
		}

        if (error)
        {
			throw new IllegalStateException(
                          "Opcode bit length has invalid value of '" + 
					      raw                                        +
                          "' in the dictionary configuration");
        }

        return value;
	}


	/**
	 * Sets the the command opcode bit length for opcodes in the command
	 * dictionary.
	 *
	 * @param opcodeBitLength
	 *            the length to set
	 * 
	 */
	public void setOpcodeBitLength(final int opcodeBitLength) {
	    this.properties.setProperty(OPCODE_BIT_LENGTH_PROPERTY,
	            String.valueOf(opcodeBitLength));
	}

	 private boolean getHideOpcodeProperty() {
		String prop = this.properties.getProperty(OPCODE_HIDE_PROPERTY, "false");
		boolean value = Boolean.parseBoolean(prop);
		return value;
	}

	/**
	 * Returns whether opcodes should be hidden where possible. It is up to the users
	 * of the dictionaries to check and respect this flag wherever opcodes may be
	 * present in log messages, database records, exceptions, etc.
	 *
	 * @return true if opcodes should be hidden
	 *
	 */
	public boolean getHideOpcode() {
		return hideOpcode;
	}

	/**
	 * Sets the hide opcode flag
	 * @param flag whether or not to hide opcodes
	 */
	public void setHideOpcode(final boolean flag) {
		hideOpcode = flag;
	}

	/**
	 * Gets the flag indicating whether to parse command sequence directives.
	 *
	 * @return true if sequence directives should be parsed; false if not
	 * 
	 */
	public boolean getParseSequenceDirectives() {
	    return Boolean.valueOf(getProperty(
	            PARSE_SEQ_DIRECTIVES_PROPERTY, "false"));
	}

	/**
	 * Sets the flag indicating whether to parse command sequence directives.
	 *
	 * @param parseSeqDirs
	 *            true if sequence directives should be parsed; false if not
	 * 
	 */
	public void setParseSequenceDirectives(final boolean parseSeqDirs) {
	    this.properties.setProperty(PARSE_SEQ_DIRECTIVES_PROPERTY,
	            String.valueOf(parseSeqDirs));
	}

	/**
     * Gets the configured multimission schema version for the given 
     * dictionary type.
     * 
     * @param dictType
     *            the DictionaryType indicator
     * @return version string
     * 
     */
    public static String getMultimissionDictionaryVersion(final DictionaryType dictType) {
        return new DictionaryProperties(false).getProperty(
                getDictionaryVersionProperty(dictType),
                IBaseDictionary.UNKNOWN);
    }
    
	/**
	 * Gets the configured parser class type for parsing the
	 * given flight dictionary type.
	 * 
	 * @param dictType
	 *            the DictionaryType indicator
	 * @return parser class type container
	 * 
	 */
	public DictionaryClassContainer getDictionaryClass(final DictionaryType dictType) {
	    final String tempStr = getProperty(
	            getDictionaryAdaptorClassProperty(dictType, true),
	            dictType.getDefaultParserClass().name());
	    DictionaryClassContainer result = null;
	    try {
	        result = new DictionaryClassContainer(DictionaryClassContainer.ClassType.valueOf(tempStr));
	    } catch (final IllegalArgumentException e) {
	        result = new DictionaryClassContainer(tempStr);
	    }
	    return result;
	}

	/**
	 * Sets the configured parser class type for parsing the
	 * given flight dictionary type.
	 * 
	 * @param dictType
	 *            the DictionaryType indicator
	 * @param classContainer
	 *            class container of the type and/or class name to set
	 * 
	 */
	public void setDictionaryClass(final DictionaryType dictType, final DictionaryClassContainer classContainer) {
	    this.properties.setProperty(
	            getDictionaryAdaptorClassProperty(dictType, true), 
	                    classContainer.getClassType() == ClassType.CUSTOM ? 
	                    classContainer.getCustomClassName() : classContainer.getClassType().name());
	}
	
	/**
	 * Gets the configured parser class type for parsing the
	 * given SSE/GSE dictionary type.
	 * 
	 * @param dictType
	 *            the DictionaryType indicator
	 * @return parser type container
	 * 
	 */
	public DictionaryClassContainer getSseDictionaryClass(final DictionaryType dictType) {
	    final String tempStr = getProperty(
                getDictionaryAdaptorClassProperty(dictType, false),
                dictType.getDefaultParserClass().name());
        DictionaryClassContainer result = null;
        try {
            result = new DictionaryClassContainer(DictionaryClassContainer.ClassType.valueOf(tempStr));
        } catch (final IllegalArgumentException e) {
            result = new DictionaryClassContainer(tempStr);
        }
        
        return result;
	}

	/**
	 * Sets the configured parser class type for parsing the
	 * given SSE/GSE dictionary type.
	 * 
	 * @param dictType
	 *            the DictionaryType indicator
	 * @param classContainer the class container object identifying
	 *            the dictionary parser class
	 * 
	 */
	public void setSseDictionaryClass(final DictionaryType dictType, final DictionaryClassContainer classContainer) {
	    this.properties.setProperty(
                getDictionaryAdaptorClassProperty(dictType, false), 
                        classContainer.getClassType() == ClassType.CUSTOM ? 
                        classContainer.getCustomClassName() : classContainer.getClassType().name());
	}

	/**
	 * Gets the configured search path for the given flight dictionary type.
	 * 
	 * @param dictType
	 *            the DictionaryType indicator
	 * @return dictionary search path: CONFIG_PATH, DICTIONARY_PATH, or a
	 *         directory name
	 * 
	 */
	public String getDictionarySearchPath(final DictionaryType dictType) {
	    return getProperty(
	            getDictionarySearchProperty(dictType, true),
	            dictType.getDefaultSearchPath());
	}


	/**
	 * Gets the schema path for a supplied DictionaryType
	 *
	 * @param dictType the DictionaryType to find the schema for
	 *
	 * @return path to the schema for the supplied DictionaryType, if it exists
	 */
	public String getSchemaLocationFromDictionaryType(DictionaryType dictType) {
		final String searchPath = getSchemaSearchPath();
		final String schemaName = getDictionarySchemaName(dictType);

		log.debug("Looking up schema for ", dictType.getDictionaryName(), " @ ",
		          schemaName, " in ", searchPath);

		try {
			File f = new File(searchPath);
			if (f.exists() && f.isDirectory()) {
				String schemaFilePath = searchPath + File.separator + schemaName;
				File schema = new File(schemaFilePath);

				if (schema.exists()) {
					return schema.getAbsolutePath();
				}
			}
		} catch(Exception e) {
			log.error(Markers.DICT, "Unexpected exception ", ExceptionTools.getMessage(e),
			          " loading schema from ", searchPath);
		}
		return "";
	}



	/**
	 * Gets the configured dictionary schema directory path for validation.
	 * Defaults to $CHILL_GDS/schema/dictionary and can be overrode with user specified directory
	 *
	 * @return the configured dictionary schema directory location
	 */
	public String getSchemaSearchPath() {
		String schemaDir = getProperty(SCHEMA_VALIDATION_DIR_PROPERTY,
		                               DEFAULT_SCHEMA_VALIDATION_LOC);

		return !schemaDir.equals(DEFAULT_SCHEMA_VALIDATION_LOC)
				? schemaDir : GdsSystemProperties.getGdsDirectory()
				+ File.separator + DEFAULT_SCHEMA_VALIDATION_LOC
				+ File.separator + DICTIONARY_PATH;
	}

	/**
	 * Gets the configured schema name for a DictionaryType
	 *
	 * @param dictionaryType the DictionaryType to get a schema for
	 *
	 * @return the schema name for the supplied DictionaryType, if it exists
	 */
	public String getDictionarySchemaName(DictionaryType dictionaryType) {
		return getProperty(SCHEMA_VALIDATION_PROPERTY
				                   + "." + dictionaryType.getDictionaryName() + ".name",
		                   dictionaryType.getDefaultSchemaName());
	}

	/**
	 * Gets the configuration for whether or not dictionary schema validation is enabled
	 *
	 * @return true if we should try and validate dictionary schemas; false otherwise
	 */
	public boolean isSchemaValidationEnabled() {
		return getBooleanProperty(SCHEMA_VALIDATION_PROPERTY, true);
	}


	/**
	 * Gets the configured search path for the given SSE/GSE dictionary type.
	 * 
	 * @param dictType
	 *            the DictionaryType indicator
	 * @return dictionary search path: CONFIG_PATH, DICTIONARY_PATH, or a
	 *         directory name
	 * 
	 */
	public String getSseDictionarySearchPath(final DictionaryType dictType) {
	    return getProperty(
	            getDictionarySearchProperty(dictType, false),
	            dictType.getDefaultSearchPath());
	}

	/**
	 * Gets the configured base file name, without path, for the given flight
	 * dictionary type.
	 * 
	 * @param dictType
	 *            the DictionaryType indicator
	 * @return dictionary file name
	 * 
	 */
	public String getDictionaryFileName(final DictionaryType dictType) {
	    return getProperty(
	            getDictionaryFileProperty(dictType, true),
	            dictType.getDefaultFileName());
	}

	/**
	 * Gets the configured base file name, without path, for the given SSE/GSE
	 * dictionary type.
	 * 
	 * @param dictType
	 *            the DictionaryType indicator
	 * @return dictionary file name
	 * 
	 */
	public String getSseDictionaryFileName(final DictionaryType dictType) {
	    return getProperty(
	            getDictionaryFileProperty(dictType, false),
	            dictType.getDefaultFileName());
	}

	/**
	 * Sets the configured search path for the given flight dictionary type.
	 * 
	 * @param dictType
	 *            the DictionaryType indicator
	 * @param path
	 *            CONFIG_PATH, DICTIONARY_PATH, or a directory name
	 * 
	 */
	public void setDictionarySearchPath(final DictionaryType dictType, final String path) {
	    this.properties.setProperty(
	            getDictionarySearchProperty(dictType, true), path);
	}

	/**
	 * Sets the configured search path for the given SSE/GSE dictionary type.
	 * 
	 * @param dictType
	 *            the DictionaryType indicator
	 * @param path
	 *            CONFIG_PATH, DICTIONARY_PATH, or a directory name
	 * 
	 */
	public void setSseDictionarySearchPath(final DictionaryType dictType, final String path) {
	    this.properties.setProperty(
	            getDictionarySearchProperty(dictType, false), path);
	}

	/**
	 * Sets the configured base file name, without path, for the given flight
	 * dictionary type.
	 * 
	 * @param dictType
	 *            the DictionaryType indicator
	 * @param file
	 *            dictionary file name to set
	 * 
	 */
	public void setDictionaryFileName(final DictionaryType dictType, final String file) {
	    this.properties.setProperty(getDictionaryFileProperty(dictType, true),
	            file);
	}

	/**
	 * Sets the configured base file name, without path, for the given SSE/GSE
	 * dictionary type.
	 * 
	 * @param dictType
	 *            the DictionaryType indicator
	 * @param file
	 *            dictionary file name to set
	 * 
	 */
	public void setSseDictionaryFileName(final DictionaryType dictType, final String file) {
	    this.properties.setProperty(getDictionaryFileProperty(dictType, false),
	            file);
	}
	
	/**
	 * Gets the assumed downlink transfer frame format. Used only with the "old"
	 * transfer frame parser class.
	 * 
	 * @return FrameFormat.TypeName; defaults to "CCSDS_AOS_2_MPDU".
	 * 
	 */
	public IFrameFormatDefinition.TypeName getAssumedFrameFormat() {
	    final String strVal = getProperty(ASSUMED_FRAME_FORMAT_PROPERTY, 
	            IFrameFormatDefinition.TypeName.CCSDS_AOS_2_MPDU.toString());
	    try {
	        return IFrameFormatDefinition.TypeName.valueOf(strVal);
	    } catch (final IllegalArgumentException e) {
	        log.error("Assumed frame format in " + getBaseFilename() + " is invalid: " + strVal + 
	                ". Assuming CCSDS_AOS_2_MPDU");
	    }
	    return IFrameFormatDefinition.TypeName.CCSDS_AOS_2_MPDU;
	}
	
	/**
	 * Gets the alarm dictionary reload interval in seconds.
	 * 
	 * @return interval in seconds
	 */
	public int getAlarmReloadInterval() {
	    return getIntProperty(ALARM_RELOAD_PROPERTY, ALARM_RELOAD_DEFAULT);
	}

	/**
	 * Copies the given instance to create a new instance of DictionaryConfiguration.
	 *  
	 * @param instanceToClone DictionaryConfiguration to copy
	 * 
	 * @return new DictionaryConfiguration
	 */
	public static DictionaryProperties copy(final DictionaryProperties instanceToClone) {
	    final DictionaryProperties newInstance = new DictionaryProperties(false);
	    newInstance.setFswDictionaryDir(instanceToClone.getFswDictionaryDir());
	    newInstance.setSseDictionaryDir(instanceToClone.getSseDictionaryDir());
	    newInstance.setFswVersion(instanceToClone.getFswVersion());
	    newInstance.setSseVersion(instanceToClone.getSseVersion());
	    newInstance.properties.putAll(instanceToClone.properties);
	    return newInstance;
	}

	@Override
	public String getPropertyPrefix() {
	    return PROPERTY_PREFIX;
	}

    /**
     * Load all jar files from the dictionary/lib location. This is another
     * location that users can use to get things on the classpath. We have to do
     * this for users to be able to add derived EHA and DPO algorithms that are
     * dictionary dependent to the classpath. We have to add to the classpath
     * dynamically because we don't know the dictionary directory until runtime.
     * If the jar files exist, they will be put on the classpath
     * 
     * Modified to use a "secure loader" that interacts
     * with an AMPCS security policy. This is designed to load JARs provided by
     * missions into a context which restricts actions that can be performed.
     * 
     * @param silent
     *            if true, no messages will be printed to STDOUT. If false,
     *            messages will print to STDOUT as JARs are loaded. Errors will
     *            still print to STDERR regardless.
     * @param classLoader
     *            the class loader to use.
     * @param log
     *            the Tracer to log with
     */
    public void loadDictionaryJarFiles(final boolean silent, final AmpcsUriPluginClassLoader classLoader,
                                       final Tracer log) {
        final String jarSubDir = getProperty(DICTIONARY_LIB_DIR_PROPERTY, DEFAULT_LIB_DIR);
    
        /*
         * Normally, I would replace the getMission()
         * call below with GdsSystemProperties.getSystemMissionIncludingSse()
         * to mimic the old call, but I believe that in this case we really do
         * not want an SSE mission name.
         */
        String dictLibDir = getFswDictionaryDir() + File.separator
                + GdsSystemProperties.getSystemMission() + File.separator
                + getFswVersion() + File.separator + jarSubDir;
    
        File userLib = new File(dictLibDir);
        if (!userLib.exists()) {
            // this is not an error, it just means the dictionary is supplying
            // no libraries
            return;
        }
    
        try {
            // Only print to STDOUT if silent is false
            if (!silent) {
                log.info(Markers.DICT, ADDING, dictLibDir, TO_CLASSPATH);
            } else {
                log.debug(Markers.DICT, ADDING, dictLibDir, TO_CLASSPATH);
            }
            
            classLoader.addDirectory(userLib);
        } catch (final Exception e1) {
            log.warn("Could not load dictionary lib directory "
                    + userLib.getAbsolutePath()
                    + " to the classpath due to an error: " + ExceptionTools.getMessage(e1), e1);
        }
    
        dictLibDir = getSseDictionaryDir() + File.separator
                + GdsSystemProperties.getSseNameForSystemMission() + File.separator
                + getSseVersion() + File.separator
                + jarSubDir;
    
        userLib = new File(dictLibDir);
        if (!userLib.exists()) {
            // this is not an error, it just means the dictionary is
            // supplying no libraries
            return;
        }
    
        try {
            //  Only print to STDOUT if silent is false
            if (!silent) {
                log.info(Markers.DICT, ADDING, dictLibDir, TO_CLASSPATH);
            } else {
                log.debug(Markers.DICT, ADDING, dictLibDir, TO_CLASSPATH);
            }
            classLoader.addDirectory(userLib);
        } catch (final Exception e1) {
            log.warn("Could not load SSE dictionary lib directory "
                    + userLib.getAbsolutePath()
                    + " to the classpath due to an error: "
                    + ExceptionTools.getMessage(e1), e1);
        }
    }

	/**
	 * Gets the property indicating whether output channel values should be formatted
	 * according to the dictionary.
	 * 
	 * @return true to use formatters from dictionary, false if not
	 */
	public boolean useChannelFormatters() {
	    return getBooleanProperty(ENABLE_CHANNEL_FORMATTING_PROPERTY, false);
	}

	/**
	 * Convenience method to construct the dictionary adaptor class property
	 * name for a specific dictionary type.
	 * 
	 * @param dictType
	 *            DictionaryType indicator
	 * @param forFlight
	 *            true if for flight, false if for GSE/SSE
	 * @return property name
	 */
	private String getDictionaryAdaptorClassProperty(final DictionaryType dictType,
	        final boolean forFlight) {
	    return PROPERTY_PREFIX + dictType.getDictionaryName() + "."
	            + (forFlight ? FLIGHT : SIM) + DICT_ADAPTOR;
	}

	/**
     * Convenience method to multimission version property name for a
     * specific dictionary type.
     * 
     * @param dictType
     *            DictionaryType indicator
     * @return property name
     * 
     */
    private static String getDictionaryVersionProperty(final DictionaryType dictType) {
        return INTERNAL_PROPERTY_BLOCK + dictType.getDictionaryName() + "."
                + MM_VERSION;
    }

	/**
	 * Convenience method to construct the search path property name for a
	 * specific dictionary type.
	 * 
	 * @param dictType
	 *            DictionaryType indicator
	 * @param forFlight
	 *            true if for flight, false if for GSE/SSE
	 * @return property name
	 */
	private String getDictionarySearchProperty(final DictionaryType dictType,
	        final boolean forFlight) {
	    return PROPERTY_PREFIX + dictType.getDictionaryName() + "."
	            + (forFlight ? FLIGHT : SIM) + SEARCH_PATH;
	}

	/**
	 * Convenience method to construct the filename property name for a specific
	 * dictionary type.
	 * 
	 * @param dictType
	 *            DictionaryType indicator
	 * @param forFlight
	 *            true if for flight, false if for GSE/SSE
	 * @return property name
	 */
	private String getDictionaryFileProperty(final DictionaryType dictType,
	        final boolean forFlight) {
	    return PROPERTY_PREFIX + dictType.getDictionaryName() + "."
	            + (forFlight ? FLIGHT : SIM) + FILE_NAME;
	}

	/**
	 * DictDirOnlyFilter is a file filter that accepts potential dictionary
	 * directories only. The CVS and "products" directory are filtered out.
	 */
	private class DictDirOnlyFilter implements FileFilter {

		/**
		 * {@inheritDoc}
		 * @see java.io.FileFilter#accept(java.io.File)
		 */
		@Override
		public boolean accept(final File arg0) {
			if (!arg0.isDirectory()) {
				return false;
			}
			final String prodDir = getDictionaryFileName(DictionaryType.PRODUCT);
			return !(arg0.getName().equals(".") || arg0.getName().equals("..")
					|| arg0.getName().equals("CVS")
					|| arg0.getName().equals(".svn")
					|| arg0.getName().equals(prodDir));
		}
	}


	@Override
	public void setTemplateContext(final Map<String, Object> map) {
		 /* 
         *  Dictionary members replaced with DictionaryConfiguration object.
         */
        if (getFswVersion() != null) {
            map.put("fswVersion", getFswVersion());
        } 

        if (getSseVersion() != null) {
            map.put("sseVersion", getSseVersion());
        } 
        
        if (getFswDictionaryDir() != null) {
            map.put("fswDictionaryDir",
                    new File(getFswDictionaryDir()).getAbsolutePath());
        } 

        if (getSseDictionaryDir() != null) {
            map.put("sseDictionaryDir",
                    new File(getSseDictionaryDir()).getAbsolutePath());
        } 
		
	}

	@Override
	public ISerializableMetadata getMetadataHeader() {
	    if (this.header == null || this.dirty) {
	        header = new MetadataMap();

	        header.setValue(MetadataKey.FSW_DICTIONARY_DIR,
	                getFswDictionaryDir());
	        header.setValue(MetadataKey.FSW_DICTIONARY_VERSION,
	                getFswVersion());
	        header.setValue(MetadataKey.SSE_DICTIONARY_DIR,
	                getSseDictionaryDir());
	        header.setValue(MetadataKey.SSE_DICTIONARY_VERSION,
	                getSseVersion());
	        this.dirty = false;
	    }
		
		return header;
	}
	
	/**
	 * Indicates if the dictionary directories or versions have changed since the last time
	 * the metadata header was fetched.
	 * 
	 * @return true if data has changed, false if not
	 */
	public boolean isDirty() {
	    return this.dirty;
	}

    /**
     * Generates dictionary-related XML elements.
     * @param writer the XML stream to write to
     * @param includeSse true to include SSE dictionary information
     * @throws XMLStreamException if there is a problem generating the XML
     */
    public void generateStaxXml(final XMLStreamWriter writer, final boolean includeSse) throws XMLStreamException {
        writer.writeStartElement("DictionaryInformation");
        
        if (getFswDictionaryDir() != null) {
            XmlUtility.writeSimpleCDataElement(writer,
                    "FswDictionaryDirectory",
                    new File(getFswDictionaryDir())
                            .getAbsolutePath());
        }
        
        if (getFswVersion() != null) {
            XmlUtility.writeSimpleElement(writer, "FswVersion", getFswVersion());
        }
        
        if (getSseDictionaryDir() != null && includeSse) {
            XmlUtility.writeSimpleCDataElement(writer,
                    "SseDictionaryDirectory",
                    new File(getSseDictionaryDir())
                            .getAbsolutePath());
        }
        
        if (getSseVersion() != null && includeSse) {
            XmlUtility.writeSimpleElement(writer, "SseVersion", getSseVersion());
        }
        
        writer.writeEndElement();
        
    }

	/**
	 * Returns the dictionary cache eviction timeout, in hours.
	 * @return eviction timeout in hours
	 */
	public int getCacheEvictionTimeout() {
		return getIntProperty(CACHE_EVICTION_TIMEOUT_PROPERTY, CACHE_EVICTION_TIMEOUT_DEFAULT);
	}

    /**
     * Used to access the SSE Context Flag within DictionaryProperties
     * 
     * @return true if SSE is enabled; false otherwise
     */
    public boolean isDictionarySseEnabled() {
        return sseFlag;
    }

}
