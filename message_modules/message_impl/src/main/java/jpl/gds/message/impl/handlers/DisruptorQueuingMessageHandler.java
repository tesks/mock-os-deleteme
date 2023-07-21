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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.context.ApplicationContext;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.TimeoutException;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;

import jpl.gds.message.api.external.IExternalMessage;
import jpl.gds.message.api.external.IMessageClientFactory;
import jpl.gds.message.api.external.IMessageServiceListener;
import jpl.gds.message.api.external.ITopicSubscriber;
import jpl.gds.message.api.external.MessageServiceException;
import jpl.gds.message.api.handler.IQueuingMessageHandler;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;

/**
 * A queuing message handler with one message service subscription as the producer and
 * multiple consumers. Uses a disruptor.
 */
public class DisruptorQueuingMessageHandler implements IQueuingMessageHandler, IMessageServiceListener {
    
    private final Tracer tracer;
    private Disruptor<MessageReceiptEvent> disruptor;    
    private RingBuffer<MessageReceiptEvent> ringBuffer;
    private ITopicSubscriber subscriber;
    private final Collection<IMessageServiceListener> listeners = new CopyOnWriteArrayList<>();  
    private final ApplicationContext appContext;
    private final IMessageClientFactory clientFactory;
    private final int ringBufferSize;
    private ExecutorService executor;

    private final AtomicBoolean started = new AtomicBoolean(false);
    
    /**
     * Constructor.
     * 
     * @param appContext the current application context
     * @param queueSize length of the handler queue
     */
    public DisruptorQueuingMessageHandler(final ApplicationContext appContext, final int queueSize) {
        this.appContext = appContext;
        this.clientFactory = appContext.getBean(IMessageClientFactory.class);
        this.ringBufferSize = queueSize;
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
        
        executor = Executors.newCachedThreadPool();

        disruptor = new Disruptor<>(MessageReceiptEvent.DATA_EVENT_FACTORY,
                this.ringBufferSize, 
                executor,
                ProducerType.SINGLE,
                new BlockingWaitStrategy());

        final EventHandler<MessageReceiptEvent>[] handlers = new MessageEventHandler[1];
        handlers[0] = new MessageEventHandler(appContext, listeners);
     
        disruptor.handleEventsWith(handlers);
        disruptor.handleExceptionsWith(new MessageEventErrorHandler(tracer));
        
        disruptor.start();
        
        this.ringBuffer = disruptor.getRingBuffer();
    
        subscriber.start();
    }

    @Override
    public synchronized void shutdown(final boolean abortSubscribers, final boolean waitToClear) {
        if (!started.get()) {
            return;
        }
        
        if (abortSubscribers) {
            subscriber.closeNoDisconnect();
        } else {
            subscriber.close();
        }
        subscriber = null;
        
        disruptor.halt();
        if (waitToClear) {
            disruptor.shutdown(); 
        } else {
            try {
                disruptor.shutdown(1, TimeUnit.MILLISECONDS);
            } catch (final TimeoutException e) {
                tracer.debug("Disruptor shutdown timeout");
            }
        }
        
        executor.shutdown();

        started.set(false);
    }

    @Override
    public void onMessage(final IExternalMessage message) {
        ringBuffer.publishEvent(MessageReceiptEvent.DATA_TRANSLATOR, message);
    }

    @Override
    public boolean hasBacklog() {
        return ringBufferSize != ringBuffer.remainingCapacity();
    }
}

