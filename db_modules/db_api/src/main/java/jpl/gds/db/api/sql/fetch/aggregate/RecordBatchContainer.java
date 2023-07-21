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

/**
 * Container object for batched records with a specific Type
 *
 * @param <T>
 */
public class RecordBatchContainer<T> {
	
	private String batchId;
	private List<T> recordBatch;
	
	/**
	 * Constructs a record batch container 
	 * 
	 * @param batchId Batch ID assigned to this record batch
	 * @param recordBatch List containing the record batch
	 */
	public RecordBatchContainer(final String batchId, final List<T> recordBatch) {
		this.batchId = batchId;
		this.recordBatch = recordBatch;
	}

	/**
	 * Gets the batch ID
	 * 
	 * @return batch ID assigned to this batch
	 */
	public String getBatchId() {
		return batchId;
	}
	
	/**
	 * Sets the batch ID
	 * 
	 * @param batchId The batch ID to set
	 */
	public void setBatchId(final String batchId) {
		this.batchId = batchId;
	}
	
	/**
	 * Gets the record batch
	 * 
	 * @return List of records
	 */
	public List<T> getBatchList() {
		return recordBatch;
	}
	
	/**
	 * Sets the record batch
	 * 
	 * @param recordBatch List of records
	 */
	public void setBatchList(final List<T> recordBatch) {
		this.recordBatch = recordBatch;
	}	
}
