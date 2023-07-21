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
package jpl.gds.db.api.sql.fetch;

import jpl.gds.db.api.sql.fetch.aggregate.IChannelAggregateFetch;
import jpl.gds.db.api.sql.fetch.cfdp.ICfdpFileGenerationFetch;
import jpl.gds.db.api.sql.fetch.cfdp.ICfdpFileUplinkFinishedFetch;
import jpl.gds.db.api.sql.fetch.cfdp.ICfdpIndicationFetch;
import jpl.gds.db.api.sql.fetch.cfdp.ICfdpPduReceivedFetch;
import jpl.gds.db.api.sql.fetch.cfdp.ICfdpPduSentFetch;
import jpl.gds.db.api.sql.fetch.cfdp.ICfdpRequestReceivedFetch;
import jpl.gds.db.api.sql.fetch.cfdp.ICfdpRequestResultFetch;


public interface IDbSqlFetchFactory {
	/**
	 * @param args
	 * @return
	 */
	public ISessionFetch getSessionFetch(final Object...args);

	/**
	 * @param args
	 * @return
	 */
	public IDbSessionPreFetch getSessionPreFetch(final Object...args);

	/**
	 * @param args
	 * @return
	 */
	public IEndSessionFetch getEndSessionFetch(final Object...args);

	/**
	 * @param args
	 * @return
	 */
	public IContextConfigFetch getContextConfigFetch(final Object...args);

	/**
	 * @param args
	 * @return
	 */
	public IDbContexConfigPreFetch getContextConfigPreFetch(final Object...args);

	/**
	 * @param args
	 * @return
	 */
	public IHostFetch getHostFetch(final Object...args);

	/**
	 * @param args
	 * @return
	 */
	/**
	 * @param args
	 * @return
	 */
	public IChannelDataPreFetch getChannelDataPreFetch(final Object...args);

	/**
	 * @param args
	 * @return
	 */
	public IChannelSummaryFetch getChannelSummaryFetch(final Object...args);

	/**
	 * @param args
	 * @return
	 */
	public IChannelValueFetch getChannelValueFetch(final Object...args);

	/**
	 * @param args
	 * @return
	 */
	public IChannelAggregateFetch getChannelAggregateFetch(final Object...args);
	
	/**
	 * @param args
	 * @return
	 */
	public ICommandFetch getCommandFetch(final Object...args);

	/**
	 * @param args
	 * @return
	 */
	public IEvrFetch getEvrFetch(final Object...args);

	/**
	 * @param args
	 * @return
	 */
	public IFrameFetch getFrameFetch(final Object...args);

	/**
	 * @param args
	 * @return
	 */
	public ILogFetch getLogFetch(final Object...args);

	/**
	 * @param args
	 * @return
	 */
	public IPacketFetch getPacketFetch(final Object...args);

	/**
	 * @param args
	 * @return
	 */
	public IDbProductFetch getProductFetch(final Object...args);



	/**
	 * @param args
	 * @return
	 */
	public ICfdpIndicationFetch getCfdpIndicationFetch(Object...args);

	/**
	 * @param args
	 * @return
	 */
	public ICfdpFileGenerationFetch getCfdpFileGenerationFetch(Object...args);

	/**
	 * @param args
	 * @return
	 */
	public ICfdpFileUplinkFinishedFetch getCfdpFileUplinkFinishedFetch(Object...args);
	
	/**
	 * @param args
	 * @return
	 */
	public ICfdpRequestReceivedFetch getCfdpRequestReceivedFetch(Object...args);

	/**
	 * @param args
	 * @return
	 */
	public ICfdpRequestResultFetch getCfdpRequestResultFetch(Object...args);
	
	/**
	 * @param args
	 * @return
	 */
	public ICfdpPduReceivedFetch getCfdpPduReceivedFetch(Object...args);

	/**
	 * @param args
	 * @return
	 */
	public ICfdpPduSentFetch getCfdpPduSentFetch(Object...args);

}
