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

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This singleton class manages the SLE interface profiles in memory.
 * 
 */
public enum SLEInterfaceProfileManager {

	/**
	 * The singleton SLE interface profile manager object.
	 */
	INSTANCE;

	/**
	 * File path of the pre-defined SLE interface profiles.
	 */
	private String profilesFilePath;

	/**
	 * File path of the properties file that contains the passwords for the
	 * pre-defined SLE interface profiles.
	 */
	private String passwordsFilePath;

	/**
	 * The table that keeps SLE interface profiles in memory.
	 */
	private final Map<String, ISLEInterfaceProfile> profileMap;

	/**
	 * The logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(SLEInterfaceProfileManager.class);

	/**
	 * Default constructor.
	 */
	SLEInterfaceProfileManager() {
		profileMap = new HashMap<String, ISLEInterfaceProfile>();
	}

	/**
	 * Initialize the SLE interface profiles manager with the provided profiles
	 * properties file and the passwords properties file.
	 * 
	 * @param profilesFilePath
	 *            File path of the pre-defined profile properties file
	 * @param passwordsFilePath
	 *            File path of the passwords properties for the pre-defined
	 *            profiles
	 * @throws IOException
	 *             Thrown when error is encountered while trying to read either
	 *             files
	 */
	public synchronized void init(final String profilesFilePath, final String passwordsFilePath) throws IOException {
		logger.debug("Initializing from {} and {}", profilesFilePath, passwordsFilePath);
		profileMap.clear();

		this.profilesFilePath = profilesFilePath;
		this.passwordsFilePath = passwordsFilePath;

		Properties profilesProperties = new Properties();
		InputStream profilesIS = new FileInputStream(this.profilesFilePath);
		profilesProperties.load(profilesIS);
		Set<String> profileNames = SLEInterfaceProfilePropertiesUtil.getAllProfileNames(profilesProperties);

		InputStream passwordsIS = new FileInputStream(this.passwordsFilePath);
		Properties passwordsProperties = new Properties();
		passwordsProperties.load(passwordsIS);

		profileNames.forEach(name -> {
			ISLEInterfaceProfile newProfile = SLEInterfaceProfileFactory.createProfile(name, profilesProperties);
			newProfile.setFromProperties(passwordsProperties);
			profileMap.put(name, newProfile);
		});

	}

	/**
	 * Get the SLE interface profile identified by the profile name.
	 * 
	 * @param profileName
	 *            Name of the profile to fetch
	 * @return Profile of the SLE interface or <code>null</code> if not found
	 */
	public synchronized ISLEInterfaceProfile getProfile(final String profileName) {
		return profileMap.get(profileName);
	}

	/**
	 * Get the entire set of SLE interface profiles in Java Properties format.
	 * 
	 * @return Entire set of SLE interface profiles in memory
	 */
	public synchronized Properties getProfilesProperties() {
		Properties p = new Properties();

		profileMap.forEach((k, v) -> {
			p.putAll(getProfileProperties(k));
		});

		return p;
	}

	/**
	 * Get the entire set of SLE interface profiles in a Map format suitable for
	 * JSON.
	 * 
	 * @return Entire set of SLE interface profiles in memory
	 */
	public synchronized List<Map<String, String>> getProfilesPropertiesStructuredForJson() {
		List<Map<String, String>> propertiesMapArray = new ArrayList<>(profileMap.size());

		profileMap.forEach((k, v) -> {
			propertiesMapArray.add(SLEInterfaceProfilePropertiesUtil.convertProfilePropertiesToJsonStructure(k,
					getProfileProperties(k)));
		});

		return propertiesMapArray;
	}

	/**
	 * Get the passwords properties of the SLE interface profiles.
	 * 
	 * @return Entire set of password properties
	 */
	public synchronized Properties getPasswordsProperties() {
		Properties p = new Properties();

		profileMap.forEach((k, v) -> {
			p.putAll(v.getPasswordProperties());
		});

		return p;
	}

	/**
	 * Get the SLE interface profile identified by the provided profile name in
	 * Java Properties format.
	 * 
	 * @param profileName
	 *            Name of the profile to fetch
	 * @return Properties object containing the properties of the desired
	 *         profile, or <code>null</code> if not found
	 */
	public synchronized Properties getProfileProperties(final String profileName) {

		if (!profileMap.containsKey(profileName)) {
			throw new IllegalArgumentException("No SLE interface profile with name '" + profileName + "' exists");
		}

		return getProfile(profileName).getNonPasswordProperties();
	}

	/**
	 * Get the SLE interface profile identified by the provided profile name in
	 * a Map format suitable for JSON.
	 * 
	 * @param profileName
	 *            Name of the profile to fetch
	 * @return Map containing the properties of the desired profile, or empty
	 *         map if not found
	 */
	public synchronized Map<String, String> getProfilePropertiesStructuredForJson(final String profileName) {
		return SLEInterfaceProfilePropertiesUtil.convertProfilePropertiesToJsonStructure(profileName,
				getProfileProperties(profileName));
	}

	/**
	 * Check to see if the profile of the provide name exists.
	 * 
	 * @param profileName
	 *            Name of the profile to look up for existence
	 * @return true if exists, false otherwise
	 */
	public synchronized boolean contains(final String profileName) {
		return profileMap.containsKey(profileName);
	}

	/**
	 * Put a new SLE interface profile in memory.
	 * 
	 * @param profileName
	 *            Name of the SLE interface profile
	 * @param newProfile
	 *            Actual profile object
	 */
	public synchronized void put(final String profileName, final ISLEInterfaceProfile newProfile) {
		profileMap.put(profileName, newProfile);
	}

	/**
	 * Update an existing SLE interface profile in memory.
	 * 
	 * @param profileName
	 *            Name of the SLE interface profile
	 * @param newProfileProperties
	 *            Properties of the profile to update
	 */
	public synchronized void update(final String profileName, final Properties newProfileProperties) {
		getProfile(profileName).setFromProperties(newProfileProperties);
	}

	/**
	 * Remove the specified SLE interface profile.
	 * 
	 * @param profileName
	 *            Name of the profile to remove
	 */
	public synchronized void remove(final String profileName) {
		profileMap.remove(profileName);
	}

	/**
	 * Persist the SLE interface profiles to the profile properties file and the
	 * password properties file..
	 */
	public synchronized void save() {

		try (OutputStream profilesOS = new FileOutputStream(profilesFilePath);
				OutputStream passwordsOS = new FileOutputStream(passwordsFilePath)) {
			getProfilesProperties().store(profilesOS, "Autosaved by chill_sle_proxy");
			getPasswordsProperties().store(passwordsOS, "Autosaved by chill_sle_proxy");
		} catch (IOException e) {
			logger.error("Failure in saving SLE interface profiles to {} and SLE interface passwords to {}",
					profilesFilePath, passwordsFilePath, e);
		}

	}

}