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
package jpl.gds.db.api.types;

public interface IDbChannelMetaDataUpdater extends IDbChannelMetaDataProvider {

    /**
     * Set channel stem.
     *
     * @param channelStem the channelStem to set
     */
    void setChannelStem(String channelStem);

    /**
     * Set channel id.
     *
     * @param channelId the channelId to set
     */
    void setChannelId(String channelId);

    /**
     * Set channel name.
     *
     * @param channelName the channelName to set
     */
    void setChannelName(String channelName);

}