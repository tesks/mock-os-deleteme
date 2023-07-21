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

import org.apache.commons.cli.ParseException;

import jpl.gds.common.config.types.DownlinkStreamType;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.EnumOptionParser;
import jpl.gds.shared.cli.options.ICommandLineOption;

/**
 * Option parser class for command line options that take a DownlinkStreamType
 * enumeration value.
 * 
 *
 * @since R8
 */
public class DownlinkStreamTypeOptionParser extends
		EnumOptionParser<DownlinkStreamType> {

	/**
	 * Constructor.
	 * 
	 * @param restrictTo
	 *            list of stream types to restrict the argument value
	 */
	public DownlinkStreamTypeOptionParser(final List<DownlinkStreamType> restrictTo) {
		super(DownlinkStreamType.class, restrictTo);
	}

	/**
	 * @{inheritDoc
	 * @see jpl.gds.shared.cli.options.EnumOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine,
	 *      jpl.gds.shared.cli.options.ICommandLineOption)
	 */
	@Override
	public DownlinkStreamType parse(final ICommandLine commandLine,
			final ICommandLineOption<DownlinkStreamType> opt) throws ParseException {

		final String rawVal = getValue(commandLine, opt);
		if (rawVal == null) {
			return null;
		}

		DownlinkStreamType streamId = null;

		try {
			streamId = DownlinkStreamType.convert(rawVal);
		} catch (final IllegalArgumentException e) {
			throw new ParseException("Invalid value for the --"
					+ opt.getLongOpt() + " option (" + rawVal + ")");
		}

		return streamId;

	}

}
