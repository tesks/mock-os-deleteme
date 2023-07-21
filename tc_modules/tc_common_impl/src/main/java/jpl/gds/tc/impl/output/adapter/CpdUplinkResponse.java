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
package jpl.gds.tc.impl.output.adapter;

import java.text.ParseException;
import java.util.List;

import gov.nasa.jpl.icmd.schema.ICmdRadiationStatusType;
import gov.nasa.jpl.icmd.schema.ICmdUplinkStateType;
import gov.nasa.jpl.icmd.schema.InsertResponseType;
import gov.nasa.jpl.icmd.schema.RequestResultStatusType;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.tc.api.CommandStatusType;
import jpl.gds.tc.api.IScmf;
import jpl.gds.tc.api.UplinkFailureReason;

/**
 * This class takes in a raw CPD response and processes it for use with AMPCS
 * 
 * @since AMPCS R3
 */
public class CpdUplinkResponse extends GenericUplinkResponse {

	/**
	 * Constructor
	 * 
	 * @param response the CPD response
	 */
	public CpdUplinkResponse(final InsertResponseType response, final IScmf scmf) {
		this.wasSuccessful = true;
		this.failureReason = UplinkFailureReason.NONE;

		this.scmfChecksum = Long.valueOf(scmf.getFileChecksum());
		this.totalCltus = Long.valueOf(scmf.getCltuCount());

		if (response != null) {
			if (response.getREQUESTINFO() != null) {
				this.requestId = response.getREQUESTINFO().getREQUESTID();

				final List<ICmdRadiationStatusType> statusList = response
						.getREQUESTINFO().getSTATUSLIST().getSTATUS();

				final ICmdRadiationStatusType statusType = statusList.get(statusList
						.size() - 1);

				final String updateTimeStr = statusType.getUPDATETIME();

				try {
					this.statusChangeTimestamp = new AccurateDateTime(updateTimeStr);
				} catch (final ParseException e) {
					this.statusChangeTimestamp = new AccurateDateTime();
					logger.warn("Unable to parse update time from CPD, using current time ("
							+ e.getMessage() + ")");
				}

				final ICmdUplinkStateType status = statusType.getSTATE();

				this.status = CommandStatusType.valueOfIgnoreCase(status
						.toString());
			} else {
				this.status = CommandStatusType.Send_Failure;
			}

			if (response.getRESPONSE() != null) {
				if (response.getRESPONSE().getSTATUS()
						.equals(RequestResultStatusType.ERROR)) {
					this.diagnosticMessage = response.getRESPONSE().getDIAG();
					this.status = CommandStatusType.Send_Failure;
					this.failureReason = UplinkFailureReason.COMMAND_SERVICE_REJECTION;
					this.wasSuccessful = false;
				}
			}
		} else {
			this.diagnosticMessage = "Unable to parse command service response";
			this.failureReason = UplinkFailureReason.UNKNOWN;
			this.wasSuccessful = false;
		}
	}

	/**
	 * Constructor
	 * 
	 * @param requestId the CPD request ID
	 * @param status the CPD request status
	 * @param isSuccessful whether or not the uplink was successful
	 * @param scmf the SCMF that was sent
	 */
	public CpdUplinkResponse(final String requestId, final CommandStatusType status,
			final boolean isSuccessful, final IScmf scmf) {
		this.requestId = requestId;
		this.status = status;
		this.wasSuccessful = isSuccessful;
	}
}
