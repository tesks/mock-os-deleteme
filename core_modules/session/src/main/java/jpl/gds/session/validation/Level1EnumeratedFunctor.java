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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import jpl.gds.shared.types.EnumeratedType;


/**
 * Level 1 validation functor for enumerated types (not Enum).
 *
 * @param <T> Parameter set enum type
 * @param <U> Parameter enumerated type
 *
 */
public final class Level1EnumeratedFunctor<T extends Enum<T>,
                                           U extends EnumeratedType>
    extends AbstractParameterFunctor<T>
    implements AbstractGenericParameter.Level1Functor<T>
{
    private static final String ME = "Level1EnumeratedFunctor: ";

    private static final String CHOICES_FIELD = "types";

    private final Constructor<U>   _enumConstructor;
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
    public Level1EnumeratedFunctor(final Class<U>         enumClass,
                                   final AllowUnknownBool allowUnknown,
                                   final UppercaseBool    doUppercase)
        throws ParameterException
    {
        super();

        _allowUnknown = checkNull(ME, allowUnknown, "Allow UNKNOWN");
        _doUppercase  = checkNull(ME, doUppercase,  "Do uppercase");

        final Class<U> uClass = checkNull(ME, enumClass, "Enumerated class");

        try
        {
            _enumConstructor = uClass.getConstructor(String.class);
        }
        catch (final NoSuchMethodException nsme)
        {
            throw new ParameterException(ME + "Unable to get constructor",
                                         nsme);
        }

        _enumConstructor.setAccessible(true);

        Field enumField = null;

        try
        {
            enumField = uClass.getField(CHOICES_FIELD);
        }
        catch (final NoSuchFieldException nsfe)
        {
            throw new ParameterException(ME + "Unable to get enums", nsfe);
        }

        enumField.setAccessible(true);

        try
        {
            _choices =
                Collections.unmodifiableList(
                    purgeUnknownFromChoices(
                        Arrays.asList(
                            String[].class.cast(enumField.get(null))),
                        _allowUnknown));
        }
        catch (final IllegalAccessException iae)
        {
            throw new ParameterException(ME + "Unable to get enums", iae);
        }
        catch (final IllegalArgumentException iare)
        {
            throw new ParameterException(ME + "Unable to get enums", iare);
        }
        catch (final ClassCastException cce)
        {
            throw new ParameterException(ME + "Unable to get enums", cce);
        }
    }


    /**
     * Validate enumerated type at level 1.
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

        final String value = valueObject.getValueAsString(_doUppercase);

        U enumConstant = null;

        try
        {
            enumConstant = _enumConstructor.newInstance(value);

            if (! _allowUnknown.get())
            {
                // Disallow UNKNOWN even if it is valid for the enum

                disallowUnknownReflected(value);
            }
        }
        catch (final InstantiationException ie)
        {
            throw new ParameterException(ME + "Unable to get enum", ie);
        }
        catch (final IllegalAccessException iae)
        {
            throw new ParameterException(ME + "Unable to get enum", iae);
        }
        catch (final IllegalArgumentException iare)
        {
            throw new ParameterException(ME + "Unable to get enum", iare);
        }
        catch (final InvocationTargetException ite)
        {
            final Throwable cause = ite.getCause();

            if (! IllegalArgumentException.class.isInstance(cause))
            {
                // IllegalArgumentException means that the string does
                // not match any enumerated constant. But anything else
                // is a real problem.

                throw new ParameterException(ME + "Unable to get enum",
                                             cause);
            }

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
