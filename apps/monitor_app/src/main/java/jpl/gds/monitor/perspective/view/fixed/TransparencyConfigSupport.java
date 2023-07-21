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
 * Fixed field configuration classes that support background transparency must
 * implement this interface.
 */
public interface TransparencyConfigSupport {
	/**
	 * Gets the transparency flag.
	 * 
	 * @return true for transparent background, false otherwise
	 */
	public boolean isTransparent();
	/**
	 * Sets the transparency flag.
	 * 
	 * @param transparent true for transparent background, false otherwise
	 */
	public void setTransparent(boolean transparent);
}
