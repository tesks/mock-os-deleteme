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

import jpl.gds.perspective.view.IViewConfiguration;
import jpl.gds.shared.swt.ChillShell;

/**
 * ViewShell contains methods shared by SingleViewShell and TabularViewShell
 */
public interface ViewShell extends ChillShell {
	
	/**
	 * Updates the view configuration
	 */
	public void updateViewConfig();
	
	/**
	 * Update the object that receives notification of perspective-related requests
	 * 
	 * @param listener listens for changes in the perspective
	 */
	public void updatePerspectiveListener(MonitorPerspectiveListener listener);
	
	/**
	 * Clears the data in all the views
	 */
	public void clearAllViews();
	
	/**
	 * Gets the view configuration for this view shell
	 * 
	 * @return view configuration object
	 */
	public IViewConfiguration getViewConfig();
	
	/**
	 * Gets the header for this view shell
	 * 
	 * @return header that displays VCID, DSS ID and message topic
	 */
	public ViewShellHeader getHeader();

	/**
	 * Gets the menu item manager which is used for sharing menu items across 
	 * the different view shells
	 * 
	 * @return menu item manager that contains menus and their respective menu items
	 */
	public MenuItemManager getMenuItemManager();
}
