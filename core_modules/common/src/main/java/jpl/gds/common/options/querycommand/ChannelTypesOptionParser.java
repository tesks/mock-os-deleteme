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

import org.apache.commons.cli.ParseException;
import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.cli.options.AbstractOptionParser;
import jpl.gds.shared.cli.options.ICommandLineOption;

/**
 * A command line option parser for the "--channelTypes" option.
 * 
 *
 * @since R8
 */
public class ChannelTypesOptionParser  extends AbstractOptionParser<ChannelTypeSelect> {
	
	/* Accepts one or more of each letter without restrictions */
	private static final String CHANNEL_TYPES_REGEX = "[" + ChannelTypesOption.ALL_CHANNEL_TYPES + "]+";
	
	/**
     * @{inheritDoc}
     * @see jpl.gds.shared.cli.cmdline.IOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine, jpl.gds.shared.cli.options.ICommandLineOption)
     */
    @Override
    public ChannelTypeSelect parse(final ICommandLine commandLine, final ICommandLineOption<ChannelTypeSelect> opt) throws ParseException {
        String value = getValue(commandLine, opt);
        if (value == null) {
            return new ChannelTypeSelect(ChannelTypesOption.ALL_CHANNEL_TYPES);
        }
        value = value.toLowerCase();
        if (!value.matches(CHANNEL_TYPES_REGEX)) {
            throw new ParseException(THE_VALUE + opt.getLongOpt() + " must contain only the letters f,r,h,m,s, or g");
        }
        return new ChannelTypeSelect(value);
    }
}
