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
package ammos.datagen.evr.generators;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import ammos.datagen.config.TraversalType;
import ammos.datagen.evr.config.EvrLevel;
import ammos.datagen.evr.config.Opcode;
import ammos.datagen.evr.generators.seeds.EvrBodyGeneratorSeed;
import ammos.datagen.evr.util.EvrGeneratorStatistics;
import ammos.datagen.evr.util.EvrTrackerType;
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
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.evr.IEvrArgumentDefinition;
import jpl.gds.dictionary.api.evr.IEvrDefinition;
import jpl.gds.dictionary.api.evr.IEvrDictionaryFactory;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.types.Pair;

/**
 * This is the data generator for EVR bodies. It creates the portion of an EVR
 * packet from the end of the secondary header onwards. It must be supplied a
 * seed object that tells it which EVRs to generate.
 * 
 *
 */
public class EvrBodyGenerator implements ISeededGenerator {

	private TruthFile truthWriter;
	private EvrBodyGeneratorSeed seedData;
	private List<IEvrDefinition> evrDefs;
	private Iterator<IEvrDefinition> iterator;
	private List<Integer> invalidIds;
	private int invalidIdIterator;
	private List<EvrLevel> evrLevels;
	private int overallSeqNum = 0;
	private String taskName;
	private final Map<String, Long> catSeqNums = new HashMap<String, Long>(8);
	private final EvrGeneratorStatistics stats = (EvrGeneratorStatistics) GeneratorStatistics
			.getGlobalStatistics();
	private final UsageTracker invalidIdTracker = new UsageTracker();

	// Generators
	private final Map<Integer, IntegerGenerator> intGenerators = new HashMap<Integer, IntegerGenerator>(
			4);
	private final Map<Integer, IntegerGenerator> uintGenerators = new HashMap<Integer, IntegerGenerator>(
			4);
	private final Map<Integer, FloatGenerator> floatGenerators = new HashMap<Integer, FloatGenerator>(
			2);
	private final OpcodeGenerator opcodeGen = new OpcodeGenerator();
	private final SeqIdGenerator seqIdGen = new SeqIdGenerator();
	private final Map<String, EnumGenerator> enumGenerators = new HashMap<String, EnumGenerator>();
	private final Random randGenerator = new Random();
	private final StringGenerator stringGenerator = new StringGenerator();
    private final DictionaryProperties dictProps;
    private final IEvrDictionaryFactory evrDictFact;

	/**
	 * Basic constructor.
	 * @param dictProps current dictionary properties object
	 * @param dictFact Evr dictionary factory
	 */
	public EvrBodyGenerator(final DictionaryProperties dictProps, final IEvrDictionaryFactory dictFact) {
         this.dictProps = dictProps;
         this.evrDictFact = dictFact;
	}

