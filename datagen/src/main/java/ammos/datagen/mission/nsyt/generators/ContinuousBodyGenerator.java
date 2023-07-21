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
package ammos.datagen.mission.nsyt.generators;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

import ammos.datagen.config.InvalidConfigurationException;
import ammos.datagen.generators.ISeededGenerator;
import ammos.datagen.generators.IntegerGenerator;
import ammos.datagen.generators.seeds.ISeedData;
import ammos.datagen.generators.seeds.InvalidSeedDataException;
import ammos.datagen.generators.util.GeneratorStatistics;
import ammos.datagen.generators.util.TruthFile;
import ammos.datagen.generators.util.UsageTracker;
import ammos.datagen.generators.util.UsageTrackerMap;
import ammos.datagen.mission.nsyt.config.SteimCompressedBlobConfig;
import ammos.datagen.mission.nsyt.generators.seeds.ContinuousBodyGeneratorSeed;
import ammos.datagen.mission.nsyt.generators.seeds.DeltaLobtGeneratorSeed;
import ammos.datagen.mission.nsyt.instrument.InstrumentTime;
import ammos.datagen.util.UnsignedUtil;
import jpl.gds.shared.checksum.InternetChecksum;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.sys.SystemUtilities;

/**
 * This is the data generator for NSYT SEIS/APSS continuous packet bodies. It
 * creates the portion of the packet from the end of the secondary header
 * onwards. It must be supplied a seed object that tells it how to generate the
 * packets.
 * <p>
 * The format of the packets generated is described by JPL D-78505-SEIS-FGICD.
 * The code is based upon a September 26, 2014 draft.
 * 
 * MPCS-6864 - 12/2/14. Added class.
 */
public class ContinuousBodyGenerator implements ISeededGenerator {

	private static final int HEADER_LENGTH = 30;
	private static final int TRAILER_LENGTH = 2;

	private TruthFile truthWriter;
	private ContinuousBodyGeneratorSeed seedData;
	private List<SteimCompressedBlobConfig> blobDefs;
	private Iterator<SteimCompressedBlobConfig> blobIterator;
	private Iterator<Integer> idIterator;
	private List<Integer> channelIds;
	private boolean writeCompressed;
	private final ContinuousGeneratorStatistics stats = (ContinuousGeneratorStatistics) GeneratorStatistics
			.getGlobalStatistics();
	private final Random random = new Random();

	// Generators
	private IntegerGenerator uintGenerator;
	private ISeededGenerator timeGenerator;

	// Trackers
	private UsageTracker blobTracker;

	/**
	 * Basic constructor.
	 */
	public ContinuousBodyGenerator() {

		SystemUtilities.doNothing();
	}

