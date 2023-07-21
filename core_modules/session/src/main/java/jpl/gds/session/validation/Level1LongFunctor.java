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


/**
 * Level 1 validation functor for longs.
 *
 * @param <T> Parameter set enum type
 *
 */
public final class Level1LongFunctor<T extends Enum<T>>
    extends AbstractParameterFunctor<T>
    implements AbstractGenericParameter.Level1Functor<T>
{
    /**
     * Constructor.
     */
    public Level1LongFunctor()
    {
        super();
    }


    /**
     * Validate long at level 1.
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

        final String svalue = valueObject.getValueAsString(
                                  UppercaseBool.DO_NOT_UPPERCASE);
        long         value  = 0L;

        try
        {
            value = Long.parseLong(svalue);
        }
        catch (final NumberFormatException nfe)
        {
            errors.add("Option "           +
                       pa.getDisplayName() +
                       " '"                +
                       svalue              +
                       "' is not parseable as an integer");
            return;
        }

        final Constraints constraints = pa.getConstraints();

        if (! constraints.checkBounds(value))
        {
            errors.add("Option "                  +
                       pa.getDisplayName()        +
                       " "                        +
                       value                      +
                       " does not lie in range [" +
                       constraints.getMinimum()   +
                       ","                        +
                       constraints.getMaximum()   +
                       "]");
            return;
        }

        valueObject.setValue(value);
    }
}
