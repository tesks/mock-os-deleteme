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
package jpl.gds.sleproxy.server.sleinterface.profile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for manipulating SLE interface profile properties.
 * 
 */
public class SLEInterfaceProfilePropertiesUtil {

	/**
	 * The logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(SLEInterfaceProfilePropertiesUtil.class);

	/**
	 * Get the fully qualified Java Properties key for the provided profile name
	 * and the property field.
	 * 
	 * @param profileName
	 *            Name of the profile
	 * @param profilePropertyField
	 *            Property field of the profile
	 * @return Fully qualified Java Properties key of the profile's property
	 */
	public static String getQualifiedPropertyKey(final String profileName,
			final ESLEInterfaceProfilePropertyField profilePropertyField) {
		return profileName + '.' + profilePropertyField;
	}

	/**
	 * Get the property field name from the fully qualified Java Properties key.
	 * 
	 * @param qualifiedPropertyKey
	 *            Fully qualified Java Properties key that contains the profile
	 *            property field
	 * @return Extracted property field
	 */
	public static String getPropertyFieldFromQualifiedPropertyKey(final String qualifiedPropertyKey) {
		String[] parts = qualifiedPropertyKey.split("\\.");

		if (parts.length > 1) {
			return parts[1];
		} else {
			return null;
		}

	}

	/**
	 * Get all the unique profile names found in the properties set consisting
	 * of fully qualified Java Properties keys.
	 * 
	 * @param profileProperties
	 *            Properties set of fully qualified Java Properties keys
	 * @return Set of unique profile names found in the properties set
	 */
	public static Set<String> getAllProfileNames(final Properties profileProperties) {
		Set<String> propertyNames = profileProperties.stringPropertyNames();

		/*
		 * Initializing the list's size to propertyNames.size() is more than
		 * enough, but at least we limit it somewhat.
		 */
		List<String> firstStemOnly = new ArrayList<String>(propertyNames.size());

		for (String propertyName : propertyNames) {
			int dotIndex = propertyName.indexOf('.');

			if (dotIndex < 0) {
				firstStemOnly.add(propertyName);
			} else {
				firstStemOnly.add(propertyName.substring(0, dotIndex));
			}

		}

		return new HashSet<String>(firstStemOnly);
	}

	/**
	 * Convert a set of fully qualified Java Properties keys and values of a SLE
	 * interface profile into a Map that is JSON-friendly.
	 * 
	 * @param profileName
	 *            Name of the profile
	 * @param profileProperties
	 *            Properties set of the profile
	 * @return Map consisting of keys (property fields) and values (property
	 *         values) of the SLE interface profile
	 */
	public static Map<String, String> convertProfilePropertiesToJsonStructure(final String profileName,
			final Properties profileProperties) {
		Set<Object> keySet = profileProperties.keySet();
		/*
		 * Below, size set to +1 because adding the additional profile name
		 * entry
		 */
		Map<String, String> profilePropertiesMap = new HashMap<>(keySet.size() + 1);
		keySet.forEach(key -> {
			String keyStr = (String) key;
			String mapKey = SLEInterfaceProfilePropertiesUtil.getPropertyFieldFromQualifiedPropertyKey(keyStr);

			if (mapKey != null) {
				profilePropertiesMap.put(mapKey.toLowerCase(), profileProperties.getProperty(keyStr));
			}

		});

		profilePropertiesMap.put("profile_name", profileName);
		return profilePropertiesMap;
	}

	/**
	 * Derive a set of fully qualified Java Properties keys and values from the
	 * provided JSON-friendly Map object.
	 * 
	 * @param profileName
	 *            Name of the profile that the provided properties map is for
	 * @param profilePropertiesMap
	 *            The map object that contains the properties for the profile
	 * @return Properties object containing the SLE interface profile in the
	 *         fully qualified Java Properties format
	 */
	public static Properties deriveProfilePropertiesFromJsonStructure(final String profileName,
			final Map<String, String> profilePropertiesMap) {
		Properties p = new Properties();

		profilePropertiesMap.forEach((k, v) -> {

			try {
				String qualifiedPropertyKey = SLEInterfaceProfilePropertiesUtil.getQualifiedPropertyKey(profileName,
						ESLEInterfaceProfilePropertyField.valueOf(k.toUpperCase()));
				p.put(qualifiedPropertyKey, v);
			} catch (IllegalArgumentException iae) {
				// An unexpected field was included
				logger.warn("An unexpected field was included in the properties JSON for profile {}: {}", profileName,
						k, iae);
			}

		});

		return p;
	}

    /**
     * Rudimentary hostname string validation. Hostname string should consist of host:port pairs separated by the pipe
     * character.
     *
     * @param hosts a host string
     * @return true if valid, false if not
     */
    public static boolean validateHosts(final String hosts) {
    	if (hosts == null || hosts.isEmpty()) {
    		return false;
		}

        final String[] hostArray = hosts.split("\\|");
        if (hostArray.length < 1) {
            return false;
        }

        for (String hostPair : hostArray) {
            if (!hostPair.matches("[a-zA-Z0-9.\\-]+:[1-9][0-9]*")) {
                return false;
            }
            String[] host = hostPair.split(":");
            try {
                final int port = Integer.parseInt(host[1]);

                if (port > 65535 || port < 0) {
                    return false;
                }
            } catch (final NumberFormatException e) {
                return false;
            }
        }

        return true;
    }

}