package jpl.gds.db.api.sql.store;

/**
 * An interface implemented by the AggregateStoreMonitor for the purpose
 * of keeping track of record counts specific to EHA Aggregation. 
 * There is a mechanism in place for transferring count information 
 * from the LDI Stores to the Inserter for all Stores but aggregates have
 * 2 types of counts which is different from the rest of the Stores and specific
 * to ChannelAggregate stores only. The 2 types of counts are the number of 
 * aggregate records and the number of total channel samples contained
 * within the aggregate records. One aggregate is configured to contain 200 channel
 * samples as of the initial implementation.
 *  
 * The store monitor is used to transfer count information from the Stores
 * to the Inserter for aggregates. 
 * 
 * Note: The Gatherer which functions as a coordinator between Stores and 
 * Inserter operate asynchronously and the Gatherer flushes the Store stream 
 * when the stream have enough elements (LDI file records or logical records 
 * which get stored into single rows in the database).
 * 
 * When the Gatherer closes the current LDI file and puts it onto the Queue
 * for the Inserter to pick up, another stream is immediately opened for the next 
 * LDI file and the Store starts writing LDI records to it. There are essentially 
 * 2 counts that exist simultaneously: In Progress count and Ready count
 *  
 * In Progress Record Count is used to keep track of the channel samples contained
 * within aggregates while the current LDI file is still being written to by the 
 * Channel Aggregate LDI Store. 
 * 
 * The Ready count is the total number of channel samples contained in the LDI
 * file received by the Inserter.
 * 
 * The Ready For Insert Record Count should be set to the In Progress Record Count
 * when the Gatherer closes the current LDI file and hands it off to the Inserter.
 *
 */
public interface IAggregateStoreMonitor extends IStoreMonitor {
    
    /**
     * Increment the in progress record count by the provided amount.
     * The incCount should be the channel samples contained in the aggregate.
     * 
     * @param incCount the increment amount
     */
    void incInProgressRecordCount(final int incCount);
    
    /**
     * Clear the in progress record count (channel sample count)
     */
    void clearInProgressRecordCount();
    
    /**
     * Get the in progress record count (channel sample count)
     * 
     * @return the number of channel samples in the currently open
     * LDI file
     */
    long getInProgressRecordCount();
    
    /**
     * Set the ready for insert record count to the specified count
     * 
     * @param count the number of channel samples in the LDI file
     * that is ready to be inserted into the database
     */
    void setReadyForInsertCount(final long count);
    
    /**
     * Get the ready for insert record count (channel sample count)
     * 
     * @return the ready for insert total channel sample count
     */
    long getReadyForInsertCount();
    
}
