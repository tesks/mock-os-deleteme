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
package jpl.gds.sleproxy.server.state;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This singleton class manages chill_sle_proxy's state in memory.
 * 
 */
public enum ProxyStateManager {

	/**
	 * The singleton object.
	 */
	INSTANCE;

	/**
	 * File path of the proxy state properties used to persist and load the
	 * state in memory.
	 */
	private String stateFilePath;

	/**
	 * State of the chill interface for uplink.
	 */
	private volatile EChillInterfaceUplinkState chillInterfaceUplinkState;

	/**
	 * Last used SLE forward service connection number.
	 */
	private volatile AtomicLong lastSLEForwardServiceConnectionNumber;

	/**
	 * Last used SLE return service connection number.
	 */
	private volatile AtomicLong lastSLEReturnServiceConnectionNumber;

	/**
	 * Last used SLE forward service profile name.
	 */
	private volatile String lastSLEForwardServiceProfileName;

	/**
	 * Last used SLE return service profile name.
	 */
	private volatile String lastSLEReturnServiceProfileName;

	/**
	 * The logger.
	 */
	private static final Logger logger = LoggerFactory.getLogger(ProxyStateManager.class);

	/**
	 * Initialize the state manager using the provided state properties file.
	 * 
	 * @param stateFilePath
	 *            File path of the state properties file
	 * @throws IOException
	 *             Thrown when an error is encountered while trying to load the
	 *             state properties file
	 */
	public synchronized void init(final String stateFilePath) throws IOException {
		logger.debug("Initializing from {}", stateFilePath);
		this.stateFilePath = stateFilePath;

		Properties configProperties = new Properties();
		InputStream is = new FileInputStream(this.stateFilePath);
		configProperties.load(is);
		setFromProperties(configProperties);
	}

	/**
	 * Set the state from the provided Properties object.
	 * 
	 * @param stateProperties
	 *            Properties object to use as the source for updating the state
	 * @throws IllegalArgumentException
	 *             Thrown when at least one of the properties has an invalid
	 *             value
	 */
	public synchronized void setFromProperties(final Properties stateProperties) throws IllegalArgumentException {

		if (stateProperties == null) {
			throw new IllegalArgumentException("Cannot process null properties");
		}

		if (stateProperties.containsKey(EProxyStatePropertyField.CHILL_INTERFACE_UPLINK_STATE.name())) {
			String chillInterfaceUplinkStateStr = stateProperties
					.getProperty(EProxyStatePropertyField.CHILL_INTERFACE_UPLINK_STATE.name()).trim();

			try {
				chillInterfaceUplinkState = EChillInterfaceUplinkState.valueOf(chillInterfaceUplinkStateStr);
			} catch (IllegalArgumentException iae) {
				throw new IllegalArgumentException(
						"Proxy state properties has invalid chill interface uplink state value: "
								+ chillInterfaceUplinkStateStr,
						iae);
			}

		}

		if (stateProperties.containsKey(EProxyStatePropertyField.LAST_SLE_FORWARD_SERVICE_CONNECTION_NUMBER.name())) {
			String lastSLEForwardServiceConnectionNumberStr = stateProperties
					.getProperty(EProxyStatePropertyField.LAST_SLE_FORWARD_SERVICE_CONNECTION_NUMBER.name()).trim();

			try {
				lastSLEForwardServiceConnectionNumber = new AtomicLong(
						Long.valueOf(lastSLEForwardServiceConnectionNumberStr));
			} catch (NumberFormatException nfe) {
				throw new IllegalArgumentException(
						"Proxy state properties has invalid last SLE forward service connection number value: "
								+ lastSLEForwardServiceConnectionNumberStr,
						nfe);
			}

		}

		if (stateProperties.containsKey(EProxyStatePropertyField.LAST_SLE_RETURN_SERVICE_CONNECTION_NUMBER.name())) {
			String lastSLEReturnServiceConnectionNumberStr = stateProperties
					.getProperty(EProxyStatePropertyField.LAST_SLE_RETURN_SERVICE_CONNECTION_NUMBER.name()).trim();

			try {
				lastSLEReturnServiceConnectionNumber = new AtomicLong(
						Long.valueOf(lastSLEReturnServiceConnectionNumberStr));
			} catch (NumberFormatException nfe) {
				throw new IllegalArgumentException(
						"Proxy state properties has invalid last SLE return service connection number value: "
								+ lastSLEReturnServiceConnectionNumberStr,
						nfe);
			}

		}

		if (stateProperties.containsKey(EProxyStatePropertyField.LAST_SLE_FORWARD_SERVICE_PROFILE_NAME.name())) {
			lastSLEForwardServiceProfileName = stateProperties
					.getProperty(EProxyStatePropertyField.LAST_SLE_FORWARD_SERVICE_PROFILE_NAME.name()).trim();
		}

		if (stateProperties.containsKey(EProxyStatePropertyField.LAST_SLE_RETURN_SERVICE_PROFILE_NAME.name())) {
			lastSLEReturnServiceProfileName = stateProperties
					.getProperty(EProxyStatePropertyField.LAST_SLE_RETURN_SERVICE_PROFILE_NAME.name()).trim();
		}

	}

