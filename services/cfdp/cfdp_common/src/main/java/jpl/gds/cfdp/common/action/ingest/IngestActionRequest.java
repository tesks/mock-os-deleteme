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

package jpl.gds.cfdp.common.action.ingest;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jpl.gds.cfdp.common.GenericRequest;

@JsonIgnoreProperties(ignoreUnknown = true)
public class IngestActionRequest extends GenericRequest {

	private EIngestSource ingestSource;
	private String ingestFileName;

	/**
	 * @return the ingestSource
	 */
	public EIngestSource getIngestSource() {
		return ingestSource;
	}

	/**
	 * @param ingestSource
	 *            the ingestSource to set
	 */
	public void setIngestSource(EIngestSource ingestSource) {
		this.ingestSource = ingestSource;
	}

	/**
	 * @return the ingestFileName
	 */
	public String getIngestFileName() {
		return ingestFileName;
	}

	/**
	 * @param ingestFileName
	 *            the ingestFileName to set
	 */
	public void setIngestFileName(String ingestFileName) {
		this.ingestFileName = ingestFileName;
	}

}
