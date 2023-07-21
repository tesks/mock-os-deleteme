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
package jpl.gds.message.impl.handlers;

import com.lmax.disruptor.ExceptionHandler;

import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Tracer;

/**
 * Error handler for the disruptor implementation of the queuing message handler.
 */
public class MessageEventErrorHandler implements ExceptionHandler<MessageReceiptEvent> {
    
    private final Tracer tracer;

    /**
     * Constructor.
     * 
     * @param log trace logger for error messages
     */
    public MessageEventErrorHandler(final Tracer log) {
        this.tracer = log;
    }

    @Override
    public void handleEventException(final Throwable ex, final long sequence, final MessageReceiptEvent event) {
        tracer.error("Error handling message receipt event: " + ExceptionTools.getMessage(ex), ex);    
    }

    @Override
    public void handleOnStartException(final Throwable ex) {
        tracer.error("Error during disruptor startup: " + ExceptionTools.getMessage(ex), ex);
    }

    @Override
    public void handleOnShutdownException(final Throwable ex) {
        tracer.error("Error during disruptor shutdown: " + ExceptionTools.getMessage(ex), ex);       
    }

}
