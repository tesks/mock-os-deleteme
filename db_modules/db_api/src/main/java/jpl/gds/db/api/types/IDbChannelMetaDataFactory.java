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


public interface IDbChannelMetaDataFactory
        extends IDbQueryableFactory<IDbChannelMetaDataProvider, IDbChannelMetaDataUpdater> {

    /**
     * Creates an instance of IDbChannelMetaDataProvider.
     * 
     * @param channelStem
     *            the channel's stem
     * @param channelId
     *            the channel's ID
     * @param channelName
     *            the channel's name
     * @return an instance of an IDbChannelMetaDataProvider
     */
    IDbChannelMetaDataProvider createQueryableProvider(String channelStem, String channelId, String channelName);

    /**
     * Creates an instance of IDbChannelMetaDataProvider.
     * 
     * @param channelStem
     *            the channel's stem
     * @param channelId
     *            the channel's ID
     * @param channelName
     *            the channel's name
     * @param count
     *            the number of channel values with this metadata
     * @return an instance of an IDbChannelMetaDataProvider
     */
    IDbChannelMetaDataProvider createQueryableProvider(String channelStem, String channelId, String channelName,
                                                       int count);

    /**
     * Creates an instance of IDbChannelMetaDataUpdater.
     * 
     * @param channelStem
     *            the channel's stem
     * @param channelId
     *            the channel's ID
     * @param channelName
     *            the channel's name
     * @return an instance of an IDbChannelMetaDataUpdater
     */
    IDbChannelMetaDataUpdater createQueryableUpdater(String channelStem, String channelId, String channelName);

    /**
     * Creates an instance of IDbChannelMetaDataUpdater.
     * 
     * @param channelStem
     *            the channel's stem
     * @param channelId
     *            the channel's ID
     * @param channelName
     *            the channel's name
     * @param count
     *            the number of channel values with this metadata
     * @return an instance of an IDbChannelMetaDataUpdater
     */
    IDbChannelMetaDataUpdater createQueryableUpdater(String channelStem, String channelId, String channelName,
                                                     int count);
}