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
package jpl.gds.evr.api.service;

import jpl.gds.evr.api.IEvr;

/**
 * This interface is to be implemented by all EVR trigger classes. It declares
 * only one method, which is called by the <code>EvrNotifier</code> to see if a
 * given EVR will trip the trigger.
 * 
 */
public interface IEvrNotificationTrigger {

	/**
	 * This method will return true if the provided EVR satisfy the trigger
	 * criteria (i.e. trips it). If the EVR doesn't trigger, false is returned.
	 * 
	 * @param evr
	 *            EVR to test for trigger
	 * @return true if triggered, false otherwise
	 */
	boolean evrTriggersNotification(final IEvr evr);

}
