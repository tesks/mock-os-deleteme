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

import java.lang.reflect.Constructor;

import jpl.gds.db.api.sql.fetch.IContextConfigFetch;
import org.apache.commons.lang.ArrayUtils;
import org.springframework.context.ApplicationContext;

import jpl.gds.db.api.sql.fetch.FetchIdentifier;
import jpl.gds.db.api.sql.fetch.IChannelDataPreFetch;
import jpl.gds.db.api.sql.fetch.IChannelSummaryFetch;
import jpl.gds.db.api.sql.fetch.IChannelValueFetch;
import jpl.gds.db.api.sql.fetch.ICommandFetch;
import jpl.gds.db.api.sql.fetch.IDbContexConfigPreFetch;
import jpl.gds.db.api.sql.fetch.IDbProductFetch;
import jpl.gds.db.api.sql.fetch.IDbSessionPreFetch;
import jpl.gds.db.api.sql.fetch.IDbSqlFetch;
import jpl.gds.db.api.sql.fetch.IDbSqlFetchFactory;
import jpl.gds.db.api.sql.fetch.IEndSessionFetch;
import jpl.gds.db.api.sql.fetch.IEvrFetch;
import jpl.gds.db.api.sql.fetch.IFetchConfiguration;
import jpl.gds.db.api.sql.fetch.IFetchConfigurationMap;
import jpl.gds.db.api.sql.fetch.IFrameFetch;
import jpl.gds.db.api.sql.fetch.IHostFetch;
import jpl.gds.db.api.sql.fetch.ILogFetch;
import jpl.gds.db.api.sql.fetch.IPacketFetch;
import jpl.gds.db.api.sql.fetch.ISessionFetch;
import jpl.gds.db.api.sql.fetch.aggregate.IChannelAggregateFetch;
import jpl.gds.db.api.sql.fetch.cfdp.ICfdpFileGenerationFetch;
import jpl.gds.db.api.sql.fetch.cfdp.ICfdpFileUplinkFinishedFetch;
import jpl.gds.db.api.sql.fetch.cfdp.ICfdpIndicationFetch;
import jpl.gds.db.api.sql.fetch.cfdp.ICfdpPduReceivedFetch;
import jpl.gds.db.api.sql.fetch.cfdp.ICfdpPduSentFetch;
import jpl.gds.db.api.sql.fetch.cfdp.ICfdpRequestReceivedFetch;
import jpl.gds.db.api.sql.fetch.cfdp.ICfdpRequestResultFetch;
import jpl.gds.shared.reflect.ReflectionException;
import jpl.gds.shared.reflect.ReflectionToolkit;

public class MultimissionFetchFactory implements IDbSqlFetchFactory {
	private final ApplicationContext appContext;
	private final IFetchConfigurationMap fetchConfigMap;
	
    /**
     * @param appContext
     *            the Spring Application Context
     */
	public MultimissionFetchFactory(final ApplicationContext appContext) {
		super();
		this.appContext = appContext;
		this.fetchConfigMap = appContext.getBean(IFetchConfigurationMap.class);
	}

    @Override
	public ISessionFetch getSessionFetch(final Object...args) {
    	return (ISessionFetch) createDbSqlFetch(FetchIdentifier.SESSION_FETCH, args);
	}

	@Override
	public IContextConfigFetch getContextConfigFetch(final Object... args) {
		return (IContextConfigFetch) createDbSqlFetch(FetchIdentifier.CONTEXT_CONFIG_FETCH, args);
	}

	@Override
	public IDbContexConfigPreFetch getContextConfigPreFetch(final Object... args) {
		return (IDbContexConfigPreFetch) createDbSqlFetch(FetchIdentifier.CONTEXT_CONFIG_PRE_FETCH, args);
	}

	@Override
	public IDbSessionPreFetch getSessionPreFetch(final Object...args) {
    	return (IDbSessionPreFetch) createDbSqlFetch(FetchIdentifier.SESSION_PRE_FETCH, args);
	}

    @Override
	public IEndSessionFetch getEndSessionFetch(final Object...args) {
    	return (IEndSessionFetch) createDbSqlFetch(FetchIdentifier.END_SESSION_FETCH, args);
	}

    @Override
	public IHostFetch getHostFetch(final Object...args) {
    	return (IHostFetch) createDbSqlFetch(FetchIdentifier.HOST_FETCH, args);
	}

    @Override
	public IChannelDataPreFetch getChannelDataPreFetch(final Object...args) {
    	return (IChannelDataPreFetch) createDbSqlFetch(FetchIdentifier.CHANNEL_DATA_PRE_FETCH, args);
	}

    @Override
	public IChannelSummaryFetch getChannelSummaryFetch(final Object...args) {
    	return (IChannelSummaryFetch) createDbSqlFetch(FetchIdentifier.CHANNEL_SUMMARY_FETCH, args);
	}

