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
package ammos.datagen.mission.nsyt.generators.seeds;

import java.io.File;
import java.io.FilenameFilter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import ammos.datagen.config.GeneralMissionConfiguration;
import ammos.datagen.config.InvalidConfigurationException;
import ammos.datagen.generators.seeds.AbstractSeedMaker;
import ammos.datagen.generators.seeds.DeltaSclkGeneratorSeed;
import ammos.datagen.generators.seeds.IPacketSeedMaker;
import ammos.datagen.generators.seeds.ISeedData;
import ammos.datagen.generators.seeds.PacketHeaderGeneratorSeed;
import ammos.datagen.mission.nsyt.config.ContinuousRunConfiguration;
import ammos.datagen.mission.nsyt.config.InstrumentTimeFormat;
import ammos.datagen.mission.nsyt.config.SteimCompressedBlobConfig;

/**
 * The is the class that manufactures the required generator seeds for the NSYT
 * continuous packet generation application. It is also responsible for
 * validating things about the configuration that the XML parsing cannot.
 * 
 *
 * MPCS-6864 - 12/1/14. Added class.
 */
public class ContinuousSeedMaker extends AbstractSeedMaker implements
		IPacketSeedMaker {

	private final ContinuousRunConfiguration runConfig;
	private List<SteimCompressedBlobConfig> blobConfigs;
	private final boolean isCompressed;

	/**
	 * Constructor.
	 * 
	 * @param missionConfig
	 *            reference to a loaded general mission configuration
	 * @param runConfig
	 *            reference to a loaded NSYT continuous run configuration
	 */
	public ContinuousSeedMaker(final GeneralMissionConfiguration missionConfig,
			final ContinuousRunConfiguration runConfig) {

		super(missionConfig, runConfig);

		this.runConfig = runConfig;

		this.isCompressed = this.runConfig.getBooleanProperty(
				ContinuousRunConfiguration.IS_COMPRESSED, false);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.generators.seeds.AbstractSeedMaker#validate()
	 */
	@Override
	public synchronized void validate() throws InvalidConfigurationException {

		super.validate();

		/*
		 * If generating compressed packets, we must have blobs that are valid
		 * for all channel IDs in the run configuration.
		 */
		if (this.isCompressed) {

			/* Load the compressed blob configurations */
			loadBlobs();

			/*
			 * Gather the list of channel IDs supported by these blobs. If the
			 * ID list is empty for any blob, that blob can be used for any
			 * channel ID.
			 */
			boolean anyChannelSupported = false;
			final List<Integer> channelIdsInBlobs = new LinkedList<Integer>();
			for (final SteimCompressedBlobConfig config : this.blobConfigs) {
				final List<Integer> channelsInBlob = config.getChannelIdList();
				if (channelsInBlob.isEmpty()) {
					anyChannelSupported = true;
				}
				channelIdsInBlobs.addAll(channelsInBlob);
			}

			/*
			 * Now, if there is no blob that supports all channels, make sure
			 * there is at least one blob for each channel ID in the run
			 * configuration.
			 */
			if (!anyChannelSupported) {
				final List<Integer> needChannelIds = this.runConfig
						.getChannelIds();
				for (final Integer id : needChannelIds) {
					if (!channelIdsInBlobs.contains(id)) {
						throw new InvalidConfigurationException(
								"Found channel ID "
										+ id
										+ " in the run configuration, but no blobs are defined for this channel ID");
					}
				}
			}

		}

	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.generators.seeds.IPacketSeedMaker#createPacketHeaderGeneratorSeeds()
	 */
	@Override
	public Map<Integer, PacketHeaderGeneratorSeed> createPacketHeaderGeneratorSeeds() {

		final Map<Integer, PacketHeaderGeneratorSeed> seeds = new HashMap<Integer, PacketHeaderGeneratorSeed>();

		/*
		 * There is only one configured APID in the run configuration, so we
		 * need only one packet header seed.
		 */
		final int apid = this.runConfig.getIntProperty(
				ContinuousRunConfiguration.APID, 0);

		final PacketHeaderGeneratorSeed pseed = new PacketHeaderGeneratorSeed();
		pseed.setApid(apid);
		seeds.put(apid, pseed);

		return seeds;
	}

	/**
	 * Creates a ContinuousBodyGeneratorSeed object from current configuration.
	 * 
	 * @return a ContinuousBodyGeneratorSeed object
	 * 
	 * @throws InvalidConfigurationException
	 *             if the seed cannot be constructed due to a configuration
	 *             issue
	 */
	public synchronized ContinuousBodyGeneratorSeed createContinuousBodyGeneratorSeed()
			throws InvalidConfigurationException {

		final ContinuousBodyGeneratorSeed contSeed = new ContinuousBodyGeneratorSeed();

		contSeed.setChannelIds(this.runConfig.getChannelIds());
		contSeed.setCompressed(this.isCompressed);
		contSeed.setConfigFileId(this.runConfig.getIntProperty(
				ContinuousRunConfiguration.CONFIGURATION_FILE_ID, 0));

		/*
		 * Have to create the instrument-specific time seed required by the
		 * configuration
		 */
		ISeedData timeSeed = getDeltaLobtSeed();
		if (timeSeed == null) {
			timeSeed = getDeltaAobtSeed();
		}
		contSeed.setInstrumentTimeSeed(timeSeed);

		if (this.isCompressed) {
			/* compressed packet generation requires blobs */
			loadBlobs();
			contSeed.setBlobConfigs(this.blobConfigs);
		} else {
			/*
			 * No blobs required, but must have an unsigned seed, min, and max
			 * samples
			 */
			makeUnsignedSeeds(contSeed);
			contSeed.setMinSamples(this.runConfig.getIntProperty(
					ContinuousRunConfiguration.MIN_SAMPLES, 1));
			contSeed.setMaxSamples(this.runConfig.getIntProperty(
					ContinuousRunConfiguration.MAX_SAMPLES, 1));
		}

		return contSeed;
	}

	/**
	 * Loads blob properties and blob data. Builds the list of blob
	 * configuration objects (member variable).
	 * 
	 * @throws InvalidConfigurationException
	 *             if there is an issue loading blobs
	 */
	private synchronized void loadBlobs() throws InvalidConfigurationException {

		/* Blobs are not used for uncompressed packets */
		if (!this.isCompressed) {
			return;
		}

		/* Blobs are cached. If they have been loaded, just return. */
		if (this.blobConfigs != null) {
			return;
		}

		/* First figure out if we have one blob, or a directory of blobs. */
		boolean haveBlobDir = false;
		String blobPath = this.runConfig.getStringProperty(
				ContinuousRunConfiguration.BLOB_FILE, null);
		if (blobPath == null) {
			blobPath = this.runConfig.getStringProperty(
					ContinuousRunConfiguration.BLOB_DIRECTORY, null);
			haveBlobDir = true;
		}

		/* Assemble the blob configuration list */
		this.blobConfigs = new LinkedList<SteimCompressedBlobConfig>();
		if (haveBlobDir) {
			/*
			 * We have a directory of blobs. Locate all the properties files in
			 * that directory.
			 */
			final File dir = new File(blobPath);
			final File files[] = dir.listFiles(new PropertyFileFilter());
			if (files.length == 0) {
				throw new InvalidConfigurationException(
						"No blob properties files found in directory"
								+ blobPath);
			}
			/* Load all the blobs. If any is in error, abort */
			for (final File f : files) {
				final SteimCompressedBlobConfig config = new SteimCompressedBlobConfig(
						f.getPath());
				if (!config.load(true)) {
					throw new InvalidConfigurationException(
							"Aborting due to bad blob properties file: "
									+ f.getPath());
				}
				this.blobConfigs.add(config);
			}

		} else {
			/* We have just one blob. Load it. If any is in error, abort */
			final SteimCompressedBlobConfig config = new SteimCompressedBlobConfig(
					blobPath);
			if (!config.load(true)) {
				throw new InvalidConfigurationException(
						"Aborting due to bad blob properties file " + blobPath);
			}
			this.blobConfigs.add(config);
		}

		/* If compressed, we cannot run if there are no blobs */
		if (this.blobConfigs.isEmpty()) {
			throw new InvalidConfigurationException(
					"Aborting because no valid blobs are configured for compressed packet generation, blob path "
							+ blobPath);
		}
	}

	/**
	 * Creates a LOBT time generator seed.
	 * 
	 * @return the DeltaLobtGeneratorSeed, or null if the time format is not
	 *         LOBT
	 */
	private DeltaLobtGeneratorSeed getDeltaLobtSeed() {
		if (this.runConfig.getTimeFormat() != InstrumentTimeFormat.LOBT) {
			return null;
		}
		final DeltaLobtGeneratorSeed seed = new DeltaLobtGeneratorSeed();
		seed.setStart(this.runConfig.getLongProperty(
				ContinuousRunConfiguration.INITIAL_LOBT, 0));
		seed.setDelta(this.runConfig.getLongProperty(
				ContinuousRunConfiguration.LOBT_DELTA, 0));
		return seed;
	}

	/**
	 * Creates a AOBT time generator seed, which is actually just a SCLK
	 * generator seed, because AOBTs look a lot like SCLKs.
	 * 
	 * @return the DeltaSclkGeneratorSeed, or null if the time format is not
	 *         AOBT
	 */
	private DeltaSclkGeneratorSeed getDeltaAobtSeed() {
		if (this.runConfig.getTimeFormat() != InstrumentTimeFormat.AOBT) {
			return null;
		}
		final DeltaSclkGeneratorSeed seed = new DeltaSclkGeneratorSeed();
		seed.setStartCoarse(this.runConfig.getLongProperty(
				ContinuousRunConfiguration.INITIAL_AOBT_COARSE, 0));
		seed.setDeltaCoarse(this.runConfig.getLongProperty(
				ContinuousRunConfiguration.AOBT_DELTA_COARSE, 0));
		seed.setStartFine(this.runConfig.getIntProperty(
				ContinuousRunConfiguration.INITIAL_AOBT_FINE, 0));
		seed.setDeltaFine(this.runConfig.getIntProperty(
				ContinuousRunConfiguration.AOBT_DELTA_FINE, 0));
		return seed;
	}

	/**
	 * Filter class for locating blob properties files.
	 * 
	 *
	 */
	public static class PropertyFileFilter implements FilenameFilter {

		/**
		 * {@inheritDoc}
		 * 
		 * @see java.io.FilenameFilter#accept(java.io.File, java.lang.String)
		 */
		@Override
		public boolean accept(final File dir, final String name) {
			return name.endsWith(".properties");
		}
	}

}