	/**
	 * Constructor that sets the truth file writer.
	 * 
	 * @param truthFile
	 *            TruthFile object for writing truth data to
	 */
	public ContinuousBodyGenerator(final TruthFile truthFile) {

		this.truthWriter = truthFile;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.generators.ISeededGenerator#setSeedData(ammos.datagen.generators.seeds.ISeedData)
	 */
	@Override
	public void setSeedData(final ISeedData seed)
			throws InvalidSeedDataException {

		if (!(seed instanceof ContinuousBodyGeneratorSeed)) {
			throw new IllegalArgumentException(
					"Seed must be of type ContinuousBodyGeneratorSeed");
		}

		this.seedData = (ContinuousBodyGeneratorSeed) seed;

		/* Pull basic config values out of the seed data */
		this.writeCompressed = this.seedData.isCompressed();
		this.channelIds = this.seedData.getChannelIds();
		this.idIterator = this.channelIds.iterator();

		/* Create the proper instrument time generator */
		final ISeedData timeSeed = this.seedData.getInstrumentTimeSeed();
		if (timeSeed instanceof DeltaLobtGeneratorSeed) {
			this.timeGenerator = new DeltaLobtGenerator();

		} else {
			this.timeGenerator = new DeltaAobtGenerator();
		}
		this.timeGenerator.setSeedData(timeSeed);

		if (this.writeCompressed) {
			/* compressed packets need blobs of compressed data */
			this.blobDefs = this.seedData.getBlobConfigs();
			this.blobIterator = this.blobDefs.iterator();

			/* Initialize the tracker for blobs */
			this.blobTracker = new UsageTracker();
			this.blobTracker.allocateSlots(this.blobDefs.size());
			UsageTrackerMap.getGlobalTrackers().addTracker(
					ContinuousTrackerType.BLOBS.getMapType(), this.blobTracker);
		} else {
			/* uncompressed packets need an unsigned generator, 32 bit only */
			this.uintGenerator = new IntegerGenerator();
			this.uintGenerator.setSeedData(this.seedData.getUnsignedSeed(4));
			if (!this.uintGenerator.load()) {
				throw new InvalidSeedDataException("Could not initialize " + 4
						+ " byte unsigned generator");
			}
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.generators.ISeededGenerator#reset()
	 */
	@Override
	public void reset() {

		this.seedData = null;
		this.channelIds = null;
		this.blobDefs = null;
		this.writeCompressed = false;
		this.blobIterator = null;
		this.idIterator = null;
		this.uintGenerator = null;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The actual return type of this method is byte[].
	 * 
	 * @see ammos.datagen.generators.ISeededGenerator#getNext()
	 */
	@Override
	public Object getNext() {

		if (this.seedData == null) {
			throw new IllegalStateException(
					"Continuous packet body generator is not seeded");
		}

		byte[] result = null;

		/*
		 * Pick the next channel ID from the configuration. Wrap when the list
		 * is exhausted.
		 */
		int channelId = 0;
		if (!this.idIterator.hasNext()) {
			this.idIterator = this.channelIds.iterator();
		}
		channelId = this.idIterator.next();

		/* Get the next instrument timestamp */
		final InstrumentTime time = (InstrumentTime) this.timeGenerator.get();

		/* Generate the packet body, compressed or uncompressed */
		if (this.writeCompressed) {
			result = generateCompressedBody(channelId, time);
		} else {
			result = generateUncompressedBody(channelId, time);
		}

		/*
		 * Update the instrument time range in the statistics and the instrument
		 * time to the truth file.
		 */
		this.stats.updateInstrumentTime(time);

		writeTruthLine("Instrument Time: " + time.toString());
		return result;

	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * This operation is unsupported for this class.
	 * 
	 * @see ammos.datagen.generators.ISeededGenerator#getRandom()
	 */
	@Override
	public Object getRandom() {

		throw new UnsupportedOperationException(
				"Random generation is not supported by this generator");

	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The actual return type of this method is byte[].
	 * 
	 * @see ammos.datagen.generators.ISeededGenerator#get()
	 */
	@Override
	public Object get() {

		return getNext();
	}

	/**
	 * Generate a compressed packet body.
	 * 
	 * @param channelId
	 *            the instrument channel ID for the packet.
	 * @param time
	 *            the InstrumentTime to be used for this packet
	 * @return a byte array of the packet data
	 */
	private byte[] generateCompressedBody(final int channelId,
			final InstrumentTime time) {

		/*
		 * Find the next Steim-compressed blob that matches this packet's
		 * channel ID
		 */
		final SteimCompressedBlobConfig config = getBlobForChannel(channelId);

		try {
			/* This is the compressed sample data */
			final byte[] blob = config.getBlob();

			/*
			 * Number of Steim deltas is one less than number of samples in the
			 * packet.
			 */
			final int numDeltas = config.getNumberOfDeltas();

			/* Write the header information. */
			final int totalLength = HEADER_LENGTH + blob.length
					+ TRAILER_LENGTH;
			final byte[] result = new byte[totalLength];
			writeHeader(result, channelId, time, numDeltas,
					config.getFirstSampleValue(), config.getLastSampleValue(),
					config.getMoments(), totalLength);

			/* Write the blob. */
			System.arraycopy(blob, 0, result, HEADER_LENGTH, blob.length);

			/*
			 * Write the CRC at the end. Note CRC computation does not include
			 * the CRC.
			 */
			writeCrc(result);

			/*
			 * If the blob has attached truth values, write them to the truth
			 * file.
			 */
			final List<Integer> truthValues = config.getTruthValues();
			if (truthValues != null) {
				for (final Integer val : truthValues) {
					writeSampleToTruth(channelId, val);
				}
			}

			/*
			 * Update statistics for number of channels per packet and number of
			 * samples per channel.
			 */
			this.stats.updateChannelPacketStatistics(numDeltas + 1);
			this.stats.incrementTotalForChannelId(channelId, numDeltas + 1);

			return result;

		} catch (final InvalidConfigurationException e) {
			e.printStackTrace();
			throw new IllegalStateException(
					"Continuous body generator is mis-configured"
							+ e.getMessage());
		}
	}

	/**
	 * Locate a compressed blob that matches the given channel ID. Note that the
	 * application seed maker has already verified that there IS a blob for
	 * every channel ID.
	 * 
	 * @param channelId
	 *            the instrument channel ID for the packet.
	 * @return a matching blob configuration
	 */
	private SteimCompressedBlobConfig getBlobForChannel(final int channelId) {

		/*
		 * We need to search all the blobs for a match to the channel ID, but we
		 * don't know where in the blob sequence we are and it wraps, so I just
		 * count the blobs I have checked until I have checked them all.
		 */
		int count = this.blobDefs.size();

		while (count > 0) {
			/*
			 * Wrap the blob iterator around if needed. Then get the next blob
			 * configuration.
			 */
			if (!this.blobIterator.hasNext()) {
				this.blobIterator = this.blobDefs.iterator();
			}

			final SteimCompressedBlobConfig config = this.blobIterator.next();

			try {
				/*
				 * The blob matches if it either is configured for the desired
				 * channel ID, or it it is configured with no channel IDs. Mark
				 * the blob as used in the usage tracker before returning.
				 */
				if (config.getChannelIdList().isEmpty()
						|| config.getChannelIdList().contains(channelId)) {
					this.blobTracker.markSlot(this.blobDefs.indexOf(config));
					return config;
				}
			} catch (final InvalidConfigurationException e) {
				e.printStackTrace();
				throw new IllegalStateException(
						"Continuous body generator is mis-configured"
								+ e.getMessage());
			}
			count--;
		}
		/*
		 * If we got this far, there is no matching blob and something is really
		 * wrong.
		 */
		throw new IllegalStateException(
				"No blob appears to be configured for channel ID " + channelId);
	}

	/**
	 * Generate an uncompressed packet body.
	 * 
	 * @param channelId
	 *            the instrument channel ID for the packet.
	 * @param time
	 *            the InstrumentTime to be used for this packet
	 * @return a byte array of the packet data
	 */
	private byte[] generateUncompressedBody(final int channelId,
			final InstrumentTime time) {

		/* Randomize the number of samples between the configured min and max */
		final int numSamples = this.random.nextInt(this.seedData
				.getMaxSamples() - this.seedData.getMinSamples() + 1)
				+ this.seedData.getMinSamples();
		/*
		 * All samples are 4 bytes. Compute the sample block size and allocate
		 * space for the whole data content.
		 */
		final int sampleBlockSize = numSamples * 4;
		final int totalLength = HEADER_LENGTH + sampleBlockSize
				+ TRAILER_LENGTH;
		final byte[] result = new byte[totalLength];

		/* First generate the samples and write them into the data block */
		int firstSample = 0;
		int lastSample = 0;
		int offset = HEADER_LENGTH;
		for (int i = 0; i < numSamples; i++) {
			lastSample = (int) this.uintGenerator.get();
			if (i == 0) {
				firstSample = lastSample;
			}
			GDR.set_u32(result, offset, lastSample);

			/* Write a truth record for the sample. */
			writeSampleToTruth(channelId, lastSample);
			offset += 4;
		}

		/*
		 * Now write the header information. Note we have to do this after we
		 * have generated samples because we need some sample information for
		 * the header.
		 */
		writeHeader(result, channelId, time, numSamples - 1, firstSample,
				lastSample, null, totalLength);

		/*
		 * Write the CRC at the end. Note CRC computation does not include the
		 * CRC itself.
		 */
		writeCrc(result);

		/*
		 * Update statistics for number of channels per packet and number of
		 * samples per channel.
		 */
		this.stats.updateChannelPacketStatistics(numSamples);
		this.stats.incrementTotalForChannelId(channelId, numSamples);

		return result;

	}

	/**
	 * Computes the CRC of the given byte buffer and and writes the CRC to the
	 * byte buffer in the trailer position.
	 * 
	 * @param buffer
	 *            the buffer to compute CRC for. Assumed to contain the space
	 *            for the CRC at the end, with CRC bytes set to 0.
	 */
	private void writeCrc(final byte[] buffer) {
		final long checksum = new InternetChecksum().calculateChecksum(buffer, 0,
				buffer.length - TRAILER_LENGTH);
		GDR.set_u16(buffer, buffer.length - TRAILER_LENGTH,
				(int)checksum & 0x0000FFFF);
	}

	/**
	 * Writes the continuous packet header. This is the content of the packet
	 * from offset 0 to the start of the sample block.
	 * 
	 * @param buffer
	 *            the buffer to write the header to, starting at offset 0
	 * @param channelId
	 *            the instrument channel ID for the current packet
	 * @param time
	 *            the InstrumentTime to be used for this packet
	 * @param numDeltas
	 *            the number of deltas in the packet, which is one less than the
	 *            number of samples
	 * @param firstSample
	 *            the value of the first sample
	 * @param lastSample
	 *            the value of the last sample
	 * @param moments
	 *            the Steim-Lite compression moments, or null if packet is not
	 *            compressed
	 * @param totalLength
	 *            the total length of the packet data area
	 */
	private void writeHeader(final byte[] buffer, final int channelId,
			final InstrumentTime time, final int numDeltas,
			final int firstSample, final int lastSample,
			final List<Integer> moments, final int totalLength) {

		int offset = 0;

		/* Write the channel ID */
		GDR.set_u16(buffer, offset, channelId);
		offset += 2;
		/* Write the config file ID from the seed data */
		GDR.set_u16(buffer, offset, this.seedData.getConfigFileId());
		offset += 2;
		/* Write a 0 for event ID, which seems not to be needed by AMPCS */
		GDR.set_u16(buffer, offset, 0);
		offset += 2;
		/*
		 * Get the bytes representing the time, padded to 6 bytes long.
		 */
		final byte[] timeBytes = time.getPaddedBytes(6);
		System.arraycopy(timeBytes, 0, buffer, offset, timeBytes.length);
		offset += 6;

		/* Write the compressed flag. No idea why they needed 16 bits for this. */
		GDR.set_u16(buffer, offset, this.writeCompressed ? 1 : 0);
		offset += 2;

		/*
		 * If uncompressed, Steim moments will be null. Just write 4 bytes of 0.
		 * Otherwise, write the 4 moments.
		 */
		if (moments == null) {
			GDR.set_u32(buffer, offset, 0);
			offset += 4;
		} else {
			for (final int moment : moments) {
				GDR.set_u8(buffer, offset++, moment);
			}
		}

		/* Write the number of deltas */
		GDR.set_u16(buffer, offset, numDeltas);
		offset += 2;

		/* Write the total data length */
		GDR.set_u16(buffer, offset, totalLength);
		offset += 2;

		/* Write the first and last sample values. */
		GDR.set_u32(buffer, offset, firstSample);
		offset += 4;
		GDR.set_u32(buffer, offset, lastSample);
		offset += 4;
	}

	/**
	 * Writes a channel/DN pair to the truth file.
	 * 
	 * @param chanId
	 *            the instrument channel ID
	 * @param dnValue
	 *            the DN value of the channel, unsigned
	 */
	private void writeSampleToTruth(final int chanId, final int dnValue) {
		/*
		 * In order to make this truth data have the same columns as the truth
		 * data for the channel generator, a channel name is needed, but I have
		 * none, so the column is just filled with "Instrument Channel". Also,
		 * all DN values are unsigned and must be formatted as such.
		 */
		final String dnValueStr = UnsignedUtil.formatAsUnsigned(dnValue);
		writeTruthLine("Sample: " + chanId + ",Instrument Channel,"
				+ dnValueStr);
	}

	/**
	 * Writes a line of test to the truth file. Does nothing if no truth file
	 * writer has been set.
	 * 
	 * @param truth
	 *            the truth data to write
	 */
	private void writeTruthLine(final String truth) {

		if (this.truthWriter != null) {
			this.truthWriter.writeLine(truth);
		}
	}
}
