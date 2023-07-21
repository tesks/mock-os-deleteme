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
package jpl.gds.shared.cli.options.time;

import jpl.gds.shared.cli.options.EnumOption;
import jpl.gds.shared.time.SclkFineFormat;
import jpl.gds.shared.time.TimeProperties;

/**
 * A command line option class that allows the entry of a format option for the
 * fine portion of SCLK, as a ScklFineFormat value. Defaults based upon a
 * supplied TimeProperties.
 * 
 * @since R8
 */
public class SclkFineFormatOption extends EnumOption<SclkFineFormat> {

	private static final long serialVersionUID = 1L;
	
	private static final String SCLK_FORMAT_SHORT_VALUE = "s";
	private static final String SCLK_FORMAT_LONG_VALUE = "sclkFormat";

	/**
	 * Constructor.
	 * 
	 * @param timeConfig
	 *            the TimeProperties instance from which to get default value
	 * @param required
	 *            true if the option is required, false if not
	 */
	public SclkFineFormatOption(TimeProperties timeConfig, boolean required) {
		super(SclkFineFormat.class, SCLK_FORMAT_SHORT_VALUE,
				SCLK_FORMAT_LONG_VALUE, "format",
				"The format used for the fine portion of SCLK values", required);
		setDefaultValue(timeConfig.useFractionalSclkFormat() ? SclkFineFormat.SUBSECONDS
				: SclkFineFormat.TICKS);
	}

}