	/**
	 * Get the entire state in the form of Java Properties.
	 * 
	 * @return The entire state properties
	 */
	public synchronized Properties getStateProperties() {
		Properties p = new Properties();
		p.setProperty(EProxyStatePropertyField.CHILL_INTERFACE_UPLINK_STATE.name(), chillInterfaceUplinkState.name());
		p.setProperty(EProxyStatePropertyField.LAST_SLE_FORWARD_SERVICE_CONNECTION_NUMBER.name(),
				Long.toString(lastSLEForwardServiceConnectionNumber.get()));
		p.setProperty(EProxyStatePropertyField.LAST_SLE_RETURN_SERVICE_CONNECTION_NUMBER.name(),
				Long.toString(lastSLEReturnServiceConnectionNumber.get()));
		p.setProperty(EProxyStatePropertyField.LAST_SLE_FORWARD_SERVICE_PROFILE_NAME.name(),
				lastSLEForwardServiceProfileName != null ? lastSLEForwardServiceProfileName : "");
		p.setProperty(EProxyStatePropertyField.LAST_SLE_RETURN_SERVICE_PROFILE_NAME.name(),
				lastSLEReturnServiceProfileName != null ? lastSLEReturnServiceProfileName : "");
		return p;
	}

	/**
	 * Persist the state.
	 */
	public synchronized void save() {

		try (OutputStream stateOS = new FileOutputStream(stateFilePath)) {
			getStateProperties().store(stateOS, "Autosaved by chill_sle_proxy");
			logger.debug("Saved state to {}", stateFilePath);
		} catch (IOException ioe) {
			logger.error("Failure in saving chill_sle_proxy state to {}", stateFilePath, ioe);
		}

	}

	/**
	 * Get the chill interface for uplink state.
	 * 
	 * @return State of the chill interface for uplink
	 */
	public EChillInterfaceUplinkState getChillInterfaceUplinkState() {
		return chillInterfaceUplinkState;
	}

	/**
	 * Set the state for the chill interface for uplink.
	 * 
	 * @param newState
	 *            New state of the chill interface for uplink
	 */
	public synchronized void setChillInterfaceUplinkState(final EChillInterfaceUplinkState newState) {
		chillInterfaceUplinkState = newState;
		save();
	}

	/**
	 * Get the new/next SLE forward service connection number.
	 * 
	 * @return New or next SLE forward service connection number
	 */
	public long getNewSLEForwardServiceConnectionNumber() {
		long newSLEForwardServiceConnectionNumber = lastSLEForwardServiceConnectionNumber.incrementAndGet();
		save();
		return newSLEForwardServiceConnectionNumber;
	}

	/**
	 * Get the new/next SLE return service connection number.
	 * 
	 * @return New or next SLE return service connection number
	 */
	public long getNewSLEReturnServiceConnectionNumber() {
		long newSLEReturnServiceConnectionNumber = lastSLEReturnServiceConnectionNumber.incrementAndGet();
		save();
		return newSLEReturnServiceConnectionNumber;
	}

	/**
	 * Get the last used SLE forward service profile name.
	 * 
	 * @return Profile name of the last used SLE forward service
	 */
	public String getLastSLEForwardServiceProfileName() {
		return lastSLEForwardServiceProfileName;
	}

	/**
	 * Set the last used SLE forward service profile name.
	 * 
	 * @param profileName
	 *            Profile name of the last used SLE forward service
	 */
	public synchronized void setLastSLEForwardServiceProfileName(final String profileName) {
		lastSLEForwardServiceProfileName = profileName;
		save();
	}

	/**
	 * Get the last used SLE return service profile name.
	 * 
	 * @return Profile name of the last used SLE return service
	 */
	public String getLastSLEReturnServiceProfileName() {
		return lastSLEReturnServiceProfileName;
	}

	/**
	 * Set the last used SLE return service profile name.
	 * 
	 * @param profileName
	 *            Profile name of the last used SLE return service
	 */
	public synchronized void setLastSLEReturnServiceProfileName(final String profileName) {
		lastSLEReturnServiceProfileName = profileName;
		save();
	}

}