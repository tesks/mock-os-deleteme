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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

/**
 * This is the Apache commons-cli implementation of the ICommandLine interface.
 * 
 *
 */
public class ApacheCommandLine implements ICommandLine {

    private final CommandLine innerCommandLine;
    
    /**
     * Constructor
     * 
     * @param cl The Apache CommandLine object wrapped by this instance
     */
    protected ApacheCommandLine(final CommandLine cl) {
        innerCommandLine = cl;
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.shared.cli.cmdline.ICommandLine#hasOption(java.lang.String)
     */
    @Override
    public boolean hasOption(final String opt) {
        return innerCommandLine.hasOption(opt);
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.shared.cli.cmdline.ICommandLine#getOptionValue(java.lang.String)
     */
    @Override
    public String getOptionValue(final String opt) {
        return innerCommandLine.getOptionValue(opt);
    }
    
    /**
     * {@inheritDoc}
     * @see jpl.gds.shared.cli.cmdline.ICommandLine#getTrailingArguments()
     */
    @Override
    public String[] getTrailingArguments() {
        return innerCommandLine.getArgs();
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public Map<String, String> getAllOptions() {
        final Map<String, String> optionMap = new HashMap<>();
        for (final Option o : innerCommandLine.getOptions()) {
            if (o.getLongOpt() != null && (o.getLongOpt().contains("Pwd") || o.getLongOpt().contains("pwd"))) {
                optionMap.put(o.getLongOpt(), "XXX");
            }
            else if (o.getLongOpt() != null) {
                optionMap.put(o.getLongOpt(), (o.getValue() == null ? "true" : o.getValue()));
            }
            else { // no long option exists?
                optionMap.put(o.getOpt(), (o.getValue() == null ? "true" : o.getValue()));
            }
        }
        return optionMap;
    }
}
