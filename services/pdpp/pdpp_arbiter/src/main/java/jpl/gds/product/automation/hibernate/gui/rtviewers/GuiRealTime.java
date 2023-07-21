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
package jpl.gds.product.automation.hibernate.gui.rtviewers;

/**
 * All real time panels for PDPP gui needs to have these methods.  These will be used to start and 
 * stop the timers when a real time panel is brought into focus.  We don't want pages to continue to 
 * update when they are not visible. 
 * 
 */
public interface GuiRealTime {
	/**
	 * Starts the timer used for controlling the frequency of real time updates.
	 * Checks if the real time button is enabled and not already running.
	 */
	public void startTimer();
	
	/**
	 * Stops the real time timer.
	 */
	public void stopTimer();
}
