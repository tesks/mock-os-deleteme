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
package jpl.gds.common.options.querycommand;

import java.util.Collection;
import java.util.List;

import jpl.gds.common.config.types.DownlinkStreamType;
import jpl.gds.shared.cli.options.CommandLineOption;

/**
 * A command option class for a Collection of DownlinkStreamType enumerated values.
 * 
 *
 * @since R8
 */
@SuppressWarnings("serial")
public class DownlinkStreamTypeListOption extends CommandLineOption<Collection<DownlinkStreamType>> {

	/**
	 * Long option name.
	 */
	public static final String LONG_OPTION = "downlinkStreamId";
	/**
	 * Short option name.
	 */
	public static final String SHORT_OPTION = "E";	
	/**
	 * Option argument name.
	 */
	public static final String ARG_NAME = "stream";
	
	/**
	 *  The description of the option for help text.
	 */
	public static final String DESCRIPTION = "downlink stream ID for TESTBED or ATLO:"
											+"'Selected DL',LV,TZ,'Command Echo'"
                                            + "Multiple values may be supplied in a comma-separated value (CSV) format.";
	
	/*
	 * @param sort
	 * 		true to sort the resulting collection based on the natural ordering - the enum declaration order for DownlinkStreamType.
	 * @param removeDuplicates
	 * 		true to remove any duplicates from the resulting collection.
     * @param required
     *            true if the option is always required on the command line
     * @param valid list of valid values 
	 */
	public DownlinkStreamTypeListOption(final boolean sort, final boolean removeDuplicates, final boolean required, final List<DownlinkStreamType> valid) {
		super(SHORT_OPTION, LONG_OPTION, true, ARG_NAME, DESCRIPTION, required, new DownlinkStreamTypeListOptionParser(sort, removeDuplicates, valid));
		
	}

}
