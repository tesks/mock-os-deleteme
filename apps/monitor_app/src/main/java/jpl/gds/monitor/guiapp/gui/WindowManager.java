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
package jpl.gds.monitor.guiapp.gui;

import jpl.gds.monitor.config.GlobalPerspectiveParameter;
import jpl.gds.monitor.perspective.view.SingleWindowViewConfiguration;
import jpl.gds.monitor.perspective.view.TabularViewConfiguration;
import jpl.gds.perspective.DisplayConfiguration;
import jpl.gds.perspective.view.IViewConfiguration;

/**
 * The WindowManager interface is implemented by perspective displays that
 * manage windows of views and need to provide an interface to their window and
 * view management operations.
 */
public interface WindowManager
{
    /**
     * Sets the display configuration of this view set manager.
     * @param config the DisplayConfiguration to set
     */
    public void setDisplayConfiguration(DisplayConfiguration config);
    
   /** 
    * Gets the display configuration of this view set manager.
    * @return the DisplayConfiguration to get
    */
   public DisplayConfiguration getDisplayConfiguration();
    
    /**
     * Adds a new tabular view configuration to this manager.
     * @param config the new configuration to add.
     * @return true if the tab was added false if not
     */
    public boolean addViewTab(TabularViewConfiguration config);
    
    /**
     * Remove a view tab shell
     * 
     * @param config the ViewConfiguration object for the shell being removed
     * @return true if the window should be closed false otherwise (don't remove last window)
     * 
     */
    public boolean removeView(IViewConfiguration config);
    
    /**
     * Remove all view windows and exit, possibly with restart.
     * @param restart true to restart monitor windows based on saved perspective.
     */
    public void removeAllViewsAndExit(boolean restart);
    
    /**
     * Adds views marked as "merged" in the current display configuration to the windows
     * currently being displayed.
     * 
     */
    public void addMergedViews();
    
    /**
     * Clears data from all managed views.
     */
    public void clearAllViews();
    
    /**
     * Sends out the current state of the header (enabled or disabled) when it changes.
     * @param param attribute in monitorDisplay.xml used to determine if header should be enabled
     * @param newValue true if header should be displayed, false otherwise
     */
    public void sendGlobalHeaderConfigurationChange(GlobalPerspectiveParameter param, Boolean newValue);

    /**
     * Adds a new single window view configuration to this manager.
     * @param config the new configuration to add.
     * @return true if the window was added false if not
     */
    public boolean addView(SingleWindowViewConfiguration config);

}
