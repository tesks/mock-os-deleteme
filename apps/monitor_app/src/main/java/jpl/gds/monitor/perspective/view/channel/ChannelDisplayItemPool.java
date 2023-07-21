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
package jpl.gds.monitor.perspective.view.channel;

import java.util.ArrayList;
import java.util.List;

/**
 * This class manages a pool of ChannelDisplayItems. The pool is used so that objects can
 * be re-utilized rather than created new and garbage collected each time. This class uses
 * a singleton pattern
 *
 */
public final class ChannelDisplayItemPool {
   	 private static final int MAX_POOL_SIZE = 200000;
   	 
   	 /**
   	  * The one static instance of this class.
   	  */
	 private static volatile ChannelDisplayItemPool instance;
	 
     private final List<ChannelDisplayItem> pool = new ArrayList<ChannelDisplayItem>();
	 
     /**
      * Constructor. Private for singleton pattern.
      */
	 private ChannelDisplayItemPool() {
		 // do nothing
	 }

	 /**
	  * This method is part of a proper singleton class. It prevents using
	  * cloning as a hack around the singleton.
	  * 
	  * @return It never returns
	  * @throws CloneNotSupportedException
	  *             This function always throws this exception
	  */
	 @Override
	 public Object clone() throws CloneNotSupportedException {
		 throw new CloneNotSupportedException();
	 }
	 
	 /**
	  * Gets the one static instance of this class.
	  * 
	  * @return ChannelDisplayItemPool
	  */
	 public static synchronized ChannelDisplayItemPool getInstance() {
		 if (instance == null) {
			 instance = new ChannelDisplayItemPool();
		 }
		 return instance;
	 }
	 
	 /**
	  * Gets a free object from the pool.
	  * 
	  * @return ChannelDisplayItem
	  */
     public synchronized ChannelDisplayItem getFromPool() {
    	 if (!pool.isEmpty()) {
    		 return pool.remove(pool.size() - 1);
    	 } else {
    		 return new ChannelDisplayItem();
    	 }
     }
     
     /**
      * Releases an object to the free pool.
      * 
      * @param item the ChannelDisplayItem to release
      */
     public synchronized void releaseToPool(ChannelDisplayItem item) {
		 item.reset();
    	 if (pool.size() < MAX_POOL_SIZE) {
    		 pool.add(item);
    	 } 
     }
}
