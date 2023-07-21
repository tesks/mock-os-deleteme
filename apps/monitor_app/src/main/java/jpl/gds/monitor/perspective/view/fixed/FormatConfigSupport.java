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
 * Fixed field configuration classes that support a format setting must implement this 
 * interface.
 *
 */
public interface FormatConfigSupport {
	/**
	 * Gets the C-printf style or Java time formatter for the text drawn by the object.
	 * 
	 * @return format string
	 */
	public String getFormat();

	/**
	 * Sets the C-printf style or Java time formatter for the text drawn by the object.
	 * 
	 * @param format the format string
	 * @throws IllegalArgumentException thrown if there is and error when setting the format
	 */
	public void setFormat(String format) throws IllegalArgumentException;
}
