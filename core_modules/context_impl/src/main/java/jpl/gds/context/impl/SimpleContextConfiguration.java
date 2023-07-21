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

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.context.api.IContextFilterInformation;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.context.api.IGeneralContextInformation;
import jpl.gds.context.api.ISimpleContextConfiguration;
import jpl.gds.context.api.filtering.IFilterableDataItem;
import jpl.gds.context.impl.spring.bootstrap.ContextSpringBootstrap;
import jpl.gds.shared.config.ReleaseProperties;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.metadata.MetadataKey;
import jpl.gds.shared.metadata.MetadataMap;
import jpl.gds.shared.metadata.context.ContextConfigurationType;
import jpl.gds.shared.spring.context.SpringContextFactory;
import jpl.gds.shared.types.UnsignedInteger;
import jpl.gds.shared.xml.XmlUtility;
import jpl.gds.shared.xml.stax.StaxStreamWriterFactory;

import org.apache.commons.lang.StringUtils;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Simple context configuration object.
 * 
 *
 * @since R8
 *
 */
public class SimpleContextConfiguration implements ISimpleContextConfiguration {

	//separator for session IDs in database context metadata
	private static final String SESSION_ID_SEPARATOR = ":";

	//list of session IDs produced by a server app
	private List<Long> sessionIds = new ArrayList<>();

	/**
	 * Version number of the simple context configuration. This tells MPCS which
	 * version of the code wrote a config file.
	 */
	public static final int OBJECT_VERSION = 0;

	/** Shared instance of the logger */
	protected final Tracer log;

	/** Application context this object represents the configuration for */
	protected final ApplicationContext appContext;

	/** Context identification */
	protected IContextIdentification contextId;

	/** General metadata */
	protected IGeneralContextInformation generalInfo;

	/** Spacecraft data filters */
	protected IContextFilterInformation scFilterInfo;

    /** The REST service port (if any) */
    protected int                        restPort       = -1;

	protected void initFromContext() {
		this.contextId = appContext.getBean(IContextIdentification.class);
		this.generalInfo = appContext.getBean(IGeneralContextInformation.class);
		this.scFilterInfo = appContext.getBean(IContextFilterInformation.class);
	}

	/**
	 * Constructor that initializes the object from an ApplicationContext.
	 * 
	 * @param appContext
	 *            the ApplicationContext to get configuration defaults and objects
	 *            from
	 */
	public SimpleContextConfiguration(final ApplicationContext appContext) {
		this.appContext = appContext;
        log = TraceManager.getDefaultTracer(appContext);
		initFromContext();
		this.contextId.getContextKey().setType(ContextConfigurationType.SIMPLE);
	}

	/**
	 * Constructor that initializes the object from an ApplicationContext.
	 *
	 * @param appContext
	 *            the ApplicationContext to get configuration defaults and objects from
	 * @param id Context identification to register
	 * @param register Whether to register the bean as singleton
	 */
	protected SimpleContextConfiguration(final ApplicationContext appContext, final IContextIdentification id,
	                                     boolean register) {
		this.appContext = appContext;
        log = TraceManager.getDefaultTracer(appContext);
		/*
		 * Register the CONTEXT IDENTIFICATION
		 */
		if(register) {
			try {
				if (appContext instanceof AnnotationConfigApplicationContext) {
					((AnnotationConfigApplicationContext) appContext).getBeanFactory().registerSingleton(
							ContextSpringBootstrap.CONTEXT_IDENTIFICATION, id);
				}
				else {
					((AnnotationConfigServletWebServerApplicationContext) appContext).getBeanFactory().registerSingleton(
							ContextSpringBootstrap.CONTEXT_IDENTIFICATION, id);
				}
			}
			catch (final Exception e) {
			log.error(
						"This error is most likely occuring because you have registered two CONTEXT IDENTIFICATIONS to the same Application Context",
						ExceptionTools.getMessage(e));
				throw e;
			}

			/*
			 * Register the CONTEXT CONFIGURATION
			 */
			try {
				if (appContext instanceof AnnotationConfigApplicationContext) {
					((AnnotationConfigApplicationContext) appContext).getBeanFactory().registerSingleton(
							ContextSpringBootstrap.CONTEXT_CONFIGURATION, this);
				}
				else {
					((AnnotationConfigServletWebServerApplicationContext) appContext).getBeanFactory().registerSingleton(
							ContextSpringBootstrap.CONTEXT_CONFIGURATION, this);
				}
			}
			catch (final Exception e) {
			log.error(
						"This error is most likely occuring because you have registered two CONTEXT CONFIGURATIONS to the same Application Context",
						ExceptionTools.getMessage(e));
				throw e;
			}
		}

		initFromContext();
		this.contextId.getContextKey().setType(ContextConfigurationType.SIMPLE);
	}

