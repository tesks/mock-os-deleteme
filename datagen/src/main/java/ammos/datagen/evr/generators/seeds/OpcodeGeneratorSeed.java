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
import ammos.datagen.evr.config.Opcode;
import ammos.datagen.generators.seeds.ISeedData;

/**
 * This is the seed class for the OpcodeGenerator. It contains all the data
 * necessary to initialize the generator. The valid and invalid opcodes are
 * loaded from the run configuration file.
 * 
 *
 */
public class OpcodeGeneratorSeed implements ISeedData {
	private List<Opcode> validOpcodes = new LinkedList<Opcode>();
	private List<Opcode> invalidOpcodes = new LinkedList<Opcode>();
	private boolean useInvalid;
	private float invalidPercent;
	private TraversalType traversalType = TraversalType.RANDOM;

	/**
	 * Gets the traversal type for opcode generation: RANDOM or SEQUENTIAL.
	 * 
	 * @return TraversalType enumeration value
	 */
	public TraversalType getTraversalType() {

		return this.traversalType;
	}

	/**
	 * Gets the traversal type for opcode generation: RANDOM or SEQUENTIAL.
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
	 * Gets the list of valid Opcode objects.
	 * 
	 * @return List of Opcode. Empty list if none configured.
	 */
	public List<Opcode> getValidOpcodes() {

		return Collections.unmodifiableList(this.validOpcodes);
	}

	/**
	 * Sets the list of valid Opcode objects.
	 * 
	 * @param validOpcodes
	 *            List of Opcodes. Set an empty list if none configured.
	 */
	public void setValidOpcodes(final List<Opcode> validOpcodes) {

		if (validOpcodes == null) {
			throw new IllegalArgumentException(
					"validOpcodes may not be null. Set an empty list.");
		}
		this.validOpcodes = Collections.unmodifiableList(validOpcodes);
	}

	/**
	 * Gets the list of invalid Opcode objects.
	 * 
	 * @return List of Opcode. Empty list if none configured.
	 */
	public List<Opcode> getInvalidOpcodes() {

		return Collections.unmodifiableList(this.invalidOpcodes);
	}

	/**
	 * Sets the list of invalid Opcode objects.
	 * 
	 * @param invalidOpcodes
	 *            List of Opcode. Set an empty list if none configured.
	 */
	public void setInvalidOpcodes(final List<Opcode> invalidOpcodes) {

		if (invalidOpcodes == null) {
			throw new IllegalArgumentException(
					"invalidOpcodes may not be null. Set an empty list.");
		}
		this.invalidOpcodes = Collections.unmodifiableList(invalidOpcodes);
	}

	/**
	 * Indicates whether the run configuration stated that invalid opcodes
	 * should be included in the output of the data generator.
	 * 
	 * @return true to include invalid opcodes, false to not
	 */
	public boolean isUseInvalid() {

		return this.useInvalid;
	}

	/**
	 * Sets the flag indicating whether the run configuration stated that
	 * invalid opcodes should be included in the output of the data generator.
	 * 
	 * @param useInvalid
	 *            true to include invalid opcodes, false to not
	 */
	public void setUseInvalid(final boolean useInvalid) {

		this.useInvalid = useInvalid;
	}

	/**
	 * Gets the percentage of output values that should be populated with
	 * invalid opcode values.
	 * 
	 * @return desired percentage of invalid values
	 */
	public float getInvalidPercent() {

		return this.invalidPercent;
	}

	/**
	 * Sets the percentage of output values that should be populated with
	 * invalid opcode values.
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
