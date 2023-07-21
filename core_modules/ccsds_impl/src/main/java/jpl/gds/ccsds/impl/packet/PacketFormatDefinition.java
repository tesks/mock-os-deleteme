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
package jpl.gds.ccsds.impl.packet;

import jpl.gds.ccsds.api.packet.IPacketFormatDefinition;


class PacketFormatDefinition implements IPacketFormatDefinition {
    
    private final String packetHeaderClass;
    private final IPacketFormatDefinition.TypeName type;
    

    protected PacketFormatDefinition(final IPacketFormatDefinition.TypeName type) {
        this.type = type;
        this.packetHeaderClass = type.getDefaultPacketHeaderClass();
    }
    

    protected PacketFormatDefinition(final IPacketFormatDefinition.TypeName type, final String headerClass) {
        this.type = type;
        this.packetHeaderClass = headerClass;
    }
    

    @Override
    public String getPacketHeaderClass() {
        return packetHeaderClass;
    }
    
    @Override
    public IPacketFormatDefinition.TypeName getType() {
        return this.type;
    }
}
