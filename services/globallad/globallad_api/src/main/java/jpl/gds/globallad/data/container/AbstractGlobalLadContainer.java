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

import jpl.gds.globallad.GlobalLadProperties;
import jpl.gds.globallad.data.GlobalLadContainerException;
import jpl.gds.globallad.data.IGlobalLADData;
import jpl.gds.globallad.data.container.search.IGlobalLadContainerSearchAlgorithm;
import jpl.gds.shared.log.Tracer;

/**
 * Classes that extend this are expected to know the parent search algorithm that is passed in and how to 
 * take that and create one to search through child containers.
 */
public abstract class AbstractGlobalLadContainer implements IGlobalLadContainer, IGlobalLadSerializable {

	protected static final Tracer log = GlobalLadProperties.getTracer();
	
	protected long lastInsert;
	
	/**
	 * Initializes lastInsert with the current time as reported by System.currentMillis().
	 */
	public AbstractGlobalLadContainer() {
		super();
		
		lastInsert = System.currentTimeMillis();
	}
	
	
	/**
	 * Searches through the particular data storage implementation and returns a collection of matching containers.
	 * 
	 * @param matcher
	 * @return
	 */
	protected abstract Collection<IGlobalLadContainer> search(final IGlobalLadContainerSearchAlgorithm matcher);
	
	/**
	 * Create a container for whatever point in the tree that needs to be created to hold data.  The implementation should 
	 * know what type of containers it holds so it should create one for the data here. 
	 * 
	 * @param data
	 * @return
	 * @throws GlobalLadContainerException 
	 */
	protected abstract IGlobalLadContainer generateContainer(IGlobalLADData data) throws GlobalLadContainerException;

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.container.IGlobalLadStatable#lastInsert()
	 */
	@Override
	public long lastInsert() {
		return this.lastInsert;
	}

	/* (non-Javadoc)
	 * @see jpl.gds.globallad.data.container.IGlobalLadStatable#lastInsertDelta()
	 */
	@Override
	public long lastInsertDelta() {
		return System.currentTimeMillis() - this.lastInsert;
	}
}
