/*
 * Copyright 2006-2019. California Institute of Technology.
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
package jpl.gds.shared.types;

/**
 * The purpose of this class is to define a strict set of values for a specified
 * field. This class by itself isn't useful until it is extended by a subclass.<br>
 * <br>
 * This class allows the user to interchangeably use int/String values and
 * restricts values to the specified set.
 * 
 */
public abstract class EnumeratedType {
	/**
	 * The maximum index of the array of values
	 */
	protected int maxIndex = -1;

	/**
	 * The index into the value array that specifies the current value of this
	 * type.
	 */
	protected int valIndex;

	/**
	 * The current string value of this type
	 */
	protected String valString;

	/**
	 * Get the string value associated with the given index.
	 * 
	 * @param index
	 *            the index of the string array (should probably be one of the
	 *            static integer values)
	 * @return the string value associated with the index passed in
	 */
	protected abstract String getStringValue(int index);

	/**
	 * Return the maximum index for this enumeration.
	 * 
	 * @return the value of the maximum index for this enumeration
	 */
	protected abstract int getMaxIndex();

	/**
	 * Constructor to specify a default value
	 * 
	 * @param intVal
	 *            The value of this type
	 */
	public EnumeratedType(int intVal) {
		setValueFromIndex(intVal);
	}

	/**
	 * Constructor to specify a default value
	 * 
	 * @param strVal
	 *            The value of this type
	 * @throws IllegalArgumentException if the string does not match one of 
	 *         the enum values 
	 */
	public EnumeratedType(String strVal) throws IllegalArgumentException {
		setValueFromString(strVal);
	}

	/**
	 * Set the value of the enumeration from a string input. May be overridden
	 * by the subclass is altered functionality is desired.
	 * 
	 * @param strVal
	 *            The string value to use to set the value of this enumeration
	 * @throws IllegalArgumentException if the string does not match one of 
	 *         the enum values 
	 */
	protected synchronized void setValueFromString(final String strVal) throws IllegalArgumentException {
		if (maxIndex < 0) {
			maxIndex = getMaxIndex();
		}

		for (int i = 0; i <= maxIndex; i++) {
			final String val = getStringValue(i);

			if (val.equalsIgnoreCase(strVal)) {
				valIndex = i;
				valString = val;

				return;
			}
		}

		throw new IllegalArgumentException("Invalid enumeration value '"
				+ strVal + "' for " + getClass().getName());
	}

	/**
	 * Set the value of the enumeration from a numeric input. May be overridden
	 * by the subclass is altered functionality is desired.
	 * 
	 * @param intVal
	 *            The numeric value to use to set the value of this enumeration
	 */
	protected synchronized void setValueFromIndex(int intVal) {
		if (this.maxIndex == -1) {
			this.maxIndex = getMaxIndex();
		}

		if (intVal < 0 || intVal > maxIndex) {
			throw new IllegalArgumentException("Invalid enumeration index "
					+ intVal);
		}

		this.valIndex = intVal;
		this.valString = getStringValue(intVal);
	}

	/**
	 * Get the current integer value of this type
	 * 
	 * @return The int value of this type
	 */
	public int getValueAsInt() {
		return this.valIndex;
	}

	/**
	 * Get the current String value of this type
	 * 
	 * @return The String value of this type
	 */
	public String getValueAsString() {
		return this.valString;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return (getValueAsString());
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object eq) {
		if (!(eq instanceof EnumeratedType)) {
			return false;
		}
		EnumeratedType compare = (EnumeratedType) eq;
		return compare.valIndex == this.valIndex;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return this.getValueAsInt();
	}
}
