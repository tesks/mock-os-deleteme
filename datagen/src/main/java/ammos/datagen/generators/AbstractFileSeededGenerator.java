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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import ammos.datagen.config.TraversalType;
import ammos.datagen.generators.seeds.IFileSeedData;
import ammos.datagen.generators.seeds.ISeedData;
import ammos.datagen.generators.util.UsageTracker;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.log.TraceManager;

/**
 * This is the base class for all the IFileSeededGenerator implementations. It
 * performs most of the work for loading a seed file. It also provides the basic
 * methods to get next and random values from the generator. The specific
 * generator subclass must provide the method to seed the generator, and should
 * override the get methods to return values of the proper object type.
 * 
 *
 */
public abstract class AbstractFileSeededGenerator implements
        IFileSeededGenerator {

    private final List<Object> seedValues = new ArrayList<Object>(256);
    private int iterator;
    private final Random random = new Random();
    private String seedFile;
    private IFileSeedData seedData;
    private final UsageTracker tracker = new UsageTracker();

    /**
     * {@inheritDoc}
     * 
     * @see ammos.datagen.generators.ISeededGenerator#setSeedData(ammos.datagen.generators.seeds.ISeedData)
     */
    @Override
    public void setSeedData(final ISeedData seed) {

        if (!(seed instanceof IFileSeedData)) {
            throw new IllegalArgumentException(
                    "Seed data is not of type IFileSeedData");
        }
        reset();
        this.seedData = (IFileSeedData) seed;
        this.seedFile = this.seedData.getSeedFile();
    }

    /**
     * Retrieves the UsageTracker object associated with this generator.
     * 
     * @return UsageTracker object; may be null
     */
    protected UsageTracker getTracker() {

        return this.tracker;
    }

    /**
     * Given a line read from the seed file, convert it to an object of
     * appropriate type for this generator. This method must be implemented by
     * each IFileSeededGenerator subclass, and should also display a warning for
     * any value that cannot be converted.
     * 
     * @param line
     *            the input line of text to parse
     * @return converted Object, or null if the input line could not be
     *         converted
     */
    protected abstract Object convertLineToPrimitive(String line);

    /**
     * Adds a value to the list of seed values loaded from the file.
     * 
     * @param value
     *            value to add
     */
    protected void addValue(final Object value) {

        this.seedValues.add(value);
    }

    /**
     * {@inheritDoc}
     * 
     * @see ammos.datagen.generators.IFileSeededGenerator#load()
     */
    @Override
    public boolean load() {

        if (this.seedFile == null) {
            throw new IllegalStateException("Generator seed data not set");
        }
        File path = new File(this.seedFile);

        if (!path.exists()) {
            final String newUri = ApplicationConfiguration.getRootDir()
                    + File.separator + this.seedFile;
            path = new File(newUri);
        }

        try {
            this.seedFile = path.getAbsolutePath();

            final BufferedReader reader = new BufferedReader(new FileReader(
                    path.getAbsolutePath()));
            String line;
            try {
                line = reader.readLine();
                int lineNum = 1;
                while (line != null) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        final Object o = convertLineToPrimitive(line);
                        if (o != null) {
                            addValue(o);
                        } else {
                            TraceManager.getDefaultTracer().error(

                                    "Error line is: " + lineNum);
                        }
                    }
                    line = reader.readLine();
                    lineNum++;
                }
                this.iterator = 0;
                if (this.tracker != null) {
                    this.tracker.allocateSlots(getCount());
                }
            } catch (final IOException e) {
                TraceManager.getDefaultTracer().error(

                        "I/O Error reading seed file " + this.seedFile);
                return false;
            } finally {
                try {
                    reader.close();
                } catch (final IOException e) {
                    // ignore
                }
            }
        } catch (final FileNotFoundException e) {
            TraceManager.getDefaultTracer().error(

                    "Unable to locate seed file " + this.seedFile);
            return false;
        }

        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see ammos.datagen.generators.ISeededGenerator#reset()
     */
    @Override
    public void reset() {

        this.seedData = null;
        this.seedValues.clear();
        this.seedFile = null;
        this.iterator = 0;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The actual return type of this method is determined by the subclass.
     * 
     * @see ammos.datagen.generators.ISeededGenerator#getNext()
     */
    @Override
    public Object getNext() {

        if (this.seedValues.isEmpty()) {
            throw new IllegalStateException("Seed table is empty");
        }

        final int index = this.iterator++;

        if (this.iterator == this.seedValues.size()) {
            this.iterator = 0;
        }
        if (this.tracker != null) {
            this.tracker.markSlot(index);
        }
        return this.seedValues.get(index);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The actual return type of this method is determined by the subclass.
     * 
     * @see ammos.datagen.generators.ISeededGenerator#getRandom()
     */
    @Override
    public Object getRandom() {

        if (this.seedValues.isEmpty()) {
            throw new IllegalStateException("Seed table is empty");
        }

        final int r = this.random.nextInt(this.seedValues.size());

        if (this.tracker != null) {
            this.tracker.markSlot(r);
        }
        return this.seedValues.get(r);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The actual return type of this method is determined by the subclass.
     * 
     * @see ammos.datagen.generators.ISeededGenerator#get()
     */
    @Override
    public Object get() {

        if (this.seedData == null) {
            throw new IllegalStateException("File seeded generator is unseeded");
        }

        if (this.seedData.getTraversalType().equals(TraversalType.SEQUENTIAL)) {
            return getNext();
        } else {
            return getRandom();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see ammos.datagen.generators.IFileSeededGenerator#getCount()
     */
    @Override
    public int getCount() {

        return this.seedValues.size();
    }

    /**
     * {@inheritDoc}
     * 
     * @see ammos.datagen.generators.IFileSeededGenerator#getSeedFile()
     */
    @Override
    public String getSeedFile() {

        return this.seedFile;
    }

    /**
     * Sets the seed file path.
     * 
     * @param seedPath
     *            the file path to the seed file
     */
    protected void setSeedFile(final String seedPath) {

        this.seedFile = seedPath;
    }
}
