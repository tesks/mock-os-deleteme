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

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.string.StringUtil;


/**
 * Abstract base class for parameter classes. Contains generally useful stuff
 * used by validators and functors and others.
 *
 * This the non-generic layer, the top. AbstractGenericParameter extends this
 * and is generic.
 *
 */
abstract public class AbstractParameter extends Object
{
    /** Convenience for subclasses */
    protected static final String NO_OPTION_NAME = null;

    /** Convenience for subclasses */
    protected static final String NO_DISPLAY_NAME = null;

    /** Convenience for subclasses */
    protected static final Constraints DEFAULT_CONSTRAINTS = new Constraints();

    /** Convenience for subclasses */
    protected static final Set<ModeEnum> EMPTY_SET =
        Collections.<ModeEnum>emptySet();

    /** Convenience for subclasses */
    protected static final Set<ModeEnum> FULL_SET =
        Collections.unmodifiableSet(EnumSet.allOf(ModeEnum.class));

    /** Convenience for subclasses */
    protected static final Set<ModeEnum> EMPTY_VALIDITY_SET = EMPTY_SET;

    /** Convenience for subclasses */
    protected static final Set<ModeEnum> FULL_VALIDITY_SET = FULL_SET;

    /** Convenience for subclasses */
    protected static final Set<ModeEnum> EMPTY_REQUIRED_SET = EMPTY_SET;

    /** Convenience for subclasses */
    protected static final Set<ModeEnum> FULL_REQUIRED_SET = FULL_SET;

    /** Tracer for all subclasses */
    protected static final Tracer        LOG                 = TraceManager.getDefaultTracer();



    /**
     * Constructor.
     */
    protected AbstractParameter()
    {
        super();
    }


    /**
     * Check for null. "who" may or may not have a blank at the end.
     *
     * @param <T> Object type
     * @param who    Who is checking
     * @param object Any object
     * @param name   Name of object
     *
     * @return Object
     *
     * @throws ParameterException If object null
     *
     */
    public static final <T> T checkNull(final String who,
                                        final T      object,
                                        final String name)
        throws ParameterException
    {
        checkForNull(who, object, name);

        return object;
    }


    /**
     * Check for null. "who" may or may not have a blank at the end.
     *
     * @param who    Who is checking
     * @param object Any object
     * @param name   Name of object
     *
     * @throws ParameterException If object null
     */
    protected static final void checkForNull(final String who,
                                             final Object object,
                                             final String name)
        throws ParameterException
    {
        if (object == null)
        {
            throw new ParameterException(StringUtil.safeTrim(who) +
                                         " "                      +
                                         name                     +
                                         " cannot be null");
        }
    }


    /**
     * Check for UNKNOWN and throw. This method eliminates PMD complaints
     * about raising an exception in a try block.
     *
     * @param value String to check
     */
    protected static final void disallowUnknown(final String value)
    {
        if ("UNKNOWN".equalsIgnoreCase(StringUtil.safeTrim(value)))
        {
            throw new IllegalArgumentException("UNKNOWN not allowed");
        }
    }


    /**
     * Check for UNKNOWN and throw. This method eliminates PMD complaints
     * about raising an exception in a try block.
     *
     * The exception is wrapped for use with reflection.
     *
     * @param value String to check
     *
     * @throws InvocationTargetException If UNKNOWN
     */
    protected static final void disallowUnknownReflected(final String value)
        throws InvocationTargetException
    {
        try
        {
            disallowUnknown(value);
        }
        catch (final IllegalArgumentException iae)
        {
            throw new InvocationTargetException(iae);
        }
    }


    /**
     * Purge UNKNOWN from a choice list if required. List is used in order to
     * preserve the original ordering of the choices. We also wind up with
     * strings.
     *
     * @param choices      A collection of choices
     * @param allowUnknown If true, leave it be
     *
     * @return List of string choices perhaps without UNKNOWN
     *
     * @param <T> Element type
     */
    protected static final <T> List<String> purgeUnknownFromChoices(
        final Collection<T>    choices,
        final AllowUnknownBool allowUnknown)
    {
        final List<String> list = new ArrayList<String>(choices.size());

        for (final T choice : choices)
        {
            final String s = StringUtil.safeTrim(choice.toString());

            if (allowUnknown.get() || ! "UNKNOWN".equalsIgnoreCase(s))
            {
                list.add(s);
            }
        }

        return list;
    }


    /**
     * Get the best absolute path of the file. Try canonical, then plain
     * absolute.
     *
     * @param file File
     *
     * @return Best path of file
     */
    protected static final String getAbsolutePath(final File file)
    {
        String result = null;

        try
        {
            result = file.getCanonicalPath();
        }
        catch (final IOException ioe)
        {
            result = file.getAbsolutePath();
        }

        return result;
    }


    /**
     * Comparator that compares on the string form of the objects.
     *
     * @param <T> Type to be compared
     */
    protected static final class AsStringComparator<T> extends Object
        implements Comparator<T>, Serializable
    {
        private static final long serialVersionUID = 0L;


        /**
         * Constructor.
         */
        public AsStringComparator()
        {
            super();
        }


        /**
         * Convert to string and then compare.
         *
         * @param left  One object
         * @param right Other object
         *
         * @return Comparison state
         */
        @Override
        public int compare(final T left,
                           final T right)
        {
            return left.toString().compareTo(right.toString());
        }


        /**
         * Is object equal to this comparator? We say, only if the same.
         *
         * @param other Other object
         *
         * @return True if the same
         */
        @Override
        public boolean equals(final Object other)
        {
            return (this == other);
        }


        /**
         * Hash code must go with equals.
         * This is safe and won't be used anyway.
         *
         * @return Zero
         */
        @Override
        public int hashCode()
        {
            return 0;
        }
    }
}
