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
package ammos.datagen.evr.generators.seeds;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import ammos.datagen.config.TraversalType;
import ammos.datagen.evr.config.EvrLevel;
import ammos.datagen.generators.seeds.EnumGeneratorSeed;
import ammos.datagen.generators.seeds.FloatGeneratorSeed;
import ammos.datagen.generators.seeds.IBasicFieldSeedHolder;
import ammos.datagen.generators.seeds.ISeedData;
import ammos.datagen.generators.seeds.IntegerGeneratorSeed;
import ammos.datagen.generators.seeds.StringGeneratorSeed;
import jpl.gds.dictionary.api.evr.IEvrDefinition;

/**
 * This is the seed data class for the EvrBodyGenerator. It contains all the
 * data necessary to initialize the generator.
 * 
 */
public class EvrBodyGeneratorSeed implements ISeedData, IBasicFieldSeedHolder {

	private List<IEvrDefinition> evrDefs = new LinkedList<IEvrDefinition>();
	private List<EvrLevel> evrLevels = new LinkedList<EvrLevel>();
	private boolean includeInvalidEvrs;
	private boolean includeInvalidIds;
	private float invalidIdPercent;

	private List<Integer> invalidIds = new LinkedList<Integer>();
	private final Map<Integer, IntegerGeneratorSeed> integerSeeds = new HashMap<Integer, IntegerGeneratorSeed>(
			4);
	private final Map<Integer, IntegerGeneratorSeed> unsignedSeeds = new HashMap<Integer, IntegerGeneratorSeed>(
			4);
	private final Map<Integer, FloatGeneratorSeed> floatSeeds = new HashMap<Integer, FloatGeneratorSeed>(
			2);
	private StringGeneratorSeed stringSeed;
	private OpcodeGeneratorSeed opcodeSeed;
	private SeqIdGeneratorSeed seqIdSeed;
	private Map<String, EnumGeneratorSeed> enumSeeds = new HashMap<String, EnumGeneratorSeed>();
	private String taskName;
	private TraversalType traversalType = TraversalType.SEQUENTIAL;
	private int stackDepth;

	/**
	 * Gets the configured depth of the FATAL EVR stack.
	 * 
	 * @return stack depth, or 0 if none configured
	 */
	public int getStackDepth() {

		return this.stackDepth;
	}

	/**
	 * Sets the configured depth of the FATAL EVR stack.
	 * 
	 * @param stackDepth
	 *            stack depth to set, or 0 if none configured.
	 */
	public void setStackDepth(final int stackDepth) {

		if (stackDepth < 0 || stackDepth > 6) {
			throw new IllegalArgumentException(
					"stackDepth must be between 0 and 6");
		}
		this.stackDepth = stackDepth;
	}

	/**
	 * Gets the desired percentage of invalid EVR ID packets to generate.
	 * 
	 * @return percentage
	 */
	public float getInvalidIdPercent() {

		return this.invalidIdPercent;
	}

	/**
	 * Sets the desired percentage of invalid EVR ID packets to generate.
	 * 
	 * @param invalidIdPercent
	 *            percentage to set, between 0 and 100.
	 */
	public void setInvalidIdPercent(final float invalidIdPercent) {

		if (invalidIdPercent < 0.0 || invalidIdPercent > 100.0) {
			throw new IllegalArgumentException(
					"invalid ID percentage must be between 0 abd 100");
		}
		this.invalidIdPercent = invalidIdPercent;
	}

	/**
	 * Gets the list of invalid EVR IDs.
	 * 
	 * @return List of Integer, or an empty list if no invalid IDs configured
	 */
	public List<Integer> getInvalidIds() {

		return Collections.unmodifiableList(this.invalidIds);
	}

	/**
	 * Sets the list of invalid EVR IDs.
	 * 
	 * @param invalidIds
	 *            List of Integer; may not be null
	 */
	public void setInvalidIds(final List<Integer> invalidIds) {

		if (invalidIds == null) {
			throw new IllegalArgumentException(
					"invalid ID list cannot be null; set an empty list");
		}

		this.invalidIds = invalidIds;
	}

	/**
	 * Gets the traversal type for EVR body generation: RANDOM or SEQUENTIAL.
	 * 
	 * @return TraversalType enumeration value
	 */
	public TraversalType getTraversalType() {

		return this.traversalType;
	}

