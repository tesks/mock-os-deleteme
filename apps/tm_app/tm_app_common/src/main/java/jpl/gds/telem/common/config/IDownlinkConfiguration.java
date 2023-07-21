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
package jpl.gds.telem.common.config;

/**
 * Common interface for applications using a downlink configuration
 *
 */
public interface IDownlinkConfiguration {

    /**
     * Gets the downlink feature set, indicating which downlink processor features
     * are enabled and disabled in the configuration.
     *
     * @return the populated feature set object
     */
    IDownlinkProperties getFeatureSet();


    /**
     * Sets the feature set indicating which downlink processor features
     * are enabled or disabled, temporarily overriding the values in the
     * configuration.
     *
     * @param featureSet The feature set object to set.
     */
    void setFeatureSet(final IDownlinkProperties featureSet);

    /**
     * Gets the use database flag, indicating whether the downlink processor
     * will write to the databases.
     *
     * @return true if writing to the database, false if not
     */
    boolean isUseDb();

    /**
     * Sets the use database flag, indicating whether the downlink processor
     * will write to the database. Any value set by this call
     * temporarily overrides the value in the configuration.
     *
     * @param useDb The useDb to set.
     */
    void setUseDb(final boolean useDb);


    /**
     * Gets the use message service flag, indicating whether the downlink
     * processor will publish messages to the message service.
     *
     * @return true if publishing, false if not
     */
    boolean isUseMessageService();

    /**
     * Sets the use message service flag, indicating whether the downlink processor
     * will publish messages. Any value set by this call
     * temporarily overrides the value in the configuration.
     *
     * @param useJms The useJms to set.
     */
    void setUseMessageService(final boolean useJms);
}
