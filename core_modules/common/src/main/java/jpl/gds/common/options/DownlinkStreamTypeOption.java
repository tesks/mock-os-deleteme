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

import jpl.gds.common.config.types.DownlinkStreamType;
import jpl.gds.shared.cli.options.EnumOption;

/**
 * A command option class for a DownlinkStreamType enumerated value.
 * 
 *
 * @since R8
 */
@SuppressWarnings("serial")
public class DownlinkStreamTypeOption extends EnumOption<DownlinkStreamType> {

	/**
	 * Long option name.
	 */
	public static final String LONG_OPTION = "downlinkStreamId";
	/**
	 * Short option name.
	 */
	public static final String SHORT_OPTION = "E";
	/**
	 * Description.
	 */
	public static final String DESCRIPTION = "downlink stream ID for TESTBED or ATLO; defaults based upon venue type";
	
	/**
	 * Constructor.
	 * 
	 * @param restrictTo
	 *            a list of DownlinkStreamTypes to restrict the argument value
	 * @param isRequired
	 *            true if the option is required, false if not
	 */
	public DownlinkStreamTypeOption(List<DownlinkStreamType> restrictTo,
			boolean isRequired) {
		super(
				DownlinkStreamType.class,
				SHORT_OPTION,
				LONG_OPTION,
				"stream",
				DESCRIPTION,
				isRequired, restrictTo);
		setParser(new DownlinkStreamTypeOptionParser(restrictTo));
	}

	/**
	 * Constructor.
	 * 
	 * @param isRequired
	 *            true if the option is required, false if not
	 */
	public DownlinkStreamTypeOption(boolean isRequired) {
		this(null, isRequired);
	}

}
