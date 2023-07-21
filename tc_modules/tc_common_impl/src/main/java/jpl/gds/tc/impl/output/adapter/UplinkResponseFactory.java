package jpl.gds.tc.impl.output.adapter;

import gov.nasa.jpl.icmd.schema.InsertResponseType;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.tc.api.CommandStatusType;
import jpl.gds.tc.api.IScmf;
import jpl.gds.tc.api.IUplinkResponse;
import jpl.gds.tc.api.UplinkFailureReason;
import jpl.gds.tc.api.output.IUplinkResponseFactory;

/**
 * An IUplinkResponse object is a holder for the various pieces of information regarding
 * the transmission of an uplink command. This factory facilitates the creation of
 * IUplinkResponse objects.
 * 
 *
 */
public class UplinkResponseFactory implements IUplinkResponseFactory {
	
	
	@Override
	public IUplinkResponse getGenericUplinkResponse() {
		return new GenericUplinkResponse();
	}
	

	@Override
	public IUplinkResponse getGenericUplinkResponse(final String requestId, final CommandStatusType status,
			final UplinkFailureReason failureReason, final String diagnosticMessage,
            final Long scmfChecksum, final Long totalCltus, final IAccurateDateTime statusChangeTimestamp) {
		return new GenericUplinkResponse(requestId, status, failureReason, diagnosticMessage, scmfChecksum, totalCltus, statusChangeTimestamp);
	}
	

	@Override
	public IUplinkResponse getCpdUplinkResponse(final InsertResponseType response, final IScmf scmf) {
		return new CpdUplinkResponse(response, scmf);
	}
	

	@Override
	public IUplinkResponse getCpdUplinkResponse(final String requestId, final CommandStatusType status,
			final boolean isSuccessful, final IScmf scmf) {
		return new CpdUplinkResponse(requestId, status, isSuccessful, scmf);
	}

}
