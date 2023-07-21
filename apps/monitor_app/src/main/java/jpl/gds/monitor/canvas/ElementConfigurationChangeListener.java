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

import jpl.gds.monitor.perspective.view.fixed.IFixedFieldConfiguration;

/**
 * This interface is implemented by classes that want to be notified of
 * configuration changes to a CanvasElement.
 *
 */
public interface ElementConfigurationChangeListener {
	
	/**
     * Notifies the listener that the elements configuration's have changed.
     * 
     * @param configs a list of the new fixed field configuration objects
     */
    public void elementsChanged(List<IFixedFieldConfiguration> configs);

	/**
	 * Notifies the listener that the time source of a time element has 
	 * changed.
	 * 
	 * @param elements the list of CanvasElements that have changed
	 */
	public void timeSourceChanged(List<CanvasElement> elements);
}
