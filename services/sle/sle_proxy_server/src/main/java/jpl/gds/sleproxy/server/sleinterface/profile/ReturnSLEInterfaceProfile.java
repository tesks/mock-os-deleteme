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

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Properties;

import com.lsespace.sle.user.service.AllowedReturnFrameQuality;
import com.lsespace.sle.user.util.JavaTimeTag;

import jpl.gds.sleproxy.server.time.DateTimeFormattingUtil;

/**
 * SLE interface profile data structure for the RAF and RCF types.
 * 
 */
public class ReturnSLEInterfaceProfile extends AbstractSLEInterfaceProfile {

	/**
	 * Type of the SLE interface.
	 */
	private EInterfaceType interfaceType;

	/**
	 * Start time of the return frames request.
	 */
	private JavaTimeTag startTime;

	/**
	 * Stop time of the return frames request.
	 */
	private JavaTimeTag stopTime;

	/**
	 * Frame quality of RAF service (not applicable to RCF).
	 */
	private AllowedReturnFrameQuality returnFrameQuality;

	/**
	 * Spacecraft ID for the RCF service (not applicable to RAF).
	 */
	private int spacecraftID;

	/**
	 * Frame version for the RCF service (not applicable to RAF).
	 */
	private int frameVersion;

	/**
	 * Virtual channel for the RCF service (not applicable to RAF).
	 */
	private int virtualChannel;

