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

import java.util.Hashtable;
import java.util.Timer;
import java.util.TimerTask;

import jpl.gds.product.api.IProductPartProvider;
import jpl.gds.product.api.builder.IProductBuilderService;
import jpl.gds.product.api.config.IProductPropertiesProvider;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessagePublicationBus;

/**
 * The product scheduler is a hash table that stores timers keyed off of a 
 * transaction id.  This provides methods to reset and cancel timers using 
 * product parts.
 * 
 * @see IProductBuilderService
 * This class is no longer a message handler, it is expected to
 * be an instance variable and used directly.
 */
public class ProductScheduler  extends Hashtable<String, TimerTask> {
	private static final long serialVersionUID = 4511652505905768178L;

    private static final Tracer          trace               = TraceManager
            .getTracer(Loggers.TLM_PRODUCT);

    
    private final Timer timer;
    private int agingTimeoutSeconds = 60; // default
    private final int vcid;
    private final IMessagePublicationBus bus;
    
    /**
     * Creates an instance of ProductScheduler for a specific virtual channel.
     * @param config the product builder configuration object from the associated 
     * product builder
     * @param vcid the virtual channel ID of products to monitor
     * @param bus the internal message publication bus to use
     */
    public ProductScheduler(final IProductPropertiesProvider config, final int vcid, final IMessagePublicationBus bus) {
        this.vcid = vcid;
        this.agingTimeoutSeconds = config.getAgingTimeout();
        this.bus = bus;
        
        this.timer = new Timer("Product Scheduler VCID " + vcid);
    }

    /**
     * When part messages are received, the previous timer task relate to that product
     * transaction is located and canceled. A new timer task is started.  This should be
     * be called whenever a new part is received for a product.
     * 
     * @param part the part
     */
    public void resetTimerForPart(final IProductPartProvider part) {
        if (part.getVcid() != vcid) {
            trace.trace("Scheduler for vcid " + vcid + " is discarding PartReceived message for vcid " +
                   part.getVcid());
            return;
        }
        final String id = part.getTransactionId();
 
        synchronized (this) {
            final TimerTask oldTask = get(id);
            if (oldTask != null) {
                try {
                    oldTask.cancel();
                    timer.purge();
                } catch (final IllegalStateException e) {}
            }
    
            final TimerTask newTask = new AgingTimeoutTask(part, vcid, bus);
            put(id, newTask);
            timer.schedule(newTask, agingTimeoutSeconds * 1000);
            
            trace.debug(new StringBuilder("New timer task started for Part ")
            		.append(part.getPartNumber())
            		.append(" and id ")
            		.append(id));
        }
    }

    /**
     * Stops the timer for part and removes it from the queue because it was assembled.  Should be called after 
     * assembling either a complete or partial product.
     * 
     * @param part the part
     */
    public void stopTimerForPartDueToAssembly(final IProductPartProvider part) {
    	final String id = part.getTransactionId();

    	synchronized (this) {
    		final TimerTask oldTask = get(id);
    		if (oldTask != null) {
    			try {
    				trace.debug("Scheduler for vcid " + vcid + " is cancelling outstanding product timer for complete product " + id);
    				oldTask.cancel();
    				timer.purge();
    			} catch (final IllegalStateException e) {
    				// timer already cancelled
    			}
    			remove(id);
    		}
    	}
    }
    
    /**
     * Shutdown the internal timer.
     */
    public void shutdown() {
    	if (timer != null) {
    		timer.cancel();
    	}
    }
}
