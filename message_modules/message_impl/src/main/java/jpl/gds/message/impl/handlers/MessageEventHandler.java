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

import java.util.Collection;

import org.springframework.context.ApplicationContext;

import com.lmax.disruptor.EventHandler;

import jpl.gds.message.api.external.IMessageServiceListener;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;

/**
 * Message event handler for the disruptor-based queuing message handler.
 */
public class MessageEventHandler implements EventHandler<MessageReceiptEvent>{
    
    private final Tracer trace;
    
    private final Collection<IMessageServiceListener> clients;

    /**
     * Constructor.
     * 
     * @param appContext the current application context
     * @param listeners message service listeners to send the event to
     */
    public MessageEventHandler(final ApplicationContext appContext, final Collection<IMessageServiceListener> listeners) {
        this.clients = listeners;
        this.trace = TraceManager.getTracer(appContext, Loggers.BUS);
    }
    
    @Override
    public void onEvent(final MessageReceiptEvent event, final long sequence, final boolean endOfBatch) throws Exception {
        if (event.getData() != null) {
            try {
                clients.forEach((c)->c.onMessage(event.getData()));
            } catch (final Throwable e) {
                trace.error("Unexpected error in message handler: " + ExceptionTools.rollUpMessages(e), e);
            }
        }
        event.setData(null);       
    }
}
