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

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.shared.cli.options.numeric.UnsignedIntOption;

/**
 * A command line option class for downlink VCID. Entered value must be a
 * defined VCID for the current mission.
 * 
 * @since R8
 *
 */
@SuppressWarnings("serial")
public class VcidOption extends UnsignedIntOption {
    
	/**
	 * Long option name.
	 */
	public static final String LONG_OPTION = "vcid";

	/**
	 * Description
	 */
	public static final String DESCRIPTION = "input virtual channel ID";
	
	/**
     * Constructor that include short option.
     * 
	 * @param shortOpt short option name
     * 
     * @param missionProps
     *            current MissionProperties objects to get valid VCIDs from
     * @param required
     *            true if the option is required, false if not
     */
    public VcidOption(final String shortOpt, final MissionProperties missionProps, final boolean required) {
        super(shortOpt, LONG_OPTION, "id", DESCRIPTION, required);
        setParser(new VcidOptionParser(missionProps));
    }

    
	/**
	 * Constructor that omits short option.
	 * 
	 * @param missionProps
	 *            current MissionProperties objects to get valid VCIDs from
	 * @param required
	 *            true if the option is required, false if not
	 */
    public VcidOption(final MissionProperties missionProps, final boolean required) {
        this(null, missionProps, required);
	}


}