	/**
	 * Constructor for use without application context. THIS SHOULD NOT BE USED
	 * UNLESS LOADING A COMPLETE CONFIGURATION FROM A FILE OR MESSAGE CONTEXT. The
	 * resulting object WILL NOT WORK for other situations.
	 * 
	 * @param missionProps
	 *            the current mission properties object
	 * 
	 */
	public SimpleContextConfiguration(final MissionProperties missionProps) {
		this.appContext = null;
		this.log = null;
		this.contextId = new ContextIdentification(missionProps, missionProps.getDefaultScid());
		//Pass new appContext instead of null
		this.generalInfo = new GeneralContextInformation(SpringContextFactory.getSpringContext());
		this.contextId.getContextKey().setType(ContextConfigurationType.SIMPLE);
		this.scFilterInfo = new ContextFilterInformation();
	}

	@Override
	public void setTemplateContext(final Map<String, Object> map) {
		map.put("mpcsVersion", ReleaseProperties.getShortVersion());
        map.put("version", OBJECT_VERSION);
        map.put("type", contextId.getType());
        map.put("restPort", restPort);
        map.put("sessionIds", getSessionIdsAsString());

		this.contextId.setTemplateContext(map);
		this.generalInfo.setTemplateContext(map);
	}

	@Override
	public void generateStaxXml(final XMLStreamWriter writer) throws XMLStreamException {
		generateStartStaxXml(writer);

		generateBodyStaxXml(writer);

		generateEndStaxXml(writer);
	}

	protected void generateBodyStaxXml(final XMLStreamWriter writer) throws XMLStreamException {

		XmlUtility.writeSimpleElement(writer, "AmpcsVersion", ReleaseProperties.getShortVersion());

		this.contextId.generateStaxXml(writer);
		this.generalInfo.generateStaxXml(writer);
		this.scFilterInfo.generateStaxXml(writer);
	}

	protected void generateStartStaxXml(final XMLStreamWriter writer) throws XMLStreamException {
		writer.writeStartElement("Contexts");
		writer.writeStartElement("Context");

		writer.writeAttribute("version", String.valueOf(OBJECT_VERSION));
		writer.writeAttribute("type", contextId.getType());
		writer.writeAttribute("restPort", String.valueOf(restPort));
	}

	protected void generateEndStaxXml(final XMLStreamWriter writer) throws XMLStreamException {
		writer.writeEndElement();
		writer.writeEndElement();
	}

	@Override
	public String toXml() {
		String output = "";
		try {
			output = StaxStreamWriterFactory.toXml(this);
		} catch (final XMLStreamException e) {
			log.error("Could not transform TestConfiguration object to XML: " + e.getMessage(), e);
		}

		return (output);
	}

	@Override
	public IContextIdentification getContextId() {
		return this.contextId;
	}

	@Override
	public IGeneralContextInformation getGeneralInfo() {
		return this.generalInfo;
	}

	@Override
	public boolean equals(final Object o) {

		if (!(o instanceof SimpleContextConfiguration)) {
			return false;
		}

		final SimpleContextConfiguration config = (SimpleContextConfiguration) o;

		return contextId.equals(config.getContextId());
	}

	@Override
	public int hashCode() {
		return contextId.hashCode();
	}

	@Override
	public String toString() {
		return contextId.toString();
	}

	@Override
	public MetadataMap getMetadata() {

		final MetadataMap metadata = new MetadataMap(getContextId().getContextKey().getNumber().toString());
		//generic context metadata
        metadata.setValue(MetadataKey.CREATE_TIME, getContextId().getStartTimeStr());
        metadata.setValue(MetadataKey.END_TIME, getContextId().getEndTimeStr());
        metadata.setValue(MetadataKey.APPLICATION_OUTPUT_DIRECTORY, getGeneralInfo().getOutputDir());

        // Updates to include the root topic and port
        metadata.setValue(MetadataKey.APPLICATION_ROOT_TOPIC, getGeneralInfo().getRootPublicationTopic());
        metadata.setValue(MetadataKey.REST_PORT, restPort);
		metadata.setValue(MetadataKey.CONTEXT_TYPE, getContextId().getType());

		metadata.setValue(MetadataKey.SESSION_IDS, getSessionIdsAsString());

		return metadata;
    }

    @Override
    public void setRestPort(final UnsignedInteger restPort) {
        this.restPort = restPort.intValue();
    }

    @Override
	public int getRestPort() {
		return this.restPort;
	}

    @Override
	public IContextFilterInformation getFilterInformation() {
		return this.scFilterInfo;
	}

	@Override
	public boolean accept(final IFilterableDataItem toFilter) {
		return contextId.accept(toFilter) && scFilterInfo.accept(toFilter, generalInfo.getSseContextFlag());
	}

	@Override
	public String getSessionIdsAsString() {
		return StringUtils.join(sessionIds.toArray(), SESSION_ID_SEPARATOR);
	}

	@Override
	public void addSessionId(final Long sessionId) {
		sessionIds.add(sessionId);
	}
}
