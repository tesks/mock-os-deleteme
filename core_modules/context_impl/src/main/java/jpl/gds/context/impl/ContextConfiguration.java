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
package jpl.gds.context.impl;

import jpl.gds.common.config.connection.ConnectionProperties;
import jpl.gds.common.config.connection.FourConnectionMap;
import jpl.gds.common.config.connection.IConnectionMap;
import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.common.config.security.AccessControlParameters;
import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.context.api.IVenueConfiguration;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.shared.config.ReleaseProperties;
import jpl.gds.shared.metadata.ISerializableMetadata;
import jpl.gds.shared.metadata.MetadataKey;
import jpl.gds.shared.metadata.MetadataMap;
import jpl.gds.shared.metadata.context.ContextConfigurationType;
import jpl.gds.shared.xml.XmlUtility;
import jpl.gds.shared.xml.stax.StaxStreamWriterFactory;
import org.springframework.context.ApplicationContext;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;

/**
 * Generic context configuration object. A container for other metadata and configuration objects
 * that can load or save the whole configuration at once.
 * 
 *
 * @since R8
 *
 */
public class ContextConfiguration extends SimpleContextConfiguration implements IContextConfiguration {
    
    /**
     * Version number of the context configuration. This tells MPCS
     * which version of the code wrote a config file.
     */
    public static final int OBJECT_VERSION = 0;

    /** Venue metadata */
    protected IVenueConfiguration venueConfig;
    /** Dictionary configuration */
    protected DictionaryProperties dictConfig;
    /** Security parameters */
    protected AccessControlParameters accessControl;
    /** Mission properties */
    protected MissionProperties missionProps;
    /** Connection configuration */
    protected IConnectionMap connectConfig;
    /** Config file name */
    protected String configFile;
    /** Metadata header */
    protected ISerializableMetadata header;
    /** Flag indicating if metadata has changed since the last time metadata header was fetched. */
    protected boolean dirty = true;
    
    /**
     * Constructor to be used by subclasses that override the
     * IContextIdentification member. Note that this method may be invoked ONCE
     * on each unique application context. A second attempt on the same context
     * will result in an IllegalStateException.
     * 
     * @param appContext
     *            the ApplicationContext to get configuration defaults and
     *            objects from
     * @param id
     *            the IContextIdentification object to use as configuration ID.
     * @param register Whether to register the bean as singleton
     */
    protected ContextConfiguration(final ApplicationContext appContext, final IContextIdentification id,
                                   boolean register) {
		super(appContext, id, register);
        this.contextId.getContextKey().setType(ContextConfigurationType.GENERIC_FULL);
	}

    @Override
    protected void initFromContext() {
    	super.initFromContext();
        this.missionProps = appContext.getBean(MissionProperties.class);
		this.dictConfig = appContext.getBean(DictionaryProperties.class);
		this.connectConfig = appContext.getBean(IConnectionMap.class);
		this.accessControl = appContext.getBean(AccessControlParameters.class);
		this.venueConfig = appContext.getBean(IVenueConfiguration.class);
	}

	/**
	 * Constructor for use without application context. THIS SHOULD NOT BE USED
	 * UNLESS LOADING A COMPLETE CONFIGURATION FROM A FILE OR MESSAGE CONTEXT. The
	 * resulting object WILL NOT WORK for other situations.
	 * 
	 * @param missionProps
	 *            the current mission properties object
	 * @param connectProps
	 *            the current connection properties object
	 * @param initFromFileSystem
	 *            true if the file system should be scanned for dictionaries
	 *            (performance intensive), false if not
	 * 
	 */
	public ContextConfiguration(final MissionProperties missionProps, final ConnectionProperties connectProps,
			final boolean initFromFileSystem) {
		super(missionProps);
		this.missionProps = missionProps;
        this.dictConfig = new DictionaryProperties(initFromFileSystem, generalInfo.getSseContextFlag());
        this.connectConfig = new FourConnectionMap(connectProps, missionProps, generalInfo.getSseContextFlag());
		this.accessControl = new AccessControlParameters();
		this.venueConfig = new VenueConfiguration(missionProps);
        this.contextId.getContextKey().setType(ContextConfigurationType.GENERIC_FULL);
	}

    /**
     * Constructor that initializes the object from an ApplicationContext. 
     * 
     * @param appContext
     *            the ApplicationContext to get configuration defaults and objects from
     */
    public ContextConfiguration(final ApplicationContext appContext) {
        super(appContext);
        this.contextId.getContextKey().setType(ContextConfigurationType.GENERIC_FULL);
    }

    @Override
    public void setTemplateContext(final Map<String, Object> map) {
        super.setTemplateContext(map);
        
        this.dictConfig.setTemplateContext(map);
        this.venueConfig.setTemplateContext(map);

        final boolean includeSse = this.missionProps.missionHasSse() && venueConfig.getVenueType() != null
                && !venueConfig.getVenueType().isOpsVenue();
        this.connectConfig.setTemplateContext(map, includeSse);


    }

