package jpl.gds.db.impl.aggregate.batch.merge;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;

import jpl.gds.db.api.sql.fetch.aggregate.*;
import jpl.gds.shared.log.Tracer;

/**
 * This class handles the reduction of temporary batch files. 
 * When the aggregate query result stream is significantly large, too many temporary
 * batch files get generated which can cause the final merge processor to hit the 
 * open file handle limit.
 *
 */
public class ParallelMergeSort implements Runnable {

	private final ExecutorService executor;
	private final AtomicBoolean running = new AtomicBoolean();
	private final IAggregateFetchConfig config;
	private final IEhaAggregateQueryCoordinator coordinator;
	private final IBatchReaderFactory batchReaderFactory;
	private final IIndexReaderFactory indexReaderFactory;
	private final IBatchWriterFactory batchWriterFactory;
	private final Tracer trace;

	/**
	 * Constructor
	 * 
	 * @param coordinator
	 * @param executor
	 * @param batchReaderFactory
	 * @param indexReaderFactory
	 * @param config
	 * @param trace
	 */
	public ParallelMergeSort(final IEhaAggregateQueryCoordinator coordinator,
							 final ExecutorService executor,
							 final IBatchReaderFactory batchReaderFactory,
							 final IIndexReaderFactory indexReaderFactory,
							 final IBatchWriterFactory batchWriterFactory,
	        				 final IAggregateFetchConfig config,
							 final Tracer trace) {
		this.executor = executor;
		this.coordinator = coordinator;
		this.batchReaderFactory = batchReaderFactory;
		this.indexReaderFactory = indexReaderFactory;
		this.batchWriterFactory = batchWriterFactory;
		this.config = config;
		this.trace = trace;
	}
	
	@Override
	public void run() {
	    trace.debug(AggregateFetchMarkers.PARALLEL_MERGE_SORT, "Starting");
		running.set(true);
		
		while(running.get()) {
			
			if (coordinator.readyForIntermediateMerge()) {
				generateMergeProcessors();
			} else {
				try {
					Thread.sleep(500);
				} catch (final InterruptedException e) {
					trace.error(AggregateFetchMarkers.PARALLEL_MERGE_SORT, 
					        "Encountered an unexpected InterruptedException while waiting: " + e.getMessage());
					Thread.currentThread().interrupt();
					shutdown();
				}
			}
		}
		trace.debug(AggregateFetchMarkers.PARALLEL_MERGE_SORT, "Finished");
	}
	
	private void generateMergeProcessors() {
		final Iterator<Entry<String, ProcessedBatchInfo>> batchIterator = coordinator.getCacheMapIterator();
		BatchSetContainer batchSetContainer = new BatchSetContainer();
		Entry<String, ProcessedBatchInfo> batchEntry;
		String batchId;
		IntermediaryBatchMerge intermediaryMerger;
		
		trace.debug(AggregateFetchMarkers.PARALLEL_MERGE_SORT, "Setting Coordinator state to Starting Processors");
		coordinator.startingMergeProcessors();
		
		while (batchIterator.hasNext()) {
	    	batchEntry = batchIterator.next();
	        batchId = batchEntry.getKey();
			
			if (batchSetContainer.size() < 4) {
				batchSetContainer.add(batchId, batchEntry.getValue());
			} else {
				
				intermediaryMerger = new IntermediaryBatchMerge(
				        coordinator, 
				        batchSetContainer, 
				        batchReaderFactory, 
				        indexReaderFactory,
				        batchWriterFactory,
				        config, 
				        trace);
				coordinator.incrementIntermediaryBatchCount();
				
				trace.debug(AggregateFetchMarkers.PARALLEL_MERGE_SORT, "Submitting Merge Processor to Executor");
				executor.execute(intermediaryMerger);
				
				batchSetContainer = new BatchSetContainer();
				batchSetContainer.add(batchId, batchEntry.getValue());
				
			}
		}
		
		intermediaryMerger = new IntermediaryBatchMerge(
		        coordinator, 
		        batchSetContainer, 
		        batchReaderFactory, 
		        indexReaderFactory,
		        batchWriterFactory,
		        config, 
		        trace);
		coordinator.incrementIntermediaryBatchCount();
		trace.debug(AggregateFetchMarkers.PARALLEL_MERGE_SORT, "Submitting Merge Processor to Executor");
		executor.execute(intermediaryMerger);
	}
		
	public boolean isRunning() {
		return running.get();
	}

	public void shutdown() {
	    trace.debug(AggregateFetchMarkers.PARALLEL_MERGE_SORT, "Received request to Shutdown");
		running.set(false);
		executor.shutdown();
	}
}
