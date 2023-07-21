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

import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.time.AccurateDateTime;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.tc.api.CommandStatusType;
import jpl.gds.tc.api.IUplinkResponse;
import jpl.gds.tc.api.UplinkFailureReason;

/**
 * A generic uplink response, from a generic uplink source
 *
 * @since AMPCS R3
 */
public class GenericUplinkResponse implements IUplinkResponse {

	/** Tracer */
	protected final static Tracer logger = TraceManager.getDefaultTracer();

	
	/** The CPD request ID */
	protected String requestId;

	/** The CPD request status */
	protected CommandStatusType status;

	/** The failure reason, if applicable. null if no failure */
	protected UplinkFailureReason failureReason;

	/** The diagnostic message in the event of a failure */
	protected String diagnosticMessage;

	/** Indicates whether or not the uplink was successful */
	protected boolean wasSuccessful;

	/** The SCMF checksum */
	protected Long scmfChecksum;

	/** The total CLTUs in the SCMF */
	protected Long totalCltus;

	/** The time the status was changed */
	protected IAccurateDateTime statusChangeTimestamp;
	
	/**
	 * Default constructor
	 */
	public GenericUplinkResponse() {
		this("", CommandStatusType.Send_Failure, UplinkFailureReason.UNKNOWN,
				"", null, null, new AccurateDateTime());
	}

	/**
     * Constructor
     * 
     * @param requestId
     *            the CPD request ID
     * @param status
     *            the CPD request status
     * @param failureReason
     *            the failure reason
     * @param diagnosticMessage
     *            the diagnostic message
     * @param scmfChecksum
     *            the SCMF checksum
     * @param totalCltus
     *            the total CLTUs
     * @param statusChangeTimestamp
     *            the time at which the status changed.
     */
	public GenericUplinkResponse(final String requestId, final CommandStatusType status,
			final UplinkFailureReason failureReason, final String diagnosticMessage,
            final Long scmfChecksum, final Long totalCltus, final IAccurateDateTime statusChangeTimestamp) {
		this.requestId = requestId;
		this.status = status;
		this.failureReason = failureReason;
		this.diagnosticMessage = diagnosticMessage;
		this.wasSuccessful = failureReason == null;
		this.scmfChecksum = scmfChecksum;
		this.totalCltus = totalCltus;
		this.statusChangeTimestamp = statusChangeTimestamp;
	}

    /**
     * {@inheritDoc}
     */
	@Override
	public void setRequestId(final String requestId) {
		this.requestId = requestId;
	}

    /**
     * {@inheritDoc}
     */
	@Override
	public void setStatus(final CommandStatusType status) {
		this.status = status;
	}

    /**
     * {@inheritDoc}
     */
	@Override
	public void setFailureReason(final UplinkFailureReason failureReason) {
		this.failureReason = failureReason;
	}

    /**
     * {@inheritDoc}
     */
	@Override
	public String getRequestId() {
		return this.requestId;
	}

    /**
     * {@inheritDoc}
     */
	@Override
	public CommandStatusType getStatus() {
		return this.status;
	}

    /**
     * {@inheritDoc}
     */
	@Override
	public UplinkFailureReason getFailureReason() {
		return this.failureReason;
	}

    /**
     * {@inheritDoc}
     */
	@Override
	public String getDiagnosticMessage() {
		return this.diagnosticMessage;
	}

    /**
     * {@inheritDoc}
     */
	@Override
	public void setDiagnosticMessage(final String diagnosticMessage) {
		this.diagnosticMessage = diagnosticMessage;
	}

    /**
     * {@inheritDoc}
     */
	@Override
	public boolean isSuccessful() {
		return this.wasSuccessful;
	}

    /**
     * {@inheritDoc}
     */
	@Override
	public void setSuccessful(final boolean successful) {
		this.wasSuccessful = successful;
	}

    /**
     * {@inheritDoc}
     */
	@Override
	public Long getScmfChecksum() {
		return this.scmfChecksum;
	}

    /**
     * {@inheritDoc}
     */
	@Override
	public Long getTotalCltus() {
		return this.totalCltus;
	}

    /**
     * {@inheritDoc}
     */
	@Override
    public IAccurateDateTime getStatusChangeTime() {
		return this.statusChangeTimestamp;
	}
}
