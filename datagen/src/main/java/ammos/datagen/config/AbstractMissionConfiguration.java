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
package ammos.datagen.config;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import jpl.gds.ccsds.api.packet.IPacketFormatDefinition;
import jpl.gds.shared.time.TimeProperties;

/**
 * This abstract class implements the common logic for the data generator
 * mission configuration. It is extended by the mission configuration classes
 * for specific generators.
 * 
 *
 */
public abstract class AbstractMissionConfiguration extends
		AbstractXmlConfiguration implements IMissionConfiguration {

	/**
	 * Constructor.
	 * 
	 * @param name
	 *            name of the configuration being parsed, for error messages
	 * @param schema
	 *            path the RNC schema file for this configuration
	 */
	public AbstractMissionConfiguration(final String name, final String schema) {

		super(name, schema);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.config.AbstractXmlConfiguration#load(java.lang.String,
	 *      org.xml.sax.helpers.DefaultHandler)
	 */
	@Override
	public boolean load(final String uri, final DefaultHandler parseHandler) {

		clear();
		return super.load(uri, parseHandler);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.config.AbstractXmlConfiguration#clear()
	 */
	@Override
	public void clear() {

		super.clear();
		setDefaultProperties();
	}

	/**
	 * Establish defaults for properties that require them (have non-zero or
	 * non-null default values).
	 */
	protected void setDefaultProperties() {

		final TimeProperties timeConfig = TimeProperties.getInstance();

		/*
		 * 10/6/14 - MPCS-6698. Default SCLK settings to those set for the
		 * current mission.
		 */

		final int coarseLen = timeConfig.getSclkCoarseBitLength();
		final int fineLen = timeConfig.getSclkFineBitLength();
		final boolean useFractional = timeConfig.getSclkFormatter().getUseFractional();
		final String sep = timeConfig.getSclkTicksSeparator();

		this.configProperties.put(IMissionConfiguration.SCLK_COARSE_LEN,
				coarseLen);
		this.configProperties.put(IMissionConfiguration.SCLK_FINE_LEN, fineLen);
		this.configProperties.put(IMissionConfiguration.USE_FRACTIONAL_SCLK,
				useFractional);
		this.configProperties.put(IMissionConfiguration.SCLK_SEPARATOR_CHAR,
				sep);
		this.configProperties.put(IMissionConfiguration.FILL_PACKET_APID, 2047);
		this.configProperties.put(IMissionConfiguration.PACKET_MAX_LEN, 65535);

		/*
		 * MPCS-7663 - 9/10/15. Defaulted packet header class property name
		 */
		this.configProperties.put(IMissionConfiguration.PACKET_HEADER_CLASS,
				IPacketFormatDefinition.TypeName.CCSDS.getDefaultPacketHeaderClass());
	}

	/**
	 * This is the SAX parse handler class for the abstract run configuration
	 * file. It is responsible for parsing XML elements common to all run
	 * configurations.
	 * 
	 *
	 */
	protected class MissionConfigurationParseHandler extends
			AbstractXmlParseHandler {

		/**
		 * {@inheritDoc}
		 * 
		 * @see ammos.datagen.config.AbstractXmlConfiguration.AbstractXmlParseHandler#endElement(java.lang.String,
		 *      java.lang.String, java.lang.String)
		 */
		@Override
		public void endElement(final String namespaceURI,
				final String localname, final String rawName)
				throws SAXException {

			super.endElement(namespaceURI, localname, rawName);

			/*
			 * Parse simple values that require no context or special handling
			 * and store them in the property map.
			 */
			boolean found = storeIntegerElement(localname, SCLK_COARSE_LEN);
			found = found || storeIntegerElement(localname, SCLK_FINE_LEN);
			found = found
					|| storeBooleanElement(localname, USE_FRACTIONAL_SCLK);
			found = found || storeIntegerElement(localname, PACKET_MAX_LEN);
			found = found || storeIntegerElement(localname, FILL_PACKET_APID);

			/*
			 * If we got this far and the element is still unrecognized, its a
			 * string property. Add the value to the map using the XML element
			 * name as key.
			 */
			if (!found && !getBufferText().isEmpty()) {
				AbstractMissionConfiguration.this.configProperties.put(
						localname, getBufferText());
			}
		}
	}
}
