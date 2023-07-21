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

import java.util.Collections;
import java.util.List;

/**
 * Information object for processed record batches
 *
 *
 */
public class ProcessedBatchInfo {
    
	private final String recordFilename;
	private final String indexFileName;
	private final List<String> recordList;
	
	/**
	 * Constructor used for storing record file name, index file name and record batch
	 * 
	 * @param recordFileName the temporary batch record file name 
	 * @param indexFileName the temporary batch index file name
	 * @param recordList the record batch
	 */
	public ProcessedBatchInfo(final String recordFileName, final String indexFileName, final List<String> recordList) {
	    this.recordFilename = recordFileName;
	    this.indexFileName = indexFileName;
	    this.recordList = recordList;
	}
	
	/**
	 * Constructor used for storing the record file name and index file name
	 * 
     * @param recordFileName the temporary batch record file name 
     * @param indexFileName the temporary batch index file name
	 */
	public ProcessedBatchInfo(final String recordFileName, final String indexFileName) {
	    this(recordFileName, indexFileName, Collections.emptyList());
	}
	
	/**
	 * Constructor user for storing the record batch
	 * 
	 * @param recordList the record batch
	 */
	public ProcessedBatchInfo(final List<String> recordList) {
	    this(null, null, recordList);
	}
	
	/**
	 * Gets the batch index file name
	 * 
	 * @return the indexFileName 
	 */
	public String getIndexFilename() {
		return indexFileName;
	}
	
	/**
	 * Gets the batch record file name
	 * 
	 * @return the recordFilename
	 */
	public String getRecordFilename() {
		return recordFilename;
	}

	/**
	 * Gets the batch record list
	 * 
	 * @return the recordList
	 */
	public List<String> getRecordList() {
		return recordList;
	}
}
