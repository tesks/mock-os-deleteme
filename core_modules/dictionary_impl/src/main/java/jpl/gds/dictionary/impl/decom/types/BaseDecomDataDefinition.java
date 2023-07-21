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
package jpl.gds.dictionary.impl.decom.types;

import jpl.gds.dictionary.api.decom.params.DecomDataParams;
import jpl.gds.dictionary.api.decom.types.IDecomDataDefinition;

/**
 * Base class for all decom data type implementation classes.  Any data
 * type class should extend this class to take advantage of its
 * constructor and accessors.
 *
 */
public abstract class BaseDecomDataDefinition implements IDecomDataDefinition {

	/**
	 * Data format specifier.
	 */
	protected String format;
	private final String name;
	private final boolean offsetSpecified;
	private final int bitOffset;
	private final String description;

	/**
	 * Create an instance initialized with the provided parameters.
	 * @param params
	 */
	public BaseDecomDataDefinition(DecomDataParams params) {
		format = params.getFormat();
		name = params.getName();
		offsetSpecified = params.offsetSpecied();
		bitOffset = params.getBitOffset();
		description = params.getDescription();
	}

	@Override
	public String getFormat() {
		return format;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public int getBitOffset() {
		return bitOffset;
	}

	@Override
	public boolean offsetSpecified() {
		return offsetSpecified;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName() + ": " + "name=" + this.getName()
				+ ", offset=" + (offsetSpecified ? bitOffset : "unspecified");
	}

}
