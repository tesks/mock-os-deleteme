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
package jpl.gds.watcher.responder.app;

/**
 * An enum representing the currently implemented watcher apps. Used to provide the name of those apps for looking up
 * IResponderAppHelper instantiations appropriate for the specific watcher application.
 */
public enum ResponderAppName implements IResponderAppName {

    /** Name for channel change watcher app */
    CHANNEL_CHANGE_WATCHER_APP_NAME("chill_change_watcher"),

    /** Name for channel watcher app */
    CHANNEL_SAMPLE_WATCHER_APP_NAME("chill_channel_watcher"),

    /** Name for evr watcher app */
    EVR_WATCHER_APP_NAME("chill_evr_watcher"),

    /** Name for packet watcher app */
    PACKET_WATCHER_APP_NAME("chill_packet_watcher"),

    /** Name for recorded eng watcher app */
    RECORDED_ENG_WATCHER_APP_NAME("chill_recorded_eng_watcher"),

    /** Name for the product watcher app */
    PRODUCT_WATCHER_APP_NAME("chill_product_watcher"),

    /** Name for tiem correlation watcher app */
    TIME_CORRELATION_WATCHER_APP_NAME("chill_time_correlation_watcher"),

    /** Name for the trigger script app */
    TRIGGER_SCRIPT_APP_NAME("chill_trigger_script"),

    /** Name for the alarm watcher app */
    ALARM_WATCHER_APP_NAME("chill_alarm_watcher");

    private String scriptName;

    private ResponderAppName(final String realName) {
        this.scriptName = realName;
    }

    @Override
    public String getAppName() {
        return this.scriptName;
    }

    /**
     * Static method that searches the contents of this enum for the specific enum instance that represents the
     * specified appName
     * 
     * @param appName
     *            the name of the app for which the enum is being queried
     * @return the enum is being queried
     */
    public static IResponderAppName valueOfAppName(final String appName) {
        for (final ResponderAppName name : ResponderAppName.values()) {
            if (name.getAppName().equals(appName)) {
                return name;
            }
        }
        return null;
    }

}
