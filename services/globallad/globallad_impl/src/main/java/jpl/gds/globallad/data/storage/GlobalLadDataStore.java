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
package jpl.gds.globallad.data.storage;

import java.util.Collection;
import java.util.Map;

import javax.json.JsonObject;

import jpl.gds.globallad.GlobalLadReapSettings;
import jpl.gds.globallad.data.IGlobalLADData;
import jpl.gds.globallad.data.container.GlobalLadContainerFactory;
import jpl.gds.globallad.data.container.IGlobalLadContainer;
import jpl.gds.globallad.data.container.IGlobalLadSearchAlgorithm;
import jpl.gds.globallad.data.container.search.IGlobalLadContainerSearchAlgorithm;
import jpl.gds.globallad.message.handler.IGlobalLad;

/**
 * Holds the master container that contains all of the global lad data.
 * 
 * Global lad works as a Spring controlled application.
 * Took out all of the inserting code including disruptors and inserters.
 * 		Now this class is only responsible for storing the data.
 */
public class GlobalLadDataStore implements IGlobalLad {
	/**
	 * Refactoring this class to separate concerns with use in
	 * Spring.  This class is now the main data store and does not care / rely on any of the 
	 * other servers / services in the application.  
	 */


	/**
	 * This is the actual global lad.  It is the top level container that is used to query 
	 * and insert into.  
	 */
	protected IGlobalLadContainer masterContainer;
	
	/**
	 * @throws Exception
	 */
	public GlobalLadDataStore() {
		masterContainer = GlobalLadContainerFactory.createMasterContainer();
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<Object, Collection<IGlobalLADData>> getAll(final IGlobalLadSearchAlgorithm matcher) {
		return masterContainer.getAll(matcher);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Map<Object, Collection<IGlobalLADData>> get(final IGlobalLadSearchAlgorithm matcher, final int numRecords) {
		return masterContainer.get(matcher, numRecords);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void deltaQuery(
			final IGlobalLadSearchAlgorithm matcher, final Map<DeltaQueryStatus, Map<Object, Collection<IGlobalLADData>>> resultMap) {
		masterContainer.deltaQuery(matcher, resultMap);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IGlobalLadContainer getMasterContainer() {
		return masterContainer;
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void clearGlad() {
		masterContainer.clear();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public void remove(final IGlobalLadContainerSearchAlgorithm matcher) throws Exception {
		masterContainer.remove(matcher);
	}
	

	/**
	 * {@inheritDoc}
	 */
	@Override
	public JsonObject getSummary(final IGlobalLadContainerSearchAlgorithm matcher) {
		return masterContainer.getSummary(matcher);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public JsonObject getStats() {
		return masterContainer.getStats();
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public JsonObject getMetadata(final IGlobalLadContainerSearchAlgorithm matcher) {
		// always true here since we are querying the global lad master container.
		return masterContainer.getMetadata(matcher);
	}
	
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getJsonId() {
		return "GlobalLadMain";
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean reap(final GlobalLadReapSettings reapSettings, final long checkTimeMilliseconds, final boolean parentWasReapable, final long parentTimeToLive) {
		return masterContainer.reap(reapSettings, checkTimeMilliseconds, false, -1);
	}
}
