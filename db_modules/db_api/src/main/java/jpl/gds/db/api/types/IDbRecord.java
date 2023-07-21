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
package jpl.gds.db.api.types;

public interface IDbRecord extends IDbInfoProvider {
	/**
     * @return the Record Offset of this record
     */
	Long getRecordOffset();

	/**
     * @return returns a byte array representing the contents of this record
     */
	byte[] getRecordBytes();

	/* 
	 * BEGIN: MPCS-6349 : DSS ID not set properly
	 * Removed field dssId from all subclasses. Updated this class with 
	 * protected fields sessionDssId and recordDssId with get/set 
	 * methods for both.
	 */
	/**
	 * Return DSS id from Record.
	 *
	 * @return DSS id from Record
	 */
	int getRecordDssId();

	/**
	 * Get record length.
	 *
	 * @return Record length
	 */
	int getRecordLength();
}