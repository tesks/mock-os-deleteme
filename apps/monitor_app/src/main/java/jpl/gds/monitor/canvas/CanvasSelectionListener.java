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

import java.util.List;

/**
 * This interface is implemented by components that want to be notified when
 * CanvasElements are selected and deselected by the user on the builder canvas
 *
 */
public interface CanvasSelectionListener {

	/**
	 * Notifies the listener that the given CanvasElements have been selected 
	 * by the user.
	 * @param oldElements list of previously selected elements
	 * @param newElements list of currently selected elements
	 */
	public void elementsSelected(List<CanvasElement> oldElements, 
	        List<CanvasElement> newElements);

	/**
	 * Notifies the listener that the given CanvasElements have been 
	 * deselected by the user.
	 * @param oldElements list of previously selected elements
	 * @param newElements list of currently selected elements
	 */
	public void elementsDeselected(List<CanvasElement> oldElements, 
	        List<CanvasElement> newElements);
}
