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
package jpl.gds.monitor.guiapp.gui.views.tab;

import org.springframework.context.ApplicationContext;

import jpl.gds.monitor.guiapp.gui.views.StatusMessageComposite;
import jpl.gds.monitor.perspective.view.StatusMessageViewConfiguration;
import jpl.gds.perspective.view.IViewConfiguration;

/**
 * 
 * StatusMessageTabItem is a tab container for the StatusMessageComposite.
 * It can be manipulated as a view by the monitor's generic view logic.
 *
 */
public class StatusMessageTabItem extends MessageListTabItem {
    /**
     * Status message tab item title
     */
    public static final String TITLE = StatusMessageComposite.TITLE;
   
    /**
     * Creates an instance of StatusMessageTabItem.
     * @param appContext the current application context 
     * @param viewConfig the StatusMessageViewConfiguration object containing display settings
     */
    public StatusMessageTabItem(final ApplicationContext appContext, final IViewConfiguration viewConfig) {
        super(viewConfig, new StatusMessageComposite(appContext, viewConfig));
    }
    
    /**
     * Creates an instance of StatusMessageTabItem with a default view configuration.
     * @param appContext the current application context
     */
    public StatusMessageTabItem(final ApplicationContext appContext) {
        this(appContext, new StatusMessageViewConfiguration(appContext));
    }
    
    /**
     * @{inheritDoc}
     * @see jpl.gds.monitor.guiapp.gui.views.tab.AbstractTabItem#getViewConfig()
     */
    @Override
    public IViewConfiguration getViewConfig() {
        return this.viewConfig;
    }
       
    /**
     * Gets the title of this tab item.
     * @return title for display
     */
    public String getTitle() {
        return TITLE;
    }
  
    /**
     * Creates the controls and composites for this tab display.
     */
    @Override
    protected void createControls() {
    	super.createControls();
        this.tab.setToolTipText("Status Message List");
    }    
}
