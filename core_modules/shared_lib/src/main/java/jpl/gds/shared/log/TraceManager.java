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

import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configurator;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationContext;

import java.util.concurrent.TimeUnit;

/**
 * This class provides a Tracer implementation that uses the Apache SLF4J
 * package. Users should not create an instance of this class or use it
 * directly. The Tracer interface should be employed instead.
 * 
 */
public class TraceManager {

    /**
     * Gets a <Tracer> WITHOUT an ApplicationContext for logging basic messages
     * 
     * NOTE: In order for messages to be routed to JMS, DB, GUI, etc, a <Tracer> should
     * be instantiated with an ApplicationContext
     * 
     * @param name
     *            <Logger> name to use
     * @return <Tracer> logger
     */
    public static Tracer getTracer(final Loggers name) {
        return new Slf4jTracer(LoggerFactory.getLogger(name.toString()));
    }
    
    /**
     * Gets a <Tracer> for logging messages to console, file, database, GUI, jms, etc.
     * 
     * @param appContext
     *            The curent application context to attach to the Tracer
     * @param name
     *            <Logger> name to use
     * @return <Tracer> logger
     */
    public static Tracer getTracer(final ApplicationContext appContext, final Loggers name) {
        try {
            // Try and instantiate Tracer as a bean
            return appContext.getBean(Tracer.class, name);
        }
        catch (final Exception e) {
            // If it fails for some reason, get a non-bean Tracer with no ApplicationContext
            return new Slf4jTracer(LoggerFactory.getLogger(name.toString()));
        }
    }
	
    /**
     * Gets the default <Tracer> WITHOUT an ApplicationContext for logging basic messages
     * 
     * NOTE: In order for messages to be routed to JMS, DB, GUI, etc, a <Tracer> should
     * be instantiated with an ApplicationContext
     * 
     * @return The default <Tracer> Logger
     */
	public static Tracer getDefaultTracer() { 
        return getTracer(Loggers.DEFAULT);
    }

    /**
     * Gets the default <Tracer> WITHOUT an ApplicationContext for logging basic messages
     * 
     * NOTE: In order for messages to be routed to JMS, DB, GUI, etc, a <Tracer> should
     * be instantiated with an ApplicationContext
     * 
     * @param appContext
     *            The curent application context to attach to the Default Tracer
     * @return The default <Tracer> Logger
     */
    public static Tracer getDefaultTracer(final ApplicationContext appContext) {
        return getTracer(appContext, Loggers.DEFAULT);
    }


    /**
     * CALL THIS METHOD WITH EXTREME CAUTION. This should be used in shutdown
     * hook logic for SIGTERM signals (CTRL+C).
     * 
     * 
     * Shuts down the supplied Logger Context
     * 
     * @param context
     *            The logger context to shut down
     * 
     */
    public static void shutdown(final LoggerContext context) {
        if (!context.isStopping() && context.isStarted()) {
            Configurator.shutdown(context, 3000L, TimeUnit.MILLISECONDS);
        }
    }

    public static TraceSeverity mapMtakLevel(String level) {
        //MTAK levels: DEBUG,INFO,WARNING,ERROR,CRITICAL
        level = level.toUpperCase();
        if (("TRACE").equals(level)) {
            return TraceSeverity.TRACE;
        }
        if ("INFO".equals(level)) {
            return (TraceSeverity.INFO);
        }
        else if ("WARNING".equals(level) || "WARN".equals(level)) {
            return (TraceSeverity.WARNING);
        }
        else if ("ERROR".equals(level)) {
            return (TraceSeverity.ERROR);
        }
        else if ("CRITICAL".equals(level)) {
            return (TraceSeverity.FATAL);
        }
        else if ("DEBUG".equals(level)) {
            return (TraceSeverity.DEBUG);
        }

        return (null);
    }
}
