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
package jpl.gds.dictionary.api.decom.params;

import jpl.gds.dictionary.api.decom.IDecomStatement;
import jpl.gds.dictionary.api.decom.types.IDecomDataDefinition;

/**
 * Parameter builder class for creating {@link IDecomDataDefinition} instances.
 * Mutable and reusable.
 * 
 * All values set for an instance of this class will be applied to the {@link IDecomStatement}
 * instance it will be used to create. For more information for each of the parameters,
 * see the interface this parameter object corresponds to.
 */
public class DecomDataParams implements IDecomDefinitionParams {

	private String name = "";
	
	private String description = "";
	
	private String format = null;
	
	private boolean offsetSpecied = false;
	
	private int bitOffset;

	/**
	 * @see IDecomDataDefinition#getName()
	 * @return the name of the decom data
	 */
	public String getName() {
		return name;
	}

	/**
	 * Set the string name of the decom data.
	 * If the argument is null, the name will be set
	 * to the empty string.
	 * @param name the name of the data
	 */
	public void setName(String name) {
		if (name != null) {
			this.name = name;
		} else {
			this.name = "";
		}
	}

	/**
	 * @see IDecomDataDefinition#getDescription()
	 * @return the description string of the decom data
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Set the description string of the decom data
	 * If the argument is null, the description will be set
	 * to the empty string.
	 * @param description the decom data description
	 */
	public void setDescription(String description) {
		if (description != null) {
			this.description = description;
		} else {
			this.description = "";
		}
	}

	/**
	 * @see IDecomDataDefinition#getFormat()
	 * @return the formatter string for this decom data
	 */
	public String getFormat() {
		return format;
	}

	/**
	 *  Set the formatter string of the decom data.
	 * @param format the formatter string for this decom data
	 */
	public void setFormat(String format) {
		if (format != null) {
			this.format = format;
		}
	}

	/**
	 * @see IDecomDataDefinition#offsetSpecified()
	 * @return true if the offset has been specified for this decom data
	 */
	public boolean offsetSpecied() {
		return offsetSpecied;
	}

	/**
	 * @see IDecomDataDefinition#getBitOffset()
	 * @return the offset, in bits, of this data
	 */
	public int getBitOffset() {
		return bitOffset;
	}

	/**
	 * @param bitOffset the numeric bit offset this data begins at
	 */
	public void setBitOffset(int bitOffset) {
		this.offsetSpecied = true;
		this.bitOffset = bitOffset;
	}

	@Override
	public void reset() {
		offsetSpecied = false;
		bitOffset = 0;
		this.format = "";
		this.description = "";
		this.name = "";
	}
	
}
