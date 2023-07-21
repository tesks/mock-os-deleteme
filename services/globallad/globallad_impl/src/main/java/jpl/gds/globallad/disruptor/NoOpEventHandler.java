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

import jpl.gds.globallad.data.container.IGlobalLadContainer;

/**
 * Used for proviling.  Does nothing on events.
 */
public class NoOpEventHandler extends AbstractGlobalLadInserterEventHandler {
	
	public NoOpEventHandler(IGlobalLadContainer globalLad, int ordinal, int outOf) {
		super(globalLad, ordinal, outOf);
	}

	@Override
	public void onEvent(GlobalLadDataEvent event, long sequence, boolean endOfBatch) throws Exception {
		/**
		 * No op handler used for profiling.  Does nothing when it gets an event.
		 */
	}
}
