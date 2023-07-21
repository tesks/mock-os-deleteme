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
package jpl.gds.session.validation;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * Level 1 validation functor for Enum.
 *
 * @param <T> Parameter set enum type
 * @param <U> Parameter enum type
 *
 * Accept command line enum values as strings with spaces
 *
 */
public final class Level1EnumFunctor<T extends Enum<T>, U extends Enum<U>>
    extends AbstractParameterFunctor<T>
    implements AbstractGenericParameter.Level1Functor<T>
{
    private static final String ME = "Level1EnumFunctor: ";

    private final Class<U>         _enumClass;
    private final List<String>     _choices;
    private final AllowUnknownBool _allowUnknown;
    private final UppercaseBool    _doUppercase;


    /**
     * Constructor.
     *
     * @param enumClass    Class of U
     * @param allowUnknown True if UNKNOWN is allowed
     * @param doUppercase  True if value should be uppercased
     *
     * @throws ParameterException On any error
     */
    public Level1EnumFunctor(final Class<U>         enumClass,
                             final AllowUnknownBool allowUnknown,
                             final UppercaseBool    doUppercase)
        throws ParameterException
    {
        super();

        _enumClass    = checkNull(ME, enumClass,    "Enum class");
        _allowUnknown = checkNull(ME, allowUnknown, "Allow UNKNOWN");
        _doUppercase  = checkNull(ME, doUppercase,  "Do uppercase");
        _choices      = Collections.unmodifiableList(
                            purgeUnknownFromChoices(
                                Arrays.asList(_enumClass.getEnumConstants()),
                                _allowUnknown));
    }


    /**
     * Validate enum type at level 1.
     *
     * @param pa     Parameter attribute
     * @param state  State
     * @param errors List to be populated with errors
     *
     * @throws ParameterException On any error
     */
    @Override
    public void validate(final ParameterAttributes<T> pa,
                         final State                  state,
                         final List<String>           errors)
        throws ParameterException
    {
        final Value valueObject = pa.getValue();

        mustHaveValue(valueObject);


		/**
         *  Accept command line enum values as strings with spaces
         * 
         *   Added the function .replaceAll(" +",  "_") to the string parsing.
         *
         *   Some missions needs to be able to input downlinkStreamId command line input
         *   as strings with spaces (eg: "Selected DL"). In order for Enum.valueOf(_enumClass, value)
         *   to return a valid enum value and NOT throw an exception the String value it is
         *   passed must match one of the enum values. downlinkStreamId does have a convert
         *   function that accepts a string (that can have spaces) and will return the appropriate
         *   enum value, but not every potential enum input will have this function.
         *   Therefore, the best option, currently, is to properly format the string before
         *   calling the Enum.valueOf function. A side effect is that all enums will also now
         *   have this functionality added to them when they are input on the command line.
         */

        final String value = valueObject.getValueAsString(_doUppercase).replaceAll(" +", "_");

        U enumConstant = null;

        try
        {
            enumConstant = Enum.valueOf(_enumClass, value);

            if (! _allowUnknown.get())
            {
                // Disallow UNKNOWN even if it is valid for the enum
                disallowUnknown(value);
            }
        }
        catch (final IllegalArgumentException iae)
        {
            errors.add("Option "                           +
                       pa.getDisplayName()                 +
                       " "                                 +
                       value                               +
                       " does not match any allowed value" +
                       appendChoices(_choices, ChoiceTypeEnum.MULTI_MISSION));
            return;
        }

        valueObject.setValue(enumConstant);
    }
}
