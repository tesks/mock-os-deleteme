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

import jpl.gds.common.config.types.CommandUserRole;

/**
 * This interface provides notification methods when CPD requests are modified.
 * 
 * @since AMPCS R3
 */
public interface ICpdRequestChangeListener {
	/**
	 * Called when the user issues a delete to a CPD request
	 * 
	 * @param requestId the request ID of the request to be deleted
	 * @param requestRole the role pool that the request belongs in
	 */
	public void onRequestDelete(String requestId, CommandUserRole requestRole);

	/**
	 * Called when the user issues a flush
	 * 
	 * @param rolePool the role pool to flush
	 */
	public void onRequestFlush(CommandUserRole rolePool);
}
