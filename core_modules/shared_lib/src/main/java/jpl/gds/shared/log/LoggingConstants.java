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

/**
 * Constants class for AMPCS Logging implementation
 * 
 *
 */
public final class LoggingConstants {
    private LoggingConstants() { }

    /**
     * The APP file routing key to use in the current ThreadContext MDC Map. If
     * the key appears in the map then file appenders will use the value as the
     * output file
     */
    public static final String FILE_APP_LOG_PATH = "APP_LOG_PATH";
    /** DOT CONSTANT */
    public static final String        DOT                   = ".";
    /** ROOT AMPCS Logger */
    public static final String ROOT_LOGGER       = "jpl.gds.loggers";
    /** LOGGING PACKAGE CONSTANT */
    public static final String PACKAGE_PREFIX    = ROOT_LOGGER + DOT;
    /** AMPCS base services Tracer block constant */
    public static final String        SERVICE_BLOCK    = PACKAGE_PREFIX + "services" + DOT;
    /** AMPCS base db LDI services Tracer block constant */
    public static final String        DB_BLOCK         = PACKAGE_PREFIX + "services" + DOT + "db";
    /** AMPCS base telemetry services Tracer block constant */
    public static final String        TLM_BLOCK        = PACKAGE_PREFIX + "tlm" + DOT;

}
