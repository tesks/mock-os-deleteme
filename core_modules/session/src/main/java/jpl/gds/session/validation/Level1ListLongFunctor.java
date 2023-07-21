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
import java.util.Collections;
import java.util.List;


/**
 * Level 1 validation functor for a list of longs.
 *
 * @param <T> Parameter set enum type
 *
 */
public final class Level1ListLongFunctor<T extends Enum<T>>
    extends AbstractParameterFunctor<T>
    implements AbstractGenericParameter.Level1Functor<T>
{
    private static final String ME = "Level1ListLongFunctor: ";

    private final DeduplicateBool _deduplicate;
    private final SortBool        _sort;
    private final AllowRangesBool _allowRanges;


    /**
     * Constructor.
     *
     * @param deduplicate True if duplicates are to be removed
     * @param sort        True if result is to be sorted
     * @param allowRanges True if ranges are allowed
     *
     * @throws ParameterException On any error
     */
    public Level1ListLongFunctor(final DeduplicateBool deduplicate,
                                 final SortBool        sort,
                                 final AllowRangesBool allowRanges)
        throws ParameterException
    {
        super();

        _deduplicate = checkNull(ME, deduplicate, "Deduplicate");
        _sort        = checkNull(ME, sort,        "Sort");
        _allowRanges = checkNull(ME, allowRanges, "Allow ranges");
    }


    /**
     * Validate list of longs at level 1. We use a list so that the order can
     * be preserved if desired.
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
        final List<Long>  list        = new ArrayList<Long>();
        final String      full        = valueObject.getValueAsString(
                                            UppercaseBool.DO_NOT_UPPERCASE);

        for (final String s : full.split(",", -1))
        {
            final String rangeValue = s.trim();

            for (final String svalue : derange(rangeValue, _allowRanges))
            {
                long value = 0L;

                try
                {
                    value = Long.parseLong(svalue);
                }
                catch (final NumberFormatException nfe)
                {
                    errors.add("Option "           +
                               pa.getDisplayName() +
                               " '"                +
                               full                +
                               "' is not parseable as an integer list");
                    return;
                }

                if (! constraints.checkBounds(value))
                {
                    errors.add("Option "                  +
                               pa.getDisplayName()        +
                               " component "              +
                               value                      +
                               " does not lie in range [" +
                               constraints.getMinimum()   +
                               ","                        +
                               constraints.getMaximum()   +
                               "]");
                    return;
                }

                if (! _deduplicate.get() || ! list.contains(value))
                {
                    list.add(value);
                }
            }
        }

        if (_sort.get())
        {
            Collections.sort(list);
        }

        valueObject.setValue(list);
    }


    /**
     * Convert range to list of strings. If ranges are not allowed, or if there
     * is any error, the original string is passed back as the only member of
     * the list. Otherwise, the list is filled with the expanded range as
     * strings.
     *
     * @param s           String that might contain a range
     * @param allowRanges True if ranges are desired
     *
     * @return List of strings
     */
    private static List<String> derange(final String          s,
                                        final AllowRangesBool allowRanges)
    {
        final List<String> list = new ArrayList<String>();

        // Assume bad or no range and send back original string

        list.add(s);

        if (! allowRanges.get())
        {
            return list;
        }

        final int mark = s.indexOf("..");

        if (mark < 0)
        {
            return list;
        }

        long left  = 0L;
        long right = 0L;

        try
        {
            left  = Long.parseLong(s.substring(0, mark).trim());
            right = Long.parseLong(s.substring(mark + 2).trim());
        }
        catch (final NumberFormatException nfe)
        {
            return list;
        }

        if (left > right)
        {
            return list;
        }

        // OK, clear and put in range as strings

        list.clear();

        for (long i = left; i <= right; ++i)
        {
            list.add(String.valueOf(i));
        }

        return list;
    }
}
