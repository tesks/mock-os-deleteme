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
import java.util.Iterator;
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
 * TabularViewConfiguration is a special sub-class of the ViewConfiguration that
 * maintains its own set of views as an array list of ViewConfigurations, which
 * are to be displayed in a tabbed display.
 * 
 */
public class TabularViewConfiguration extends ViewConfiguration implements
        IViewConfigurationContainer {
    private List<IViewConfiguration> views = new ArrayList<IViewConfiguration>();

    /**
     * Boolean flag for indicating if view is iconified
     */
    public static final String IS_ICONIFIED_CONFIG = "isIconified";

    /**
     * Indicates tab that is currently selected
     */
    public static final String TOP_TAB_NUMBER_CONFIG = "topTabIndex";

    /**
     * Creates an instance of TabularViewConfiguration.
     * 
     * @param appContext the current application context
     */
    public TabularViewConfiguration(final ApplicationContext appContext) {
        super(appContext);
        initToDefaults();
    }

    /**
     * Indicates whether this view is iconified at time of perspective save.
     * 
     * @param icon
     *            true if view is iconified
     */
    public void setIsIconified(final boolean icon) {
        this.setConfigItem(IS_ICONIFIED_CONFIG, String.valueOf(icon));
    }

    /**
     * Gets the flag indicating if view was iconified at time of last save.
     * 
     * @return true if iconified, false if not
     */
    public boolean isIconified() {
        final String str = this.getConfigItem(IS_ICONIFIED_CONFIG);
        if (str == null) {
            return false;
        }
        return Boolean.parseBoolean(str);
    }

    /**
     * Sets the tab that is currently selected
     * 
     * @param index
     *            index of tab that is currently selected
     */
    public void setTopTabIndex(final int index) {
        this.setConfigItem(TOP_TAB_NUMBER_CONFIG, String.valueOf(index));
    }

    /**
     * Gets the index of the tab that is currently selected
     * 
     * @return index of the currently selected tab
     */
    public int getTopTabIndex() {
        final String str = this.getConfigItem(TOP_TAB_NUMBER_CONFIG);
        if (str == null) {
            return 0;
        }
        return Integer.parseInt(str);
    }

    /**
     * @{inheritDoc}
     */
    @Override
    public String toXML() {
        final StringBuilder result = new StringBuilder();
        if (this.isReference()) {
            result.append(this.reference.toXml());
        } else {
            result.append("<" + IViewConfigurationContainer.VIEW_CONTAINER_TAG
                    + " " + IViewConfiguration.VIEW_NAME_TAG + "=\""
                    + StringEscapeUtils.escapeXml(this.viewName) + "\" ");
            result.append(IViewConfiguration.VIEW_TYPE_TAG + "=\""
                    + this.viewType.getValueAsString() + "\" ");
            result.append(VIEW_VERSION_TAG + "=\"" + WRITE_VERSION + "\">\n");
            result.append(getAttributeXML());
            result.append(getConfigItemXML());

            if (this.views != null) {
                result.append("<" + IViewConfigurationContainer.CHILD_VIEWS_TAG
                        + ">\n");
                final Iterator<IViewConfiguration> it = this.views.iterator();
                while (it.hasNext()) {
                    final IViewConfiguration vc = it.next();
                    vc.setLocation(null);
                    vc.setSize(null);
                    result.append(vc.toXML());
                }
                result.append("</" + IViewConfigurationContainer.CHILD_VIEWS_TAG
                        + ">\n");
            }

            result.append("</" + IViewConfigurationContainer.VIEW_CONTAINER_TAG
                    + ">\n");
        }
        return result.toString();
    }

    /**
     * {@inheritDoc}
     * 
     */
    @Override
	public void addViewConfiguration(final IViewConfiguration vc) {
        if (this.views == null) {
            this.views = new ArrayList<IViewConfiguration>();
        }
        this.views.add(vc);
    }

    /**
     * {@inheritDoc}
     * 
     */
    @Override
	public void removeViewConfiguration(final IViewConfiguration vc) {
        if (this.views == null) {
            return;
        }
        this.views.remove(vc);
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.perspective.view.IViewConfigurationContainer#getViews()
     */
    @Override
	public List<IViewConfiguration> getViews() {
        return this.views;
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.perspective.view.IViewConfigurationContainer#setViews(java.util.List)
     */
    @Override
	public void setViews(final List<IViewConfiguration> views) {
        this.views = views;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void initToDefaults() {
        initToDefaults(appContext.getBean(PerspectiveProperties.class).getViewProperties(ViewType.MESSAGE_TAB),
                "jpl.gds.monitor.guiapp.gui.TabularViewShell",
                null,
                "jpl.gds.monitor.guiapp.gui.views.preferences.WindowPreferencesShell");
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.perspective.view.IViewConfigurationContainer#isImportViews()
     */
    @Override
	public boolean isImportViews() {
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.perspective.view.IViewConfigurationContainer#isRemovable()
     */
    @Override
	public boolean isRemovable() {
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.perspective.view.IViewConfigurationContainer#isWindowContainer()
     */
    @Override
	public boolean isWindowContainer() {
        return false;
    }
}
