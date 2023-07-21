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
package jpl.gds.monitor.guiapp.gui.views.support;

/**
 * Common interface for Channel Chart, Channel List and Alarm composites to 
 * get and set maximum size of channels that can be selected
 *
 */
public interface ChannelSetConsumer {
	/**
	 * Gets the maxChannelSelectionSize
	 * 
	 * @return Returns the maxChannelSelectionSize.
	 */
	public long getMaxChannelSelectionSize();

	/**
	 * Sets the maxChannelSelectionSize
	 *
	 * @param maxChannelSelectionSize The maxChannelSelectionSize to set.
	 */
	public void setMaxChannelSelectionSize(long maxChannelSelectionSize);
}
