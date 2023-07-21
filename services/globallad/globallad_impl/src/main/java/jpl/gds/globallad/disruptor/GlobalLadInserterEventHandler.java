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
 * Production event handler to insert data into the global lad.
 */
public class GlobalLadInserterEventHandler extends AbstractGlobalLadInserterEventHandler {
	public GlobalLadInserterEventHandler(IGlobalLadContainer globalLad, int ordinal, int outOf) {
		super(globalLad, ordinal, outOf);
	}

	@Override
	public void onEvent(GlobalLadDataEvent event, long sequence,
			boolean endOfBatch) throws Exception {
		/**
		 * Since the disrupter will always publish to all handlers use the sequence and the number assigned
		 * to this handler to make sure an event will only be processed a single time.
		 */
		if (sequence % outOf == ordinal) {
			lastSequence = sequence;
			globalLad.insert(event.data);
			
			/**
			 * Once we are done with this event we need to set the buffer to null so it can be GC'd.  If we don't 
			 * the buffers can end up in old gen space because the events are never GC'd and this causes out of memory
			 * issues if the ring buffer size is large.  We are guarenteed to be the only handler working on this event at 
			 * any time so this is a safe procedure.
			 */
			event.setData(null);
		}
	}
}
