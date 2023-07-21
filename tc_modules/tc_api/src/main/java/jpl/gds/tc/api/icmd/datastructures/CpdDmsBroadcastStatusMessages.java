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

import java.util.List;

import gov.nasa.jpl.icmd.schema.BitRateAndModIndexType;
import gov.nasa.jpl.icmd.schema.UplinkRequest;
import jpl.gds.tc.api.ICpdUplinkStatus;

/**
 * This data structure holds the parsed values of individual messages
 * encapsulated in the DMS broadcast status messages poll response.
 *
 * @since AMPCS R7.1
 * @see jpl.gds.tc.impl.icmd.CpdClient
 * MPCS-5934
 */
public class CpdDmsBroadcastStatusMessages {

	final private List<ICpdUplinkStatus> radiationList;
	final private List<UplinkRequest> radiationRequests;
	final private List<CpdIncrementalRequestStatus> incrementalRequestStatusList;
	final private CpdConfiguration configuration;
	final private CpdConnectionStatus connectionState;
	final private BitRateAndModIndexType bitRateModIndex;

	/**
	 * Constructs a CpdDmsBroadcastStatusMessages object using all required
	 * fields.
	 *
	 * @param radiationList Radiation list
	 * @param radiationRequests Radiation requests in list form
	 * @param incrementalRequestStatusList
	 * @param configuration CPD configuration
	 * @param connectionState Connection status
	 * @param bitRateModIindex Bit-rate and mod-index
	 */
	public CpdDmsBroadcastStatusMessages(final List<ICpdUplinkStatus> radiationList,
			final List<UplinkRequest> radiationRequests,
			final List<CpdIncrementalRequestStatus> incrementalRequestStatusList,
			final CpdConfiguration configuration,
			final CpdConnectionStatus connectionState,
			final BitRateAndModIndexType bitRateModIindex) {
		super();
		this.radiationList = radiationList;
		this.radiationRequests = radiationRequests;
		this.incrementalRequestStatusList = incrementalRequestStatusList;
		this.configuration = configuration;
		this.connectionState = connectionState;
		this.bitRateModIndex = bitRateModIindex;
	}

	/**
	 * Returns the radiation list. Note that, with the new CPD long polling,
	 * this method may return null if the long poll doesn't contain any
	 * RADIATION_LIST. So the caller should always check for a possible null.
	 *
	 * Also, the list object can be shared across different objects and threads,
	 * so any modification operations should be done on a copy of the list
	 * returned here.
	 *
	 * @return the radiationList
	 */
	public List<ICpdUplinkStatus> getRadiationList() {
		return radiationList;
	}

	/**
	 * Returns the radiation requests. Note that, with the new CPD long polling,
	 * this method may return null if the long poll doesn't contain any
	 * RADIATION_LIST. So the caller should always check for a possible null.
	 *
	 * Also, the list object can be shared across different objects and threads,
	 * so any modification operations should be done on a copy of the list
	 * returned here.
	 *
	 * @return the radiationRequests
	 */
	public List<UplinkRequest> getRadiationRequests() {
		return radiationRequests;
	}

	/**
	 * Returns the list of incremental request statuses. Never returns null, but
	 * the list may be empty.
	 *
	 * Also, the list object can be shared across different objects and threads,
	 * so any modification operations should be done on a copy of the list
	 * returned here.

	 * @return the incrementalRequestStatusList
	 */
	public List<CpdIncrementalRequestStatus> getIncrementalRequestStatusList() {
		return incrementalRequestStatusList;
	}

	/**
	 * Returns the CPD configuration. Null if no such message included.
	 *
	 * @return the configuration
	 */
	public CpdConfiguration getConfiguration() {
		return configuration;
	}

	/**
	 * Returns the connection state. Null if no such message included.
	 *
	 * @return the connectionState
	 */
	public CpdConnectionStatus getConnectionState() {
		return connectionState;
	}

	/**
	 * Returns the bit-rate and mod-index. Null if no such message included.
	 *
	 * @return the bitRateModIindex
	 */
	public BitRateAndModIndexType getBitRateModIndex() {
		return bitRateModIndex;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "CpdDmsBroadcastStatusMessages[config: " + this.configuration
				+ ", connState: " + this.connectionState + ", radList: "
				+ this.radiationList + ", radReqs: " + this.radiationRequests
				+ ", incReqs: " + this.incrementalRequestStatusList
				+ ", br/mi: " + this.bitRateModIndex + "]";
	}

}
