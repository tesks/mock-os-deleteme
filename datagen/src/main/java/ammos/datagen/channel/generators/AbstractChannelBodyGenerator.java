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
package ammos.datagen.channel.generators;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import ammos.datagen.channel.config.ApidRotationCount;
import ammos.datagen.channel.generators.seeds.ChannelBodyGeneratorSeed;
import ammos.datagen.channel.util.ChannelGeneratorStatistics;
import ammos.datagen.channel.util.ChannelTrackerType;
import ammos.datagen.config.TraversalType;
import ammos.datagen.generators.BooleanGenerator;
import ammos.datagen.generators.EnumGenerator;
import ammos.datagen.generators.FloatGenerator;
import ammos.datagen.generators.ISeededGenerator;
import ammos.datagen.generators.IntegerGenerator;
import ammos.datagen.generators.StringGenerator;
import ammos.datagen.generators.seeds.EnumGeneratorSeed;
import ammos.datagen.generators.seeds.ISeedData;
import ammos.datagen.generators.seeds.InvalidSeedDataException;
import ammos.datagen.generators.util.GeneratorStatistics;
import ammos.datagen.generators.util.TruthFile;
import ammos.datagen.generators.util.UsageTracker;
import ammos.datagen.generators.util.UsageTrackerMap;
import ammos.datagen.util.UnsignedUtil;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.shared.gdr.GDR;

/**
 * This is abstract data generator class for creating channel packet bodies when
 * configured to generate pre-channelized packets that are populated with
 * generated data values. It creates the portion of an pre-channelized packet
 * from the end of the secondary header onwards. It must be supplied a seed
 * object that tells it which channels and APIDs to generate. Subclasses handle
 * the selection of channels to generate for each packet APID.
 * 
 */
public abstract class AbstractChannelBodyGenerator implements ISeededGenerator {

	/**
	 * Random number generator for use by this class and subclasses.
	 */
	protected final Random randGenerator = new Random();
	/**
	 * The seed data object for the generator.
	 */
	protected ChannelBodyGeneratorSeed seedData;
	/**
	 * The list of channel definitions for channels to be created by the
	 * generator.
	 */
	protected List<IChannelDefinition> chanDefs;
	/**
	 * The map of channel definitions for channels to be created by the
	 * generator. This is the same as the chanDefs list but can be accessed by
	 * channel ID.
	 */
	protected Map<String, IChannelDefinition> chanMap;

	/**
	 * The global statistics tracker.
	 */
	protected final ChannelGeneratorStatistics stats = (ChannelGeneratorStatistics) GeneratorStatistics
			.getGlobalStatistics();

	private TruthFile truthWriter;
	private List<Integer> invalidIndices;
	private final UsageTracker invalidIndexTracker = new UsageTracker();
	private List<ApidRotationCount> apidCounts;
	private int currentApidPair;
	private int currentApidCount;

	// Generators
	private final Map<Integer, IntegerGenerator> intGenerators = new HashMap<Integer, IntegerGenerator>(
			4);
	private final Map<Integer, IntegerGenerator> uintGenerators = new HashMap<Integer, IntegerGenerator>(
			4);
	private final Map<Integer, FloatGenerator> floatGenerators = new HashMap<Integer, FloatGenerator>(
			2);
	private final Map<String, EnumGenerator> enumGenerators = new HashMap<String, EnumGenerator>();
	private final BooleanGenerator boolGenerator = new BooleanGenerator();
	private final StringGenerator stringGenerator = new StringGenerator();

	/**
	 * Basic constructor.
	 */
	public AbstractChannelBodyGenerator() {

		// do nothing
	}

	/**
	 * Constructor that sets the truth file writer
	 * 
	 * @param truthFile
	 *            TruthFile object for writing truth data to
	 */
	public AbstractChannelBodyGenerator(final TruthFile truthFile) {

		this.truthWriter = truthFile;
	}