	/**
	 * Construct a new return frames SLE interface profile using the provided
	 * profile name.
	 * 
	 * @param profileName
	 *            Name of the new profile
	 * @throws IllegalArgumentException
	 *             Thrown when an invalid profile name is provided
	 */
	public ReturnSLEInterfaceProfile(final String profileName) throws IllegalArgumentException {
		super(profileName);
		spacecraftID = -1;
		frameVersion = -1;
		virtualChannel = -1;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jpl.gds.sle.proxy_server.sle_interface.profile.
	 * AbstractSLEInterfaceProfile#setFromProperties(java.util.Properties)
	 */
	@Override
	public final void setFromProperties(final Properties profileProperties) throws IllegalArgumentException {
		super.setFromProperties(profileProperties);

		String propertyKey = SLEInterfaceProfilePropertiesUtil.getQualifiedPropertyKey(getProfileName(),
				ESLEInterfaceProfilePropertyField.INTERFACE_TYPE);

		if (profileProperties.containsKey(propertyKey)) {
			String interfaceTypeStr = profileProperties.getProperty(propertyKey).trim();

			try {
				interfaceType = EInterfaceType.valueOf(interfaceTypeStr.toUpperCase());

				/*
				 * Disallow changing the interface type to something other than
				 * a 'return'
				 */
				if (EInterfaceType.RETURN_ALL != interfaceType && EInterfaceType.RETURN_CHANNEL != interfaceType) {
					throw new IllegalArgumentException("SLE interface profile '" + getProfileName()
							+ "' is already configured as a \"return\" service. Cannot set to " + interfaceType);
				}

			} catch (Exception e) {
				throw new IllegalArgumentException("SLE interface profile '" + getProfileName()
						+ "' has invalid interface type configured: " + interfaceTypeStr, e);
			}

		}

		propertyKey = SLEInterfaceProfilePropertiesUtil.getQualifiedPropertyKey(getProfileName(),
				ESLEInterfaceProfilePropertyField.START_TIME);

		if (profileProperties.containsKey(propertyKey)) {
			String startTimeStr = profileProperties.getProperty(propertyKey).trim();

			if (!startTimeStr.isEmpty()) {

				try {
					startTime = JavaTimeTag.ofEpochMilli(LocalDateTime
							.parse(startTimeStr, DateTimeFormattingUtil.INSTANCE.getAMPCSDateTimeFormatter())
							.toInstant(ZoneOffset.UTC).toEpochMilli());
				} catch (Exception e) {
					throw new IllegalArgumentException("SLE interface profile '" + getProfileName()
							+ "' has invalid return service start time configured: " + startTimeStr, e);
				}

			}

		}

		propertyKey = SLEInterfaceProfilePropertiesUtil.getQualifiedPropertyKey(getProfileName(),
				ESLEInterfaceProfilePropertyField.STOP_TIME);

		if (profileProperties.containsKey(propertyKey)) {
			String stopTimeStr = profileProperties.getProperty(propertyKey).trim();

			if (!stopTimeStr.isEmpty()) {

				try {
					stopTime = JavaTimeTag.ofEpochMilli(LocalDateTime
							.parse(stopTimeStr, DateTimeFormattingUtil.INSTANCE.getAMPCSDateTimeFormatter())
							.toInstant(ZoneOffset.UTC).toEpochMilli());
				} catch (Exception e) {
					throw new IllegalArgumentException("SLE interface profile '" + getProfileName()
							+ "' has invalid return service stop time configured: " + stopTimeStr, e);
				}

			}

		}

		switch (interfaceType) {

		case RETURN_ALL:
			propertyKey = SLEInterfaceProfilePropertiesUtil.getQualifiedPropertyKey(getProfileName(),
					ESLEInterfaceProfilePropertyField.FRAME_QUALITY);

			if (profileProperties.containsKey(propertyKey)) {
				String returnFrameQualityStr = profileProperties.getProperty(propertyKey).trim();

				if (!returnFrameQualityStr.isEmpty()) {

					try {
						returnFrameQuality = AllowedReturnFrameQuality.valueOf(returnFrameQualityStr.toUpperCase());
					} catch (Exception e) {
						throw new IllegalArgumentException("SLE interface profile '" + getProfileName()
								+ "' has invalid return service frame quality configured: " + returnFrameQualityStr, e);
					}

				}

			}

			break;

		case RETURN_CHANNEL:
			propertyKey = SLEInterfaceProfilePropertiesUtil.getQualifiedPropertyKey(getProfileName(),
					ESLEInterfaceProfilePropertyField.SPACECRAFT_ID);

			if (profileProperties.containsKey(propertyKey)) {
				String spacecraftIDStr = profileProperties.getProperty(propertyKey).trim();

				if (!spacecraftIDStr.isEmpty()) {

					try {
						spacecraftID = Integer.valueOf(spacecraftIDStr);
					} catch (NumberFormatException nfe) {
						throw new IllegalArgumentException(
								"SLE interface profile '" + getProfileName()
										+ "' has invalid return service spacecraft ID configured: " + spacecraftIDStr,
								nfe);
					}

				}

			}

			propertyKey = SLEInterfaceProfilePropertiesUtil.getQualifiedPropertyKey(getProfileName(),
					ESLEInterfaceProfilePropertyField.FRAME_VERSION);

			if (profileProperties.containsKey(propertyKey)) {
				String frameVersionStr = profileProperties.getProperty(propertyKey).trim();

				if (!frameVersionStr.isEmpty()) {

					try {
						frameVersion = Integer.valueOf(frameVersionStr);
					} catch (NumberFormatException nfe) {
						throw new IllegalArgumentException(
								"SLE interface profile '" + getProfileName()
										+ "' has invalid return service frame version configured: " + frameVersionStr,
								nfe);
					}

				}

			}

			propertyKey = SLEInterfaceProfilePropertiesUtil.getQualifiedPropertyKey(getProfileName(),
					ESLEInterfaceProfilePropertyField.VIRTUAL_CHANNEL);

			if (profileProperties.containsKey(propertyKey)) {
				String virtualChannelStr = profileProperties.getProperty(propertyKey).trim();

				if (!virtualChannelStr.isEmpty()) {

					try {
						virtualChannel = Integer.valueOf(virtualChannelStr);
					} catch (NumberFormatException nfe) {
						throw new IllegalArgumentException("SLE interface profile '" + getProfileName()
								+ "' has invalid return service virtual channel configured: " + virtualChannelStr, nfe);
					}

				}

			}

			break;

		default:
			throw new IllegalArgumentException("SLE interface profile '" + getProfileName()
					+ "' has invalid return interface type configured: " + interfaceType);
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jpl.gds.sle.proxy_server.sle_interface.profile.
	 * AbstractSLEInterfaceProfile#getNonPasswordProperties()
	 */
	@Override
	public final Properties getNonPasswordProperties() {
		Properties p = super.getNonPasswordProperties();
		p.setProperty(
				SLEInterfaceProfilePropertiesUtil.getQualifiedPropertyKey(getProfileName(),
						ESLEInterfaceProfilePropertyField.INTERFACE_TYPE),
				interfaceType != null ? interfaceType.name() : "");
		p.setProperty(
				SLEInterfaceProfilePropertiesUtil.getQualifiedPropertyKey(getProfileName(),
						ESLEInterfaceProfilePropertyField.START_TIME),
				startTime != null ? DateTimeFormattingUtil.INSTANCE.toAMPCSDateTimeString(startTime) : "");
		p.setProperty(
				SLEInterfaceProfilePropertiesUtil.getQualifiedPropertyKey(getProfileName(),
						ESLEInterfaceProfilePropertyField.STOP_TIME),
				stopTime != null ? DateTimeFormattingUtil.INSTANCE.toAMPCSDateTimeString(stopTime) : "");

		switch (interfaceType) {

		case RETURN_ALL:
			p.setProperty(
					SLEInterfaceProfilePropertiesUtil.getQualifiedPropertyKey(getProfileName(),
							ESLEInterfaceProfilePropertyField.FRAME_QUALITY),
					returnFrameQuality != null ? returnFrameQuality.name() : "");
			break;
		case RETURN_CHANNEL:
			p.setProperty(
					SLEInterfaceProfilePropertiesUtil.getQualifiedPropertyKey(getProfileName(),
							ESLEInterfaceProfilePropertyField.SPACECRAFT_ID),
					spacecraftID >= 0 ? Integer.toString(spacecraftID) : "");
			p.setProperty(
					SLEInterfaceProfilePropertiesUtil.getQualifiedPropertyKey(getProfileName(),
							ESLEInterfaceProfilePropertyField.FRAME_VERSION),
					frameVersion >= 0 ? Integer.toString(frameVersion) : "");
			p.setProperty(
					SLEInterfaceProfilePropertiesUtil.getQualifiedPropertyKey(getProfileName(),
							ESLEInterfaceProfilePropertyField.VIRTUAL_CHANNEL),
					virtualChannel >= 0 ? Integer.toString(virtualChannel) : "");
			break;
		default:
			throw new IllegalArgumentException("SLE interface profile '" + getProfileName()
					+ "' has invalid return interface type configured: " + interfaceType);
		}

		return p;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jpl.gds.sle.proxy_server.sle_interface.profile.
	 * AbstractSLEInterfaceProfile#getInterfaceType()
	 */
	@Override
	public final EInterfaceType getInterfaceType() {
		return interfaceType;
	}

	/**
	 * Get the start time of the return frames request.
	 * 
	 * @return The frames start time
	 */
	public final JavaTimeTag getStartTime() {
		return startTime;
	}

	/**
	 * Get the stop time of the return frames request.
	 * 
	 * @return The frames stop time
	 */
	public final JavaTimeTag getStopTime() {
		return stopTime;
	}

	/**
	 * Get the frame quality for the RAF service.
	 * 
	 * @return The frame quality
	 */
	public final AllowedReturnFrameQuality getReturnFrameQuality() {
		return returnFrameQuality;
	}

	/**
	 * Get the spacecraft ID for the RCF service.
	 * 
	 * @return The spacecraft ID
	 */
	public final int getSpacecraftID() {
		return spacecraftID;
	}

	/**
	 * Get the frame version for the RCF service.
	 * 
	 * @return The frame version
	 */
	public final int getFrameVersion() {
		return frameVersion;
	}

	/**
	 * @return the virtualChannel
	 */
	public final int getVirtualChannel() {
		return virtualChannel;
	}

}
