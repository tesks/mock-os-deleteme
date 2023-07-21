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
package jpl.gds.tc.api.icmd;

import gov.nasa.jpl.icmd.schema.RequestResultStatusType;
import gov.nasa.jpl.icmd.schema.ResponseStatus;

/**
 * This class wraps a response from CPD
 * 
 * @since AMPCS R5
 */
public class CpdResponse {
	/** The CPD response status */
	private RequestResultStatusType status;

	/** The CPD diagnostic message */
	private String diagnosticMessage;

	/**
	 * Constructor
	 * 
	 * @param response the CPD response
	 */
	public CpdResponse(ResponseStatus response) {
		this.status = response.getSTATUS();
		this.diagnosticMessage = response.getDIAG();
	}

	/**
	 * Indicates whether or not the request was successful
	 * 
	 * @return true if successful, false otherwise
	 */
	public boolean isSuccessful() {
		return this.status.equals(RequestResultStatusType.OK);
	}

	/**
	 * Get the CPD diagnostic message
	 * 
	 * @return the CPD diagnostic message
	 */
	public String getDiagnosticMessage() {
		return this.diagnosticMessage;
	}

	/**
	 * Returns pertinent attributes of the response in a key=value
	 * comma-separated format
	 * 
	 * @return pertinent attributes of the response in a key=value
	 *         comma-separated format
	 */
	public String toKeyValueCsv() {
		StringBuilder builder = new StringBuilder();

		builder.append("status");
		builder.append("=");
		builder.append(this.status.toString());

		if (this.diagnosticMessage != null && !this.diagnosticMessage.isEmpty()) {
			builder.append(",");
			builder.append("message");
			builder.append("=");
			builder.append(this.diagnosticMessage);
		}

		return builder.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((diagnosticMessage == null) ? 0 : diagnosticMessage
						.hashCode());
		result = prime * result + ((status == null) ? 0 : status.hashCode());
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj == null) {
			return false;
		}

		if (getClass() != obj.getClass()) {
			return false;
		}

		CpdResponse other = (CpdResponse) obj;

		if (diagnosticMessage == null) {
			if (other.diagnosticMessage != null) {
				return false;
			}
		} else if (!diagnosticMessage.equals(other.diagnosticMessage)) {
			return false;
		}

		if (status != other.status) {
			return false;
		}

		return true;
	}
}
