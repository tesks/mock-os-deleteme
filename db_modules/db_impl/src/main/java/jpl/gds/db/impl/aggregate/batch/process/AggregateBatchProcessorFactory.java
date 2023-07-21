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
package jpl.gds.db.impl.aggregate.batch.process;

import org.springframework.context.ApplicationContext;

import jpl.gds.db.api.sql.fetch.aggregate.IAggregateFetchConfig;
import jpl.gds.db.api.sql.fetch.aggregate.IBatchProcessorBuilder;
import jpl.gds.db.api.sql.fetch.aggregate.IEhaAggregateDbRecord;
import jpl.gds.db.api.sql.fetch.aggregate.RecordBatchContainer;
import jpl.gds.db.api.sql.order.IChannelAggregateOrderByType;

/**
 * Factory used to create Aggregate Batch Processors
 *
 */
public class AggregateBatchProcessorFactory implements IBatchProcessorBuilder<IEhaAggregateDbRecord> {
	
	private final ApplicationContext appContext;
	private final IAggregateFetchConfig config;

	/**
	 * Constructor.
	 * 
	 * @param appContext the Spring Application Context
	 */
	public AggregateBatchProcessorFactory(final ApplicationContext appContext) {
		this.appContext = appContext;
		this.config = appContext.getBean(IAggregateFetchConfig.class);
	}

	
	@Override
	public Runnable getBatchProcessor(final RecordBatchContainer<IEhaAggregateDbRecord> rbc) {
		
		if (config.getOrderByType() == IChannelAggregateOrderByType.NONE_TYPE) {
			return new NonSortingBatchProcessor(appContext, rbc);
		}
		
		return new FileBasedSortingProcessor(appContext, rbc);

	}

}

