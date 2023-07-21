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

import java.util.List;

import jpl.gds.dictionary.api.decom.params.DataStructureParams;
import jpl.gds.dictionary.api.decom.types.ICompositeDataDefinition;
import jpl.gds.dictionary.api.decom.types.IDecomDataDefinition;

/**
 * Base implementation class for any class implementing the {@linkplain ICompositeDataDefinition} interface.
 * Provides accessing data definition within the composite data in an uniform manner.
 *
 */
public class BaseCompositeDataDefinition extends BaseDecomDataDefinition implements ICompositeDataDefinition {
	private final List<IDecomDataDefinition> definitions;
	
	/**
	 * Create instance, initializing from the provided parameters.
	 * @param params parameters used to initialize the new instance.
	 */
	public BaseCompositeDataDefinition(DataStructureParams params) {
		super(params);
		definitions = params.getDataDefinitions();
	}

	@Override
	public List<IDecomDataDefinition> getDataDefinitions() {
		return definitions;
	}

}
