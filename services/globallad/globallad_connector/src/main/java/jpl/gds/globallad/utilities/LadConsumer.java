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
import java.util.Set;

import org.springframework.context.ApplicationContext;

import jpl.gds.common.config.types.VenueType;
import jpl.gds.context.api.IContextIdentification;
import jpl.gds.context.api.IVenueConfiguration;
import jpl.gds.context.api.TimeComparisonStrategyContextFlag;
import jpl.gds.eha.api.channel.IClientChannelValue;
import jpl.gds.globallad.GlobalLadException;

/**
 * The LAD consumer is the interface between AMPCS and the specific LAD storage.
 * It provides a way for AMPCS components to operate with LAD data without
 * having to be aware of how to access the LAD storage.
 */
public class LadConsumer {
    private final ICoreGlobalLadQuery queryApi;
    private final TimeComparisonStrategyContextFlag timeStrategy;
    
    /**
     * @throws Exception - error creating the global lad data factory.
     */
    public LadConsumer(final TimeComparisonStrategyContextFlag timeStrategy, final ICoreGlobalLadQuery queryApi) throws Exception {
        this.timeStrategy = timeStrategy;
        this.queryApi = queryApi;
    }
    
    public void setFilterType() {
    	
    }
  
    public Collection<IClientChannelValue> getLadAsChannelValues(final ApplicationContext appContext,
            final Set<Integer> monDataStations) throws GlobalLadException {
        
        final int scid = appContext.getBean(IContextIdentification.class).getSpacecraftId();
        final VenueType venue = appContext.getBean(IVenueConfiguration.class).getVenueType();
        final String sessionHost = appContext.getBean(IContextIdentification.class).getHost();
        final long sessionId = appContext.getBean(IContextIdentification.class).getNumber();

        return this.queryApi.getLadAsChannelValue(timeStrategy, scid, venue, sessionHost,
                        sessionId, monDataStations);
    }
}