    @Override
	public IChannelValueFetch getChannelValueFetch(final Object...args) {
    	return (IChannelValueFetch) createDbSqlFetch(FetchIdentifier.CHANNEL_VALUE_FETCH, args);
	}

    @Override
	public ICommandFetch getCommandFetch(final Object...args) {
    	return (ICommandFetch) createDbSqlFetch(FetchIdentifier.COMMAND_FETCH, args);
	}

    @Override
	public IEvrFetch getEvrFetch(final Object...args) {
    	return (IEvrFetch) createDbSqlFetch(FetchIdentifier.EVR_FETCH, args);
	}

    @Override
	public IFrameFetch getFrameFetch(final Object...args) {
    	return (IFrameFetch) createDbSqlFetch(FetchIdentifier.FRAME_FETCH, args);
	}

    @Override
	public ILogFetch getLogFetch(final Object...args) {
    	return (ILogFetch) createDbSqlFetch(FetchIdentifier.LOG_FETCH, args);
	}

    @Override
	public IPacketFetch getPacketFetch(final Object...args) {
    	return (IPacketFetch) createDbSqlFetch(FetchIdentifier.PACKET_FETCH, args);
	}

    @Override
	public IDbProductFetch getProductFetch(final Object...args) {
    	return (IDbProductFetch) createDbSqlFetch(FetchIdentifier.PRODUCT_FETCH, args);
	}


    @Override
    public ICfdpIndicationFetch getCfdpIndicationFetch(Object... args) {
    	return (ICfdpIndicationFetch) createDbSqlFetch(FetchIdentifier.CFDP_INDICATION_FETCH, args);
	}

    @Override
    public ICfdpFileGenerationFetch getCfdpFileGenerationFetch(Object... args) {
    	return (ICfdpFileGenerationFetch) createDbSqlFetch(FetchIdentifier.CFDP_FILE_GENERATION_FETCH, args);
	}

    @Override
    public ICfdpFileUplinkFinishedFetch getCfdpFileUplinkFinishedFetch(Object... args) {
    	return (ICfdpFileUplinkFinishedFetch) createDbSqlFetch(FetchIdentifier.CFDP_FILE_UPLINK_FINISHED_FETCH, args);
	}

    @Override
    public ICfdpRequestReceivedFetch getCfdpRequestReceivedFetch(Object... args) {
    	return (ICfdpRequestReceivedFetch) createDbSqlFetch(FetchIdentifier.CFDP_REQUEST_RECEIVED_FETCH, args);
	}

    @Override
    public ICfdpRequestResultFetch getCfdpRequestResultFetch(Object... args) {
    	return (ICfdpRequestResultFetch) createDbSqlFetch(FetchIdentifier.CFDP_REQUEST_RESULT_FETCH, args);
	}

    @Override
    public ICfdpPduReceivedFetch getCfdpPduReceivedFetch(Object... args) {
    	return (ICfdpPduReceivedFetch) createDbSqlFetch(FetchIdentifier.CFDP_PDU_RECEIVED_FETCH, args);
	}

    @Override
    public ICfdpPduSentFetch getCfdpPduSentFetch(Object... args) {
    	return (ICfdpPduSentFetch) createDbSqlFetch(FetchIdentifier.CFDP_PDU_SENT_FETCH, args);
	}

	@Override
	public IChannelAggregateFetch getChannelAggregateFetch(Object... args) {
		return (IChannelAggregateFetch) createDbSqlFetch(FetchIdentifier.CHANNEL_AGGREGATE_FETCH, args);
	}
    
	/**
     * @param fetchId
     *            the ID of fetch object being requested
     * @param args
     *            the specific arguments required for the requested fetch object
     * @return the SQL Fetch Object corresponding to the fetchId for the given adaptation
     */
	@SuppressWarnings("unchecked")
    protected <T extends IDbSqlFetch> T createDbSqlFetch(final FetchIdentifier fetchId, final Object... args) {
	    final IFetchConfiguration fetchConfig = fetchConfigMap.get(fetchId);
        Constructor<?> ctor = fetchConfig.getConstructor(args);
        try {
		    if (null == ctor) {
                final Class<?> klass = fetchConfig.getFetchClass();
    			ctor = ReflectionToolkit.getConstructor(klass, (Class<?>[]) ArrayUtils.addAll(new Class<?>[]{appContext.getClass()}, fetchConfig.getArgList(args)));
    			fetchConfig.setConstructor(args, ctor);
		    }
			return (T) fetchConfig.getReturnType().cast(ReflectionToolkit.createObject(ctor, ArrayUtils.addAll(new Object[]{appContext}, args)));
        }
        catch (ReflectionException | IllegalArgumentException e) {
            e.printStackTrace();
            throw new RuntimeException("Unable to create Database Fetch: " + e.toString(), e);
        }
	}
}
