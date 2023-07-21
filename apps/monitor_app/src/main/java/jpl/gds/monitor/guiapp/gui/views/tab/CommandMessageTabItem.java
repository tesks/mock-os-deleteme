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

import jpl.gds.monitor.guiapp.gui.views.CommandMessageComposite;
import jpl.gds.monitor.perspective.view.CommandMessageViewConfiguration;
import jpl.gds.perspective.view.IViewConfiguration;

/**
 * 
 * CommandMessageTabItem is a tab container for the CommandMessageComposite.
 * It acts as a view in the non-eclipse RCP version of the monitor and can
 * be manipulated by the monitor's generic view logic.
 *
 */
public class CommandMessageTabItem extends MessageListTabItem {
    /**
     * Command tab item title
     */
    public static final String TITLE = CommandMessageComposite.TITLE;
    
	/**
	 * Creates an instance of CommandMessageTabItem.
	 * 
	 * @param config
	 *            the CommandMessageViewConfiguration object containing display
	 *            configuration
	 * @param appContext the current application context object
	 *
	 */
    public CommandMessageTabItem(final ApplicationContext appContext, final IViewConfiguration config) {
        super(config, new CommandMessageComposite(appContext, config));
    }
    
    /**
     * Creates an instance of CommandMessageTabItem with a default view configuration.
     * @param appContext the current application context object
     */
    public CommandMessageTabItem(final ApplicationContext appContext) {
        this(appContext, new CommandMessageViewConfiguration(appContext));
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
     * @{inheritDoc}
     * @see jpl.gds.monitor.guiapp.gui.views.tab.MessageListTabItem#getDefaultName()
     */
    @Override
    public String getDefaultName() {
        return TITLE;
    }
    
    /**
     * Creates the controls and composites for this tab display.
     */
    @Override
    protected void createControls() {
    	super.createControls();
        this.tab.setToolTipText("Command Message List");
    }    
}
