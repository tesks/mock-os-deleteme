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
package jpl.gds.tcapp.app.gui.icmd;

/**
 * Interface that for a listener that is interested in the event that a detailed
 * CPD request view has been closed
 * 
 * @since AMPCS R3
 */
public interface ICpdDetailedViewCloseListener {
	/**
	 * Called when a CPD detailed request view is closed
	 * 
	 * @param requestId the request ID of the request whose detailed view was
	 *            closed
	 */
	public void onDetailedViewClose(String requestId);
}
