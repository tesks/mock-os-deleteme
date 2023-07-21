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
package jpl.gds.ccsds.api.tm.packet;

import jpl.gds.ccsds.api.packet.ISpacePacketHeader;

/**
 * Interface to be implemented by secondary packet header lookup classes, which are
 * used to get the proper extractor for the secondary header in a telemetry packet.
 * 
 * @since R8
 */
public interface ISecondaryPacketHeaderLookup {

    /** 
     * Fetch a secondary header extractor corresponding to the packet metadata given in the IPacketHeader
     *  argument.
     * @param primaryHeader an header object that will be used to look up the extractor.  Must be initialized.
     * @return an extractor that can be used with the packet corresponding to the primaryHeader
     */
    public ISecondaryPacketHeaderExtractor lookupExtractor(
            ISpacePacketHeader primaryHeader);

    /**
     * Fetch the error message associated with a secondary header extractor that
     * could not be loaded.
     * @param apid the APID for which the secondary header extractor could not be loaded
     * @return the error string, or an empty string if the apid does not have an associated error
     */
    public String failureReasonFor(short apid);
    
    /**
     * Gets an instance of a Null secondary header, used when no secondary header is defined.
     * @return packet header instance
     */
    public ISecondaryPacketHeader getNullSecondaryHeaderInstance();

}