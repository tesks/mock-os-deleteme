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

import jpl.gds.shared.string.StringUtil;


/**
 * Level 1 validation functor for strings.
 *
 * @param <T> Parameter set enum type
 *
 */
public final class Level1StringFunctor<T extends Enum<T>>
    extends AbstractParameterFunctor<T>
    implements AbstractGenericParameter.Level1Functor<T>
{

    private final Set<Character> _valid;
    private final String         _description;


    /**
     * Constructor.
     *
     * @param valid       Set of valid characters or null
     * @param description Description of valid characters or null
     */
    public Level1StringFunctor(final Set<Character> valid,
                               final String         description)
    {
        super();

        _valid       = (((valid != null) && ! valid.isEmpty())
                            ? valid
                            : null);
        _description = StringUtil.safeTrim(description);
    }


    /**
     * Constructor.
     */
    public Level1StringFunctor()
    {
        this(null, null);
    }


    /**
     * Validate string at level 1.
     * If the length is wrong, continue and check the characters. But bail
     * after finding a single bad character.
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

        final Constraints constraints = pa.getConstraints();
        final String      value       =
            valueObject.getValueAsString(constraints.getUppercase());
        final int         length      = value.length();
        boolean           haveErrors  = false;

        if (! constraints.checkBounds(value))
        {
            errors.add("Option "                         +
                       pa.getDisplayName()               +
                       " '"                              +
                       value                             +
                       "' of length "                    +
                       length                            +
                       " does not lie in length range [" +
                       constraints.getMinimum()          +
                       ","                               +
                       constraints.getMaximum()          +
                       "]");

            haveErrors = true;
        }

        if (_valid != null)
        {
            for (int i = 0; i < length; ++i)
            {
                final char c = value.charAt(i);

                if (! _valid.contains(c))
                {
                    errors.add("Option "                              +
                               pa.getDisplayName()                    +
                               " contains invalid character '"        +
                               c                                      +
                               "'(0x"                                 +
                               Integer.toHexString(c)                 +
                               ") at offset "                         +
                               i                                      +
                               (! _description.isEmpty() ? " (" : "") +
                               _description                           +
                               (! _description.isEmpty() ? ")" : ""));


                    haveErrors = true;

                    break;
                }
            }
        }

        if (! haveErrors)
        {
            valueObject.setValue(value);
        }
    }
}
