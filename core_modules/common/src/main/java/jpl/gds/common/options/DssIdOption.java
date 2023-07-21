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
 * A command line option class for DSS (station) ID.
 * 
 *
 * @since R8
 *
 */
@SuppressWarnings("serial")
public class DssIdOption extends UnsignedIntOption {

	/**
	 * Long option name.
	 */
	public static final String LONG_OPTION = "dssId";
	/**
	 * Description.
	 */
	public static final String DESCRIPTION = "station identifier";

	/**
	 * Constructor.
	 * 
	 * @param missionProps
	 *            the current MissionProperties object, used to get valid
	 *            station IDs for the current mission
	 * @param required
	 *            true if the option is required, false or not
	 */
	public DssIdOption(final MissionProperties missionProps, final boolean required) {
	    super(null, LONG_OPTION, "id", DESCRIPTION, required);
	    setParser(new DssIdOptionParser(missionProps));
	}
	

}
