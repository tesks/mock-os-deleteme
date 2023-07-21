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
 * This is the seed data class for the boolean value generator. It includes all
 * the data necessary to initialize the generator.
 * 
 *
 */
public class BooleanGeneratorSeed implements ISeedData {
	private boolean includeOtherValue;
	private TraversalType traversalType = TraversalType.RANDOM;

	/**
	 * Indicates whether an unsigned integer values other than 0 and 1 should be
	 * included in boolean value generation.
	 * 
	 * @return true if non 0 or 1 value should be generated, false if not
	 */
	public boolean isIncludeExtraValue() {

		return this.includeOtherValue;
	}

	/**
	 * Sets the flag indicating whether an unsigned integer values other than 0
	 * and 1 should be included in boolean value generation.
	 * 
	 * @param includeOtherValue
	 *            true if non 0 or 1 value should be generated, false if not
	 */
	public void setIncludeExtraValue(final boolean includeOtherValue) {

		this.includeOtherValue = includeOtherValue;
	}

	/**
	 * Gets the traversal type for boolean generation: RANDOM or SEQUENTIAL.
	 * 
	 * @return TraversalType value
	 */
	public TraversalType getTraversalType() {

		return this.traversalType;
	}

	/**
	 * Gets the traversal type for boolean generation: RANDOM or SEQUENTIAL.
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
}
