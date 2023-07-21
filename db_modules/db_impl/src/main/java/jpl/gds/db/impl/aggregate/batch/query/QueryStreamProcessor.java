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
package jpl.gds.db.impl.aggregate.batch.query;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.fetch.aggregate.AggregateFetchException;
import jpl.gds.db.api.sql.fetch.aggregate.AggregateFetchMarkers;
import jpl.gds.db.api.sql.fetch.aggregate.IQueryCoordinator;
import jpl.gds.db.api.sql.fetch.aggregate.IQueryStreamProcessor;
import jpl.gds.db.api.sql.fetch.aggregate.IRecordBatchHandler;
import jpl.gds.db.api.sql.fetch.aggregate.RecordBatchContainer;
import jpl.gds.shared.log.Tracer;

/**
 * The is the base class used to process a stream of database records.
 *
 * @param <T>
 */
public abstract class QueryStreamProcessor<T> implements IQueryStreamProcessor<T> {
	
	protected Tracer trace;
	protected AtomicBoolean running = new AtomicBoolean();
	private final IRecordBatchHandler<T> batchHandler;
	private final IQueryCoordinator queryCoordinator;
	
	/**
	 * Constructor.
	 * 
	 * @param aggQueryCoordinator the query coordinator
	 * @param batchHandler the batch handler 
	 * @param trace the tracer
	 */
	public QueryStreamProcessor(
	        final IQueryCoordinator aggQueryCoordinator, 
	        final IRecordBatchHandler<T> batchHandler,
	        final Tracer trace) {
		this.queryCoordinator = aggQueryCoordinator;
		this.trace = trace;
		this.batchHandler = batchHandler;
	}

	@Override
    public void run() {
	    trace.debug(AggregateFetchMarkers.QUERY_STREAM_PROCESSOR, 
                "Processing Started");
	    running.set(true);
		startProcessing();
		running.set(false);
	    trace.debug(AggregateFetchMarkers.QUERY_STREAM_PROCESSOR, 
	                "Processing Finished");
	}
	
	private void startProcessing() {
		List<T> recordBatch = new ArrayList<>();
		while(running.get()) {
			try {
			    trace.debug(AggregateFetchMarkers.QUERY_STREAM_PROCESSOR, 
			            "Started Batching Records");
				recordBatch = batchRecords();
                trace.debug(AggregateFetchMarkers.QUERY_STREAM_PROCESSOR, 
                        "Finished Batching Records");
			} catch (final DatabaseException e) {
				trace.error(AggregateFetchMarkers.QUERY_STREAM_PROCESSOR, 
				        "Encountered an unexpected DatabaseException: " + e.getMessage());
				running.set(false);
			}
			
			// Construct and RecordBatchContainer and send it to the Batch Handler
			if (recordBatch != null && !recordBatch.isEmpty()) {
				final String batchId = queryCoordinator.generateBatchId();
				trace.debug(AggregateFetchMarkers.QUERY_STREAM_PROCESSOR, 
				        "Publishing batch with ID: " + batchId);
				try {
                    batchHandler.handleRecordBatch(
                            new RecordBatchContainer<T>(batchId, new ArrayList<>(recordBatch)));
                } catch (final AggregateFetchException e) {
                    trace.error(AggregateFetchMarkers.QUERY_STREAM_PROCESSOR, "Encountered an unexpected "
                            + "exception when submitting the Record Batch Container "
                            + "to the Batch Handler: " + e.getMessage());
                    running.set(false);
                }
				recordBatch.clear();
				recordBatch = null;
			} else {
			    // If the record batch is empty we have reached the end of the query stream
			    running.set(false);
			}
		}
	}
	
    @Override
    public boolean isRunning() {
        return running.get();
    }
}
