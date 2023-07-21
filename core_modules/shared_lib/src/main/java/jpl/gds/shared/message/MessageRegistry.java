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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A registry for known message configurations. Allows a message configuration
 * object to be located for any message type. 
 * 
 * @since R8
 */
public class MessageRegistry {

	private static final Map<IMessageType, IMessageConfiguration> messageConfigMap = new HashMap<>();

	private MessageRegistry() {
		// do nothing
	}

	/**
	 * Adds a new message configuration to the registry.
	 * 
	 * @param config
	 *            IMessageConfiguration to add
	 */
	public static synchronized void registerMessageType(
			final IMessageConfiguration config) {
		messageConfigMap.put(config.getMessageType(), config);
	}


	/**
	 * Gets the message configuration for a message with the specified internal
	 * type tag.
	 * 
	 * @param type
	 *            the internal message type to look for
	 * @return IMessageConfiguration object or null if not found in the registry
	 */
	public static IMessageConfiguration getMessageConfig(final IMessageType type) {
		return messageConfigMap.get(type);
	}

    /**
     * Gets the message configuration for a message with the specified subscription tag.
     * 
     * @param subscriptionTag
     *            the subscription tag to look for
     * @return IMessageConfiguration object or null if not found in the registry
     */
    public static IMessageConfiguration getMessageConfig(final String subscriptionTag) {
        for (final IMessageConfiguration mc : messageConfigMap.values()) {
            if (mc.isTagAliasFor(subscriptionTag)) {
                return mc;         
            }
        }
        return null;
    }

	/**
	 * Gets the message configuration for a message with the specified external
	 * XML root element name.
	 * 
	 * @param root
	 *            the XML root element name to look for
	 * @return IMessageConfiguration object or null if not found in the registry
	 */
	public static synchronized IMessageConfiguration getMessageConfigForXmlRoot(
			final String root) {
		for (final IMessageConfiguration config : messageConfigMap.values()) {
			if (config.getExternalRootElement().equals(root)) {
				return config;
			}
		}
		return null;
	}

    /**
     * Gets the default external XML root element name, given a message type.
     * 
     * @param type the internal message type
     * @return default XML root element name
     */
    public static String getDefaultExternalXmlRoot(final IMessageType type) {
        return MessageConstants.EXTERNAL_MESSAGE_ROOT_PREFIX + type.getSubscriptionTag() + MessageConstants.MESSAGE_ROOT_SUFFIX;
    }
    
    /**
     * Gets the default internal XML root element name, given a message type.
     * 
     * @param type the internal message type
     * @return default XML root element name
     */
    public static String getDefaultInternalXmlRoot(final IMessageType type) {
        return type.getSubscriptionTag() + MessageConstants.MESSAGE_ROOT_SUFFIX;
    }

    /**
     * Gets the default base schema name, given a message type.
     * 
     * @param type the internal message type
     * @return default base schema name, no path
     */
    public static String getDefaultSchemaName(final IMessageType type) {
        return type.getSubscriptionTag() + MessageConstants.SCHEMA_SUFFIX;
    }

    /**
     * Gets the default template sub-directory name, given a message type.
     * 
     * @param type the internal message type
     * @return template sub-directory name
     */
    public static String getDefaultTemplateDir(final IMessageType type) {
        return type.getSubscriptionTag();
    }


    /**
     * Gets an list containing the subscription tags for all registered message types.
     * @param externalOnly true if subscription tags for external messages only should be returned
     * @return list of subscription tags, never null
     */
    public static List<String> getAllSubscriptionTags(final boolean externalOnly) {
        final List<String> result = new ArrayList<>(messageConfigMap.size());
        messageConfigMap.forEach((k,v)->{
            if ((externalOnly && v.isExternal()) || !externalOnly) {
                result.add(v.getSubscriptionTag());
            }
        });    
        return result;
    }
    
    /**
     * Gets a list of all the registered message configurations
     * @param externalOnly true if configurations for external messages only should be returned
     * @return list of message configuration, never null
     */
    public static List<IMessageConfiguration> getAllMessageConfigs(final boolean externalOnly) {
        final List<IMessageConfiguration> list = new ArrayList<>(messageConfigMap.size());
        messageConfigMap.forEach((k,v)->{
            if ((externalOnly && v.isExternal()) || !externalOnly) {
                list.add(v);
            }
        }); 
        return list;
    }

}