    @Override
	public void resetTruthFile(final TruthFile newFile) {
	    this.truthWriter = newFile;
	}
	
	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.generators.ISeededGenerator#setSeedData(ammos.datagen.generators.seeds.ISeedData)
	 */
	@Override
	public void setSeedData(final ISeedData seed)
			throws InvalidSeedDataException {

		if (!(seed instanceof ChannelBodyGeneratorSeed)) {
			throw new IllegalArgumentException(
					"Seed must be of type ChannelBodyGeneratorSeed");
		}

		reset();

		this.seedData = (ChannelBodyGeneratorSeed) seed;

		/*
		 * Get the list of channel definitions from the seed. Save as both list
		 * and hashmap.
		 */
		this.chanDefs = this.seedData.getChannelDefs();
		this.chanMap = new HashMap<String, IChannelDefinition>();
		for (final IChannelDefinition def : this.chanDefs) {
			this.chanMap.put(def.getId(), def);
		}

		/*
		 * Get the packet APID rotation counts from the seed. This tells the
		 * generator which packet APIDs to generate.
		 */
		this.apidCounts = this.seedData.getApidCounts();

		/*
		 * Setup the integer DN generators, one for each supported integer byte
		 * size.
		 */
		final int[] sizes = { 1, 2, 4, 8 };

		for (final int i : sizes) {
			final IntegerGenerator intGen = new IntegerGenerator();
			intGen.setSeedData(this.seedData.getIntegerSeed(i));
			if (!intGen.load()) {
				throw new InvalidSeedDataException("Could not initialize " + i
						+ " byte integer randGenerator");
			}
			this.intGenerators.put(i, intGen);
		}

		/*
		 * Setup the unsigned DN generators, one for each supported integer byte
		 * size.
		 */
		for (final int i : sizes) {
			final IntegerGenerator uintGen = new IntegerGenerator();
			uintGen.setSeedData(this.seedData.getUnsignedSeed(i));
			if (!uintGen.load()) {
				throw new InvalidSeedDataException("Could not initialize " + i
						+ " byte unsigned randGenerator");
			}
			this.uintGenerators.put(i, uintGen);
		}

		/*
		 * Setup the float DN generators, one for each supported float byte
		 * size.
		 */
		final int[] floatSizes = { 4, 8 };

		for (final int i : floatSizes) {
			final FloatGenerator floatGen = new FloatGenerator();
			floatGen.setSeedData(this.seedData.getFloatSeed(i));
			if (!floatGen.load()) {
				throw new InvalidSeedDataException("Could not initialize " + i
						+ " byte float randGenerator");
			}
			this.floatGenerators.put(i, floatGen);
		}

		/*
		 * Setup the generators for enumerated DN values, one per enumeration
		 * generator seed (one per enumeration in the dictionary).
		 */
		final Map<String, EnumGeneratorSeed> enumSeeds = this.seedData
				.getEnumSeeds();
		final Set<String> keys = enumSeeds.keySet();
		for (final String key : keys) {
			final EnumGenerator enumGen = new EnumGenerator();
			enumGen.setSeedData(enumSeeds.get(key));
			this.enumGenerators.put(key, enumGen);
		}

		/*
		 * Setup the generator for boolean DN values.
		 */
		this.boolGenerator.setSeedData(this.seedData.getBooleanSeed());

		/*
		 * Setup the generator for string DN values.
		 */
		this.stringGenerator.setSeedData(this.seedData.getStringSeed());

		/*
		 * If configured to generate invalid channel indices, get the list of
		 * invalid indices from the seed and set up the UsageTracker object for
		 * invalid indices.
		 */
		if (this.seedData.isIncludeInvalidIndices()) {
			this.invalidIndices = this.seedData.getInvalidIndices();
			this.invalidIndexTracker.allocateSlots(this.invalidIndices.size());
			UsageTrackerMap.getGlobalTrackers().addTracker(
					ChannelTrackerType.INVALID_INDEX.getMapType(),
					this.invalidIndexTracker);
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
		this.chanDefs = null;
		this.chanMap = null;
		this.invalidIndices = null;
		this.enumGenerators.clear();
		this.intGenerators.clear();
		this.uintGenerators.clear();
		this.floatGenerators.clear();
		this.boolGenerator.reset();
		this.stringGenerator.reset();
		this.apidCounts = null;
		this.currentApidCount = 0;
		this.currentApidPair = 0;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The actual return type of this method is Pair<Integer, byte[]>, where the
	 * integer is the packet APID and the byte[] is the channel packet body.
	 * 
	 * @see ammos.datagen.generators.ISeededGenerator#get()
	 */
	@Override
	public Object get() {

		if (this.seedData == null || this.chanDefs.isEmpty()) {
			throw new IllegalStateException(
					"channel body generator is unseeded");
		}

		if (this.seedData.getTraversalType().equals(TraversalType.SEQUENTIAL)) {
			return getNext();
		} else {
			return getRandom();
		}
	}

	/**
	 * Generates a binary body in pre-channelized packet form for the given list
	 * of channel definitions. If configured to do so, randomly inserts invalid
	 * channel indices. Updates UsageTracker objects to reflect which samples
	 * (valid and invalid) have been generated.
	 * 
	 * @param chanDefs
	 *            the list of channel definition objects
	 * @return array of bytes containing the channel data
	 */
	protected byte[] getChannelBody(final List<IChannelDefinition> chanDefs) {

		/*
		 * This list will hold many byte arrays, each containing the index/DN
		 * pair for one channel sample.
		 */
		final List<byte[]> sampleChunks = new LinkedList<byte[]>();

		/*
		 * Set flags to indicate whether an invalid index has been added. We
		 * only want to generate one of these per packet body.
		 */
		boolean invalidIndexAdded = false;
		boolean invalidIndexAttempted = false;

		/*
		 * Loop through the incoming list of channel definitions.
		 */
		for (final IChannelDefinition def : chanDefs) {
			byte[] sample = null;

			/*
			 * We attempt invalid index generation only once. Based upon the
			 * configuration, it may or may not succeed.
			 */
			if (!invalidIndexAdded && !invalidIndexAttempted) {
				sample = getInvalidSample();
				invalidIndexAttempted = true;
			}
			if (sample == null) {
				/*
				 * We did not generate a sample with an invalid index. Time to
				 * generate a valid sample for this channel. Here we track the
				 * number of samples we have generated per channel.
				 */
				sample = getSample(def, invalidIndexAdded);
				this.stats.incrementTotalForChannelId(def.getId());

				/*
				 * BUT, if this packet already has an invalid index in it, it
				 * will be impossible for the GDS to process this sample, so we
				 * increment the invalid sample counter for this channel also.
				 */
				if (invalidIndexAdded) {
					this.stats.incrementTotalForInvalidChannelId(def.getId());
				}
			} else {
				/*
				 * We got a sample with an invalid index. Record this fact.
				 */
				invalidIndexAdded = true;
			}

			/*
			 * Either way, the sample byte array goes into the sample chunk
			 * list.
			 */
			sampleChunks.add(sample);
		}

		/*
		 * Now compute the size we need for the whole channel body by adding up
		 * the size of the samples.
		 */
		int neededSize = 0;
		for (final byte[] sample : sampleChunks) {
			neededSize += sample.length;
		}

		/*
		 * Allocate a byte array for the whole thing and copy all the sample
		 * chunks into it, one after the other.
		 */
		final byte[] chanBody = new byte[neededSize];
		int offset = 0;

		for (final byte[] sample : sampleChunks) {
			System.arraycopy(sample, 0, chanBody, offset, sample.length);
			offset += sample.length;
		}

		/*
		 * Update statistics for number of channels per packet.
		 */
		this.stats.updateChannelPacketStatistics(sampleChunks.size());

		return chanBody;
	}

	/**
	 * Gets the bytes representing one valid channel sample (channel index and
	 * DN value) for the channel with the given definition. Also writes the
	 * sample to the truth file, unless the invalidIndexAdded argument is true.
	 * 
	 * @param def
	 *            channel definition from the dictionary
	 * @param invalidIndexAdded
	 *            true if an invalid channel index has already been added to the
	 *            channel body for this packet, false if not
	 * @return byte array containing channel index and DN value
	 */
	@SuppressWarnings({ "fallthrough", "PMD.SwitchDensity" })
	private byte[] getSample(final IChannelDefinition def,
			final boolean invalidIndexAdded) {

		String dnValue = null;

		/*
		 * This array is bigger than we will ever need for a channel sample.
		 * Copy the 16-bit channel index into it as the first thing.
		 */
		final byte[] bytesForSample = new byte[1024];
		int off = GDR.set_u16(bytesForSample, 0, def.getIndex());

		/*
		 * DN Is generated based upon the channel data type.
		 */
		/*  MPCS_6115 - 5/24/14. Added TIME case below. */
		switch (def.getChannelType()) {
		case ASCII:
			/*
			 * String channel. Get a random string length from 1 to channel
			 * length.
			 */
			final int maxLen = def.getSize() / 8;
			String stringArgVal = this.stringGenerator.get();
			int vlen = stringArgVal.length();
			stringArgVal = stringArgVal.substring(0, Math.min(vlen, maxLen));
			vlen = stringArgVal.length();

			/*
			 * Set it into the sample byte array.
			 */
			off += GDR.set_string_no_pad(bytesForSample, off, stringArgVal);
			/*
			 * String channel byte length must match dictionary length, so we
			 * pad the sample array with zeros out to that length.
			 */
			for (int i = 0; i < maxLen - vlen; i++) {
				GDR.set_u8(bytesForSample, off++, 0);
			}
			/*
			 * Save DN value for truth file.
			 */
			dnValue = stringArgVal;
			break;

		case BOOLEAN:
			/*
			 * Boolean channel. Get the value from the boolean generator and set
			 * it into the sample byte array. Note that booleans can be a
			 * variety of sizes, so we have to check and act appropriately.
			 */
			final Short boolVal = (Short) this.boolGenerator.get();
			switch (def.getSize()) {
			case 64:
				GDR.set_u64(bytesForSample, off, boolVal);
				off += 8;
				dnValue = String.valueOf(boolVal);
				break;
			case 32:
				GDR.set_u32(bytesForSample, off, boolVal);
				off += 4;
				dnValue = String.valueOf(boolVal);
				break;
			case 16:
				GDR.set_u16(bytesForSample, off, boolVal);
				off += 2;
				dnValue = String.valueOf(boolVal);
				break;
			case 8:
				GDR.set_u8(bytesForSample, off, boolVal.byteValue());
				dnValue = String.valueOf(boolVal.byteValue());
				off += 1;
				break;
			default:
				throw new IllegalStateException(
						"Unsupported boolean channel size for channel "
								+ def.getId());
			}
			/*
			 * Need to save the DN for the truth file.
			 */
			dnValue = boolVal.toString();
			break;
		case DIGITAL:
		case UNSIGNED_INT:
		case TIME:
			/*
			 * Unsigned channel. Get an unsigned value of the appropriate size
			 * using the matching unsigned generator. Save the formatted DN
			 * Value for the truth file, and set the value into the sample byte
			 * array.
			 */
			switch (def.getSize()) {
			case 64:
				final Long u64ArgVal = (Long) this.uintGenerators.get(8).get();
				GDR.set_u64(bytesForSample, off, u64ArgVal.longValue());
				dnValue = UnsignedUtil.formatAsUnsigned(u64ArgVal);
				off += 8;
				break;
			case 32:
				final Integer u32ArgVal = (Integer) this.uintGenerators.get(4)
						.get();
				GDR.set_u32(bytesForSample, off, u32ArgVal.intValue());
				dnValue = UnsignedUtil.formatAsUnsigned(u32ArgVal);
				off += 4;
				break;
			case 24:
				/*  MPCS-7229 - Correct 24-bit handling */
				final Integer u24ArgVal = (Integer) this.uintGenerators.get(4)
						.get();
				GDR.set_i24(bytesForSample, off, u24ArgVal.intValue());
				off += 3;
				dnValue = UnsignedUtil.formatAsUnsigned(u24ArgVal);
				break;
			case 16:
				final Short u16ArgVal = (Short) this.uintGenerators.get(2)
						.get();
				GDR.set_u16(bytesForSample, off, u16ArgVal);
				off += 2;
				dnValue = UnsignedUtil.formatAsUnsigned(u16ArgVal);
				break;
			case 8:
				final Byte u8ArgVal = (Byte) this.uintGenerators.get(1).get();
				GDR.set_u8(bytesForSample, off, u8ArgVal);
				off += 1;
				dnValue = UnsignedUtil.formatAsUnsigned(u8ArgVal);
				break;
			default:
				throw new IllegalStateException(
						"Unsupported unsigned channel size for channel "
								+ def.getId());
			}
			break;
		case FLOAT:
			/* MPCS-6115 - 5/23/14. Removed check for DOUBLE TYPE above. */
			/*
			 * Float channel. Get a float value of the appropriate size using
			 * the matching float generator. Save the formatted DN Value for the
			 * truth file, and set the value into the sample byte array.
			 */
			if (def.getSize() == 64) {
				final Double f64ArgVal = (Double) this.floatGenerators.get(8)
						.get();
				GDR.set_double(bytesForSample, off, f64ArgVal);
				dnValue = String.valueOf(GDR.get_double(bytesForSample, off));

				off += 8;
			} else {
				final Float f32ArgVal = (Float) this.floatGenerators.get(4)
						.get();
				GDR.set_float(bytesForSample, off, f32ArgVal);
				dnValue = String.valueOf((double) GDR.get_float(bytesForSample,
						off));

				off += 4;
			}
			break;
		case SIGNED_INT:
			/*
			 * Integer channel. Get an integer value of the appropriate size
			 * using the matching integer generator. Save the formatted DN Value
			 * for the truth file, and set the value into the sample byte array.
			 */
			switch (def.getSize()) {
			case 64:
				final Long i64ArgVal = (Long) this.intGenerators.get(8).get();
				GDR.set_i64(bytesForSample, off, i64ArgVal);
				off += 8;
				dnValue = String.valueOf(i64ArgVal);
				break;
			case 32:
				final Integer i32ArgVal = (Integer) this.intGenerators.get(4)
						.get();
				GDR.set_i32(bytesForSample, off, i32ArgVal);
				off += 4;
				dnValue = String.valueOf(i32ArgVal);
				break;
			case 24:
				/* MPCS-7229 - Add 24-bit handling */
				final Integer i24ArgVal = (Integer) this.intGenerators.get(4)
						.get();
				GDR.set_i24(bytesForSample, off, i24ArgVal.intValue());
				off += 3;
				dnValue = UnsignedUtil.formatAsUnsigned(i24ArgVal);
				break;
			case 16:
				final Short i16ArgVal = (Short) this.intGenerators.get(2).get();
				GDR.set_i16(bytesForSample, off, i16ArgVal);
				off += 2;
				dnValue = String.valueOf(i16ArgVal);
				break;
			case 8:
				final Byte i8ArgVal = (Byte) this.intGenerators.get(1).get();
				GDR.set_i8(bytesForSample, off, i8ArgVal);
				off += 1;
				dnValue = String.valueOf(i8ArgVal);
				break;
			default:
				throw new IllegalStateException(
						"Unsupported integer channel size for channel "
								+ def.getId());
			}
			break;
		case STATUS:
			/*
			 * Enum channel. Use the matching enum generator to get a value and
			 * save the DN value for the truth file. Note that enums can be of
			 * several sizes, so appropriate handling is needed when writing the
			 * value to the sample byte array.
			 */
			final String name = def.getLookupTable().getName();
			final EnumGenerator gen = this.enumGenerators.get(name);
			final Long which = (Long) gen.get();
			switch (def.getSize()) {
			case 64:
				GDR.set_u64(bytesForSample, off, which.longValue());
				off += 8;
				dnValue = String.valueOf(which.longValue());
				break;
			case 32:
				GDR.set_u32(bytesForSample, off, which.intValue());
				off += 4;
				dnValue = String.valueOf(which.intValue());
				break;
			case 16:
				GDR.set_u16(bytesForSample, off, which.shortValue());
				off += 2;
				dnValue = String.valueOf(which.shortValue());
				break;
			case 8:
				GDR.set_u8(bytesForSample, off, which.byteValue());
				off += 1;
				dnValue = String.valueOf(which.byteValue());
				break;
			default:
				throw new IllegalStateException(
						"Unsupported enum channel size for channel "
								+ def.getId());
			}
			break;
		default:
			throw new IllegalStateException("Unknown channel type for channel "
					+ def.getId());
		}

		/*
		 * Write the sample to the truth file. If an invalid index has been
		 * added to the packet previously, do not log this sample to truth. The
		 * GDS will abandon processing when it sees the invalid index.
		 */
		if (!invalidIndexAdded) {
			/* 6/4/14 - MPCS-6213. Write out channel title, not name */
			writeSampleToTruth(def.getId(), def.getTitle(), dnValue);
		}

		final byte[] realBytes = new byte[off];
		System.arraycopy(bytesForSample, 0, realBytes, 0, off);
		return realBytes;
	}

	/**
	 * Gets a sample that contains an invalid channel index, if configured to do
	 * so and the random generator in this method decides to. Desired percentage
	 * of invalid index values is established by the run configuration.
	 * 
	 * @return array of bytes containing the invalid sample, or null if none
	 *         generated.
	 */
	@SuppressWarnings("PMD.ReturnEmptyArrayRatherThanNull")
	private byte[] getInvalidSample() {

		/*
		 * Only generate invalid indices if configured to do so.
		 */
		if (this.seedData.isIncludeInvalidIndices()
				&& this.invalidIndices.size() != 0) {
			/*
			 * Start out assuming we do not need an invalid value.
			 */
			boolean needInvalid = false;
			/*
			 * Check to see if we have generated the mandatory one invalid
			 * index. If not, we need to generate one.
			 */
			if (!this.stats.getAndSetInvalidIndexGenerated(true)) {
				needInvalid = true;
			} else {
				/*
				 * We have generated the mandatory invalid index, so any further
				 * invalid values are generated according to the user-requested
				 * percentage.
				 */
				final float rand = this.randGenerator.nextFloat()
						* (float) 100.0;
				if (rand < this.seedData.getInvalidIndexPercent()) {
					needInvalid = true;
				}
			}
			if (needInvalid) {
				/*
				 * We need an invalid index. Randomly choose one from the
				 * configured list of them.
				 */
				final int intrand = this.randGenerator
						.nextInt(this.invalidIndices.size());
				final Integer badIndex = this.invalidIndices.get(intrand);

				/*
				 * Update the usage tracker for invalid indices.
				 */
				this.stats.incrementTotalForInvalidIndex(badIndex);
				this.invalidIndexTracker.markSlot(intrand);

				/*
				 * Create the sample byte array, which consists of the invalid
				 * index and two extra bytes that fake a sample. The size of the
				 * fake sample is not relevant. The GDS will stop processing
				 * when it sees the invalid index.
				 */
				final byte[] result = new byte[4];
				GDR.set_u16(result, 0, badIndex);
				return result;
			}
		}
		return null;
	}

	/**
	 * Writes a channel/DN pair to the truth file.
	 * 
	 * @param chanId
	 *            channel ID string
	 * @param name
	 *            channel name string
	 * @param dnValue
	 *            the DN value of the channel as a string
	 */
	protected void writeSampleToTruth(final String chanId, final String name,
			final Object dnValue) {

		writeTruthLine("Sample: " + chanId + "," + name + "," + dnValue);
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

	/**
	 * Gets the next packet APID, according to the packet rotation counts.
	 * 
	 * @return packet APID
	 */
	protected int getNextApid() {

		/*
		 * Get the rotation counter for the last APID we used.
		 */
		ApidRotationCount currentPair = this.apidCounts
				.get(this.currentApidPair);
		/*
		 * If we have not exceeded the rotation count for that APID, we want to
		 * generate another packet with the same APID.
		 */
		if (this.currentApidCount < currentPair.getCount()) {
			this.currentApidCount++;
			return currentPair.getApid();
		} else {
			/*
			 * Otherwise, move on to the next APID. If we go off the end of the
			 * list of rotation counters, wrap around to the beginning.
			 */
			this.currentApidPair++;
			if (this.currentApidPair == this.apidCounts.size()) {
				this.currentApidPair = 0;
			}
			currentPair = this.apidCounts.get(this.currentApidPair);
			this.currentApidCount = 1;
			return currentPair.getApid();
		}
	}
}
