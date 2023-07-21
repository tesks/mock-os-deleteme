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
 * Fixed field configuration classes that support only foreground color must
 * implement this interface.
 */
public interface OneColorConfigSupport {
	/**
	 * Gets the foreground color.
	 * 
	 * @return SWT Color object
	 */
	public ChillColor getForeground();
	/**
	 * Sets the foreground color.
	 * 
	 * @param color SWT Color object
	 */
	public void setForeground(ChillColor color);

	/**
	 * Gets the flag indicating whether this field uses or overrides the 
	 * default colors in the fixed layout.
	 * 
	 * @return true if using default colors; false if overriding them
	 */
	public boolean usesDefaultColors();

	/**
	 * Gets the flag indicating whether this field uses or overrides the 
	 * default colors in the fixed layout.
	 * 
	 * @param usesDefaultColors true if field is using the default colors, false
	 * if the defaults are overridden.
	 */
	public void usesDefaultColors(boolean usesDefaultColors);
}
