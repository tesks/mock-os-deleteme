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
import jpl.gds.common.config.mission.StationMapper;
import jpl.gds.shared.cli.options.ListCheckingUnsignedIntOptionParser;
import jpl.gds.shared.types.UnsignedInteger;

/**
 * An option parser class for any DSS ID option. The value is checked
 * to ensure it is on the list of configured downlink station IDs for the
 * mission. 
 * 
 *
 * @since R8
 *
 */
public class DssIdOptionParser extends ListCheckingUnsignedIntOptionParser {

	/**
	 * Constructor.
	 * 
	 * @param missionProps
	 *            the current MissionProperties object, used to get valid
	 *            station IDs for the current mission
	 */
    public DssIdOptionParser(MissionProperties missionProps) {
        super();
        final List<UnsignedInteger> restrictedVals = new LinkedList<UnsignedInteger>();
        final StationMapper stationMap = missionProps.getStationMapper();
        for (final Integer id : stationMap.getStationIdsAsSet()) {
            restrictedVals.add(UnsignedInteger.valueOf(id));
        }
        setRestrictionList(restrictedVals);
    }
}
