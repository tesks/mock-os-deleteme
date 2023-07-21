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
import jpl.gds.dictionary.api.decom.types.IFloatingPointDefinition;
import jpl.gds.dictionary.api.decom.types.IFloatingPointDefinition.FloatEncoding;
import jpl.gds.dictionary.api.decom.types.IFloatingPointDefinition.Precision;

/**
 * Parameter builder class for creating {@link IFloatingPointDefinition} instances.
 * Mutable and reusable.
 * 
 * All values set for an instance of this class will be applied to the {@link IDecomStatement}
 * instance it will be used to create. For more information for each of the parameters,
 * see the interface this parameter object corresponds to.
 */
public class FloatingPointParams extends NumericParams {

	private FloatEncoding encoding = FloatEncoding.IEEE;
	
	private Precision precision = Precision.DOUBLE;

	/**
	 * @see IFloatingPointDefinition#getEncoding()
	 * @return the encoding for the floating point data
	 */
	public FloatEncoding getEncoding() {
		return encoding;
	}

	/**
	 * Set the encoding for this floating point data
	 * @param encoding
	 */
	public void setEncoding(FloatEncoding encoding) {
		this.encoding = encoding;
	}

	/**
	 * Get the precision of the floating point data
	 * @return the precision of the floating point data
	 */
	public Precision getPrecision() {
		return precision;
	}

	/**
	 * Set the precision of the floating point data
	 * @param precision
	 */
	public void setPrecision(Precision precision) {
		this.precision = precision;
	} 
}
