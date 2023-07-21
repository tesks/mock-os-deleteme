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
package jpl.gds.globallad.data.container;

import java.util.Collection;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jpl.gds.globallad.data.IGlobalLADData;

/**
 * Public interface for querying the global lad.
 */
public interface IGlobalLadQueryable {
	
	/**
	 * Finds all the values in the data store that are matched by matcher.
	 * 
	 * @param matcher
	 * @return Map of collections for all data that is matched by matcher, keyed by the data identifier.
	 */
	@JsonIgnore
	public Map<Object, Collection<IGlobalLADData>> getAll(IGlobalLadSearchAlgorithm matcher);

	/**
	 * Finds numRecords most recent values based on the data primary time.
	 * 
	 * @param matcher
	 * @param numRecords
	 * @return Map of collections for data that is matched by matcher, keyed by the data identifier.  Each collection will have at 
	 * most numRecords records in it.
	 */
	@JsonIgnore
	public Map<Object, Collection<IGlobalLADData>> get(IGlobalLadSearchAlgorithm matcher, int numRecords);
	
	
	
}
