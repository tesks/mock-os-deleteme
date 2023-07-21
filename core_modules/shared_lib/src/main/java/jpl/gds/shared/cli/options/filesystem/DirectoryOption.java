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
package jpl.gds.shared.cli.options.filesystem;

import jpl.gds.shared.cli.options.CommandLineOption;

/**
 * A class that represents a command line option whose value is a directory
 * name. Optionally, the directory can be validated and/or created.
 * 
 *
 */
public class DirectoryOption extends CommandLineOption<String> {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a validating or non-validating directory option.
     * 
     * @param opt
     *            the short option name (letter); may be null
     * @param longOpt
     *            the long option name; may be null only if opt is not
     * @param argName
     *            the name of the option's argument for help text
     * @param description
     *            the description of the option for help text
     * @param required
     *            true if the option is always required on the command line
     * @param validate
     *            true to have the option parser validate the directory's
     *            existence
     */
    public DirectoryOption(final String opt, final String longOpt,
            final String argName, final String description,
            final boolean required, boolean validate) {

        this(opt, longOpt, argName, description, required, validate, false);
    }

    /**
     * Creates a validating or non-validating directory option that can
     * optionally create the directory if it does not exist.
     * 
     * @param opt
     *            the short option name (letter); may be null
     * @param longOpt
     *            the long option name; may be null only if opt is not
     * @param argName
     *            the name of the option's argument for help text
     * @param description
     *            the description of the option for help text
     * @param required
     *            true if the option is always required on the command line
     * @param validate
     *            true to have the option parser validate the directory
     * @param create
     *            true to have the option parser create the directory if it does
     *            not exist
     */
    public DirectoryOption(final String opt, final String longOpt,
            final String argName, final String description,
            final boolean required, boolean validate, boolean create) {

        super(opt, longOpt, true, argName, description, required,
                create ? new DirectoryCreatingOptionParser(validate)
                         : new DirectoryOptionParser(validate));
    }

}
