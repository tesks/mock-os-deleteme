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
import java.util.Set;

import ammos.datagen.config.TraversalType;
import ammos.datagen.generators.seeds.EnumGeneratorSeed;
import ammos.datagen.generators.seeds.IFileSeedData;
import ammos.datagen.generators.seeds.ISeedData;
import ammos.datagen.generators.seeds.IntegerGeneratorSeed;
import ammos.datagen.generators.seeds.InvalidSeedDataException;
import ammos.datagen.generators.util.FieldGeneratorStatistics;
import ammos.datagen.generators.util.GeneratorStatistics;

/**
 * This is the generator class for enumerated values. These values may be used
 * as EVR arguments, channel data numbers, or data product fields. Each
 * generator is associated with one enumeration in the dictionary.
 * 
 *
 */
public class EnumGenerator implements ISeededGenerator {

    private EnumGeneratorSeed seedData;
    private List<Long> enumValues;
    private Iterator<Long> iterator;
    private final Random random = new Random();
    private IntegerGenerator badValueGenerator;
    private final FieldGeneratorStatistics stats = (FieldGeneratorStatistics) GeneratorStatistics
            .getGlobalStatistics();

    /**
     * {@inheritDoc}
     * 
     * @see ammos.datagen.generators.ISeededGenerator#setSeedData(ammos.datagen.generators.seeds.ISeedData)
     */
    @Override
    public void setSeedData(final ISeedData seed)
            throws InvalidSeedDataException, IllegalArgumentException {

        if (!(seed instanceof EnumGeneratorSeed)) {
            throw new IllegalArgumentException(
                    "Seed must be of type EnumGeneratorSeed");
        }
        this.seedData = (EnumGeneratorSeed) seed;
        final Set<Long> temp = this.seedData.getEnumDef().getAllAsSortedMap()
                .keySet();
        this.enumValues = new ArrayList<Long>(temp);
        this.iterator = this.enumValues.iterator();

        // Invalid values are generated using an integer generator and
        // the default integer table.
        if (this.seedData.isUseInvalid()) {
            this.badValueGenerator = new IntegerGenerator();
            final IntegerGeneratorSeed intSeed = new IntegerGeneratorSeed(4,
                    IFileSeedData.DEFAULT_TABLE_NAME);
            // Do not track this generator.
            this.badValueGenerator.setTracking(false);
            this.badValueGenerator.setSeedData(intSeed);
            if (!this.badValueGenerator.load()) {
                throw new InvalidSeedDataException(
                        "Unable to load bad value generator in enum generator");
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
        this.enumValues = null;
        this.iterator = null;
        this.badValueGenerator = null;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Actual return type of this method is Long.
     * 
     * @see ammos.datagen.generators.ISeededGenerator#getNext()
     */
    @Override
    public Object getNext() {

        if (this.seedData == null) {
            throw new IllegalStateException("Enum generator is unseeded.");
        }
        this.stats.incrementTotalEnumCount();
        final Long badValue = generateInvalidValue();
        if (badValue != null) {
            return badValue;
        }
        if (this.enumValues.isEmpty()) {
            this.stats.incrementInvalidEnumCount();
            return Long.valueOf(0);
        }
        if (this.iterator.hasNext()) {
            return Long.valueOf(this.iterator.next().intValue());
        } else {
            this.iterator = this.enumValues.iterator();
            return Long.valueOf(this.iterator.next().intValue());
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Actual return type of this method is Long.
     * 
     * @see ammos.datagen.generators.ISeededGenerator#getRandom()
     */
    @Override
    public Object getRandom() {

        if (this.seedData == null) {
            throw new IllegalStateException("Enum generator is unseeded");
        }

        this.stats.incrementTotalEnumCount();

        if (this.enumValues.isEmpty()) {
            this.stats.incrementInvalidEnumCount();
            return Long.valueOf(0);
        }
        final int r = this.random.nextInt(this.enumValues.size());

        return Long.valueOf(this.enumValues.get(r).intValue());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Actual return type of this method is Long.
     * 
     * @see ammos.datagen.generators.ISeededGenerator#get()
     */
    @Override
    public Object get() {

        if (this.seedData == null) {
            throw new IllegalStateException("Enum generator is unseeded");
        }

        if (this.seedData.getTraversalType().equals(TraversalType.SEQUENTIAL)) {
            return getNext();
        } else {
            return getRandom();
        }
    }

    /**
     * Returns the total number of valid enumeration values this generator will
     * return before looping.
     * 
     * @return count of total values in the enumeration
     */
    public int getCount() {

        return this.enumValues == null ? 0 : this.enumValues.size();
    }

    /**
     * Generates an invalid value, if this generator is configured to do so and
     * the random generator in this method decides to. Desired percentage of
     * invalid values is established by the run configuration.
     * 
     * @return an Long not in the enumeration, or null if no value generated
     */
    private Long generateInvalidValue() {

        if (this.seedData.isUseInvalid() && !this.enumValues.isEmpty()) {
            boolean needInvalid = false;

            if (!this.stats.getAndSetInvalidEnumGenerated(true)) {
                needInvalid = true;
            } else {
                final float rand = this.random.nextFloat() * (float) 100.0;
                if (rand < this.seedData.getInvalidPercent()) {
                    needInvalid = true;
                }
            }
            Integer badVal = null;
            if (needInvalid) {
                while (needInvalid) {

                    badVal = (Integer) this.badValueGenerator.getRandom();
                    if (!this.enumValues.contains(Long.valueOf(badVal))) {
                        needInvalid = false;
                    }
                }
                this.stats.incrementInvalidEnumCount();
                return Long.valueOf(badVal);
            }
        }
        return null;
    }
}
