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
package jpl.gds.station.api.dsn.chdo;

import java.util.Map;

/**
 * 
 * An interface to be implemented by CHDO configuration classes.
 *
 */
public interface IChdoConfiguration {
    
    /**
     * Returns the default file name for CHDO configurations.
     * 
     * @return file name, no path
     */
    public static String getDefaultConfigFilename() {
        return "chdo.xml";
    }
    
    /**
     * Parses the CHDO config file from the given location on the file system and populates
     * the IChdoConfiguration object. 
     * 
     * @param filename the complete file path to the project dictionary file to parse; may
     * be null; the implementation must check for this condition and throw
     * 
     * @throws ChdoConfigurationException; if the file cannot be located or cannot be
     * successfully parsed
     * 
     */
    public void parse(String filename) throws ChdoConfigurationException;
    
    /**
     * Removes/clears all information parsed from the configuration file from the IChdoConfiguration 
     * object, making it ready to re-parse the definition file(s). Must also reset any static
     * variables that may affect a clean restart of the configuration.
     * 
     */
    public void clear();
    
    /**
     * Returns the version of the CHDO configuration.  
     * 
     * @return the version String, or "unknown" if no version is defined
     * 
     */
    public String getVersion();
	

	/**
	 * Gets a CHDO definition by its numeric type.
	 * 
	 * @param type CHDO type number
	 * @return IChdoDefinition object if found; null if not
	 */
	public IChdoDefinition getDefinitionByType(int type);


	/**
	 * Retrieves the CHDO typeToDefinition map.
	 * 
	 * @return Map of CHDO type to IChdoDefinition object
	 */
	public Map<Integer, IChdoDefinition> getTypeToDefinitionMap();

	/**
	 * Gets the list of defined control authority IDs (CAIDs)
	 * 
	 * @return array of CAID strings
	 */
	public String[] getControlAuthorityIds();
	
	/**
	 * Gets the IChdoProperty object from its property name.
	 * 
	 * @param propertyName ChdoProperty name
	 * @return IChdoProperty
	 */
	public IChdoProperty getPropertyByName(String propertyName);
	
	/**
	 * Gets the entire NameToPropertyMap for IChdoProperties.
	 * 
	 * @return nameToPropertyMap
	 */
	public Map<String, IChdoProperty> getNameToPropertyMap();
	
	/**
	 * Gets an XML representation of the CHDO configuration.
	 * 
	 * @return XML string
	 * @throws ChdoConfigurationException if there is a problem creating the XML from the configuration
	 */
	public String returnXmlString() throws ChdoConfigurationException;
	
}