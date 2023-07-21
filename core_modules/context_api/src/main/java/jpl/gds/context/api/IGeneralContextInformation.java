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
import java.util.Calendar;
import java.util.Collection;
import java.util.TimeZone;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.springframework.context.ApplicationContext;

import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.config.ReleaseProperties;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.metadata.ISerializableMetadata;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.shared.template.Templatable;
import jpl.gds.shared.time.TimeProperties;

/**
 * An interface to be implemented by classes containing general context information,
 * such as output directory and messaging topic.
 * 
 *
 * @since R8
 *
 */
public interface IGeneralContextInformation extends Templatable {
	/**
	 * Root session output subdirectory, relative to $CHILL_GDS.
	 */
	public static final String ROOT_OUTPUT_SUBDIR = "test";

	/**
	 * Indicates whether the output directory is automatically generated or has been
	 * overridden by a hard-coded value.
	 * 
	 * @return true if output directory is overridden, false if not
	 */
	public boolean isOutputDirOverridden();
	
	/**
	 * Sets the flag indicating whether the output directory is automatically generated or has been
	 * overridden by a hard-coded value.
	 * 
	 * @param override true if output directory is overridden, false if not
	 */
	public void setOutputDirOverridden(boolean override);
	
	/**
	 * Gets the output directory path for the current context. This value will
	 * be automatically generated based upon current context unless the value
	 * has been specifically overridden.
	 * 
	 * @return the output directory path
	 */
	public String getOutputDir();
	
	/**
	 * Sets the output directory path for the current context. To ensure this value
	 * sticks under all circumstances, the "output directory overridden" flag should be
	 * set.
	 * 
	 * @param dir the output directory path
	 */
	public void setOutputDir(String dir);

	/**
	 * Gets the root messaging topic name for message publication in
	 * the current context.
	 * 
	 * @return topic name
	 */
	public String getRootPublicationTopic();
	
	/**
	 * Sets the root messaging topic name for message publication 
	 * in the current context.
	 * 
	 * @param topic topic name to set
	 */
	public void setRootPublicationTopic(String topic);
	
	/**
	 * Gets the collection of message subscription topics in the current context. 
	 * These may include root topics and data subtopics.
	 * 
	 * @return collection of topic strings
	 */
	public Collection<String> getSubscriptionTopics();
	
    /**
     * Sets the list of message subscription topics in the current context. 
     * These may include root topics and data subtopics.
     * 
     * @param topicsToSet list of topic strings
     */
    public void setSubscriptionTopics(Collection<String> topicsToSet);
	
	/**
	 * Gets the messaging subtopic for the current context. Note that the subtopic
	 * is already included in root publication or subscription topics.  This is
	 * just here for backwards compatibility with the old session configuration,
	 * which needs a separate field for this in the session XML and in the database.
	 * 
	 * @return subtopic name
	 */
	public String getSubtopic();
	
	/**
	 * Sets the messaging subtopic for the current context. Note that the subtopic
     * is already included in root publication or subscription topics.  This is
     * just here for backwards compatibility with the old session configuration,
     * which needs a separate field for this in the session XML and in the database.
	 * 
	 * @param subt subtopic name to set
	 */
	public void setSubtopic(String subt);
	
	/**
	 * Deep copies members, excluding the application context, from the given
	 * IGeneralContextInformation object to the current one.
	 * 
	 * @param toCopy
	 *            the object to copy members from
	 */
	public void copyValuesFrom(IGeneralContextInformation toCopy);

	/**
	 * Gets the ApplicationContext associated with this object.
	 * 
	 * @return ApplicationContext
	 */
	public ApplicationContext getApplicationContext();
	
    /**
     * Clears fields that should be cleared when creating a new configuration. The output
     * directory will be cleared only if the "output directory overridden" flag is not set.
     */
    public void clearFieldsForNewConfiguration();
    
    /**
     * Gets metadata values from this object as a serializable map.
     * 
     * @return ISerializableMetadata
     */
    public ISerializableMetadata getMetadataHeader();
    
    /**
     * Indicates whether the object metadata has changed since the last time the
     * metadata header object was fetched.
     * 
     * @return true if the object has been modified, false if not
     */
    public boolean isDirty();
    
    /**
     * Generates the XML for this connection.
     * 
     * @param writer XML stream to write to
     * @throws XMLStreamException if there is a problem generating the XML
     */
    public void generateStaxXml(final XMLStreamWriter writer) throws XMLStreamException;

	
    /**
     * Static method to compute output directory name from a context.
     * 
     * @param appContext the current ApplicationContext
     * 
     * @return output directory name
     */
	public static String computeOutputDir(final ApplicationContext appContext) {

		final IContextIdentification id = appContext.getBean(IContextIdentification.class);
		if (id.getName() == null) {
			throw new IllegalStateException(
					"Context name must be set in order to generate the output directory name");
		} else if (id.getStartTime() == null) {
			throw new IllegalStateException(
					"Start time must be set in order to generate the output directory name");
		}

		String testPath = null;

		// get the root directory
		final StringBuilder dir = new StringBuilder(256);
        dir.append(GdsSystemProperties.getSystemProperty(GdsSystemProperties.DIRECTORY_PROPERTY,
                                                         GdsSystemProperties.DEFAULT_ROOT_DIR));

		// get the output sub-directory
		dir.append(File.separator + ROOT_OUTPUT_SUBDIR);

		final boolean useDoyDir = TimeProperties.getInstance()
				.useDoyOutputDirectory();

		final Calendar c = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
		if (useDoyDir) {
            c.setTimeInMillis(id.getStartTime().getTime());
			dir.append(File.separator
					+ GDR.fillStr(String.valueOf(c.get(Calendar.YEAR)), 4, '0'));
			dir.append(File.separator
					+ GDR.fillStr(String.valueOf(c.get(Calendar.DAY_OF_YEAR)),
							3, '0'));
		} else {
			// add the year, month, day (GMT)
            c.setTimeInMillis(id.getStartTime().getTime());
			dir.append(File.separator
					+ GDR.fillStr(String.valueOf(c.get(Calendar.YEAR)), 4, '0'));
			// we have to add 1 to the month because Calendar.JANUARY equals 0
			// and Calendar.DECEMBER equals 11
			dir.append(File.separator
					+ GDR.fillStr(String.valueOf(c.get(Calendar.MONTH) + 1), 2,
							'0'));
			dir.append(File.separator
					+ GDR.fillStr(String.valueOf(c.get(Calendar.DAY_OF_MONTH)),
							2, '0'));
		}

		// add the mpcs dir
		dir.append(File.separator + ReleaseProperties.getProductLine().toLowerCase());

		// add the host name
		dir.append(File.separator + id.getHost());

		// add the venue
		dir.append(File.separator + id.getUser() + "_"
				+ id.getName().replaceAll("\\W", "_"));

		// add a timestamp
		dir.append("_" + id.getStartTimeStr().replaceAll("\\W", "_"));

		testPath = dir.toString();

		return (testPath);
	}
	
	/**
	 * Indicates if the default root topic is overridden by a user-specified topic.
	 * 
	 * @return true if topic overridden, false if not
	 */
	public boolean isTopicOverridden();
	
	/**
     * Sets the flag indicating if the default root topic is overridden by a user-specified topic.
     * 
     * @param override true if topic overridden, false if not
     */
	public void setTopicIsOverridden(boolean override);

    public SseContextFlag getSseContextFlag();

}
