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
package jpl.gds.context.api;

import java.io.File;
import java.io.IOException;

import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.connection.IConnectionMap;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.common.config.security.AccessControlParameters;
import jpl.gds.context.api.filtering.IFilterableDataItem;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.metadata.IMetadataHeaderProvider;
import jpl.gds.shared.string.StringUtil;
import jpl.gds.shared.xml.validation.XmlValidationException;
import jpl.gds.shared.xml.validation.XmlValidator;
import jpl.gds.shared.xml.validation.XmlValidatorFactory;
import jpl.gds.shared.xml.validation.XmlValidatorFactory.SchemaType;

/**
 * Top-level interface to be implemented by context configuration classes.
 * 
 *
 * @since R8
 */
public interface IContextConfiguration extends ISimpleContextConfiguration, IMetadataHeaderProvider {

	/**
	 * Gets the connection map object used by this context configuration to
	 * store uplink and downlink connection configuration.
	 * 
	 * @return ConnectionConfiguration
	 */
	public abstract IConnectionMap getConnectionConfiguration();

	/**
	 * Gets the MissionProperties configuration object used when creating this context
	 * configuration.
	 * 
	 * @return MissionProperties
	 */
	public abstract MissionProperties getMissionProperties();

	/**
	 * Gets the AccessControlParameters object used by this context configuration to
	 * store security parameters.
	 * 
	 * @return AccessControlParameters object; never null
	 * 
	 */
	public abstract AccessControlParameters getAccessControlParameters();

	/**
	 * Gets the dictionary configuration object for this context configuration.
	 * 
	 * @return DictionaryConfiguration object
	 */
	public abstract DictionaryProperties getDictionaryConfig();

	/**
	 * Gets the venue configuration object used by this context configuration to 
	 * store venue configuration information.
	 * 
	 * @return IVenueConfiguration object
	 */
	public abstract IVenueConfiguration getVenueConfiguration();

	/**
	 * Copies members into this context configuration from the given context configuration
	 * object. This is a shallow copy of the properties members and a deep copy of the
	 * other members, except for the application context itself, which is not copied.
	 * 
	 * @param tc
	 *            the IContextConfiguration to get values from
	 */
	public abstract void copyValuesFrom(IContextConfiguration tc);

	/**
	 * Clears fields that should not be inherited by a new context.
	 * 
	 */
	public abstract void clearFieldsForNewConfiguration();

	/**
	 * Saves an XML version of the current context configuration to a file.
	 * 
	 * @param filename
	 *            the pathname of the output file
	 * @throws IOException
	 *             thrown if encountered
	 */
	public abstract void save(String filename) throws IOException;

	/**
	 * Saves an XML version of the current context configuration to the config 
	 * filename stored in this object.
	 * 
	 * @throws IOException
	 *             thrown if encountered
	 */
	public abstract void save() throws IOException;

	/**
	 * Loads this object instance from an XML file.
	 * 
	 * @param filename
	 *            the pathname of the input file
	 * @return true if the load succeeded; false otherwise.
	 */
	public abstract boolean load(String filename);
	
	/**
	 * Get the configuration file to which the context configuration will be saved.
	 * 
	 * @return config file path
	 */
	public String getConfigFile();
		
	/**
	 * Sets the configuration file to which the context configuration will be saved.
	 * 
	 * @param filepath the file path to set
	 */
	public void setConfigFile(String filepath);
	
	/**
	 * Gets the application context associated with this configuration,
	 * if any.
	 * 
	 * @return ApplicationContext; may be null if the object was not created
	 *         from context
	 */
	public ApplicationContext getApplicationContext();
	
	  /**
     * Produces an XML version of this configuration that is
     * pretty-printed.
     * @return XML string
     */
    public String toPrettyXml();
    
    /**
     * Validate that the configFile conforms to Schema definition
     * @param schemaFile schema file to check XML against
     * @param configFile to be checked
     * @return true if XML is valid, false otherwise
     * @throws XmlValidationException thrown if there is a problem validating 
     *         the XML file
     */
    public static boolean schemaVsConfigFileCheck(final File schemaFile, final File configFile) throws XmlValidationException {        
          final XmlValidator validator = XmlValidatorFactory.createValidator(SchemaType.RNC);
          return(validator.validateXml(schemaFile, configFile));
   
    }
    
    /**
     * Reset the context type to the application type. Used when the context is set
     * from the database by external processes.
     *
     * @param sc context configuration
     */
    public static void resetTypeToApplication(final IContextConfiguration sc)
    {
        if (sc == null)
        {
            return;
        }

        final IContextIdentification si = sc.getContextId();

        if (si == null)
        {
            return;
        }

        si.setType(
                StringUtil.emptyAsNull(ApplicationConfiguration.getApplicationName()));
    }

    /**
     * Method to access the SSE context flag from an IContextConfiguration
     * 
     * @return Whether or not the application is SSE
     */
    public boolean getSseContextFlag();

	
}