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
package jpl.gds.context.api;


/**
 * Holder class for the global "enable SSE downlink" flag in certain types of 
 * application contexts, particularly the classic session configuration.
 * 
 *
 * @since R8
 *
 */
public class EnableSseDownlinkContextFlag {
	
	private Boolean isEnabled = Boolean.valueOf(true);
	
	/**
	 * Gets the flag indicating if SSE downlink is enabled in the current context. 
	 * 
	 * @return true if SSE downlink is in use, false if not
	 */
	public boolean isSseDownlinkEnabled() {
		return isEnabled;
	}
	
	/**
	 * Sets the flag indicating if SSE Downlink is enabled in the current context. 
	 * 
	 * @param enable true to enable SSE Downlink, false to not
	 */
	public void setSseDownlinkEnabled(boolean enable) {
		isEnabled = enable;
	}
}
