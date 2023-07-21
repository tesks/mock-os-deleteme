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

/**
 * This interface is implemented by fixed field configurations that support line 
 * style and thickness.
 *
 */
public interface LineConfigSupport {
	/**
	 * Gets the style of line used to draw the line.
	 * 
	 * @return LineStyle object
	 */
	public LineStyle getLineStyle(); 

	/**
	 * Sets the style of line used to draw the box.
	 * 
	 * @param lineStyle LineStyle object
	 */ 
	public void setLineStyle(LineStyle lineStyle);

	/**
	 * Gets the thickness, in pixels, of the line used to draw the line.
	 * 
	 * @return line thickness
	 */
	public int getLineThickness();

	/**
	 * Sets the thickness, in pixels, of the line used to draw the box.
	 * 
	 * @param thickness line thickness
	 */
	public void setLineThickness(int thickness);
}
