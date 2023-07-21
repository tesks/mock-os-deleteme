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

import java.util.Map;

import jpl.gds.dictionary.api.decom.IDecomDictionary;
import jpl.gds.dictionary.api.decom.IDecomStatement;

/**
 * Represents a reference to another decom map within a decom map.
 * Consists of an ID and an optional mapping of variable names to channel IDs.
 * When encountered within a decom map, the decom processor should continue decommutation
 * using the referenced map's statements, until reaching the end of that map. 
 *
 */
public interface IDecomMapReference extends IDecomStatement {
	
	/**
	 * 
	 * @return the string identifying the referenced decom map. Its interpretation is a task
	 * 		   left to the implementatino of the {@link IDecomDictionary} being used.
	 */
	public String getMapId();
	
	/**
	 * Get a mapping of decom variable names to the channel IDs they should map to, if channelization
	 * is enabled.
	 * @return the map instance
	 */
	public Map<String, String> getNameToChannelMap();

}
