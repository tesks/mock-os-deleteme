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
package jpl.gds.db.mysql.impl.sql.fetch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import jpl.gds.db.api.sql.fetch.FetchConfiguration;
import jpl.gds.db.api.sql.fetch.FetchIdentifier;
import jpl.gds.db.api.sql.fetch.IChannelDataPreFetch;
import jpl.gds.db.api.sql.fetch.IChannelSummaryFetch;
import jpl.gds.db.api.sql.fetch.IChannelValueFetch;
import jpl.gds.db.api.sql.fetch.ICommandFetch;
import jpl.gds.db.api.sql.fetch.IContextConfigFetch;
import jpl.gds.db.api.sql.fetch.IDbContexConfigPreFetch;
import jpl.gds.db.api.sql.fetch.IDbProductFetch;
import jpl.gds.db.api.sql.fetch.IDbSessionPreFetch;
import jpl.gds.db.api.sql.fetch.IEndSessionFetch;
import jpl.gds.db.api.sql.fetch.IEvrFetch;
import jpl.gds.db.api.sql.fetch.IFetchConfiguration;
import jpl.gds.db.api.sql.fetch.IFetchConfigurationMap;
import jpl.gds.db.api.sql.fetch.IFrameFetch;
import jpl.gds.db.api.sql.fetch.IHostFetch;
import jpl.gds.db.api.sql.fetch.ILogFetch;
import jpl.gds.db.api.sql.fetch.IPacketFetch;
import jpl.gds.db.api.sql.fetch.ISessionFetch;
import jpl.gds.db.api.sql.fetch.PreFetchType;
import jpl.gds.db.api.sql.fetch.aggregate.IChannelAggregateFetch;
import jpl.gds.db.api.sql.fetch.cfdp.ICfdpFileGenerationFetch;
import jpl.gds.db.api.sql.fetch.cfdp.ICfdpFileUplinkFinishedFetch;
import jpl.gds.db.api.sql.fetch.cfdp.ICfdpIndicationFetch;
import jpl.gds.db.api.sql.fetch.cfdp.ICfdpPduReceivedFetch;
import jpl.gds.db.api.sql.fetch.cfdp.ICfdpPduSentFetch;
import jpl.gds.db.api.sql.fetch.cfdp.ICfdpRequestReceivedFetch;
import jpl.gds.db.api.sql.fetch.cfdp.ICfdpRequestResultFetch;
import jpl.gds.db.mysql.impl.sql.fetch.aggregate.ChannelAggregateFetch;
import jpl.gds.db.mysql.impl.sql.fetch.cfdp.CfdpFileGenerationFetch;
import jpl.gds.db.mysql.impl.sql.fetch.cfdp.CfdpFileUplinkFinishedFetch;
import jpl.gds.db.mysql.impl.sql.fetch.cfdp.CfdpIndicationFetch;
import jpl.gds.db.mysql.impl.sql.fetch.cfdp.CfdpPduReceivedFetch;
import jpl.gds.db.mysql.impl.sql.fetch.cfdp.CfdpPduSentFetch;
import jpl.gds.db.mysql.impl.sql.fetch.cfdp.CfdpRequestReceivedFetch;
import jpl.gds.db.mysql.impl.sql.fetch.cfdp.CfdpRequestResultFetch;
import jpl.gds.shared.log.TraceManager;


