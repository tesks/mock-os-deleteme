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
package jpl.gds.db.api.sql.fetch.aggregate;

/**
 * An interface to be implemented by Aggregate Batch Processors
 *
 * @param <T>
 */
public interface IBatchProcessorBuilder<T> {
    
	/**
	 * Gets a batch processor for the passed in RecordBatchContainer
	 * 
	 * @param rbc The RecordBatchContainer that holds the batch 
	 * @return The batch processor which implements the Runnable interface
	 */
	public Runnable getBatchProcessor(final RecordBatchContainer<T> rbc);
}

