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
package jpl.gds.dictionary.api;

import java.util.List;

import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.config.DictionaryType;
import jpl.gds.shared.log.Tracer;

/**
 * The IBaseDictionary interface is to be extended by all dictionary adaptation
 * interfaces. It includes basic parse() and clear() methods, for populating the
 * dictionary contents and clearing them, respectively, as well as methods to
 * get the mission, spacecraft, dictionary versions, and schema versions. It is
 * assumed that the results of the parse method are stored in the dictionary
 * object itself. <br>
 * <p>
 * <b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b>
 * <p>
 * <p>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b>
 * <p>
 * 
 *
 */
public interface IBaseDictionary extends ICacheableDictionary {
    
    /**
     * String representing an unknown mission, XML, or schema version.
     * 
     */
    public static final String UNKNOWN = "unknown";
    
    /**
     * Constant representing an unknown spacecraft ID.
     * 
     */
    public static final int UNKNOWN_SCID = -1;
    
    /**
     * Parses the dictionary file from the given location on the file system,
     * using the parser specified in the GLOBAL DictionaryConfiguration, and
     * populates the dictionary object. The only exception that should be thrown
     * by this method is DictionaryException. This method must catch all other
     * exceptions and re-throw them as DictionaryException.
     * 
     * @param filename
     *            the complete file path to the project dictionary file to
     *            parse; may be null; the implementation must check for this
     *            condition and throw
     * 
     * @throws DictionaryException
     *             if the dictionary file cannot be located or cannot be
     *             successfully parsed
     */
    public void parse(String filename) throws DictionaryException;
    
    /**
     * Parses the dictionary file from the given location on the file system,
     * using the parser specified in the given DictionaryConfiguration, and
     * populates the dictionary object. The only exception that should be thrown
     * by this method is DictionaryException. This method must catch all other
     * exceptions and re-throw them as DictionaryException.
     * 
     * @param filename
     *            the complete file path to the project dictionary file to
     *            parse; may be null; the implementation must check for this
     *            condition and throw
     * @param config
     *            DictionaryConfiguration to use
     * @throws DictionaryException
     *             if the dictionary file cannot be located or cannot be
     *             successfully parsed
     */
    public void parse(String filename, DictionaryProperties config) throws DictionaryException;

    /**
     * Parses the dictionary file from the given location on the file system,
     * using the parser specified in the given DictionaryConfiguration, and
     * populates the dictionary object. The only exception that should be thrown
     * by this method is DictionaryException. This method must catch all other
     * exceptions and re-throw them as DictionaryException.
     * 
     * @param filename
     *            the complete file path to the project dictionary file to
     *            parse; may be null; the implementation must check for this
     *            condition and throw
     * @param config
     *            DictionaryConfiguration to use
     * @param tracer
     *            The Tracer with an application context to log with
     * @throws DictionaryException
     *             if the dictionary file cannot be located or cannot be
     *             successfully parsed
     */
    public void parse(String filename, DictionaryProperties config, final Tracer tracer) throws DictionaryException;

    /**
     * Removes/clears all information parsed from the dictionary file from the Dictionary
     * object, making it ready to re-parse the definition file(s). Must also reset any static
     * variables that may affect a clean restart of the dictionary.
     */
    public void clear();
    
    /**
     * Retrieves the type of this dictionary
     * 
     * @return DictionaryType, never null
     * 
     */
    public DictionaryType getDictionaryType();
    
    /**
     * Sets the current dictionary type. Should only be used to override
     * a general dictionary type (CHANNEL) to a more specific one (e.g. HEADER).
     * 
     * @param type the DictionaryType to set
     */
    public void setDictionaryType(DictionaryType type);
    
    /**
     * Retrieves the DictionaryConfiguration object used by for constructing 
     * this dictionary.
     * 
     * @return DictionaryConfiguration, never null
     * 
     */
    public DictionaryProperties getDictionaryConfiguration();
    
    /**
     * Sets the DictionaryConfiguration object used by for constructing 
     * this dictionary.  Only takes effect upon the next parse.
     * 
     * @param dictConfig DictionaryConfiguration to set, never null
     * 
     */
    public void setDictionaryConfiguration(DictionaryProperties dictConfig);
    
    /**
     * Returns the GDS version of the dictionary.  From the AMPCS perspective,
     * this is the name of the dictionary version directory.
     * 
     * @return the dictionary version String, or the UNKNOWN constant if no version defined
     * 
     */
    public String getGdsVersionId();
    
    /**
     * Returns the FSW or SSE build ID. This is an ID used by the mission to map
     * dictionaries to specific flight or SSE software builds. There is no standard
     * format and this is not a required dictionary field.
     * 
     * @return the build version ID string, may be null
     * 
     *
     */
    public String getBuildVersionId();
       
    /**
     * Returns the FSW or SSE release version ID. This is an ID used by the
     * mission to map GDS dictionary versions to specific flight or SSE
     * dictionary versions. There is no standard format and this is not a
     * required dictionary field.
     * 
     * @return the release version ID string
     * 
     *
     */
    public String getReleaseVersionId();
    
    /**
     * Returns a list of spacecraft IDs from the dictionary file, or UNKNOWN_SCID.
     * 
     * @return List of spacecraft IDs, or List containing UNKNOWN_SCID if none defined
     */
    public List<Integer> getSpacecraftIds();
    
    /**
     * Returns the latest implemented schema version from the dictionary parser.
     * An XML file with a later version than this version may produce a warning
     * during parsing, but the warning does not necessarily imply the dictionary
     * could not be parsed.
     * 
     * @return latest implemented schema version, or UNKNOWN if none defined
     */
    public String getImplementedSchemaVersion();
    
    /**
     * Returns the actual schema version from the dictionary XML.
     * 
     * @return actual schema version, or UNKNOWN if none defined
     */
    public String getActualSchemaVersion();
    
    /**
     * Gets the name of the mission/project from the dictionary XML.
     * 
     * @return mission (usually the mission mnemonic), or UNKNOWN if none defined
     */
    public String getMission();

}
