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
package jpl.gds.globallad;

import javax.json.JsonObject;

import jpl.gds.globallad.data.container.search.IGlobalLadContainerSearchAlgorithm;

/**
 * Interface for a global lad object to give a summary of the data container within.
 */
public interface IGlobalLadSummarizableContainer {
	/**
	 * Get the containers matched by matcher and return a summary of those containers.  
	 * 
	 * @param matcher
	 * @return - Json object with the summary of the matched containers.
	 */
	public JsonObject getSummary(IGlobalLadContainerSearchAlgorithm matcher);

}
