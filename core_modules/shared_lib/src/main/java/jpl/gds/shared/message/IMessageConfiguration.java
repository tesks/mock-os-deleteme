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
package jpl.gds.shared.message;

import java.util.List;

/**
 * An interface to be implemented by classes that represent a message
 * configuration.
 * 
 * @since R8
 *
 */
public interface IMessageConfiguration extends IMessageType {
    
    /**
     * Gets the internal message type for this message configuration.
     * 
     * @return message type
     */
    public IMessageType getMessageType();

	/**
	 * Returns the XML root element name for external (on the message service)
	 * versions of this message.
	 * 
	 * @return XML root element name
	 */
	public abstract String getExternalRootElement();

	/**
	 * Returns the name of the message schema, without directory path
	 * 
	 * @return message schema file name
	 */
	public abstract String getSchemaName();

	/**
	 * Returns the message subscription tag (also known as the internal type
	 * name)
	 * 
	 * @return subscription tag
	 */
	@Override
    public abstract String getSubscriptionTag();

	/**
	 * Gets the fully-qualified class name of the message XML parser
	 * 
	 * @return class name
	 */
	public abstract String getXmlParserClassName();

	/**
	 * Gets the fully-qualified class name of the message binary parser
	 * 
	 * @return class name
	 */
	public abstract String getBinaryParserClassName();

	/**
	 * Gets the list of the message template sub-directories. Each should be a single
	 * path element, not a full path.
	 * 
	 * @return list of template sub-directory names
	 */
	public abstract List<String> getTemplateDirs();
	
	/**
	 * Gets the list of subscription tag aliases for this message configuration.
	 * 
	 * @return list of alternate subscription tags; may be null
	 */
	public abstract List<String> getSubscriptionTagAliases();
	
	/**
	 * Determines if the given subscription tag is a match or alias for
	 * this message configuration.
	 * 
	 * @param tag subscription tag to check
	 * @return true if a match, false if not
	 */
	public abstract boolean isTagAliasFor(String tag);
	
	/**
	 * Indicates if messages with this configuration are published externally to the 
	 * message service.
	 * 
	 * @return true if message is external, false if not
	 */
	public abstract boolean isExternal();

}