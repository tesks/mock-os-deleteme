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

import java.security.Provider;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.lsespace.sle.user.proxy.isp1.AuthenticationMode;

/**
 * This abstract class defines the parts of the SLE interface profiles that are
 * common across the board, between RAF, RCF, and FCLTU profile types.
 * 
 */
public abstract class AbstractSLEInterfaceProfile implements ISLEInterfaceProfile {

	/**
	 * Name of the SLE interface profile.
	 */
	private final String profileName;

	/**
	 * SLE service provider's name.
	 */
	private String providerName;

	/**
	 * Host names and ports of the SLE service provider.
	 */
	private List<ProviderHost> providerHosts;

	/**
	 * Host names and ports of the SLE service provider, in properties file string form.
	 */
	private String providerHostsString;

	/**
	 * Authentication mode of the SLE service provider.
	 */
	private AuthenticationMode providerAuthenticationMode;

	/**
	 * Service Instance ID.
	 */
	private String serviceInstanceID;

	/**
	 * User name for the SLE service.
	 */
	private String userName;

	/**
	 * User authentication mode for the SLE service.
	 */
	private AuthenticationMode userAuthenticationMode;

	/**
	 * Password for the SLE service provider. This is excluded from being
	 * included in the getNonPasswordProfileProperties() results.
	 */
	private String providerPassword;

	/**
	 * Password for the SLE service user. This is excluded from being included
	 * in the getNonPasswordProfileProperties() results.
	 */
	private String             userPassword;

