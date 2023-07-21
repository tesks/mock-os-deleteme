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
package jpl.gds.shared.collection;

import java.util.Set;


/**
 * Class with set-related utilities. Final because it has only static methods.
 *
 */
public final class SetUtilities extends Object
{
    /**
     * Private constructor, never used.
     */
    private SetUtilities()
    {
        super();
    }


    /**
     * Get first element of set, if there is one.
     *
     * @param set Set in question
     *
     * @return First element or null
     *
     * @param <T> Element type of set
     */
    public static <T> T getFirstElement(final Set<T> set)
    {
        T result = null;

        if ((set != null) && ! set.isEmpty())
        {
            for (final T element : set)
            {
                result = element;
                break;
            }
        }

        return result;
    }
}
