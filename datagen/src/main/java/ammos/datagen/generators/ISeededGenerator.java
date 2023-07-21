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

import ammos.datagen.generators.seeds.ISeedData;
import ammos.datagen.generators.seeds.InvalidSeedDataException;
import ammos.datagen.generators.util.TruthFile;

/**
 * This interface is implemented by all data generators that take seed data read
 * from the configuration.
 * 
 *
 */
public interface ISeededGenerator {

	/**
	 * Sets seed data into the generator.
	 * 
	 * @param seed
	 *            the ISeedData object specific to the generator being seeded
	 * 
	 * @throws IllegalArgumentException
	 *             if the seed is of the wrong type or null
	 * @throws InvalidSeedDataException
	 *             if the generator cannot be properly initialized with the seed
	 */
	public void setSeedData(ISeedData seed) throws InvalidSeedDataException,
			IllegalArgumentException;

	/**
	 * Resets the generator to its non-loaded, non-seeded state.
	 */
	public void reset();

	/**
	 * Gets the next value from the generator. This method will traverse the
	 * list of seed values sequentially, wrapping around when it reaches the
	 * end.
	 * 
	 * @return next value; Object type is dependent upon the generator subclass.
	 */
	public Object getNext();

	/**
	 * Gets the next value from the generator. This method will select from the
	 * list of seed values randomly.
	 * 
	 * @return random value; Object type is dependent upon the generator
	 *         subclass.
	 */
	public Object getRandom();

	/**
	 * Gets next value from the generator. This method will either traverse the
	 * list of seed values sequentially, or select from them randomly, depending
	 * on the TraversalType indicated by the seed data.
	 * 
	 * @return random or next value; Object type is dependent upon the generator
	 *         subclass.
	 */
	public Object get();
	
	/**
     * Sets the generator to start using a new truth file. Default implementation throws
     * UnsupportedOperationException unless overridden.
     * 
     * @param newFile the TruthFile to set
     * 
     * MPCS-9375 - 1/3/18 - Added interface
     */
    public default void resetTruthFile(final TruthFile newFile) {
        throw new UnsupportedOperationException("Reset of truth file is not supported by this generator");
    }
	
}
