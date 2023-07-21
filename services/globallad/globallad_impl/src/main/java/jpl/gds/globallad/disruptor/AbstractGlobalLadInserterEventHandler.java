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
package jpl.gds.globallad.disruptor;

import com.lmax.disruptor.EventHandler;

import jpl.gds.globallad.data.container.IGlobalLadContainer;

/**
 * Event handler to insert data into the global lad.  This is abstract in order to support testing of
 * the global lad.
 */
public abstract class AbstractGlobalLadInserterEventHandler implements EventHandler<GlobalLadDataEvent> {
	protected IGlobalLadContainer globalLad;
	protected final int ordinal;
	protected final int outOf;
	public long lastSequence = -1;
	
	/**
	 * 
	 * @param globalLad
	 * @param ordinal
	 * @param outOf
	 */
	public AbstractGlobalLadInserterEventHandler(IGlobalLadContainer globalLad, int ordinal, int outOf) {
		this.globalLad = globalLad;
		this.ordinal = ordinal;
		this.outOf = outOf;
	}
	
	/**
	 * If a new global lad is created by a dump file we need to replace the current instance.
	 * @param newGlobalLad
	 */
	public void setGlobalLad(IGlobalLadContainer newGlobalLad) {
		this.globalLad = newGlobalLad;
	}
}
