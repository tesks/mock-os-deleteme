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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import jpl.gds.shared.collection.CollectionUtilities;


/**
 * Abstract base class for parameter functors. Contains generally useful stuff
 * used by functors.
 *
 *
 * @param <T> Enum of parameters
 */
abstract public class AbstractParameterFunctor<T extends Enum<T>>
    extends AbstractGenericParameter<T>
{
    /** Specifies the level of a choice collection */
    protected static enum ChoiceTypeEnum
    {
        /** Choices are across missions */
        MULTI_MISSION,

        /** Choices are for mission */
        MISSION,

        /** Choices are specific to the situation */
        SUBSET
    }


    /** The maximum number of collection elements to display in messages */
    private static final int MAX_COLLECTION_SIZE = 15;


    /**
     * Constructor.
     */
    protected AbstractParameterFunctor()
    {
        super();
    }


    /**
     * Add standard error for an unexpected missing value.
     *
     * @param item    Name of the missing item
     * @param functor Functor involved
     * @param errors  Error list to add error to
     */
    protected static final void valueMissing(final Object       item,
                                             final String       functor,
                                             final List<String> errors)
    {
        errors.add("Internal error, no value for " +
                   item                            +
                   " in "                          +
                   functor);
    }


    /**
     * Add a choice collection to a message.
     *
     * @param <T> Element type
     * @param choices A collection
     * @param level   Specifies level of choices
     *
     * @return Collection as string
     *
     * @throws ParameterException On bad parameter
     *
     */
    protected static final <T> String appendChoices(
                                          final Collection<T>  choices,
                                          final ChoiceTypeEnum level)
        throws ParameterException
    {
        if (choices.isEmpty())
        {
            return "; no choices available";
        }

        final StringBuilder sb = new StringBuilder("; ");

        switch (checkNull("appendChoices: ", level, "Level"))
        {
            case MULTI_MISSION:
                sb.append("multi-mission ");
                break;

            case MISSION:
                sb.append("mission ");
                break;

            case SUBSET:
            default:
                break;
        }

        final int size    = choices.size();
        final int useSize = Math.min(size, MAX_COLLECTION_SIZE);

        if (size != useSize)
        {
            sb.append("first ").append(useSize).append(' ');
        }

        sb.append("choices are ");

        // Do we have a collection of Number?

        final boolean isNumber =
            Number.class.isInstance(
                CollectionUtilities.getFirstElement(choices));

        // We want to sort as String, except for Number

        final Set<T> sorted =
            (isNumber ? new TreeSet<T>()
                      : new TreeSet<T>(new AsStringComparator<T>()));

        sorted.addAll(choices);

        // Get as many as we want, in sorted order

        final List<T> selected = new ArrayList<T>(useSize);

        for (final T choice : sorted)
        {
            if (selected.size() == useSize)
            {
                break;
            }

            selected.add(choice);
        }

        sb.append(selected);

        return sb.toString();
    }


    /**
     * Double-check that there is a value in situations where there should
     * be one. If this throws there is a serious logic error somewhere.
     *
     * @param valueObject Value to check
     *
     * @throws ParameterException If there is no value
     */
    protected static final void mustHaveValue(final Value valueObject)
        throws ParameterException
    {
        if (! valueObject.hasValue())
        {
            throw new ParameterException("Internal error, expected value " +
                                         "missing");
        }
    }


    /**
     * Double-check that there is no value in situations where there should
     * be none. If this throws there is a serious logic error somewhere.
     *
     * @param valueObject Value to check
     *
     * @throws ParameterException If there is a value
     */
    protected static final void mustNotHaveValue(final Value valueObject)
        throws ParameterException
    {
        if (valueObject.hasValue())
        {
            throw new ParameterException("Internal error, unexpected value");
        }
    }
}
