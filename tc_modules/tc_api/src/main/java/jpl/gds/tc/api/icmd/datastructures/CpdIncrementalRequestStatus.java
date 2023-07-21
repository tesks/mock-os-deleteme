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

package jpl.gds.tc.api.icmd.datastructures;

import gov.nasa.jpl.icmd.schema.InsertUplinkRequestType;
import gov.nasa.jpl.icmd.schema.RequestIdListTypeMin1;
import jpl.gds.shared.time.IAccurateDateTime;

/**
 * An ecapsulating class for the UPLINK_REQ_ADDED, UPLINK_REQ_UPDATED, and
 * UPLINK_REQ_DELETED message types from CPD.
 *
 * @since AMPCS R7.1
 * @see jpl.gds.tc.api.icmd.datastructures.CpdDmsBroadcastStatusMessages
 * MPCS-5934
 */
public class CpdIncrementalRequestStatus {

	final private StatusType type;
	final private Object status;

	/*
	 * MPCS-7355 - Josh Choi - 6/1/2015: This field is being added specifically
	 * for the UPLINK_REQ_DELETED messages, because they don't carry the
	 * UplinkRequest object which includes the time of the status change. For
	 * UPLINK_REQ_DELETED, the creator of this object should manually provide
	 * the timestamp.
	 */
    private IAccurateDateTime timestamp;

	/**
	 * Default constructor.
	 *
	 * @param type indicate which of the 3 {@link StatusType} this new status is
	 * @param status actual status object (from CPD schema)
	 */
	public CpdIncrementalRequestStatus(final StatusType type, final Object status) {
		assert(type != null);
		assert(status != null);
		this.type = type;
		this.status = status;
	}

	/**
	 * Return the type of status.
	 *
	 * @return status type
	 */
	public StatusType getType() {
		return this.type;
	}

	/**
	 * Gets the status item as a UPLINK_REQ_ADDED type. Caller should check this
	 * object's type by calling {@link #getType()} beforehand to make sure that
	 * it's the right type.
	 *
	 * @return status item as UPLINK_REQ_ADDED type
	 */
	public InsertUplinkRequestType getAsUplinkReqAdded() {
		return (InsertUplinkRequestType) this.status;
	}

	/**
	 * Gets the status item as a UPLINK_REQ_UPDATED type. Caller should check
	 * this object's type by calling {@link #getType()} beforehand to make sure
	 * that it's the right type.
	 *
	 * @return status item as UPLINK_REQ_UPDATED type
	 */
	public InsertUplinkRequestType getAsUplinkReqUpdated() {
		return (InsertUplinkRequestType) this.status;
	}

	/**
	 * Gets the status item as a UPLINK_REQ_DELETED type. Caller should check
	 * this object's type by calling {@link #getType()} beforehand to make sure
	 * that it's the right type.
	 *
	 * @return status item as UPLINK_REQ_DELETED type
	 */
	public RequestIdListTypeMin1 getAsUplinkReqDeleted() {
		return (RequestIdListTypeMin1) this.status;
	}

	/**
	 * Returns the timestamp. Note: Applies to UPLINK_REQ_DELETED types only.
	 *
	 * @return the timestamp
	 */
    public IAccurateDateTime getTimestamp() {
		return timestamp;
	}

	/**
	 * Sets the timestamp. Note: Applies to UPLINK_REQ_DELETED types only.
	 *
	 * @param timestamp
	 *            the timestamp to set
	 */
    public void setTimestamp(final IAccurateDateTime timestamp) {
		this.timestamp = timestamp;
	}

	/**
	 * Enumeration to differentiate between UPLINK_REQ_ADDED,
	 * UPLINK_REQ_UPDATED, and UPLINK_REQ_DELETED types.
	 *
	 * @since AMPCS R7.1
	 * MPCS-5934
	 */
	public static enum StatusType {
		ADDED, UPDATED, DELETED
	}
}
