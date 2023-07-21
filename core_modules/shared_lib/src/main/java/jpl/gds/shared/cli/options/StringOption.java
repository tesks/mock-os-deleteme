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

import java.util.List;

/**
 * This class represents a ICommandLineOption whose value is a String. Optionally,
 * can verify that the argument value is in a specified list of allowed values.
 * 
 */
public class StringOption extends CommandLineOption<String> {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor for a non-validating string option. 
     * 
     * @param shortOpt
     *            short option name; may be null
     * @param longOpt
     *            long option name; may only be null if shortOpt is not
     * @param argName
     *            the name of the argument for help text; may not be null
     * @param description
     *            description of the argument for help text
     * @param required
     *            true if the argument must always be present on the command
     *            line
     */
    public StringOption(final String shortOpt, final String longOpt,
            final String argName,
            final String description, final boolean required) {
        super(shortOpt, longOpt, true, argName, description, required, new StringOptionParser());
    }
    
    /**
     * Constructor for a validating string option. 
     * 
     * @param shortOpt
     *            short option name; may be null
     * @param longOpt
     *            long option name; may only be null if shortOpt is not
     * @param argName
     *            the name of the argument for help text; may not be null
     * @param description
     *            description of the argument for help text
     * @param required
     *            true if the argument must always be present on the command
     *            line
     * @param restrictionValues List of valid String values           
     */
    public StringOption(final String shortOpt, final String longOpt,
            final String argName,
            final String description, final boolean required,
            List<String> restrictionValues) {
        super(shortOpt, longOpt, true, argName, description, required, 
                new StringOptionParser(restrictionValues));
    }
    
    /**
     * Note this method overrides the interface return type to
     * StringOptionParser.
     * 
     * @{inheritDoc
     * @see jpl.gds.shared.cli.options.ICommandLineOption#getParser()
     */
    @Override
    public StringOptionParser getParser() {
        return (StringOptionParser) parser;
    }
}
