package jpl.gds.db.impl.aggregate.batch.merge;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicBoolean;

import jpl.gds.db.api.sql.fetch.aggregate.*;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Tracer;

/**
 * This class merges a set of temporary batches into a single batch file.
 *
 */
public class IntermediaryBatchMerge implements Runnable {
	
	private final AtomicBoolean running = new AtomicBoolean();
	private final IEhaAggregateQueryCoordinator coordinator;
	private final IBatchReaderFactory batchReaderFactory;
	private final IIndexReaderFactory indexReaderFactory;
	private final Tracer trace;
	private final IAggregateFetchConfig config;
	private final BatchSetContainer batchSetContainer;
	private final IBatchWriterFactory batchWriterFactory;

	/**
	 * Constructor
	 * 
	 * @param coordinator the query coordinator
	 * @param batchSetContainer the batch set container
	 * @param batchReaderFactory the batch reader factory
	 * @param indexReaderFactory the index reader factory
	 * @param config the fetch configuration
	 * @param trace the tracer
	 */
	public IntermediaryBatchMerge(
			final IEhaAggregateQueryCoordinator coordinator,
			final BatchSetContainer batchSetContainer,
			final IBatchReaderFactory batchReaderFactory,
			final IIndexReaderFactory indexReaderFactory,
			final IBatchWriterFactory batchWriterFactory,
			final IAggregateFetchConfig config,
			final Tracer trace) {
		this.coordinator = coordinator;
		this.batchSetContainer = batchSetContainer;
		this.batchReaderFactory = batchReaderFactory;
		this.indexReaderFactory = indexReaderFactory;
		this.batchWriterFactory = batchWriterFactory;
		this.config = config;
		this.trace = trace;
	}

	@Override
	public void run() {
		running.set(true);
		try {
            mergeBatchSet(batchSetContainer);
        } catch (AggregateFetchException | IOException e) {
            trace.error(AggregateFetchMarkers.INTERMEDIARY_BATCH_MERGE, 
                    "Encountered error while merging batch set: " +
                    e.getMessage());
        }
	}
	
