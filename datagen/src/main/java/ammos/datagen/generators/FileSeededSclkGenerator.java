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

import ammos.datagen.generators.seeds.FileSeededSclkGeneratorSeed;
import ammos.datagen.generators.seeds.ISeedData;
import ammos.datagen.generators.util.TrackerType;
import ammos.datagen.generators.util.UsageTrackerMap;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.time.SclkFmt.SclkFormatter;
import jpl.gds.shared.time.TimeProperties;

/**
 * This is a file seeded SCLK value generator class. The values to be produced
 * by the generator are loaded from a seed file, which must be specified by the
 * run configuration. The data type returned by getNext() and get() is Sclk.
 * Random generation is not currently supported.
 * 
 * 
 *
 */
public class FileSeededSclkGenerator extends AbstractFileSeededGenerator
        implements IFileSeededGenerator, ISclkGenerator {

    private FileSeededSclkGeneratorSeed seedData;
    private final SclkFormatter sclkFmt = TimeProperties.getInstance().getSclkFormatter();

    /**
     * {@inheritDoc}
     * 
     * @see ammos.datagen.generators.AbstractFileSeededGenerator#setSeedData(ammos.datagen.generators.seeds.ISeedData)
     */
    @Override
    public void setSeedData(final ISeedData seed) {

        super.setSeedData(seed);

        if (!(seed instanceof FileSeededSclkGeneratorSeed)) {
            throw new IllegalArgumentException(
                    "Seed must be of type FileSeedSclkGeneratorSeed");
        }
        this.seedData = (FileSeededSclkGeneratorSeed) seed;
    }

    /**
     * {@inheritDoc}
     * 
     * @see ammos.datagen.generators.AbstractFileSeededGenerator#reset()
     */
    @Override
    public void reset() {

        super.reset();
        this.seedData = null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see ammos.datagen.generators.AbstractFileSeededGenerator#load()
     */
    @Override
    public boolean load() {

        if (this.seedData == null || getSeedFile() == null) {
            throw new IllegalStateException(
                    "Float generator is unseeded or has a null seed file");
        }
        UsageTrackerMap.getGlobalTrackers().addTracker(
                TrackerType.SCLK.getMapType(), getTracker());
        return super.load();
    }

    /**
     * {@inheritDoc}
     * 
     * @see ammos.datagen.generators.AbstractFileSeededGenerator#convertLineToPrimitive(java.lang.String)
     */
    @Override
    protected Object convertLineToPrimitive(final String line) {

        if (this.seedData == null) {
            throw new IllegalStateException("float generator is unseeded");
        }
        try {
            return sclkFmt.valueOf(line);
        } catch (final IllegalArgumentException e) {
            TraceManager.getDefaultTracer().error(

                    "Non-parseable SCLK value found in SCLK seed file: "
                            + getSeedFile());
            return null;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ammos.datagen.generators.AbstractFileSeededGenerator#getRandom()
     */
    @Override
    public Object getRandom() {

        throw new UnsupportedOperationException(
                "random SCLK generation is not supported");
    }

    /**
     * {@inheritDoc}
     * 
     * @see ammos.datagen.generators.ISclkGenerator#isExhausted()
     */
    @Override
    public boolean isExhausted() {

        return this.seedData.isStopWhenExhausted()
                && getTracker().getPercentFilled() >= 100.0;
    }
}
