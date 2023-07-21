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
import jpl.gds.shared.time.DateTimeFormat;
import jpl.gds.shared.time.TimeProperties;

/**
 * A command line option class that allows the user to enter a format specifier
 * (a DateTimeFormat value) for an IAccurateDateTime. Defaults based upon a
 * supplied TimeProperties.
 * 
 *
 * @since R8
 */
public class AccurateDateTimeFormatOption extends EnumOption<DateTimeFormat> {

	private static final long serialVersionUID = 1L;
	private static final String SCET_FORMAT_SHORT_VALUE = "t";
	private static final String SCET_FORMAT_LONG_VALUE = "scetFormat";

	/**
	 * Constructor.
	 * 
	 * @param timeConfig
	 *            the TimeProperties instance to use for defaulting the
	 *            option
	 * @param required
	 *            true if the option is required on the command line, false if
	 *            not
	 * @param timeType
	 *            a text string indicating to the user what kind of time tag is
	 *            expected (SCET, ERT, etc).
	 */
	public AccurateDateTimeFormatOption(TimeProperties timeConfig,
			boolean required, String timeType) {
		super(DateTimeFormat.class, SCET_FORMAT_SHORT_VALUE,
				SCET_FORMAT_LONG_VALUE, "format", "The format used for "
						+ timeType + " values", required);
		setDefaultValue(timeConfig.useDoyOutputFormat() ? DateTimeFormat.DOY
				: DateTimeFormat.ISO);
	}

}
