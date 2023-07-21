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
package jpl.gds.eha.api.feature;

import jpl.gds.common.service.telem.ITelemetryFeatureManager;
import jpl.gds.eha.api.service.alarm.IAlarmNotifierService;
import jpl.gds.eha.api.service.channel.ISuspectChannelService;

/**
 * An interface for the EHA feature manager.
 * 
 * @since R8
 */
public interface IEhaFeatureManager extends ITelemetryFeatureManager {

    /**
     * Generic decom can be treated as a "sub"-feature to this EHA
     * feature.
     * 
     * @param isEnabled boolean value to turn on/off generic decom
     */
    void enableGenericDecom(boolean isEnabled);

    /**
     * Generic decom may publish EVRs, but can be ignored. This method
     * controls whether any EVRs encountered are published or not.
     * @param isEnabled boolean value to turn on/off EVR publishing
     */
    void enableGenericEvrDecom(boolean isEnabled);

    /**
     * Enables or disables alarm processing.
     * 
     * @param isEnabled true to enable, false if not
     */
    void enableAlarmProcessing(boolean isEnabled);

    /**
     * Gets the ISuspectChannelService object used by this feature manager.
     * @return ISuspectChannelService object, or null if this object not initialized
     */
    ISuspectChannelService getSuspectChannelService();

    /**
     * Gets the Alarm Notifier object used by this feature manager.
     * 
     * @return Alarm Notifier object, or null if this object not initialized
     */
    IAlarmNotifierService getAlarmNotifier();

}