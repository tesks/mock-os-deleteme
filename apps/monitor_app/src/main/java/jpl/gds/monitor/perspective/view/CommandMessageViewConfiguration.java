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

import org.springframework.context.ApplicationContext;

import jpl.gds.perspective.config.PerspectiveProperties;
import jpl.gds.perspective.config.ViewType;

/**
 * CommandMessageViewConfiguration is a view configuration for the command
 * message view, which is just a message list view with specific message
 * filters.
 * 
 */
public class CommandMessageViewConfiguration extends
        MessageListViewConfiguration {

    /**
     * Creates an instance of CommandMessageViewConfiguration.
     */
    public CommandMessageViewConfiguration(ApplicationContext appContext) {
        super(appContext);
        initToDefaults(appContext.getBean(PerspectiveProperties.class).getViewProperties(ViewType.COMMAND),
                "jpl.gds.monitor.guiapp.gui.views.CommandMessageComposite",
                "jpl.gds.monitor.guiapp.gui.views.tab.CommandMessageTabItem",
                "jpl.gds.monitor.guiapp.gui.views.preferences.MessageListPreferencesShell");
    }
}
