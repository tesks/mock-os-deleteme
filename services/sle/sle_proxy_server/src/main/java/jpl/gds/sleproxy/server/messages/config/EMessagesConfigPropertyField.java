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

/**
 * This enumeration defines the different configuration properties accepted by
 * the <code>MessagesConfigManager</code>, to control the behavior of the
 * messages feature.
 * 
 */
public enum EMessagesConfigPropertyField {

	/**
	 * Configuration property field for messages bounding scheme.
	 */
	MESSAGES_BOUNDING_SCHEME,

	/**
	 * Configuration property field for messages purge age, in hours.
	 */
	MESSAGES_PURGE_AGE_IN_HOURS,

	/**
	 * Configuration property field for messages purge size.
	 */
	MESSAGES_QUEUE_SIZE,

	/**
	 * Configuration property field for the appender name of the
	 * <code>MessagesAppender</code> as set in the Log4J properties.
	 */
	MESSAGES_LOG4J_APPENDER_NAME,

	/**
	 * Configuration property field for the key abbreviation of a message
	 * entry's index number.
	 */
	MESSAGES_KEY_ABBREVIATION_FOR_INDEX,

	/**
	 * Configuration property field for the key abbreviation of a message
	 * entry's timestamp.
	 */
	MESSAGES_KEY_ABBREVIATION_FOR_TIME,

	/**
	 * Configuration property field for the key abbreviation of a message
	 * entry's level. 
	 */
	MESSAGES_KEY_ABBREVIATION_FOR_LEVEL,

	/**
	 * Configuration property field for the key abbreviation of a message
	 * entry's message content. 
	 */
	MESSAGES_KEY_ABBREVIATION_FOR_MESSAGE;

}