@SuppressWarnings("serial")
public class MultimissionFetchConfigurationMap extends HashMap<FetchIdentifier, IFetchConfiguration>
        implements IFetchConfigurationMap {

    /**
     * 
     */
    // @formatter:off
    public MultimissionFetchConfigurationMap() {
        registerConfiguration(new FetchConfiguration(FetchIdentifier.NONE));

        registerConfiguration(new FetchConfiguration(FetchIdentifier.CHANNEL_DATA_PRE_FETCH, ChannelDataPreFetch.class,
                                                     IChannelDataPreFetch.class, new ArrayList<List<Class<?>>>() {{
            add(new ArrayList<Class<?>>(Arrays.asList(new Class<?>[]{Boolean.class, IDbSessionPreFetch.class, String[].class, String[].class})));
        }}));
        
        registerConfiguration(new FetchConfiguration(FetchIdentifier.CHANNEL_SUMMARY_FETCH, ChannelSummaryFetch.class,
                                                     IChannelSummaryFetch.class, new ArrayList<List<Class<?>>>() {{
            add(new ArrayList<Class<?>>(Arrays.asList(new Class<?>[]{Boolean.class})));
            add(new ArrayList<Class<?>>(Arrays.asList(new Class<?>[]{})));
        }}));
        
        registerConfiguration(new FetchConfiguration(FetchIdentifier.CHANNEL_VALUE_FETCH, ChannelValueFetch.class,
                                                     IChannelValueFetch.class, new ArrayList<List<Class<?>>>() {{
            add(new ArrayList<Class<?>>(Arrays.asList(new Class<?>[]{Boolean.class, String[].class, String[].class, Boolean.class, Boolean.class, Boolean.class})));
            add(new ArrayList<Class<?>>(Arrays.asList(new Class<?>[]{Boolean.class, String[].class, String[].class, Boolean.class, Boolean.class})));
            add(new ArrayList<Class<?>>(Arrays.asList(new Class<?>[]{String[].class})));
            add(new ArrayList<Class<?>>(Arrays.asList(new Class<?>[]{})));
        }}));
        
		registerConfiguration(new FetchConfiguration(FetchIdentifier.CHANNEL_AGGREGATE_FETCH, ChannelAggregateFetch.class, 
													 IChannelAggregateFetch.class, new ArrayList<List<Class<?>>>() {{
			add(new ArrayList<Class<?>>(Arrays.asList(new Class<?>[] {Boolean.class, String[].class, String[].class, Boolean.class, Boolean.class, Boolean.class})));
			add(new ArrayList<Class<?>>(Arrays.asList(new Class<?>[] {Boolean.class, String[].class, String[].class, Boolean.class, Boolean.class })));
			add(new ArrayList<Class<?>>(Arrays.asList(new Class<?>[] {String[].class})));
			add(new ArrayList<Class<?>>(Arrays.asList(new Class<?>[] {})));
		}}));       
        
        registerConfiguration(new FetchConfiguration(FetchIdentifier.COMMAND_FETCH, CommandFetch.class,
                                                     ICommandFetch.class, new ArrayList<List<Class<?>>>() {{
            add(new ArrayList<Class<?>>(Arrays.asList(new Class<?>[]{Boolean.class})));
            add(new ArrayList<Class<?>>(Arrays.asList(new Class<?>[]{})));
        }}));
        
        registerConfiguration(new FetchConfiguration(FetchIdentifier.END_SESSION_FETCH, EndSessionFetch.class,
                                                     IEndSessionFetch.class, new ArrayList<List<Class<?>>>() {{
            add(new ArrayList<Class<?>>(Arrays.asList(new Class<?>[]{Boolean.class})));
            add(new ArrayList<Class<?>>(Arrays.asList(new Class<?>[]{})));
        }}));
        
        registerConfiguration(new FetchConfiguration(FetchIdentifier.EVR_FETCH, EvrFetch.class, IEvrFetch.class,
                                                     new ArrayList<List<Class<?>>>() {{
            add(new ArrayList<Class<?>>(Arrays.asList(new Class<?>[]{Boolean.class})));
            add(new ArrayList<Class<?>>(Arrays.asList(new Class<?>[]{})));
        }}));
        
        registerConfiguration(new FetchConfiguration(FetchIdentifier.FRAME_FETCH, FrameFetch.class, IFrameFetch.class,
                                                     new ArrayList<List<Class<?>>>() {{
            add(new ArrayList<Class<?>>(Arrays.asList(new Class<?>[]{Boolean.class})));
            add(new ArrayList<Class<?>>(Arrays.asList(new Class<?>[]{})));
        }}));
        
        registerConfiguration(new FetchConfiguration(FetchIdentifier.HOST_FETCH, HostFetch.class, IHostFetch.class,
                                                     new ArrayList<List<Class<?>>>() {{
            add(new ArrayList<Class<?>>(Arrays.asList(new Class<?>[]{})));
        }}));
        
        registerConfiguration(new FetchConfiguration(FetchIdentifier.LOG_FETCH, LogFetch.class, ILogFetch.class,
                                                     new ArrayList<List<Class<?>>>() {{
            add(new ArrayList<Class<?>>(Arrays.asList(new Class<?>[]{Boolean.class})));
            add(new ArrayList<Class<?>>(Arrays.asList(new Class<?>[]{})));
        }}));
        
        registerConfiguration(new FetchConfiguration(FetchIdentifier.PACKET_FETCH, PacketFetch.class,
                                                     IPacketFetch.class, new ArrayList<List<Class<?>>>() {{
            add(new ArrayList<Class<?>>(Arrays.asList(new Class<?>[]{Boolean.class})));
            add(new ArrayList<Class<?>>(Arrays.asList(new Class<?>[]{})));
        }}));
        
        registerConfiguration(new FetchConfiguration(FetchIdentifier.PRODUCT_FETCH, ReferenceProductFetch.class,
                                                     IDbProductFetch.class, new ArrayList<List<Class<?>>>() {{
            add(new ArrayList<Class<?>>(Arrays.asList(new Class<?>[]{Boolean.class})));
            add(new ArrayList<Class<?>>(Arrays.asList(new Class<?>[]{})));
        }}));
        
        registerConfiguration(new FetchConfiguration(FetchIdentifier.SESSION_FETCH, SessionFetch.class,
                                                     ISessionFetch.class, new ArrayList<List<Class<?>>>() {{
            add(new ArrayList<Class<?>>(Arrays.asList(new Class<?>[]{Boolean.class})));
            add(new ArrayList<Class<?>>(Arrays.asList(new Class<?>[]{})));
        }}));

        registerConfiguration(new FetchConfiguration(FetchIdentifier.CONTEXT_CONFIG_FETCH, ContextConfigFetch.class,
                                                     IContextConfigFetch.class, new ArrayList<List<Class<?>>>() {{
            add(new ArrayList<Class<?>>(Arrays.asList(new Class<?>[]{Boolean.class})));
            add(new ArrayList<Class<?>>(Arrays.asList(new Class<?>[]{})));
        }}));

        registerConfiguration(new FetchConfiguration(FetchIdentifier.CONTEXT_CONFIG_PRE_FETCH,
                                                     ContextConfigPreFetch.class,
                                                     IDbContexConfigPreFetch.class, new ArrayList<List<Class<?>>>() {{
            add(new ArrayList<Class<?>>(Arrays.asList(new Class<?>[]{Boolean.class})));
            add(new ArrayList<Class<?>>(Arrays.asList(new Class<?>[]{})));
        }}));
        
        registerConfiguration(new FetchConfiguration(FetchIdentifier.SESSION_PRE_FETCH, SessionPreFetch.class,
                                                     IDbSessionPreFetch.class, new ArrayList<List<Class<?>>>() {{
            add(new ArrayList<Class<?>>(Arrays.asList(new Class<?>[]{Boolean.class, PreFetchType.class})));
            add(new ArrayList<Class<?>>(Arrays.asList(new Class<?>[]{Boolean.class})));
            add(new ArrayList<Class<?>>(Arrays.asList(new Class<?>[]{})));
        }}));

        
        registerConfiguration(new FetchConfiguration(FetchIdentifier.CFDP_INDICATION_FETCH, CfdpIndicationFetch.class,
                ICfdpIndicationFetch.class, new ArrayList<List<Class<?>>>() {{
                	add(new ArrayList<Class<?>>(Arrays.asList(new Class<?>[]{Boolean.class})));
                	add(new ArrayList<Class<?>>(Arrays.asList(new Class<?>[]{})));
        }}));

        registerConfiguration(new FetchConfiguration(FetchIdentifier.CFDP_FILE_GENERATION_FETCH, CfdpFileGenerationFetch.class,
                ICfdpFileGenerationFetch.class, new ArrayList<List<Class<?>>>() {{
                	add(new ArrayList<Class<?>>(Arrays.asList(new Class<?>[]{Boolean.class})));
                	add(new ArrayList<Class<?>>(Arrays.asList(new Class<?>[]{})));
        }}));

        registerConfiguration(new FetchConfiguration(FetchIdentifier.CFDP_FILE_UPLINK_FINISHED_FETCH, CfdpFileUplinkFinishedFetch.class,
                ICfdpFileUplinkFinishedFetch.class, new ArrayList<List<Class<?>>>() {{
                	add(new ArrayList<Class<?>>(Arrays.asList(new Class<?>[]{Boolean.class})));
                	add(new ArrayList<Class<?>>(Arrays.asList(new Class<?>[]{})));
        }}));

        registerConfiguration(new FetchConfiguration(FetchIdentifier.CFDP_REQUEST_RECEIVED_FETCH, CfdpRequestReceivedFetch.class,
                ICfdpRequestReceivedFetch.class, new ArrayList<List<Class<?>>>() {{
                	add(new ArrayList<Class<?>>(Arrays.asList(new Class<?>[]{Boolean.class})));
                	add(new ArrayList<Class<?>>(Arrays.asList(new Class<?>[]{})));
        }}));

        registerConfiguration(new FetchConfiguration(FetchIdentifier.CFDP_REQUEST_RESULT_FETCH, CfdpRequestResultFetch.class,
                ICfdpRequestResultFetch.class, new ArrayList<List<Class<?>>>() {{
                	add(new ArrayList<Class<?>>(Arrays.asList(new Class<?>[]{Boolean.class})));
                	add(new ArrayList<Class<?>>(Arrays.asList(new Class<?>[]{})));
        }}));

        registerConfiguration(new FetchConfiguration(FetchIdentifier.CFDP_PDU_RECEIVED_FETCH, CfdpPduReceivedFetch.class,
                ICfdpPduReceivedFetch.class, new ArrayList<List<Class<?>>>() {{
                	add(new ArrayList<Class<?>>(Arrays.asList(new Class<?>[]{Boolean.class})));
                	add(new ArrayList<Class<?>>(Arrays.asList(new Class<?>[]{})));
        }}));

        registerConfiguration(new FetchConfiguration(FetchIdentifier.CFDP_PDU_SENT_FETCH, CfdpPduSentFetch.class,
                ICfdpPduSentFetch.class, new ArrayList<List<Class<?>>>() {{
                	add(new ArrayList<Class<?>>(Arrays.asList(new Class<?>[]{Boolean.class})));
                	add(new ArrayList<Class<?>>(Arrays.asList(new Class<?>[]{})));
        }}));

    }
    // @formatter:on

	/* (non-Javadoc)
	 * @see jpl.gds.db.api.sql.store.IStoreConfigurationMap#registerConfiguration(jpl.gds.db.api.sql.store.IStoreConfiguration)
	 */
	@Override
	public void registerConfiguration(final IFetchConfiguration fetch) {
	    if (this.containsKey(fetch.getFetchIdentifier())) {
            TraceManager.getDefaultTracer().debug("Overridding existing FETCH definition for Fetch Identifier "
                    + fetch.getFetchIdentifier());

	    }
	    put(fetch.getFetchIdentifier(), fetch);
	}
}
