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
package jpl.gds.db.mysql.impl.sql.store;

import java.util.HashMap;

import jpl.gds.db.api.sql.IDbTableNames;
import jpl.gds.db.api.sql.store.IStoreConfiguration;
import jpl.gds.db.api.sql.store.IStoreConfigurationMap;
import jpl.gds.db.api.sql.store.StoreConfiguration;
import jpl.gds.db.api.sql.store.StoreIdentifier;
import jpl.gds.db.api.sql.store.ICommandUpdateStore;
import jpl.gds.db.api.sql.store.ldi.IChannelValueLDIStore;
import jpl.gds.db.api.sql.store.ldi.ICommandMessageLDIStore;
import jpl.gds.db.api.sql.store.ldi.IEvrLDIStore;
import jpl.gds.db.api.sql.store.ldi.IFrameLDIStore;
import jpl.gds.db.api.sql.store.ldi.IHeaderChannelValueLDIStore;
import jpl.gds.db.api.sql.store.ldi.ILogMessageLDIStore;
import jpl.gds.db.api.sql.store.ldi.IMonitorChannelValueLDIStore;
import jpl.gds.db.api.sql.store.ldi.IPacketLDIStore;
import jpl.gds.db.api.sql.store.ldi.IProductLDIStore;
import jpl.gds.db.api.sql.store.ldi.ISseChannelValueLDIStore;
import jpl.gds.db.api.sql.store.ldi.ISseEvrLDIStore;
import jpl.gds.db.api.sql.store.ldi.ISsePacketLDIStore;
import jpl.gds.db.api.sql.store.ldi.aggregate.IChannelAggregateLDIStore;
import jpl.gds.db.api.sql.store.ldi.aggregate.IHeaderChannelAggregateLDIStore;
import jpl.gds.db.api.sql.store.ldi.aggregate.IMonitorChannelAggregateLDIStore;
import jpl.gds.db.api.sql.store.ldi.aggregate.ISseChannelAggregateLDIStore;
import jpl.gds.db.api.sql.store.ldi.cfdp.ICfdpFileGenerationLDIStore;
import jpl.gds.db.api.sql.store.ldi.cfdp.ICfdpFileUplinkFinishedLDIStore;
import jpl.gds.db.api.sql.store.ldi.cfdp.ICfdpIndicationLDIStore;
import jpl.gds.db.api.sql.store.ldi.cfdp.ICfdpPduReceivedLDIStore;
import jpl.gds.db.api.sql.store.ldi.cfdp.ICfdpPduSentLDIStore;
import jpl.gds.db.api.sql.store.ldi.cfdp.ICfdpRequestReceivedLDIStore;
import jpl.gds.db.api.sql.store.ldi.cfdp.ICfdpRequestResultLDIStore;
import jpl.gds.shared.log.TraceManager;


