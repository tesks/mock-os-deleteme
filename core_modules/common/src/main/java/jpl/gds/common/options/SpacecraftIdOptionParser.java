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
 * Option parser class for the SPACECRAFT_ID option. Will validate that the
 * parsed value is among the spacecraft IDs configured for the current
 * mission. A default value is established. 
 * 
 */
public class SpacecraftIdOptionParser extends
            ListCheckingUnsignedIntOptionParser {

	/**
	 * Constructor.
	 * 
	 * @param missionProps
	 *            the MissionProperties object to get default and valid values
	 *            from
	 */
    public SpacecraftIdOptionParser(MissionProperties missionProps) {
        super();

        if (missionProps.getDefaultScid() != MissionProperties.UNKNOWN_ID) {
            setDefaultValue(UnsignedInteger.valueOf(missionProps.getDefaultScid()));
        } else {
            setDefaultValue(UnsignedInteger.valueOf(0));
        }
        final List<UnsignedInteger> restrictedVals = new LinkedList<UnsignedInteger>();
        for (final Integer scid : missionProps.getAllScids()) {
            if (scid >= 0) {
                restrictedVals.add(UnsignedInteger.valueOf(scid));
            }
        }
        setRestrictionList(restrictedVals);
    }
}
