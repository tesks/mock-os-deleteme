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
package jpl.gds.monitor.perspective.view.fixed;

import jpl.gds.shared.swt.types.ChillColor;

/**
 * Fixed field configuration classes that support foreground and background colors must
 * implement this interface.
 */
public interface TwoColorConfigSupport extends OneColorConfigSupport {
	/**
	 * Gets the background color.
	 * 
	 * @return ChillColor object
	 */
	public ChillColor getBackground();
	/**
	 * Sets the background color.
	 * 
	 * @param color ChillColor object
	 */
	public void setBackground(ChillColor color);
}
