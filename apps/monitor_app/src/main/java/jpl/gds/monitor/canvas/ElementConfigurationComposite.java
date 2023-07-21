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

import org.eclipse.swt.widgets.Composite;

import jpl.gds.monitor.perspective.view.fixed.IFixedFieldConfiguration;

/**
 * This interface is implemented by GUI composite objects in the fixed 
 * builder palette that are used to configure a canvas element's attributes.
 * builder palette that are used to configure a canvas element's attributes.
 *
 */
public interface ElementConfigurationComposite {

	/**
	 * Gets the main GUI composite from the configuration component.
	 * 
	 * @return SWT Composite object
	 */
	public Composite getComposite();

	/**
	 * Gets an updated fixed field configuration object for the element 
	 * being configured.
	 * 
	 * @return FixedFieldConfiguration object, containing user changes to
	 * the element being edited
	 */
	public IFixedFieldConfiguration getConfiguration();

	/**
	 * Sets the GUI fields in the configuration composite to match
	 * the input fixed field configuration.
	 * 
	 * @param config the FixedFieldConfiguration to get element configuration 
	 * from
	 */
	public void setConfiguration(IFixedFieldConfiguration config);

	/**
	 * Adds an element configuration change listener to the GUI Composite.
	 * 
	 * @param l the ElementConfigurationChangeListener to add
	 */
	public void addChangeListener(ElementConfigurationChangeListener l);

	/**
	 * Removes an element configuration change listener from the GUI Composite.
	 * 
	 * @param l the ElementConfigurationChangeListener to remove
	 */
	public void removeChangeListener(ElementConfigurationChangeListener l);
}
