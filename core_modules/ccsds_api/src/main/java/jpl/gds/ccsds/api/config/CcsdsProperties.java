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
package jpl.gds.ccsds.api.config;

import jpl.gds.ccsds.api.packet.IPacketFormatDefinition;
import jpl.gds.ccsds.api.packet.PacketFormatFactory;
import jpl.gds.shared.config.GdsHierarchicalProperties;
import jpl.gds.shared.spring.context.flag.SseContextFlag;

/**
 * A configuration properties object for CCSDS-project properties.
 * 
 *
 * @since R8
 */
public class CcsdsProperties extends GdsHierarchicalProperties {

    private static final String PROPERTY_FILE = "ccsds.properties";
    
    private static final String PROPERTY_PREFIX = "ccsds.";
    
    private static final String PACKET_BLOCK = PROPERTY_PREFIX + "tm.packet.";

    private static final String PACKET_HEADER_FORMAT_PROPERTY = PACKET_BLOCK + "headerFormat";

    private static final String ALLOW_IDLE_PACKET_SECONDARY_HEADER = PACKET_BLOCK + "idle.secondaryHeaderAllowed";
    
    /**
     * Test constructor
     */
    public CcsdsProperties() {
        this(new SseContextFlag());
    }

    /**
     * Constructor that loads the default properties file.
     * 
     * @param sseFlag
     *            The SSE context flag
     */
    public CcsdsProperties(final SseContextFlag sseFlag) {
        super(PROPERTY_FILE, sseFlag);
    }


    
    /**
     * Gets the configured packet header format definition.
     * 
     * @return IPacketFormatDefinition object
     */
    public IPacketFormatDefinition getPacketHeaderFormat() {
        final String temp = getProperty(PACKET_HEADER_FORMAT_PROPERTY, IPacketFormatDefinition.TypeName.CCSDS.toString());
        try {
            final IPacketFormatDefinition.TypeName type =  IPacketFormatDefinition.TypeName.valueOf(temp);
            return PacketFormatFactory.create(type);
        } catch (final IllegalArgumentException e) {
            return PacketFormatFactory.create(IPacketFormatDefinition.TypeName.CUSTOM_CLASS, temp);
        }
    }

    /**
     * Indicates whether IDLE packets are allowed to have a Secondary Header
     *
     * @return true if Packet Secondary Header allowed, false otherwise
     */
    public boolean isIdlePacketSecondaryHeaderAllowed() {
        return getBooleanProperty(ALLOW_IDLE_PACKET_SECONDARY_HEADER, false);
    }


    @Override
    public String getPropertyPrefix() {
        return PROPERTY_PREFIX;
    }
}
