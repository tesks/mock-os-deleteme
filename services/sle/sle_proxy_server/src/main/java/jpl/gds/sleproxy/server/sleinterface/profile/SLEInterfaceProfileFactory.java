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

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility factory class for creating SLE interface profiles.
 * 
 */
public class SLEInterfaceProfileFactory {

	/**
	 * The logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(SLEInterfaceProfileFactory.class);

	/**
	 * Create a new SLE interface profile using the provided profile name and
	 * properties.
	 * 
	 * @param profileName
	 *            Name of the new profile
	 * @param profileProperties
	 *            Properties for the new profile
	 * @return New SLE interface profile that has been initialized with the
	 *         provided data
	 */
	public static ISLEInterfaceProfile createProfile(final String profileName, final Properties profileProperties) {
		logger.debug("Entered createProfile(profileName: {})", profileName);
		ISLEInterfaceProfile newProfile = null;

		if (profileProperties.getProperty(SLEInterfaceProfilePropertiesUtil.getQualifiedPropertyKey(profileName,
				ESLEInterfaceProfilePropertyField.INTERFACE_TYPE)) == null) {
			throw new IllegalArgumentException("Cannot create new profile without "
					+ ESLEInterfaceProfilePropertyField.INTERFACE_TYPE + " property");
		}

		if (EInterfaceType.valueOf(profileProperties
				.getProperty(SLEInterfaceProfilePropertiesUtil.getQualifiedPropertyKey(profileName,
						ESLEInterfaceProfilePropertyField.INTERFACE_TYPE))
				.trim().toUpperCase()) == EInterfaceType.FORWARD) {
			newProfile = new ForwardSLEInterfaceProfile(profileName);

		} else {
			newProfile = new ReturnSLEInterfaceProfile(profileName);
		}

		newProfile.setFromProperties(profileProperties);
		logger.debug("Exiting createProfile()");
		return newProfile;
	}

}