@SuppressWarnings("serial")
public class MultimissionStoreConfigurationMap extends HashMap<StoreIdentifier, IStoreConfiguration> implements
IStoreConfigurationMap {
    /**
     * 
     */
    public MultimissionStoreConfigurationMap() {
        registerConfiguration(new StoreConfiguration(StoreIdentifier.None));
       
        registerConfiguration(new StoreConfiguration(StoreIdentifier.EndSession));
        
        registerConfiguration(new StoreConfiguration(StoreIdentifier.Host));
        
        registerConfiguration(new StoreConfiguration(StoreIdentifier.Frame,
                IDbTableNames.DB_FRAME_BODY_TABLE_NAME,
                IFrameLDIStore.DB_FRAME_BODY_FIELDS,
                IDbTableNames.DB_FRAME_DATA_TABLE_NAME,
                IFrameLDIStore.DB_FRAME_DATA_FIELDS,
                IFrameLDIStore.DB_FRAME_SET_CLAUSE, false));
        
        registerConfiguration(new StoreConfiguration(StoreIdentifier.Packet,
                IDbTableNames.DB_PACKET_BODY_TABLE_NAME,
                IPacketLDIStore.DB_PACKET_BODY_FIELDS,
                IDbTableNames.DB_PACKET_DATA_TABLE_NAME,
                IPacketLDIStore.DB_PACKET_DATA_FIELDS,
                IPacketLDIStore.DB_PACKET_SET_CLAUSE, false));
        
        registerConfiguration(new StoreConfiguration(StoreIdentifier.SsePacket,
                IDbTableNames.DB_SSE_PACKET_BODY_TABLE_NAME,
                ISsePacketLDIStore.DB_SSE_PACKET_BODY_FIELDS,
                IDbTableNames.DB_SSE_PACKET_DATA_TABLE_NAME,
                ISsePacketLDIStore.DB_SSE_PACKET_DATA_FIELDS,
                ISsePacketLDIStore.DB_SSE_PACKET_SET_CLAUSE, true));
        
        registerConfiguration(new StoreConfiguration(
                StoreIdentifier.ChannelValue,
                IDbTableNames.DB_CHANNEL_VALUE_TABLE_NAME,
                IChannelValueLDIStore.DB_CHANNEL_VALUE_FIELDS,
                IDbTableNames.DB_CHANNEL_DATA_TABLE_NAME,
                IChannelValueLDIStore.DB_CHANNEL_DATA_FIELDS,
                IChannelValueLDIStore.DB_CHANNEL_SET_CLAUSE, false));
        
        registerConfiguration(new StoreConfiguration(
                StoreIdentifier.HeaderChannelValue,
                IDbTableNames.DB_HEADER_CHANNEL_VALUE_TABLE_NAME,
                IHeaderChannelValueLDIStore.DB_HEADER_CHANNEL_VALUE_FIELDS,
                IDbTableNames.DB_CHANNEL_DATA_TABLE_NAME,
                IChannelValueLDIStore.DB_CHANNEL_DATA_FIELDS,
                IChannelValueLDIStore.DB_CHANNEL_SET_CLAUSE, false));
        
        registerConfiguration(new StoreConfiguration(
                StoreIdentifier.MonitorChannelValue,
                IDbTableNames.DB_MONITOR_CHANNEL_VALUE_TABLE_NAME,
                IMonitorChannelValueLDIStore.DB_MONITOR_CHANNEL_VALUE_FIELDS,
                IDbTableNames.DB_CHANNEL_DATA_TABLE_NAME,
                IChannelValueLDIStore.DB_CHANNEL_DATA_FIELDS,
                IChannelValueLDIStore.DB_CHANNEL_SET_CLAUSE, false));
        
        registerConfiguration(new StoreConfiguration(
                StoreIdentifier.SseChannelValue,
                IDbTableNames.DB_SSE_CHANNEL_VALUE_DATA_TABLE_NAME,
                ISseChannelValueLDIStore.DB_SSE_CHANNEL_VALUE_FIELDS,
                IDbTableNames.DB_CHANNEL_DATA_TABLE_NAME,
                IChannelValueLDIStore.DB_CHANNEL_DATA_FIELDS,
                IChannelValueLDIStore.DB_CHANNEL_SET_CLAUSE, true));

        registerConfiguration(new StoreConfiguration(
                StoreIdentifier.ChannelAggregate,
                IDbTableNames.DB_CHANNEL_AGGREGATE_TABLE_NAME,
                IChannelAggregateLDIStore.DB_CHANNEL_AGGREGATE_FIELDS,
                IDbTableNames.DB_CHANNEL_DATA_TABLE_NAME,
                IChannelValueLDIStore.DB_CHANNEL_DATA_FIELDS,
                IChannelAggregateLDIStore.DB_CHANNEL_SET_CLAUSE, false));
        
        registerConfiguration(new StoreConfiguration(
                StoreIdentifier.HeaderChannelAggregate,
                IDbTableNames.DB_HEADER_CHANNEL_AGGREGATE_TABLE_NAME,
                IHeaderChannelAggregateLDIStore.DB_HEADER_CHANNEL_AGGREGATE_FIELDS,
                IDbTableNames.DB_CHANNEL_DATA_TABLE_NAME,
                IChannelValueLDIStore.DB_CHANNEL_DATA_FIELDS,
                IChannelAggregateLDIStore.DB_CHANNEL_SET_CLAUSE, false));
        
        registerConfiguration(new StoreConfiguration(
                StoreIdentifier.MonitorChannelAggregate,
                IDbTableNames.DB_MONITOR_CHANNEL_AGGREGATE_TABLE_NAME,
                IMonitorChannelAggregateLDIStore.DB_MONITOR_CHANNEL_AGGREGATE_FIELDS,
                IDbTableNames.DB_CHANNEL_DATA_TABLE_NAME,
                IChannelValueLDIStore.DB_CHANNEL_DATA_FIELDS,
                IChannelAggregateLDIStore.DB_CHANNEL_SET_CLAUSE, false));
        
        registerConfiguration(new StoreConfiguration(
                StoreIdentifier.SseChannelAggregate,
                IDbTableNames.DB_SSE_CHANNEL_AGGREGATE_TABLE_NAME,
                ISseChannelAggregateLDIStore.DB_SSE_CHANNEL_AGGREGATE_FIELDS,
                IDbTableNames.DB_CHANNEL_DATA_TABLE_NAME,
                IChannelValueLDIStore.DB_CHANNEL_DATA_FIELDS,
                IChannelAggregateLDIStore.DB_CHANNEL_SET_CLAUSE, true));        
        
        registerConfiguration(new StoreConfiguration(StoreIdentifier.Evr,
                IDbTableNames.DB_EVR_DATA_TABLE_NAME,
                IEvrLDIStore.DB_EVR_DATA_FIELDS,
                IDbTableNames.DB_EVR_METADATA_TABLE_NAME,
                IEvrLDIStore.DB_EVR_METADATA_FIELDS, null, false));
        
        registerConfiguration(new StoreConfiguration(StoreIdentifier.SseEvr,
                IDbTableNames.DB_SSE_EVR_DATA_TABLE_NAME,
                ISseEvrLDIStore.DB_SSE_EVR_DATA_FIELDS,
                IDbTableNames.DB_SSE_EVR_METADATA_TABLE_NAME,
                ISseEvrLDIStore.DB_EVR_METADATA_FIELDS, null, true));
        
        registerConfiguration(new StoreConfiguration(
                StoreIdentifier.CommandMessage,
                IDbTableNames.DB_COMMAND_MESSAGE_DATA_TABLE_NAME,
                ICommandMessageLDIStore.DB_COMMAND_MESSAGE_FIELDS,
                IDbTableNames.DB_COMMAND_STATUS_TABLE_NAME,
                ICommandMessageLDIStore.DB_COMMAND_STATUS_FIELDS, null, false));
        
        // MPCS-9908 - register the CommandUpdateStore
        registerConfiguration(new StoreConfiguration(
                StoreIdentifier.CommandUpdate,
                IDbTableNames.DB_COMMAND_MESSAGE_DATA_TABLE_NAME,
                ICommandUpdateStore.DB_COMMAND_MESSAGE_FIELDS, null, null,
                null, false));

        registerConfiguration(new StoreConfiguration(
                StoreIdentifier.LogMessage,
                IDbTableNames.DB_LOG_MESSAGE_DATA_TABLE_NAME,
                ILogMessageLDIStore.DB_LOG_MESSAGE_DATA_FIELDS, null, null,
                null, false));
        
        registerConfiguration(new StoreConfiguration(StoreIdentifier.Product,
                IDbTableNames.DB_PRODUCT_DATA_TABLE_NAME,
                IProductLDIStore.DB_REFERENCE_PRODUCT_DATA_FIELDS, null, null,
                null, false));

        registerConfiguration(new StoreConfiguration(StoreIdentifier.CfdpIndication,
                IDbTableNames.DB_CFDP_INDICATION_DATA_TABLE_NAME,
                ICfdpIndicationLDIStore.DB_CFDP_INDICATION_FIELDS, null, null,
                null, false));

        registerConfiguration(new StoreConfiguration(StoreIdentifier.CfdpFileGeneration,
                IDbTableNames.DB_CFDP_FILE_GENERATION_DATA_TABLE_NAME,
                ICfdpFileGenerationLDIStore.DB_CFDP_FILE_GENERATION_FIELDS, null, null,
                null, false));

        registerConfiguration(new StoreConfiguration(StoreIdentifier.CfdpFileUplinkFinished,
                IDbTableNames.DB_CFDP_FILE_UPLINK_FINISHED_DATA_TABLE_NAME,
                ICfdpFileUplinkFinishedLDIStore.DB_CFDP_FILE_UPLINK_FINISHED_FIELDS, null, null,
                null, false));

        registerConfiguration(new StoreConfiguration(StoreIdentifier.CfdpRequestReceived,
                IDbTableNames.DB_CFDP_REQUEST_RECEIVED_DATA_TABLE_NAME,
                ICfdpRequestReceivedLDIStore.DB_CFDP_REQUEST_RECEIVED_FIELDS, null, null,
                null, false));

        registerConfiguration(new StoreConfiguration(StoreIdentifier.CfdpRequestResult,
                IDbTableNames.DB_CFDP_REQUEST_RESULT_DATA_TABLE_NAME,
                ICfdpRequestResultLDIStore.DB_CFDP_REQUEST_RESULT_FIELDS, null, null,
                null, false));

        registerConfiguration(new StoreConfiguration(StoreIdentifier.CfdpPduReceived,
                IDbTableNames.DB_CFDP_PDU_RECEIVED_DATA_TABLE_NAME,
                ICfdpPduReceivedLDIStore.DB_CFDP_PDU_RECEIVED_FIELDS, null, null,
                null, false));

        registerConfiguration(new StoreConfiguration(StoreIdentifier.CfdpPduSent,
                IDbTableNames.DB_CFDP_PDU_SENT_DATA_TABLE_NAME,
                ICfdpPduSentLDIStore.DB_CFDP_PDU_SENT_FIELDS, null, null,
                null, false));

    }

    /* (non-Javadoc)
     * @see jpl.gds.db.api.sql.store.IStoreConfigurationMap#registerConfiguration(jpl.gds.db.api.sql.store.IStoreConfiguration)
     */
    @Override
	public void registerConfiguration(final IStoreConfiguration store) {
        if (this.containsKey(store.getStoreIdentifier())) {
            TraceManager.getDefaultTracer().debug("Overriding existing STORE definition for Store Identifier " + store.getStoreIdentifier());

        }
        put(store.getStoreIdentifier(), store);

    }
}
