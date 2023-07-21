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
package jpl.gds.db.api.sql.fetch;

import java.util.Set;

public interface IFrameQueryOptionsProvider {

	/**
	 * Get frame type.
	 *
	 * @return String
	 */
	String getFrameType();

	/**
	 * Get VCIDs.
	 *
	 * @return Set<Integer>
	 */
	Set<Integer> getVcid();

	/**
	 * Get DSS ids.
	 *
	 * @return Set of DSS ids
	 */
	Set<Integer> getDss();

	/**
	 * Get relay id.
	 *
	 * @return Long
	 */
	Long getRelayId();

	/**
	 * Get id.
	 *
	 * @return Long
	 */
	Long getFrameId();

	/**
	 * Get "good" status.
	 *
	 * @return Boolean
	 */
	Boolean getGood();

	/**
	 * Get VCFC ranges.
	 *
	 * @return VcfcRanges
	 */
	VcfcRanges getVcfcs();

}