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
package jpl.gds.product.impl.builder;

import jpl.gds.product.api.builder.IProductPacketFilter;
import jpl.gds.tm.service.api.packet.ITelemetryPacketMessage;


/**
 * PacketFilter provides a common interface for product packet filtering 
 * classes.
 *
 *
 */
public abstract class PacketFilter implements IProductPacketFilter {
    /**
     * Determines whether the given Packet matches the filter
     * criteria.
     * @param packet the IPacketMessage to match
     * @return true if the Packet "passes" the filter; false otherwise
     */
    @Override
    public abstract boolean matches(ITelemetryPacketMessage packet);
}
