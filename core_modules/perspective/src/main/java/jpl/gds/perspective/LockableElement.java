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
package jpl.gds.perspective;

/**
 * Interface that is to be implemented by classes whose methods need to 
 * locked. A perspective that is locked will not allow edits.
 * 
 *
 */
public interface LockableElement {
    /**
     * Retrieves flag that determines if a perspective is locked, that is if 
     * edits will not be saved
     * 
     * @return true if perspective is locked, false otherwise
     */
    public boolean isLocked();
    
    /**
     * Sets the lock flag
     * 
     * @param lock true if perspective should be locked, false otherwise
     */
    public void setLocked(boolean lock);
}
