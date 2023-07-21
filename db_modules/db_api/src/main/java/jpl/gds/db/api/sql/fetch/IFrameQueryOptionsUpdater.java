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

public interface IFrameQueryOptionsUpdater extends IFrameQueryOptionsProvider {

	/**
	 * Set frame type.
	 *
	 * @param frameType Frame type
	 */
	void setFrameType(String frameType);

	/**
	 * Set VCID.
	 *
	 * @param vcid VCIDs
	 */
	void setVcid(Set<Integer> vcid);

	/**
	 * Set VCID.
	 *
	 * @param vcid VCID
	 */
	void setVcid(int vcid);

	/**
	 * Set DSS id.
	 *
	 * @param dss DSS id
	 */
	void setDss(Set<Integer> dss);

	/**
	 * Set relay id.
	 *
	 * @param relayId Relay id
	 */
	void setRelayId(Long relayId);

	/**
	 * Set id.
	 *
	 * @param id Id
	 */
	void setFrameId(Long id);

	/**
	 * Set "good" status.
	 *
	 * @param good Good status
	 */
	void setGood(Boolean good);

	/**
	 * Set VCFC ranges.
	 *
	 * @param vcfcs VCFC ranges
	 */
	void setVcfcs(VcfcRanges vcfcs);

}