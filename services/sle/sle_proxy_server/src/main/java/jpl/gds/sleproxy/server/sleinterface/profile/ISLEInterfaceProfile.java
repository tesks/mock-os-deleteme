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

import java.util.List;
import java.util.Properties;

import com.lsespace.sle.user.proxy.isp1.AuthenticationMode;

/**
 * Interface for the SLE interface profile objects.
 * 
 */
public interface ISLEInterfaceProfile {

	/**
	 * Set the profile from the provided properties.
	 * 
	 * @param profileProperties
	 *            Properties that contain the values to populate the SLE
	 *            interface profile object.
	 * @throws IllegalArgumentException
	 *             Thrown when profileProperties is invalid
	 */
	void setFromProperties(Properties profileProperties) throws IllegalArgumentException;

	/**
	 * Get all of the properties from the SLE interface profile that excludes
	 * password properties.
	 * 
	 * @return Profile properties minus the passwords
	 */
	Properties getNonPasswordProperties();

	/**
	 * Get just the passwords properties from the SLE interface profile.
	 * 
	 * @return Passwords properties of the profile
	 */
	Properties getPasswordProperties();

	/**
	 * Get the name of the SLE interface profile.
	 * 
	 * @return Profile's name
	 */
	String getProfileName();

	/**
	 * Get the name of the SLE interface profile's service provider.
	 * 
	 * @return Provider name in the profile
	 */
	String getProviderName();

	/**
	 * Get the host names and ports of the SLE interface profile's service provider.
	 * 
	 * @return Host names and ports of the SLE service provider in the profile
	 */
	List<ProviderHost> getProviderHosts();

	/**
	 * Get the SLE service provider's authentication mode as defined in the
	 * profile.
	 * 
	 * @return SLE service provider's authentication mode
	 */
	AuthenticationMode getProviderAuthenticationMode();

	/**
	 * Get the Service Instance ID defined in the profile.
	 * 
	 * @return The SIID defined in the profile
	 */
	String getServiceInstanceID();

	/**
	 * Get the SLE service user name defined in the profile.
	 * 
	 * @return User name of the SLE service
	 */
	String getUserName();

	/**
	 * Get the SLE service user's authentication mode as defined in the profile.
	 * 
	 * @return SLE service user's authentication mode
	 */
	AuthenticationMode getUserAuthenticationMode();

	/**
	 * Get the SLE service provider's password.
	 * 
	 * @return SLE service provider's password
	 */
	String getProviderPassword();

	/**
	 * Get the SLE service user's password.
	 * 
	 * @return SLE service user's password
	 */
	String getUserPassword();

	/**
	 * Get the type of the SLE interface defined in the profile.
	 * 
	 * @return Type of the SLE interface
	 */
	EInterfaceType getInterfaceType();

}