	/**
	 * Gets the traversal type for EVR body generation: RANDOM or SEQUENTIAL.
	 * 
	 * @param traverse
	 *            TraversalType enumeration value; may not be null
	 */
	public void setTraversalType(final TraversalType traverse) {

		if (traverse == null) {
			throw new IllegalArgumentException("traverse may not be null");
		}
		this.traversalType = traverse;
	}

	/**
	 * Gets the task name string to write into EVR bodies.
	 * 
	 * @return task name
	 */
	public String getTaskName() {

		return this.taskName;
	}

	/**
	 * Sets the task name string to write into EVR bodies.
	 * 
	 * @param taskName
	 *            the string to set; may not be null
	 */
	public void setTaskName(final String taskName) {

		if (taskName == null) {
			throw new IllegalArgumentException("taskName cannot be null");
		}
		this.taskName = taskName;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.generators.seeds.IBasicFieldSeedHolder#getEnumSeeds()
	 */
	@Override
	public Map<String, EnumGeneratorSeed> getEnumSeeds() {

		return Collections.unmodifiableMap(this.enumSeeds);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.generators.seeds.IBasicFieldSeedHolder#setEnumSeeds(java.util.Map)
	 */
	@Override
	public void setEnumSeeds(final Map<String, EnumGeneratorSeed> enumSeeds) {

		if (enumSeeds == null) {
			throw new IllegalArgumentException(
					"enumSeeds may not be null. Set an empty map.");
		}

		this.enumSeeds = Collections.unmodifiableMap(enumSeeds);
	}

	/**
	 * Indicates whether to include invalid (corrupted) EVR bodies in the result
	 * file.
	 * 
	 * @return true to generate invalid EVRs, false to not
	 */
	public boolean isIncludeInvalidEvrs() {

		return this.includeInvalidEvrs;
	}

	/**
	 * Sets the flag indicating whether to include invalid (corrupted) EVR
	 * bodies in the result file.
	 * 
	 * @param includeInvalid
	 *            true to generate invalid EVRs, false to not
	 */
	public void setIncludeInvalidEvrs(final boolean includeInvalid) {

		this.includeInvalidEvrs = includeInvalid;
	}

	/**
	 * Indicates whether to include invalid EVR identifiers in the result file.
	 * 
	 * @return true to generate invalid IDs, false to not
	 */
	public boolean isIncludeInvalidIds() {

		return this.includeInvalidIds;
	}

	/**
	 * Sets the flag indicating whether to include invalid EVR identifiers in
	 * the result file.
	 * 
	 * @param includeInvalid
	 *            true to generate invalid IDs, false to not
	 */
	public void setIncludeInvalidIds(final boolean includeInvalid) {

		this.includeInvalidIds = includeInvalid;
	}

	/**
	 * Gets the configured list of EVR levels, which come from the mission
	 * configuration.
	 * 
	 * @return List of EvrLevel objects; returns an empty list if none
	 *         configured
	 */
	public List<EvrLevel> getEvrLevels() {

		return Collections.unmodifiableList(this.evrLevels);
	}

	/**
	 * Sets the configured list of EVR levels, which come from the mission
	 * configuration.
	 * 
	 * @param evrLevels
	 *            the list of EvrLevel objects; set an empty list if none
	 *            configured
	 */
	public void setEvrLevels(final List<EvrLevel> evrLevels) {

		if (evrLevels == null) {
			throw new IllegalArgumentException(
					"evrLevels may not be null. Set an empty list.");
		}

		this.evrLevels = Collections.unmodifiableList(evrLevels);
	}

	/**
	 * Gets the list of EVR definition objects for EVRs to include in the result
	 * file. This list has been filtered per the EVR seed configuration in the
	 * run configuration file.
	 * 
	 * @return List of IEvrDefinition, or the empty list if none configured
	 */
	public List<IEvrDefinition> getEvrDefs() {

		return Collections.unmodifiableList(this.evrDefs);
	}

	/**
	 * Sets the list of EVR definition objects for EVRs to include in the result
	 * file. This list has been filtered per the EVR seed configuration in the
	 * run configuration file.
	 * 
	 * @param evrDefs
	 *            List of IEvrDefinition, or the empty list if none configured
	 */
	public void setEvrDefs(final List<IEvrDefinition> evrDefs) {

		if (evrDefs == null) {
			throw new IllegalArgumentException(
					"evrDefs may not be null. Set an empty list.");
		}
		this.evrDefs = Collections.unmodifiableList(evrDefs);
	}

	/**
	 * Gets the seed data for String argument generation.
	 * 
	 * @return StringGeneratorSeed object
	 */
	public StringGeneratorSeed getStringSeed() {

		return this.stringSeed;
	}

	/**
	 * Sets the seed data for String argument generation.
	 * 
	 * @param stringSeed
	 *            StringGeneratorSeed object to set
	 */
	public void setStringSeed(final StringGeneratorSeed stringSeed) {

		if (stringSeed == null) {
			throw new IllegalArgumentException("string seed may not be null");
		}
		this.stringSeed = stringSeed;
	}

	/**
	 * Gets the seed data for integer argument generation.
	 * 
	 * @param byteSize
	 *            byte size for the integer seed to get: 1, 2, 4, or 8
	 * @return IntegerGeneratorSeed object
	 */
	@Override
	public IntegerGeneratorSeed getIntegerSeed(final int byteSize) {

		return this.integerSeeds.get(byteSize);
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

		if (integerSeed == null) {
			throw new IllegalArgumentException("integer seed may not be null");
		}
		if (byteSize != 1 && byteSize != 2 && byteSize != 4 && byteSize != 8) {
			throw new IllegalArgumentException("invalid byte size");
		}
		this.integerSeeds.put(byteSize, integerSeed);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.generators.seeds.IBasicFieldSeedHolder#getUnsignedSeed(int)
	 */
	@Override
	public IntegerGeneratorSeed getUnsignedSeed(final int byteSize) {

		return this.unsignedSeeds.get(byteSize);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.generators.seeds.IBasicFieldSeedHolder#setUnsignedSeed(ammos.datagen.generators.seeds.IntegerGeneratorSeed,
	 *      int)
	 */
	@Override
	public void setUnsignedSeed(final IntegerGeneratorSeed unsignedSeed,
			final int byteSize) {

		if (unsignedSeed == null) {
			throw new IllegalArgumentException("integer seed may not be null");
		}
		if (byteSize != 1 && byteSize != 2 && byteSize != 4 && byteSize != 8) {
			throw new IllegalArgumentException("invalid byte size");
		}
		this.unsignedSeeds.put(byteSize, unsignedSeed);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.generators.seeds.IBasicFieldSeedHolder#getFloatSeed(int)
	 */
	@Override
	public FloatGeneratorSeed getFloatSeed(final int byteSize) {

		return this.floatSeeds.get(byteSize);
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

		if (floatSeed == null) {
			throw new IllegalArgumentException("float seed may not be null");
		}
		if (byteSize != 4 && byteSize != 8) {
			throw new IllegalArgumentException("invalid byte size");
		}
		this.floatSeeds.put(byteSize, floatSeed);
	}

	/**
	 * Gets the seed data for opcode argument generation.
	 * 
	 * @return OpcodeGeneratorSeed object
	 */
	public OpcodeGeneratorSeed getOpcodeSeed() {

		return this.opcodeSeed;
	}

	/**
	 * Sets the seed data for opcode argument generation.
	 * 
	 * @param opcodeSeed
	 *            OpcodeGeneratorSeed object to set
	 */
	public void setOpcodeSeed(final OpcodeGeneratorSeed opcodeSeed) {

		if (opcodeSeed == null) {
			throw new IllegalArgumentException("opcode seed may not be null");
		}
		this.opcodeSeed = opcodeSeed;
	}

	/**
	 * Gets the seed data for SEQID argument generation.
	 * 
	 * @return SeqIdGeneratorSeed object
	 */
	public SeqIdGeneratorSeed getSeqIdSeed() {

		return this.seqIdSeed;
	}

	/**
	 * Sets the seed data for SEQID argument generation.
	 * 
	 * @param seqIdSeed
	 *            SeqIdGeneratorSeed object to set
	 */
	public void setSeqIdSeed(final SeqIdGeneratorSeed seqIdSeed) {

		if (seqIdSeed == null) {
			throw new IllegalArgumentException("seqid seed may not be null");
		}
		this.seqIdSeed = seqIdSeed;
	}

}
