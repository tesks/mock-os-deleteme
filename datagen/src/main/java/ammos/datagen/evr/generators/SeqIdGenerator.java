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
import ammos.datagen.evr.generators.seeds.SeqIdGeneratorSeed;
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
 * generator the list of valid and invalid seqids, as well as whether invalid
 * seqids should be generated. The class of the objects returned by the
 * getNext() and getRandom() methods is Integer.
 * 
 */
public class SeqIdGenerator implements ISeededGenerator {

	private SeqIdGeneratorSeed seedData;
	private List<Integer> seqids;
	private List<Integer> invalidSeqIds;
	private Iterator<Integer> iterator;
	private final Random random = new Random();
	private final EvrGeneratorStatistics stats = (EvrGeneratorStatistics) (GeneratorStatistics
			.getGlobalStatistics());
	private final UsageTracker validSeqIdTracker = new UsageTracker();
	private final UsageTracker invalidSeqIdTracker = new UsageTracker();

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.generators.ISeededGenerator#setSeedData(ammos.datagen.generators.seeds.ISeedData)
	 */
	@Override
	public void setSeedData(final ISeedData seed) {

		if (!(seed instanceof SeqIdGeneratorSeed)) {
			throw new IllegalArgumentException(
					"Seed must be of type SeqIdGeneratorSeed");
		}
		this.seedData = (SeqIdGeneratorSeed) seed;
		final SortedSet<Integer> sortedSeqIds = new TreeSet<Integer>(
				this.seedData.getValidSeqIds());
		this.seqids = new ArrayList<Integer>(sortedSeqIds);
		this.validSeqIdTracker.allocateSlots(this.seqids.size());
		UsageTrackerMap.getGlobalTrackers()
				.addTracker(EvrTrackerType.VALID_SEQID.getMapType(),
						this.validSeqIdTracker);

		if (this.seedData.isUseInvalid()) {
			final SortedSet<Integer> badSortedSeqIds = new TreeSet<Integer>(
					this.seedData.getInvalidSeqIds());
			this.invalidSeqIds = new ArrayList<Integer>(badSortedSeqIds);

			this.invalidSeqIdTracker.allocateSlots(this.invalidSeqIds.size());
			UsageTrackerMap.getGlobalTrackers().addTracker(
					EvrTrackerType.INVALID_SEQID.getMapType(),
					this.invalidSeqIdTracker);
		}
		this.iterator = this.seqids.iterator();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.generators.ISeededGenerator#reset()
	 */
	@Override
	public void reset() {

		this.seedData = null;
		this.seqids = null;
		this.invalidSeqIds = null;
		this.iterator = null;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * Note: Object type returned by this method is Integer.
	 * 
	 * @see ammos.datagen.generators.ISeededGenerator#getNext()
	 */
	@Override
	public Object getNext() {

		if (this.seedData == null || this.seqids.isEmpty()) {
			throw new IllegalStateException(
					"SeqId generator is not seeded or contains no seqids");
		}

		this.stats.incrementTotalSeqIdCount();

		final Integer badVal = generateInvalidValue();
		if (badVal != null) {
			return badVal;
		}

		Integer val = null;
		if (this.iterator.hasNext()) {
			val = this.iterator.next();
		} else {
			this.iterator = this.seqids.iterator();
			val = this.iterator.next();
		}

		this.validSeqIdTracker.markSlot(this.seqids.indexOf(val));
		return val;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * Note: Object type returned by this method is Integer.
	 * 
	 * @see ammos.datagen.generators.ISeededGenerator#getRandom()
	 */
	@Override
	public Object getRandom() {

		if (this.seedData == null || this.seqids.isEmpty()) {
			throw new IllegalStateException(
					"SeqId generator is not seeded or contains no seqids");
		}

		this.stats.incrementTotalSeqIdCount();

		final Integer badVal = generateInvalidValue();
		if (badVal != null) {
			return badVal;
		}

		final int r = this.random.nextInt(this.seqids.size());
		this.validSeqIdTracker.markSlot(r);

		return this.seqids.get(r);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * The actual return type of this method is Integer.
	 * 
	 * @see ammos.datagen.generators.ISeededGenerator#get()
	 */
	@Override
	public Object get() {

		if (this.seedData == null) {
			throw new IllegalStateException("SeqId generator is unseeded");
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

		if (this.seqids == null) {
			return 0;
		}
		return this.seqids.size();
	}

	/**
	 * Generates an invalid value, if this generator is configured to do so and
	 * the random generator in this method decides to. Desired percentage of
	 * invalid values is established by the run configuration.
	 * 
	 * @return an Integer from the invalid SEQID list, or null if no value
	 *         generated
	 */
	private Integer generateInvalidValue() {

		if (this.seedData.isUseInvalid() && this.invalidSeqIds.size() != 0) {
			boolean needInvalid = false;
			if (!this.stats.getAndSetInvalidSeqIdGenerated(true)) {
				needInvalid = true;
			} else {
				final float rand = this.random.nextFloat() * (float) 100.0;
				if (rand < this.seedData.getInvalidPercent()) {
					needInvalid = true;
				}
			}
			if (needInvalid) {
				final int intrand = this.random.nextInt(this.invalidSeqIds
						.size());
				this.stats.incrementInvalidSeqIdCount();
				this.invalidSeqIdTracker.markSlot(intrand);
				return this.invalidSeqIds.get(intrand);
			}
		}
		return null;
	}
}
