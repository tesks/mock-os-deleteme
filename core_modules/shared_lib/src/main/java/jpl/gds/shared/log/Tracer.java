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
package jpl.gds.shared.log;

import org.slf4j.Marker;
import org.springframework.context.ApplicationContext;

/**
 * This interface provides a set of methods for tracing/logging various levels
 * of information. Its purpose in life is to provide a wrapper around the SLF4J
 * logging package, so the underlying implementation can be changed without
 * impact to the code that uses tracing.
 * 
 * @see TraceManager
 * 
 */
public interface Tracer {

	/**
	 * Create a trace message of any severity.
	 * 
	 * @param priority
	 *            the severity of the message
	 * @param message
	 *            the message itself; if not a String, the toString() value will
	 *            be used as the actual message text unless attached Appenders
	 *            have special handling for the given Object type
	 */
	public void log(TraceSeverity priority, Object message);

	/**
	 * Create a trace message of any severity, with an associated cause.
	 * 
	 * @param priority
	 *            the severity of the message
	 * @param message
	 *            the message itself; if not a String, the toString() value will
	 *            be used as the actual message text unless attached Appenders
	 *            have special handling for the given Object type
	 * @param t
	 *            a Throwable cause associated with the message
	 */
	public void log(TraceSeverity priority, Object message, Throwable t);
	
    /**
     * Create a trace message of any severity, with an associated cause and
     * markers
     * 
     * @param markers
     *            the markers attached to this log message
     * @param priority
     *            the severity of the message
     * @param message
     *            the message itself; if not a String, the toString() value will
     *            be used as the actual message text unless attached Appenders
     *            have special handling for the given Object type
     * @param t
     *            a Throwable cause associated with the message
     */
    public void log(Marker markers, TraceSeverity priority, Object message, Throwable t);

    /**
     * Create a trace message of any severity, with associated markers
     * 
     * @param markers
     *            the markers attached to this log message
     * @param priority
     *            the severity of the message
     * @param message
     *            the message itself; if not a String, the toString() value will
     *            be used as the actual message text unless attached Appenders
     *            have special handling for the given Object type
     */
    public void log(Marker markers, TraceSeverity priority, Object message);

    /**
     * @param message
     *            The IPublishableLogMessage to log
     */
    public void log(IPublishableLogMessage message);

	/**
	 * Create a trace message of "TRACE" severity.
	 * 
	 * @param message
	 *            the message itself; if not a String, the toString() value will
	 *            be used as the actual message text unless attached Appenders
	 *            have special handling for the given Object type
	 */
	public void trace(Object message);


	/**
	 * Create a trace message of "TRACE" severity, with an associated cause.
	 * 
	 * @param message
	 *            the message itself; if not a String, the toString() value will
	 *            be used as the actual message text unless attached Appenders
	 *            have special handling for the given Object type
	 * @param t
	 *            a Throwable cause associated with the message
	 */
	public void trace(Object message, Throwable t);

    /**
     * Create a trace message of "TRACE" severity, with an associated cause and
     * markers
     * 
     * @param markers
     *            The markers to log with
     * @param message
     *            the message itself; if not a String, the toString() value will
     *            be used as the actual message text unless attached Appenders
     *            have special handling for the given Object type
     * @param t
     *            a Throwable cause associated with the message
     */
    public void trace(Marker markers, Object message, Throwable t);

    /**
     * Create a trace message of "TRACE" severity, with associated markers
     * 
     * @param markers
     *            The markers to log with
     * @param message
     *            the message itself; if not a String, the toString() value will
     *            be used as the actual message text unless attached Appenders
     *            have special handling for the given Object type
     */
    public void trace(Marker markers, Object message);

    /**
     * @param messages
     *            the messages to log
     */
    public void trace(Object... messages);

    /**
     * 
     * @param markers
     *            markers to log with
     * @param messages
     *            messages to log
     */
    public void trace(Marker markers, Object... messages);

    /**
     * 
     * @param markers
     *            markers to log with
     * @param t
     *            Throwable cause to log
     * @param messages
     *            messages to log
     */
    public void trace(Marker markers, Throwable t, Object... messages);

	/**
	 * Create a trace message of "DEBUG" severity.
	 * 
	 * @param message
	 *            the message itself; if not a String, the toString() value will
	 *            be used as the actual message text unless attached Appenders
	 *            have special handling for the given Object type
	 */
	public void debug(Object message);

