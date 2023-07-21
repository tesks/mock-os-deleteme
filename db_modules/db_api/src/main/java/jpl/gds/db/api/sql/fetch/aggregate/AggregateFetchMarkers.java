package jpl.gds.db.api.sql.fetch.aggregate;

import org.slf4j.IMarkerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

public class AggregateFetchMarkers {
    private static final IMarkerFactory factory            = MarkerFactory.getIMarkerFactory();
    
    /** Channel Aggregate Fetch App */
    public static final Marker          CHANNEL_AGGREGATE_FETCH_APP        = factory.getMarker("CHANNEL_AGGREGATE_FETCH_APP");
    
    /** QUERY_COORDINATOR */
    public static final Marker          QUERY_COORDINATOR                  = factory.getMarker("QUERY_COORDINATOR");
    
    /** Batch Processor */
    public static final Marker          BATCH_PROCESSOR                    = factory.getMarker("BATCH_PROCESSOR");
    
    /** Aggregate Batch Processor */
    public static final Marker          AGGREGATE_BATCH_PROCESSOR          = factory.getMarker("AGGREGATE_BATCH_PROCESSOR");

    /** File Based Sorting Processor */
    public static final Marker          FILE_BASED_SORTING_PROCESSOR       = factory.getMarker("FILE_BASED_SORTING_PROCESSOR");
    
    
    /** Query Stream Processor */
    public static final Marker          QUERY_STREAM_PROCESSOR             = factory.getMarker("QUERY_STREAM_PROCESSOR");
    
    /** Output Controller */
    public static final Marker          OUTPUT_CONTROLLER                  = factory.getMarker("OUTPUT_CONTROLLER");

    /** Non Sorting Merge */
    public static final Marker          NON_SORTING_MERGE                  = factory.getMarker("NON_SORTING_MERGE");

    /** Sorting Merge */
    public static final Marker          SORTING_MERGE                      = factory.getMarker("SORTING_MERGE");

    /** Parallel Merge Sort */
    public static final Marker          PARALLEL_MERGE_SORT                = factory.getMarker("PARALLEL_MERGE_SORT");

    /** Intermediary Batch Merge */
    public static final Marker          INTERMEDIARY_BATCH_MERGE           = factory.getMarker("INTERMEDIARY_BATCH_MERGE");

   
}
