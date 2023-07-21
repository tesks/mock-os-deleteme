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

import jpl.gds.decom.IDecomDelegate;
import jpl.gds.decom.IDecomListener;
import jpl.gds.eha.api.channel.IServiceChannelValue;
import jpl.gds.shared.time.IAccurateDateTime;
import jpl.gds.shared.time.ISclk;
import jpl.gds.tm.service.api.packet.ITelemetryPacketInfo;

import java.util.List;

/**
 * An interface to be implemented by decom channelization listeners.
 * 
 * @since R8
 */
public interface IChannelizationListener extends IDecomListener, IDecomDelegate {

    /**
     * Returns the channel values gathered by the listener.
     * 
     * @return list of channel values
     */
    public List<IServiceChannelValue> collectChannelValues();

    /**
     * Sets the timestamps and spacecraft ID to be applied to channel values
     * collected by this listener.
     * 
     * @param sclk
     *            the spacecraft clock time
     * @param scet
     *            the spacecraft event time
     * @param ert
     *            the earth received time
     * @param scid
     *            the spacecraft ID
     */
    void setCurrentTimes(ISclk sclk, IAccurateDateTime scet, IAccurateDateTime ert, int scid);


    /**
     * Set the parameters that will be used as the basis
     * for time tagging new channel values and evrs.
     *
     * @param pm packetInfo from the packet
     */
    void setPacketInfo(ITelemetryPacketInfo pm);
}
