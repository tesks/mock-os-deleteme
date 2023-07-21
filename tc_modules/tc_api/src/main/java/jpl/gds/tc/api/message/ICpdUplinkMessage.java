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

package jpl.gds.tc.api.message;


import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.tc.api.CommandStatusType;
import jpl.gds.tc.api.UplinkFailureReason;

/**
 * This interface defines methods that all ICMD-based transmission request
 * messages will implement.
 * 
 * @since AMPCS R3
 */
public interface ICpdUplinkMessage extends IUplinkMessage {

	/**
	 * Returns the ICMD request ID for the command request represented by this
	 * message.
	 * 
	 * @return the request ID string from ICMD.
	 */
	public String getICmdRequestId();

	/**
	 * Sets the ICMD request ID.
	 * 
	 * @param iCmdRequestId
	 *            the request ID from ICMD to set for this message.
	 */
	public void setICmdRequestId(String iCmdRequestId);

	/**
	 * Returns the ICMD request status for the command request represented by
	 * this message.
	 * 
	 * @return the status of the command request.
	 */
	public CommandStatusType getICmdRequestStatus();

	/**
	 * Sets the ICMD command request status.
	 * 
	 * @param iCmdRequestStatus
	 *            the command request status from ICMD to set for this message.
	 */
	public void setICmdRequestStatus(CommandStatusType iCmdRequestStatus);

	/**
	 * Returns the failure reason string from ICMD for the requested command
	 * represented by this message.
	 * 
	 * @return failure reason of the command request.
	 */
	public String getICmdRequestFailureReason();

	/**
	 * Sets the failure reason string for the requested command.
	 * 
	 * @param iCmdRequestFailureReason
	 *            failure reason of the request.
	 */
	public void setICmdRequestFailureReason(UplinkFailureReason iCmdRequestFailureReason);

	/**
	 * Returns the location and filename of the original SCMF, raw data, or
	 * other file requested to be transmitted.
	 * 
	 * @return Full path and filename of the original file requested to ICMD for
	 *         transmission.
	 */
	public String getOriginalFilename();

	/**
	 * Sets the location and filename of the original SCMF, raw data, or other
	 * file requested to ICMD for transmission.
	 * 
	 * @param origFilename
	 *            The original full path and filename of the file requested to
	 *            ICMD for transmission.
	 */
	public void setOriginalFilename(String origFilename);

	/**
	 * Returns the location and filename of the generated SCMF (or archived, if
	 * original was an SCMF) that was requested to be transmitted.
	 * 
	 * @return Full path and filename of the generated SCMF file requested to
	 *         ICMD for transimssion.
	 */
	public String getScmfFilename();

	/**
	 * Sets the location and filename of the generated SCMF (or archived, if
	 * original was an SCMF) that was requested to be transmitted.
	 * 
	 * @param scmfFilename
	 *            The full path and filename of the generated (or archived) SCMF
	 *            requested to ICMD for transmission.
	 */
	public void setScmfFilename(String scmfFilename);
	
	/**
	 * Gets the id that links this cmd message with a transmit event
	 * 
	 * @return hashcode object associated with TransmitEvent object
	 */
	public int getTransmitEventId();
	
	public void setTransmitEventId(int transmitEventId);


    /**
     * Get the SCMF checksum.
     *
     * @return Value
     */
    public Long getChecksum();


    /**
     * Get the CLTU count.
     *
     * @return Value
     */
    public Long getTotalCltus();


    /**
     * Get the station id.
     *
     * @return Station id
     */
    public Integer getDssId();


    /**
     * Get the radiation start time.
     *
     * @return Start time
     */
    public IAccurateDateTime getBit1RadTime();


    /**
     * Get the radiation end time.
     *
     * @return End time
     */
    public IAccurateDateTime getLastBitRadTime();
    
    /**
     * Set the SCMF checksum.
     *
     * @param checksum the SCMF checksum
     */
    public void setChecksum(Long checksum);


    /**
     * Set the CLTU count.
     *
     * @param totalCltus the CLTU count
     */
    public void setTotalCltus(Long totalCltus);

    /**
     * Set the radiation start time.
     *
     * @param bit1RadTime the radiation start time
     */
    public void setBit1RadTime(IAccurateDateTime bit1RadTime);


    /**
     * Set the radiation end time.
     * 
     * @param lastBitRadTime the radiation end time
     */
    public void setLastBitRadTime(IAccurateDateTime lastBitRadTime);
}
