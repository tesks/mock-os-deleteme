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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import jpl.gds.db.api.sql.fetch.aggregate.AggregateFetchException;
import jpl.gds.db.api.sql.fetch.aggregate.AggregateFetchMarkers;
import jpl.gds.db.api.sql.fetch.aggregate.ComparableIndexItem;
import jpl.gds.db.api.sql.fetch.aggregate.IAggregateFetchConfig;
import jpl.gds.db.api.sql.fetch.aggregate.IBatchFileIndexProvider;
import jpl.gds.db.api.sql.fetch.aggregate.IBatchFileRecordProvider;
import jpl.gds.db.api.sql.fetch.aggregate.IBatchReaderFactory;
import jpl.gds.db.api.sql.fetch.aggregate.IEhaAggregateQueryCoordinator;
import jpl.gds.db.api.sql.fetch.aggregate.IIndexReaderFactory;
import jpl.gds.db.api.sql.fetch.aggregate.ProcessedBatchInfo;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Tracer;

/**
 * This class handles the external merge sort of processed batch files.
 * When aggregate query data processing reaches this state, its expected for the
 *
 */
public class SortBatchMerge implements Runnable {

	private final Tracer trace;
	private final IAggregateFetchConfig config;
	private final IBatchReaderFactory batchReaderFactory;
	private final IEhaAggregateQueryCoordinator aggQueryQoordinator;
	private final IIndexReaderFactory indexReaderFactory;

	/**
	 * Constructor
	 * 
	 * @param aggQueryQoordinator the query coordinator
	 * @param config the fetch configuration
	 * @param batchReaderFactory the batch record reader factory
	 * @param indexReaderFactory the batch index reader factory
	 * @param trace the tracer
	 */
	public SortBatchMerge(
			final IEhaAggregateQueryCoordinator aggQueryQoordinator, 
			final IAggregateFetchConfig config, 
			final IBatchReaderFactory batchReaderFactory,
			final IIndexReaderFactory indexReaderFactory,
			final Tracer trace) {
		this.aggQueryQoordinator = aggQueryQoordinator;
		this.config = config;
		this.batchReaderFactory = batchReaderFactory;
		this.indexReaderFactory = indexReaderFactory;
		this.trace = trace;
	}
		
