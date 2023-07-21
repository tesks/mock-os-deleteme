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
package jpl.gds.sleproxy.server.messages.config;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Singleton configuration manager for defining the behavior of the messages
 * feature.
 * 
 */
public enum MessagesConfigManager {

	/**
	 * Singleton object.
	 */
	INSTANCE;

	/**
	 * File path of the messages configuration file.
	 */
	private String configFilePath;

	/**
	 * Configured scheme for bounding the size of messages.
	 */
	private volatile EMessagesBoundingScheme messagesBoundingScheme;

	/**
	 * Configured value of the messages age-out, in hours. Use of this depends
	 * on whether the bounding scheme is set to "age".
	 */
	private volatile int messagesPurgeAgeInHours;

	/**
	 * Configured value of the maximum number of messages to retain. Use of this
	 * depends on whether the bounding scheme is set to "size".
	 */
	private volatile int messagesQueueSize;

	/**
	 * Configured value of the appender name. This must match the appender name
	 * defined for <code>MessagesAppender</code> in Log4J's configuration.
	 */
	private String messagesLog4JAppenderName;

	/**
	 * Configured value of the key abbreviation for a message entry's index
	 * number.
	 */
	private String messagesKeyAbbreviationForIndex;

	/**
	 * Configured value of the key abbreviation for a message entry's timestamp.
	 */
	private String messagesKeyAbbreviationForTime;

	/**
	 * Configured value of the key abbreviation for a message entry's level.
	 */
	private String messagesKeyAbbreviationForLevel;

	/**
	 * Configured value of the key abbreviation for a message entry's message
	 * content.
	 */
	private String messagesKeyAbbreviationForMessage;

	/**
	 * Initialize the messages configuration manager, loading the configured
	 * properties into memory.
	 * 
	 * @param configFilePath
	 *            File path of the configuration file to load
	 * @throws IOException
	 *             Thrown when exception is encounterd while trying to the
	 *             configuration file
	 */
	public synchronized void init(final String configFilePath) throws IOException {
		this.configFilePath = configFilePath;

		Properties configProperties = new Properties();
		InputStream is = new FileInputStream(this.configFilePath);
		configProperties.load(is);
		setFromProperties(configProperties);
	}

