/*
 * Copyright 2006-2019. California Institute of Technology.
 *  ALL RIGHTS RESERVED.
 *  U.S. Government sponsorship acknowledged.
 *
 *  This software is subject to U. S. export control laws and
 *  regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 *  extent that the software is subject to U.S. export control laws
 *  and regulations, the recipient has the responsibility to obtain
 *  export licenses or other export authority as may be required
 *  before exporting such information to foreign countries or
 *  providing access to foreign nationals.
 */
package jpl.gds.cfdp.common.commandline;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.util.Arrays;
import java.util.List;

/**
 * Class CfdpCommandLineUtil
 *
 */
public class CfdpCommandLineUtil {

    public static void divideArgs(final CommandLineParser parser, final Options options, String[] args,
                             final List<String> optionArgs, final List<String> nonOptionArgs) throws ParseException {
        CommandLine commandLine;
        int leftOverArgsLength;

        do {
            final int currentArgsLength = args.length;
            commandLine = parser.parse(options, args, true);
            leftOverArgsLength = commandLine.getArgs().length;
            final int numArgsConsumed = currentArgsLength - leftOverArgsLength;

            for (int i = 0; i < numArgsConsumed; i++) {
                optionArgs.add(args[i]);
            }

            if (leftOverArgsLength > 0) {
                nonOptionArgs.add(commandLine.getArgs()[0]);
                args = Arrays.copyOfRange(commandLine.getArgs(), 1, leftOverArgsLength);
            }

        } while (leftOverArgsLength > 0);

    }

}
