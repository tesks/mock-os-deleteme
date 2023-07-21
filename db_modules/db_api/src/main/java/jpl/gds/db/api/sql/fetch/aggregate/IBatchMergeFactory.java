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
 * An interface to be implemented by the Batch Merge Factory
 *
 */
public interface IBatchMergeFactory {
    
	/**
	 * Gets the batch merge object based on fetch configuration and passed in parameters
	 * 
	 * @param aggQueryQoordinator The EHA aggregate coordinator
	 * @param batchReaderFactory The batch record reader factory
	 * @param indexReaderFactory The batch index reader factory
	 * 
	 * @return The Batch Merge object which is a Runnable
	 */
	public Runnable getBatchMerge(
			IEhaAggregateQueryCoordinator aggQueryQoordinator, 
			IBatchReaderFactory batchReaderFactory,
			IIndexReaderFactory indexReaderFactory);
}
