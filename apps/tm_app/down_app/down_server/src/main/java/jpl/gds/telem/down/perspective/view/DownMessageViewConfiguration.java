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
package jpl.gds.telem.down.perspective.view;

import org.springframework.context.ApplicationContext;

import jpl.gds.perspective.config.PerspectiveProperties;
import jpl.gds.perspective.config.ViewType;
import jpl.gds.perspective.view.ViewConfiguration;

/**
 * DownMessageViewConfiguration encapsulates the view configuration for the
 * general message list in the downlink GUI.
 * 
 *
 */
public class DownMessageViewConfiguration extends ViewConfiguration {
    private static final int SHOW_INFO = 0;
    private static final int SHOW_WARNING = 1;
    private static final int SHOW_ERROR = 2;
    /** Deprecate FATAL log message filtering but maintain backwards compatibility */

    private static final String DEFAULT_ROWS_CONFIG = "defaultRows";

    private static final String MAX_ROWS_CONFIG = "maxRows";
    private static final String SHOW_LOG_CONFIG = "showLog";
    private static final String SHOW_FRAMESYNC_CONFIG = "showFramesync";
    private static final String SHOW_PACKETEXTRACT_CONFIG = "showPacketExtract";
    private static final String SHOW_RAWDATA_CONFIG = "showRawData";
    private static final String SHOW_CONTROL_CONFIG = "showTestControl";
    private static final String LOG_FILTER_CONFIG = "logFilters";

    private static final int DEFAULT_MAX_ROWS = 500;

    private boolean[] logFilters;
    
    /**
     * Creates an instance of DownMessageViewConfiguration.
     * @param appContext the current application context
     */
    public DownMessageViewConfiguration(final ApplicationContext appContext) {
        super(appContext);
        initToDefaults();
    }

    /**
     * Gets the value of the show frame sync flag
     * 
     * @return Returns the show frame sync messages flag
     */
    public boolean isShowFrameSync() {
        return this.getConfigItem(SHOW_FRAMESYNC_CONFIG).equals("true");
    }

    /**
     * Sets the show framesync messages flag.
     * 
     * @param showFrameSync
     *            true to show frame sync messages in the downlink window
     */
    public void setShowFrameSync(final boolean showFrameSync) {
        this
                .setConfigItem(SHOW_FRAMESYNC_CONFIG, String
                        .valueOf(showFrameSync));
    }

    /**
     * Gets the value of the show raw data flag
     * 
     * @return Returns the show raw data messages flag
     */
    public boolean isShowRawData() {
        if (this.getConfigItem(SHOW_RAWDATA_CONFIG) == null) {
            setShowRawData(true);
            return true;
        }
        return this.getConfigItem(SHOW_RAWDATA_CONFIG).equals("true");
    }

    /**
     * Sets the show raw data messages flag.
     * 
     * @param showRawData
     *            true to show raw data messages in the downlink window
     */
    public void setShowRawData(final boolean showRawData) {
        this.setConfigItem(SHOW_RAWDATA_CONFIG, String.valueOf(showRawData));
    }

    /**
     * Gets the value of the show log flag
     * 
     * @return Returns the show log messages flag.
     */
    public boolean isShowLog() {
        return this.getConfigItem(SHOW_LOG_CONFIG).equals("true");
    }

    /**
     * Sets the show log messages flag.
     * 
     * @param showLog
     *            true to show log messages in the downlink window
     */
    public void setShowLog(final boolean showLog) {
        this.setConfigItem(SHOW_LOG_CONFIG, String.valueOf(showLog));
    }

    /**
     * Gets the value of the show packet extract flag
     * 
     * @return Returns the show packet extract messages flag.
     */
    public boolean isShowPacketExtract() {
        return this.getConfigItem(SHOW_PACKETEXTRACT_CONFIG).equals("true");
    }

    /**
     * Sets the show packet extract messages flag.
     * 
     * @param showPacketExtract
     *            true to show messages in the downlink window.
     */
    public void setShowPacketExtract(final boolean showPacketExtract) {
        this.setConfigItem(SHOW_PACKETEXTRACT_CONFIG, String
                .valueOf(showPacketExtract));
    }

    /**
     * Gets the value of the show test control flag
     * 
     * @return Returns the show test control messages flag.
     */
    public boolean isShowTestControl() {
        return this.getConfigItem(SHOW_CONTROL_CONFIG).equals("true");
    }

    /**
     * Sets the show test control messages flag.
     * 
     * @param showTestControl
     *            true to show messages in the downlink window
     */
    public void setShowTestControl(final boolean showTestControl) {
        this
                .setConfigItem(SHOW_CONTROL_CONFIG, String
                        .valueOf(showTestControl));
    }