	/**
	 * Create a trace message of "DEBUG" severity, with an associated cause.
	 * 
	 * @param message
	 *            the message itself; if not a String, the toString() value will
	 *            be used as the actual message text unless attached Appenders
	 *            have special handling for the given Object type
	 * @param t
	 *            a Throwable cause associated with the message
	 */
	public void debug(Object message, Throwable t);

    /**
     * Create a trace message of "DEBUG" severity, with an associated cause and
     * markers
     * 
     * @param markers
     *            The markers to log with
     * @param message
     *            the message itself; if not a String, the toString() value will
     *            be used as the actual message text unless attached Appenders
     *            have special handling for the given Object type
     * @param t
     *            a Throwable cause associated with the message
     */
    public void debug(Marker markers, Object message, Throwable t);

    /**
     * Create a trace message of "DEBUG" severity, with associated markers
     * 
     * @param markers
     *            The markers to log with
     * @param message
     *            the message itself; if not a String, the toString() value will
     *            be used as the actual message text unless attached Appenders
     *            have special handling for the given Object type
     */
    public void debug(Marker markers, Object message);

    /**
     * @param messages
     *            the messages to log
     */
    public void debug(Object... messages);

    /**
     * 
     * @param markers
     *            markers to log with
     * @param messages
     *            messages to log
     */
    public void debug(Marker markers, Object... messages);

    /**
     * 
     * @param markers
     *            markers to log with
     * @param t
     *            Throwable cause to log
     * @param messages
     *            messages to log
     */
    public void debug(Marker markers, Throwable t, Object... messages);

	/**
	 * Create a trace message of "WARNING" severity.
	 * 
	 * @param message
	 *            the message itself; if not a String, the toString() value will
	 *            be used as the actual message text unless attached Appenders
	 *            have special handling for the given Object type
	 */
	public void warn(Object message);

	/**
	 * Create a trace message of "WARNING" severity, with an associated cause.
	 * 
	 * @param message
	 *            the message itself; if not a String, the toString() value will
	 *            be used as the actual message text unless attached Appenders
	 *            have special handling for the given Object type
	 * @param t
	 *            a Throwable cause associated with the message
	 */
	public void warn(Object message, Throwable t);

    /**
     * Create a trace message of "WARNING" severity, with an associated cause
     * and markers
     * 
     * @param markers
     *            The markers to log with
     * @param message
     *            the message itself; if not a String, the toString() value will
     *            be used as the actual message text unless attached Appenders
     *            have special handling for the given Object type
     * @param t
     *            a Throwable cause associated with the message
     */
    public void warn(Marker markers, Object message, Throwable t);

    /**
     * Create a trace message of "WARNING" severity, with associated markers
     * 
     * @param markers
     *            The markers to log with
     * @param message
     *            the message itself; if not a String, the toString() value will
     *            be used as the actual message text unless attached Appenders
     *            have special handling for the given Object type
     */
    public void warn(Marker markers, Object message);

    /**
     * @param messages
     *            the messages to log
     */
    public void warn(Object... messages);

    /**
     * 
     * @param markers
     *            markers to log with
     * @param messages
     *            messages to log
     */
    public void warn(Marker markers, Object... messages);

    /**
     * 
     * @param markers
     *            markers to log with
     * @param t
     *            Throwable cause to log
     * @param messages
     *            messages to log
     */
    public void warn(Marker markers, Throwable t, Object... messages);

	/**
	 * Create a trace message of "INFO" severity.
	 * 
	 * @param message
	 *            the message itself; if not a String, the toString() value will
	 *            be used as the actual message text unless attached Appenders
	 *            have special handling for the given Object type
	 */
	public void info(Object message);

	/**
	 * Create a trace message of "INFO" severity, with an associated cause.
	 * 
	 * @param message
	 *            the message itself; if not a String, the toString() value will
	 *            be used as the actual message text unless attached Appenders
	 *            have special handling for the given Object type
	 * @param t
	 *            a Throwable cause associated with the message
	 */
	public void info(Object message, Throwable t);

    /**
     * Create a trace message of "INFO" severity, with an associated cause and
     * markers
     * 
     * @param markers
     *            The markers to log with
     * @param message
     *            the message itself; if not a String, the toString() value will
     *            be used as the actual message text unless attached Appenders
     *            have special handling for the given Object type
     * @param t
     *            a Throwable cause associated with the message
     */
    public void info(Marker markers, Object message, Throwable t);

    /**
     * Create a trace message of "INFO" severity, with associated markers
     * 
     * @param markers
     *            The markers to log with
     * @param message
     *            the message itself; if not a String, the toString() value will
     *            be used as the actual message text unless attached Appenders
     *            have special handling for the given Object type
     */
    public void info(Marker markers, Object message);

