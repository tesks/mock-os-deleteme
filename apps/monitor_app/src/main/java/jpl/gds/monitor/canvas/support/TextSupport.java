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
 * classes that implement this marker interface allow text/titles to 
 * be set
 *
 */
public interface TextSupport extends AbstractSupport {

	/**
	 * Gets the text or title to be drawn by the object.
	 * 
	 * @return text string
	 */
	public String getText();

	/**
	 * Sets the text or title drawn by the object.
	 * 
	 * @param text the text string to set
	 */
	public void setText(String text); 
}
