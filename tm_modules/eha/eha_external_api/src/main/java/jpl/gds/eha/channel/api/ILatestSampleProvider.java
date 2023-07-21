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
package jpl.gds.eha.channel.api;

/**
 * The ILatestSampleProvider interface is used to provide customer derivations
 * the ability to access samples in the downlink processor's local LAD. This interface cannot be used
 * directly by derivations. It is used by AMPCS algorithm initialization.
 * Customer classes should use the convenience methods in IDerivationAlgorithm
 * for accessing latest samples. It is the implementation of those methods that
 * rely on this interface.
 * <p>
 * <b>THIS IS A MULTI-MISSION CORE ADAPTATION INTERFACE</b>
 * <p>
 * <b>This is a controlled interface. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b>
 * 
 *
 */
public interface ILatestSampleProvider {
    /**
     * Gets the most recent value for a channel
     * 
     * @param id The ID of the channel whose most recent value is wanted
     * @param realtime true if the channel value desired is realtime, false 
     * for recorded
     * @param station station ID for this channel id.  Station is only used 
     * for monitor channels. For all other channels, station will be set to 0.
     * @return The most recent value of the channel, or null if no match
     */
    public IChannelValue getMostRecentValue(String id, boolean realtime,
            int station);
}