    @Override
    public void generateStaxXml(final XMLStreamWriter writer)
            throws XMLStreamException {
        writer.writeStartElement("Contexts");
        writer.writeStartElement("Context");

        writer.writeAttribute("version", String.valueOf(OBJECT_VERSION));
        writer.writeAttribute("type", contextId.getType());
        writer.writeAttribute("restPort", String.valueOf(restPort));

               
        XmlUtility.writeSimpleElement(writer, "AmpcsVersion",
                ReleaseProperties.getShortVersion());
        
        final boolean includeSse = this.missionProps.missionHasSse() && venueConfig.getVenueType() != null && 
                !venueConfig.getVenueType().isOpsVenue();

        this.contextId.generateStaxXml(writer);
        this.generalInfo.generateStaxXml(writer);
        this.venueConfig.generateStaxXml(writer);
        this.scFilterInfo.generateStaxXml(writer);
        this.dictConfig.generateStaxXml(writer, includeSse);
        this.connectConfig.generateStaxXml(writer, includeSse);

        writer.writeEndElement();
        writer.writeEndElement();
    }

    @Override
    public String toPrettyXml() {
        String output = "";
        try {
            output = StaxStreamWriterFactory.toPrettyXml(this);
        } catch (final XMLStreamException e) {
            log.error("Could not transform ContextConfiguration object to XML: "
                    + e.getMessage(), e);
        }

        return (output);
    }


    @Override
    public ISerializableMetadata getMetadataHeader() {
        if (this.header == null || this.dirty || contextId.isDirty()
                || venueConfig.isDirty() || scFilterInfo.isDirty()
                || dictConfig.isDirty()) {
            header = contextId.getMetadataHeader();
            header.addContextValues(venueConfig.getMetadataHeader());
            header.addContextValues(scFilterInfo.getMetadataHeader());
            header.addContextValues(dictConfig.getMetadataHeader());
        }
        return this.header;
    }

    @Override
    public IConnectionMap getConnectionConfiguration() {
        return this.connectConfig;
    }

    @Override
    public MissionProperties getMissionProperties() {
        return this.missionProps;
    }

    @Override
    public AccessControlParameters getAccessControlParameters() {
        return this.accessControl;
    }

    @Override
    public DictionaryProperties getDictionaryConfig() {
        return this.dictConfig;
    }

    @Override
    public IVenueConfiguration getVenueConfiguration() {
        return this.venueConfig;
    }

    @Override
    public void copyValuesFrom(final IContextConfiguration toCopy) {

        this.contextId.copyValuesFrom(toCopy.getContextId());
        this.venueConfig.copyValuesFrom(toCopy.getVenueConfiguration());
        this.scFilterInfo.copyValuesFrom(toCopy.getFilterInformation());
        this.accessControl.copyValuesFrom(toCopy.getAccessControlParameters());
        this.connectConfig.copyValuesFrom(toCopy.getConnectionConfiguration());
        this.dictConfig.copyValuesFrom(toCopy.getDictionaryConfig());
        this.generalInfo.copyValuesFrom(toCopy.getGeneralInfo());
        setConfigFile(toCopy.getConfigFile());
        this.dirty = true;

    }

    @Override
    public void clearFieldsForNewConfiguration() {
        contextId.clearFieldsForNewConfiguration();
        generalInfo.clearFieldsForNewConfiguration();
        this.dirty = true;
    }

    @Override
    public void save(final String filename) throws IOException {
        final String toSave = this.toPrettyXml();
        final FileOutputStream fos = new FileOutputStream(filename);
        final PrintStream ps = new PrintStream(fos);
        ps.println("<?xml version=\"1.0\"?>");
        ps.println(toSave);
        ps.close();
    }

    @Override
    public void save() throws IOException {
        if (getConfigFile() == null) {
            throw new IllegalStateException(
                    "Configuration file name is not set");
        }
        save(getConfigFile());
    }


    @Override
    public boolean load(final String filename) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getConfigFile() {
        return this.configFile;
    }

    @Override
    public void setConfigFile(final String filepath) {
        this.configFile = filepath;
        this.dirty = true;
        
    }

    @Override
    public ApplicationContext getApplicationContext() {
        return this.appContext;
    }
    
   @Override
   public boolean equals(final Object o) {
       
       if (!(o instanceof ContextConfiguration)) {
           return false;
       }

       return super.equals(o);
   }

   /** Utility method used by sub-classes to add dictionary info to metadata
    *
    * @param metadata MetadataMap object
    * */
   protected void addDictionaryInfo(final MetadataMap metadata){
        final DictionaryProperties prop = this.getDictionaryConfig();
        //dictionary
        metadata.setValue(MetadataKey.FSW_DICTIONARY_DIR, prop.getFswDictionaryDir());
        metadata.setValue(MetadataKey.FSW_DICTIONARY_VERSION, prop.getFswVersion());
        metadata.setValue(MetadataKey.SSE_DICTIONARY_DIR, prop.getSseDictionaryDir());
        metadata.setValue(MetadataKey.SSE_DICTIONARY_VERSION, prop.getSseVersion());
    }

    @Override
    public boolean getSseContextFlag() {
        return generalInfo.getSseContextFlag().isApplicationSse();
    }

}
