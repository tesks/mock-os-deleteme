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

import java.util.ArrayList;
import java.util.List;

import jpl.gds.dictionary.api.decom.IDecomStatement;
import jpl.gds.dictionary.api.decom.types.ICompositeDataDefinition;
import jpl.gds.dictionary.api.decom.types.IDecomDataDefinition;

/**
 * Parameter builder class for creating {@link ICompositeDataDefinition} instances.
 * Mutable and reusable.
 * 
 * All values set for an instance of this class will be applied to the {@link IDecomStatement}
 * instance it will be used to create. For more information for each of the parameters,
 * see the interface this parameter object corresponds to.
 */
public class DataStructureParams extends DecomDataParams {
	
	private List<IDecomDataDefinition> defs = new ArrayList<>();;
	
	/**
	 * Add a constituent piece of data..
	 * @param def definition of a piece of data defined within a data structure.
	 * 		  Must not be null.
	 * @throws IllegalArgumentException when null argument is passed in
	 */
	public void add(IDecomDataDefinition def) {
		if (def == null) {
			throw new IllegalArgumentException("Null decom data definition added to data structure.");
		}
		defs.add(def);
	}
	
	/**
	 * @see ICompositeDataDefinition#getDataDefinitions()
	 * @return a list of data definition within this data strucutre
	 */
	public List<IDecomDataDefinition> getDataDefinitions() {
		return defs;
	}
	
	@Override
	public void reset() {
		super.reset();
		defs = new ArrayList<>();
	}

}
