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
import jpl.gds.dictionary.api.decom.types.ICaseBlockDefinition;


/**
 * Parameter builder class for creating {@link ICaseBlockDefinition} instances.
 * Mutable and reusable.
 * 
 * All values set for an instance of this class will be applied to the {@link IDecomStatement}
 * instance it will be used to create. For more information for each of the parameters,
 * see the interface this parameter object corresponds to.
 */
public class CaseParams extends StatementContainerParams {

	private long value;
	private boolean isDefault;
	
	/**
	 * Mark this case as a default case.
	 */
	public void setIsDefault() {
		isDefault = true;
	}
	
	/**
	 * Set the value that selects this case.
	 * @param value the value that would select this case
	 */
	public void setValue(long value) {
		isDefault = false;
		this.value = value;
	}
	
	/**
	 * Get the value for this case. If
	 * {@link #isDefault()} returns true, this value is not valid,
	 * so the of this method should check the default status first.
	 * @return the value that selects this case
	 */
	public long getValue() {
		return value;
	}
	
	/**
	 * 
	 * @return true if this case is the default
	 */
	public boolean isDefault() {
		return isDefault;
	}
	
	@Override
	public void reset() {
		super.reset();
		isDefault = false;
		value = 0;
	}
}
