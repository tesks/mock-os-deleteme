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
package jpl.gds.monitor.fixedbuilder;

import java.io.File;

import jpl.gds.perspective.AbstractUserSettings;
import jpl.gds.shared.config.GdsSystemProperties;
import jpl.gds.shared.swt.types.ChillColor;
import jpl.gds.shared.swt.types.ChillColor.ColorName;
import jpl.gds.shared.swt.types.CoordinateSystemType;

/**
 * 
 * FixedBuilderSettings is s specific type of AbstractUserSettings that contains
 * user-specific preferences for the fixed view builder. The data is kept in
 * <GdsUserConfigDir>/FixedBuilderSettings.properties.
 * 
 */
@SuppressWarnings("serial")
public class FixedBuilderSettings extends AbstractUserSettings {

    /**
     * The names of the settings, or configuration variables, in this settings
     * object.
     * 
     */
    public enum Setting {
        
        /**
         * Flag in the builder properties file that indicates whether the 
         * builder dictionary replacement confirmation dialog should be shown 
         * in the future.
         */
        BUILDER_DICT_REPLACE_CONFIRM("builder.dictionary.replace.confirm"), 
        
        /**
         * Flag in builder properties file that indicates whether the user's 
         * last answer to the builder dictionary replace confirmation was yes 
         * (true) or no (false).
         */
        BUILDER_DICT_REPLACE_ANSWER("builder.dictionary.replace.answer"), 
        
        /**
         * Flag in the builder properties file that indicates whether the view 
         * dictionary replacement confirmation dialog should be shown in the 
         * future.
         */
        VIEWER_DICT_REPLACE_CONFIRM("view.dictionary.replace.confirm"), 
        
        /**
         * Flag in the builder properties file that indicates whether the 
         * user's last answer to the viewer dictionary replace confirmation 
         * was yes (true) or no (false).
         */
        VIEWER_DICT_REPLACE_ANSWER("view.dictionary.replace.answer"), 
        
        /**
         * Flag in the builder properties file that indicates whether to 
         * scroll the message in the builder status bar.
         */
        TICKER_ENABLE("builder.ticker.enable"), 
        
        /**
         * Flag in the builder properties file that indicates whether to draw 
         * a grid on the builder canvas.
         */
        GRID_ENABLE("builder.grid.enable"), 
        
        /**
         * Property in the builder properties file that indicates the grid 
         * spacing, in pixels or characters. The unit is assumed to match the 
         * current coordinate system.
         */
        GRID_SIZE("builder.grid.size"), 
        
        /**
         * Property in the builder properties file that indicates the 
         * configured grid color. Default is light gray.
         */
        GRID_COLOR("builder.grid.color"), 
        
        /**
         * Property in the builder properties file that indicates which 
         * coordinate system to use (character or pixel)
         */
        COORDINATE_SYSTEM("builder.coordinate.system");

        /**
         * The name of the setting's property in the properties file.
         */
        private String configName;

        /**
         * Constructs a Setting object with the given property name.
         * 
         * @param name
         *            property name of the setting
         */
        private Setting(final String name) {
            this.configName = name;
        }

        /**
         * Gets the property name associated with this Setting.
         * 
         * @return the property name
         */
        public String getConfigName() {
            return this.configName;
        }
    }

    /**
     * Path to the Java property file containing settings.
     */
    public static final String BUILDER_SETTINGS_FILE = GdsSystemProperties
            .getUserConfigDir()
            + File.separator + "FixedBuilderSettings.properties";

    /**
     * Creates an instance of FixedBuilderSettings.
     */
    public FixedBuilderSettings() {
        super(BUILDER_SETTINGS_FILE);
    }

    /**
     * Indicates whether the user's last answer to the builder dictionary
     * replace confirmation was yes (true) or no (false).
     * 
     * @return true if builder dictionary should be replaced, false if not;
     *         default is true
     */
    public boolean getBuilderDictReplaceAnswer() {
        return getBoolean(Setting.BUILDER_DICT_REPLACE_ANSWER.getConfigName(),
                true);
    }

    /**
     * Returns the configured coordinate system. Default is CHARACTER.
     * 
     * @return the CoordinateSystemType
     */
    public CoordinateSystemType getCoordinateSystem() {
        final String str = getProperty(Setting.COORDINATE_SYSTEM
                .getConfigName(), CoordinateSystemType.CHARACTER.toString());
        final CoordinateSystemType type = Enum.valueOf(
                CoordinateSystemType.class, str);
        return type;
    }

    /**
     * Returns the configured grid color. Default is light gray.
     * 
     * @return the ChillColor object representing the grid color
     */
    public ChillColor getGridColor() {
        final String col = getProperty(Setting.GRID_COLOR.getConfigName(),
                new ChillColor(ColorName.LIGHT_GREY).getRgbString());
        return new ChillColor(col);
    }

    /**
     * Returns the configured grid spacing, in pixels or characters. The unit is
     * assumed to match the currently configured coordinate system. Default is 2
     * characters.
     * 
     * @see #getCoordinateSystem()
     * 
     * @return grid size
     */
    public int getGridSize() {
        return getInteger(Setting.GRID_SIZE.getConfigName(), 2);
    }

