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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.SortedSet;
import java.util.TreeSet;

import ammos.datagen.config.TraversalType;
import ammos.datagen.evr.config.Opcode;
import ammos.datagen.evr.generators.seeds.OpcodeGeneratorSeed;
import ammos.datagen.evr.util.EvrGeneratorStatistics;
import ammos.datagen.evr.util.EvrTrackerType;
import ammos.datagen.generators.ISeededGenerator;
import ammos.datagen.generators.seeds.ISeedData;
import ammos.datagen.generators.util.GeneratorStatistics;
import ammos.datagen.generators.util.UsageTracker;
import ammos.datagen.generators.util.UsageTrackerMap;

/**
 * This is an opcode value generator class. The values to be produced by the
 * generator are loaded from the run configuration file. The seed data tells the
 * generator the list of valid and invalid opcodes, as well as whether invalid
 * opcodes should be generated. The class of the objects returned by the
 * getNext() and getRandom() methods is Opcode.
 * 
 * @see ammos.datagen.evr.config.Opcode
 * 
 */
public class OpcodeGenerator implements ISeededGenerator {

	private OpcodeGeneratorSeed seedData;
	private List<Opcode> opcodes;
	private List<Opcode> invalidOpcodes;
	private Iterator<Opcode> iterator;
	private final Random random = new Random();
	private final EvrGeneratorStatistics stats = (EvrGeneratorStatistics) (GeneratorStatistics
			.getGlobalStatistics());
	private final UsageTracker validOpcodeTracker = new UsageTracker();
	private final UsageTracker invalidOpcodeTracker = new UsageTracker();

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.generators.ISeededGenerator#setSeedData(ammos.datagen.generators.seeds.ISeedData)
	 */
	@Override
	public void setSeedData(final ISeedData seed) {

		if (!(seed instanceof OpcodeGeneratorSeed)) {
			throw new IllegalArgumentException(
					"Seed must be of type OpcodeGeneratorSeed");
		}
		this.seedData = (OpcodeGeneratorSeed) seed;
		final SortedSet<Opcode> sortedOpcodes = new TreeSet<Opcode>(
				this.seedData.getValidOpcodes());
		this.opcodes = new ArrayList<Opcode>(sortedOpcodes);
		this.validOpcodeTracker.allocateSlots(this.opcodes.size());
		UsageTrackerMap.getGlobalTrackers().addTracker(
				EvrTrackerType.VALID_OPCODE.getMapType(),
				this.validOpcodeTracker);

		if (this.seedData.isUseInvalid()) {
			final SortedSet<Opcode> badSortedOpcodes = new TreeSet<Opcode>(
					this.seedData.getInvalidOpcodes());
			this.invalidOpcodes = new ArrayList<Opcode>(badSortedOpcodes);
			this.invalidOpcodeTracker.allocateSlots(this.invalidOpcodes.size());
			UsageTrackerMap.getGlobalTrackers().addTracker(
					EvrTrackerType.INVALID_OPCODE.getMapType(),
					this.invalidOpcodeTracker);
		}
		this.iterator = this.opcodes.iterator();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.generators.ISeededGenerator#reset()
	 */
	@Override
	public void reset() {

		this.seedData = null;
		this.opcodes = null;
		this.invalidOpcodes = null;
		this.iterator = null;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * Note: Object type returned by this method is Opcode.
	 * 
	 * @see ammos.datagen.generators.ISeededGenerator#getNext()
	 */
	@Override
	public Object getNext() {

		if (this.seedData == null || this.opcodes.isEmpty()) {
			throw new IllegalStateException(
					"Opcode generator is not seeded or contains no opcodes");
		}

		this.stats.incrementTotalOpcodeCount();

		final Opcode badVal = generateInvalidValue();
		if (badVal != null) {
			return badVal;
		}
		Opcode o = null;
		if (this.iterator.hasNext()) {
			o = this.iterator.next();
		} else {
			this.iterator = this.opcodes.iterator();
			o = this.iterator.next();
		}
		this.validOpcodeTracker.markSlot(this.opcodes.indexOf(o));
		return o;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * Note: Object type returned by this method is Opcode.
	 * 
	 * @see ammos.datagen.generators.ISeededGenerator#getRandom()
	 */
	@Override
	public Object getRandom() {

		if (this.seedData == null || this.opcodes.isEmpty()) {
			throw new IllegalStateException(
					"Opcode generator is not seeded or contains no opcodes");
		}

		this.stats.incrementTotalOpcodeCount();

		final Opcode badVal = generateInvalidValue();
		if (badVal != null) {
			return badVal;
		}

		final int r = this.random.nextInt(this.opcodes.size());

		this.validOpcodeTracker.markSlot(r);

		return this.opcodes.get(r);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The actual return type of this method is Opcode.
	 * 
	 * @see ammos.datagen.generators.ISeededGenerator#get()
	 */
	@Override
	public Object get() {

		if (this.seedData == null) {
			throw new IllegalStateException("Opcode generator is unseeded");
		}

		if (this.seedData.getTraversalType().equals(TraversalType.SEQUENTIAL)) {
			return getNext();
		} else {
			return getRandom();
		}
	}

	/**
	 * Returns the total number of different values this generator will produce
	 * from getNext() before wrapping.
	 * 
	 * @return count of total potential values
	 */
	public int getCount() {

		if (this.opcodes == null) {
			return 0;
		}
		return this.opcodes.size();
	}

	/**
	 * Generates an invalid value, if this generator is configured to do so and
	 * the random generator in this method decides to. Desired percentage of
	 * invalid values is established by the run configuration.
	 * 
	 * @return an Opcode from the invalid opcode list, or null if no value
	 *         generated
	 */
	private Opcode generateInvalidValue() {

		if (this.seedData.isUseInvalid() && this.invalidOpcodes.size() != 0) {
			boolean needInvalid = false;
			if (!this.stats.getAndSetInvalidOpcodeGenerated(true)) {
				needInvalid = true;
			} else {
				final float rand = this.random.nextFloat() * (float) 100.0;
				if (rand < this.seedData.getInvalidPercent()) {
					needInvalid = true;
				}
			}
			if (needInvalid) {
				final int intrand = this.random.nextInt(this.invalidOpcodes
						.size());

				this.stats.incrementInvalidOpcodeCount();
				this.invalidOpcodeTracker.markSlot(intrand);

				return this.invalidOpcodes.get(intrand);
			}
		}
		return null;
	}
}