	/**
	 * Override the messages configuration using the provided properties object.
	 * 
	 * @param configProperties
	 *            Properties object that contains the new configuration to
	 *            override the current one in memory
	 * @throws IllegalArgumentException
	 *             Thrown when <code>null</code> properties object is provided
	 */
	public synchronized void setFromProperties(final Properties configProperties) throws IllegalArgumentException {

		if (configProperties == null) {
			throw new IllegalArgumentException("Cannot process null properties");
		}

		if (configProperties.containsKey(EMessagesConfigPropertyField.MESSAGES_BOUNDING_SCHEME.name())) {
			String messagesBoundingSchemeStr = configProperties
					.getProperty(EMessagesConfigPropertyField.MESSAGES_BOUNDING_SCHEME.name()).trim();

			try {
				messagesBoundingScheme = EMessagesBoundingScheme.valueOf(messagesBoundingSchemeStr);
			} catch (IllegalArgumentException iae) {
				throw new IllegalArgumentException(
						"Messages bounding scheme has invalid value: " + messagesBoundingSchemeStr, iae);
			}

		}

		if (configProperties.containsKey(EMessagesConfigPropertyField.MESSAGES_PURGE_AGE_IN_HOURS.name())) {
			String messagesPurgeAgeInHoursStr = configProperties
					.getProperty(EMessagesConfigPropertyField.MESSAGES_PURGE_AGE_IN_HOURS.name()).trim();

			try {
				messagesPurgeAgeInHours = Integer.valueOf(messagesPurgeAgeInHoursStr);

				if (messagesPurgeAgeInHours <= 0) {
					throw new NumberFormatException("Value is less than or equal to 0");
				}

			} catch (NumberFormatException nfe) {
				throw new IllegalArgumentException(
						"Messages purge age must be a positive integer: " + messagesPurgeAgeInHoursStr, nfe);
			}

		}

		if (configProperties.containsKey(EMessagesConfigPropertyField.MESSAGES_QUEUE_SIZE.name())) {
			String messagesQueueSizeStr = configProperties
					.getProperty(EMessagesConfigPropertyField.MESSAGES_QUEUE_SIZE.name()).trim();

			try {
				messagesQueueSize = Integer.valueOf(messagesQueueSizeStr);
			} catch (NumberFormatException nfe) {
				throw new IllegalArgumentException(
						"Messages config properties has invalid messages queue size value: " + messagesQueueSizeStr,
						nfe);
			}

		}

		if (configProperties.containsKey(EMessagesConfigPropertyField.MESSAGES_LOG4J_APPENDER_NAME.name())) {
			messagesLog4JAppenderName = configProperties
					.getProperty(EMessagesConfigPropertyField.MESSAGES_LOG4J_APPENDER_NAME.name()).trim();
		}

		if (configProperties.containsKey(EMessagesConfigPropertyField.MESSAGES_KEY_ABBREVIATION_FOR_INDEX.name())) {
			messagesKeyAbbreviationForIndex = configProperties
					.getProperty(EMessagesConfigPropertyField.MESSAGES_KEY_ABBREVIATION_FOR_INDEX.name()).trim();
		}

		if (configProperties.containsKey(EMessagesConfigPropertyField.MESSAGES_KEY_ABBREVIATION_FOR_TIME.name())) {
			messagesKeyAbbreviationForTime = configProperties
					.getProperty(EMessagesConfigPropertyField.MESSAGES_KEY_ABBREVIATION_FOR_TIME.name()).trim();
		}

		if (configProperties.containsKey(EMessagesConfigPropertyField.MESSAGES_KEY_ABBREVIATION_FOR_LEVEL.name())) {
			messagesKeyAbbreviationForLevel = configProperties
					.getProperty(EMessagesConfigPropertyField.MESSAGES_KEY_ABBREVIATION_FOR_LEVEL.name()).trim();
		}

		if (configProperties.containsKey(EMessagesConfigPropertyField.MESSAGES_KEY_ABBREVIATION_FOR_MESSAGE.name())) {
			messagesKeyAbbreviationForMessage = configProperties
					.getProperty(EMessagesConfigPropertyField.MESSAGES_KEY_ABBREVIATION_FOR_MESSAGE.name()).trim();
		}

	}

	/**
	 * Get the configured messages bounding scheme.
	 * 
	 * @return The configured messages bounding scheme
	 */
	public EMessagesBoundingScheme getMessagesBoundingScheme() {
		return messagesBoundingScheme;
	}

	/**
	 * Get the configured messages purge age, in hours.
	 * 
	 * @return The configured messages purge age, in hours
	 */
	public int getMessagesPurgeAgeInHours() {
		return messagesPurgeAgeInHours;
	}

	/**
	 * Get the configured messages queue size.
	 * 
	 * @return The configured messages queue size
	 */
	public int getMessagesQueueSize() {
		return messagesQueueSize;
	}

	/**
	 * Get the configured messages appender name.
	 * 
	 * @return The configured messages Log4J appender name
	 */
	public String getMessagesLog4JAppenderName() {
		return messagesLog4JAppenderName;
	}

	/**
	 * Get the configured key abbreviation for message entry's index number.
	 * 
	 * @return The configured key abbreviation for message index
	 */
	public String getMessagesKeyAbbreviationForIndex() {
		return messagesKeyAbbreviationForIndex;
	}

	/**
	 * Get the configured key abbreviation for message entry's timestamp.
	 * 
	 * @return The configured key abbreviation for message time
	 */
	public String getMessagesKeyAbbreviationForTime() {
		return messagesKeyAbbreviationForTime;
	}

	/**
	 * Get the configured key abbreviation for message entry's level.
	 * 
	 * @return The configured key abbreviation for message level
	 */
	public String getMessagesKeyAbbreviationForLevel() {
		return messagesKeyAbbreviationForLevel;
	}

	/**
	 * Get the configured key abbreviation for message entry's message content.
	 * 
	 * @return The configured key abbreviation for message text
	 */
	public String getMessagesKeyAbbreviationForMessage() {
		return messagesKeyAbbreviationForMessage;
	}

}