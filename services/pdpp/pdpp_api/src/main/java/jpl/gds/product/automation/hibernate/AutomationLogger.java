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
package jpl.gds.product.automation.hibernate;

import org.springframework.context.ApplicationContext;

import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.TraceSeverity;
import jpl.gds.shared.log.Tracer;

/**
 * Need a way to make the log messages in the database make sense.  This class takes some arguments and will
 * add some fields to the log messages to indicate where they came from.  
 * 
 * 06/15/16 - MPCS-8179 - Added to AMPCS, updated from original version in MPCS for MSL G9.
 *                     Updated variable names to more appropriately reflect AMPCS usage.
 */
public class AutomationLogger implements IAutomationLogger {
	
	/**
	 * Character used to separate ProductAutomationTracer message elements
	 */
	public static final char SEPERATOR_STRING = ';';
	private static final Long ARBITER_PROCESSOR_ID = new Long(0);
	
    private final Tracer      trace;
	
	
	/**
	 * Default constructor. Utilizes the "default" ProductAutomationTracer
	 */
	public AutomationLogger() {
        this(null);
	}
	
    /**
     * @param appContext
     */
    public AutomationLogger(final ApplicationContext appContext) {
        trace = TraceManager.getTracer(appContext, Loggers.PDPP);
    }
	
	//Arbiter loggers
	/**
	 * Publishes a fatal level arbiter message
	 * 
	 * @param message arbiter message to be published
	 */
	@Override
	@Deprecated
	public void fatal(final String message) {
		logMaster(TraceSeverity.FATAL, message, ARBITER_PROCESSOR_ID);
	}

	/**
	 * Publishes an error level arbiter message
	 * 
	 * @param message arbiter message to be published
	 */
	@Override
	public void error(final String message) {
		logMaster(TraceSeverity.ERROR, message, ARBITER_PROCESSOR_ID);
	}
	
	/**
	 * Publishes a warning level arbiter message
	 * 
	 * @param message arbiter message to be published
	 */
	@Override
	public void warn(final String message) {
		logMaster(TraceSeverity.WARNING, message, ARBITER_PROCESSOR_ID);
	}
	
	/**
	 * Publishes an info level arbiter message
	 * 
	 * @param message arbiter message to be published
	 */
	@Override
	public void info(final String message) {
		logMaster(TraceSeverity.INFO, message, ARBITER_PROCESSOR_ID);
	}
	
	/**
	 * Publishes a debug level arbiter message
	 * 
	 * @param message arbiter message to be published
	 */
	@Override
	public void debug(final String message) {
		logMaster(TraceSeverity.DEBUG, message, ARBITER_PROCESSOR_ID);
	}
	
	/**
	 * Publishes a trace level arbiter message
	 * 
	 * @param message arbiter message to be published
	 */
	@Override
	public void trace(final String message) {
		logMaster(TraceSeverity.TRACE, message, ARBITER_PROCESSOR_ID);
	}
	
	// Processor loggers
	/**
	 * Publishes a fatal level processor message
	 * 
	 * @param message processor message to be published
	 * 
	 * @param processorId processor id of the process publishing the message
	 */
	@Override
	@Deprecated
	public void fatal(final String message, final Long processorId) {
		logMaster(TraceSeverity.FATAL, message, processorId);
	}

	/**
	 * Publishes an error level processor message
	 * 
	 * @param message processor message to be published
	 * 
	 * @param processorId processor id of the process publishing the message
	 */
	@Override
	public void error(final String message, final Long processorId) {
		logMaster(TraceSeverity.ERROR, message, processorId);
	}

	/**
	 * Publishes a warning level processor message
	 * 
	 * @param message processor message to be published
	 * 
	 * @param processorId processor id of the process publishing the message
	 */
	@Override
	public void warn(final String message, final Long processorId) {
		logMaster(TraceSeverity.WARNING, message, processorId);
	}

	/**
	 * Publishes an info level processor message
	 * 
	 * @param message processor message to be published
	 * 
	 * @param processorId processor id of the process publishing the message
	 */
	@Override
	public void info(final String message, final Long processorId) {
		logMaster(TraceSeverity.INFO, message, processorId);
	}

	/**
	 * Publishes a debug level processor message
	 * 
	 * @param message processor message to be published
	 * 
	 * @param processorId processor id of the process publishing the message
	 */
	@Override
	public void debug(final String message, final Long processorId) {
		logMaster(TraceSeverity.DEBUG, message, processorId);
	}

