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
package jpl.gds.sleproxy.server.chillinterface.config;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This singleton class manages the chill interface configuration in memory.
 * 
 */
public enum ChillInterfaceConfigManager {

	/**
	 * Singleton instance.
	 */
	INSTANCE;

	/**
	 * Path of the chill interface configuration file.
	 */
	private String configFilePath;

	/**
	 * Downlink host.
	 */
	private String downlinkHost;

	/**
	 * Downlink port.
	 */
	private int downlinkPort;

	/**
	 * Uplink listening port.
	 */
	private int uplinkListeningPort;

	/**
	 * The logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(ChillInterfaceConfigManager.class);

	/**
	 * Initialize the chill interface configuration manager.
	 * 
	 * @param configFilePath
	 *            Path of the chill interface configuration file to use for
	 *            initializing
	 * @throws IOException
	 *             Thrown when unable to successfully read the configuration
	 *             file
	 */
	public synchronized void init(final String configFilePath) throws IOException {
		logger.debug("Initializing from {}", configFilePath);
		this.configFilePath = configFilePath;

		Properties configProperties = new Properties();
		InputStream is = new FileInputStream(this.configFilePath);
		configProperties.load(is);
		setFromProperties(configProperties);
	}

	/**
	 * Set the chill interface configuration from the provided Properties
	 * object.
	 * 
	 * @param configProperties
	 *            Properties object to use as the source for updating the chill
	 *            interface configuration
	 * @throws IllegalArgumentException
	 *             Thrown when at least one of the properties has an invalid
	 *             value
	 */
	public synchronized void setFromProperties(final Properties configProperties) throws IllegalArgumentException {

		if (configProperties == null) {
			throw new IllegalArgumentException("Cannot process null properties");
		}

		if (configProperties.containsKey(EChillInterfaceConfigPropertyField.DOWNLINK_HOST.name())) {
			downlinkHost = configProperties.getProperty(EChillInterfaceConfigPropertyField.DOWNLINK_HOST.name()).trim();
		}

		if (configProperties.containsKey(EChillInterfaceConfigPropertyField.DOWNLINK_PORT.name())) {
			String downlinkPortStr = configProperties
					.getProperty(EChillInterfaceConfigPropertyField.DOWNLINK_PORT.name()).trim();

			try {
				downlinkPort = Integer.valueOf(downlinkPortStr);
			} catch (NumberFormatException nfe) {
				throw new IllegalArgumentException(
						"chill interface configuration has invalid downlink port number configured: " + downlinkPortStr,
						nfe);
			}

		}

		if (configProperties.containsKey(EChillInterfaceConfigPropertyField.UPLINK_LISTENING_PORT.name())) {
			String uplinkListeningPortStr = configProperties
					.getProperty(EChillInterfaceConfigPropertyField.UPLINK_LISTENING_PORT.name()).trim();

			try {
				uplinkListeningPort = Integer.valueOf(uplinkListeningPortStr);
			} catch (NumberFormatException nfe) {
				throw new IllegalArgumentException(
						"chill interface configuration has invalid uplink listening port number configured: "
								+ uplinkListeningPortStr,
						nfe);
			}

		}

	}

	/**
	 * Get the chill interface configuration in a Properties format.
	 * 
	 * @return Properties object containing the current chill interface
	 *         configuration properties
	 */
	public synchronized Properties getConfigProperties() {
		Properties p = new Properties();
		p.setProperty(EChillInterfaceConfigPropertyField.DOWNLINK_HOST.name(), downlinkHost);
		p.setProperty(EChillInterfaceConfigPropertyField.DOWNLINK_PORT.name(), Integer.toString(downlinkPort));
		p.setProperty(EChillInterfaceConfigPropertyField.UPLINK_LISTENING_PORT.name(),
				Integer.toString(uplinkListeningPort));
		return p;
	}

	/**
	 * Persist the chill interface configuration.
	 */
	public synchronized void save() {

		try (OutputStream configOS = new FileOutputStream(configFilePath)) {
			getConfigProperties().store(configOS, "Autosaved by chill_sle_proxy");
			logger.debug("Saved configuration to {}", configFilePath);
		} catch (IOException ioe) {
			logger.error("Failure in saving chill interface configuration to {}", configFilePath, ioe);
		}

	}

	/**
	 * Get the uplink listening port.
	 * 
	 * @return Uplink listening port
	 */
	public int getUplinkListeningPort() {
		return uplinkListeningPort;
	}

	/**
	 * Get the downlink host.
	 * 
	 * @return Downlink host
	 */
	public String getDownlinkHost() {
		return downlinkHost;
	}

	/**
	 * Get the downlink port.
	 * 
	 * @return Downlink port
	 */
	public int getDownlinkPort() {
		return downlinkPort;
	}

}