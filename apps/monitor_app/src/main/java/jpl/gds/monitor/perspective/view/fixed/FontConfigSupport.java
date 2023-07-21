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

import jpl.gds.shared.swt.types.ChillFont;

/**
 * Fixed field configuration classes that support a font setting must implement this 
 * interface.
 *
 */
public interface FontConfigSupport {
	/**
	 * Gets the font.
	 * 
	 * @return ChillFont object
	 */
	public ChillFont getFont();

	/**
	 * Sets the font.
	 * 
	 * @param font ChillFont object to set
	 */
	public void setFont(ChillFont font);

	/**
	 * Gets the value of the uses default font flag, indicating whether this object
	 * overrides the default font in the fixed layout.
	 * 
	 * @return true if field is using the default font, false if the default
	 * is overridden.
	 */
	public boolean usesDefaultFont();

	/**
	 * Sets the value of the uses default font flag, indicating whether this object
	 * uses or overrides the default font in the fixed layout.
	 * 
	 * @param usesDefaultFont true if field is using the default font, false if the 
	 * default is overridden.
	 */
	public void usesDefaultFont(boolean usesDefaultFont);
}
