/*
 * Copyright 2006-2019. California Institute of Technology.
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

package jpl.gds.telem.common.config;

import jpl.gds.shared.config.IWritableProperties;

import java.util.List;

/**
 * Common interface for downlink properties objects
 */
public interface IDownlinkProperties extends IWritableProperties {

    /**
     * Gets the ongoing mode flag, which indicates if processing of the current
     * session is to be continued until an end-of-session record is seen on the source session
     *
     * @return true if ongoing mode is enabled, false if disabled
     */
    boolean isEnableOngoingMode();


    /**
     * Gets the list of miscellaneous downlink feature managers' class names that are
     * configured.
     *
     * @return list of DownlinkFeatureManager to use with downlink session
     */
    List<String> getMiscFeatures();


    /**
     * Get configured FSW REST Port
     * @return FSW Rest port
     */
    int getRestPortFsw();


    /**
     * Get configured SSE REST Port
     * @return SSE Rest port
     */
    int getRestPortSse();


    /**
     * Gets the enable flag for miscellaneous downlink features.
     * @return true if miscellaneous downlink feature is enabled, false otherwise
     */
    public boolean isEnableMiscFeatures();

}
