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
package jpl.gds.monitor.canvas.support;

import org.eclipse.swt.graphics.Font;

/**
 * classes that implement this marker interface allow font (size, 
 * type, bold/italic/normal/reverse) to be set
 *
 */
public interface FontSupport extends AbstractSupport {
	/**
	 * Gets the Font.
	 * 
	 * @return SWT Font object
	 */
	public Font getFont();
	
	/**
	 * Sets the font.
	 * @param font SWT Font object to set
	 */
	public void setFont(Font font);

}
