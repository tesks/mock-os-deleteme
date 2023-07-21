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
package jpl.gds.shared.log.config;

import jpl.gds.shared.config.GdsHierarchicalProperties;
import jpl.gds.shared.metadata.context.IContextKey;
import jpl.gds.shared.spring.context.flag.SseContextFlag;

/**
 *
 * Logging properties
 *
 */
public class LoggingProperties extends GdsHierarchicalProperties {
    private static final String PROPERTY_PREFIX                 = "logging.";
    private static final String PROPERTY_FILE                   = PROPERTY_PREFIX + "properties";
    private static final String CONTEXT_PROPERTY                = "context.";
    private static final String DISPLAY_PROPERTY                = "display";
    private static final String CONTEXT_BLOCK                   = PROPERTY_PREFIX + CONTEXT_PROPERTY;
    private static final String CONTEXT_DISPLAY_PROPERTY        = CONTEXT_BLOCK + DISPLAY_PROPERTY;
    private static final String CONTEXT_DISPLAY_FORMAT_PROPERTY = CONTEXT_BLOCK + DISPLAY_PROPERTY + ".format";
    private static final String CONTEXT_SEPARATOR                 = ":";

    /** Default display format for context id in log messages */
    public static final String  DEFAULT_FORMAT                  = "short";
    private final String        format;
    private final boolean       enabled;

    /**
     * Test constructor
     * 
     */
    public LoggingProperties() {
        this(new SseContextFlag());
    }

    /**
     * Logging Configuration properties
     * 
     * @param sseFlag
     *            The SSE Context flag
     */
    public LoggingProperties(final SseContextFlag sseFlag) {
        super(PROPERTY_FILE, sseFlag);
        
        this.enabled = getBooleanProperty(CONTEXT_DISPLAY_PROPERTY, true);
        this.format = getProperty(CONTEXT_DISPLAY_FORMAT_PROPERTY, DEFAULT_FORMAT);
    }

    /**
     * @return true if we want to display context id with log messages when
     *         possible
     */
    public boolean getContextDisplay() {
        return this.enabled;
    }

    /**
     * @return The context id format to log with; short or long
     */
    public String getContextFormat() {
        return this.format;
    }
    
    @Override
    public String getPropertyPrefix(){
        return PROPERTY_PREFIX;
    }

    /**
     * Gets the formatted context id for logging from the LoggingProperties config
     * For the short context, the format is sessionId:contextId, or just sessionId if no parent is present
     * For the long context, the format is number/host/hostId/fragment/parentNumber/parentHostId
     *
     * @param context
     *            The IContextKey to get the identifier from
     * @return A ContextId in long or short format
     */
    public String getFormattedContext(final IContextKey context) {
        long parentNumber = context.getParentNumber() == null ? 0 : context.getParentNumber();
        String shortContext = parentNumber == 0 ? String.valueOf(context.getShortContextId()) :
                context.getShortContextId() + CONTEXT_SEPARATOR + parentNumber;
        return this.format.equals(DEFAULT_FORMAT) ? shortContext : context.getContextId();
    }

}
