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
package jpl.gds.db.api.sql.fetch.aggregate;

import java.io.File;
import java.sql.ResultSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;


/**
 * An interface implemented by the EHA Aggregate Query Coordinator.
 * The coordinator manages all components involved during EHA Aggregate
 * query processing.
 *
 */
public interface IEhaAggregateQueryCoordinator extends IQueryCoordinator {
    
	/**
	 * Add the batch identified by the Batch ID and 
	 * 
	 * @param batchId the batch ID
	 * @param indexCacheItem 
	 */
	public void addBatchToCacheMap(final String batchId, final ProcessedBatchInfo indexCacheItem);
	
	/**
	 * Check to see if data is being processed or not
	 * 
	 * @return True if data is still being processed, false otherwise
	 */
	public boolean dataIsBeingProcessed();

	/**
	 * Check to see if any spawned threads are still running
	 * @return True if any spawned threads are still alive
	 */
	boolean threadsAlive();
	
	/**
	 * Check to see if all batch files are ready to star the final merge
	 * 
	 * @return
	 */
	public boolean batchFilesReadyForFinalMerge();
	
	/**
	 * Set the ResultSet
	 * 
	 * @param resultSet 
	 */
	public void setResultSet(final ResultSet resultSet);
	
	/**
	 * Check to see if the next batch is ready
	 * 
	 * @return True if next batch is ready, false otherwise
	 */
	public boolean nextBatchReady();
	
	/**
	 * Send the next batch to the Output Controller 
	 * @throws AggregateFetchException 
	 */
	public void pushBatchToOutputController() throws AggregateFetchException;
	
	/**
	 * Get the next batch id
	 * 
	 * @return the next batch id
	 */
	public String getNextBatchId();
	
	/**
	 * Initiate Shutdown of the components
	 * @throws AggregateFetchException 
	 */
	public void initiateShutdown() throws AggregateFetchException;
	
	/**
	 * Get the Cache Map Iterator
	 * 
	 * @return the Iterator
	 */
	public Iterator<Entry<String, ProcessedBatchInfo>> getCacheMapIterator();
	
	/**
	 * Get the ProcessedBatchInfo using the batch id
	 * 
	 * @param batchId the batch id
	 * @return 
	 */
	public ProcessedBatchInfo getBatch(final String batchId);
	
	/**
	 * Remove the batch information from the Record Map associated with the specified batch id
	 * 
	 * @param batchId the batch id
	 */
	public void removeBatch(final String batchId);
	
	/**
	 * Send the specified record batch to the Output Controller
	 * 
	 * @param outputRecBatch 
	 * @throws AggregateFetchException 
	 */
	public void pushBatchToOutputController(final List<String> outputRecBatch) throws AggregateFetchException;
	
	/**
	 * Get the output record queue size
	 * 
	 * @return the output queue size
	 */
	public int getOutputQueueSize();
	
	/**
	 * Add the intermediary merged batch to the record map
	 * 
	 * @param batchSetContainer 
	 */
	public void addIntermediaryMergedBatchToMap(final BatchSetContainer batchSetContainer);
	
	/**
	 * Check to see if batches are ready for intermediary merge
	 * 
	 * @return True if ready, false otherwise
	 */
	public boolean readyForIntermediateMerge();
	
	
	/**
	 * Increment the intermediary batch count
	 */
	public void incrementIntermediaryBatchCount();
	
	/**
	 * Set the state to starting merge processors
	 */
	public void startingMergeProcessors();

	/**
	 * Remove the batch information from the Record Map associated with the specified batch id
	 *
	 * @param batchId the batch id
	 */
	public void removeBatchIdFromBatchIdList(final String batchId);

	/**
	 * Clean up temporary files and remove directory
	 */
	public void cleanUpTempFiles();
}