    /**
     * @param messages
     *            the messages to log
     */
    public void info(Object... messages);

    /**
     * 
     * @param markers
     *            markers to log with
     * @param messages
     *            messages to log
     */
    public void info(Marker markers, Object... messages);

    /**
     * 
     * @param markers
     *            markers to log with
     * @param t
     *            Throwable cause to log
     * @param messages
     *            messages to log
     */
    public void info(Marker markers, Throwable t, Object... messages);

	/**
	 * Create a trace message of "ERROR" severity.
	 * 
	 * @param message
	 *            the message itself; if not a String, the toString() value will
	 *            be used as the actual message text unless attached Appenders
	 *            have special handling for the given Object type
	 */
	public void error(Object message);

	/**
	 * Create a trace message of "ERROR" severity, with an associated cause.
	 * 
	 * @param message
	 *            the message itself; if not a String, the toString() value will
	 *            be used as the actual message text unless attached Appenders
	 *            have special handling for the given Object type
	 * @param t
	 *            a Throwable cause associated with the message
	 */
	public void error(Object message, Throwable t);

    /**
     * Create a trace message of "ERROR" severity, with an associated cause and
     * markers
     * 
     * @param markers
     *            The markers to log with
     * @param message
     *            the message itself; if not a String, the toString() value will
     *            be used as the actual message text unless attached Appenders
     *            have special handling for the given Object type
     * @param t
     *            a Throwable cause associated with the message
     */
    public void error(Marker markers, Object message, Throwable t);

    /**
     * Create a trace message of "ERROR" severity, with associated markers
     * 
     * @param markers
     *            The markers to log with
     * @param message
     *            the message itself; if not a String, the toString() value will
     *            be used as the actual message text unless attached Appenders
     *            have special handling for the given Object type
     */
    public void error(Marker markers, Object message);

    /**
     * @param messages
     *            the messages to log
     */
    public void error(Object... messages);

    /**
     * 
     * @param markers
     *            markers to log with
     * @param messages
     *            messages to log
     */
    public void error(Marker markers, Object... messages);

    /**
     * 
     * @param markers
     *            markers to log with
     * @param t
     *            Throwable cause to log
     * @param messages
     *            messages to log
     */
    public void error(Marker markers, Throwable t, Object... messages);

	/**
	 * Create a trace message of "FATAL" severity.
	 * 
	 * @param message
	 *            the message itself; if not a String, the toString() value will
	 *            be used as the actual message text unless attached Appenders
	 *            have special handling for the given Object type
	 */
	@Deprecated
	public void fatal(Object message);

	/**
	 * Create a trace message of "FATAL" severity, with an associated cause.
	 * 
	 * @param message
	 *            the message itself; if not a String, the toString() value will
	 *            be used as the actual message text unless attached Appenders
	 *            have special handling for the given Object type
	 * @param t
	 *            a Throwable cause associated with the message
	 */
	@Deprecated
	public void fatal(Object message, Throwable t);

    /**
     * Indicates whether trace messages for a specific severity level are
     * currently enabled.
     * 
     * @param priority
     *            the severity level to check
     * @return true if the specific trace level is enabled; false if not
     */
	public boolean isEnabledFor(TraceSeverity priority);
	
    /**
     * Indicates whether trace messages for a specific severity level and Marker
     * are currently enabled.
     * 
     * @param priority
     *            the severity level to check
     * @param markers
     *            The markers to check
     * @return true if the specific trace level is enabled; false if not
     */
    public boolean isEnabledFor(TraceSeverity priority, Marker markers);

    /**
     * @return Whether or not the tracer is enabled for debug logging
     */
	public boolean isDebugEnabled();
	
    /**
     * @return The Tracer name
     */
	public String getName();
	
    /**
     * @return The application context attached to this Tracer; may be null.
     */
	public ApplicationContext getAppContext();

    /**
     * Set the application context associated with a Tracer
     * 
     * @param context
     *            The current application context
     */
	public void setAppContext(ApplicationContext context);
	
	/**
	 * Establishes a text prefix to add to all messages from this Tracer.
	 * 
	 * @param prefix text prefix to set
	 */
	public void setPrefix(String prefix);
	
	/**
	 * Gets the text prefix to add to all messages from this Tracer.
	 * 
	 * @return prefix text; may be null
	 */
    public String getPrefix();

    /**
     * Programmatically sets the log level on this Tracer
     * 
     * @param level
     *            The level to set; may not be null
     */
    public void setLevel(TraceSeverity level);
	
}
