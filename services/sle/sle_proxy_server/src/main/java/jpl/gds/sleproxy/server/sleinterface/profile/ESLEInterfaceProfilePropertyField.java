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

/**
 * Enumerates the different types of properties that are configurable for each
 * SLE interface profile.
 * 
 */
public enum ESLEInterfaceProfilePropertyField {

	/**
	 * Property field for the name of the SLE service provider.
	 * 
	 * Handled by ASLEInterfaceProfile.
	 */
	PROVIDER_NAME,

	/**
	 * Property field for the host names and ports of the SLE service provider.
	 * 
	 * Handled by ASLEInterfaceProfile.
	 */
	PROVIDER_HOSTS, // Handled by ASLEInterfaceProfile

	/**
	 * Property field for the SLE service provider's authentication mode.
	 * 
	 * Handled by ASLEInterfaceProfile.
	 */
	PROVIDER_AUTHENTICATION_MODE,

	/**
	 * Property field for the Service Instance ID, or SIID.
	 * 
	 * Handled by ASLEInterfaceProfile.
	 */
	SERVICE_INSTANCE_ID,

	/**
	 * Property field for the user name of the SLE service user.
	 * 
	 * Handled by ASLEInterfaceProfile.
	 */
	USER_NAME,

	/**
	 * Property field for the SLE service user's authentication mode.
	 * 
	 * Handled by ASLEInterfaceProfile.
	 */
	USER_AUTHENTICATION_MODE,

	/**
	 * Property field for the SLE service provider's password.
	 * 
	 * Handled by ASLEInterfaceProfile.
	 */
	PROVIDER_PASSWORD,

	/**
	 * Property field for the SLE service user's password.
	 * 
	 * Handled by ASLEInterfaceProfile.
	 */
	USER_PASSWORD,

	/**
	 * Property field for the type of the SLE interface profile.
	 * 
	 * Handled by subclasses of ASLEInterfaceProfile.
	 */
	INTERFACE_TYPE,

	/**
	 * Property field for the start time of the SLE interface profile for return
	 * services.
	 * 
	 * Handled by ReturnSLEInterfaceProfile.
	 */
	START_TIME,

	/**
	 * Property field for the stop time of the SLE interface profile for return
	 * services.
	 * 
	 * Handled by ReturnSLEInterfaceProfile.
	 */
	STOP_TIME,

	/**
	 * Property field for the frame quality if the SLE interface profile is RAF.
	 * 
	 * ReturnSLEInterfaceProfile if INTERFACE_TYPE is RETURN_ALL.
	 */
	FRAME_QUALITY,

	/**
	 * Property field for the spacecraft ID if the SLE interface profile is RCF.
	 * 
	 * Handled by ReturnSLEInterfaceProfile if INTERFACE_TYPE is RETURN_CHANNEL.
	 */
	SPACECRAFT_ID,

	/**
	 * Property field for the frame version if the SLE interface profile is RCF.
	 * 
	 * Handled by ReturnSLEInterfaceProfile if INTERFACE_TYPE is RETURN_CHANNEL.
	 */
	FRAME_VERSION,

	/**
	 * Property field for the virtual channel if the SLE interface profile is RCF.
	 * 
	 * Handled by ReturnSLEInterfaceProfile if INTERFACE_TYPE is RETURN_CHANNEL.
	 */
	VIRTUAL_CHANNEL;

}