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
 * An interface to be implemented by the Batch Reader Factory
 *
 */
public interface IBatchReaderFactory {
    
	/**
	 * Gets a batch reader object based on fetch configuration and passed in parameters
	 * 
	 * @param aggQueryQoordinator The query coordinator
	 * @param batchId The batch ID
	 * @return The batch reader object
	 * @throws AggregateFetchException
	 */
	public IBatchFileRecordProvider getBatchRecordProvider(final IEhaAggregateQueryCoordinator coordinator, final String batchId) throws AggregateFetchException;
}
