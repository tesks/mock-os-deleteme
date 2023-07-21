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
package jpl.gds.security.cam;


/**
 * Used with access control classes.
 *
 */
public class AccessControlException extends Exception
{
    private static final long serialVersionUID = 0L;

    private final boolean _serious;


    /**
     * Constructor.
     *
     * @param serious True if a serious error
     */
    public AccessControlException(final boolean serious)
    {
        super();

        _serious = serious;
    }


    /**
     * Constructor.
     */
    public AccessControlException()
    {
        this(false);
    }


    /**
     * Constructor.
     *
     * @param message Detail message for this exception
     * @param serious True if a serious error
     */
    public AccessControlException(final String  message,
                                  final boolean serious)
    {
        super(message);

        _serious = serious;
    }


    /**
     * Constructor.
     *
     * @param message Detail message for this exception
     */
    public AccessControlException(final String message)
    {
        this(message, false);
    }


    /**
     * Constructor.
     *
     * @param cause   The Throwable that triggered this exception
     * @param serious True if a serious error
     */
    public AccessControlException(final Throwable cause,
                                  final boolean   serious)
    {
        super(cause);

        _serious = serious;
    }


    /**
     * Constructor.
     *
     * @param cause The Throwable that triggered this exception
     */
    public AccessControlException(final Throwable cause)
    {
        this(cause, false);
    }


    /**
     * Constructor.
     *
     * @param message The detail message for this exception
     * @param cause   The Throwable that triggered this exception
     * @param serious True if a serious error
     */
    public AccessControlException(final String    message,
                                  final Throwable cause,
                                  final boolean   serious)
    {
        super(message, cause);

        _serious = serious;
    }


    /**
     * Constructor.
     *
     * @param message The detail message for this exception
     * @param cause   The Throwable that triggered this exception
     */
    public AccessControlException(final String    message,
                                  final Throwable cause)
    {
        this(message, cause, false);
    }


    /**
     * Return true if error was a "serious" one.
     *
     * @return Serious state
     */
    public boolean getSerious()
    {
        return _serious;
    }
}
