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
 * A command line option parser for the "--packetTypes" option.
 * 
 *
 * @since R8
 */
public class PacketTypesOptionParser extends AbstractOptionParser<PacketTypeSelect> {

	private static final String PACKET_TYPES_REGEX = "(f|s){1}"; //accepts only one of either f or s
	
	/**
     * @{inheritDoc}
     * @see jpl.gds.shared.cli.cmdline.IOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine, jpl.gds.shared.cli.options.ICommandLineOption)
     */
    @Override
    public PacketTypeSelect parse(final ICommandLine commandLine, final ICommandLineOption<PacketTypeSelect> opt) throws ParseException {
        String value = getValue(commandLine, opt);
        if (value == null) {
            return new PacketTypeSelect("f");
        }
        value = value.toLowerCase();
        if (!value.matches(PACKET_TYPES_REGEX)) {
            throw new ParseException(THE_VALUE + opt.getLongOpt() + " must contain only one of the letters f or s, and not both.");
        }
        return new PacketTypeSelect(value);
    }
}
