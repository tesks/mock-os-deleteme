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

import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.config.Configurator;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Slf4jTracer is used as a 'wrapper' class around SLF4J's logging API
 * 
 * DO NOT ACCESS THESE TRACERS WITHOUT TraceManager.java
 * 
 */
public class Slf4jTracer implements Tracer, ApplicationContextAware {

    private final Logger category;
    private ApplicationContext appContext;

    private String             prefix = "";

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) throws BeansException {
        this.appContext = applicationContext;
    }

    /**
     * Public constructor for SPRING FRAMEWORK to instantiate a <Tracer>.
     * 
     * DO NOT USE THIS CONSTRUCTOR DIRECTLY OR DEFINE A SLF4JTRACER.
     * 
     * Tracer t = TraceManager.getTracer(Loggers.DEFAULT);
     * 
     * @param tracer
     *            Logger name to use
     */
    public Slf4jTracer(final Logger tracer) {
        this.category = tracer;
    }

    @Override
    public void log(final Marker markers, final TraceSeverity priority, final Object message, final Throwable t) {
        logMessagesFast(priority, markers, t, message);
    }

    /**
     * Internal method for log message objects without the ILogMessage
     * interface. All non-message logs will pass through here.
     * 
     * 
     * @param level
     * @param markers
     * @param t
     * @param messages
     */
    private void logMessagesFast(final TraceSeverity level, final Marker markers, final Throwable t,
            final Object... messages) {
        if (messages == null) {
            throw new IllegalArgumentException("Message object for trace log is null");
        }
        if (level == null) {
            throw new IllegalArgumentException("Priority for trace log is null");
        }

        if (isEnabledFor(level)) {
            logMessage(new AmpcsLog4jMessage(level, appContext, markers, t, prefix, messages));
        } // silently do nothing because logging on this level is not enabled
    }


    @Override
    public void log(final IPublishableLogMessage message) {
        log(appContext, message);
    }

    /**
     * Internal method for message objects with the ILogMessage interface
     * 
     * @param appContext
     *            The current application context associated with this tracer;
     *            may be null
     * @param message
     *            The ILogMessage object to log
     */
    private void log(final ApplicationContext appContext, final IPublishableLogMessage message) {
        if (message == null || message.getMessage() == null) {
            throw new IllegalArgumentException("Message object for trace log is null");
        }
        if (message.getSeverity() == null) {
            throw new IllegalArgumentException("Priority for trace log is null");
        }

        if (isEnabledFor(message.getSeverity())) {
            logMessage(new AmpcsLog4jMessage(appContext, prefix, message));
        } // silently do nothing because logging on this level is not enabled
    }

    @Override
    public void log(final TraceSeverity priority, final Object message) {
        log(null, priority, message, null);
    }

    @Override
    public void log(final Marker markers, final TraceSeverity priority, final Object message) {
        log(markers, priority, message, null);
    }

    @Override
    public void log(final TraceSeverity priority, final Object message, final Throwable t) {
        log(null, priority, message, t);
    }

    @Override
    public void trace(final Object message) {
        log(TraceSeverity.TRACE, message);
    }

    @Override
    public void trace(final Object message, final Throwable t) {
        log(TraceSeverity.TRACE, message, t);
    }

    @Override
    public void trace(final Marker markers, final Object message) {
        log(markers, TraceSeverity.TRACE, message, null);
    }

    @Override
    public void trace(final Marker markers, final Object message, final Throwable t) {
        log(markers, TraceSeverity.TRACE, message, t);
    }

    @Override
    public void trace(final Object... messages) {
        trace(null, messages);
    }

    @Override
    public void trace(final Marker markers, final Object... messages) {
        trace(markers, null, messages);
    }

    @Override
    public void trace(final Marker markers, final Throwable t, final Object... messages) {
        logMessagesFast(TraceSeverity.TRACE, markers, t, messages);
    }

    @Override
    public void debug(final Object message) {
        log(TraceSeverity.DEBUG, message);
    }

    @Override
    public void debug(final Object message, final Throwable t) {
        log(TraceSeverity.DEBUG, message, t);
    }

    @Override
    public void debug(final Marker markers, final Object message) {
        log(markers, TraceSeverity.DEBUG, message, null);
    }

    @Override
    public void debug(final Marker markers, final Object message, final Throwable t) {
        log(markers, TraceSeverity.DEBUG, message, t);
    }

    @Override
    public void debug(final Object... messages) {
        debug(null, messages);
    }

    @Override
    public void debug(final Marker markers, final Object... messages) {
        debug(markers, null, messages);
    }

    @Override
    public void debug(final Marker markers, final Throwable t, final Object... messages) {
        logMessagesFast(TraceSeverity.DEBUG, markers, t, messages);
    }

    @Override
    public void info(final Object message) {
        log(TraceSeverity.INFO, message);
    }

    @Override
    public void info(final Object message, final Throwable t) {
        log(TraceSeverity.INFO, message, t);
    }

    @Override
    public void info(final Marker markers, final Object message) {
        log(markers, TraceSeverity.INFO, message, null);
    }

    @Override
    public void info(final Marker markers, final Object message, final Throwable t) {
        log(markers, TraceSeverity.INFO, message, t);
    }

    @Override
    public void info(final Object... messages) {
        info(null, messages);
    }

    @Override
    public void info(final Marker markers, final Object... messages) {
        info(markers, null, messages);
    }

    @Override
    public void info(final Marker markers, final Throwable t, final Object... messages) {
        logMessagesFast(TraceSeverity.INFO, markers, t, messages);
    }

    @Override
    public void warn(final Object message) {
        log(TraceSeverity.WARN, message);
    }

    @Override
    public void warn(final Object message, final Throwable t) {
        log(TraceSeverity.WARN, message, t);
    }

    @Override
    public void warn(final Marker markers, final Object message) {
        log(markers, TraceSeverity.WARN, message, null);
    }

    @Override
    public void warn(final Marker markers, final Object message, final Throwable t) {
        log(markers, TraceSeverity.WARN, message, t);
    }

    @Override
    public void warn(final Object... messages) {
        warn(null, messages);
    }

    @Override
    public void warn(final Marker markers, final Object... messages) {
        warn(markers, null, messages);
    }

    @Override
    public void warn(final Marker markers, final Throwable t, final Object... messages) {
        logMessagesFast(TraceSeverity.WARN, markers, t, messages);
    }

    @Override
    public void error(final Object message) {
        log(TraceSeverity.ERROR, message);
    }

    @Override
    public void error(final Object message, final Throwable t) {
        log(TraceSeverity.ERROR, message, t);
    }

    @Override
    public void error(final Marker markers, final Object message) {
        log(markers, TraceSeverity.ERROR, message, null);
    }

    @Override
    public void error(final Marker markers, final Object message, final Throwable t) {
        log(markers, TraceSeverity.ERROR, message, t);
    }

    @Override
    public void error(final Object... messages) {
        error(null, messages);
    }

    @Override
    public void error(final Marker markers, final Object... messages) {
        error(markers, null, messages);
    }

    @Override
    public void error(final Marker markers, final Throwable t, final Object... messages) {
        logMessagesFast(TraceSeverity.ERROR, markers, t, messages);
    }

    @Override
    @Deprecated
    public void fatal(final Object message) {
        error(message);
    }

    @Override
    @Deprecated
    public void fatal(final Object message, final Throwable t) {
        error(message, t);
    }

    @Override
    public void setLevel(final TraceSeverity level) {
        Configurator.setLevel(category.getName(), getLevelFromSeverity(level));
    }

    /**
     * Converts TraceSeverity levels to Log4j2
     * 
     * @param lvl
     *            TraceSeverity level
     * @return Level The corresponding Log4j2 level
     */
    @SuppressWarnings("deprecation")
    private Level getLevelFromSeverity(final TraceSeverity lvl) {
        switch (lvl) {
            case ALL:
            case TRACE:
                return Level.TRACE;
            case DEBUG:
                return Level.DEBUG;
            case WARN:
            case WARNING:
                return Level.WARN;
            case INFO:
            case USER:
                return Level.INFO;
            case ERROR:
            case FATAL:
                return Level.ERROR;
            case OFF:
                return Level.OFF;
            default:
                return Level.INFO;
        }
    }

    @Override
    public boolean isEnabledFor(final TraceSeverity priority) {
        return isEnabledFor(priority, null);
        
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean isEnabledFor(final TraceSeverity priority, final Marker markers) {
        switch (priority) {
            case ALL:
            case TRACE:
                return category.isTraceEnabled(markers);
            case DEBUG:
                return category.isDebugEnabled(markers);
            case WARNING:
            case WARN:
                return category.isWarnEnabled(markers);
            case INFO:
            case USER:
                return category.isInfoEnabled(markers);
            case ERROR:
            case FATAL:
                return category.isErrorEnabled(markers);
            case OFF:
                return Boolean.FALSE.equals(category.isTraceEnabled(markers));
            default:
                return category.isTraceEnabled(markers);
        }
    }

    @Override
    public boolean isDebugEnabled() {
        return this.category.isDebugEnabled();
    }

    @Override
    public void setPrefix(final String prefix) {
        this.prefix = (prefix == null ? "" : prefix);
    }

    @Override
    public String getPrefix() {
        return this.prefix;
    }

    @Override
    public String getName() {
        return this.category.getName();
    }

    @Override
    public ApplicationContext getAppContext() {
        return this.appContext;
    }

    @Override
    public void setAppContext(final ApplicationContext appContext) {
        this.appContext = appContext;
    }

    /**
     * Internal method to log the AmpcsLog4jMessage object All log statements
     * end up coming through here
     * 
     * @param msg
     *            The Internal AmpcsLog4jMessage object
     */
    @SuppressWarnings("deprecation")
    private void logMessage(final AmpcsLog4jMessage msg) {
        switch (msg.getSeverity()) {
            case TRACE:
                category.trace(msg.getMarkers(), msg.getFormat(), msg);
                break;
            case DEBUG:
                category.debug(msg.getMarkers(), msg.getFormat(), msg,
                        msg.getThrowable() != null && msg.getThrowable().getStackTrace() != null
                                ? Arrays.stream(msg.getThrowable().getStackTrace())
                                .map(StackTraceElement::toString).collect(Collectors.joining("\n")) : null);
                break;
            case INFO:
            case USER:
                category.info(msg.getMarkers(), msg.getFormat(), msg);
                break;
            case WARNING:
            case WARN:
                category.warn(msg.getMarkers(), msg.getFormat(), msg);
                break;
            case ERROR:
            case FATAL:
                category.error(msg.getMarkers(), msg.getFormat(), msg,
                        msg.getThrowable() != null && msg.getThrowable().getStackTrace() != null
                                ? Arrays.stream(msg.getThrowable().getStackTrace())
                                .map(StackTraceElement::toString).collect(Collectors.joining("\n")) : null);
                break;
            default:
                category.trace(msg.getMarkers(), msg.getFormat(), msg);
        } // end switch
    }


}
