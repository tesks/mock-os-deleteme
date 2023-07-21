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
package jpl.gds.globallad.data.container.search;

import jpl.gds.globallad.data.IGlobalLADData;
import jpl.gds.globallad.data.IGlobalLADData.GlobalLadPrimaryTime;

/**
 * Search algorithm used to match IGlobalLADData objects.
 */
public interface IGlobalLadDataSearchAlgorithm {

	/**
	 * Checks to see if data is matched by this instance of the algorithm.
	 * 
	 * @param data
	 * @return true if data is matched by the criteria in this search algorithm.
	 */
	public boolean isMatched(IGlobalLADData data);
	
	/**
	 * @return the time type for this search algorithm.
	 */
	public GlobalLadPrimaryTime getTimeType();
	
	/**
	 * Used for delta queries.  Gets the lower bound time of the search algorithm.
	 * 
	 * @return The lower bound millisecond time for this search algorithm.  -1 if no lower bound time was defined.
	 */
	public long getLowerBoundMilliseconds();
	

}
