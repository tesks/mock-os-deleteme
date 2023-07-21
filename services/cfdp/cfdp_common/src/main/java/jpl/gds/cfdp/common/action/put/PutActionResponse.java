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

package jpl.gds.cfdp.common.action.put;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jpl.gds.cfdp.common.GenericActionResponse;
import jpl.gds.shared.log.Tracer;

/**
 * Class PutActionResponse
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PutActionResponse extends GenericActionResponse {

	private List<Long> newTransactionId;

	/**
	 * @return the newTransactionId
	 */
	public List<Long> getNewTransactionId() {
		return newTransactionId;
	}

	/**
	 * @param newTransactionId
	 *            the newTransactionId to set
	 */
	public void setNewTransactionId(final List<Long> newTransactionId) {
		this.newTransactionId = newTransactionId;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see jpl.gds.cfdp.common.GenericActionResponse#printToSystemOut()
	 */
	@Override
	public void printToSystemOut() {
		super.printToSystemOut();

		if (getNewTransactionId() != null) {
			System.out.println("New transaction started...");
			System.out.println(Long.toUnsignedString(newTransactionId.get(0).longValue()) + ":"
					+ Long.toUnsignedString(newTransactionId.get(1).longValue()));
		} else {
			System.out.println("New transaction ID is unknown");
		}

	}
	
	@Override
	public void printToTracer(final Tracer log) {
		super.printToTracer(log);

		if (getNewTransactionId() != null) {
			log.info("New transaction started...");
			log.info(Long.toUnsignedString(newTransactionId.get(0).longValue()) + ":"
					+ Long.toUnsignedString(newTransactionId.get(1).longValue()));
		} else {
			log.info("New transaction ID is unknown");
		}

	}

}
