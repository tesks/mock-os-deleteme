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
package jpl.gds.time.api;

/**
 * An interface that identifies beans in the Spring configuration for the 
 * time_correlation projects.
 * 
 *
 * @since R8
 */
public interface TimeCorrelationApiBeans {
    /** Bean name for the time correlation service. */  
    public static final String TIME_CORRELATION_SERVICE = "TIME_CORRELATION_SERVICE";
    /** Bean name for the time correlation packet parser. */  
    public static final String TIME_CORRELATION_PARSER = "TIME_CORRELATION_PARSER";
    /** Bean name for the time correlation message factory. */  
    public static final String TIME_CORRELATION_MESSAGE_FACTORY = "TIME_CORRELATION_MESSAGE_FACTORY";
    /** Bean name for time correlation properties. */
    public static final String TIME_CORRELATION_PROPERTIES = "TIME_CORRELATION_PROPERTIES";
}
