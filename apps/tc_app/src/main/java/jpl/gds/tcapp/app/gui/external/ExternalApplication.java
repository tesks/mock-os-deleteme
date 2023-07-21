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
package jpl.gds.tcapp.app.gui.external;

import java.io.IOException;

import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import jpl.gds.shared.process.LineHandler;

/**
 * This is an interface to be implemented by classes that represent external applications
 * that are to be spawned by MPCS.
 * 
 *
 */
public interface ExternalApplication
{
	/**
	 * Create a menu item that can go on an SWT GUI and be used to launch this application. Only one menuItem should
	 * ever be created. If this method is called multiple times, it should return the same MenuItem instance ever time.
	 * 
	 * @param menu The menu object that this menu item will be a part of
	 * @param handler The selection handler to call when this menu item is selected
	 * 
	 * @return An SWT menu item that will populate the given menu
	 */
	public MenuItem createMenuItem(final Menu menu, final SelectionListener handler);
	
	/**
	 * Companion function to the createMenuItem method. This method should return null if the
	 * "createMenuItem" function has not been called yet, otherwise it should return the menu item
	 * that was created by "createMenuItem".
	 * 
	 * @return Null if no menu item has been created or the created menu item if it exists
	 */
	public MenuItem getMenuItem();
	
	/**
	 * Launch this external process. The stdout and stderr streams will be discarded.
	 * 
	 * @throws IOException if there is an error launching the process
	 */
	public void launch() throws IOException;
	
	/**
	 * Launch this external process
	 * 
	 * @param stdoutHandler The stdout data handler
	 * @param stderrHandler The stderr data handler
	 * 
	 * @throws IOException if there is an error launching the process
	 */
	public void launch(final LineHandler stdoutHandler,final LineHandler stderrHandler) throws IOException;

	/**
	 * Indicates whether the application is currently running.
	 * 
	 * @return true if application is executing, false if not
	 */
	public boolean isRunning();
	
	/**
	 * Shuts down the external application.
	 */
	public void shutdown();
	
	/**
	 * See if this application is enabled in the configuration
	 * 
	 * @return True if the application is enabled, false otherwise
	 */
	public boolean isEnabled();
	
	/**
	 * See if this application takes over focus when it is launched. If waitForExit is false,
	 * then the MPCS app that launches this application should still be usable while the new app is running,
	 * otherwise the MPCS app won't be usable until this new application is dismissed.
	 * 
	 * This is essentially a flag that indicates whether or not you should call the "waitForExit()" function on
	 * the ProcessLauncher class when you launch this process.
	 * 
	 * @return True if waitForExit is true in the configuration, false otherwise
	 */
	public boolean isWaitForExit();
	
	/**
	 * Get the name of this application (generally used for display)
	 * 
	 * @return The name of the external application
	 */
	public String getName();
}
