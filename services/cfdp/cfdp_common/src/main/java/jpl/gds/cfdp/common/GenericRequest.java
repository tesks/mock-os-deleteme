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

@JsonIgnoreProperties(ignoreUnknown = true)
public class GenericRequest extends APrettyPrintingJson {

	private String requesterId;

	/**
	 * Method that copies fields to a second object.
	 *
	 * @param objToCopyFieldsTo object to copy local fields to
	 */
	protected void copyFieldsTo(final GenericRequest objToCopyFieldsTo) {
		objToCopyFieldsTo.setRequesterId(getRequesterId());
	}

	/**
	 * @return the requesterId
	 */
	public String getRequesterId() {
		return requesterId;
	}

	/**
	 * @param requesterId
	 *            the requesterId to set
	 */
	public void setRequesterId(String requesterId) {
		this.requesterId = requesterId;
	}

}