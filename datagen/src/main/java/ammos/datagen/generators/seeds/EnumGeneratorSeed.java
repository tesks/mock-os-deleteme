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
import jpl.gds.dictionary.api.EnumerationDefinition;

/**
 * This is the generator seed class for the EnumGenerator class, which generates
 * enumerated values. It contains all the data necessary to initialize the
 * generator.
 * 
 */
public class EnumGeneratorSeed implements ISeedData {
	private boolean useInvalid;
	private EnumerationDefinition enumDef;
	private String enumName;
	private float invalidPercent;
	private TraversalType traversalType = TraversalType.RANDOM;

	/**
	 * Constructor.
	 * 
	 * @param name
	 *            enumeration name (typedef name) from the EVR dictionary
	 * @param values
	 *            EnumerationDefinition object containing the enumeration values
	 * @param useInvalid
	 *            true if the generator should include invalid values, false if
	 *            not
	 */
	public EnumGeneratorSeed(final String name,
			final EnumerationDefinition values, final boolean useInvalid) {

		setEnumName(name);
		setEnumDef(values);
		setUseInvalid(useInvalid);
	}

	/**
	 * Gets the traversal type for enum generation: RANDOM or SEQUENTIAL.
	 * 
	 * @return TraversalType enumeration value
	 */
	public TraversalType getTraversalType() {

		return this.traversalType;
	}

	/**
	 * Gets the traversal type for enum generation: RANDOM or SEQUENTIAL.
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
	 * Gets the enumeration name.
	 * 
	 * @return enumeration name (string)
	 */
	public String getEnumName() {

		return this.enumName;
	}

	/**
	 * Sets the enumeration name.
	 * 
	 * @param enumName
	 *            enumeration name (string); may not be null
	 */
	public void setEnumName(final String enumName) {

		if (enumName == null) {
			throw new IllegalArgumentException("enumName may not be null");
		}

		this.enumName = enumName;
	}

	/**
	 * Gets the EnumerationDefinition object containing the values in the
	 * enumeration.
	 * 
	 * @return EnumerationDefinition object
	 */
	public EnumerationDefinition getEnumDef() {

		return this.enumDef;
	}

	/**
	 * Sets the EnumerationDefinition object containing the values in the
	 * enumeration.
	 * 
	 * @param enumDef
	 *            EnumerationDefinition object to set; may not be null
	 */
	public void setEnumDef(final EnumerationDefinition enumDef) {

		if (enumDef == null) {
			throw new IllegalArgumentException("enumDef may not be null");
		}

		this.enumDef = enumDef;
	}

	/**
	 * Gets the flag indicating whether the generator should generate invalid
	 * values (values not in the enumeration).
	 * 
	 * @return true to generate invalid values, false to not
	 */
	public boolean isUseInvalid() {

		return this.useInvalid;
	}

	/**
	 * Sets the flag indicating whether the generator should generate invalid
	 * values (values not in the enumeration).
	 * 
	 * @param useInvalid
	 *            true to generate invalid values, false to not
	 */
	public void setUseInvalid(final boolean useInvalid) {

		this.useInvalid = useInvalid;
	}

	/**
	 * Gets the percentage of output values that should be populated with
	 * invalid enumeration values.
	 * 
	 * @return desired percentage of invalid values
	 */
	public float getInvalidPercent() {

		return this.invalidPercent;
	}

	/**
	 * Sets the percentage of output values that should be populated with
	 * invalid enumeration values.
	 * 
	 * @param invalidPercent
	 *            desired percentage of invalid values; must be >= 0.
	 */
	public void setInvalidPercent(final float invalidPercent) {

		if (invalidPercent < 0 || invalidPercent > 100) {
			throw new IllegalArgumentException(
					"invalidPercent must be >= 0.0 and <=100.0");
		}
		this.invalidPercent = invalidPercent;
	}

}
