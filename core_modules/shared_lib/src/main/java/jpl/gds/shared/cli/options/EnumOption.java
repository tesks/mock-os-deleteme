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

import java.util.Arrays;
import java.util.List;

/**
 * A generic command line option class for an option that takes an enum value as
 * argument.
 * 
 * @param <U>
 *            the enum class
 */
public class EnumOption<U extends Enum<U>> extends CommandLineOption<U> {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor for a validating enum option. The enum type of the option is
     * always validated. Validation adds an additional check for a specific
     * subset of the enum values.
     * 
     * @param enumClass
     *            the enum class this option is for
     * @param shortOpt
     *            the short option name (letter); may be null
     * @param longOpt
     *            the long option name; may be null only if shortOpt is not
     * @param argName
     *            the name of the argument for help text
     * @param description
     *            the description of the option for help text
     * @param required
     *            true if the option must always be present on the command line
     * @param restrictionValues
     *            List of enum values the argument value should be restricted to
     */
    public EnumOption(final Class<U> enumClass, final String shortOpt,
            final String longOpt, final String argName,
            final String description, final boolean required,
            final List<U> restrictionValues) {
        super(shortOpt, longOpt, true, argName, description + ": [" +
                (restrictionValues == null ? listAllValues(enumClass) : listValues(restrictionValues)) + "]", 
                required, new EnumOptionParser<U>(enumClass, restrictionValues));
    }

    /**
     * Constructor for a non-validating enum option. The enum type of the option
     * is always validated, but there is no validation beyond that.
     * 
     * @param enumClass
     *            the enum class this option is for
     * @param shortOpt
     *            the short option name (letter); may be null
     * @param longOpt
     *            the long option name; may be null only if shortOpt is not
     * @param argName
     *            the name of the argument for help text
     * @param description
     *            the description of the option for help text
     * @param required
     *            true if the option must always be present on the command line
     */
    public EnumOption(final Class<U> enumClass, final String shortOpt,
            final String longOpt, final String argName,
            final String description, final boolean required) {
        this(enumClass, shortOpt, longOpt, argName, description, required,
                null);
    }
    
    /**
     * Note this method overrides the interface return type to EnumOptionParser.
     * 
     * @{inheritDoc}
     * @see jpl.gds.shared.cli.options.ICommandLineOption#getParser()
     */
    @Override
    public EnumOptionParser<U> getParser() {
        return (EnumOptionParser<U>)parser;
    }
    
    /**
     * Returns a comma-separated string of all the values the supplied enum class.
     * Values with embedded spaces will be enclosed in single quotes.
     * 
     * @param enumClass the enum class to list values for
     * @return string listing all the values in CSV format
     * 
     */
    public static String listAllValues(final Class<? extends Enum<?>> enumClass) {
        final Enum<?>[] vals = enumClass.getEnumConstants();
        return listValues((Arrays.asList(vals)));
       
    }

    /**
     * Returns a comma-separated string of all the values the supplied enum list.
     * Values with embedded spaces will be enclosed in single quotes.
     * 
     * @param values the list of enum values
     * @return string listing all the values in CSV format
     * 
     */
    public static String listValues(final List<? extends Object> values) {
        

        final StringBuilder sb = new StringBuilder();
        int i = 0;
        for (final Object val : values) {
            if (val.toString().contains(" ")) {
                sb.append("'" + val + "'");
            } else {
                sb.append(val);
            }
            if (i++ != values.size() - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }
}
