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
package jpl.gds.monitor.guiapp.gui.views;

import org.springframework.context.ApplicationContext;

import jpl.gds.perspective.view.IViewConfiguration;

/**
 * 
 * CommandMessageComposite is a monitor view that displays a table of
 * command-related messages.
 *
 */
public class CommandMessageComposite extends MessageListComposite {
    /**
     * Command message composite title
     */
    public static final String TITLE = "Command";
    
    /**
     * Creates an instance of CommandMessageTabItem.
     * @param appContext the current application context
     * @param config the MessageListViewConfiguration object containing
     * display settings
     */
    public CommandMessageComposite(final ApplicationContext appContext, final IViewConfiguration config) {
        super(appContext, config);
    }
    
    /**
     * @{inheritDoc}
     * @see jpl.gds.monitor.guiapp.gui.views.MessageListComposite#getDefaultName()
     */
    @Override
    public String getDefaultName() {
        return TITLE;
    }
    
}
