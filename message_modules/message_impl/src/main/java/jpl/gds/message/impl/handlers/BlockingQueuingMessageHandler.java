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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.context.ApplicationContext;

import jpl.gds.message.api.external.IExternalMessage;
import jpl.gds.message.api.external.IMessageClientFactory;
import jpl.gds.message.api.external.IMessageServiceListener;
import jpl.gds.message.api.external.ITopicSubscriber;
import jpl.gds.message.api.external.MessageServiceException;
import jpl.gds.message.api.handler.IQueuingMessageHandler;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;

/**
 * A queuing message handler with one message service subscription as the producer and
 * multiple consumers. Uses a blocking queue.
 */
public class BlockingQueuingMessageHandler implements IQueuingMessageHandler, IMessageServiceListener {
    
    private static final int SHUTDOWN_SLEEP = 250;
    
    private final Tracer tracer;  
    private BlockingQueue<IExternalMessage> queue;
    private ITopicSubscriber subscriber;
    private final Collection<IMessageServiceListener> listeners = new CopyOnWriteArrayList<>();
    private final IMessageClientFactory clientFactory;
    private final int queueSize;
    private Thread notifierThread;

    private final AtomicBoolean started = new AtomicBoolean(false);
    private final AtomicBoolean stopping = new AtomicBoolean(false);
    private final AtomicBoolean stopped = new AtomicBoolean(false);
    
    private boolean waitToClear;
    
    /**
     * Constructor.
     * 
     * @param appContext the current application context
     * @param queueSize length of the handler queue
     */
    public BlockingQueuingMessageHandler(final ApplicationContext appContext, final int queueSize) {
        this.clientFactory = appContext.getBean(IMessageClientFactory.class);
        this.queueSize = queueSize;
        tracer = TraceManager.getTracer(appContext, Loggers.BUS);
    }
    
    @Override
    public synchronized void addListener(final IMessageServiceListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }

    }

    @Override
    public synchronized void removeListener(final IMessageServiceListener listener) {
        listeners.remove(listener);

    }

    @Override
    public synchronized void clearListeners() {
        listeners.clear();

    }

    @Override
    public synchronized void setSubscription(final String topic, final String filter, final boolean sharedConnection) throws MessageServiceException {
        
        if (started.get()) {
            throw new IllegalStateException("Cannot set subscription. Handler is already started");
        }
        
        if (subscriber != null) {
            throw new IllegalStateException("Subscription is already set");
        }
        
        subscriber = this.clientFactory.getTopicSubscriber(topic, filter, sharedConnection);
        subscriber.setMessageListener(this);

    }

    @Override
    public synchronized void start() throws MessageServiceException {
        
        if (started.getAndSet(true)) {
            throw new IllegalStateException("handler is already started");
        }
        
        if (subscriber == null) {
            throw new IllegalStateException("no subscriber set for handler");
        }
        
        stopped.set(false);
        
        queue = new ArrayBlockingQueue<>(queueSize);
        
        this.notifierThread = new Thread(new NotifyThread());
        this.notifierThread.start();    
    
        subscriber.start();
    }

    @Override
    public synchronized void shutdown(final boolean abortSubscribers, final boolean waitToClear) {
        if (!started.get()) {
            return;
        }
        
        this.waitToClear = waitToClear;
        
        if (abortSubscribers) {
            subscriber.closeNoDisconnect(); 
        } else {
            subscriber.close();
        }
        subscriber = null;
        
        stopping.set(true);
        
        notifierThread.interrupt();
        
        while (!stopped.get()) {
            synchronized(stopped) {
                try {
                    stopped.wait(SHUTDOWN_SLEEP);
                } catch (final InterruptedException e) {
                    tracer.debug(e);
                    Thread.currentThread().interrupt();
                }
            }
        }

        started.set(false);
    }

    @Override
    public void onMessage(final IExternalMessage message) {
        try {
            queue.put(message);
        } catch (final InterruptedException e) {
            tracer.debug("On message method in BlockingQueuingMessageHandler was interrupted", e);
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * Runnable class that processes messages in the queue and sends them to listeners.
     */
    private class NotifyThread implements Runnable {

        @Override
        public void run() {
            while (!stopping.get() || (stopping.get() && waitToClear && !queue.isEmpty())) {
                try {
                    final IExternalMessage msg = queue.take();
                    if (msg != null) {
                        for (final IMessageServiceListener l: listeners) {
                            try { 
                                l.onMessage(msg);
                            } catch (final Exception e) {
                                tracer.error("Error in message handler: " + ExceptionTools.getMessage(e), e);
                            }
                        }
                    }
                } catch (final InterruptedException e) {
                    tracer.debug("Run method in BlockingQueuingMessageHandler was interrupted", e);
                    Thread.currentThread().interrupt();
                }
            }
            synchronized(stopped) {
                stopped.set(true);  
                stopped.notifyAll();
            }
        }      
    }

    @Override
    public boolean hasBacklog() {
        return !queue.isEmpty();
    }
}

