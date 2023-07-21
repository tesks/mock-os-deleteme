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

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import ammos.datagen.generators.seeds.DeltaSclkGeneratorSeed;
import ammos.datagen.generators.seeds.EnumGeneratorSeed;
import ammos.datagen.generators.seeds.FloatGeneratorSeed;
import ammos.datagen.generators.seeds.IBasicFieldSeedHolder;
import ammos.datagen.generators.seeds.ISeedData;
import ammos.datagen.generators.seeds.IntegerGeneratorSeed;
import ammos.datagen.generators.seeds.StringGeneratorSeed;
import ammos.datagen.mission.nsyt.config.SteimCompressedBlobConfig;

/**
 * This is the seed class for the NSYT SEIS/APPS continuous packet body
 * generator. It contains all the data necessary to initialize the
 * ContinuousBodyGenerator.
 * 
 *
 * MPCS-6864 - 12/1/14. Added class.
 */
public class ContinuousBodyGeneratorSeed implements ISeedData,
		IBasicFieldSeedHolder {

	private int configFileId;
	private List<Integer> channelIds = new LinkedList<Integer>();
	private boolean isCompressed;
	private List<SteimCompressedBlobConfig> blobConfigs = new LinkedList<SteimCompressedBlobConfig>();
	private int minSamples;
	private int maxSamples;
	private IntegerGeneratorSeed unsignedSeed;
	private ISeedData instrumentTimeSeed;

	/**
	 * Gets the configuration file ID to write to packets.
	 * 
	 * @return the file ID (unsigned)
	 */
	public int getConfigFileId() {
		return this.configFileId;
	}

	/**
	 * Sets the configuration file ID to write to packets.
	 * 
	 * @param configFileId
	 *            the file ID to set (unsigned)
	 */
	public void setConfigFileId(final int configFileId) {
		if (configFileId < 0) {
			throw new IllegalArgumentException("config file ID must be >= 0");
		}
		this.configFileId = configFileId;
	}

	/**
	 * Gets the list of instrument channel IDs to use when writing packets.
	 * 
	 * @return non-modifiable list of integer channel IDs (unsigned); never
	 *         empty or null
	 */
	public List<Integer> getChannelIds() {
		return Collections.unmodifiableList(this.channelIds);
	}

	/**
	 * Sets the list of instrument channel IDs to use when writing packets.
	 * 
	 * @param channelIds
	 *            list of integer channel IDs (unsigned) to set; never empty or
	 *            null
	 */
	public void setChannelIds(final List<Integer> channelIds) {
		if (channelIds == null || channelIds.isEmpty()) {
			throw new IllegalArgumentException(
					"channel ID list cannot be null or empty");
		}
		this.channelIds = channelIds;
	}

	/**
	 * Gets the flag indicating whether packet sample data will be compressed.
	 * 
	 * @return true if data is to be compressed, false if uncompressed
	 */
	public boolean isCompressed() {
		return this.isCompressed;
	}

	/**
	 * Sets the flag indicating whether packet sample data will be compressed.
	 * 
	 * @param isCompressed
	 *            true if data is to be compressed, false if uncompressed
	 */
	public void setCompressed(final boolean isCompressed) {
		this.isCompressed = isCompressed;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * This class supports only 4-byte unsigned seeds. Will throw for other byte
	 * sizes.
	 * 
	 * @see ammos.datagen.generators.seeds.IBasicFieldSeedHolder#getUnsignedSeed(int)
	 */
	@Override
	public IntegerGeneratorSeed getUnsignedSeed(final int byteSize) {
		if (byteSize != 4) {
			throw new IllegalArgumentException(
					"Requesting unsigned seed generator for unsupported byte size");
		}

		return this.unsignedSeed;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * This class supports only 4-byte unsigned seeds. Will do nothing for other
	 * byte sizes.
	 * 
	 * @see ammos.datagen.generators.seeds.IBasicFieldSeedHolder#setUnsignedSeed(ammos.datagen.generators.seeds.IntegerGeneratorSeed,
	 *      int)
	 * 
	 */
	@Override
	public void setUnsignedSeed(final IntegerGeneratorSeed unsignedSeed,
			final int byteSize) {

		if (unsignedSeed == null) {
			throw new IllegalArgumentException("integer seed may not be null");
		}
		if (byteSize != 4) {
			return;
		}
		this.unsignedSeed = unsignedSeed;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.generators.seeds.IBasicFieldSeedHolder#getEnumSeeds()
	 */
	@Override
	public Map<String, EnumGeneratorSeed> getEnumSeeds() {
		throw new UnsupportedOperationException(
				"This seed class does not support enum seeds");
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.generators.seeds.IBasicFieldSeedHolder#setEnumSeeds(java.util.Map)
	 */
	@Override
	public void setEnumSeeds(final Map<String, EnumGeneratorSeed> enumSeeds) {
		throw new UnsupportedOperationException(
				"This seed class does not support enum seeds");

	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.generators.seeds.IBasicFieldSeedHolder#getIntegerSeed(int)
	 */
	@Override
	public IntegerGeneratorSeed getIntegerSeed(final int byteSize) {
		throw new UnsupportedOperationException(
				"This seed class does not support integer seeds");
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.generators.seeds.IBasicFieldSeedHolder#setIntegerSeed(ammos.datagen.generators.seeds.IntegerGeneratorSeed,
	 *      int)
	 */
	@Override
	public void setIntegerSeed(final IntegerGeneratorSeed integerSeed,
			final int byteSize) {
		throw new UnsupportedOperationException(
				"This seed class does not support integer seeds");
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.generators.seeds.IBasicFieldSeedHolder#getFloatSeed(int)
	 */
	@Override
	public FloatGeneratorSeed getFloatSeed(final int byteSize) {
		throw new UnsupportedOperationException(
				"This seed class does not support float seeds");
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.generators.seeds.IBasicFieldSeedHolder#setFloatSeed(ammos.datagen.generators.seeds.FloatGeneratorSeed,
	 *      int)
	 */
	@Override
	public void setFloatSeed(final FloatGeneratorSeed floatSeed,
			final int byteSize) {
		throw new UnsupportedOperationException(
				"This seed class does not support float seeds");
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.generators.seeds.IBasicFieldSeedHolder#setStringSeed(ammos.datagen.generators.seeds.StringGeneratorSeed)
	 */
	@Override
	public void setStringSeed(final StringGeneratorSeed strSeed) {
		throw new UnsupportedOperationException(
				"This seed class does not support string seeds");
	}

	/**
	 * Gets the list of compressed blob configuration objects. These define
	 * compressed data to write to compressed packets.
	 * 
	 * @return non-modifiable list of CompressedBlobConfig; will be empty if no
	 *         blobs defined, but never null
	 */
	public List<SteimCompressedBlobConfig> getBlobConfigs() {
		return Collections.unmodifiableList(this.blobConfigs);
	}

	/**
	 * Sets the list of compressed blob configuration objects. These define
	 * compressed data to write to compressed packets.
	 * 
	 * @return list of CompressedBlobConfig objects to set; may be empty, but
	 *         never null
	 */
	public void setBlobConfigs(final List<SteimCompressedBlobConfig> blobConfigs) {
		if (blobConfigs == null) {
			throw new IllegalArgumentException(
					"blob config list cannot be null");
		}
		this.blobConfigs = blobConfigs;
	}

	/**
	 * Gets the generator seed for the instrument time.
	 * 
	 * @return instrument seed data object; will be an instance of
	 *         DeltaSclkGeneratorSeed or DeltaLobtGeneratorSeed
	 */
	public ISeedData getInstrumentTimeSeed() {
		return this.instrumentTimeSeed;
	}

	/**
	 * Sets the generator seed for the instrument time.
	 * 
	 * @return instrument seed data object to set; cannot be null, and must be
	 *         an instance of DeltaSclkGeneratorSeed or DeltaLobtGeneratorSeed
	 */
	public void setInstrumentTimeSeed(final ISeedData instrumentTimeSeed) {
		if (instrumentTimeSeed == null) {
			throw new IllegalArgumentException("time seed cannot be null");
		}
		if (!(instrumentTimeSeed instanceof DeltaSclkGeneratorSeed || instrumentTimeSeed instanceof DeltaLobtGeneratorSeed)) {
			throw new IllegalArgumentException(
					"time seed is of the wrong class");
		}
		this.instrumentTimeSeed = instrumentTimeSeed;
	}

	/**
	 * Gets the minimum number of samples to write to each packet. Used only for
	 * uncompressed packets.
	 * 
	 * @return minimum number of samples
	 */
	public int getMinSamples() {
		return this.minSamples;
	}

	/**
	 * Sets the minimum number of samples to write to each packet. Used only for
	 * uncompressed packets.
	 * 
	 * @param minSamples
	 *            minimum number of samples (must be > 1)
	 */
	public void setMinSamples(final int minSamples) {
		if (minSamples < 1) {
			throw new IllegalArgumentException(
					"minumum number of samples must be >= 1");
		}
		this.minSamples = minSamples;
	}

	/**
	 * Gets the maximum number of samples to write to each packet. Used only for
	 * uncompressed packets.
	 * 
	 * @return maximum number of samples
	 */
	public int getMaxSamples() {
		return this.maxSamples;
	}

	/**
	 * Sets the maximum number of samples to write to each packet. Used only for
	 * uncompressed packets.
	 * 
	 * @param maxSamples
	 *            maximum number of samples (must be > 1)
	 */
	public void setMaxSamples(final int maxSamples) {
		if (maxSamples < 1) {
			throw new IllegalArgumentException(
					"maximum number of samples must be >= 1");
		}
		this.maxSamples = maxSamples;
	}

}
