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
package jpl.gds.db.impl.aggregate.batch.merge;

import jpl.gds.db.api.sql.fetch.aggregate.IAggregateFetchConfig;
import jpl.gds.db.api.sql.fetch.aggregate.IBatchMergeFactory;
import jpl.gds.db.api.sql.fetch.aggregate.IBatchReaderFactory;
import jpl.gds.db.api.sql.fetch.aggregate.IEhaAggregateQueryCoordinator;
import jpl.gds.db.api.sql.fetch.aggregate.IIndexReaderFactory;
import jpl.gds.db.api.sql.order.IChannelAggregateOrderByType;
import jpl.gds.shared.log.Tracer;


/**
 * Factory class used to create the batch merge object based
 * on fetch configuration.
 *
 */
public class BatchMergeFactory implements IBatchMergeFactory {
		
	private IAggregateFetchConfig config;
	private Tracer trace;
		
	/**
	 * Constructor
	 * 
	 * @param config
	 *         IAggregateFetchConfig configuration
	 * @param trace
	 *         Tracer trace
	 */
	public BatchMergeFactory(IAggregateFetchConfig config, Tracer trace) {
		this.config = config;
		this.trace = trace;
	}

	/* (non-Javadoc)
	 * @see jpl.gds.db.api.sql.fetch.aggregate.IBatchMergeFactory#getBatchMerge(jpl.gds.db.api.sql.fetch.aggregate.IEhaAggregateQueryCoordinator, jpl.gds.db.api.sql.fetch.aggregate.IBatchReaderFactory, jpl.gds.db.api.sql.fetch.aggregate.IIndexReaderFactory)
	 */
	@Override
	public Runnable getBatchMerge(IEhaAggregateQueryCoordinator aggQueryQoordinator,
			IBatchReaderFactory batchReaderFactory, IIndexReaderFactory indexReaderFactory) {
		if (config.getOrderByType() == IChannelAggregateOrderByType.NONE_TYPE) {
			return new NonSortBatchMerge(aggQueryQoordinator, trace);
		} else {
			return new SortBatchMerge(aggQueryQoordinator, config, batchReaderFactory, indexReaderFactory, trace);
		}
	}
}


