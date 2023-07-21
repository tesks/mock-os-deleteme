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

import java.util.List;

import jpl.gds.common.config.types.VenueType;
import jpl.gds.shared.cli.options.EnumOption;

/**
 * A command line option class for a VenueTyp enumeration value.
 * 
 *
 * @since R8
 *
 */
@SuppressWarnings("serial")
public class VenueTypeOption extends EnumOption<VenueType> {

	/**
	 * Long option name.
	 */
	public static final String LONG_OPTION = "venueType";
	/**
	 * Short option name.
	 */
	public static final String SHORT_OPTION = "V";
	/**
	 * Description.
	 */
	public static final String DESCRIPTION = "operational or test venue to use";
	
	/**
	 * Constructor.
	 * 
	 * @param restrictTo
	 *            list of venue types to restrict the argument value to
	 * @param isRequired
	 *            true if the option is required, false if not
	 */
	public VenueTypeOption(List<VenueType> restrictTo, boolean isRequired) {
		super(VenueType.class, SHORT_OPTION, LONG_OPTION, "venue",
				DESCRIPTION, isRequired, restrictTo);
	}

	/**
	 * Constructor.
	 * 
	 * @param isRequired
	 *            true if the option is required, false if not
	 */
	public VenueTypeOption(boolean isRequired) {
		this(null, isRequired);
	}
}
