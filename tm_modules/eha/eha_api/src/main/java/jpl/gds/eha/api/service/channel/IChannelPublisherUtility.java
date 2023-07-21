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
package jpl.gds.eha.api.service.channel;

import java.util.Collection;
import java.util.List;

import jpl.gds.eha.api.channel.IServiceChannelValue;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.ILocalSolarTime;
import jpl.gds.tm.service.api.packet.ITelemetryPacketMessage;

/**
 * An interface to be implemented by channel publisher utility objects, which
 * perform much of the work of channel message publication and channel
 * derivation.
 * 
 * @since R8
 */
public interface IChannelPublisherUtility {
    
    /**
     * Initialize the derivation tables in this utility instance. Must be called
     * before data starts to flow.
     */
    public void initDerivations();

    /**
     * Generates a stream id. This is an ID attached to streams of related
     * channel value messages.
     * 
     * @param trailer
     *            postfix to append to the string
     * @return Generated stream id
     */
    public String genStreamId(String trailer);

    /**
     * Derive channels from the channels in the given list. Note that the derived
     * channels are themselves candidates for derivation, around and around.
     * 
     * @param ehaList    The channels to use as parents for derivation
     * @param isRealtime true if these channel values are realtime,
     *                   else false if recorded
     * @param rct        The record creation time to use on derived channels
     * @param ert        The earth receive time to apply to new channels, or monitor time (mst) for monitor channels
     * @param scet       The spacecraft event time to apply to new channels; may be null
     * @param sclk       The spacecraft clock time to apply to new channels; mat be null
     * @param sol        The local solar time to apply to new channels; may be null 
     * @param dss		 The DSS ID; may be -1 to denote no ID
     * @param vcid       The virtual channel ID; may be null to denote no VCID
     */
    public void doChannelDerivations(List<IServiceChannelValue> ehaList,
                                     boolean isRealtime, IAccurateDateTime rct, IAccurateDateTime ert,
            IAccurateDateTime scet, ISclk sclk, ILocalSolarTime sol, int dss,
            Integer vcid);

    /**
     * Derive channels from the channels in the given list. Note that the derived
     * channels are themselves candidates for derivation, around and around.
     *
     * The new channels will all have the same packet id. This is because the initial
     * list comes from a single packet, so all have the same id.
    
     * The bit-unpack are one-to-one, so the child takes the id of the parent, so all
     * must come out the same, no matter how many levels we iterate.
     *
     * For trigger channel mode, the trigger channel determines the packet id, so that works
     * out like bit-unpack.
     *
     * For non-trigger channel mode, you need at least one parent not from the LAD, so that works out
     * the same as well.
     * 
     * @param ehaList     The channels to use as parents for derivation
     * @param isRealtime  true if these channel values are realtime,
     *                    else false if recorded
     * @param rct         The record creation time to use on derived channels
     * @param ert         The earth receive time to apply to new channels, or monitor time (mst) for monitor channels
     * @param scet        The spacecraft event time to apply to new channels; may be null
     * @param sclk        The spacecraft clock time to apply to new channels; mat be null
     * @param sol         The local solar time to apply to new channels; may be null 
     * @param dss		  The DSS ID; may be -1 to denote no ID
     * @param vcid        The virtual channel ID; may be null to denote no VCID
     * @param useTriggers true if derivation should use triggers, false if not
     */
    public void doChannelDerivations(List<IServiceChannelValue> ehaList,
                                     boolean isRealtime, IAccurateDateTime rct, IAccurateDateTime ert,
            IAccurateDateTime scet, ISclk sclk, ILocalSolarTime sol, int dss,
            Integer vcid, boolean useTriggers);

    /*
     * Wrapper method created below to address the said JIRA issue.
     */
    /**
     * Wrapper method for enclosing the publishing of flight channels and all
     * the derived channel into a single unit, surrounded by an opening
     * StartChannelProcMessage and a closing EndChannelProcMessage.
     * 
     * By default, this method publishes the flight channels then follows with
     * derivation calculations and publishing of them as well. But if
     * disableDerivations flag of "true" is passed, the second step is skipped
     * entirely.
     * 
     * @param disableDerivations
     *            true if derivations step should be skipped, false otherwise
     * @param ehaList
     *            the list of channel values to send.
     * @param rct
     *            The record creation time to use on these channels
     * @param ert
     *            The earth receive time, or monitor time (mst) for monitor
     *            channels
     * @param scet
     *            The spacecraft event time
     * @param sclk
     *            The spacecraft clock time
     * @param sol
     *            The local solar time
     * @param streamID
     *            the stream ID to attach to all messages
     * @param isRealtime
     *            true if the channel value is realtime, false if recorded
     * @param dss
     *            The DSS (receiving station) ID
     * @param vcid
     *            The virtual channel ID
     * @param useTriggers
     *            true if derivation should use triggers, false if not; if null,
     *            will use GDS configured value
     * 
     */
    public void publishFlightAndDerivedChannels(boolean disableDerivations, List<IServiceChannelValue> ehaList,
                                                IAccurateDateTime rct, IAccurateDateTime ert, IAccurateDateTime scet,
                                                ISclk sclk, ILocalSolarTime sol, String streamID, boolean isRealtime,
                                                int dss, Integer vcid, Boolean useTriggers);

    /**
     * Sends a sequence of EhaChannel Messages for the given list of channel values. The
     * RCT will be applied to every channel, while the ert, scet, sclk, and sol will be applied
     * only if the existing channel value contains a null for these. 
     * 
     * @param ehaList the list of channel values to send.
     * @param rct        The record creation time to use on these channels
     * @param ert        The earth receive time, or monitor time (mst) for monitor channels
     * @param scet       The spacecraft event time
     * @param sclk       The spacecraft clock time
     * @param sol        The local solar time
     * @param streamID the stream ID to attach to all messages
     * @param isRealtime true if the channel value is realtime, false if recorded
     * @param dss		 The DSS (receiving station) ID
     * @param vcid       The virtual channel ID
     */
    public void sendChannelMessages(List<IServiceChannelValue> ehaList, IAccurateDateTime rct, IAccurateDateTime ert,
                                    IAccurateDateTime scet, ISclk sclk, ILocalSolarTime sol, String streamID,
                                    boolean isRealtime, int dss, Integer vcid);

    /** 
     * Computes and sets the EU value for the given channel value. If there is no
     * EU conversion defined, does nothing.
     * 
     * @param chanval the IInternalChannelValue to convert and set EU for
     * 
     */
    public void computeAndSetEu(IServiceChannelValue chanval);
    
    /**
     * Assigns a packet ID to each channel value in the specified list.
     * 
     * @param ehaList
     *            the list of channel values that need packet ID
     * @param pm
     *            the ITelemetryPacketMessage containing the packet ID to use
     */
    public void assignPacketId(final Collection<IServiceChannelValue> ehaList,
            final ITelemetryPacketMessage pm);
}