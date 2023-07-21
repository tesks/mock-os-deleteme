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

/**
 * Classes that implement this marker interface can set a line 
 * style and line thickness
 *
 */
public interface LineSupport extends AbstractSupport {
	/**
	 * Gets the style of line used to draw the line.
	 * 
	 * @return SWT.LINE_SOLID, SWT.LINE_DASH, etc.
	 */
	public int getLineStyle(); 

	/**
	 * Sets the style of line used to draw the box.
	 * 
	 * @param lineStyle SWT.LINE_SOLID, SWT.LINE_DASH, etc.
	 */ 
	public void setLineStyle(int lineStyle);

	/**
	 * Gets the thickness, in pixels, of the line used to draw the line.
	 * 
	 * @return line thickness
	 */
	public int getThickness();

	/**
	 * Sets the thickness, in pixels, of the line used to draw the box.
	 * 
	 * @param thickness line thickness
	 */
	public void setThickness(int thickness); 
}
