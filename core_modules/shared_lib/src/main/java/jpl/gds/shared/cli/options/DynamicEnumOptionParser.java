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

import jpl.gds.shared.cli.cmdline.ICommandLine;
import jpl.gds.shared.config.DynamicEnum;

import java.util.List;

import org.apache.commons.cli.ParseException;

import static jpl.gds.shared.config.DynamicEnum.valueOf;

/**
 * A generic command line option parser for dynamic enum CommandLineOptions. Optionally
 * validates that the option is one of a subset of the enum values.
 *
 * @since 8.3
 * @see  jpl.gds.shared.cli.options.EnumOptionParser
 *
 * @param <U> the enum class
 */
public class DynamicEnumOptionParser<U extends DynamicEnum<U>> extends AbstractListCheckingOptionParser<U> {

	/**
	 * Constant that may be used to represent an UNKNOWN enum value.
	 */
    public static final String UNKNOWN = "UNKNOWN";

    private final Class<U> enumClass;
    private boolean convertToUpperCase;
    private boolean allowUnknown = true;

    /**
     * Constructor for a non-validating enum option parser. The type of the
     * option is always validated.
     *
     * @param theClass
     *            the enum class of the option
     */
    public DynamicEnumOptionParser(Class<U> theClass) {
        super();
        enumClass = theClass;
    }

    /**
     * Constructor for a validating enum option parser. The type of the option
     * is always validated. This extends the validation to restrict the input
     * arguments to a subset of the total list of enum values.
     *
     * @param theClass
     *            the enum class of the option
     * @param restrictionValues
     *            a subset of the enum values for validation
     */
    public DynamicEnumOptionParser(final Class<U> theClass, final List<U> restrictionValues) {
        super(restrictionValues);
        enumClass = theClass;
    }

    /**
     * Sets the flag indicating if the input value should be converted to upper case.
     *
     * @param convert true to upper-case argument value, false to not
     */
    public void setConvertToUpperCase(final boolean convert) {
        convertToUpperCase = convert;
    }

    /**
     * Sets the flag indicating whether an UNKNOWN enum value is supported
     * by the current enum class and can be entered on the command line.
     *
     * @param allow true to allow UNKNOWN, false to not
     */
    public void setAllowUnknown(final boolean allow) {
        allowUnknown = allow;
    }

    @Override
    public U parse(final ICommandLine commandLine, final ICommandLineOption<U> opt)
            throws ParseException {
        
        String value = getValue(commandLine, opt);
        
        if (value == null) {
            return null;          
        }

        if (convertToUpperCase) {
            value = value.toUpperCase();
        }
        
        if (!allowUnknown && value.equalsIgnoreCase(UNKNOWN)) {
            throw new ParseException(THE_OPTION + opt.getLongOpt() + " cannot be set to " + value);
        }
        
        U result = null;

        try {
            result = valueOf(enumClass, value);
        } catch (final IllegalArgumentException e) {
            throw new ParseException(THE_VALUE
                    + opt.getLongOpt() + " is not one of the accepted values: " +
                    DynamicEnumOption.listAllValues(enumClass));
        }

        checkValueInList(opt, result, true);
        
        return result;
    }
 
}
