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
package jpl.gds.monitor.guiapp.common.gui;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.matchers.MatcherEditor;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;


/**
 * A wrapper for a basic Glazed Event List that limits its length by removing
 * items from the front as it is adding items to the end. It can also handle
 * pausing and resuming deletions from the list, to accommodate the pause
 * capability of NAT Table views.
 *
 * @param <T>
 *            any type that implements INatListItem
 */
public class BoundedGlazedEventList<T extends INatListItem> {
    
    private final Tracer        tracer;

    
    private final EventList<T> baseEventList;
    private int maxBound;
    private int pauseCount;

    private boolean dropFlag = false;
    private final AtomicInteger enqueued;

    private List<MatcherEditor<T>> matcherEditors = new ArrayList<>();

    /**
     * Constructor.
     * 
     * @param maxSize the initial maximum size of the list
     */
    public BoundedGlazedEventList(final int maxSize) {
        this.maxBound = maxSize;
        final LinkedList<T> temp = new LinkedList<T>();
        baseEventList = GlazedLists.eventList(temp);

        enqueued = new AtomicInteger(0);
        tracer = TraceManager.getDefaultTracer();
    }
    
    /**
     * Adds the given list of INatListItems to the end of the list, removing
     * items from the front if the maximum size is exceeded and not in pause 
     * state.
     * 
     * @param toAdd list of items to add
     */
    public void addAll(final List<T> toAdd) {

        final List<T> postFilter;

        if (toAdd != null) {
            postFilter = new ArrayList<>(toAdd.size());
            for (final T item : toAdd) {
                boolean matches = true;
                for (final MatcherEditor<T> m : matcherEditors) {
                    matches = matches && m.getMatcher().matches(item);
                }
                if (matches) {
                    postFilter.add(item);
                }
            }
            enqueued.addAndGet(postFilter.size());
        }
        else {
            postFilter = null;
        }
    		
        Display.getDefault().asyncExec(new Runnable() {
            /**
             * {@inheritDoc}
             * @see java.lang.Runnable#run()
             */
            @Override
            public void run() {
                
                /* Glazed event lists must be locked for write */
                baseEventList.getReadWriteLock().writeLock().lock();
                try {
                    
                	/* Add the new items. */
                    if (postFilter != null) {

                		if((enqueued.get() > maxBound) && (pauseCount == 0)){
                			//warn the user on the first drop
                			if(!dropFlag){
                                tracer.warn("Messages are being dropped due to excessive queue length");

                				
                				dropFlag = true;
                				
                				try {
                					final Color color = Display.getDefault().getSystemColor(SWT.COLOR_RED);
                					final Shell shells[] = Display.getDefault().getShells();
                					for(final Shell shell : shells){
                						if(shell != null && !shell.isDisposed()){
                							shell.setBackground(color);
                						}
                					}
                				} catch (final Exception e) {
                					e.printStackTrace();
                					tracer.error("In BoundedGlazedEventList.run(): Ignoring exception: " + e.getMessage());
                				}
                			}
                		}
                		else{
                            baseEventList.addAll(postFilter);
                		}
                	}
                	//if toAdd is null, it came from the queue maxBound being changed. reset the display warning color.
                	else{
                		if(dropFlag){

                			dropFlag = false;
                			
                			try {
                				final Shell shells[] = Display.getDefault().getShells();
                				for(final Shell shell : shells){
                					if(shell != null && !shell.isDisposed()){
                						shell.setBackground(null);
                					}
                				}
                			} catch (final Exception e) {
                				e.printStackTrace();
                				tracer.error("In BoundedGlazedEventList.run(): Ignoring exception: " + e.getMessage());
                			}
                		}
                	}
                    
                    /* Figure out how many to remove */
                    final int toRemove = Math.max(0, baseEventList.size() - maxBound);

                    /* Remove items, unless paused, in which case the list just grows
                     * until the pause count is 0.
                     */
                    if (pauseCount == 0) {
                        for (int i = 0 ; i < toRemove; i++) {
                            baseEventList.remove(0);
                        }
                    }
                    
                    
                } catch (final Exception e) {
                    e.printStackTrace();
                    tracer.error("In BoundedGlazedEventList.run(): Ignoring exception: " + e.getMessage());
                } finally {
                    /* Make sure to always release the write lock */
                    baseEventList.getReadWriteLock().writeLock().unlock();

                    if (postFilter != null) {
                        enqueued.addAndGet(postFilter.size() * -1);
                    }
                }
            }
        });
    }

    /**
     * Gets the underlying Glazed Event List.
     * 
     * @return EventList<T>
     */
    public EventList<T> getEventList() {
        return this.baseEventList;
    }

    /**
     * Sets the maximum length of the bounded list, and forces the
     * removal of any items beyond the max bound.
     * 
     * @param newValue new length to set
     */
    public void setMaxSize(final Integer newValue) {
        this.maxBound = newValue;
        /* Adding an empty message list forces the event list to resize by removing
         * any items beyond the new maximum bound.
         */
        addAll(null);
    } 
    
    /**
     * Increments the pause count, meaning some view into this
     * list has been paused by the user. Items will not be deleted
     * from the list while pauseCount is greater than 0. It is the 
     * responsibility of the IGlazedListHistorySubscriber to release
     * the pause if the event list gets critically long.
     */
    public synchronized void pause() {
        pauseCount++;
    }
    
    /**
     * Decrements the pause count, meaning some view into this list that was
     * paused by the user has been resumed. Items will not be deleted from the
     * list while pauseCount is greater than 0.
     */
    public synchronized void releasePause() {
        if (pauseCount > 0) {
            pauseCount--;
        }
    }
    
    /**
     * Indicates if the list is paused, i.e., if it is not deleting items.
     * 
     * @return true if paused, false if not
     */
    public synchronized boolean isPaused() {
        return pauseCount != 0;
    }
    
    /**
     * Immediately sets the pause count to 0, resuming deletions from
     * the list.
     */
    public synchronized void releaseAllPauses() {
        pauseCount = 0;
    }

    /**
     * Gets the current size of the Glazed Event List.
     * 
     * @return list size
     */
    public int size() {
        return this.baseEventList.size();
    }

    /**
     * Set the filters applied to be applied to events prior to
     * adding them to the backing event list.
     * 
     * @param matcherEditors
     *            list of filters to apply.
     */
    public void setFilters(final List<MatcherEditor<T>> matcherEditors) {
        this.matcherEditors = matcherEditors;
    }
}
