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
package ammos.datagen.evr.config;

/**
 * This class represents a command opcode, valid or invalid. It is used to
 * represent opcodes loaded from the configuration file, and which can then be
 * use to populate EVR opcode arguments.
 * 
 *
 */
public class Opcode implements Comparable<Opcode> {
	private final long number;
	private final String stem;
	private final boolean valid;

	/**
	 * Constructor.
	 * 
	 * @param num
	 *            numeric opcode
	 * @param stem
	 *            string command stem the opcode maps to; may be null for
	 *            invalid opcodes
	 * @param isValid
	 *            true if this is a valid opcode, false if not
	 */
	public Opcode(final long num, final String stem, final boolean isValid) {

		this.number = num;
		this.stem = stem;
		this.valid = isValid;
	}

	/**
	 * Gets the numeric opcode.
	 * 
	 * @return opcode
	 */
	public long getNumber() {

		return this.number;
	}

	/**
	 * String command stem.
	 * 
	 * @return command stem; may be null if the opcode is not valid
	 * 
	 */
	public String getStem() {

		return this.stem;
	}

	/**
	 * Indicates whether the opcode is valid.
	 * 
	 * @return true if opcode is valid, false if not
	 */
	public boolean isValid() {

		return this.valid;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {

		return Long.valueOf(this.number).hashCode();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object o) {

		return Long.valueOf(this.number).equals(
				Long.valueOf(((Opcode) o).number));
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(final Opcode o) {

		return Long.valueOf(this.number).compareTo(Long.valueOf(o.number));
	}
}