	// TODO: Quite a bit of the code here is the same as the MergeBatchSort class.
	private void mergeBatchSet(final BatchSetContainer batchSetContainer) throws AggregateFetchException, IOException {

	    final Map<String, IBatchFileRecordProvider> batchFileRecordProviderMap = new HashMap<>();
	    final Map<String, IBatchFileIndexProvider> batchFileIndexProviderMap = new HashMap<>();
	        
	    final Map<String, Iterator<String>> batchFileReaderMap = new HashMap<>();
	    final Map<String, Iterator<ComparableIndexItem<String>>> batchIndexReaderMap = new HashMap<>();

		final Map<String, String> batchRecordMap = new HashMap<>();
		final Map<String, ComparableIndexItem<String>> batchIndexMap = new HashMap<>();
		
		final List<ComparableIndexItem<String>> sortList = new ArrayList<>();
		final Iterator<Entry<String, ProcessedBatchInfo>> batchIterator = batchSetContainer.getBatchSet().entrySet().iterator();
		
		String batchId;
        IBatchFileRecordProvider recordProvider;
        Iterator<String> recordIterator;
        
        IBatchFileIndexProvider indexProvider;
        Iterator<ComparableIndexItem<String>> indexIterator;

		Map.Entry<String, ProcessedBatchInfo> batchEntry;
				
		// Go through all of the batch records and prepare the necessary maps and sort list to do the 
		// actual merge
		// 1. Construct a Batch Record Provider for each batch file, which is essentially a file reader
		// 2. Store the Batch Record Provider in a Map using the Batch Id as the key
		// 3. Get the Sorted Index Iterator 
	    while (batchIterator.hasNext()) {
	        
	    	batchEntry = batchIterator.next();
	        batchId = batchEntry.getKey();

			try {
				recordProvider = batchReaderFactory.getBatchRecordProvider(coordinator, batchId);
				recordIterator = recordProvider.iterator();

				indexProvider = indexReaderFactory.getBatchIndexProvider(coordinator, batchId);
				indexIterator = indexProvider.iterator();

				batchFileRecordProviderMap.put(batchId, recordProvider);
				batchFileIndexProviderMap.put(batchId, indexProvider);

				batchFileReaderMap.put(batchId, recordIterator);
				batchIndexReaderMap.put(batchId, indexIterator);

				// Get the sorted index iterator for each batch
				// 1. Add the first index of each batch to the sort list
				//    - Once this while loop completes, the sort list will contain the
				//      first index from all batch files
				// 2. Store the sorted index iterator in a Map again using the Batch ID as the key
				// 3. Store the very first record of each batch in a Map using the Batch Id as the key
				if (indexIterator.hasNext() && recordIterator.hasNext()) {
					batchIndexMap.put(batchId, indexIterator.next());
					batchRecordMap.put(batchId, recordIterator.next());
					sortList.add(batchIndexMap.get(batchId));
				}
			} catch (final AggregateFetchException e) {
				// MCSECLIV-1003: skip processing if file no longer exists
				// it might have been cleaned up as part of ctrl+c
				trace.trace(ExceptionTools.getMessage(e), e);
				this.coordinator.removeBatch(batchId);
			}
	    }
	    
	    String mergedTempRecFileName = config.getChunkDir() + "/IntermediaryMerged_" + System.nanoTime() + "_";
	    String mergedTempIndexFileName;

	    mergedTempIndexFileName = mergedTempRecFileName + "tcif.sorted";
	    mergedTempRecFileName += "tcf.sorted";

		// Create the intermediary merged record file
		// MPCS-12308: Formatting large chill_get_chanvals queries
		// returns no data
		// MPCS-12308 clones MPCS-12070
		// Comments of MPCS-12070 contains a lot of information with JP identifying the root cause.
		// Issue is resolved with the use of the correct Batch Writer
		final IStringWriter recBatchWriter = batchWriterFactory.getBatchWriter(mergedTempRecFileName);
		try (BufferedWriter indexBuffWriter = new BufferedWriter(new FileWriter(mergedTempIndexFileName))) {

			boolean batchStart = true;
			ComparableIndexItem<String> sortedItem;
			String outputRecord;
			String outputIndex;

			// This is where the actual merge happens
			// sortList contains one record from each batch at any given time.
			// Is empty once all records from all batches have been sorted and merged.
			while (!sortList.isEmpty()) {
				if (batchStart) {
					trace.debug(AggregateFetchMarkers.INTERMEDIARY_BATCH_MERGE, "Merge Sort Batch Start");
					batchStart = false;
				}

				Collections.sort(sortList, ComparableIndexItem.NATURAL_ORDER);

				sortedItem = sortList.get(0);

				outputIndex = sortedItem.getComparable();
				outputRecord = batchRecordMap.get(sortedItem.getBatchId());

				recordIterator = batchFileReaderMap.get(sortedItem.getBatchId());
				indexIterator = batchIndexReaderMap.get(sortedItem.getBatchId());

				if (indexIterator.hasNext() && recordIterator.hasNext()) {
					batchRecordMap.put(sortedItem.getBatchId(), recordIterator.next());
					batchIndexMap.put(sortedItem.getBatchId(), indexIterator.next());
					sortList.set(0, batchIndexMap.get(sortedItem.getBatchId()));

				}
				else {
					trace.debug(AggregateFetchMarkers.INTERMEDIARY_BATCH_MERGE, "Finished Batch: " + sortedItem.getBatchId());

					final String batchRecordFile = coordinator.getBatch(sortedItem.getBatchId()).getRecordFilename();
					trace.debug(AggregateFetchMarkers.INTERMEDIARY_BATCH_MERGE, "Deleting batch record file: " + batchRecordFile);
					try {
						new File(batchRecordFile).delete();
					}
					catch (Exception e) {
						trace.debug(ExceptionTools.getMessage(e));
					}

					final String indexFileName = coordinator.getBatch(sortedItem.getBatchId()).getIndexFilename();
					trace.debug(AggregateFetchMarkers.INTERMEDIARY_BATCH_MERGE, "Deleting batch index file: " + indexFileName);
					try {
						new File(indexFileName).delete();
					}
					catch (Exception e) {
						trace.debug(ExceptionTools.getMessage(e));
					}

					batchFileRecordProviderMap.get(sortedItem.getBatchId()).close();
					batchFileIndexProviderMap.get(sortedItem.getBatchId()).close();

					batchIndexMap.remove(sortedItem.getBatchId());
					batchRecordMap.remove(sortedItem.getBatchId());

					sortList.remove(0);
				}

				// Write record to file
				recBatchWriter.write(outputRecord);
				// write index to file
				indexBuffWriter.write(outputIndex + "\n");
			}

			recBatchWriter.close();
			indexBuffWriter.close();
		}

		trace.debug(AggregateFetchMarkers.INTERMEDIARY_BATCH_MERGE,
	            "Finished File: " + mergedTempRecFileName);
	    final ProcessedBatchInfo batchInfo = new ProcessedBatchInfo(mergedTempRecFileName, mergedTempIndexFileName);
	    batchSetContainer.setBic(batchInfo);
	    coordinator.addIntermediaryMergedBatchToMap(batchSetContainer);
	}
	
	public boolean isRunning() {
		return running.get();
	}
	
	public void shutdown() {
		running.set(false);;
	}
}
