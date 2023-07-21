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

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.springframework.context.ApplicationContext;

import jpl.gds.context.api.ContextTopicNameFactory;
import jpl.gds.context.api.IGeneralContextInformation;
import jpl.gds.context.api.TopicNameToken;
import jpl.gds.shared.metadata.ISerializableMetadata;
import jpl.gds.shared.metadata.MetadataKey;
import jpl.gds.shared.metadata.MetadataMap;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.string.StringUtil;
import jpl.gds.shared.xml.XmlUtility;

/**
 * A class containing general metadata used by a context configuration.
 * 
 *
 * @since R8
 *
 */
public class GeneralContextInformation implements IGeneralContextInformation {

	private String userOutputDir;
	private boolean outputDirOverridden;
	private String topic;
	private String subtopic;
    private boolean topicOverridden;
	private ISerializableMetadata header;
	private Collection<String> subscriptionTopics = new LinkedList<>();
	private boolean dirty = true;
    private SseContextFlag           sseFlag;


	private final ApplicationContext appContext;
	
	/**
	 * Constructor.
	 * 
	 * @param appContext the current application context
	 */
	public GeneralContextInformation(final ApplicationContext appContext) {
		this.appContext = appContext;
        sseFlag = appContext != null ? appContext.getBean(SseContextFlag.class) : new SseContextFlag();
	}
	

	@Override
	public String getOutputDir() {
		if (userOutputDir!= null) {
			return (userOutputDir);
		}
		if (appContext != null) {
			return IGeneralContextInformation.computeOutputDir(appContext);
		} else {
			throw new IllegalStateException("Output directory cannot be computed, because no context, and it has not been overridden");
		}
	}

	@Override
	public void setOutputDir(final String dir) {
		this.userOutputDir = dir;
		this.dirty = true;
	}

	@Override
	public String getRootPublicationTopic() {
		if (this.topic == null) {
			if (this.appContext != null) {
				return ContextTopicNameFactory.getTopicNameFromConfigValue(
						this.appContext, TopicNameToken.APPLICATION);
			} else {
				return "";
			}
		}
		return topic;
	}

	@Override
	public void setRootPublicationTopic(final String topic) {
		this.topic = StringUtil.emptyAsNull(topic);
		this.dirty = true;
	}
	
	@Override
    public boolean isTopicOverridden() {
	    return this.topicOverridden;
	}
	
	@Override
    public void setTopicIsOverridden(final boolean override) {
	    this.topicOverridden = override;
	    this.dirty = true;
	}

	@Override
	public String getSubtopic() {
		return this.subtopic;
	}

	@Override
	public void setSubtopic(final String subt) {
		this.subtopic = subt;
		this.dirty = true;
	}

	@Override
	public boolean isOutputDirOverridden() {
		return this.outputDirOverridden;
	}

	@Override
	public void setOutputDirOverridden(final boolean override) {
		this.outputDirOverridden = override;
		this.dirty = true;
	}

	@Override
	public void copyValuesFrom(final IGeneralContextInformation toCopy) {
		setOutputDirOverridden(toCopy.isOutputDirOverridden());
		if (toCopy.isOutputDirOverridden()) {
			setOutputDir(toCopy.getOutputDir());
		}
		try {
			setRootPublicationTopic(toCopy.getRootPublicationTopic());	
		} catch (final IllegalStateException e) {
			// ignore - leave topic unset
		}
		setSubtopic(toCopy.getSubtopic());

        sseFlag.setApplicationIsSse(toCopy.getSseContextFlag().isApplicationSse());

		this.dirty = true;
	}

	@Override
	public ApplicationContext getApplicationContext() {
		return this.appContext;
	}

	@Override
	public void clearFieldsForNewConfiguration() {
		if (!isOutputDirOverridden()) {
			setOutputDir(null);
		}
		if (!this.topicOverridden) {
			setRootPublicationTopic(null);
		}
        sseFlag = new SseContextFlag();

		this.dirty = true;
	}
	
	 @Override
    public ISerializableMetadata getMetadataHeader() {
	     
	     if (this.header == null || this.dirty) {
	         this.header = new MetadataMap();
	     
	         this.header.setValue(MetadataKey.APPLICATION_OUTPUT_DIRECTORY, getOutputDir());
	         if (this.getRootPublicationTopic() != null) {
	             this.header.setValue(MetadataKey.APPLICATION_ROOT_TOPIC, getRootPublicationTopic());
	         }
	         dirty = false;
	     }
	     return header;
	 }


    @Override
    public boolean isDirty() {
        return this.dirty;
    }


    @Override
    public void setTemplateContext(final Map<String, Object> map) {
        map.put("outputDirOverride", isOutputDirOverridden());

        try {
            map.put("outputDir",
                    new File(getOutputDir()).getAbsolutePath());
        } catch (final IllegalStateException ise) {
            map.put("outputDir", "Undefined");
        }
        

        if (getSubtopic() != null) {
            // New value is just "subtopic" but leave "jmsSubtopic" for backwards-compatibility
            map.put("jmsSubtopic", getSubtopic());
            map.put("subtopic", getSubtopic());
        }

        if (getRootPublicationTopic() != null) {
            map.put("topic", getRootPublicationTopic());
        }
        
        if (sseFlag != null) {
            map.put("isSse", Boolean.valueOf(sseFlag.isApplicationSse()));
        }

    }


    @Override
    public void generateStaxXml(final XMLStreamWriter writer) throws XMLStreamException {

        writer.writeStartElement("GeneralInformation");
        
        try {
            final String outputDir = getOutputDir();
            XmlUtility.writeSimpleCDataElement(writer, "OutputDirectory",
                    new File(outputDir).getAbsolutePath());
        } catch (final IllegalStateException e) {
            // Insufficient state to determine output dir at this time. Skip the
            // element.
        }
        
        XmlUtility.writeSimpleElement(writer, "OutputDirOverridden",
                isOutputDirOverridden());
        
        if (getSubtopic() != null) {
            XmlUtility.writeSimpleElement(writer, "Subtopic",
                getSubtopic());
        }
        
        if (getRootPublicationTopic() != null) {
            XmlUtility.writeSimpleElement(writer, "Topic",
               getRootPublicationTopic());
        }
        
        if (sseFlag != null) {
            XmlUtility.writeSimpleElement(writer, "isSse", getSseContextFlag().isApplicationSse());
        }

//        /*
//         * R8 Refactor TODO.  This represents an interface change. The old
//         * session configuration did not have this in the XML. Needs to be changed
//         * in a SIS.
//         */
//        
//        if (!getSubscriptionTopics().isEmpty()) {
//            final StringBuilder sb = new StringBuilder();
//            for (final String t : getSubscriptionTopics()) {
//                if (sb.length() != 0) {
//                    sb.append(",");
//                }
//                sb.append(t);
//            }
//            XmlUtility.writeSimpleElement(writer, "SubscriptionTopics", sb.toString());
//        }
//        
        writer.writeEndElement();
    }


    @Override
    public Collection<String> getSubscriptionTopics() {
        return new LinkedList<>(this.subscriptionTopics);
    }

    @Override
    public void setSubscriptionTopics(final Collection<String> topicsToSet) {
        this.subscriptionTopics = new LinkedList<>(topicsToSet);
        this.dirty = true;      
    }


    @Override
    public SseContextFlag getSseContextFlag() {
        return sseFlag;
    }
}
