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
package jpl.gds.globallad.message.handler;

import jpl.gds.globallad.IGlobalLadJsonable;
import jpl.gds.globallad.IGlobalLadReapable;
import jpl.gds.globallad.IGlobalLadSummarizableContainer;
import jpl.gds.globallad.data.container.IGlobalLadContainer;
import jpl.gds.globallad.data.container.IGlobalLadDeltaQueryable;
import jpl.gds.globallad.data.container.IGlobalLadQueryable;
import jpl.gds.globallad.data.container.search.IGlobalLadContainerSearchAlgorithm;

/**
 * Public interface for the basic methods of the global lad.  This is used for dependency injection into 
 * the REST resources and protects the global lad from abuse.
 */
public interface IGlobalLad extends IGlobalLadQueryable, IGlobalLadDeltaQueryable, IGlobalLadJsonable,
		IGlobalLadSummarizableContainer, IGlobalLadReapable {
	
	/**
	 * Clears the global lad.
	 */
	public void clearGlad();

	/**
	 * Trims the data based on the matcher given.  
	 * 
	 * @param matcher
	 * @param force - Force removal even if clients are connected.
	 * @throws Exception - If error or clients are connected without the force flag.
	 */
	public void remove(IGlobalLadContainerSearchAlgorithm matcher) throws Exception;

	/**
	 * Returns the master container, or top level of the global lad.
	 * 
	 * @return
	 */
	public IGlobalLadContainer getMasterContainer();
}
