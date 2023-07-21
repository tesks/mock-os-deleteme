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
package jpl.gds.globallad.utilities;

import java.util.Collection;

import jpl.gds.common.config.types.VenueType;
import jpl.gds.context.api.TimeComparisonStrategyContextFlag;
import jpl.gds.eha.api.channel.IClientChannelValue;
import jpl.gds.evr.api.IEvr;
import jpl.gds.globallad.GlobalLadException;
import jpl.gds.globallad.data.IGlobalLADData.GlobalLadPrimaryTime;
import jpl.gds.globallad.data.factory.IGlobalLadDataFactory.DataSource;
import jpl.gds.globallad.data.factory.IGlobalLadDataFactory.RecordedState;
import jpl.gds.globallad.data.utilities.IGlobalLadQuery;

/**
 * Core version of the global lad query.  Contains the legacy lad query methods.
 */
public interface ICoreGlobalLadQuery extends IGlobalLadQuery {

    /**
     * Legacy interface method. 
     *  
     * @param timeStrategy 
     * @param scid
     * @param venue
     * @param sessionHost
     * @param sessionId
     * @param stationIdList
     * @return Collection of data that matches the query.
     * @throws GlobalLadException
     */
    public Collection<IClientChannelValue> getLadAsChannelValue(
    		TimeComparisonStrategyContextFlag timeStrategy,
    		int scid,
            VenueType venue, String sessionHost,
            Long sessionId, Iterable<Integer> stationIdList)
            throws GlobalLadException;

    
    /**
     * Queries the lad for the given channel.
     * 
     * @param channelId
     * @param source
     * @param recordedState
     * @param timeType
     * @param scid
     * @param venue
     * @param sessionHost
     * @param sessionId
     * @param stationIdList
     * @param maxResults
     * 
     * @return Collection of data that matches the query.
     * 
     * @throws GlobalLadException
     */
    public Collection<IClientChannelValue> getChannelHistory(String channelId, DataSource source , RecordedState recordedState, GlobalLadPrimaryTime timeType, int scid,
            VenueType venue, String sessionHost, Long sessionId, Iterable<Integer> stationIdList, int maxResults)
            throws GlobalLadException;

    
    /**
     * @param scid
     * @param venue
     * @param sessionHost
     * @param sessionId
     * @param stationIdList
     * @return Collection of data that matches the query.
     * @throws GlobalLadException
     */
    public Collection<IEvr> getLadAsEvrValue(int scid,
            VenueType venue, String sessionHost,
            Long sessionId, Iterable<Integer> stationIdList)
            throws GlobalLadException;
}
