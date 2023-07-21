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
package jpl.gds.shared.exceptions;

import java.sql.SQLException;

import org.apache.commons.lang3.StringUtils;

import jpl.gds.shared.log.TraceManager;


/**
 * Class ExceptionTools.
 *
 */
public class ExceptionTools extends Object {

    /** Constant to check if exception message contains the nested exception message */
    public static final String NESTED_EXCEPTION = "nested exception is";

    /**
     * Prevent instantiation.
     */
    private ExceptionTools()
    {
        super();
    }


    /**
     * Get the message and all "caused by" names and messages. Optionally
     * include the exception name.
     *
     * @param t
     * @param full
     *
     * @return String
     */
    private static String rollUpInternal(final Throwable t,
                                         final boolean   full)
    {
        if (t == null)
        {
            return "null";
        }

        final StringBuilder sb      = new StringBuilder();
        Throwable           current = t;

        while (true)
        {
            sb.append(full ? current.toString()
                           : current.getLocalizedMessage());

            if (current instanceof SQLException)
            {
                final SQLException sqle = (SQLException) current;

                sb.append(" code ").append(sqle.getErrorCode());
                sb.append(" state '").append(sqle.getSQLState()).append("'");
            }

            current = current.getCause();

            if (current == null)
            {
                break;
            }

            sb.append(" caused by: ");
        }

        return sb.toString();
    }


    /**
     * Get the exception name and message and all "caused by" messages.
     *
     * @param t Throwable
     *
     * @return String
     */
    public static String rollUpMessages(final Throwable t)
    {
        return rollUpInternal(t, true);
    }


    /**
     * Get the message and all "caused by" messages. Used when we want to avoid
     * "Exception" in the log.
     *
     * @param t Throwable
     *
     * @return String
     */
    public static String rollUpMessagesOnly(final Throwable t)
    {
        return rollUpInternal(t, false);
    }


    /**
     * Turn a stack trace to a string.
     *
     * @param stack Array of stack elements
     *
     * @return Stack in string form
     */
    public static String printStack(final StackTraceElement[] stack)
    {
        if (stack == null)
        {
            return "null";
        }

        final StringBuilder sb    = new StringBuilder();
        boolean             first = true;

        for (final StackTraceElement ste : stack)
        {
            if (! first)
            {
                sb.append('\n');
            }
            else
            {
                first = false;
            }

            sb.append(ste);
        }

        return sb.toString();
    }


    /**
     * Utility to set the cause of an older exception that does not overload
     * all of the constructors.
     * 
     * @param <T> type of the exception
     * @param exception Exception to throw
     * @param cause     Original exception
     *
     * @throws T The exception, now with a cause attached
     *
     */
    public static <T extends Exception> void addCauseAndThrow(
                                                 final T         exception,
                                                 final Throwable cause)
        throws T
    {
        exception.initCause(cause);

        throw exception;
    }
    
    /**
     * Returns information about the Throwable t via t.getMessage() if it is not null;
     * otherwise, returns the description via t.toString().
     * 
     * @param t Throwable
     * @return information about the Throwable in String form
     */
    public static String getMessage(final Throwable t){
    	return (t.getMessage() == null? t.toString() : t.getMessage());
    }

    /**
     * Helper method for handling SpringBoot application startup errors
     * 
     * @param t
     *            Throwable
     */
    public static void handleSpringBootStartupError(final Throwable t) {
        final Throwable cause = t.getCause();
        String err = "";
        if (cause != null) {
            err = cause.getLocalizedMessage();
        }
        else if (t.getLocalizedMessage() != null) {
            err = t.getLocalizedMessage();
        }
        else {
            err = ExceptionTools.getMessage(t);
        }
        if (err.contains(ExceptionTools.NESTED_EXCEPTION)) {
            err = StringUtils.substringAfterLast(err, ExceptionTools.NESTED_EXCEPTION).trim();
        }
        TraceManager.getDefaultTracer().error("ERROR: " + err);
    }
}