    /**
     * Sets the log filters for display of log messages in the downlink window.
     * 
     * @param showInfo
     *            true to enable display of informational log messages
     * @param showWarn
     *            true to enable display of warning log messages
     * @param showError
     *            true to enable display of error log messages
     * @param showFatal
     *            true to enable display of fatal log messages
     */
    @Deprecated
    public void setLogFilters(final boolean showInfo, final boolean showWarn,
            final boolean showError, final boolean showFatal) {
        final String flags = String.valueOf(showInfo) + "," + String.valueOf(showWarn) + "," + String.valueOf(showError)
                + "," + String.valueOf(showFatal);
        this.setConfigItem(LOG_FILTER_CONFIG, flags);
        loadLogFilters();
    }

    /**
     * Sets the log filters for display of log messages in the downlink window.
     * 
     * @param showInfo
     *            true to enable display of informational log messages
     * @param showWarn
     *            true to enable display of warning log messages
     * @param showError
     *            true to enable display of error log messages
     */
    public void setLogFilters(final boolean showInfo, final boolean showWarn, final boolean showError) {
        final String flags = String.valueOf(showInfo) + "," + String.valueOf(showWarn) + ","
                + String.valueOf(showError);
        this.setConfigItem(LOG_FILTER_CONFIG, flags);
        loadLogFilters();
    }

    /**
     * Gets value of show info log flag
     * 
     * @return true if informational log messages should be displayed; false if
     *         not
     */
    public boolean isShowInfoLog() {
        loadLogFilters();
        return this.logFilters[SHOW_INFO];
    }

    /**
     * Gets value of show warning log flag
     * 
     * @return true if warning log messages should be displayed; false if not
     */
    public boolean isShowWarningLog() {
        loadLogFilters();
        return this.logFilters[SHOW_WARNING];
    }

    /**
     * Gets value of show error log flag
     * 
     * @return true if error log messages should be displayed; false if not
     */
    public boolean isShowErrorLog() {
        loadLogFilters();
        return this.logFilters[SHOW_ERROR];
    }

    /**
     * Gets value of show fatal log flag
     * 
     * @return true if informational log messages should be displayed; false if
     *         not
     */
    @Deprecated
    public boolean isShowFatalLog() {
        loadLogFilters();
        return this.logFilters[SHOW_ERROR];
    }

    private void loadLogFilters() {
        final String types = this.getConfigItem(LOG_FILTER_CONFIG);
        if (types != null) {
            final String[] filters = types.split(",");
            if (!(filters.length == 3 || filters.length == 4)) {
                trace.error("Log filters (", LOG_FILTER_CONFIG, "=", types,
                            ") for downlink message view have an invalid format in the configuration file");
                setLogFilters(true, true, true);
            }
            this.logFilters = new boolean[filters.length];
            for (int i = 0; i < this.logFilters.length; i++) {
                this.logFilters[i] = Boolean.parseBoolean(filters[i]);
            }
        }
    }

    /**
     * Gets the maximum number of rows/messages to display at one time.
     * 
     * @return the number of rows
     */
    public int getMaxRows() {
        final String str = this.getConfigItem(MAX_ROWS_CONFIG);
        if (str == null) {
            return DEFAULT_MAX_ROWS;
        }
        return Integer.parseInt(str);
    }

    /**
     * Sets the maximum number of rows/messages to display at one time.
     * 
     * @param rows
     *            the number of rows to set
     */
    public void setMaxRows(final int rows) {
        this.setConfigItem(MAX_ROWS_CONFIG, String.valueOf(rows));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void initToDefaults() {
        
		final PerspectiveProperties pp = appContext.getBean(PerspectiveProperties.class);
        super.initToDefaults(pp.getViewProperties(ViewType.DOWN_MESSAGE),
                "jpl.gds.telem.down.gui.MessageTable", null, null);

        setMaxRows(viewProperties.getIntegerDefault(DEFAULT_ROWS_CONFIG, DEFAULT_MAX_ROWS));
        setShowFrameSync(viewProperties.getBooleanDefault(SHOW_FRAMESYNC_CONFIG, true));
        setShowPacketExtract(viewProperties.getBooleanDefault(SHOW_PACKETEXTRACT_CONFIG, true));
        setShowTestControl(viewProperties.getBooleanDefault(SHOW_CONTROL_CONFIG, true));
        setShowLog(viewProperties.getBooleanDefault(SHOW_LOG_CONFIG, true));
        final String types = viewProperties.getStringDefault(LOG_FILTER_CONFIG, "true,true,true");
        if (types != null) {
            final String[] filters = types.split(",");
            // Deprecate FATAL filtering but need to keep backwards compatibility (size 4)
            if (!(filters.length == 4 || filters.length == 3)) {
                trace.error("Default log filters for downlink message view have an invalid format in the configuration file: ",
                            types);
                setLogFilters(true, true, true);
            }
            this.logFilters = new boolean[filters.length];
            for (int i = 0; i < this.logFilters.length; i++) {
                this.logFilters[i] = Boolean.parseBoolean(filters[i]);
            }
        }
    }
}
