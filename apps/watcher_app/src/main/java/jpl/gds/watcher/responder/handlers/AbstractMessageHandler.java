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
package jpl.gds.watcher.responder.handlers;

import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.context.api.IVenueConfiguration;
import jpl.gds.dictionary.api.client.FlightDictionaryLoadingStrategy;
import jpl.gds.dictionary.api.client.SseDictionaryLoadingStrategy;
import jpl.gds.message.api.external.IExternalMessageUtility;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.watcher.IMessageHandler;

/**
 * AbstractMessageHandler contains common methods for MessageHandlers
 * and can be used as a base class for all handlers.
 */
public abstract class AbstractMessageHandler implements IMessageHandler {
    
    private boolean verbose;
    
    /** current application context */
    protected ApplicationContext      appContext;
    /** trace logger */
    protected final Tracer                  trace;
    /** message utility for parsing messages */
    protected IExternalMessageUtility externalMessageUtil;

    /** indicates if this message handler must load SSE dictionaries. */
	protected boolean doSse;

    protected final SseContextFlag    sseFlag;
    
    /**
     * Constructor.
     * 
     * @param appContext the current application context.
     */
    public AbstractMessageHandler(final ApplicationContext appContext) {
    	this.appContext = appContext;
        this.trace = TraceManager.getTracer(appContext, Loggers.WATCHER);
    	this.externalMessageUtil = appContext.getBean(IExternalMessageUtility.class);
        this.sseFlag = appContext.getBean(SseContextFlag.class);

    	/**
    	 * Enable the dictionaries we want.
    	 */
        this.doSse = !appContext.getBean(IVenueConfiguration.class).getVenueType().isOpsVenue() && 
        		appContext.getBean(MissionProperties.class).missionHasSse() && 
                !sseFlag.isApplicationSse();
        
    	appContext.getBean(FlightDictionaryLoadingStrategy.class)
    	.enableChannel()
    	.enableAlarm();

    	if (doSse) {
    		appContext.getBean(SseDictionaryLoadingStrategy.class)
    		.enableChannel()
    		.enableAlarm();
    	}

    }
    
    /**
     * Logs a timestamped message to standard output if verbose mode is enabled.
     * @param message the message text to log
     */
    protected void writeLog(final String message) {
        if (verbose) {
            trace.info(message);
        }
    }
    

    /**
     * Logs a timestamped message to standard output.
     * @param message the message text to log
     */
    protected void writeInfo(final String message)
    {
        trace.info(message);
    }
    

    /**
     * Logs a timestamped message to standard output.
     *
     * @param message the message text to log
     */
    protected void writeDebug(final String message)
    {
        trace.debug(message);
    }
    
    /**
     * Logs a timestamped message to standard error as a warning.
     *
     * @param message the message text to log
     */
    protected void writeWarn(final String message)
    {
        trace.warn(message);
    }

   
    /**
     * Logs a timestamped message to standard error.
     * @param message the message text to log
     */
    protected void writeError(final String message) {
        trace.error(message);
    }
    
    /**
     * Logs a timestamped message to standard error.
     * @param message the message text to log
     * @param e the exception that caused this error
     */
    protected void writeError(final String message, final Exception e) {
        trace.error(message, e);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.watcher.IMessageHandler#setVerbose(boolean)
     */
    @Override
	public void setVerbose(final boolean enable) {
        this.verbose = enable;
    }
}
