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

import java.util.Collection;

import jpl.gds.globallad.data.GlobalLadSearchAlgorithmException;
import jpl.gds.globallad.data.container.IGlobalLadContainer;

/**
 * Interface for a search class that knows how to navigate the global lad data tree to match data to a set of search criteria.
 */
public interface IGlobalLadContainerSearchAlgorithm {
	public static final Class<?>[] nullClassArray = (Class<?>[]) null;
	public static final Object[] nullObjectArray = (Object[]) null;

	/**
	 * Matches child containers with the search criteria contained in this search algorithm.
	 * 
	 * @param container search target.
	 * @return Collections children of container that matches the search criteria contained in this search algorithm.
	 * @throws GlobalLadSearchAlgorithmException 
	 */
	public Collection<IGlobalLadContainer> getMatchedChildren(final IGlobalLadContainer container) throws GlobalLadSearchAlgorithmException;
	
	/**
	 * Check to see if the given container is matched by this search algorithm.
	 * 
	 * @param container search target.
	 * @return true if container is matched by the search criteria contained in this search algorithm.
	 * @throws GlobalLadSearchAlgorithmException 
	 */
	public boolean isContainerMatch(final IGlobalLadContainer container) throws GlobalLadSearchAlgorithmException;
	
	/**
	 * Return true if search criteria has been specified to match the child type of container.
	 * 
	 * @param container search target
	 * @return true if there are defined search criteria in this search algorithm to match the type of child container is configured to hold.
	 * @throws GlobalLadSearchAlgorithmException 
	 */
	public boolean isChildMatchNeeded(IGlobalLadContainer container) throws GlobalLadSearchAlgorithmException;
	
}
