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
package ammos.datagen.mission.nsyt.config;

import java.io.File;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import ammos.datagen.config.AbstractRunConfiguration;
import ammos.datagen.config.IRunConfiguration;
import ammos.datagen.config.InvalidConfigurationException;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.xml.XmlUtility;

/**
 * This is the NsytContinuousConfiguration class for the AMPCS data generators.
 * It parses an XML file that contains configuration information that may change
 * from run to run of the NSYT continuous packet generator. The XML file
 * containing the configuration is verified against its schema before loading
 * it. After the XML file is loaded, the configuration values it contained are
 * available using various accessor methods.
 * 
 *
 * MPCS-6864 - 11/21/14. Added class.
 * 
 */
public class ContinuousRunConfiguration extends AbstractRunConfiguration
		implements IRunConfiguration {

	/**
	 * Default 32-bit unsigned seed table, which for these types of packets,
	 * must contain only 24-bit data
	 */
	public static final String DEFAULT_32_BIT_TABLE = "config/Default24UnsignedTable.txt";

	private static final String SCHEMA_RELATIVE_PATH = "schema/mission/NsytContinuousRunConfig.rnc";
	private static final Tracer log = TraceManager.getDefaultTracer();


	/** Packet APID XML property. (Unsigned) */
	public static final String APID = "Apid";

	/** Packet configuration File ID XML property. (Unsigned) */
	public static final String CONFIGURATION_FILE_ID = "ConfigurationFileId";

	/**
	 * Blob properties directory XML property. XML will have either blob
	 * directory or blob properties file, not both. (String)
	 */
	public static final String BLOB_DIRECTORY = "BlobDirectory";

	/**
	 * Blob properties file XML property. XML will have either blob directory or
	 * blob properties file, not both. (String)
	 */
	public static final String BLOB_FILE = "BlobPropertyFile";

	/**
	 * Minimum number of samples XML property (used for uncompressed packets
	 * only, unsigned int)
	 */
	public static final String MIN_SAMPLES = "MinSamples";

	/**
	 * Maximum number of samples XML property (used for uncompressed packets
	 * only, unsigned int)
	 */
	public static final String MAX_SAMPLES = "MaxSamples";

	/**
	 * Starting LOBT timestamp XML property (for SEIS packets only, unsigned
	 * long)
	 */
	public static final String INITIAL_LOBT = "InitialLobt";

	/** LOBT delta time XML property (for SEIS packets only, long) */
	public static final String LOBT_DELTA = "LobtDelta";

	/**
	 * Starting AOBT coarse timestamp XML property (for APSS packets only,
	 * unsigned long)
	 */
	public static final String INITIAL_AOBT_COARSE = "InitialAobtCoarse";

	/**
	 * Starting AOBT fine timestamp XML property (for APSS packets only,
	 * unsigned int)
	 */
	public static final String INITIAL_AOBT_FINE = "InitialAobtFine";

	/**
	 * AOBT coarse delta time XML property (for APSS packets only, long)
	 */
	public static final String AOBT_DELTA_COARSE = "AobtDeltaCoarse";

	/** AOBT fine delta time XML property (for APSS packets only, int) */
	public static final String AOBT_DELTA_FINE = "AobtDeltaFine";

	/** Instrument time format property (InstrumentTimeFormat) */
	public static final String TIME_FORMAT = "TimeFormat";

	/** Compressed/uncompressed packet flag property. (Boolean) */
	public static final String IS_COMPRESSED = "IsCompressed";

	private static final String CHANNEL_ID = "ChannelId";
	private static final String LOBT_SEED_DATA = "LobtSeedData";
	private static final String AOBT_SEED_DATA = "AobtSeedData";
	private static final String INITIAL_AOBT = "InitialAobt";
	private static final String AOBT_DELTA = "AobtDelta";
	private static final String COARSE = "coarse";
	private static final String FINE = "fine";
	private static final String COMPRESSED_SEED_DATA = "CompressedSampleSeedData";
	private static final String UNCOMPRESSED_SEED_DATA = "UncompressedSampleSeedData";

	private final List<Integer> channelIds = new LinkedList<Integer>();

	/**
	 * Constructor. Instance is useless until load() is invoked.
	 */
	public ContinuousRunConfiguration() {

		super("Nsyt Continuous Run Configuration", SCHEMA_RELATIVE_PATH);
	}

	/**
	 * Loads the configuration file.
	 * 
	 * @param uri
	 *            the path to the configuration file.
	 * 
	 * @return true if the file was successfully loaded, false if not.
	 */
	public synchronized boolean load(final String uri) {

		boolean ok = super.load(uri, new RunConfigurationParseHandler());
		if (ok) {
			try {
				/*
				 * Both the AOBT coarse and fine deltas must have the same sign.
				 */
				if (getTimeFormat() == InstrumentTimeFormat.AOBT) {
					final long cd = this.getLongProperty(AOBT_DELTA_COARSE, 0);
					final long fd = this.getLongProperty(AOBT_DELTA_FINE, 0);

					if ((cd > 0 && fd < 0) || (cd < 0 && fd > 0)) {
						throw new InvalidConfigurationException(
								"Both AOBT deltas in the run configuration (coarse and fine) must have the same sign (positive or negative)");
					}
				}

				/*
				 * Blob file or directory must exist if packet format is
				 * compressed. Also, blob paths may be absolute or relative to
				 * the config dir. If relative, we want to adjust these values
				 * to state the full path.
				 */
				if (getBooleanProperty(IS_COMPRESSED, false)) {
					String path = getStringProperty(BLOB_FILE, null);
					if (path != null) {

						if (!path.startsWith(File.separator)) {
							path = GdsSystemProperties
									.getMostLocalPath(path);
						}

						final File f = path == null ? null : new File(path);
						if (f == null || !f.exists() || !f.isFile()) {
							throw new InvalidConfigurationException(
									"Blob property file "
											+ getStringProperty(BLOB_FILE, null)
											+ " in the run configuration does not exist or is not a file");
						} else {
							this.configProperties.put(BLOB_FILE, path);
						}
					} else {
						path = getStringProperty(BLOB_DIRECTORY, null);

						if (!path.startsWith(File.separator)) {
							path = GdsSystemProperties
									.getMostLocalPath(path);
						}

						final File f = path == null ? null : new File(path);
						if (f == null || !f.exists() || !f.isDirectory()) {
							throw new InvalidConfigurationException(
									"Blob directory "
											+ getStringProperty(BLOB_DIRECTORY,
													null)
											+ " in the run configuration does not exist or is not a directory");
						} else {
							this.configProperties.put(BLOB_DIRECTORY, path);
						}

					}
				} else {
					/* If uncompressed, min samples must be <= max samples */
					final int min = getIntProperty(MIN_SAMPLES, 0);
					final int max = getIntProperty(MAX_SAMPLES, 0);
					if (max < min) {
						throw new InvalidConfigurationException(
								"Maximum number of samples in the run configuration ("
										+ max
										+ ") must be less than the minumum number of samples ("
										+ min + ")");
					}

				}
			} catch (final InvalidConfigurationException e) {
				log.error(e.getMessage());
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
	public synchronized void clear() {

		super.clear();
		this.channelIds.clear();
	}

	/**
	 * Gets the list of instrument channel IDs created from the configuration.
	 * 
	 * @return List of sorted Integer objects
	 */
	public synchronized List<Integer> getChannelIds() {

		final List<Integer> l = new LinkedList<Integer>(this.channelIds);
		return Collections.unmodifiableList(l);
	}

	/**
	 * Establish defaults for properties that require them (have non-zero or
	 * non-null default values).
	 */
	@Override
	protected synchronized void setDefaultProperties() {

		super.setDefaultProperties();

		this.configProperties.put(CONFIGURATION_FILE_ID, 0);
		this.configProperties.put(MIN_SAMPLES, 1);
		this.configProperties.put(MAX_SAMPLES, 1024);
		this.configProperties.put(IRunConfiguration.UNSIGNED_32_SEED_TABLE,
				DEFAULT_32_BIT_TABLE);
	}

	/**
	 * Gets the time format for the instrument times in packets.
	 * 
	 * @return InstrumentTimeFormat enum value
	 */
	public InstrumentTimeFormat getTimeFormat() {
		return (InstrumentTimeFormat) this.configProperties.get(TIME_FORMAT);
	}

	/**
	 * This is the SAX parse handler class for the NYST continuous packet
	 * generator run configuration file.
	 * 
	 *
	 */
	class RunConfigurationParseHandler extends
			AbstractRunConfigurationParseHandler {

		/**
		 * {@inheritDoc}
		 */
		@Override
		public void startElement(final String namespaceURI,
				final String localname, final String rawName,
				final Attributes atts) throws SAXException {

			super.startElement(namespaceURI, localname, rawName, atts);

			if (localname.equalsIgnoreCase(LOBT_SEED_DATA)) {
				ContinuousRunConfiguration.this.configProperties.put(
						TIME_FORMAT, InstrumentTimeFormat.LOBT);
			} else if (localname.equalsIgnoreCase(AOBT_SEED_DATA)) {
				ContinuousRunConfiguration.this.configProperties.put(
						TIME_FORMAT, InstrumentTimeFormat.AOBT);
			} else if (localname.equalsIgnoreCase(INITIAL_AOBT)) {
				final long coarse = XmlUtility.getUnsignedIntFromAttr(atts,
						COARSE);
				final long fine = XmlUtility.getUnsignedIntFromAttr(atts, FINE);
				ContinuousRunConfiguration.this.configProperties.put(
						INITIAL_AOBT_COARSE, coarse);
				ContinuousRunConfiguration.this.configProperties.put(
						INITIAL_AOBT_FINE, (int) fine);
			} else if (localname.equalsIgnoreCase(AOBT_DELTA)) {
				final long coarse = XmlUtility.getIntFromAttr(atts, COARSE);
				final long fine = XmlUtility.getIntFromAttr(atts, FINE);
				ContinuousRunConfiguration.this.configProperties.put(
						AOBT_DELTA_COARSE, coarse);
				ContinuousRunConfiguration.this.configProperties.put(
						AOBT_DELTA_FINE, (int) fine);
			} else if (localname.equalsIgnoreCase(COMPRESSED_SEED_DATA)) {
				ContinuousRunConfiguration.this.configProperties.put(
						IS_COMPRESSED, true);
			} else if (localname.equalsIgnoreCase(UNCOMPRESSED_SEED_DATA)) {
				ContinuousRunConfiguration.this.configProperties.put(
						IS_COMPRESSED, false);
			}
		}


		/**
		 * {@inheritDoc}
		 */
		@Override
		public void endElement(final String namespaceURI,
				final String localname, final String rawName)
				throws SAXException {

			if (localname.equalsIgnoreCase(CHANNEL_ID)) {
				final String channel = this.getBufferText();
				ContinuousRunConfiguration.this.channelIds.add(Integer
						.parseInt(channel));
				return;
			}

			boolean found = storeIntegerElement(localname, MIN_SAMPLES);
			found = found || storeIntegerElement(localname, MAX_SAMPLES);
			found = found || storeIntegerElement(localname, APID);
			found = found || storeLongElement(localname, INITIAL_LOBT);
			found = found || storeLongElement(localname, LOBT_DELTA);
			found = found
					|| storeIntegerElement(localname, CONFIGURATION_FILE_ID);

			// If we got to here and the element is not found, let the
			// super class handle it.

			if (!found) {
				super.endElement(namespaceURI, localname, rawName);
			}
		}
	}
}
