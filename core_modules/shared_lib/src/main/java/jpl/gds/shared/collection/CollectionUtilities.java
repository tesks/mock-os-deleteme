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

import java.util.Collection;


/**
 * Class with collection-related utilities. Final because it has only static
 * methods.
 *
 */
public final class CollectionUtilities extends Object
{
    /**
     * Private constructor, never used.
     */
    private CollectionUtilities()
    {
        super();
    }


    /**
     * Get first element of collection, if there is one.
     *
     * @param collection Collection in question
     *
     * @return First element or null
     *
     * @param <T> Element type of collection
     */
    public static <T> T getFirstElement(final Collection<T> collection)
    {
        T result = null;

        if ((collection != null) && ! collection.isEmpty())
        {
            for (final T element : collection)
            {
                result = element;
                break;
            }
        }

        return result;
    }


    /**
     * Search an unsorted array.
     *
     * @param array  Array in question
     * @param object What to search for (may be null)
     *
     * @return True if found
     *
     * @param <T> Element type of array
     */
    public static <T> boolean contains(final T[] array,
                                       final T   object)
    {
        if ((array == null) || (array.length == 0))
        {
            return false;
        }

        if (object != null)
        {
            for (final T element : array)
            {
                if (object.equals(element))
                {
                    return true;
                }
            }
        }
        else
        {
            for (final T element : array)
            {
                if (element == null)
                {
                    return true;
                }
            }
        }

        return false;
    }
}
