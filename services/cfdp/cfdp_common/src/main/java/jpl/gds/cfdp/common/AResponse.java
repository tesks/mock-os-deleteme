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

import com.fasterxml.jackson.annotation.JsonIgnore;

import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.util.HostPortUtility;

public abstract class AResponse extends APrettyPrintingJson {

	private String cfdpProcessorHostName;
	private String cfdpProcessorPort;
	private String cfdpProcessorInstanceId;

	public AResponse() {
		super();
		cfdpProcessorHostName = HostPortUtility.getLocalHostName();
	}

	/**
	 * @return the cfdpProcessorHostName
	 */
	public String getCfdpProcessorHostName() {
		return cfdpProcessorHostName;
	}

	/**
	 * @param cfdpProcessorHostName
	 *            the cfdpProcessorHostName to set
	 */
	public void setCfdpProcessorHostName(final String cfdpProcessorHostName) {
		this.cfdpProcessorHostName = cfdpProcessorHostName;
	}

	/**
	 * @return the cfdpProcessorPort
	 */
	public String getCfdpProcessorPort() {
		return cfdpProcessorPort;
	}

	/**
	 * @param cfdpProcessorPort
	 *            the cfdpProcessorPort to set
	 */
	public void setCfdpProcessorPort(final String cfdpProcessorPort) {
		this.cfdpProcessorPort = cfdpProcessorPort;
	}

	/**
	 * @return the cfdpProcessorInstanceId
	 */
	public String getCfdpProcessorInstanceId() {
		return cfdpProcessorInstanceId;
	}

	/**
	 * @param cfdpProcessorInstanceId
	 *            the cfdpProcessorInstanceId to set
	 */
	public void setCfdpProcessorInstanceId(final String cfdpProcessorInstanceId) {
		this.cfdpProcessorInstanceId = cfdpProcessorInstanceId;
	}

	@JsonIgnore
	public String getFullCfdpProcessorIdentification() {
		return "CFDP Processor Instance " + getCfdpProcessorInstanceId() + " at " + getCfdpProcessorHostName() + ":"
				+ getCfdpProcessorPort();
	}

	public void printToSystemOut() {
		System.out.println("Response received from " + getFullCfdpProcessorIdentification());
	}
	
	public void printToTracer(final Tracer log) {
		log.info("Response received from " + getFullCfdpProcessorIdentification());
	}

}