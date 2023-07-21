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

import java.util.SortedSet;

import jpl.gds.tm.service.api.packet.ITelemetryPacketMessage;


/**
 * ProductsOnlyFilter is a packetFilter designed to filter 
 * incoming packet messages based upon APID and VCID, to ensure
 * they are product packets on the virtual channel being processed
 * by the product builder component using this filter. 
 *
 *
 */
public class ProductsOnlyFilter extends PacketFilter {

    private final SortedSet<Integer> apids;
    private final int vcid;
    
    /**
     * Creates an instance of ProductsOnlyFilter.
     * @param apids the list of product apids
     * @param vcid the the virtual channel ID of products to be filtered 
     */
    public ProductsOnlyFilter(final SortedSet<Integer> apids, final int vcid) {
        this.apids = apids;
        this.vcid = vcid;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.product.impl.builder.PacketFilter#matches(ITelemetryPacketMessage)
     */
    @Override
	public boolean matches(final ITelemetryPacketMessage packet) {
        if (packet == null) {
            return false;
        }
        /* Get VCID from packet info rather than frame info. */
        if (packet.getPacketInfo().getVcid() != vcid) {
            return false;
        }
        int apid = packet.getPacketInfo().getApid();
        if (apids == null) {
        	return false;
        }
        return apids.contains(Integer.valueOf(apid));
    }
}
