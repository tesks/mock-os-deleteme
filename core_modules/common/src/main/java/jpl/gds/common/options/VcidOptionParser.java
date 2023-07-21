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
package jpl.gds.common.options;

import java.util.LinkedList;
import java.util.List;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.shared.cli.options.ListCheckingUnsignedIntOptionParser;
import jpl.gds.shared.types.UnsignedInteger;

/**
 * An option parser class for any downlink VCID option. The value is checked to
 * ensure it is on the list of configured downlink VCIDs for the mission.
 * 
 *
 * @since R8
 */
public class VcidOptionParser extends ListCheckingUnsignedIntOptionParser {

	/**
	 * Constructor.
	 * 
	 * @param missionProps
	 *            the current MissionProperties object to get valid VCIDs from
	 */
	public VcidOptionParser(MissionProperties missionProps) {
		super();
		final List<UnsignedInteger> restrictedVals = new LinkedList<UnsignedInteger>();
		for (final Integer vcid : missionProps.getAllDownlinkVcids()) {
			restrictedVals.add(UnsignedInteger.valueOf(vcid));
		}
		setRestrictionList(restrictedVals);
	}
}