	/**
	 * Constructor that sets the truth file writer
	 * @param dictProps current dictionary properties object
	 * @param dictFact Evr dictionary factory
	 * 
	 * @param truthFile
	 *            TruthFile object for writing truth data to
	 */
	public EvrBodyGenerator(final DictionaryProperties dictProps, final TruthFile truthFile, final IEvrDictionaryFactory dictFact) {
		this.truthWriter = truthFile;
        this.dictProps = dictProps;
        this.evrDictFact = dictFact;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.generators.ISeededGenerator#setSeedData(ammos.datagen.generators.seeds.ISeedData)
	 */
	@Override
	public void setSeedData(final ISeedData seed)
			throws InvalidSeedDataException {

		if (!(seed instanceof EvrBodyGeneratorSeed)) {
			throw new IllegalArgumentException(
					"Seed must be of type EvrBodyGeneratorSeed");
		}

		this.seedData = (EvrBodyGeneratorSeed) seed;

		this.evrDefs = this.seedData.getEvrDefs();
		this.evrLevels = this.seedData.getEvrLevels();
		this.iterator = this.evrDefs.iterator();

		final int[] sizes = { 1, 2, 4, 8 };

		for (final int i : sizes) {
			final IntegerGenerator intGen = new IntegerGenerator();
			intGen.setSeedData(this.seedData.getIntegerSeed(i));
			if (!intGen.load()) {
				throw new InvalidSeedDataException("Could not initialize " + i
						+ " byte integer generator");
			}
			this.intGenerators.put(i, intGen);
		}

		for (final int i : sizes) {
			final IntegerGenerator uintGen = new IntegerGenerator();
			uintGen.setSeedData(this.seedData.getUnsignedSeed(i));
			if (!uintGen.load()) {
				throw new InvalidSeedDataException("Could not initialize " + i
						+ " byte unsigned generator");
			}
			this.uintGenerators.put(i, uintGen);
		}

		final int[] floatSizes = { 4, 8 };

		for (final int i : floatSizes) {
			final FloatGenerator floatGen = new FloatGenerator();
			floatGen.setSeedData(this.seedData.getFloatSeed(i));
			if (!floatGen.load()) {
				throw new InvalidSeedDataException("Could not initialize " + i
						+ " byte float generator");
			}
			this.floatGenerators.put(i, floatGen);
		}

		final Map<String, EnumGeneratorSeed> enumSeeds = this.seedData
				.getEnumSeeds();
		final Set<String> keys = enumSeeds.keySet();
		for (final String key : keys) {
			final EnumGenerator enumGen = new EnumGenerator();
			enumGen.setSeedData(enumSeeds.get(key));
			this.enumGenerators.put(key, enumGen);
		}

		this.opcodeGen.setSeedData(this.seedData.getOpcodeSeed());
		this.seqIdGen.setSeedData(this.seedData.getSeqIdSeed());

		this.stringGenerator.setSeedData(this.seedData.getStringSeed());

		this.taskName = String.format("%-6s", this.seedData.getTaskName());

		if (this.seedData.isIncludeInvalidIds()) {
			this.invalidIds = this.seedData.getInvalidIds();
			this.invalidIdIterator = 0;
			this.invalidIdTracker.allocateSlots(this.invalidIds.size());
			UsageTrackerMap.getGlobalTrackers().addTracker(
					EvrTrackerType.INVALID_ID.getMapType(),
					this.invalidIdTracker);
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
		this.evrDefs = null;
		this.invalidIds = null;
		this.overallSeqNum = 0;
		this.catSeqNums.clear();
		this.iterator = null;
		this.invalidIdIterator = 0;
		this.enumGenerators.clear();
		this.intGenerators.clear();
		this.uintGenerators.clear();
		this.floatGenerators.clear();
		this.stringGenerator.reset();
		this.opcodeGen.reset();
		this.taskName = null;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The actual return type of this method is Pair<IEvrDefinition, byte[]>. It
	 * will return null if the selected EVR does not have a configured level.
	 * 
	 * @see ammos.datagen.generators.ISeededGenerator#getNext()
	 */
	@Override
	public Object getNext() {

		if (this.seedData == null || this.evrDefs.isEmpty()) {
			throw new IllegalStateException(
					"EVR body generator is not seeded or contains no EVR definitions");
		}

		final Pair<IEvrDefinition, byte[]> invalidEvr = getInvalidEvr();
		if (invalidEvr != null) {
			return invalidEvr;
		}

		IEvrDefinition defToBuild = null;

		if (this.iterator.hasNext()) {
			defToBuild = this.iterator.next();
		} else {
			this.iterator = this.evrDefs.iterator();
			defToBuild = this.iterator.next();
		}

		// We need to check if this EVR has a defined level
		// If so we return null and let the caller handle it
		final EvrLevel level = getLevel(defToBuild.getLevel());

		if (level == null) {
			TraceManager

					.getDefaultTracer()
					.error("Found EVR level "
							+ defToBuild.getLevel()
							+ " which is not configured in the mission configuration file; skipping EVR");
			return null;

		}

		final byte[] b = getEvrBody(defToBuild);
		return new Pair<IEvrDefinition, byte[]>(defToBuild, b);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The actual return type of this method is Pair<IEvrDefinition, byte[]>. It
	 * will return null if the selected EVR does not have a configured level.
	 * 
	 * @see ammos.datagen.generators.ISeededGenerator#getRandom()
	 */
	@Override
	public Object getRandom() {

		if (this.seedData == null || this.evrDefs.isEmpty()) {
			throw new IllegalStateException(
					"EVR body generator is not seeded or contains no EVR definitions");
		}

		final Pair<IEvrDefinition, byte[]> invalidEvr = getInvalidEvr();
		if (invalidEvr != null) {
			return invalidEvr;
		}

		final int r = this.randGenerator.nextInt(this.evrDefs.size());

		final IEvrDefinition defToBuild = this.evrDefs.get(r);

		// We need to check if this EVR has a defined level.
		// If so we return null and let the caller handle it
		final EvrLevel level = getLevel(defToBuild.getLevel());

		if (level == null) {
			TraceManager

					.getDefaultTracer()
					.error("Found EVR level "
							+ defToBuild.getLevel()
							+ " which is not configured in the mission configuration file; skipping EVR");
			return null;

		}

		final byte[] b = getEvrBody(defToBuild);
		return new Pair<IEvrDefinition, byte[]>(defToBuild, b);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The actual return type of this method is Pair<IEvrDefinition, byte[]>. It
	 * will return null if the selected EVR does not have a configured level.
	 * 
	 * @see ammos.datagen.generators.ISeededGenerator#get()
	 */
	@Override
	public Object get() {

		if (this.seedData == null || this.evrDefs.isEmpty()) {
			throw new IllegalStateException("EVR body generator is unseeded");
		}

		if (this.seedData.getTraversalType().equals(TraversalType.SEQUENTIAL)) {
			return getNext();
		} else {
			return getRandom();
		}
	}

	/**
	 * Finds a specific EVR level object.
	 * 
	 * @param levelName
	 *            the name of the EVR level to find
	 * @return the EvrLevel object, or null if none found.
	 */
	private EvrLevel getLevel(final String levelName) {

		for (final EvrLevel l : this.evrLevels) {
			if (l.getLevelName().equalsIgnoreCase(levelName)) {
				return l;
			}
		}
		return null;
	}

	/**
	 * Generates a binary EVR body for the given EVR definition.
	 * 
	 * @param def
	 *            the EVR definition object
	 * @return array of bytes containing the EVR data
	 */
	private byte[] getEvrBody(final IEvrDefinition def) {

		this.stats.incrementTotalForEvrId(def.getId());

		// We'll need to know if the EVR is fatal
		final EvrLevel level = getLevel(def.getLevel());
		final boolean isFatal = level.isFatal();

		// First build EVR header byte array
		final byte[] bytesForHeader = new byte[19];
		int off = 0;

		final List<String> metadata = new LinkedList<String>();

		// Task ID
		GDR.set_string(bytesForHeader, off, this.taskName);
		off += 6;

		// Event ID
		GDR.set_u32(bytesForHeader, off, def.getId());
		off += 4;

		// Overall EVR sequence number
		final int overallSeq = getNextOverallSequenceNumber();
		GDR.set_u32(bytesForHeader, off, overallSeq);
		off += 4;

		// EVR category sequence number ( per level)
		final long catSeq = getNextCategorySequenceNumber(def);
		GDR.set_u32(bytesForHeader, off, catSeq);
		off += 4;

		// Number of arguments. Fatal stack length always adds an extra
		// byte
		GDR.set_u8(bytesForHeader, off, def.getNargs() + (isFatal ? 1 : 0));
		off += 1;

		// Add the address stack, randomizing length from 1 to 6 addresses
		byte[] bytesForStack = null;
		if (isFatal) {
			final StringBuilder stack = new StringBuilder(256);
			final int depth = this.seedData.getStackDepth() == 0 ? this.randGenerator
					.nextInt(6) + 1 : this.seedData.getStackDepth();
			bytesForStack = new byte[1 + 4 * depth];
			off = 0;
			GDR.set_u8(bytesForStack, off, 4 * depth);
			off += 1;
			for (int i = 1; i <= depth; i++) {
				final Integer address = this.randGenerator.nextInt();
				GDR.set_u32(bytesForStack, off, address);
				off += 4;
				stack.append(String.format("0x%08x", address));
				if (i != depth) {
					stack.append(',');
				}
			}
			metadata.add(stack.toString());
		}

		metadata.add(UnsignedUtil.formatAsUnsigned(catSeq));
		metadata.add(UnsignedUtil.formatAsUnsigned(overallSeq));
		metadata.add(this.taskName.trim());

		// Now create the EVR argument block
		final List<String> argTruth = new LinkedList<String>();
		final byte[] bytesForArgs = createEvrArgumentData(def, argTruth);

		// Update the truth file with EVR metadata and arguments
		writeEvrToTruth(def, metadata, isFatal);
		writeArgumentsToTruth(argTruth);

		// Now build the whole EVR body from the header bytes, stack bytes, and
		// argument bytes
		final byte[] evrBytes = new byte[bytesForHeader.length
				+ (bytesForStack == null ? 0 : bytesForStack.length)
				+ bytesForArgs.length];
		System.arraycopy(bytesForHeader, 0, evrBytes, 0, bytesForHeader.length);
		if (bytesForStack != null) {
			System.arraycopy(bytesForStack, 0, evrBytes, bytesForHeader.length,
					bytesForStack.length);
			System.arraycopy(bytesForArgs, 0, evrBytes, bytesForHeader.length
					+ bytesForStack.length, bytesForArgs.length);
		} else {
			System.arraycopy(bytesForArgs, 0, evrBytes, bytesForHeader.length,
					bytesForArgs.length);
		}

		return evrBytes;
	}

	/**
	 * Generates an invalid EVR if so configured in the seed.
	 * 
	 * @return a Pair of IEvrDefinition and body bytes, or null if no invalid
	 *         EVR was generated.
	 */
	private Pair<IEvrDefinition, byte[]> getInvalidEvr() {

		if (this.seedData.isIncludeInvalidIds() && !this.invalidIds.isEmpty()) {
			boolean needInvalid = false;

			if (!this.stats.getAndSetInvalidIdGenerated(true)) {
				needInvalid = true;
			} else {
				final float rand = this.randGenerator.nextFloat()
						* (float) 100.0;
				if (rand < this.seedData.getInvalidIdPercent()) {
					needInvalid = true;
				}
			}
			if (needInvalid) {
				final int index = this.invalidIdIterator++;
				if (this.invalidIdIterator == this.invalidIds.size()) {
					this.invalidIdIterator = 0;
				}
				final int invalidId = this.invalidIds.get(index);

				// First build EVR header byte array It needs include only the
				// task ID and event ID. The EVR processor in the GDS will read
				// no further.
				final byte[] bytesForHeader = new byte[10];
				int off = 0;

				// Task ID
				GDR.set_string(bytesForHeader, off, this.taskName);
				off += 6;
				// Event ID
				GDR.set_u32(bytesForHeader, off, invalidId);
				off += 4;

				this.stats.incrementTotalForInvalidEvrId(invalidId);

				// Need a dummy EVR definition
				final IEvrDefinition def = evrDictFact.getMultimissionEvrDefinition();
				
				final int levelIndex = this.randGenerator
						.nextInt(this.evrLevels.size());
				def.setId(invalidId);
				def.setLevel(this.evrLevels.get(levelIndex).getLevelName());
				def.setName("UNKNOWN");

				writeEvrToTruth(def, new LinkedList<String>(), false);
				this.invalidIdTracker.markSlot(index);

				return new Pair<IEvrDefinition, byte[]>(def, bytesForHeader);
			}
		}
		return null;
	}

	/**
	 * Gets the next category sequence number for the given EVR definition.
	 * 
	 * @param def
	 *            the IEvrDefinition for the EVR being generated
	 * @return sequence number
	 */
	private long getNextCategorySequenceNumber(final IEvrDefinition def) {

		Long catSeq = this.catSeqNums.get(def.getLevel());
		if (catSeq == null || catSeq.longValue() == 4294967295L) {
			catSeq = Long.valueOf(0);
		} else {
			catSeq = Long.valueOf(catSeq.longValue() + 1);
		}
		this.catSeqNums.put(def.getLevel(), catSeq);

		return catSeq;
	}

	/**
	 * Gets the next overall sequence number.
	 * 
	 * @return sequence number
	 */
	private int getNextOverallSequenceNumber() {

		// Number must roll to 0 rather than going negative for non-RT-ring EVRs
		if (this.overallSeqNum == Integer.MAX_VALUE) {
			this.overallSeqNum = 0;
		} else {
			this.overallSeqNum++;
		}
		return this.overallSeqNum;
	}

	/**
	 * Creates the data bytes for the EVR arguments. Arguments are generated
	 * using appropriate data generators.
	 * 
	 * @param def
	 *            the IEvrDefinition we are creating arguments for
	 * @param argTruth
	 *            truth vector to write argument values to while generating
	 *            bytes
	 * @return array of bytes containing EVR argument data
	 */
	private byte[] createEvrArgumentData(final IEvrDefinition def,
			final List<String> argTruth) {

		/*
		 * MPCS-6206 - 6/1/14. Change EvrArgumentEntry to
		 * IEvrArgumentDefinition (throughout)
		 */
		final List<IEvrArgumentDefinition> args = def.getArgs();
		final byte[] bytesForArgs = new byte[2048]; // this is larger than we
													// can possibly need

		int off = 0;

		if (args != null) {
			for (final IEvrArgumentDefinition arg : args) {
				switch (arg.getType()) {
				case U64:
					GDR.set_u8(bytesForArgs, off, 8);
					off += 1;
					final Long u64ArgVal = (Long) this.uintGenerators.get(8)
							.get();
					GDR.set_u64(bytesForArgs, off, u64ArgVal);
					argTruth.add(UnsignedUtil.formatAsUnsigned(u64ArgVal));
					off += 8;
					break;
				case U32:
					GDR.set_u8(bytesForArgs, off, 4);
					off += 1;
					final Integer u32ArgVal = (Integer) this.uintGenerators
							.get(4).get();
					GDR.set_u32(bytesForArgs, off, u32ArgVal);
					argTruth.add(UnsignedUtil.formatAsUnsigned(u32ArgVal));
					off += 4;
					break;
				case U16:
					GDR.set_u8(bytesForArgs, off, 2);
					off += 1;
					final Short u16ArgVal = (Short) this.uintGenerators.get(2)
							.get();
					GDR.set_u16(bytesForArgs, off, u16ArgVal);
					argTruth.add(UnsignedUtil.formatAsUnsigned(u16ArgVal));
					off += 2;
					break;
				case U8:
					GDR.set_u8(bytesForArgs, off, 1);
					off += 1;
					final Byte u8ArgVal = (Byte) this.uintGenerators.get(1)
							.get();
					GDR.set_u8(bytesForArgs, off, u8ArgVal);
					argTruth.add(UnsignedUtil.formatAsUnsigned(u8ArgVal));
					off += 1;
					break;
				case I64:
					GDR.set_u8(bytesForArgs, off, 8);
					off += 1;
					final Long i64ArgVal = (Long) this.intGenerators.get(8)
							.get();
					GDR.set_i64(bytesForArgs, off, i64ArgVal);
					argTruth.add(String.valueOf(i64ArgVal));
					off += 8;
					break;
				case I32:
					GDR.set_u8(bytesForArgs, off, 4);
					off += 1;
					final Integer i32ArgVal = (Integer) this.intGenerators.get(
							4).get();
					GDR.set_i32(bytesForArgs, off, i32ArgVal);
					argTruth.add(String.valueOf(i32ArgVal));
					off += 4;
					break;
				case I16:
					GDR.set_u8(bytesForArgs, off, 2);
					off += 1;
					final Short i16ArgVal = (Short) this.intGenerators.get(2)
							.get();
					GDR.set_i16(bytesForArgs, off, i16ArgVal);
					argTruth.add(String.valueOf(i16ArgVal));
					off += 2;
					break;
				case I8:
					GDR.set_u8(bytesForArgs, off, 1);
					off += 1;
					final Byte i8ArgVal = (Byte) this.intGenerators.get(1)
							.get();
					GDR.set_i8(bytesForArgs, off, i8ArgVal);
					argTruth.add(String.valueOf(i8ArgVal));
					off += 1;
					break;
				case F32:
					GDR.set_u8(bytesForArgs, off, 4);
					off += 1;
					final Float f32ArgVal = (Float) this.floatGenerators.get(4)
							.get();
					GDR.set_float(bytesForArgs, off, f32ArgVal);
					argTruth.add(String.valueOf((double) f32ArgVal));
					off += 4;
					break;
				case F64:
					GDR.set_u8(bytesForArgs, off, 8);
					off += 1;
					final Double f64ArgVal = (Double) this.floatGenerators.get(
							8).get();
					GDR.set_double(bytesForArgs, off, f64ArgVal);
					argTruth.add(String.valueOf(f64ArgVal));
					off += 8;
					break;
				case VAR_STRING:
					final String stringArgVal = this.stringGenerator.get();
					// Generate a random string length
					GDR.set_u8(bytesForArgs, off, stringArgVal.length());
					off += 1;
					GDR.set_string_no_pad(bytesForArgs, off, stringArgVal);
					argTruth.add(stringArgVal);
					off += stringArgVal.length();
					break;
				case ENUM:
					// Generate a random ordinal within the enum range.Always
					// uses
					// a 32-bit length for the argument
					final String name = arg.getEnumTableName();
					final EnumGenerator gen = this.enumGenerators.get(name);
					final long which = (Long) gen.get();
					GDR.set_u8(bytesForArgs, off, 4);
					off += 1;
					GDR.set_i32(bytesForArgs, off, (int) which);
					argTruth.add(String.valueOf((int) which));
					off += 4;
					break;
				case OPCODE:
					final Opcode opcodeArg = (Opcode) this.opcodeGen.get();
					if (arg.getLength() == 2) {
						GDR.set_u8(bytesForArgs, off, 2);
						off += 1;
						GDR.set_u16(bytesForArgs, off,
								(short) (opcodeArg.getNumber()));
						off += 2;
						argTruth.add(UnsignedUtil
								.formatAsUnsigned((short) opcodeArg.getNumber()));
					} else {
						GDR.set_u8(bytesForArgs, off, 4);
						off += 1;
						GDR.set_u32(bytesForArgs, off,
								(int) (opcodeArg.getNumber()));
						off += 4;
						argTruth.add(UnsignedUtil
								.formatAsUnsigned((int) opcodeArg.getNumber()));
					}
					break;
				case SEQID:
					final int seqIdArg = (Integer) this.seqIdGen.get();
					if (arg.getLength() == 2) {
						GDR.set_u8(bytesForArgs, off, 2);
						off += 1;
						GDR.set_u16(bytesForArgs, off, (short) (seqIdArg));
						off += 2;
						argTruth.add(UnsignedUtil
								.formatAsUnsigned((short) seqIdArg));
					} else {
						GDR.set_u8(bytesForArgs, off, 4);
						off += 1;
						GDR.set_u32(bytesForArgs, off, seqIdArg);
						off += 4;
						argTruth.add(UnsignedUtil.formatAsUnsigned(seqIdArg));
					}
					break;
				default:
					throw new IllegalArgumentException(
							"Unrecognized EVR argument type: " + arg.getType());
				}
			}
		}
		final byte[] finalBytes = new byte[off];

		System.arraycopy(bytesForArgs, 0, finalBytes, 0, off);
		return finalBytes;
	}

	/**
	 * Writes the given EVR and its metadata to the truth file.
	 * 
	 * @param def
	 *            the EVR definition
	 * @param metadata
	 *            list of EVR metadata values
	 * @param isFatal
	 *            true if the EVR is fatal, false if not
	 */
	private void writeEvrToTruth(final IEvrDefinition def,
			final List<String> metadata, final boolean isFatal) {

		writeTruthLine("EVR: " + def.getName() + "," + def.getLevel() + ","
				+ UnsignedUtil.formatAsUnsigned(def.getId()) + ","
				+ getMetadataKeyString(metadata, isFatal) + ","
				+ getMetadataValueString(metadata));
	}

	/**
	 * Writes the given list of EVR arguments to the truth file.
	 * 
	 * @param args
	 *            list of string arguments to the latest EVR generated
	 */
	private void writeArgumentsToTruth(final List<String> args) {

		if (args.isEmpty()) {
			writeTruthLine("No arguments");
			return;
		}
		final StringBuilder sb = new StringBuilder(256);
		for (final String s : args) {
			sb.append(s);
			sb.append(",");
		}
		sb.delete(sb.length() - 1, sb.length());
		writeTruthLine("Arguments: " + sb.toString());
	}

	/**
	 * Gets a string representation of the EVR metadata keys.
	 * 
	 * @param md
	 *            list of string metadata to the latest EVR generated
	 * @param isFatal
	 *            true if the EVR is fatal, false if not
	 */
	private String getMetadataKeyString(final List<String> md,
			final boolean isFatal) {

		if (md.isEmpty()) {
			return "[]";
		} else if (isFatal) {
			return "[(AddressStack),(CategorySequenceId),(SequenceId),(TaskName)]";
		} else {
			return "[(CategorySequenceId),(SequenceId),(TaskName)]";
		}

	}

	/**
	 * Gets a string representation of the EVR metadata values.
	 * 
	 * @param md
	 *            list of string metadata to the latest EVR generated
	 */
	private String getMetadataValueString(final List<String> md) {

		if (md.isEmpty()) {
			return "[]";
		}

		final StringBuilder sb = new StringBuilder(256);
		sb.append('[');
		for (final String s : md) {
			sb.append("(" + s + ")");
			sb.append(",");
		}
		sb.delete(sb.length() - 1, sb.length());
		sb.append("]");
		return sb.toString();
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
