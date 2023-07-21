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
package jpl.gds.db.impl.aggregate.batch.merge;

import jpl.gds.db.api.sql.fetch.aggregate.AggregateFetchException;
import jpl.gds.db.api.sql.fetch.aggregate.AggregateFetchMarkers;
import jpl.gds.db.api.sql.fetch.aggregate.IEhaAggregateQueryCoordinator;
import jpl.gds.shared.log.Tracer;

public class NonSortBatchMerge implements Runnable {

	private final Tracer trace;
	private final IEhaAggregateQueryCoordinator ehaAggregateQueryQoordinator;

	public NonSortBatchMerge(final IEhaAggregateQueryCoordinator ehaAggregateQueryQoordinator, final Tracer trace) {
		this.ehaAggregateQueryQoordinator = ehaAggregateQueryQoordinator;
		this.trace = trace;
	}
	
	@Override
	public void run() {

		while (ehaAggregateQueryQoordinator.dataIsBeingProcessed()) {
			
			if (ehaAggregateQueryQoordinator.nextBatchReady()) {
				trace.debug(AggregateFetchMarkers.NON_SORTING_MERGE,
				        "Pushing batch to QUEUE for output: " 
				        + ehaAggregateQueryQoordinator.getNextBatchId());
				try {
                    ehaAggregateQueryQoordinator.pushBatchToOutputController();
                } catch (final AggregateFetchException e) {
                    trace.error(AggregateFetchMarkers.NON_SORTING_MERGE,
                            "Encountered "
                            + "an error while pushing the next batch to the output controller: " + e.getMessage());
                }
			} else {
				trace.debug(AggregateFetchMarkers.NON_SORTING_MERGE,
				        "Waiting for next batch to become ready: " 
				        + ehaAggregateQueryQoordinator.getNextBatchId());
				try {
					Thread.sleep(1000);
				} catch (final InterruptedException e) {
				    trace.error(AggregateFetchMarkers.NON_SORTING_MERGE, "Interrupted while "
				            + "waiting for data processing to complete: " + e.getMessage());
					Thread.currentThread().interrupt();
				}
			}
		}
	}
}

