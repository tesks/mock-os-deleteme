/*
 * Copyright 2006-2019. California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government sponsorship acknowledged.
 *
 *  This software is subject to U. S. export control laws and
 *  regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 *  extent that the software is subject to U.S. export control laws
 *  and regulations, the recipient has the responsibility to obtain
 *  export licenses or other export authority as may be required
 *  before exporting such information to foreign countries or
 *  providing access to foreign nationals.
 */

package jpl.gds.cfdp.data.api;

/**
 * Class ECfdpFaultCondition
 */
public enum ECfdpFaultCondition implements ICfdpCondition {

	POSITIVE_ACK_LIMIT_REACHED,
	KEEP_ALIVE_LIMIT_REACHED,
	INVALID_TRANSMISSION_MODE,
	FILESTORE_REJECTION,
	FILE_CHECKSUM_FAILURE,
	FILE_SIZE_ERROR,
	NAK_LIMIT_REACHED,
	INACTIVITY_DETECTED,
	INVALID_FILE_STRUCTURE,
	CHECK_LIMIT_REACHED;

	@Override
	public String toString() {
		return name();
	}
	
}
