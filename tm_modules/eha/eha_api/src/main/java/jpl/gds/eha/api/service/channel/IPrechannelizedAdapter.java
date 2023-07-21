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

import java.util.List;

import jpl.gds.eha.api.channel.IServiceChannelValue;
import jpl.gds.tm.service.api.packet.ITelemetryPacketMessage;

/**
 * The IEhaAdapter interface is to be implemented by all channel extraction 
 * adapters.
 * <p>
 * <b>THIS IS A MULTI-MISSION CORE ADAPTATION INTERFACE</b>
 * <p>
 * <b>This is a controlled interface. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b>
 * <p>
 * IEhaAdapter defines methods needed by the channel extraction adapters, so
 * that the channel processing system can use the adapter to perform
 * mission-specific extraction of channel values from packets. The primary job
 * of such an adapter is to extract IInternalChannelValues from an IPacketMessage and
 * return them. IEhaAdapters should only be created through the
 * EhaAdapterFactory class, and should interact with channel values and packets
 * only through the IInternalChannelValue and IPacketMessage interfaces. Interaction
 * with the actual implementation classes in an IEhaAdapter implementation is
 * contrary to multi-mission development standards.
 * <p>
 * It is also important to note than the class that implements this interface should
 * create IInternalChannelValues using the ChannelValueFactory, and MUST set the EU of each
 * channel value if one is defined.
 * 
 * @see ITelemetryPacketMessage
 * @see IServiceChannelValue
 */
public interface IPrechannelizedAdapter {
    /**
     * Extracts IInternalChannelValues from the given IPacketMessage. The passed IPacketMessage
     * is guaranteed to contain a valid packet with a pre-channelized APID, including the
     * packet primary and secondary headers. The packet may contain either recorded or realtime 
     * content.  The returned IInternalChannelValues must have DN and EU set.  Timestamps need 
     * not be set, as they will be filled in by the channel processing system from the packet
     * header. If they are set by the adapter, they will be left alone.
     * 
     * @param pm the IPacketMessage containing the EHA packet to process
     * @return a list of IInternalChannelValues; if no values are extracted from the packet, an
     * empty list must be returned
     * 
     * @throws PrechannelizedAdapterException if any error occurs during the extraction of channel values
     */
    public List<IServiceChannelValue> extractEha(final ITelemetryPacketMessage pm) throws PrechannelizedAdapterException;
    
    /**
     * Extracts IInternalChannelValues from the given byte array. The passed array
     * is guaranteed to contain the data content of a pre-channelized APID, less the packet
     * header information. It may be either recorded or realtime content.  The returned
     * IInternalChannelValues must have DN and EU set.  Timestamps need not be set, as they will 
     * be filled in by the channel processing system from the packet header. If they are
     * set by the adapter, they will be left alone.
     * 
     * @param data the byte array of channel data
     * @param offset the starting offset into the byte array
     * @param length the number of bytes to process
     * @return List of IInternalChannelValue
     * 
     * @throws PrechannelizedAdapterException if any error occurs during the extraction of channel values
     */
    public List<IServiceChannelValue> extractEha(final byte[] data, final int offset, final int length) 
        throws PrechannelizedAdapterException;

    /**
      * Puts the IPrechannelizedAdapter into a strict mode of processing. If processing errors occur, the
      * EHA adapter will not return channel values extracted prior to the error's occurrence.
      *
      * @param isStrict if true, throw on error
      */
    void setStrict(boolean isStrict);

	/**
      * Check if the current IPrechannelizedAdapter is strict or not.
      * @return true if the adapter is strict
      */
	boolean isStrict();
}
