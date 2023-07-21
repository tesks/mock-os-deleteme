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

/**
 * The class represents a command line option that is a boolean flag, i.e, is
 * either present or not present but takes no argument value.
 * 
 *
 */
public class FlagOption extends CommandLineOption<Boolean> {

    private static final long serialVersionUID = 1L;
    
    /**
     * Added this constructor. It seems to me,
     * that by defintion, a FlagOption cannot be required since it's purpose is
     * to flag a condition if it is present. If it is required, then it can
     * never be the case that this condition is false. Creates a flag option.
     * 
     * @param opt
     *            short option name (letter); may be null
     * @param longOpt
     *            long option name; may be null only if opt is not
     * @param description
     *            description of the option for help text
     */
    public FlagOption(final String opt, final String longOpt, final String description) {
        super(opt, longOpt, false, null, description, false, new FlagOptionParser());
    }

    /**
     * Creates a flag option.
     * 
     * @param opt
     *            short option name (letter); may be null
     * @param longOpt
     *            long option name; may be null only if opt is not
     * @param description
     *            description of the option for help text
     * @param required
     *            true if the option is always required on the command line
     */
    public FlagOption(final String opt, final String longOpt,
            final String description, final boolean required) {
        super(opt, longOpt, false, null, description, required, new FlagOptionParser());
    }
}
