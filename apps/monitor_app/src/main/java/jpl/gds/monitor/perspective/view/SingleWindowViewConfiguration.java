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
package jpl.gds.monitor.perspective.view;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringEscapeUtils;
import org.springframework.context.ApplicationContext;

import jpl.gds.perspective.config.PerspectiveProperties;
import jpl.gds.perspective.config.ViewType;
import jpl.gds.perspective.view.IViewConfiguration;
import jpl.gds.perspective.view.IViewConfigurationContainer;
import jpl.gds.perspective.view.ViewConfiguration;

/**
 * 
 * SingleWindowViewConfiguration is a special sub-class of the ViewConfiguration that 
 * acts as a standalone window that contains one child view.
 *
 */
public class SingleWindowViewConfiguration extends ViewConfiguration implements IViewConfigurationContainer
{
	private IViewConfiguration childView = null;
	
	/**
	 * String that will be replaced eventually with the actual test name
	 */
	public static final String TEST_TOKEN = "[test-name]";
	
	/**
	 * Boolean flag for indicating if view is iconified
	 */
	public static final String IS_ICONIFIED_CONFIG = "isIconified";

	/**
	 * Creates an instance of SingleWindowViewConfiguration.
	 * @param appContext the current application context
	 */
	public SingleWindowViewConfiguration(final ApplicationContext appContext) {
		super(appContext);
		initToDefaults();
	}

	/**
	 * Indicates whether this view is iconified at time of perspective save.
	 * @param icon true if view is iconified
	 */
	public void setIsIconified(final boolean icon) {
		this.setConfigItem(IS_ICONIFIED_CONFIG, String.valueOf(icon));
	}


	/**
	 * Gets the flag indicating if view was iconified at time of last save.
	 * @return true if iconified, false if not
	 */
	public boolean isIconified() {
		final String str = this.getConfigItem(IS_ICONIFIED_CONFIG);
		if (str == null) {
			return false;
		}
		return Boolean.parseBoolean(str);
	}


	@Override
	public String toXML()
	{
		final StringBuilder result = new StringBuilder();
		if (this.isReference()) {
			result.append(reference.toXml());
		} else {
			result.append("<" + IViewConfigurationContainer.VIEW_CONTAINER_TAG + " " + 
					IViewConfiguration.VIEW_NAME_TAG + "=\"" + StringEscapeUtils.escapeXml(viewName) + "\" ");
			result.append(IViewConfiguration.VIEW_TYPE_TAG + "=\"" + viewType.getValueAsString() + "\" ");
			result.append(VIEW_VERSION_TAG + "=\"" + WRITE_VERSION + "\">\n");
			result.append(getAttributeXML());
			result.append(getConfigItemXML());

			if (childView != null) {
				result.append("<" + IViewConfigurationContainer.CHILD_VIEWS_TAG + ">\n");
				childView.setLocation(null);
				childView.setSize(null);
				result.append(childView.toXML());
				result.append("</" + IViewConfigurationContainer.CHILD_VIEWS_TAG + ">\n");
			}

			result.append("</" + IViewConfigurationContainer.VIEW_CONTAINER_TAG + ">\n");
		}
		return result.toString();        
	}

	@Override
	public void addViewConfiguration(final IViewConfiguration vc) {
		childView = vc;
	}


	@Override
	public void removeViewConfiguration(final IViewConfiguration vc) {
		if (vc == childView) {
			childView = null;
		}
	}

	@Override
	public List<IViewConfiguration> getViews()
	{
		final ArrayList<IViewConfiguration> views = new ArrayList<IViewConfiguration>(1);
		if (childView != null) {
			views.add(childView);
		}
		return views;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.perspective.view.IViewConfigurationContainer#setViews(java.util.List)
	 */
	@Override
	public void setViews(final List<IViewConfiguration> views)
	{
		if (views != null && views.size() >= 1) {
			childView = views.get(0);
		}
	}

	@Override
	public void initToDefaults() {
	    initToDefaults(appContext.getBean(PerspectiveProperties.class).getViewProperties(ViewType.SINGLE_VIEW_WINDOW),
                "jpl.gds.monitor.guiapp.gui.SingleViewShell",
                null,
                "jpl.gds.monitor.guiapp.gui.views.preferences.WindowPreferencesShell");
	}

	/**
     * {@inheritDoc}
	 * @see jpl.gds.perspective.view.IViewConfigurationContainer#isImportViews()
	 */
	@Override
	public boolean isImportViews() {
		return true;
	}

	/**
     * {@inheritDoc}
	 * @see jpl.gds.perspective.view.IViewConfigurationContainer#isRemovable()
	 */
	@Override
	public boolean isRemovable() {
		return true;
	}

	/**
     * {@inheritDoc}
	 * @see jpl.gds.perspective.view.IViewConfigurationContainer#isWindowContainer()
	 */
	@Override
	public boolean isWindowContainer() {
		return false;
	}
}
