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
package jpl.gds.perspective.view;


/**
 * The ViewSetManager interface is implemented by perspective displays that 
 * manage multiple views and need to provide an interface to their view 
 * management operations.
 *
 *
 */
public interface ViewSetManager {

    /**
     * Adds a view configuration to the list of views owned by the
     * view set manager.
     * @param config the ViewConfiguration to add
     */
    public void addView(IViewConfiguration config);
    
    /**
     * Removes a view configuration from the list of views owned by the
     * view set manager.
     * @param config the ViewConfiguration to remove
     */
    public void removeView(IViewConfiguration config);
    
    /**
     * Clears data contents from all views.
     */
    public void clearAllViews();
}
