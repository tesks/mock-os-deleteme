package jpl.gds.tc.api.output;

import gov.nasa.jpl.icmd.schema.InsertResponseType;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.tc.api.CommandStatusType;
import jpl.gds.tc.api.IScmf;
import jpl.gds.tc.api.IUplinkResponse;
import jpl.gds.tc.api.UplinkFailureReason;

/**
 * An IUplinkResponse object is a holder for the various pieces of information regarding
 * the transmission of an uplink command. This factory facilitates the creation of
 * IUplinkResponse objects.
 * 
 *
 * MPCS-10813 - 04/09/19 - created
 */
public interface IUplinkResponseFactory {

	/**
	 * Create an empty generic uplink response
	 * 
	 * @return an empty generic uplink response
	 */
	public IUplinkResponse getGenericUplinkResponse();

	/**
	 * Create a populated generic uplink response
	 * 
	 * @param requestId the ID of the uplink session
	 * @param status the status received for the uplink session
	 * @param failureReason the failure reason for the uplink session
	 * @param diagnosticMessage the message for this uplink session
	 * @param scmfChecksum the checksum from the SCMF for this uplink session
	 * @param totalCltus the total number of CLTUs uplinked in this session
	 * @param statusChangeTimestamp the time of this status change
	 * 
	 * @return a populated generic uplink response
	 */
	public IUplinkResponse getGenericUplinkResponse(String requestId, CommandStatusType status,
			UplinkFailureReason failureReason, String diagnosticMessage, Long scmfChecksum, Long totalCltus,
			IAccurateDateTime statusChangeTimestamp);

	/**
	 * Create an uplink response with data from CPD
	 * 
	 * @param response the InsertResponseType from CPD
	 * @param scmf the SCMF for this upink session
	 * 
	 * @return a populated CPD uplink response
	 */
	public IUplinkResponse getCpdUplinkResponse(InsertResponseType response, IScmf scmf);

	/**
	 * Create an uplink response for CPD that does not have an InsertResponseType
	 * 
	 * @param requestId the ID of the uplink session
	 * @param status the status received for the uplink session
	 * @param isSuccessful boolean flag indicating if the uplink succeeded or not
	 * @param scmf the SCMF for this uplink session
	 * 
	 * @return a CPD uplink response
	 */
	public IUplinkResponse getCpdUplinkResponse(String requestId, CommandStatusType status, boolean isSuccessful, IScmf scmf);

}