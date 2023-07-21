/*
 * Copyright 2006-2020. California Institute of Technology.
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
package jpl.gds.globallad.data.container.search.query;

import jpl.gds.globallad.data.EhaGlobalLadData;
import jpl.gds.globallad.data.IGlobalLADData;
import jpl.gds.globallad.data.IGlobalLADData.GlobalLadPrimaryTime;
import jpl.gds.globallad.data.container.IGlobalLadSearchAlgorithm;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;


/**
 * Search algorithm to find and match EHA data.
 */
public class EhaQuerySearchAlgorithm extends BasicQuerySearchAlgorithm {

    private boolean alarmQuery;

    /**
     * Full private constructor.
     *
     * @param channelIds
     * @param hosts
     * @param venues
     * @param sessions
     * @param scids
     * @param dataTypes
     * @param hostWildCards
     * @param venueWildCards
     * @param sessionWildCards
     * @param dataTypeWildCards
     * @param channelIdWildCards
     * @param upperBoundCoarseTime
     * @param lowerBoundCoarseTime
     * @param upperBoundFineTime
     * @param lowerBoundFineTime
     * @param vcids
     * @param dssIds
     * @param timeType
     * @param matchOnNullOrEmpty
     */
    private EhaQuerySearchAlgorithm(Collection<Object> channelIds,
                                    Collection<String> hosts,
                                    Collection<String> venues,
                                    Collection<Long> sessions,
                                    Collection<Integer> scids,
                                    Collection<Byte> dataTypes,
                                    Collection<String> hostWildCards,
                                    Collection<String> venueWildCards,
                                    Collection<String> sessionWildCards,
                                    Collection<String> dataTypeWildCards,
                                    Collection<String> channelIdWildCards,
                                    long upperBoundCoarseTime,
                                    long lowerBoundCoarseTime,
                                    long upperBoundFineTime,
                                    long lowerBoundFineTime,
                                    Collection<Byte> vcids,
                                    Collection<Byte> dssIds,
                                    GlobalLadPrimaryTime timeType,
                                    boolean matchOnNullOrEmpty,
                                    boolean alarmQuery) {
        super(channelIds, hosts, venues, sessions, scids, dataTypes, hostWildCards,
                venueWildCards, sessionWildCards, dataTypeWildCards,
                channelIdWildCards, upperBoundCoarseTime, lowerBoundCoarseTime,
                upperBoundFineTime, lowerBoundFineTime, vcids, dssIds, timeType, matchOnNullOrEmpty);
        this.alarmQuery = alarmQuery;
    }

    @Override
    public boolean isSpecialMatched(IGlobalLADData data) {
        if (data instanceof EhaGlobalLadData) {
            EhaGlobalLadData eha = (EhaGlobalLadData) data;

            return isInAlarm(eha);
        } else {
            return false;
        }
    }

    /**
     * Check alarm state of EHA data
     *
     * @param eha
     * @return
     */
    private boolean isInAlarm(EhaGlobalLadData eha) {
        // if this is an alarm query, check alarms.
        if (alarmQuery) {
            return eha.getAlarmValueSet().inAlarm();
        } else {
            // if not an alarm query, do not disqualify this data as a match
            return true;
        }
    }

    /**
     * Created builder class and a create method.
     */

    /**
     * Creates an empty builder class.
     *
     * @return builder class.
     */
    public static EhaSearchAlgorithmBuilder createBuilder() {
        return new EhaSearchAlgorithmBuilder();
    }

    /**
     * Builder class to create a query search algorithm.
     * <p>
     * Note:  the default value for matchOnNullOrEmpty is true which means if nothing is set for that level it will
     * match.  To change this a call to set this value must be made.
     * <p>
     * This builder is the same as the basic query builder, only proxies the name for the identifiers to channel ids.
     */
    public static class EhaSearchAlgorithmBuilder extends BasicQuerySearchAlgorithmBuilder {

        private boolean alarmQuery = false;

        @Override
        public IGlobalLadSearchAlgorithm build() {
            return new EhaQuerySearchAlgorithm(identifiers == null ? null : new ArrayList<>(identifiers),
                    hosts,
                    venues,
                    sessionNumbers,
                    scids,
                    userDataTypes,
                    hostWildCards,
                    venueWildCards,
                    sessionNumberWildCards,
                    userDataTypeWildCards,
                    identifierWildCards,
                    upperBoundMilliseconds,
                    lowerBoundMilliseconds,
                    upperBoundNanoseconds,
                    lowerBoundNanoseconds,
                    vcids,
                    dssIds,
                    timeType,
                    matchOnNullOrEmpty,
                    alarmQuery);
        }

        /**
         * @param channelIds
         * @return this
         */
        public EhaSearchAlgorithmBuilder setChannelIds(Collection<String> channelIds) {
            setIdentifiers(channelIds);

            return this;
        }

        /**
         * @param channelId
         * @return
         */
        public EhaSearchAlgorithmBuilder setChannelId(String channelId) {
            if (channelId != null) {
                setChannelIds(Arrays.asList(channelId));
            }

            return this;
        }

        /**
         * @param channelIdWildCards
         * @return this
         */
        public EhaSearchAlgorithmBuilder setChannelIdWildCards(Collection<String> channelIdWildCards) {
            setIdentifierWildCards(channelIdWildCards);

            return this;
        }

        /**
         * @param channelIdWildCard
         * @return
         */
        public EhaSearchAlgorithmBuilder setChannelIdWildCard(String channelIdWildCard) {
            if (channelIdWildCard != null) {
                setChannelIdWildCards(Arrays.asList(channelIdWildCard));
            }

            return this;
        }

        /**
         * @param alarmQuery
         * @return
         */
        public EhaSearchAlgorithmBuilder setAlarmQuery(boolean alarmQuery) {
            this.alarmQuery = alarmQuery;

            return this;
        }
    }
}
