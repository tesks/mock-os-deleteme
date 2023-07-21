/*
 * Copyright 2006-2019. California Institute of Technology.
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

import jpl.gds.shared.config.DynamicEnum;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *  A generic command line option class for an option that takes an dynamic enum value as
 *  argument.
 *
 * @since 8.3
 * @see jpl.gds.shared.cli.options.EnumOption
 *
 * @param <U> the enum class
 *
 */
public class DynamicEnumOption<U extends DynamicEnum<U>> extends CommandLineOption<U>{
    private static final long serialVersionUID = 1L;
    private static final String SPACE = " ";
    private static final String UNKNOWN = "UNKNOWN";

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
    public DynamicEnumOption(final Class<U> enumClass, final String shortOpt,
                      final String longOpt, final String argName,
                      final String description, final boolean required,
                      final List<U> restrictionValues) {
        super(shortOpt, longOpt, true, argName, description + ": [" +
                      (restrictionValues == null ? listAllValues(enumClass) : listValues(restrictionValues)) + "]",
              required, new DynamicEnumOptionParser<U>(enumClass, restrictionValues));
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
    public DynamicEnumOption(final Class<U> enumClass, final String shortOpt,
                      final String longOpt, final String argName,
                      final String description, final boolean required) {
        this(enumClass, shortOpt, longOpt, argName, description, required,
             null);
    }

    /**
     * Note this method overrides the interface return type to DynamicEnumOptionParser.
     *
     */
    @Override
    public DynamicEnumOptionParser<U> getParser() {
        return (DynamicEnumOptionParser<U>)parser;
    }

    /**
     * Returns a comma-separated string of all the values the supplied enum class.
     * Values with embedded spaces will be enclosed in single quotes.
     *
     * @param enumClass the enum class to list values for
     * @return string listing all the values in CSV format
     *
     */
    public static String listAllValues(final Class<? extends DynamicEnum<?>> enumClass) {
        final DynamicEnum<?>[] vals = DynamicEnum.values(enumClass);
        return listValues((Arrays.asList(vals)));
    }

    /**
     * Returns a comma-separated string of all the values the supplied enum list.
     * Values with embedded spaces will be enclosed in single quotes, UNKNOWN is ignored
     *
     * @param values the list of enum values
     * @return string listing all the values in CSV format
     *
     */
    public static String listValues(final List<? extends Object> values) {
        //transform to list of string
        final List<String> strings = new ArrayList<>();
        for(Object val: values){
            final String str = val.toString();
            if(str.equals(UNKNOWN)) {
                continue;
            }

            if(str.contains(SPACE)) {
                strings.add("'" + val + "'");
            }
            else{
                strings.add(str);
            }
        }

        //transform to CSV
        final String[] strArr = new String[strings.size()];
        return String.join(", ", strings.toArray(strArr));
    }
}