	/**
	 * Publishes a trace level processor message
	 * 
	 * @param message processor message to be published
	 * 
	 * @param processorId processor id of the process publishing the message
	 */
	@Override
	public void trace(final String message, final Long processorId) {
		logMaster(TraceSeverity.TRACE, message, processorId);
	}
	
	/*
	 * MPCS-4330 - Adding loggers that include the product ID in them as well as the processor id.
	 */
	/**
	 * Publishes a fatal level processor message with a product ID
	 * 
	 * @param message processor message to be published
	 * 
	 * @param processorId processor id of the process publishing the message
	 * 
	 * @param productId product id of the product that this message is concerning
	 */
	@Override
	@Deprecated
	public void fatal(final String message, final Long processorId, final Long productId) {
		logMaster(TraceSeverity.FATAL, message, processorId, productId);
	}

	/**
	 * Publishes an error level processor message with a product ID
	 * 
	 * @param message processor message to be published
	 * 
	 * @param processorId processor id of the process publishing the message
	 * 
	 * @param productId product id of the product that this message is concerning
	 */
	@Override
	public void error(final String message, final Long processorId, final Long productId) {
		logMaster(TraceSeverity.ERROR, message, processorId, productId);
	}

	/**
	 * Publishes a warning level processor message with a product ID
	 * 
	 * @param message processor message to be published
	 * 
	 * @param processorId processor id of the process publishing the message
	 * 
	 * @param productId product id of the product that this message is concerning
	 */
	@Override
	public void warn(final String message, final Long processorId, final Long productId) {
		logMaster(TraceSeverity.WARNING, message, processorId, productId);
	}

	/**
	 * Publishes an info level processor message with a product ID
	 * 
	 * @param message processor message to be published
	 * 
	 * @param processorId processor id of the process publishing the message
	 * 
	 * @param productId product id of the product that this message is concerning
	 */
	@Override
	public void info(final String message, final Long processorId, final Long productId) {
		logMaster(TraceSeverity.INFO, message, processorId, productId);
	}

	/**
	 * Publishes a debug level processor message with a product ID
	 * 
	 * @param message processor message to be published
	 * 
	 * @param processorId processor id of the process publishing the message
	 * 
	 * @param productId product id of the product that this message is concerning
	 */
	@Override
	public void debug(final String message, final Long processorId, final Long productId) {
		logMaster(TraceSeverity.DEBUG, message, processorId, productId);
	}

	/**
	 * Publishes a trace level processor message with a product ID
	 * 
	 * @param message processor message to be published
	 * 
	 * @param processorId processor id of the process publishing the message
	 * 
	 * @param productId product id of the product that this message is concerning
	 */
	@Override
	public void trace(final String message, final Long processorId, final Long productId) {
		logMaster(TraceSeverity.TRACE, message, processorId, productId);
	}	

	/**
	 * Publishes a fatal level processor message with a product ID
	 * 
	 * @param message processor message to be published
	 * 
	 * @param processorId processor id of the process publishing the message
	 * 
	 * @param productId product id of the product that this message is concerning
	 */
	private void logMaster(final TraceSeverity level, final String message, final Long processordId) {
		logMaster(level, message, processordId, null);
	}
	
	/**
	 * Constructs and published the message
	 * If any of the values are null, replaces with an empty string.
	 * 
	 * @param level severity of the message to be published
	 * @param message message to be published
	 * @param processorId processor id of the process publishing the message
	 * @param productId product id of the product that this message is concerning
	 */
	private void logMaster(final TraceSeverity level, final String message, final Long processorId, final Long productId) {
		if (message == null) {
			return;
		}
		
		// Expected format in the string is (message, processorId)
		String msg = message;
		
		// MPCS-4330 - Deal with product.  The appender can handle the cases where thing are not given, 
		// so changing this up a bit.
		if (processorId == null) {
			msg = msg + SEPERATOR_STRING + "";
		} else {
			msg = msg + SEPERATOR_STRING + processorId;
		}
		
		if (productId == null) {
			msg = msg + SEPERATOR_STRING + "";
		} else {
			msg = msg + SEPERATOR_STRING + productId;
		}		
		
		
		trace.log(level, msg);
	}

	@Override
	public Tracer getTracer() {
		return trace;
	}
}
