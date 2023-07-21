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
import jpl.gds.common.filtering.DssIdFilter;
import jpl.gds.shared.cli.options.CommandLineOption;
import jpl.gds.shared.holders.StationIdHolder;
import jpl.gds.shared.types.UnsignedInteger;

/**
 * Creates an option for a  list of DSS IDs to be used for data or message filtering.  
 * 
 *
 */
public class DssIdListOption extends CommandLineOption<DssIdFilter> {

	private static final long serialVersionUID = -1737757802006553242L;
	
	/** 
	 * Long option name. Used by default if the constructor does not supply option name.
	 */
	public static final String LONG_OPTION = "filterDssIds";
	
	private static final String NO_NONE_ARG = "id[,id...]";
	private static final String NONE_ARG = "id,[id|NONE...]";
	
	private static final String NO_NONE_DESC = "A comma-separated list of station IDs to filter for";
    private static final String NONE_DESC        = "A comma-separated list of station IDs to filter for; may include NONE to accept messages/data with no defined station or a station ID of "
            + StationIdHolder.UNSPECIFIED_VALUE;
	
	/**
	 * Constructor for use when the caller wants to define the option name and description.
	 * 
     * @param opt
     *            the short option name (letter); may be null
     * @param longOpt
     *            the long option name; may be null only if opt is not
     * @param argName
     *            the name of the option's argument for help text
     * @param description
     *            the description of the option for help text
     * @param allowNone
     *      allow NONE as a station ID entry
     * @param required
     *            true if the option is always required on the command line
     * @param mprops MissionProperties mission properties object containing 
     *        valid station IDs; may be null           
     */
    public DssIdListOption(final String opt, final String longOpt, final String argName, final String description,
            final boolean required, final boolean allowNone, final MissionProperties mprops) {
        super(opt, (longOpt == null ? LONG_OPTION : longOpt), true, argName, description, required, new DssIdListOptionParser(allowNone,
                getValidStations(mprops)));
    }

	/**
	 * Constructor for use when the caller wants default option name and description.
	 * 
	 * @param allowNone
	 * 		allow NONE as a station ID entry
     * @param required
     *            true if the option is always required on the command line
     * @param mprops MissionProperties mission properties object containing 
     *        valid station IDs; may be null           
	 */
	public DssIdListOption(final boolean required, final boolean allowNone, final MissionProperties mprops) {
		super(null, LONG_OPTION, true, (allowNone ? NONE_ARG : NO_NONE_ARG), (allowNone ? NONE_DESC : NO_NONE_DESC), required, new DssIdListOptionParser(allowNone,
		        getValidStations(mprops)));
	}
	
	private static List<UnsignedInteger> getValidStations(final MissionProperties mprops) {
	    if (mprops == null) {
	        return null; 
	    }
	    final Integer[] stations = mprops.getStationMapper().getStationIds();
	    final List<UnsignedInteger> result = new LinkedList<>();
	    for (final Integer i: stations) {
	        result.add(UnsignedInteger.valueOf(i));
	    }
	    return result;
	}
}
