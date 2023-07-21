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

import java.util.List;
import java.util.Set;


/**
 * Level 2 validation functor for values in a set.
 *
 * @param <T> Parameter set enum
 * @param <U> Parameter type
 *
 */
public final class Level2SetFunctor<T extends Enum<T>, U>
    extends AbstractParameterFunctor<T>
    implements AbstractGenericParameter.Level2Functor<T>
{
    private final Class<U> _valueClass;
    private final Set<U>   _set;


    /**
     * Constructor.
     *
     * @param valueClass Class of U
     * @param set        Set of U
     *
     * @throws ParameterException On any error
     */
    public Level2SetFunctor(final Class<U> valueClass,
                            final Set<U>   set)
        throws ParameterException
    {
        super();

        _valueClass = checkNull("Level2SetFunctor",
                                valueClass,
                                "Value class");
        _set        = checkNull("Level2SetFunctor",
                                set,
                                "Set");
    }
    /**
     * Validate at level 2.
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

        final U value = valueObject.getValue(_valueClass);

        if (! _set.contains(value))
        {
            final boolean quote = CharSequence.class.isInstance(value);

            errors.add("Option "                  +
                       pa.getDisplayName()        +
                       " "                        +
                       (quote ? "'" : "")         +
                       value                      +
                       (quote ? "'" : "")         +
                       " is not allowed for the " +
                       state.getMission()         +
                       " mission"                 +
                       appendChoices(_set, ChoiceTypeEnum.MISSION));
        }
    }
}
