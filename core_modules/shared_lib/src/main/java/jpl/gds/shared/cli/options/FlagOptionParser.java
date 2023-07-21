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
package jpl.gds.shared.cli.options;

import org.apache.commons.cli.ParseException;

import jpl.gds.shared.cli.cmdline.ICommandLine;

/**
 * A parser for a command line option that is a boolean flag, i.e, is
 * either present or not present but takes no argument value.
 * 
 *
 */
public class FlagOptionParser extends AbstractOptionParser<Boolean> {

    /**
     * @{inheritDoc}
     * @see jpl.gds.shared.cli.cmdline.IOptionParser#parse(jpl.gds.shared.cli.cmdline.ICommandLine, jpl.gds.shared.cli.options.ICommandLineOption)
     */
    @Override
    public Boolean parse(ICommandLine commandLine, ICommandLineOption<Boolean> opt)
            throws ParseException {
        return isPresent(commandLine, opt);
    }

}
