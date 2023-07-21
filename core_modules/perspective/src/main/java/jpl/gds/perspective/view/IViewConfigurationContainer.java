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
package jpl.gds.perspective.view;

import java.util.List;

/**
 * This interface is implemented by ViewConfigurations that contain other
 * ViewConfigurations.
 *
 *
 */
public interface IViewConfigurationContainer  {
	/**
	 * XML view container element name
	 */
	public static final String VIEW_CONTAINER_TAG = "ViewContainer";
	
	/**
	 * XML child views element name
	 */
	public static final String CHILD_VIEWS_TAG = "ChildViews";

	/**
	 * Sets the entire list of child views.
	 * @param vcList the List of child ViewConfigurations
	 */
	public void setViews(List<IViewConfiguration> vcList);

	/**
	 * Gets the entire list of child views.
	 * @return the List of child ViewConfigurations
	 */
	public List<IViewConfiguration> getViews();

	/**
	 * Adds a child view configuration to the end of the list of child view configurations.
	 * @param vc the ViewConfiguration to add
	 */
	public void addViewConfiguration(IViewConfiguration vc);

	/**
	 * Removes a child view configuration from the list of child view configurations.
	 * @param vc the ViewConfiguration to remove
	 */
	public void removeViewConfiguration(IViewConfiguration vc);

	/**
	 * Indicates whether new views can be imported into this ViewContainer by the perspective
	 * editor
	 * @return true if views can be imported/added; false if not
	 */
	public boolean isImportViews();

	/**
	 * Indicates whether the user should be able to remove this ViewContainer in the
	 * perspective editor.
	 * @return true if the container can be removed; false if not
	 */
	public boolean isRemovable();

	/**
	 * Indicates whether this view container contains only standalone window views,
	 * or cannot contain standalone windows. 
	 * @return true if this container can only include standalone window children,
	 * false if not
	 */
	public boolean isWindowContainer();
}
