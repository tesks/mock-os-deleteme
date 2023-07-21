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

import jpl.gds.globallad.data.IGlobalLADData;

/**
 * Interface controlling global lad containers that support delta queries.
 */
public interface IGlobalLadDeltaQueryable {
	public enum DeltaQueryStatus {
		complete, incomplete, unknown
	}

	/**
	 * Performs a delta query with matcher and merges the results in resultMap keyed by the DeltaQueryStatus.
	 * Note, a status of incomplete should mean that it is possible, but not guaranteed that the result for the query 
	 * is missing data.  
	 * 
	 * @param matcher
	 * @param resultMap Map to merge all results into.
	 */
	public void deltaQuery(IGlobalLadSearchAlgorithm matcher, Map<DeltaQueryStatus, Map<Object, Collection<IGlobalLADData>>> resultMap);
}
