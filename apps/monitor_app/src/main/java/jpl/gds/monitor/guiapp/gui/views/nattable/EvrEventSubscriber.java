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
package jpl.gds.monitor.guiapp.gui.views.nattable;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.eclipse.swt.widgets.Display;
import org.springframework.context.ApplicationContext;

import jpl.gds.evr.api.message.EvrMessageType;
import jpl.gds.evr.api.message.IEvrMessage;
import jpl.gds.monitor.config.MonitorConfigValues;
import jpl.gds.monitor.guiapp.common.GeneralMessageDistributor;
import jpl.gds.monitor.guiapp.common.GeneralMessageListener;
import jpl.gds.monitor.guiapp.common.gui.BoundedGlazedEventList;
import jpl.gds.monitor.guiapp.common.gui.IGlazedListHistorySubscriber;
import jpl.gds.monitor.guiapp.common.gui.IGlazedListHistorySubscriberListener;
import jpl.gds.monitor.guiapp.common.gui.INatListItem;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.message.IMessage;

/**
 * An IGlazedListHistorySubscriber for EVR messages. Stores EvrNatListItems in a 
 * BoundedGlazedEventList.
 *
 */
public class EvrEventSubscriber implements GeneralMessageListener, IGlazedListHistorySubscriber<EvrNatListItem> {
    
    /*
     * If the view is paused, rows will accumulate in the event list. The max
     * default pause length is a bailout limit. No matter what the conditions,
     * the pause will be released if the queue reaches this length. I would put
     * this in a config file, but I just refuse to add any more monitor stuff to
     * the ridiculous GdsSystemConfig.xml file. This can be placed in a
     * monitor.properties file when there finally is one. TODO.
     */
    private static final int MAX_DEFAULT_PAUSE_LENGTH = 100000;

    private final List<BoundedGlazedEventList<EvrNatListItem>>               eventLists;

    // private static EvrEventSubscriber instance;
    private final CopyOnWriteArrayList<IGlazedListHistorySubscriberListener> pauseListeners = 
            new CopyOnWriteArrayList<IGlazedListHistorySubscriberListener>();

	private final GeneralMessageDistributor messageDist;

	private final ApplicationContext appContext;
    
    /**
     * Constructor.
     * 
     * @param appContext the current application context
     */
    public EvrEventSubscriber(final ApplicationContext appContext) {
    	this.appContext = appContext;
    	this.messageDist = this.appContext.getBean(GeneralMessageDistributor.class);
        eventLists = new ArrayList<>();
      
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.monitor.guiapp.common.gui.IGlazedListHistorySubscriber#addListener(jpl.gds.monitor.guiapp.common.gui.IGlazedListHistorySubscriberListener)
     */
    @Override
    public synchronized void addListener(final IGlazedListHistorySubscriberListener listener) {
        pauseListeners.add(listener);
        /* If this is the fist listener, start the subscription to the message distributor. */
        if (pauseListeners.size() == 1) {
            messageDist.addDataListener(this, EvrMessageType.Evr);
        }
    }
    
    @Override
    public void addBoundedEventList(final BoundedGlazedEventList<EvrNatListItem> eventList) {
        eventLists.add(eventList);
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.monitor.guiapp.common.GeneralMessageListener#messageReceived(jpl.gds.shared.message.IMessage[])
     */
    @Override
    public void messageReceived(final IMessage[] m) {
        
        /* Create a list of EvrNatListItems wrapping the EVR messages */
        final List<EvrNatListItem> itemsToAdd = new LinkedList<EvrNatListItem>();
        for (final IMessage msg : m) {
            itemsToAdd.add(new EvrNatListItem(appContext.getBean(MonitorConfigValues.class),(IEvrMessage)msg));
        }
        for (final BoundedGlazedEventList<EvrNatListItem> eventList : eventLists) {
            /* Add all of these items to the bounded event list. */
            eventList.addAll(itemsToAdd);

            /*
             * If the list has been paused to long according to our parameters, it
             * must be un-paused or it will get too long.
             */
            if (eventList.isPaused() && eventList.size() >= MAX_DEFAULT_PAUSE_LENGTH) {
                releaseAllPauses(eventList);
            }
        }
        
        

        
    }
    
    /**
     * @{inheritDoc}
     * @see jpl.gds.monitor.guiapp.common.gui.IGlazedListHistorySubscriber#removeListener(jpl.gds.monitor.guiapp.common.gui.IGlazedListHistorySubscriberListener)
     */
    @Override
    public synchronized void removeListener(final IGlazedListHistorySubscriberListener listener) {

    	pauseListeners.remove(listener);

    	/* If it the last listener, remove the subscription to the message distributor
    	 * for EVR messages
    	 */
    	if (pauseListeners.size() == 0) {
    		messageDist.removeDataListener(this);
    	}

    }

 
    /**
     * Releases the pause on the bounded event list and notifies listeners
     * that pause has been released.
     */
    private synchronized void releaseAllPauses(final BoundedGlazedEventList<? extends INatListItem> eventList) {
        
        TraceManager.getDefaultTracer()
                    .warn("EVR history list has reached maximum size; EVR view pause will be released");

        
        /* Notify listeners on the SWT thread in case they have to do GUI things. */
        Display.getDefault().asyncExec(new Runnable() {
            /**
             * {@inheritDoc}
             * @see java.lang.Runnable#run()
             */
            @Override
            public void run() {
                try {
                    for (final IGlazedListHistorySubscriberListener l : pauseListeners) {
                        l.pauseReleased();
                    }
                    
                    
                } catch (final Exception e) {
                    e.printStackTrace();
                    TraceManager.getDefaultTracer().error("In EvrEventSubscriber.releasePaused: Ignoring exception: " + e.getMessage());

                } 
            }
        });
    
        eventList.releaseAllPauses();
    }

    @Override
    public void removeBoundedEventList(final BoundedGlazedEventList<EvrNatListItem> eventList) {
        eventLists.remove(eventList);
    }
    

}
