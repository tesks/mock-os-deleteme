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
package ammos.datagen.generators;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import ammos.datagen.config.TraversalType;
import ammos.datagen.generators.seeds.BooleanGeneratorSeed;
import ammos.datagen.generators.seeds.ISeedData;
import ammos.datagen.generators.seeds.InvalidSeedDataException;
import ammos.datagen.generators.util.FieldGeneratorStatistics;
import ammos.datagen.generators.util.GeneratorStatistics;
import ammos.datagen.generators.util.TrackerType;
import ammos.datagen.generators.util.UsageTracker;
import ammos.datagen.generators.util.UsageTrackerMap;

/**
 * This is the generator class for boolean values. These values may be used
 * channel data numbers or data product fields.
 * 
 *
 */
public class BooleanGenerator implements ISeededGenerator {

    private BooleanGeneratorSeed seedData;
    private List<Short> boolValues;
    private Iterator<Short> iterator;
    private final Random random = new Random();
    private final FieldGeneratorStatistics stats = (FieldGeneratorStatistics) GeneratorStatistics
            .getGlobalStatistics();
    private final UsageTracker booleanTracker = new UsageTracker();

    /**
     * {@inheritDoc}
     * 
     * @see ammos.datagen.generators.ISeededGenerator#setSeedData(ammos.datagen.generators.seeds.ISeedData)
     */
    @Override
    public void setSeedData(final ISeedData seed)
            throws InvalidSeedDataException, IllegalArgumentException {

        if (!(seed instanceof BooleanGeneratorSeed)) {
            throw new IllegalArgumentException(
                    "Seed must be of type BooleanGeneratorSeed");
        }
        this.seedData = (BooleanGeneratorSeed) seed;
        this.boolValues = new ArrayList<Short>();
        this.boolValues.add((short) 0);
        this.boolValues.add((short) 1);

        // If configured to add a non zero or one value, generate a random
        // short integer to include in the list of seed values.
        if (this.seedData.isIncludeExtraValue()) {
            int randVal = this.random.nextInt(Short.MAX_VALUE);
            while (randVal == 0 || randVal == 1) {
                randVal = this.random.nextInt(Short.MAX_VALUE);
            }
            this.boolValues.add((short) randVal);
        }

        this.iterator = this.boolValues.iterator();

        UsageTrackerMap.getGlobalTrackers().addTracker(
                TrackerType.BOOLEAN.getMapType(), this.booleanTracker);
        this.booleanTracker.allocateSlots(this.boolValues.size());
    }

    /**
     * {@inheritDoc}
     * 
     * @see ammos.datagen.generators.ISeededGenerator#reset()
     */
    @Override
    public void reset() {

        this.seedData = null;
        this.boolValues = null;
        this.iterator = null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Actual return type of this method is Short, and guaranteed to be positive
     * in sign.
     * 
     * @see ammos.datagen.generators.ISeededGenerator#getNext()
     */
    @Override
    public Object getNext() {

        if (this.seedData == null) {
            throw new IllegalStateException("Boolean generator is unseeded.");
        }
        this.stats.incrementTotalBooleanCount();
        Short o = null;

        if (this.iterator.hasNext()) {
            o = this.iterator.next();
        } else {
            this.iterator = this.boolValues.iterator();
            o = this.iterator.next();
        }

        this.booleanTracker.markSlot(this.boolValues.indexOf(o));

        return o;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Actual return type of this method is Short, and guaranteed to be positive
     * in sign.
     * 
     * @see ammos.datagen.generators.ISeededGenerator#getRandom()
     */
    @Override
    public Object getRandom() {

        if (this.seedData == null) {
            throw new IllegalStateException("Enum generator is unseeded");
        }

        this.stats.incrementTotalBooleanCount();

        if (this.boolValues.isEmpty()) {
            this.stats.incrementInvalidEnumCount();
            return Integer.valueOf(0);
        }
        final int r = this.random.nextInt(this.boolValues.size());

        this.booleanTracker.markSlot(r);

        return Short.valueOf(this.boolValues.get(r).shortValue());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Actual return type of this method is Short, and guaranteed to be positive
     * in sign.
     * 
     * @see ammos.datagen.generators.ISeededGenerator#get()
     */
    @Override
    public Object get() {

        if (this.seedData == null) {
            throw new IllegalStateException("Boolean generator is unseeded");
        }

        if (this.seedData.getTraversalType().equals(TraversalType.SEQUENTIAL)) {
            return getNext();
        } else {
            return getRandom();
        }
    }

    /**
     * Returns the total number of boolean values this generator will return
     * before looping.
     * 
     * @return count of total values
     */
    public int getCount() {

        return this.boolValues == null ? 0 : this.boolValues.size();
    }
}
