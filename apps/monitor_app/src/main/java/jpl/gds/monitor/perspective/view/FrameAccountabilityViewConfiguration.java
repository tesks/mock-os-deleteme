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
import jpl.gds.perspective.view.ViewConfiguration;


/**
 * FrameAccountabilityViewConfiguration encapsulates the configuration for the Frame 
 * Accountability display.
 *
 */
public class FrameAccountabilityViewConfiguration extends ViewConfiguration {
    private static final String OVERLAY_COMMANDS_PROPERTY = "overlayCommands";
    
    /**
     * Creates an instance of FrameWatchViewConfiguration.
     */
    public FrameAccountabilityViewConfiguration(final ApplicationContext appContext) {
        super(appContext);
        
        super.initToDefaults(appContext.getBean(PerspectiveProperties.class).getViewProperties(ViewType.FRAME_ACCOUNTABILITY),
                "jpl.gds.monitor.guiapp.gui.views.FrameAccountabilityComposite",
                "jpl.gds.monitor.guiapp.gui.views.tab.FrameAccountabilityTabItem",
                "jpl.gds.monitor.guiapp.gui.views.preferences.FrameAccountabilityPreferencesShell");
    }

    /**
     * Indicates whether this view is to include commands in the tree of framesync events.
     * @param use true to include commands, false if not
     */
    public void setOverlayCommands(final boolean use) {
        this.setConfigItem(OVERLAY_COMMANDS_PROPERTY, String.valueOf(use));
    }
    

    /**
     * Gets the flag indicating if view is to include commands in the tree of framesync events.
     * @return true to include commands, false if not
     */
    public boolean isOverlayCommands() {
        final String str = this.getConfigItem(OVERLAY_COMMANDS_PROPERTY);
        if (str == null) {
            return false;
        }
        return Boolean.parseBoolean(str);
    } 

	/**
     * {@inheritDoc}
	 */
	@Override
	protected void initToDefaults() {
	}
}