	@Override
	public void run() {
		
		// Wait for all batches to be prepared
		while(!aggQueryQoordinator.batchFilesReadyForFinalMerge()) {
			try {
				Thread.sleep(500);
			} catch (final InterruptedException e) {
				trace.error(AggregateFetchMarkers.SORTING_MERGE, "Encountered an unexpected "
				        + "InterruptedException while waiting for batch "
				        + "files be prepared for final merge: " + e.getMessage());
			}
		}
		
		trace.debug(AggregateFetchMarkers.SORTING_MERGE, "Starting the final merge process");
		
		// Start Merging Batch files
		try {
            mergeBatchFiles();
        } catch (AggregateFetchException | IOException e) {
            trace.error(AggregateFetchMarkers.SORTING_MERGE, "Encountered an unexpected "
                    + " exception while merging batch files: " + e.getMessage());
            e.printStackTrace();
        }
		trace.debug(AggregateFetchMarkers.SORTING_MERGE, "Finished the final merge process");
	}

	
	private void mergeBatchFiles() throws AggregateFetchException, IOException {
		
	    final Map<String, IBatchFileRecordProvider> batchFileRecordProviderMap = new HashMap<>();
	    final Map<String, IBatchFileIndexProvider> batchFileIndexProviderMap = new HashMap<>();
	    
		final Map<String, Iterator<String>> batchFileReaderMap = new HashMap<>();
		final Map<String, Iterator<ComparableIndexItem<String>>> batchIndexReaderMap = new HashMap<>();
		
		final Map<String, String> batchRecordMap = new HashMap<>();
		final Map<String, ComparableIndexItem<String>> batchIndexMap = new HashMap<>();
		
		final List<ComparableIndexItem<String>> sortList = new ArrayList<>();
		
		final Iterator<Entry<String, ProcessedBatchInfo>> batchIterator = aggQueryQoordinator.getCacheMapIterator();
		
		String batchId;
		IBatchFileRecordProvider recordProvider;
		Iterator<String> recordIterator;
		
		IBatchFileIndexProvider indexProvider;
		Iterator<ComparableIndexItem<String>> indexIterator;
		
		Map.Entry<String, ProcessedBatchInfo> batchEntry;
		
		trace.debug(AggregateFetchMarkers.SORTING_MERGE, "Initializing necessary Maps");
		
		// Go through all of the batch records and prepare the necessary maps and sort list to do the 
		// actual merge
		// 1. Construct a Batch Record Provider for each batch file, which is essentially a file reader
		// 2. Store the Batch Record Provider in a Map using the Batch Id as the key
		// 3. Get the Sorted Index Iterator 
	    while (batchIterator.hasNext()) {
	    	batchEntry = batchIterator.next();
	        batchId = batchEntry.getKey();

			try {
				recordProvider = batchReaderFactory.getBatchRecordProvider(aggQueryQoordinator, batchId);
				recordIterator = recordProvider.iterator();

				indexProvider = indexReaderFactory.getBatchIndexProvider(aggQueryQoordinator, batchId);
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
			} catch (AggregateFetchException fe) {
				// MCSECLIV-1003: skip processing if file no longer exists
				// it might have been cleaned up as part of ctrl+c
				trace.trace(ExceptionTools.getMessage(fe), fe);
				aggQueryQoordinator.removeBatch(batchId);

			}

	    }
	    
	    long sortTimeTotal = 0;
	    long fileReadlineTotal = 0;
	    boolean batchStart = true;
	    long mergeSortBatchStart = 0;
	    long batchFileReadLineTotal = 0;
	    ComparableIndexItem<String> sortedItem;
	    String outputRecord;
	    
	    final List<String> outputRecBatch = new ArrayList<>();
	    
	    // This is where the actual merge happens
	    // sortList contains one record from each batch at any given time.
	    // Is empty once all records from all batches have been sorted and merged.
	    while (!sortList.isEmpty()) {
	    	if (batchStart) {
	    		trace.debug(AggregateFetchMarkers.SORTING_MERGE, "Starting the actual merge sort");
	    		mergeSortBatchStart = System.nanoTime();
	    		batchStart = false;
	    		batchFileReadLineTotal = 0;
	    	}
	    	
	    	final long sortStart = System.nanoTime();
	    	Collections.sort(sortList, ComparableIndexItem.NATURAL_ORDER);
		    
		    sortTimeTotal += (System.nanoTime() - sortStart);
		    
		    sortedItem = sortList.get(0);
		    outputRecord = batchRecordMap.get(sortedItem.getBatchId());
		    
		    final long fileReadlineStart = System.nanoTime();
		    
		    recordIterator = batchFileReaderMap.get(sortedItem.getBatchId());
		    indexIterator = batchIndexReaderMap.get(sortedItem.getBatchId());
		    
		    if (indexIterator.hasNext() && recordIterator.hasNext()) {
			    batchRecordMap.put(sortedItem.getBatchId(), recordIterator.next());
			    batchIndexMap.put(sortedItem.getBatchId(), indexIterator.next());
			    sortList.set(0, batchIndexMap.get(sortedItem.getBatchId()));

		    } else {
		    	trace.debug(AggregateFetchMarkers.SORTING_MERGE, "Finished Batch: " + sortedItem.getBatchId());
		    	
		    	if (!config.isKeepTempFiles()) {
		    		final String batchRecordFile = aggQueryQoordinator.getBatch(sortedItem.getBatchId()).getRecordFilename();
		    		trace.debug(AggregateFetchMarkers.SORTING_MERGE, "Deleting batch record file: " +  batchRecordFile);
					try {
						new File(batchRecordFile).delete();
					} catch (Exception e) {
						trace.debug(ExceptionTools.getMessage(e));
					}
		    		
                    final String indexFileName = aggQueryQoordinator.getBatch(sortedItem.getBatchId())
                            .getIndexFilename();
                    trace.debug(AggregateFetchMarkers.SORTING_MERGE, "Deleting batch index file: " + indexFileName);
					try {
						new File(indexFileName).delete();
					} catch (Exception e) {
						trace.debug(ExceptionTools.getMessage(e));
					}
		    		
		    	}
		    	
		    	batchFileRecordProviderMap.get(sortedItem.getBatchId()).close();
		    	batchFileIndexProviderMap.get(sortedItem.getBatchId()).close();

		    	aggQueryQoordinator.removeBatch(sortedItem.getBatchId());
		    	batchIndexMap.remove(sortedItem.getBatchId());
		    	batchRecordMap.remove(sortedItem.getBatchId());
		    	
		    	sortList.remove(0);
		    }
		    
		    batchFileReadLineTotal += (System.nanoTime() - fileReadlineStart); 
		    fileReadlineTotal += batchFileReadLineTotal;
		    
		    outputRecBatch.add(outputRecord);
		    
		    if (outputRecBatch.size() >= 500000) {
		    	final long newArrayStart = System.nanoTime();
		    	
		    	trace.debug(AggregateFetchMarkers.SORTING_MERGE, "Batch File Read Time = " 
		    			+ (batchFileReadLineTotal)/1000000.0 + " msecs");
		    	
		    	trace.debug(AggregateFetchMarkers.SORTING_MERGE, "Array Copy Time = " 
		    			+ (System.nanoTime() - newArrayStart)/1000000.0 + " msecs");
		    	
		    	trace.debug(AggregateFetchMarkers.SORTING_MERGE, "Batch Preparation Time = " 
		    			+ (System.nanoTime() - mergeSortBatchStart)/1000000.0 + " msecs");
		    	
		    	trace.debug(AggregateFetchMarkers.SORTING_MERGE, "Start Sending Batch to Output Controller");
		    	
		    	aggQueryQoordinator.pushBatchToOutputController(new ArrayList<String>(outputRecBatch));
		    	
		    	trace.debug(AggregateFetchMarkers.SORTING_MERGE, "Start Sending Batch to Output Controller, QUEUE size = " + aggQueryQoordinator.getOutputQueueSize());
		    	batchStart = true;
		    	final long batchClearStart = System.nanoTime();
		    	outputRecBatch.clear();
		    	trace.debug(AggregateFetchMarkers.SORTING_MERGE, "Batch Clear Time = " 
		    			+ (System.nanoTime() - batchClearStart)/1000000.0 + " msecs");
		    }
	    }	 
	    
		aggQueryQoordinator.pushBatchToOutputController(outputRecBatch);
	    trace.debug(AggregateFetchMarkers.SORTING_MERGE, "File Read Line Total Time = " + (fileReadlineTotal)/1000000.0 + " msecs");
	    trace.debug(AggregateFetchMarkers.SORTING_MERGE, "Merge Sort Total Time = " + (sortTimeTotal)/1000000.0 + " msecs");
	}
}

