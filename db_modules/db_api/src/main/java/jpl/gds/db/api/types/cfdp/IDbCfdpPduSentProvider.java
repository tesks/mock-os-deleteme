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
package jpl.gds.db.api.types.cfdp;

import jpl.gds.db.api.types.IDbQueryable;
import jpl.gds.shared.time.IAccurateDateTime;

/**
 * This is the interface for DatabaseCfdpPduSent objects, which are representations of the records
 * in the database that track each CFDP PDU that has been sent by an AMPCS CFDP processor.
 */
public interface IDbCfdpPduSentProvider extends IDbQueryable {

	/**
	 * Get the time at which this event occurred
	 * @return the IAccurateDateTime representation of when this PDU was sent
	 */
	IAccurateDateTime getPduTime();

	/**
	 * The name of the CFDP processor that sent the CFDP PDU
	 *
	 * @return the name of the CFDP processor that sent the CFDP PDU
	 */
	String getCfdpProcessorInstanceId();

	/**
	 * The unique identifier of the PDU that was sent
	 * @return the unique identifier of the PDU that was sent
	 */
	String getPduId();

	/**
	 * All metadata for the sent CFDP PDU is concatenated into a single string
	 * @return the string containing all of the metadata for the CFDP PDU
	 */
	String getMetadata();

	/**
	 * Get a single value from the sent CFDP PDU metadata
	 * @param key the name of a metadata value
	 * @return the string value of a metadata value. A blank string will be returned
	 * if no value is found.
	 */
	String getMetadata(final String key);

}