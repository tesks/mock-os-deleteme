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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jpl.gds.db.api.DatabaseException;
import jpl.gds.db.api.sql.fetch.aggregate.AggregateFetchMarkers;
import jpl.gds.db.api.sql.fetch.aggregate.AggregateRecord;
import jpl.gds.db.api.sql.fetch.aggregate.IAggregateFetchConfig;
import jpl.gds.db.api.sql.fetch.aggregate.IEhaAggregateDbRecord;
import jpl.gds.db.api.sql.fetch.aggregate.IEhaAggregateQueryCoordinator;
import jpl.gds.db.api.sql.fetch.aggregate.IRecordBatchHandler;
import jpl.gds.db.api.sql.fetch.aggregate.PacketInfo;
import jpl.gds.shared.holders.ApidHolder;
import jpl.gds.shared.holders.ApidNameHolder;
import jpl.gds.shared.holders.HolderException;
import jpl.gds.shared.holders.SessionFragmentHolder;
import jpl.gds.shared.holders.SpscHolder;
import jpl.gds.shared.holders.VcfcHolder;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.time.DbTimeUtility;

/**
 * This class processes a ResultSet stream of EHA Aggregates.
 * Creates record batches based on the configured batch size.
 *
 */
public class AggregateQueryStreamProcessor extends QueryStreamProcessor<IEhaAggregateDbRecord> {
	
	private final int batchSize;
	private ResultSet resultSet;
	private final boolean isIncludePacketInfo;
	private final IAggregateFetchConfig config;

	/**
	 * Constructor. 
	 * 
	 * @param aggregateQueryQoordinator the aggregate query coordinator
	 * @param config the fetch configuration
	 * @param trace the tracer
	 * @param resultSet the ResultSet used to retrieve database records
	 * @param batchHandler the 
	 */
	public AggregateQueryStreamProcessor(
	        final IEhaAggregateQueryCoordinator aggregateQueryQoordinator,
			final IAggregateFetchConfig config,
			final Tracer trace,
			final ResultSet resultSet,
			final IRecordBatchHandler<IEhaAggregateDbRecord> batchHandler) {
		super(aggregateQueryQoordinator, batchHandler, trace);
		this.resultSet = resultSet;
		this.batchSize = config.getChunkSize();
		this.isIncludePacketInfo = config.isIncludePacketInfo();
		this.config = config;
	}

	@Override
	public List<IEhaAggregateDbRecord> batchRecords() throws DatabaseException {
	    
        if (resultSet == null) {
            // we have nothing to process 
        	running.set(false);
        	return Collections.emptyList();
        }
        
        int count = 0;
        AggregateRecord aggregateRecord;
        final List<IEhaAggregateDbRecord> recordBatch = new ArrayList<>();
        
        try {
            final List<SQLWarning> warnings = new ArrayList<SQLWarning>();

            while(count < batchSize) {

                if (! resultSet.next()) {
                    break;
                }
                aggregateRecord = new AggregateRecord();
                
                final Long testSessionId = resultSet.getLong("sessionId");
                aggregateRecord.setSessionId(testSessionId);
                
                if (isIncludePacketInfo) {
                	PacketInfo packetInfoObj;
                	final Map<Long, PacketInfo> packetInfoMap = new HashMap<>();
                	// Parse and setup packetInfoMap
    				final String packetInfo = resultSet.getString("packetInfo");
    				final String packetInfoList[] = packetInfo.split("\\|");
    				 
    				for (int i=0; i<packetInfoList.length; i++) {
    					final String sesIdPacketInfo[] = packetInfoList[i].split(":");
    					final Long sessionId = Long.parseLong(sesIdPacketInfo[0]);
    					if (sessionId != aggregateRecord.getSessionId()) {
    						// Report an error
    						trace.error(AggregateFetchMarkers.QUERY_STREAM_PROCESSOR, "PacketInfo sessionId: " + sessionId + " does not match record sessionId: " + aggregateRecord.getSessionId());
    					}
    					final String packetInfoDetailList[] = sesIdPacketInfo[1].split(",");
    					packetInfoObj = new PacketInfo();
    					packetInfoObj.setApid(ApidHolder.valueOfString(packetInfoDetailList[1]));
    					packetInfoObj.setApidName(ApidNameHolder.valueOf(packetInfoDetailList[2]));
    					packetInfoObj.setRct(
    							DbTimeUtility.dateFromCoarseFine(
    									Long.parseLong(packetInfoDetailList[3]), 
    									Integer.parseInt(packetInfoDetailList[4])));
    					if (packetInfoDetailList[5].equalsIgnoreCase("NULL")) {
    						packetInfoObj.setVcfc(VcfcHolder.UNSUPPORTED);
    					} else {
    						packetInfoObj.setVcfc(VcfcHolder.valueOfString(packetInfoDetailList[5]));
    					}
    					packetInfoObj.setSpsc(SpscHolder.valueOfString(packetInfoDetailList[6]));
    					packetInfoMap.put(Long.parseLong(packetInfoDetailList[0]), packetInfoObj);
    				}
                	
                	aggregateRecord.setPacketInfoMap(packetInfoMap);
                }
                
                warnings.clear();
                aggregateRecord.setSessionFragmentHolder(SessionFragmentHolder.getFromDbRethrow(resultSet, "sessionFragment", warnings));

                final int hostId = resultSet.getInt("hostId");
                aggregateRecord.setHostId(hostId);
                
                
                aggregateRecord.setHost(config.getSessionPreFetch().lookupHost(hostId));
                aggregateRecord.setSpacecraftId(config.getSessionPreFetch().lookupSCID(hostId, testSessionId));
                
                // The ResultSet javadoc states "The column value; if the value is SQL NULL, 
                // the value returned is 0"
                // When its SQL NULL we want to set it to JAVA NULL as well instead of 0
                Integer vcid = resultSet.getInt("vcid");
                if (resultSet.wasNull() || (vcid < 0)) {
                    vcid = null;
                }
                aggregateRecord.setVcid(vcid);
                aggregateRecord.setDssId(resultSet.getInt("dssId"));
                
                aggregateRecord.setChannelType(resultSet.getString("channelType"));
                /*
                aggregateRecord.setRctCoarse(resultSet.getLong("rctCoarse"));
                aggregateRecord.setRctFine(resultSet.getInt("rctFine"));
                */      
                aggregateRecord.setContents(resultSet.getBytes("contents"));
                aggregateRecord.setChannelIdsString(resultSet.getString("chanIdsString"));

                recordBatch.add(aggregateRecord);
                
                count++;
            }


            if (resultSet.isAfterLast()) {
            	trace.info("ResultSet is COMPLETE");
                resultSet.close();
                resultSet        = null;
                running.set(false);
            }
        }
        catch (final SQLException se) {
            throw new DatabaseException("Error retrieving channel values " +
                                            "from database: "              +
                                            se.getMessage(),
                                        se);
        }
        catch (final HolderException he)
        {
            throw new DatabaseException("Error retrieving columns " +
                                            "from database: "       +
                                            he.getMessage(),
                                        he);
        }

        return recordBatch;
	}
}
