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
import java.util.LinkedList;
import java.util.List;

import ammos.datagen.config.TraversalType;
import ammos.datagen.generators.seeds.ISeedData;

/**
 * This is the seed class for the SeqIdGenerator. It contains all the data
 * necessary to initialize the generator. The valid and invalid SEQIDs are
 * loaded from the run configuration file.
 * 
 *
 */
public class SeqIdGeneratorSeed implements ISeedData {
	private List<Integer> validSeqIds = new LinkedList<Integer>();
	private List<Integer> invalidSeqIds = new LinkedList<Integer>();
	private boolean useInvalid;
	private float invalidPercent;
	private TraversalType traversalType = TraversalType.RANDOM;

	/**
	 * Gets the traversal type for SEQID generation: RANDOM or SEQUENTIAL.
	 * 
	 * @return TraversalType enumeration value
	 */
	public TraversalType getTraversalType() {

		return this.traversalType;
	}

	/**
	 * Gets the traversal type for SEQID generation: RANDOM or SEQUENTIAL.
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
	 * Gets the list of valid SEQIDs.
	 * 
	 * @return List of Integer. Empty list if none configured.
	 */
	public List<Integer> getValidSeqIds() {

		return Collections.unmodifiableList(this.validSeqIds);
	}

	/**
	 * Sets the list of valid SEQIDs.
	 * 
	 * @param validSeqIds
	 *            List of Integer. Set an empty list if none configured.
	 */
	public void setValidSeqIds(final List<Integer> validSeqIds) {

		if (validSeqIds == null) {
			throw new IllegalArgumentException(
					"validSeqIds may not be null. Set an empty list.");
		}
		this.validSeqIds = Collections.unmodifiableList(validSeqIds);
	}

	/**
	 * Gets the list of invalid SEQIDs.
	 * 
	 * @return List of Integer. Empty list if none configured.
	 */
	public List<Integer> getInvalidSeqIds() {

		return Collections.unmodifiableList(this.invalidSeqIds);
	}

	/**
	 * Sets the list of invalid SEQIDs.
	 * 
	 * @param invalidSeqIds
	 *            List of Integer. Set an empty list if none configured.
	 */
	public void setInvalidSeqIds(final List<Integer> invalidSeqIds) {

		if (invalidSeqIds == null) {
			throw new IllegalArgumentException(
					"invalidSeqids may not be null. Set an empty list.");
		}
		this.invalidSeqIds = Collections.unmodifiableList(invalidSeqIds);
	}

	/**
	 * Indicates whether the run configuration stated that invalid SEQIDs should
	 * be included in the output of the data generator.
	 * 
	 * @return true to include invalid SEQIDs, false to not
	 */
	public boolean isUseInvalid() {

		return this.useInvalid;
	}

	/**
	 * Sets the flag indicating whether the run configuration stated that
	 * invalid SEQIDs should be included in the output of the data generator.
	 * 
	 * @param useInvalid
	 *            true to include invalid SEQIDs, false to not
	 */
	public void setUseInvalid(final boolean useInvalid) {

		this.useInvalid = useInvalid;
	}

	/**
	 * Gets the percentage of output values that should be populated with
	 * invalid SEQID values.
	 * 
	 * @return desired percentage of invalid values
	 */
	public float getInvalidPercent() {

		return this.invalidPercent;
	}

	/**
	 * Sets the percentage of output values that should be populated with
	 * invalid SEQID values.
	 * 
	 * @param invalidPercent
	 *            desired percentage of invalid values; must be >= 0.
	 */
	public void setInvalidPercent(final float invalidPercent) {

		if (invalidPercent < 0 || invalidPercent > 100) {
			throw new IllegalArgumentException(
					"invalidPercent must be >= 0.0 and <= 100.0");
		}
		this.invalidPercent = invalidPercent;
	}
}
