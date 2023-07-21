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
import jpl.gds.dictionary.api.decom.types.IDynamicArrayDefinition;

/**
 * Parameter builder class for creating {@link IDynamicArrayDefinition} instances.
 * Mutable and reusable.
 * 
 * All values set for an instance of this class will be applied to the {@link IDecomStatement}
 * instance it will be used to create. For more information for each of the parameters,
 * see the interface this parameter object corresponds to.
 */
public class DynamicArrayParams extends DataStructureParams {

	private String lengthVariableName;

	/**
	 * @see IDynamicArrayDefinition#getLengthVariableName()
	 * @return the name of the decom data field that holds the length of the array
	 */
	public String getLengthVariableName() {
		return lengthVariableName;
	}

	/**
	 * Set the name of the decom variable that contains the length of this array 
	 * @param lengthVariableName the variable's name
	 */
	public void setLengthVariableName(String lengthVariableName) {
		if (lengthVariableName == null) {
			throw new IllegalArgumentException("The length variable name for a dynamic array cannot be null");
		} else {
			this.lengthVariableName = lengthVariableName;
		}
	}
	
	@Override
	public void reset() {
		super.reset();
		lengthVariableName = "";
	}
}
