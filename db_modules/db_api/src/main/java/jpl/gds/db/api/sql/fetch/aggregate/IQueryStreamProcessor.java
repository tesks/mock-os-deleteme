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

import java.util.List;

import jpl.gds.db.api.DatabaseException;

/**
 * An interface implemented by the Query Stream Processor
 *
 * @param <T>
 */
public interface IQueryStreamProcessor<T> extends Runnable {
	
	/**
	 * Creates record batches from the database query result stream
	 * 
	 * @return the batch of records
	 * @throws DatabaseException
	 */
	public List<T> batchRecords() throws DatabaseException;
	
	/**
	 * Checks running state
	 * 
	 * @return true if running and false otherwise
	 */
	public boolean isRunning();
}
