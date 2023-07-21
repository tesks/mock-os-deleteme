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

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.apache.commons.configuration2.CombinedConfiguration;
import org.apache.commons.configuration2.XMLConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.configuration2.tree.MergeCombiner;
import org.apache.commons.configuration2.tree.NodeCombiner;

import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.xml.validation.XmlValidationException;
import jpl.gds.shared.xml.validation.XmlValidator;
import jpl.gds.shared.xml.validation.XmlValidatorFactory;
import jpl.gds.shared.xml.validation.XmlValidatorFactory.SchemaType;

/**
 * This class is meant to be used as a super class for
 * config files that a) should be loaded hierarchically
 * according to the nominal AMPCS mechanism and
 * b) are XML config files that can be validated and
 * can be manipulated effectively using the HierarchicalConfiguration
 * interface from the Apache Commons Configuration library.
 * 
 * Using this class is encouraged in such cases so that
 * custom XML parsing and handling can be avoided for every new
 * XML configuration file type that may be added to AMPCS
 * in the future.
 *
 */
public class GdsHierarchicalXml implements IGdsConfiguration {

	private final NodeCombiner combiner = new MergeCombiner();
	
	/**
	 * This field can be used by subclasses to get configuration properties.
	 * It represents the effective configuration resulting from loading and merging
	 * all levels of config files.
	 */
	protected CombinedConfiguration cc = new CombinedConfiguration(combiner);
	
	/** The mutableConfig field is meant to be utilized by subclasses that may
	 *  need to provide setter methods for configuration properties. Properties
	 *  cannot be set on the combined configuration, but setting them on this
	 *  field will result in changes when getting properties from the combined configuration.
	 */
	protected XMLConfiguration mutableConfig = new XMLConfiguration();

	private final XmlValidator validator = XmlValidatorFactory.createValidator(SchemaType.RNC);

	private final List<String> configDirs = new ArrayList<>();

    private final String baseXmlFilename;
    
    /** Properties from override file */
    protected final Properties propertyOverrides = new Properties();
    
    private List<String> whitelistedDirectories;
    
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

    private final Tracer            log                              = TraceManager.getTracer(Loggers.CONFIG);
    
    private final boolean           sseFlag;

	/**
     * Create a GdsHierarchicalXml instance. This constructor attempts to load
     * configuration files.
     * 
     * @param fileName
     *            - the filename (no directory) to look for in each directory
     *            searched.
     * @param schemaName
     *            - the filename of the schema to validate against.
     * @param required
     *            - if true, an IllegalStateException is thrown if no files are
     *            found
     * 
     * @param sseFlag
     *            The SSE context flag
     * 
     * 07/26/17 -updated directories checked for
     *          loading
     * 08/01/17 - Updated to use the getFullConfigPathList,
     *          which potentially uses either the fixed or flex paths instead of
     *          being a fixed list that is created in the method
     */
    public GdsHierarchicalXml(final String fileName, final String schemaName, final boolean required,
            final SseContextFlag sseFlag) {
        this.baseXmlFilename = fileName;
        this.sseFlag = sseFlag.isApplicationSse();

        configDirs.addAll(GdsSystemProperties.getFullConfigPathList(this.sseFlag));
		Collections.reverse(configDirs);
		
		this.loadOverrideProperties(log);
		final boolean loadUserProps = Boolean.parseBoolean(this.propertyOverrides.getProperty(USER_CONFIG_OVERRIDE, "true"));

		cc.addConfiguration(mutableConfig);
		boolean noFilesLoaded = true;

		for (final String dir : configDirs) {
		    
		    final boolean whitelisted = isDirWhitelisted(dir);
            if(!loadUserProps && !whitelisted){
                continue;
            }
		    
			final String path = String.format("%s%s%s", dir, File.separator, fileName);
            final String schema = String.format("%s%s%s", GdsSystemProperties.getSchemaDirectory(), File.separator, schemaName);
        	if (new File(path).exists()) {
				try {
					final boolean valid = validator.validateXml(schema, path);
					if (!valid) {
						// The RNC validator we currently use (Jing) is unreliable at times, so this log
						// is left at INFO level and we attempt to load the file anyways.
						log.warn("Could not validate XML file: " + path + " using schema " + schema);
					}
				} catch (final XmlValidationException e1) {
					log.warn("Could not validate XML file: " + path + " using schema " + schema, e1);
				}
				final Parameters params = new Parameters();
				final FileBasedConfigurationBuilder<XMLConfiguration> builder =
			    	new FileBasedConfigurationBuilder<XMLConfiguration>(XMLConfiguration.class)
			    	.configure(params.xml()
			    			.setEncoding("UTF-8")
			    			.setFileName(path));

				try {
					cc.addConfiguration(builder.getConfiguration());
				} catch (final ConfigurationException e) {
					log.warn("Problem parsing configuration: " + path, e);
				}
				noFilesLoaded = false;
			}
		}
		if (noFilesLoaded && required) {
			final String errorMessage = String.format("Failed to load any %s configuration files.", fileName);
			log.error(errorMessage);
			throw new IllegalStateException(errorMessage);
		}
	}
	
	@Override
    public boolean supportsFlatProperties() {
	    return false;
	}
	
	@Override
    public String getPropertyPrefix() {
	    return "UNDEFINED";
	}
	
	/**
     * Get the configuration a flat Properties object.
     * @return properties
     */
    @Override
    public Properties asProperties() {
        final Properties result = new Properties();
        final Iterator<String> itr = cc.getKeys();
        while (itr.hasNext()) {
            final String nextKey = itr.next();
            result.setProperty(String.format("%s%s", getPropertyPrefix(), nextKey), cc.getString(nextKey));
        }
        return result;
    }

    @Override
    public String getBaseFilename() {
        return this.baseXmlFilename;
    }
    
    /**
     * Loads override properties
     */
    private void loadOverrideProperties(final Tracer log) {
        final String systemConfigDir = GdsSystemProperties.getSystemConfigDir();
        final String propertyOverridesPath = systemConfigDir + PROPERTY_OVERRIDES_FILENAME;
        final File propertyOverridesFile = new File(propertyOverridesPath);
        if(propertyOverridesFile.exists()) {
            try {
                this.propertyOverrides.load(new FileReader(propertyOverridesFile));
                log.debug("Loaded properties from " + propertyOverridesFile.getPath());
            } catch (final IOException e) {
                log.error("I/O error loading " + propertyOverridesFile.getPath());
            }
        }
    }
    
    /**
     * Get the list of directories that may contain properties files that are allowed to load internal properties.
     * By default this list will include the fixed configuration path directories along with any specified in the
     * override properties
     * @return the List of directories that can load internal properties
     */
    private List<String> getInternalWhitelistDirectories(){
        if(this.whitelistedDirectories == null){
            final String directories = this.propertyOverrides.getProperty(INTERNAL_PROPERTY_WHITELIST_DIRS);
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
    private boolean isDirWhitelisted(final String checkedDirectory){
        final List<String> checkDirs = getInternalWhitelistDirectories();
        
        for(final String dir : checkDirs){
            if(checkedDirectory.startsWith(dir)){
                return true;
            }
        }
        return false;
    }

}
