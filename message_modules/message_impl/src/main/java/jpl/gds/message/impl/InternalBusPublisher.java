/*
 * Copyright 2006-2019. California Institute of Technology.
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

package jpl.gds.message.impl;

import jpl.gds.message.api.IInternalBusPublisher;
import jpl.gds.message.api.external.IExternalMessage;
import jpl.gds.message.api.external.IExternalMessageUtility;
import jpl.gds.message.api.spill.ISpillProcessor;
import jpl.gds.shared.exceptions.ExceptionTools;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.thread.SleepUtilities;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * This is an internal bus publisher class that is capable of queuing
 * external messages, translate them to internal messages,
 * and publishing them on a separate thread.
 *
 * It uses a SpillProcessor to spill messages on disk
 */
public class InternalBusPublisher implements IInternalBusPublisher {

    /** Time to wait for publication thread to complete. */
    private static final long PUBLICATION_THREAD_JOIN_WAIT = 10000L;

    private Tracer tracer;

    private IExternalMessageUtility messageUtil;

    /**
     * Stop toggle used to publish queued messages when a close is issued.
     */
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    /**
     * Signifies if the publisher is closed.
     */
    private final AtomicBoolean stopping = new AtomicBoolean(false);

    /**
     * Worker thread that performs the publication.
     */
    private Thread publisherThread;

    private IMessagePublicationBus bus;

    /**
     * Queue used to hold messages waiting to be sent.
     */
    private ISpillProcessor<IExternalMessage> messageToSend;

    /**
     * Constructor
     *
     * @param spillProc A spill processor bean
     * @param tracer  Tracer for logging
     * @param bus A message publication bus bean
     * @param messageUtil An external message utility bean
     *
     */
    public InternalBusPublisher(final ISpillProcessor<IExternalMessage> spillProc, final Tracer tracer,
                                final IMessagePublicationBus bus, final IExternalMessageUtility messageUtil){
        this.tracer = tracer;
        this.bus = bus;
        this.messageUtil = messageUtil;
        this.messageToSend = spillProc;

        // Start the spill processor.
        messageToSend.start();
    }

    @Override
    public synchronized void start() {
        stopped.set(false);
        stopping.set(false);
        publisherThread = new Thread(this, "Internal Bus Publisher");
        publisherThread.start();
    }

    @Override
    public void run() {
        try {
            while (!stopping.get()) {
                IExternalMessage extMessage = null;
                try {
                    //Get the next message from the queue. This will block until a message
                    //is found or the thread is interrupted

                    extMessage = messageToSend.poll();
                }
                catch (final InterruptedException e) {
                    tracer.debug("publication run thread was interrupted; probably stopping", ExceptionTools.getMessage(e), e);
                    extMessage = null;
                    Thread.currentThread().interrupt();
                }

                if (extMessage != null) {
                    // We have a message to publish
                    try {
                        final IMessage[] messages = messageUtil.instantiateMessages(extMessage);
                        for (IMessage message : messages) {
                            message.setIsExternallyPublishable(false);
                            bus.publish(message);
                        }
                    }
                    // Catch all exceptions otherwise this object can become stuck and not continue processing
                    catch (Exception e) {
                        tracer.error("Error publishing messages.", e.getMessage());
                    }
                }
            }
        }
        // Moved setting stop flag to true in finally block
        finally {
            tracer.debug("Stopping internal bus publisher. Has blacklog: ", hasBacklog());
            if (hasBacklog()) {
                tracer.warn("Internal bus Backlog size: ", messageToSend.size());
            }
            stopped.set(true);
        }
    }

    @Override
    public synchronized void queueMessageForPublication(final IExternalMessage extMessage){
            messageToSend.put(extMessage);
    }

    @Override
    public synchronized void close() {

        //Close the spill processor. This blocks and clears all queues.
        messageToSend.shutDownAndClose();

        //Tell the publication thread to stop and wait
        stopping.set(true);
        this.publisherThread.interrupt();

        SleepUtilities.checkedJoin(this.publisherThread, PUBLICATION_THREAD_JOIN_WAIT, "Internal Bus Publisher", tracer);

        stopped.set(true);
    }

    @Override
    public boolean hasBacklog() {
        return !messageToSend.isEmpty();
    }
}
