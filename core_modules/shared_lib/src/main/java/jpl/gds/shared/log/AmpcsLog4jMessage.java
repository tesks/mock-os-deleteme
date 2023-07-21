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

import org.apache.logging.log4j.message.Message;
import org.slf4j.Marker;
import org.springframework.context.ApplicationContext;

import jpl.gds.shared.log.config.LoggingProperties;
import jpl.gds.shared.message.CommonMessageType;
import jpl.gds.shared.message.PublishableLogMessage;
import jpl.gds.shared.metadata.context.ContextKey;
import jpl.gds.shared.metadata.context.IContextKey;

/**
 * AMPCS Log4j2 Message object implementation. Log messages are converted to an
 * AmpcsLog4jMessage before being logged and sent to appenders. This object can
 * be extracted from the LogEvent object seen in custom appenders
 * 
 */
public class AmpcsLog4jMessage extends PublishableLogMessage implements Message {

    private final StringBuilder     sb              = new StringBuilder();
    /** Auto generated serial */
    private static final long       serialVersionUID = -4688767673251784289L;

    private final String            prefix;
    private final Marker            markers;

    private ApplicationContext      appContext;
    private Throwable               throwable;



    /**
     * Creates an AmpcsLog4jMessage object
     * 
     * @param level
     *            The level to log
     * 
     * @param t
     *            The throwable cause; may be null
     * @param prefix
     *            The message prefix; may be empty
     * @param messages
     *            The message to log
     */
    public AmpcsLog4jMessage(final TraceSeverity level, final Throwable t, final String prefix,
            final Object... messages) {
        this(level, null, t, prefix, messages);
    }

    /**
     * @param level
     *            The level to log
     * @param markers
     *            The markers to associate the log message with; may be null
     * @param t
     *            The throwable to log; may be null
     * @param prefix
     *            The prefix to append to a log message; may be empty
     * @param messages
     *            The message to log
     */
    public AmpcsLog4jMessage(final TraceSeverity level, final Marker markers, final Throwable t, final String prefix,
            final Object... messages) {
        this(level, null, markers, t, prefix, messages);
    }

    /**
     * Creates an AmpcsLog4jMessage object
     * 
     * @param level
     *            The level to log
     * 
     * @param appContext
     *            The current application context; may be null
     * @param markers
     *            The markers to associate logs with; may be null
     * @param t
     *            The throwable cause; may be null
     * @param prefix
     *            The message prefix; may be empty
     * @param messages
     *            The message to log
     */
    public AmpcsLog4jMessage(final TraceSeverity level, final ApplicationContext appContext, final Marker markers,
            final Throwable t, final String prefix, final Object... messages) {
        this(level, appContext, markers, t, prefix, LogMessageType.GENERAL, messages);
    }

    /**
     * Creates an AmpcsLog4jMessage object
     * 
     * @param level
     *            The level to log
     * 
     * @param appContext
     *            The current application context; may be null
     * @param markers
     *            The markers to associate logs with; may be null
     * @param t
     *            The throwable cause; may be null
     * @param prefix
     *            The message prefix; may be empty
     * @param logMsgType
     *            The log message type
     * @param messages
     *            The message to log
     */
    public AmpcsLog4jMessage(final TraceSeverity level, final ApplicationContext appContext, final Marker markers,
            final Throwable t, final String prefix, final LogMessageType logMsgType, final Object... messages) {
        super(CommonMessageType.Log, level, logMsgType);
        this.throwable = t;
        this.prefix = prefix;
        this.appContext = appContext;
        this.markers = markers;

        setContextKey(appContext == null ? new ContextKey() : appContext.getBean(IContextKey.class));
        setMessage(parseMessageObject(messages));

    }

    /**
     * @param message
     *            The IPublishableLogMessage to log
     */
    public AmpcsLog4jMessage(final IPublishableLogMessage message) {
        this(null, message);
    }

    /**
     * @param prefix
     *            The message prefix; may be null
     * @param message
     *            The IPublishableLogMessage to log
     */
    public AmpcsLog4jMessage(final String prefix, final IPublishableLogMessage message) {
        this(null, prefix, message);
    }

    /**
     * @param appContext
     *            The current application context; may be null
     * @param prefix
     *            The message prefix; may be null
     * @param message
     *            The IPublishableLogMessage to log
     */
    public AmpcsLog4jMessage(final ApplicationContext appContext, final String prefix,
            final IPublishableLogMessage message) {
        super(message.getType(), message.getSeverity(), message.getLogType());
        this.throwable = null;
        this.prefix = prefix;
        this.appContext = appContext;
        this.markers = Markers.markFromLogType(getLogType());
        setEventTime(message.getEventTime());

        setContextKey(appContext == null ? new ContextKey() : appContext.getBean(IContextKey.class));

        setMessage(parseMessageObject(message.getMessage()));
    }

    /**
     * Internal method to parse the message object
     * 
     * @param msg
     *            The message to parse
     * @return The log message as an Object
     */
    private String parseMessageObject(final Object... msg) {
        synchronized (this) {
            sb.setLength(0);

            // append prefix if there is one
            if (prefix != null && !prefix.isEmpty()) {
                sb.append(prefix);
                sb.append(" ");
            }
            if (appContext != null) {
                final LoggingProperties prop = appContext.getBean(LoggingProperties.class);
                if (prop.getContextDisplay()) {
                    sb.append("{");
                    sb.append(prop.getFormattedContext(getContextKey()));
                    sb.append("} ");
                }
            }

            for (final Object o : msg) {
                if (o instanceof Throwable) {
                    this.throwable = (Throwable) o;
                }
                else if (o != null) {
                    sb.append(o.toString());
                } else {  
                   // Append 'null' string when o is null
                    sb.append("null");
                }
            }
        }
        return sb.toString().trim();
    }
 

    @Override
    public String getFormattedMessage() {
        return toString();
    }

    @Override
    public String getFormat() {
        if (throwable != null && (severity.equals(TraceSeverity.DEBUG) || severity.equals(TraceSeverity.ERROR))) {
            return "{}\n{}";
        }
        return "{}";
    }

    @Override
    public Object[] getParameters() {
        return new Object[] {};
    }

    @Override
    public Throwable getThrowable() {
        return throwable;
    }

    @Override
    public String toString() {
        return message;
    }

    /**
     * @return Marker to log with
     */
    public Marker getMarkers() {
        return markers;
    }

    /**
     * @return The application context associated with this log event; may be
     *         null
     */
    public ApplicationContext getApplicationContext() {
        return appContext;
    }

    /**
     * Set the application context to be associated with the log event
     * 
     * @param appContext
     *            The application context to set
     */
    public void setApplicationContext(final ApplicationContext appContext) {
        this.appContext = appContext;
    }

    /**
     * 
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        }
        final AmpcsLog4jMessage fobj = (AmpcsLog4jMessage) obj;
        return (message.equals(fobj.getMessage()) && logType.equals(fobj.getLogType()) && configType.equals(fobj.getType())
                && severity.equals(fobj.getSeverity()) && markers.equals(fobj.getMarkers())
                && eventTime == fobj.eventTime
                && getContextKey().equals(fobj.getContextKey()));

    }

}
