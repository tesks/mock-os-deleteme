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
import java.util.Map;
import java.util.Set;


/**
 * Special functor for the simple case where the value is to be always set
 * if not already set. The secondary validity set allows the value to be set
 * on a subset of the possible modes of the parameter, i.e., just for certain
 * applications. Remember that special functors are called even for invalid
 * modes, so the secondary set should normally contain members not in the
 * primary set.
 *
 *
 * @param <T> Parameter set enum type
 * @param <U> Value type
 */
public final class SpecialSimpleFunctor<T extends Enum<T>, U>
    extends AbstractParameterFunctor<T>
    implements AbstractGenericParameter.SpecialFunctor<T>
{
    private static final String ME = "SpecialSimpleFunctor";

    private final T             _parameter;
    private final U             _value;
    private final Set<ModeEnum> _secondary;


    /**
     * Constructor.
     *
     * @param parameter Parameter to be defaulted
     * @param value     Default value
     * @param secondary Secondary validity set
     *
     * @throws ParameterException On any error
     */
    public SpecialSimpleFunctor(final T             parameter,
                                final U             value,
                                final Set<ModeEnum> secondary)
        throws ParameterException
    {
        super();

        _parameter = checkNull(ME, parameter, "Parameter enum");
        _value     = checkNull(ME, value,     "Default value");
        _secondary = checkNull(ME, secondary, "Secondary validity set");
    }


    /**
     * Do special processing for parameter.
     *
     * @param map    Map of parameter attributes
     * @param state  State
     * @param errors List to be populated with errors
     *
     * @throws ParameterException On any error
     */
    @Override
    public void specialProcessing(final Map<T, ParameterAttributes<T>> map,
                                  final State                          state,
                                  final List<String>                   errors)
            throws ParameterException
    {
        if (! _secondary.contains(state.getMode()))
        {
            return;
        }

        final Value valueObject = map.get(_parameter).getValue();

        if (valueObject.hasValue())
        {
            return;
        }

        valueObject.setValue(_value);
        valueObject.setDefaulted();

        LOG.trace(_parameter + " specially defaulted to: " + _value);
    }
}
