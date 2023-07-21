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

package jpl.gds.tc.api.icmd;

import jpl.gds.tc.api.icmd.datastructures.CpdDmsBroadcastStatusMessages;


/**
 * This interface is implemented by classes that want to subscribe to the DMS
 * broadcast status messages.
 *
 * @since AMPCS R7.1
 * MPCS-5934
 */
public interface ICpdDmsBroadcastStatusMessagesSubscriber {

	/**
	 * Handle the DMS broadcast status messages from CPD. Also implicitly
	 * signals that the poll is working fine and data isn't stale.
	 *
	 * @param msgs
	 *            DMS broadcast status messages data structure
	 */
	void handleNewMessages(CpdDmsBroadcastStatusMessages msgs);

	/**
	 * Signals to the subscriber that the poll isn't working and whatever data
	 * previously handled should now be considered stale.
	 */
	void dataNowStale();

}
