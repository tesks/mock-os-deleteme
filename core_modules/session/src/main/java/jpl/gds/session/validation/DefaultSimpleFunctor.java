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

import jpl.gds.session.config.SessionConfiguration;


/**
 * Default functor that just sets to a value if not already set. The
 * secondary validity set allows the default to be set on a subset of the
 * valid modes of the parameter, i.e., just for certain applications. Any
 * members of the secondary set that are not also in the primary set will have
 * no effect, so it is OK to just use FULL_VALIDITY_SET if you do not need
 * to differentiate modes.
 *
 *
 * @param <T> Parameter set enum type
 * @param <U> Value type
 */
public final class DefaultSimpleFunctor<T extends Enum<T>, U>
    extends AbstractParameterFunctor<T>
    implements AbstractGenericParameter.DefaultFunctor<T>
{
    private static final String ME = "DefaultSimpleFunctor";

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
    public DefaultSimpleFunctor(final T             parameter,
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
     * Determine default for parameter.
     *
     * @param sc     Session configuration
     * @param map    Map of parameter attributes
     * @param state  State
     * @param errors List to be populated with errors
     *
     * @throws ParameterException If there is a value
     */
    @Override
    public void constructDefault(final SessionConfiguration           sc,
                                 final Map<T, ParameterAttributes<T>> map,
                                 final State                          state,
                                 final List<String>                   errors)
        throws ParameterException
    {

        final Value valueObject = map.get(_parameter).getValue();

        mustNotHaveValue(valueObject);

        if (! _secondary.contains(state.getMode()))
        {
            return;
        }

        valueObject.setValue(_value);
        valueObject.setDefaulted();

        LOG.trace(_parameter + " defaulted to: " + _value);
    }
}
