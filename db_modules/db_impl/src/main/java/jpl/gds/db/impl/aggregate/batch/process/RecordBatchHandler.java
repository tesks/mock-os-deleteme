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

import jpl.gds.db.api.sql.fetch.aggregate.AggregateFetchException;
import jpl.gds.db.api.sql.fetch.aggregate.IBatchProcessorBuilder;
import jpl.gds.db.api.sql.fetch.aggregate.IBoundedExecutor;
import jpl.gds.db.api.sql.fetch.aggregate.IRecordBatchHandler;
import jpl.gds.db.api.sql.fetch.aggregate.RecordBatchContainer;


/**
 * This class uses the Processor Builder Factory to retrieve the batch 
 * processor and submits the processor to the Bounded Executor for actual
 * execution.
 *
 * @param <T>
 */
public class RecordBatchHandler<T> implements IRecordBatchHandler<T> {
	
	private final IBatchProcessorBuilder<T> processorBuilderFactory;
	private final IBoundedExecutor boundedExecutor;
	
	/**
	 * Constructor
	 * 
	 * @param processorBuilderFactory the aggregate processor builder factory
	 * @param boundedExecutor the bounded executor used for task submission
	 */
	public RecordBatchHandler(final IBatchProcessorBuilder<T> processorBuilderFactory, final IBoundedExecutor boundedExecutor) {
		this.processorBuilderFactory = processorBuilderFactory;
		this.boundedExecutor = boundedExecutor;
	}

	@Override
	public void handleRecordBatch(final RecordBatchContainer<T> batchContainer) throws AggregateFetchException {
		boundedExecutor.submitTask(processorBuilderFactory.getBatchProcessor(batchContainer));
	}
}
