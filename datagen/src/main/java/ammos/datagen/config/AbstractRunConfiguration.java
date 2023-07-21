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

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import ammos.datagen.generators.seeds.IFileSeedData;
import ammos.datagen.generators.seeds.StringGeneratorSeed;
import jpl.gds.shared.log.TraceManager;

/**
 * This abstract class implements the common logic for the data generator run
 * configuration. It is extended by the run configuration classes for specific
 * generators.
 * 
 *
 */
public abstract class AbstractRunConfiguration extends AbstractXmlConfiguration
		implements IRunConfiguration {

	/**
	 * Name of XML element that wraps SCLK seed values.
	 */
	private static final String PACKET_SCLK_ELEMENT = "SclkSeedData";

	/**
	 * Constructor.
	 * 
	 * @param name
	 *            name of the configuration being parsed, for error messages
	 * @param schema
	 *            path the RNC schema file for this configuration
	 */
	public AbstractRunConfiguration(final String name, final String schema) {

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
		boolean ok = super.load(uri, parseHandler);

		/*
		 * The file parsed ok. Check things that the parser cannot easily
		 * verify.
		 */
		if (ok) {
			/*
			 * Both the SCLK coarse and fine deltas must have the same sign.
			 */
			final long cd = this.getLongProperty(SCLK_COARSE_DELTA, 0);
			final long fd = this.getLongProperty(SCLK_FINE_DELTA, 0);
			if ((cd > 0 && fd < 0) || (cd < 0 && fd > 0)) {
				TraceManager

						.getDefaultTracer()
						.error("Both SCLK deltas in the run configuration (coarse and fine) must have the same sign (positive or negative)");
				ok = false;
			}
		}
		return ok;
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

		this.configProperties.put(INTEGER_8_SEED_TABLE,
				IFileSeedData.DEFAULT_TABLE_NAME);
		this.configProperties.put(INTEGER_16_SEED_TABLE,
				IFileSeedData.DEFAULT_TABLE_NAME);
		this.configProperties.put(INTEGER_32_SEED_TABLE,
				IFileSeedData.DEFAULT_TABLE_NAME);
		this.configProperties.put(INTEGER_64_SEED_TABLE,
				IFileSeedData.DEFAULT_TABLE_NAME);
		this.configProperties.put(UNSIGNED_8_SEED_TABLE,
				IFileSeedData.DEFAULT_TABLE_NAME);
		this.configProperties.put(UNSIGNED_16_SEED_TABLE,
				IFileSeedData.DEFAULT_TABLE_NAME);
		this.configProperties.put(UNSIGNED_32_SEED_TABLE,
				IFileSeedData.DEFAULT_TABLE_NAME);
		this.configProperties.put(UNSIGNED_64_SEED_TABLE,
				IFileSeedData.DEFAULT_TABLE_NAME);
		this.configProperties.put(FLOAT_32_SEED_TABLE,
				IFileSeedData.DEFAULT_TABLE_NAME);
		this.configProperties.put(FLOAT_64_SEED_TABLE,
				IFileSeedData.DEFAULT_TABLE_NAME);
		this.configProperties.put(INITIAL_SCLK_COARSE, 0);
		this.configProperties.put(INITIAL_SCLK_FINE, 0);
		this.configProperties.put(SCLK_COARSE_DELTA, 0);
		this.configProperties.put(SCLK_FINE_DELTA, 0);
		this.configProperties.put(INCLUDE_INVALID_ENUMS, false);
		this.configProperties.put(INCLUDE_EMPTY_STRINGS, false);
		this.configProperties.put(INCLUDE_NULL_CHAR, false);
		this.configProperties.put(INCLUDE_NAN_INFINITE_FLOATS, false);
		this.configProperties.put(INCLUDE_NON_ZERO_ONE_BOOL, false);
		this.configProperties.put(SCLK_STOP_WHEN_EXHAUSTED, false);
		this.configProperties.put(STRING_CHAR_SET,
				StringGeneratorSeed.DEFAULT_CHAR_SET);
		this.configProperties.put(DESIRED_REPORT_INTERVAL, 5);
		this.configProperties.put(INTEGER_TRAVERSAL_TYPE, TraversalType.RANDOM);
		this.configProperties
				.put(UNSIGNED_TRAVERSAL_TYPE, TraversalType.RANDOM);
		this.configProperties.put(ENUM_TRAVERSAL_TYPE, TraversalType.RANDOM);
		this.configProperties.put(BOOL_TRAVERSAL_TYPE, TraversalType.RANDOM);
		this.configProperties.put(STRING_TRAVERSAL_TYPE, TraversalType.RANDOM);
		this.configProperties.put(DESIRED_FILL_PERCENT, (float) 0.0);
		/* MPCS-6864 - 11/21/14. Added desired number of packets property. */
		this.configProperties.put(DESIRED_NUM_PACKETS, 0);
	    /* MPCS-TBD - 01/02/18. Added number of files property, defaults to 1 */
		this.configProperties.put(DESIRED_NUM_FILES, 1);
	}

	/**
	 * This is the SAX parse handler class for the abstract run configuration
	 * file. It is responsible for parsing XML elements common to all run
	 * configurations.
	 * 
	 *
	 */
	protected class AbstractRunConfigurationParseHandler extends
			AbstractXmlParseHandler {

		private boolean inPacketSclkSeed = false;

		/**
		 * {@inheritDoc}
		 * 
		 * @see ammos.datagen.config.AbstractXmlConfiguration.AbstractXmlParseHandler#startElement(java.lang.String,
		 *      java.lang.String, java.lang.String, org.xml.sax.Attributes)
		 */
		@Override
		public void startElement(final String namespaceURI,
				final String localname, final String rawName,
				final Attributes atts) throws SAXException {

			super.startElement(namespaceURI, localname, rawName, atts);

			/*
			 * Take note of when we start parsing a packet SCLK seed.
			 */
			if (localname.equalsIgnoreCase(PACKET_SCLK_ELEMENT)) {
				this.inPacketSclkSeed = true;
			}
		}

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

			boolean found = false;

			/*
			 * Take note of when we start parsing a packet SCLK seed.
			 */
			if (localname.equalsIgnoreCase(PACKET_SCLK_ELEMENT)) {
				this.inPacketSclkSeed = false;
				return;
			}

			/*
			 * If we are parsing a packet SCLK seed, look for the elements in
			 * the SCLK seed and store them in the property table if found.
			 * There may be more than one SCLK Seed in the run configuration so
			 * we need to make sure we are parsing the right one.
			 */
			if (this.inPacketSclkSeed) {
				found = found
						|| storeLongElement(localname, INITIAL_SCLK_COARSE);
				found = found || storeLongElement(localname, INITIAL_SCLK_FINE);
				found = found || storeLongElement(localname, SCLK_COARSE_DELTA);
				found = found || storeLongElement(localname, SCLK_FINE_DELTA);
				found = found
						|| storeBooleanElement(localname,
								SCLK_STOP_WHEN_EXHAUSTED);
				found = found || storeStringElement(localname, SCLK_SEED_TABLE);
			}
			/*
			 * Parse any other non-string element we recognize and store the
			 * value in the property map.
			 */
			found = found || storeIntegerElement(localname, STRING_MAX_LEN);
			found = found
					|| storeBooleanElement(localname, INCLUDE_INVALID_ENUMS);
			found = found
					|| storeBooleanElement(localname, INCLUDE_EMPTY_STRINGS);
			found = found || storeBooleanElement(localname, INCLUDE_NULL_CHAR);
			found = found
					|| storeBooleanElement(localname, INCLUDE_NON_ZERO_ONE_BOOL);
			found = found
					|| storeBooleanElement(localname,
							INCLUDE_NAN_INFINITE_FLOATS);
			found = found || storeLongElement(localname, DESIRED_FILE_SIZE);
			found = found || storeFloatElement(localname, INVALID_ENUM_PERCENT);
			found = found
					|| storeIntegerElement(localname, DESIRED_REPORT_INTERVAL);
			found = found
					|| storeTraversalTypeElement(localname,
							INTEGER_TRAVERSAL_TYPE);
			/*
			 * MPCS-6864 - 11/21/14. Added desired number of packets
			 * parsing.
			 */
			found = found
					|| storeIntegerElement(localname, DESIRED_NUM_PACKETS);

			/*
			 * MPCS-6340 - 7/2/14. Parsing of float traversal type was
			 * missing.
			 */
			found = found
					|| storeTraversalTypeElement(localname,
							FLOAT_TRAVERSAL_TYPE);
			found = found
					|| storeTraversalTypeElement(localname,
							UNSIGNED_TRAVERSAL_TYPE);
			found = found
					|| storeTraversalTypeElement(localname, ENUM_TRAVERSAL_TYPE);
			found = found
					|| storeTraversalTypeElement(localname, BOOL_TRAVERSAL_TYPE);
			found = found
					|| storeTraversalTypeElement(localname,
							STRING_TRAVERSAL_TYPE);
			found = found || storeFloatElement(localname, DESIRED_FILL_PERCENT);

			 /*
             * MPCS-TBD - 01/02/18. Added desired number of files
             * parsing.
             */
            found = found
                    || storeIntegerElement(localname, DESIRED_NUM_FILES);
            
			/*
			 * If we got to here and the element is not found, only string
			 * properties are left. Just shove those into the property table. No
			 * special parsing required.
			 */
			if (!found && !getBufferText().isEmpty()) {
				AbstractRunConfiguration.this.configProperties.put(localname,
						getBufferText());
			}
		}
	}

}
