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

/**
 * SLE interface profile data structure for the FCLTU type.
 * 
 */
public class ForwardSLEInterfaceProfile extends AbstractSLEInterfaceProfile {

	/**
	 * The mode of FCLTU service.
	 */
	public final String forwardServiceMode;

	/**
	 * Construct a new SLE FLCTU interface profile using the provided profile
	 * name.
	 * 
	 * @param profileName
	 *            Name of the new profile
	 * @throws IllegalArgumentException
	 *             Thrown when the provided profile name is invalid
	 */
	public ForwardSLEInterfaceProfile(final String profileName) throws IllegalArgumentException {
		super(profileName);
		forwardServiceMode = "FIFO";
	}

	/* (non-Javadoc)
	 * @see jpl.gds.sle.proxy_server.sle_interface.profile.AbstractSLEInterfaceProfile#setFromProperties(java.util.Properties)
	 */
	@Override
	public final void setFromProperties(final Properties profileProperties) throws IllegalArgumentException {
		super.setFromProperties(profileProperties);

		String propertyKey = SLEInterfaceProfilePropertiesUtil.getQualifiedPropertyKey(getProfileName(),
				ESLEInterfaceProfilePropertyField.INTERFACE_TYPE);

		if (profileProperties.containsKey(propertyKey)) {
			String interfaceTypeStr = null;

			interfaceTypeStr = profileProperties.getProperty(propertyKey).trim();

			/*
			 * Disallow changing the interface type to something other than a
			 * 'forward'
			 */
			if (!"FORWARD".equals(interfaceTypeStr)) {
				throw new IllegalArgumentException("SLE interface profile '" + getProfileName()
						+ "' is already configured as a \"forward\" service. Cannot set to " + interfaceTypeStr);
			}

		}

	}

	/* (non-Javadoc)
	 * @see jpl.gds.sle.proxy_server.sle_interface.profile.AbstractSLEInterfaceProfile#getNonPasswordProperties()
	 */
	@Override
	public final Properties getNonPasswordProperties() {
		Properties p = super.getNonPasswordProperties();
		p.setProperty(SLEInterfaceProfilePropertiesUtil.getQualifiedPropertyKey(getProfileName(),
				ESLEInterfaceProfilePropertyField.INTERFACE_TYPE), EInterfaceType.FORWARD.name());
		return p;
	}


	/* (non-Javadoc)
	 * @see jpl.gds.sle.proxy_server.sle_interface.profile.AbstractSLEInterfaceProfile#getInterfaceType()
	 */
	@Override
	public final EInterfaceType getInterfaceType() {
		return EInterfaceType.FORWARD;
	}
	
}