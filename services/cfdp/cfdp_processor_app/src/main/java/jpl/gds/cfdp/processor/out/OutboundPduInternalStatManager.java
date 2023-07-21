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

package jpl.gds.cfdp.processor.out;

import org.springframework.stereotype.Service;

/**
 * Holder class for internal PDU flags and statistics.
 * 
 */
@Service
public class OutboundPduInternalStatManager {

	private boolean pduSentThisCycle;
	private long pduSentTimestamp;

	/**
	 * Getter for pduSentThisCycle flag
	 * 
	 * @return pduSentThisCycle flag
	 */
	public boolean isPduSentThisCycle() {
		return pduSentThisCycle;
	}

	/**
	 * Setter for pduSentThisCycle flag
	 * 
	 * @param pduSentThisCycle
	 *            flag to set
	 */
	public void setPduSentThisCycle(boolean pduSentThisCycle) {
		this.pduSentThisCycle = pduSentThisCycle;
	}

	/**
	 * Getter for pduSentTimestamp value
	 * 
	 * @return pduSentTimestamp value
	 */
	public long getPduSentTimestamp() {
		return pduSentTimestamp;
	}

	/**
	 * Setter for pduSentTimestamp value
	 * 
	 * @param pduSentTimestamp
	 *            value to set
	 */
	public void setPduSentTimestamp(long pduSentTimestamp) {
		this.pduSentTimestamp = pduSentTimestamp;
	}

}