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

import com.fasterxml.jackson.annotation.JsonIgnore;

import jpl.gds.globallad.IGlobalLadJsonable;

/**
 * Public interface for any global lad object to return a basic set of stats about themselves.
 */
public interface IGlobalLadStatable extends IGlobalLadJsonable {
	
	/**
	 * @return Number of child containers.
	 */
	@JsonIgnore
	public int getChildCount();
	
	/**
	 * Aggregate sum of all the global lad data objects in the insert or event time range held in all the children 
	 * of this node.
	 * 
	 * @return Sum of the insert buffer data counts for all child containers.
	 */
	@JsonIgnore
	public long getInsertDataCount();
	
	/**
	 * Aggregate sum of all the global lad data objects in the insert or event time range held in all the children 
	 * of this node.
	 * 
	 * @return Sum of the ert buffer data counts for all child containers.
	 */
	@JsonIgnore
	public long getErtDataCount();

	/**
	 * Aggregate sum of all the global lad data objects in the insert or event time range held in all the children 
	 * of this node.
	 * 
	 * @return Sum of the scet buffer data counts for all child containers.
	 */
	@JsonIgnore
	public long getScetDataCount();

	
	/**
	 * Get the time of the last insert.
	 * 
	 * @return Milliseconds from epoch of the last insert.
	 */
	public long lastInsert();
	
	/**
	 * @return Number of milliseconds since the last insert.
	 */
	public long lastInsertDelta();
}