	/**
	 * Construct a new SLE interface profile with the given profile name.
	 * 
	 * @param profileName
	 *            Name of the new SLE interface profile
	 * @throws IllegalArgumentException
	 *             Thrown when name is invalid
	 */
	public AbstractSLEInterfaceProfile(final String profileName) throws IllegalArgumentException {

		if (profileName == null || profileName.isEmpty()) {
			throw new IllegalArgumentException("Profile name cannot be null or empty string");
		}

		/*
		 * Properties's setProperty doesn't take null values, so initialize to
		 * empty strings
		 */
		this.profileName = profileName;
		providerName = "";
		providerHosts = new ArrayList<>();
		providerAuthenticationMode = null;
		serviceInstanceID = "";
		userName = "";
		userAuthenticationMode = null;
		providerPassword = "";
		userPassword = "";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jpl.gds.sle.proxy_server.sle_interface.profile.ISLEInterfaceProfile#
	 * getProfileName()
	 */
	@Override
	public final String getProfileName() {
		return profileName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jpl.gds.sle.proxy_server.sle_interface.profile.ISLEInterfaceProfile#
	 * setFromProperties(java.util.Properties)
	 */
	@Override
	public void setFromProperties(final Properties profileProperties) throws IllegalArgumentException {

		if (profileProperties == null) {
			throw new IllegalArgumentException("Cannot process null properties");
		}

		String propertyKey = SLEInterfaceProfilePropertiesUtil.getQualifiedPropertyKey(this.profileName,
				ESLEInterfaceProfilePropertyField.PROVIDER_NAME);

		if (profileProperties.containsKey(propertyKey)) {
			providerName = profileProperties.getProperty(propertyKey).trim();
		}

		propertyKey = SLEInterfaceProfilePropertiesUtil.getQualifiedPropertyKey(this.profileName,
				ESLEInterfaceProfilePropertyField.PROVIDER_HOSTS);

		if (profileProperties.containsKey(propertyKey)) {
			providerHosts.clear();
			providerHostsString = profileProperties.getProperty(propertyKey).trim();
			final String[] providerHostPortPairs = providerHostsString.split("\\|");
			for (String hostPortPair : providerHostPortPairs) {
				final String[] pair = hostPortPair.split(":");
				final ProviderHost host = new ProviderHost(pair[0], pair[1]);
				providerHosts.add(host);
			}
		}

		propertyKey = SLEInterfaceProfilePropertiesUtil.getQualifiedPropertyKey(this.profileName,
				ESLEInterfaceProfilePropertyField.PROVIDER_AUTHENTICATION_MODE);

		if (profileProperties.containsKey(propertyKey)) {
			String providerAuthenticationModeStr = profileProperties.getProperty(propertyKey).trim();

			if (!providerAuthenticationModeStr.isEmpty()) {

				try {
					providerAuthenticationMode = AuthenticationMode
							.valueOf(providerAuthenticationModeStr.toUpperCase());
				} catch (Exception e) {
					throw new IllegalArgumentException("SLE interface profile '" + this.profileName
							+ "' has invalid provider authentication mode configured: " + providerAuthenticationModeStr,
							e);
				}

			}

		}

		propertyKey = SLEInterfaceProfilePropertiesUtil.getQualifiedPropertyKey(this.profileName,
				ESLEInterfaceProfilePropertyField.SERVICE_INSTANCE_ID);

		if (profileProperties.containsKey(propertyKey)) {
			serviceInstanceID = profileProperties.getProperty(propertyKey).trim();
		}

		propertyKey = SLEInterfaceProfilePropertiesUtil.getQualifiedPropertyKey(this.profileName,
				ESLEInterfaceProfilePropertyField.USER_NAME);

		if (profileProperties.containsKey(propertyKey)) {
			userName = profileProperties.getProperty(propertyKey).trim();
		}

		propertyKey = SLEInterfaceProfilePropertiesUtil.getQualifiedPropertyKey(this.profileName,
				ESLEInterfaceProfilePropertyField.USER_AUTHENTICATION_MODE);

		if (profileProperties.containsKey(propertyKey)) {
			String userAuthenticationModeStr = profileProperties.getProperty(propertyKey).trim();

			if (!userAuthenticationModeStr.isEmpty()) {

				try {
					userAuthenticationMode = AuthenticationMode.valueOf(userAuthenticationModeStr.toUpperCase());
				} catch (Exception e) {
					throw new IllegalArgumentException(
							"SLE interface profile '" + this.profileName
									+ "' has invalid user authentication mode configured: " + userAuthenticationModeStr,
							e);
				}

			}

		}

		propertyKey = SLEInterfaceProfilePropertiesUtil.getQualifiedPropertyKey(this.profileName,
				ESLEInterfaceProfilePropertyField.PROVIDER_PASSWORD);

		if (profileProperties.containsKey(propertyKey)) {
			providerPassword = profileProperties.getProperty(propertyKey).trim();
		}

		propertyKey = SLEInterfaceProfilePropertiesUtil.getQualifiedPropertyKey(this.profileName,
				ESLEInterfaceProfilePropertyField.USER_PASSWORD);

		if (profileProperties.containsKey(propertyKey)) {
			userPassword = profileProperties.getProperty(propertyKey).trim();
		}

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jpl.gds.sle.proxy_server.sle_interface.profile.ISLEInterfaceProfile#
	 * getNonPasswordProperties()
	 */
	@Override
	public Properties getNonPasswordProperties() {
		Properties p = new Properties();
		p.setProperty(SLEInterfaceProfilePropertiesUtil.getQualifiedPropertyKey(profileName,
				ESLEInterfaceProfilePropertyField.PROVIDER_NAME), providerName);
		p.setProperty(SLEInterfaceProfilePropertiesUtil.getQualifiedPropertyKey(profileName,
				ESLEInterfaceProfilePropertyField.PROVIDER_HOSTS), providerHostsString);
		p.setProperty(
				SLEInterfaceProfilePropertiesUtil.getQualifiedPropertyKey(profileName,
						ESLEInterfaceProfilePropertyField.PROVIDER_AUTHENTICATION_MODE),
				providerAuthenticationMode != null ? providerAuthenticationMode.name() : "");
		p.setProperty(SLEInterfaceProfilePropertiesUtil.getQualifiedPropertyKey(profileName,
				ESLEInterfaceProfilePropertyField.SERVICE_INSTANCE_ID), serviceInstanceID);
		p.setProperty(SLEInterfaceProfilePropertiesUtil.getQualifiedPropertyKey(profileName,
				ESLEInterfaceProfilePropertyField.USER_NAME), userName);
		p.setProperty(
				SLEInterfaceProfilePropertiesUtil.getQualifiedPropertyKey(profileName,
						ESLEInterfaceProfilePropertyField.USER_AUTHENTICATION_MODE),
				userAuthenticationMode != null ? userAuthenticationMode.name() : "");
		return p;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jpl.gds.sle.proxy_server.sle_interface.profile.ISLEInterfaceProfile#
	 * getPasswordProperties()
	 */
	@Override
	public final Properties getPasswordProperties() {
		Properties p = new Properties();
		p.setProperty(SLEInterfaceProfilePropertiesUtil.getQualifiedPropertyKey(profileName,
				ESLEInterfaceProfilePropertyField.USER_PASSWORD), userPassword);
		p.setProperty(SLEInterfaceProfilePropertiesUtil.getQualifiedPropertyKey(profileName,
				ESLEInterfaceProfilePropertyField.PROVIDER_PASSWORD), providerPassword);
		return p;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see jpl.gds.sle.proxy_server.sle_interface.profile.ISLEInterfaceProfile#
	 * getProviderName()
	 */
	@Override
	public final String getProviderName() {
		return providerName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.nasa.jpl.ammos.sle_proxy_server.sle_interface.profile.
	 * ISLEInterfaceProfile#getProviderHost()
	 */
	@Override
	public final List<ProviderHost> getProviderHosts() {
		return providerHosts;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.nasa.jpl.ammos.sle_proxy_server.sle_interface.profile.
	 * ISLEInterfaceProfile#getProviderAuthenticationMode()
	 */
	@Override
	public final AuthenticationMode getProviderAuthenticationMode() {
		return providerAuthenticationMode;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.nasa.jpl.ammos.sle_proxy_server.sle_interface.profile.
	 * ISLEInterfaceProfile#getServiceInstanceID()
	 */
	@Override
	public final String getServiceInstanceID() {
		return serviceInstanceID;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.nasa.jpl.ammos.sle_proxy_server.sle_interface.profile.
	 * ISLEInterfaceProfile#getUserName()
	 */
	@Override
	public final String getUserName() {
		return userName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.nasa.jpl.ammos.sle_proxy_server.sle_interface.profile.
	 * ISLEInterfaceProfile#getUserAuthenticationMode()
	 */
	@Override
	public final AuthenticationMode getUserAuthenticationMode() {
		return userAuthenticationMode;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.nasa.jpl.ammos.sle_proxy_server.sle_interface.profile.
	 * ISLEInterfaceProfile#getProviderPassword()
	 */
	@Override
	public final String getProviderPassword() {
		return providerPassword;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.nasa.jpl.ammos.sle_proxy_server.sle_interface.profile.
	 * ISLEInterfaceProfile#getUserPassword()
	 */
	@Override
	public final String getUserPassword() {
		return userPassword;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see gov.nasa.jpl.ammos.sle_proxy_server.sle_interface.profile.
	 * ISLEInterfaceProfile#getInterfaceType()
	 */
	@Override
	public abstract EInterfaceType getInterfaceType();

}