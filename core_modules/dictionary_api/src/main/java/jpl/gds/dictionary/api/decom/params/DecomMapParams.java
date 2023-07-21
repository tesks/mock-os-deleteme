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

import jpl.gds.dictionary.api.decom.IDecomMapDefinition;
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
public class DecomMapParams extends StatementContainerParams {
	
	private String id;

	/**
	 * @see IDecomMapDefinition#getId()
	 * @return the string ID for the decom map
	 */
	public String getId() {
		return id;
	}

	/**
	 * Set the id for the decom map.
	 * If the argument is null, the id will be set
	 * to the empty string.
	 * @param id
	 */
	public void setId(String id) {
		if (id != null) {
			this.id = id;
		} else { 
			this.id = "";
		}
	}
	
	@Override
	public void reset() {
		super.reset();
		id = "";
	}


}
