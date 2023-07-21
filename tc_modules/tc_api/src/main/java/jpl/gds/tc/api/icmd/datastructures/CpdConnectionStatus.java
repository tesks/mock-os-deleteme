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

import gov.nasa.jpl.icmd.schema.ConnectionStatusResponseMessageType;
import gov.nasa.jpl.icmd.schema.DSSIdRange;
import gov.nasa.jpl.icmd.schema.MdcConnectionStatusType;
import gov.nasa.jpl.icmd.schema.ResponseStatus;
import gov.nasa.jpl.icmd.schema.StatusTypeAndStatusValueType;
import jpl.gds.tc.api.icmd.CpdResponse;

/**
 * This class wraps a CPD response to a request for connection status.
 *
 * @since AMPCS R5
 */
public class CpdConnectionStatus extends CpdResponse {
	/** The CPD connection status */
	private final MdcConnectionStatusType connectionStatus;

	private final String connectedStation;

	/**
	 * Constructor that takes in ConnectionStatusResponseMessageType
	 *
	 * @param connectionStatus the CPD connection status response
	 */
	public CpdConnectionStatus(
			ConnectionStatusResponseMessageType connectionStatus) {
		super(connectionStatus.getRESPONSE());
		this.connectionStatus = connectionStatus.getCONNECTIONSTATUS();
		this.connectedStation = connectionStatus.getDSSID();
	}

	/*
	 * MPCS-5934 - Josh Choi - 4/27/2015: With CPD long polling, the connection
	 * status no longer is encapsulated in CONNECTION_STATUS_RESPONSE, but
	 * instead in STATUS_TYPE_AND_STATUS_VALUE. We need to be able to handle
	 * both.
	 */
	/**
	 *
	 * Constructor that takes in StatusTypeAndStatusValueType
	 *
	 * @param connectionStatus the CPD connection status
	 */
	public CpdConnectionStatus(StatusTypeAndStatusValueType connectionStatus) {
		super(new ResponseStatus());
		this.connectionStatus = connectionStatus.getCONNECTIONSTATUS();
		DSSIdRange dssIdRange = connectionStatus.getDSSID();
		this.connectedStation = dssIdRange == null ? null : dssIdRange.value();
	}

	/**
	 * Indicates whether or not CPD is connected to a station
	 *
	 * @return true if CPD is connected to a station, false otherwise
	 */
	public boolean isConnected() {
		return this.connectionStatus.equals(MdcConnectionStatusType.CONNECTED);
	}

	/**
	 * Indicates whether or not CPD is waiting for a connection from a station
	 *
	 * @return true if CPD is waiting for a connection from a station (in
	 *         PENDING state), false otherwise
	 */
	public boolean isPending() {
		return this.connectionStatus.equals(MdcConnectionStatusType.PENDING);
	}

	/**
	 * Indicates whether or not CPD is waiting to terminate connection from a station
	 *
	 * @return true if CPD is waiting for a disconnection from a station (in
	 *         TERMINATING state), false otherwise
	 */
	public boolean isTerminating() {
		return this.connectionStatus.equals(MdcConnectionStatusType.TERMINATING);
	}

	/**
	 * Get the ID of the station CPD is connected to
	 *
	 * @return the ID of the station CPD is connected to, or null if CPD is not
	 *         connected to a station
	 */
	public String getConnectedStationId() {
		if (this.isConnected()) {
			return this.connectedStation;
		} else {
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return this.connectionStatus.toString();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see jpl.gds.tc.impl.icmd.datastructures.CpdResponse#toKeyValueCsv()
	 */
	@Override
	public String toKeyValueCsv() {
		StringBuilder builder = new StringBuilder(super.toKeyValueCsv());

		builder.append(",");
		builder.append("connection_status");
		builder.append("=");
		builder.append(this.toString());

		if (this.isConnected()) {
			builder.append(",");
			builder.append("connected_station");
			builder.append("=");
			builder.append(this.getConnectedStationId());
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
				+ ((connectedStation == null) ? 0 : connectedStation.hashCode());
		result = prime
				* result
				+ ((connectionStatus == null) ? 0 : connectionStatus.hashCode());
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

		CpdConnectionStatus other = (CpdConnectionStatus) obj;

		if (connectedStation == null) {
			if (other.connectedStation != null) {
				return false;
			}
		} else if (!connectedStation.equals(other.connectedStation)) {
			return false;
		}

		if (connectionStatus != other.connectionStatus) {
			return false;
		}

		return true;
	}

}
