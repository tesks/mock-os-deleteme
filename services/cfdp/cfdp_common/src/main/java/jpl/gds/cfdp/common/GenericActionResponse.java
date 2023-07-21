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

package jpl.gds.cfdp.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jpl.gds.shared.log.Tracer;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GenericActionResponse extends AResponse {

	protected static String actionNotAppliedToAnyTransactionMessage = "Action did not get applied to any transaction";

	private String message;
	private String requestId;

	/**
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * @param message
	 *            the message to set
	 */
	public void setMessage(final String message) {
		this.message = message;
	}

	/**
	 * @return the requestId
	 */
	public String getRequestId() {
		return requestId;
	}

	/**
	 * @param requestId
	 *            the requestId to set
	 */
	public void setRequestId(final String requestId) {
		this.requestId = requestId;
	}

	@Override
	public void printToSystemOut() {
		super.printToSystemOut();
		System.out.println("Request ID: " + getRequestId());

		if (getMessage() != null) {
			System.out.println("Message: " + getMessage());
		}

	}
	
	@Override
	public void printToTracer(final Tracer log) {
		super.printToTracer(log);
		log.info("Request ID: " + getRequestId());

		if (getMessage() != null) {
			log.info("Message: " + getMessage());
		}
	}

}