    /**
     * Indicates whether the user's last answer to the viewer dictionary replace
     * confirmation was yes (true) or no (false).
     * 
     * @return true if viewer dictionary should be replaced, false if not;
     *         default value is false
     */
    public boolean getViewerDictReplaceAnswer() {
        return getBoolean(Setting.VIEWER_DICT_REPLACE_ANSWER.getConfigName(),
                false);
    }

    /**
     * Indicates whether the configured coordinate system is CHARACTER.
     * 
     * @return true if the CHARACTER coordinate system is currently enabled
     */
    public boolean isCharacterCoordinateSystem() {
        return getCoordinateSystem().equals(CoordinateSystemType.CHARACTER);
    }

    /**
     * Sets whether the user's last answer to the builder dictionary replace
     * confirmation was yes (true) or no (false).
     * 
     * @param enable
     *            true if builder dictionary should be replaced, false if not
     */
    public void setBuilderDictReplaceAnswer(final boolean enable) {
        setBoolean(Setting.BUILDER_DICT_REPLACE_ANSWER.getConfigName(), enable);
    }

    /**
     * Sets the builder coordinate system.
     * 
     * @param type
     *            the CoordinateSystemType to set
     */
    public void setCoordinateSystem(final CoordinateSystemType type) {
        setProperty(Setting.COORDINATE_SYSTEM.getConfigName(), type.toString());
    }

    /**
     * Sets the builder grid color.
     * 
     * @param col
     *            ChillColor to set
     */
    public void setGridColor(final ChillColor col) {
        setProperty(Setting.GRID_COLOR.getConfigName(), col.getRgbString());
    }

    /**
     * Sets the grid spacing, in pixels or characters. The unit is assumed to
     * match the current coordinate system.
     * 
     * @see #getCoordinateSystem()
     * 
     * @param size
     *            grid spacing in pixels or characters
     */
    public void setGridSize(final int size) {
        setInteger(Setting.GRID_SIZE.getConfigName(), size);
    }

    /**
     * Sets the flag indicating whether the builder dictionary replacement
     * confirmation dialog should be shown in the future.
     * 
     * @param enable
     *            true to enable dialog display
     */
    public void setShowBuilderDictReplaceConfirmation(final boolean enable) {
        setBoolean(Setting.BUILDER_DICT_REPLACE_CONFIRM.getConfigName(), enable);
    }

    /**
     * Sets the flag indicating whether the view dictionary replacement
     * confirmation dialog should be shown in the future.
     * 
     * @param enable
     *            true to enable dialog display
     */
    public void setShowViewerDictReplaceConfirmation(final boolean enable) {
        setBoolean(Setting.VIEWER_DICT_REPLACE_CONFIRM.getConfigName(), enable);
    }

    /**
     * Sets the flag indicating whether to draw a grid on the builder canvas.
     * 
     * @param enable
     *            true to enable grid, false to disable
     */
    public void setUseGrid(final boolean enable) {
        setBoolean(Setting.GRID_ENABLE.getConfigName(), enable);
    }

    /**
     * Sets whether to scroll the message in the builder status bar.
     * 
     * @param enable
     *            true to enable ticker, false to disable
     */
    public void setUseMessageTicker(final boolean enable) {
        setBoolean(Setting.TICKER_ENABLE.getConfigName(), enable);
    }

    /**
     * Sets whether the user's last answer to the viewer dictionary replace
     * confirmation was yes (true) or no (false).
     * 
     * @param enable
     *            true if viewer dictionary should be replaced, false if not
     */
    public void setViewerDictReplaceAnswer(final boolean enable) {
        setBoolean(Setting.VIEWER_DICT_REPLACE_ANSWER.getConfigName(), enable);
    }

    /**
     * Indicates whether the builder dictionary replacement confirmation dialog
     * should be shown in the future.
     * 
     * @return true if the dialog should be displayed, false if not
     */
    public boolean showBuilderDictReplaceConfirmation() {
        return getBoolean(Setting.BUILDER_DICT_REPLACE_CONFIRM.getConfigName(),
                true);
    }

    /**
     * Indicates whether the view dictionary replacement confirmation dialog
     * should be shown in the future.
     * 
     * @return true if the dialog should be displayed, false if not
     */
    public boolean showViewerDictReplaceConfirmation() {
        return getBoolean(Setting.VIEWER_DICT_REPLACE_CONFIRM.getConfigName(),
                true);
    }

    /**
     * Indicates whether to draw a grid on the builder canvas.
     * 
     * @return true if grid enabled, false if not; default is false
     */
    public boolean useGrid() {
        return getBoolean(Setting.GRID_ENABLE.getConfigName(), false);
    }

    /**
     * Indicates whether to scroll the message in the builder status bar.
     * 
     * @return true if ticker enabled, false if not
     */
    public boolean useMessageTicker() {
        return getBoolean(Setting.TICKER_ENABLE.getConfigName(), true);
    }
}
