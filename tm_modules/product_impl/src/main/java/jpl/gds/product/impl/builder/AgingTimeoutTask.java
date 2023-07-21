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
package jpl.gds.product.impl.builder;

import java.util.TimerTask;

import jpl.gds.product.api.IProductPartProvider;
import jpl.gds.product.impl.message.AgingTimeoutMessage;
import jpl.gds.product.impl.message.ForcePartialMessage;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessagePublicationBus;

/**
 * AgingTimeoutTask is an extension of TimerTask that is created for a product
 * builder transaction. If new product parts are not seen before the timer goes
 * off or is canceled, a ForcePartialMessage or AgingTimeoutMessage is issued to
 * the internal message bus for the product in question. The publication of an
 * aging timeout means that the no packets have been seen for the data product 
 * in question during the project-configuration product timeout period. The 
 * publication of a force partial generally means we are shutting down, and all
 * products must be forced out before the process exits.
 * 
 */
public class AgingTimeoutTask extends TimerTask {
    private static final Tracer          log    = TraceManager.getTracer(Loggers.TLM_PRODUCT);


    private final IMessagePublicationBus messageContext;
    private final IProductPartProvider part;
    private boolean active = true;
    private boolean forced;
    @SuppressWarnings("unused")
    private final int vcid;
    
    /**
     * Creates an instance of AgingTimeoutTask.
     * @param part the product part associated with this task
     * @param vcid the ID of the virtual channel on which the product data is received
     * @param bus the internal message publication bus to use
     */
    public AgingTimeoutTask(final IProductPartProvider part,
                            final int vcid, final IMessagePublicationBus bus)
    {
        this.messageContext = bus;
        this.part = part;
        
        /*
         *  Was not initializing VCID, so Partial Data Products
         * with a non-zero VCID will never close-out, and remain in "In Progress" state 
         * in chill_monitor.
         */
        this.vcid = vcid;
    }

    /**
     * Sets the flag indicating whether a partial product should be forced out
     * by the product builder.
     * 
     * @param val true if the partial should be forced
     */
    public synchronized void setForced(final boolean val) {
        this.forced = val;
    }
    
    /**
     * {@inheritDoc}
     * @see java.util.TimerTask#run()
     */
    @Override
    public void run() {
    	sendTimeoutMessage();
    }
    
    /**
     * {@inheritDoc}
     * @see java.util.TimerTask#cancel()
     */
    @Override
    public synchronized boolean cancel() {
    	try {
    		// set the active flag to false at this point, just as an added precaution.
    		this.active = false;
    	    return super.cancel();
    	} catch (final IllegalStateException ex) {}
    	return true;
    }
    
    /**
     * Creates the appropriate message to force product creation. 
     * 
     * @return the timeout message
     */
    public IMessage getTimeoutMessage() {
        IMessage message = null;
        if (this.forced) {
            log.debug(part.getTransactionId() + ": Product generation forced");
            message = new ForcePartialMessage(this.part);
        } else {
            log.debug(part.getTransactionId() + ": Aging timer expired");
            message = new AgingTimeoutMessage(this.part);
        }
        
        return message;
    }
    
    private synchronized void sendTimeoutMessage() {
        if (!this.active) {
            return;
        }

        final IMessage message = this.getTimeoutMessage();
        this.messageContext.publish(message);
        this.active = false;
    }
}
