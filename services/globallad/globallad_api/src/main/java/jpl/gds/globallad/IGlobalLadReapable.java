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
package jpl.gds.globallad;

/**
 * Interface for global lad containers / objects that can be reaped, IE can expire and should be destroyed.
 */
public interface IGlobalLadReapable {
	
	/**
	 * The reapable-ness and max time to live of a container is now inherited by all descendants.
	 */

	/**
	 * True if this should be reaped, IE be destroyed.
	 * 
	 * Note it is possible that a parent is marked as reapable with a reap time to live and 
	 * none of the descendants have reap times configured so the reap time must be inherited 
	 * if necessary. 
	 * 
	 * @param reapLevel defines the settings for the reap.
	 * @param checkTimeMilliseconds - Base time for checking.  
	 * @param parentWasReapable A way to pass the reapable status down to ancestors.  
	 * @param parentTimeToLive A way to pass the time to live an ancestor to all of its 
	 * descendants.  
	 * @return - True if this object can be reaped, otherwise false.
	 */
	public boolean reap(GlobalLadReapSettings reapSettings, long checkTimeMilliseconds, boolean parentWasReapable, long parentTimeToLive); 
	
	/**
	 * A way to check if this reap target is important enough to be logged when reaped.  Gives some 
	 * extra granularity to the reaper.  This returns true as the default so any reapable should override
	 * this to return false if necessary.
	 * 
	 * @return true if reaping of this target should be logged.
	 */
	public default boolean logableReap() {
		return true;
	}

}
