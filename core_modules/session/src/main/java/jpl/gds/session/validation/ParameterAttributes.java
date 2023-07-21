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

import java.util.Set;

import jpl.gds.shared.string.StringUtil;


/**
 * Holds all attributes for parameters. Doesn't do much but hold stuff.
 *
 * There are four different kinds of parameters possible. First, let's define
 * some terminology. An "option" is the standard CLI "dash-dash" option name.
 * A "trailing option" is for values that occur at the end of the command-line
 * without any name. Both kinds are called "parameters". An "argument" is the
 * value that can be associated with either kind of parameter.
 *
 * The most common kind is the option name followed by its argument. This kind
 * must have at least a level 1 functor to get the parameter value.
 *
 * A less common kind of parameter is the boolean parameter where the presence
 * of the parameter on the command-line means true and its absence false. This
 * kind cannot have a level 1 or 2 or default functor because its value (which
 * must be boolean) is determined from the initial parsing of the command-line.
 *
 * It is allowed to have a parameter without an option name. This is called a
 * "ghost" parameter and is used when there is a need for a parameter that
 * goes along with the other parameters but that is not to be specified by a
 * user. It cannot have a level 1 or 2 functor (because there is nothing for
 * them to do) but must have a default functor. The default functor supplies
 * the value if it needs to do so. There is no place to put an argument. A
 * ghost parameter can be of any value class.
 *
 * The final kind also has no option name but is supplied by the user at the
 * end of the command-line. These are the trailing options. There can be more
 * than one of these if you want. This kind must have at least a level 1
 * functor to process the value.
 *
 * There are four available constructors, one for each kind.
 *
 *
 * @param <T> Enum of parameters
 */
