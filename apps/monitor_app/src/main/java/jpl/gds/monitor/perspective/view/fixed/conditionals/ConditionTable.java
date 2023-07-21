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
package jpl.gds.monitor.perspective.view.fixed.conditionals;

import java.util.HashMap;
import java.util.Map;

import jpl.gds.monitor.perspective.view.fixed.IConditionConfiguration;


/**
 * Stores the condition ID to condition definition mappings

 *
 */
public class ConditionTable {
    
    private final Map<String, IConditionConfiguration> conditionIdMapping;
	
	/** The single static instance of this class */
	private static volatile ConditionTable instance = null;

	/**
	 * Accessor for the singleton static instance of this class
	 * 
	 * @return The single instance of the ConditionTable
	 */
	public static ConditionTable getInstance() {
		if (instance == null) {
			instance = new ConditionTable();
		}

		return instance;
	}

	/**
	 * for testing purposes
	 */
	public static void forceNewInstance() {
		instance = new ConditionTable();
	}

	/**
	 * Creates an instance of ConditionTable. Initialize all the mappings to empty.
	 */
	private ConditionTable() {
		conditionIdMapping = new HashMap<String, IConditionConfiguration>();
	}

	/**
	 * Add a new Condition to this table for the given ID.
	 * 
	 * @param conditionId the condition ID
	 * @param condition The condition definition to add
	 */
	public synchronized void addCondition(final String conditionId, final IConditionConfiguration condition) {

		if (condition == null) {
			throw new IllegalArgumentException("Null condition input not accepted");
		}

		if (conditionId == null) {
			throw new IllegalArgumentException("Null condition ID input not accepted");
		}

		conditionIdMapping.put(conditionId, condition);
	}

	/**
     * Get the Condition Configuration associated with the given id
     * 
     * @param cid is the condition configuration ID
     * @return the condition configuration associated with cid
     */
	public IConditionConfiguration getCondition(final String cid) {

		if (cid == null) {
			throw new IllegalArgumentException("Null channel ID input not accepted");
		}

		return conditionIdMapping.get(cid);
	}


	/**
	 * For testing purposes
	 * @return condition ID to condition configuration mappings
	 */
	public Map<String, IConditionConfiguration> getConditionIdMapping()
	{
		return conditionIdMapping;
	}
}
