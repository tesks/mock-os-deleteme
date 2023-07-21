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

import com.fasterxml.jackson.annotation.JsonIgnore;

import jpl.gds.globallad.data.container.search.IGlobalLadContainerSearchAlgorithm;

/**
 * Interface for any global lad object to return a json about themselves.  Includes json ignore for all 
 * of the methods so they will not be picked up by any jackson marshalling.
 */
public interface IGlobalLadJsonable {
	
	/**
	 * Json object of all of the statistics for this object. 
	 * 
	 * @return - Statistics json.
	 */
	@JsonIgnore
	public JsonObject getStats();
	
	/**
	 * Get metadata for all matched objects.
	 * 
	 * @param matcher
	 * @return - Metadata json.
	 */
	@JsonIgnore
	public JsonObject getMetadata(IGlobalLadContainerSearchAlgorithm matcher);

	/**
	 * Some identifier to be used in a stats json.
	 * 
	 * @return - String to be used as a json identifier.
	 */
	@JsonIgnore
	public String getJsonId();
	
}
