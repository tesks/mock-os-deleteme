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
package jpl.gds.jms;

/**
 * Topic configuration object for use with JMS.
 */
public class JmsTopicConfiguration {
    /**
     * Topic name.
     */
    protected String topicName;
    /**
     * Whether the topic is persistent.
     */
    protected boolean persistent;

    /**
     * Initialize configuration with no name, and not persistent.
     */
    public JmsTopicConfiguration() {
        this((String) null, false);
    }

    /**
     * Initialize configuration with no name.
     * 
     * @param paramPersistent
     *            Whether the topic is persistent.
     */
    public JmsTopicConfiguration(final boolean paramPersistent) {
        this((String) null, paramPersistent);
    }

    /**
     * Initialize configuration with a nonpersistent topic.
     * 
     * @param paramTopicName
     *            Topic name.
     */
    public JmsTopicConfiguration(final String paramTopicName) {
        this(paramTopicName, false);
    }

    /**
     * Initialize configuration based on parameters.
     * 
     * @param paramTopicName
     *            Topic name.
     * @param paramPersistent
     *            Whether the topic is persistent.
     */
    public JmsTopicConfiguration(final String paramTopicName,
            final boolean paramPersistent) {
        this.topicName = paramTopicName;
        this.persistent = paramPersistent;
    }

    /**
     * Returns the topic name.
     * 
     * @return topic name
     */
    public String getTopicName() {
        return topicName;
    }

    /**
     * Returns whether the topic is persistent.
     * 
     * @return if the topic is persistent
     */
    public boolean isPersistent() {
        return persistent;
    }

    /**
     * Sets the persistency of a topic.
     * 
     * @param flag
     *            Whether the topic is persistent.
     */
    public void setPersistent(final boolean flag) {
        persistent = flag;
    }

    /**
     * Sets the topic name.
     * 
     * @param paramTopicName
     *            Name of the topic.
     */
    public void setTopicName(final String paramTopicName) {
        this.topicName = paramTopicName;
    }
}