public final class ParameterAttributes<T extends Enum<T>>
    extends AbstractGenericParameter<T>
{
    private static final String ME = "ParameterAttributes: ";

    private final T                     _index;
    private final String                _optionName;
    private final String                _displayName;
    private final Class<?>              _valueClass;
    private final Set<ModeEnum>         _validity;
    private final Set<ModeEnum>         _required;
    private final Value                 _value = new Value();
    private final Constraints           _constraints;
    private final Level1Functor<T>      _level1Functor;
    private final Level2Functor<T>      _level2Functor;
    private final DefaultFunctor<T>     _defaultFunctor;
    private final Level3Functor<T>      _level3Functor;
    private final SpecialFunctor<T>     _specialFunctor;
    private final AssignmentFunctor<T>  _assignmentFunctor;
    private final TrailingParameterBool _trailingParameter;
    private final NeedsArgumentBool     _needsArgument;


    /**
     * Full constructor.
     *
     * @param index             Enum of this parameter
     * @param optionName        Long option name
     * @param displayName       Display name
     * @param valueClass        Value class
     * @param constraints       Constraints
     * @param validity          Set of valid ModeEnum
     * @param required          Set of ModeEnum where parameter is required
     * @param level1Functor     Level 1 functor
     * @param level2Functor     Level 2 functor
     * @param defaultFunctor    Default functor
     * @param level3Functor     Level 3 functor
     * @param specialFunctor    Special functor
     * @param assignmentFunctor Assignment functor
     * @param trailingParameter True for trailing non-option processing
     * @param needsArgument     True if needs argument
     *
     * @throws ParameterException On inconsistent values
     */
    private ParameterAttributes(final T                     index,
                                final String                optionName,
                                final String                displayName,
                                final Class<?>              valueClass,
                                final Constraints           constraints,
                                final Set<ModeEnum>         validity,
                                final Set<ModeEnum>         required,
                                final Level1Functor<T>      level1Functor,
                                final Level2Functor<T>      level2Functor,
                                final DefaultFunctor<T>     defaultFunctor,
                                final Level3Functor<T>      level3Functor,
                                final SpecialFunctor<T>     specialFunctor,
                                final AssignmentFunctor<T>  assignmentFunctor,
                                final TrailingParameterBool trailingParameter,
                                final NeedsArgumentBool     needsArgument)
        throws ParameterException
    {
        super();

        _index             = checkNull(ME, index, "Index");
        _optionName        = StringUtil.emptyAsNull(optionName);
        _valueClass        = checkNull(ME, valueClass, "Value class");
        _constraints       = checkNull(ME, constraints, "Constraints");
        _validity          = checkNull(ME, validity, "Validity set");
        _required          = checkNull(ME, required, "Required set");
        _level1Functor     = level1Functor;
        _level2Functor     = level2Functor;
        _defaultFunctor    = defaultFunctor;
        _level3Functor     = level3Functor;
        _specialFunctor    = specialFunctor;
        _assignmentFunctor = assignmentFunctor;
        _trailingParameter = checkNull(ME,
                                       trailingParameter,
                                       "Trailing parameter");
        _needsArgument     = checkNull(ME, needsArgument, "Needs argument");

        final boolean hasName    = (_optionName     != null);
        final boolean has1       = (_level1Functor  != null);
        final boolean has2       = (_level2Functor  != null);
        final boolean hasDefault = (_defaultFunctor != null);

        // Need a display name always, so make one up if need be

        String dn = StringUtil.emptyAsNull(displayName);

        if (dn == null)
        {
            if (hasName)
            {
                dn = ("--" + _optionName);
            }
            else
            {
                dn = _index.toString();
            }
        }

        _displayName = dn;

        // Check for legal states

        if (! _trailingParameter.get())
        {
            if (hasName)
            {
                if (_needsArgument.get())
                {
                    // Standard case

                    if (! has1)
                    {
                        throw new ParameterException(ME                     +
                                                     "Must have a level 1 " +
                                                     "functor for named "   +
                                                     "options with argument");
                    }
                }
                else if (has1 || has2 || hasDefault)
                {
                    // Pure boolean option

                    throw new ParameterException(ME                       +
                                                 "Cannot have a level 1 " +
                                                 "or 2 or default "       +
                                                 "functor for named "     +
                                                 "options without an "    +
                                                 "argument");
                }
                else if (! _valueClass.equals(Boolean.class))
                {
                    throw new ParameterException(ME                      +
                                                 "Must have boolean "    +
                                                 "value type for named " +
                                                 "options without an "   +
                                                 "argument");
                }
            }
            else if (! _needsArgument.get())
            {
                // Unnamed option

                if (has1 || has2 || ! hasDefault)
                {
                    throw new ParameterException(ME                       +
                                                 "Cannot have a level 1 " +
                                                 "or 2 functor but must " +
                                                 "have a default "        +
                                                 "functor for unnamed "   +
                                                 "options");
                }
            }
            else
            {
                throw new ParameterException(ME                             +
                                             "Unnamed options cannot have " +
                                             "an argument");
            }
        }
        else if (! hasName && _needsArgument.get())
        {
            // Trailing parameter

            if (! has1)
            {
                throw new ParameterException(ME                      +
                                             "Must have a level 1 "  +
                                             "functor for trailing " +
                                             "options");
            }
        }
        else
        {
            throw new ParameterException(ME                            +
                                         "Must have an argument "      +
                                         "and cannot have a name for " +
                                         "trailing options");
        }
    }


    /**
     * Constructor for option with name and argument.
     *
     * @param index             Enum of this parameter
     * @param optionName        Long option name
     * @param displayName       Display name
     * @param valueClass        Value class
     * @param constraints       Constraints
     * @param validity          Set of valid ModeEnum
     * @param required          Set of ModeEnum where parameter is required
     * @param level1Functor     Level 1 functor
     * @param level2Functor     Level 2 functor
     * @param defaultFunctor    Default functor
     * @param level3Functor     Level 3 functor
     * @param specialFunctor    Special functor
     * @param assignmentFunctor Assignment functor
     *
     * @throws ParameterException On inconsistent values
     */
    public ParameterAttributes(final T                    index,
                               final String               optionName,
                               final String               displayName,
                               final Class<?>             valueClass,
                               final Constraints          constraints,
                               final Set<ModeEnum>        validity,
                               final Set<ModeEnum>        required,
                               final Level1Functor<T>     level1Functor,
                               final Level2Functor<T>     level2Functor,
                               final DefaultFunctor<T>    defaultFunctor,
                               final Level3Functor<T>     level3Functor,
                               final SpecialFunctor<T>    specialFunctor,
                               final AssignmentFunctor<T> assignmentFunctor)
        throws ParameterException
    {
        this(index,
             optionName,
             displayName,
             valueClass,
             constraints,
             validity,
             required,
             level1Functor,
             level2Functor,
             defaultFunctor,
             level3Functor,
             specialFunctor,
             assignmentFunctor,
             TrailingParameterBool.OPTION,
             NeedsArgumentBool.NEEDS_ARGUMENT);
    }


    /**
     * Constructor for option with name but no argument.
     *
     * @param index             Enum of this parameter
     * @param optionName        Long option name
     * @param displayName       Display name
     * @param constraints       Constraints
     * @param validity          Set of valid ModeEnum
     * @param required          Set of ModeEnum where parameter is required
     * @param level3Functor     Level 3 functor
     * @param specialFunctor    Special functor
     * @param assignmentFunctor Assignment functor
     *
     * @throws ParameterException On inconsistent values
     */
    public ParameterAttributes(final T                    index,
                               final String               optionName,
                               final String               displayName,
                               final Constraints          constraints,
                               final Set<ModeEnum>        validity,
                               final Set<ModeEnum>        required,
                               final Level3Functor<T>     level3Functor,
                               final SpecialFunctor<T>    specialFunctor,
                               final AssignmentFunctor<T> assignmentFunctor)
        throws ParameterException
    {
        this(index,
             optionName,
             displayName,
             Boolean.class,
             constraints,
             validity,
             required,
             null,
             null,
             null,
             level3Functor,
             specialFunctor,
             assignmentFunctor,
             TrailingParameterBool.OPTION,
             NeedsArgumentBool.NEEDS_NO_ARGUMENT);
    }


    /**
     * Constructor for option with no name.
     *
     * @param index             Enum of this parameter
     * @param displayName       Display name
     * @param valueClass        Value class
     * @param constraints       Constraints
     * @param validity          Set of valid ModeEnum
     * @param required          Set of ModeEnum where parameter is required
     * @param defaultFunctor    Default functor
     * @param level3Functor     Level 3 functor
     * @param specialFunctor    Special functor
     * @param assignmentFunctor Assignment functor
     *
     * @throws ParameterException On inconsistent values
     */
    public ParameterAttributes(final T                    index,
                               final String               displayName,
                               final Class<?>             valueClass,
                               final Constraints          constraints,
                               final Set<ModeEnum>        validity,
                               final Set<ModeEnum>        required,
                               final DefaultFunctor<T>    defaultFunctor,
                               final Level3Functor<T>     level3Functor,
                               final SpecialFunctor<T>    specialFunctor,
                               final AssignmentFunctor<T> assignmentFunctor)
        throws ParameterException
    {
        this(index,
             NO_OPTION_NAME,
             displayName,
             valueClass,
             constraints,
             validity,
             required,
             null,
             null,
             defaultFunctor,
             level3Functor,
             specialFunctor,
             assignmentFunctor,
             TrailingParameterBool.OPTION,
             NeedsArgumentBool.NEEDS_NO_ARGUMENT);
    }


    /**
     * Constructor for trailing option with no name but an argument.
     *
     * @param index             Enum of this parameter
     * @param displayName       Display name
     * @param valueClass        Value class
     * @param constraints       Constraints
     * @param validity          Set of valid ModeEnum
     * @param required          Set of ModeEnum where parameter is required
     * @param level1Functor     Level 1 functor
     * @param level2Functor     Level 2 functor
     * @param defaultFunctor    Default functor
     * @param level3Functor     Level 3 functor
     * @param specialFunctor    Special functor
     * @param assignmentFunctor Assignment functor
     *
     * @throws ParameterException On inconsistent values
     */
    public ParameterAttributes(final T                    index,
                               final String               displayName,
                               final Class<?>             valueClass,
                               final Constraints          constraints,
                               final Set<ModeEnum>        validity,
                               final Set<ModeEnum>        required,
                               final Level1Functor<T>     level1Functor,
                               final Level2Functor<T>     level2Functor,
                               final DefaultFunctor<T>    defaultFunctor,
                               final Level3Functor<T>     level3Functor,
                               final SpecialFunctor<T>    specialFunctor,
                               final AssignmentFunctor<T> assignmentFunctor)
        throws ParameterException
    {
        this(index,
             NO_OPTION_NAME,
             displayName,
             valueClass,
             constraints,
             validity,
             required,
             level1Functor,
             level2Functor,
             defaultFunctor,
             level3Functor,
             specialFunctor,
             assignmentFunctor,
             TrailingParameterBool.TRAILING,
             NeedsArgumentBool.NEEDS_ARGUMENT);
    }


    /**
     * Getter for index.
     *
     * @return Index enum
     */
    public T getIndex()
    {
        return _index;
    }


    /**
     * Getter for option name.
     *
     * @return Option name
     */
    public String getOptionName()
    {
        return _optionName;
    }


    /**
     * Getter for display name.
     *
     * @return Display name
     */
    public String getDisplayName()
    {
        return _displayName;
    }


    /**
     * Getter for trailing state.
     *
     * @return True if trailing parameter
     */
    public TrailingParameterBool getTrailing()
    {
        return _trailingParameter;
    }


    /**
     * Getter for needs-argument.
     *
     * @return True if needs argument
     */
    public NeedsArgumentBool getNeedsArgument()
    {
        return _needsArgument;
    }


    /**
     * Getter for value class.
     *
     * @return Class of value
     */
    public Class<?> getValueClass()
    {
        return _valueClass;
    }


    /**
     * Getter for constraints.
     *
     * @return Constraints
     */
    public Constraints getConstraints()
    {
        return _constraints;
    }


    /**
     * Getter for validity set.
     *
     * @return Validity set
     */
    public Set<ModeEnum> getValiditySet()
    {
        return _validity;
    }


    /**
     * Check for a mode in the validity set.
     *
     * @param mode Mode to check for
     *
     * @return True if set
     */
    public boolean hasMode(final ModeEnum mode)
    {
        return _validity.contains(mode);
    }


    /**
     * Getter for required set.
     *
     * @return Required set
     */
    public Set<ModeEnum> getRequiredSet()
    {
        return _required;
    }


    /**
     * Check for a mode in the required set.
     *
     * @param mode Mode to check for
     *
     * @return True if set
     */
    public boolean isRequired(final ModeEnum mode)
    {
        return _required.contains(mode);
    }


    /**
     * Getter for value.
     *
     * @return Value object
     */
    public Value getValue()
    {
        return _value;
    }


    /**
     * Convenience method to see if there is a value.
     *
     * @return True if there is a value
     */
    public boolean hasValue()
    {
        return _value.hasValue();
    }


    /**
     * Getter for level 1 functor.
     *
     * @return Level 1 functor
     */
    public Level1Functor<T> getLevel1Functor()
    {
        return _level1Functor;
    }


    /**
     * Getter for level 2 functor.
     *
     * @return Level 2 functor
     */
    public Level2Functor<T> getLevel2Functor()
    {
        return _level2Functor;
    }


    /**
     * Getter for level 3 functor.
     *
     * @return Level 3 functor
     */
    public Level3Functor<T> getLevel3Functor()
    {
        return _level3Functor;
    }


    /**
     * Getter for default functor.
     *
     * @return Default functor
     */
    public DefaultFunctor<T> getDefaultFunctor()
    {
        return _defaultFunctor;
    }


    /**
     * Getter for special functor.
     *
     * @return Special functor
     */
    public SpecialFunctor<T> getSpecialFunctor()
    {
        return _specialFunctor;
    }


    /**
     * Getter for assignment functor.
     *
     * @return Assignment functor
     */
    public AssignmentFunctor<T> getAssignmentFunctor()
    {
        return _assignmentFunctor;
    }
}
