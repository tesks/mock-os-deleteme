package jpl.gds.db.api.sql.fetch.aggregate;

import java.util.List;

/**
 * An interface to be implemented by Channel Stream Filters which are designed
 * to be applied by the Output Controller component of the EHA Aggregate query
 * process. Output Controller receives the record stream in its final sorted order
 * and hands it off to the configured output stream consumer.
 *
 * @param <T>
 */
public interface IChannelStreamFilter<T> {
    
    /**
     * Applies a filter to the incoming record list and returns
     * a new list containing the filtered records.
     * 
     * @param recordList the record list which needs to be filtered
     * @return the filtered record list
     */
    public List<T> filterRecordList(final List<T> recordList);

}
