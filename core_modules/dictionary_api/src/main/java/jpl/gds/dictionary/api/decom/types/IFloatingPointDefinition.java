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
package jpl.gds.dictionary.api.decom.types;

/**
 * Represents floating point data within a decom mpa.
 *
 */
public interface IFloatingPointDefinition extends INumericDataDefinition {

	/**
	 * Defines the floating point encodings supported by generic decom.
	 */
	public enum FloatEncoding {
		/** Refers to IEEE 754 floating point standard. */
		IEEE,
		/** Refers to the MIL-1750A floating point standard. */
		MIL
	}
	
	/**
	 * Defines the floating point precisions supported by generic decom.
	 *
	 */
	public enum Precision {
		/** Single precision floating point */
		SINGLE,
		/** Double precision floating point */
		DOUBLE
	}

	/**
	 * 
	 * @return the binary float encoding
	 */
	public FloatEncoding getEncoding();
	
	/**
	 * 
	 * @return the precision of the floating point data
	 */
	public Precision getPrecision();

}
