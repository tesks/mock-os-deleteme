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
package jpl.gds.perspective;

import java.io.File;

import jpl.gds.shared.config.GdsSystemProperties;

/**
 * 
 * PromptSettings is manages a user property file that stores flags for
 * "don't prompt for this again" GUI questions. The data is kept in
 * $HOME/CHILL/PromptSettings.properties.
 * 
 *
 */
@SuppressWarnings("serial")
public class PromptSettings extends AbstractUserSettings {
	private static final String PROMPT_SETTINGS_FILE = GdsSystemProperties
			.getUserConfigDir() + File.separator + "PromptSettings.properties";
	/**
	 * Configuration property location for perspective exit confirmation
	 */
	public static final String PERSPECTIVE_EXIT_PROPERTY = "perspective.exit.confirm";
	
	/**
	 * Configuration property location for monitor exit confirmation
	 */
	public static final String MONITOR_EXIT_PROPERTY = "monitor.exit.confirm";
	
	/**
	 * Configuration property location for monitor tab exit confirmation
	 */
	public static final String MONITOR_TAB_CLOSE_PROPERTY = "monitor.tab.close.confirm";
	
	/**
	 * Configuration property location for monitor plot preferences change
	 * confirmation
	 * 
	 */
	public static final String MONITOR_PLOT_PREFS_CHANGE_PROPERTY = "monitor.plot.prefs.change.confirm";

	/**
	 * Configuration property location for monitor EVR preferences change
	 * confirmation
	 * 
	 */
	public static final String MONITOR_EVR_PREFS_CHANGE_PROPERTY = "monitor.evr.prefs.change.confirm";

	/**
	 * Creates an instance of PromptSettings.
	 */
	public PromptSettings() {
		super(PROMPT_SETTINGS_FILE);
	}

	/**
	 * Indicates whether the perspective exit confirmation dialog should be
	 * shown.
	 * 
	 * @return true if the dialog should be displayed
	 */
	public boolean showPerspectiveExitConfirmation() {
		return getBoolean(PERSPECTIVE_EXIT_PROPERTY, true);
	}

	/**
	 * Sets the flag indicating whether the perspective exit confirmation dialog
	 * should be shown.
	 * 
	 * @param enable true to enable dialog display
	 */
	public void setPerspectiveExitConfirmation(boolean enable) {
		setProperty(PERSPECTIVE_EXIT_PROPERTY, String.valueOf(enable));
	}

	/**
	 * Sets the flag indicating whether the monitor exit confirmation dialog
	 * should be shown.
	 * 
	 * @param enable true to enable dialog display
	 */
	public void setMonitorExitConfirmation(boolean enable) {
		setProperty(MONITOR_EXIT_PROPERTY, String.valueOf(enable));
	}

	/**
	 * Indicates whether the monitor exit confirmation dialog should be shown.
	 * 
	 * @return true if the dialog should be displayed
	 */
	public boolean showMonitorExitConfirmation() {
		return getBoolean(MONITOR_EXIT_PROPERTY, true);
	}
	
    /**
     * Sets the flag indicating whether the monitor channel plot preferences
     * change confirmation dialog should be shown.
     * 
     * @param enable
     *            true to enable dialog display
     * 
     */
    public void setMonitorPlotPreferencesConfirmation(boolean enable) {

        setProperty(MONITOR_PLOT_PREFS_CHANGE_PROPERTY, String.valueOf(enable));
    }

    /**
     * Indicates whether the monitor channel plot preferences change
     * confirmation dialog should be shown.
     * 
     * @return true if the dialog should be displayed
     * 
     */
    public boolean showMonitorPlotPreferencesConfirmation() {

        return getBoolean(MONITOR_PLOT_PREFS_CHANGE_PROPERTY, true);
    }

    /**
     * Sets the flag indicating whether the monitor EVR preferences change
     * confirmation dialog should be shown.
     * 
     * @param enable
     *            true to enable dialog display
     * 
     */
    public void setMonitorEvrPreferencesConfirmation(boolean enable) {

        setProperty(MONITOR_EVR_PREFS_CHANGE_PROPERTY, String.valueOf(enable));
    }

    /**
     * Indicates whether the monitor EVR preferences change confirmation dialog
     * should be shown.
     * 
     * @return true if the dialog should be displayed
     * 
     */
    public boolean showMonitorEvrPreferencesConfirmation() {

        return getBoolean(MONITOR_EVR_PREFS_CHANGE_PROPERTY, true);
    }

	/**
	 * Sets the flag indicating whether the monitor tab closure confirmation
	 * dialog should be shown.
	 * 
	 * @param enable true to enable dialog display
	 */
	public void setMonitorTabCloseConfirmation(boolean enable) {
		setProperty(MONITOR_TAB_CLOSE_PROPERTY, String.valueOf(enable));
	}

	/**
	 * Indicates whether the monitor tab closure confirmation dialog should be
	 * shown.
	 * 
	 * @return true if the dialog should be displayed
	 */
	public boolean showMonitorTabCloseConfirmation() {
		return getBoolean(MONITOR_TAB_CLOSE_PROPERTY, true);
	}
}
