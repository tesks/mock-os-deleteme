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
package jpl.gds.tc.api;

import jpl.gds.shared.time.IAccurateDateTime;

/**
 * This interface provides a common ancestor for all uplink response.
 * 
 * @since AMPCS R3
 */
public interface IUplinkResponse {
	/**
	 * Get the time of the status change
	 * @return the time of the status change
	 */
    public IAccurateDateTime getStatusChangeTime();
	
	/**
	 * Retrieve the uplink request ID
	 * 
	 * @return the uplink request ID
	 */
	public String getRequestId();

	/**
	 * Retrieve the uplink request status
	 * 
	 * @return the uplink request status
	 */
	public CommandStatusType getStatus();
	
	/**
	 * Retrieve the uplink failure reason if the uplink failed.
	 * @return the uplink failure reason
	 */
	public UplinkFailureReason getFailureReason();
	
	/**
	 * Set the CPD request ID
	 * @param requestId the CPD request ID
	 */
	public void setRequestId(String requestId);

	/**
	 * Set the CPD request status
	 * @param status the CPD request status
	 */
	public void setStatus(CommandStatusType status);

	/**
	 * Set the failure reason
	 * @param failureReason the failure reason
	 */
	public void setFailureReason(UplinkFailureReason failureReason);
	
	/**
	 * Get the diagnostic message that describes the problem, if applicable
	 * @return the diagnostic message
	 */
	public String getDiagnosticMessage();
	
	/**
	 * Set the diagnostic message to describe a problem, if applicable
	 * @param diagnosticMessage the diagnostic message
	 */
	public void setDiagnosticMessage(String diagnosticMessage);
	
	/**
	 * Indicates whether or not the uplink was successful
	 * @return true if successful, false otherwise
	 */
	public boolean isSuccessful();
	
	/**
	 * Set whether or not the uplink was successful
	 * @param successful true if successful, false otherwise
	 */
	public void setSuccessful(boolean successful);
	
	/**
	 * Get the checksum of the SCMF that was sent
	 * @return the checksum of the SCMF that was sent
	 */
	public Long getScmfChecksum();
	
	/**
	 * Get the total CLTUs in the SCMF that was sent
	 * @return the total CLTUs in the SCMF that was sent
	 */
	public Long getTotalCltus();
}
