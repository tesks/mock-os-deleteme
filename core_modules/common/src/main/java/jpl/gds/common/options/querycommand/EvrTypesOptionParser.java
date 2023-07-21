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
 * A command line option parser for the "--evrTypes" option.
 * 
 *
 * @since R8
 */
public class EvrTypesOptionParser extends AbstractOptionParser<EvrTypeSelect> {
    
    private static final String EVR_TYPES_REGEX = "[frs]+";

    /**
     * @{inheritDoc}
     * @see jpl.gds.shared.cli.cmdline.IOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine, jpl.gds.shared.cli.options.ICommandLineOption)
     */
    @Override
    public EvrTypeSelect parse(final ICommandLine commandLine, final ICommandLineOption<EvrTypeSelect> opt) throws ParseException {
        final String value = getValue(commandLine, opt);
        if (value == null) {
            return new EvrTypeSelect("frs");
        }
        if (!value.matches(EVR_TYPES_REGEX)) {
            throw new ParseException(THE_VALUE + opt.getLongOpt() + " must contain only the letters f, r, and s");
        }
        return new EvrTypeSelect(value);
    }

}
