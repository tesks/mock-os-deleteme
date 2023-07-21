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
package ammos.datagen.generators.seeds;

import ammos.datagen.config.TraversalType;

/**
 * This is the seed class for the FileSeededSclkGenerator. It provides
 * everything needed to initialize the generator.
 * 
 *
 */
public class FileSeededSclkGeneratorSeed implements IFileSeedData {

	private String seedFile;
	private boolean stopWhenExhausted;

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.generators.seeds.IFileSeedData#getSeedFile()
	 */
	@Override
	public String getSeedFile() {

		return this.seedFile;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.generators.seeds.IFileSeedData#setSeedFile(java.lang.String)
	 */
	@Override
	public void setSeedFile(final String tableName) {

		this.seedFile = tableName;

	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * TraversalType is currently hard-wired to SEQUENTIAL in this class. The
	 * use case for random SCLKs is very limited.
	 * 
	 * @see ammos.datagen.generators.seeds.IFileSeedData#getTraversalType()
	 */
	@Override
	public TraversalType getTraversalType() {

		return TraversalType.SEQUENTIAL;
	}

	/**
	 * Gets the flag indicating whether packet generation should stop when all
	 * of the SCLKs in the seed table have been used. Applies only when this
	 * seed is used for generation of packet SCLKs (not for time field
	 * generation).
	 * 
	 * @return true if packet generation should stop when all SCLKs used, false
	 *         if not.
	 */
	public boolean isStopWhenExhausted() {

		return this.stopWhenExhausted;
	}

	/**
	 * Sets the flag indicating whether packet generation should stop when all
	 * of the SCLKs in the seed table have been used. Applies only when this
	 * seed is used for generation of packet SCLKs (not for time field
	 * generation).
	 * 
	 * @param stopWhenExhausted
	 *            true if packet generation should stop when all SCLKs used,
	 *            false if not.
	 */
	public void setStopWhenExhausted(final boolean stopWhenExhausted) {

		this.stopWhenExhausted = stopWhenExhausted;
	}
}
