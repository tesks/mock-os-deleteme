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

import jpl.gds.shared.time.ISclk;


/** A simple interface for encapsulating information needed for handling
 *  secondary headers on telemetry space packets.
 *
 *
 */
public interface ISecondaryPacketHeader {
	
    /**
     * Gets the length in bytes of the secondary packet header. Note for some
     * missions, not all packets have the same secondary packet length.
     * 
     * @return secondary header length in bytes
     */
    public int getSecondaryHeaderLength();
    
    /**
     * Gets a ISclk to be associated with the packet that contained the secondary header.
     * Do not return null if no timestamp was available; use a ISclk with a 0, 0 coarse
     * and fine time. Must be expressed in terms of the canonical mission Sclk.
     * @return the ISclk stored in the secondary header.
     */
    public ISclk getSclk();
}
