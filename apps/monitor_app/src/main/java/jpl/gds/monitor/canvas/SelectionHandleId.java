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
package jpl.gds.monitor.canvas;

/**
 * An enumeration of selection handle identifiers. Selection handles are the 
 * little blue boxes drawn at points on graphical objects so it can be grabbed 
 * and re-sized.
 *
 */
public enum SelectionHandleId {
	/** Undefined handle */
	HANDLE_NONE,
	/** Handle at top left corner of object */
	HANDLE_TOP_LEFT,
	/** Handle at top right corner of object */
	HANDLE_TOP_RIGHT,
	/** Handle at bottom left corner of object */
	HANDLE_BOTTOM_LEFT,
	/** Handle at bottom right corner of object */
	HANDLE_BOTTOM_RIGHT;

	/**
	 * Do not include HANDLE_NONE in this count
	 */
	public static final int MAX_HANDLES = 4;
}
