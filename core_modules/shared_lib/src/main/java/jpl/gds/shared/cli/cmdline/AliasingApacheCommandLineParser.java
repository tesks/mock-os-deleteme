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
package jpl.gds.shared.cli.cmdline;

import java.util.Collection;

import org.apache.commons.cli.ParseException;

import jpl.gds.shared.cli.options.ICommandLineOption;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;

/**
 * A ICommandLineParser implementation that extends the Apache DefaultParser to
 * add aliasing capability, which allows aliases for options to be used on the
 * command line. The alias is replaced with the current option name by the parsing
 * step. Note that the methods here will not function if the defined Options
 * object contains Apache Option objects.  It must contain only instances of
 * CommandLineObject, which is an AMPCS class that extends the Apache class.
 * 
 * 
 */
public class AliasingApacheCommandLineParser extends ApacheCommandLineParser {
    private static final Tracer log = TraceManager.getDefaultTracer();

    /**
     * @{inheritDoc}
     * @see jpl.gds.shared.cli.cmdline.ApacheCommandLineParser#parse(jpl.gds.shared.cli.cmdline.OptionSet, java.lang.String[])
     */
    @Override
    public ICommandLine parse(final OptionSet opts, final String[] args) throws ParseException {
        
        final String[] newArgs = dealias(opts, args);
        return super.parse(opts, newArgs);
    }

    /**
     * Replaces any aliases found on the command line with the official option names.
     * @param opts the OptionsSet object containing defined ICommandLineOption
     * objects.
     * @param opts the OptionSet containing all command line options
     * @param inputArgs the list of command line objects
     * @return dealiased argument array
     */
    public String[] dealias(final OptionSet opts, final String[] inputArgs) {
        final String[] result = new String[inputArgs.length];
        System.arraycopy(inputArgs, 0, result, 0, inputArgs.length);
        
        final Collection<ICommandLineOption<?>> allOptions = opts.getAllOptions();
        for (int i = 0; i < result.length; i++) {
            String toCheck = result[i];
            final boolean longOpt = toCheck.startsWith("--");
            final boolean shortOpt = !longOpt && toCheck.startsWith("-");
            
            if (longOpt) {
                toCheck = toCheck.substring(2);
            } else if (shortOpt) {
                toCheck = toCheck.substring(1);
            }
            for (final ICommandLineOption<?> copt: allOptions) {
                
                if ((longOpt || shortOpt) && copt.isAlias(toCheck)) {
                    log.debug("BaseCommandOptions is replacing alias ", result[i], " with option ", copt.getLongOpt());
                    result[i] = "--" + copt.getLongOpt();
                }
            }
        }
        return result;
        
    }
}
