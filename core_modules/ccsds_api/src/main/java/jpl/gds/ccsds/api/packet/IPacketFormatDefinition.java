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
package jpl.gds.ccsds.api.packet;

import jpl.gds.shared.annotation.CustomerAccessible;

/**
 * An interface to be implemented by frame format definition objects. Defines 
 * a frame format type enumeration and allows access to class names used to 
 * parse the frame header and compute the frame CRC.
 * <br>
 * <b>THIS IS A MULTI-MISSION CORE ADAPTATION INTERFACE</b><br>
 * <br>
 * <b>This is a controlled interface. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b><br>
 * <br>
 * 
 * 
 *
 */
@CustomerAccessible(immutable = true)
public interface IPacketFormatDefinition {

    /**
     * An enumeration of supported telemetry packet formats.
     * 
     */
    public enum TypeName {
        
        /** Packet is a vanilla CCSDS packet */
        CCSDS("jpl.gds.ccsds.impl.packet.CcsdsPacketHeader"),
        
        /** Frame is a CCSDS-like frame, but requires custom classes for processing. */
        CUSTOM_CLASS(null),
        
        /** Frame type is unknown */
        UNKNOWN(null);
    
        String defaultPacketHeaderClass;
    
        /**
         * Constructs a TypeName enum value.
         * 
         * @param headerClass
         *            the full class name of the packet header (ISpacePacketHeader)
         *            class.
         */
        private TypeName(final String headerClass) {
            defaultPacketHeaderClass = headerClass;
        }
    
        /**
         * Gets the default frame header parsing class for this frame format type.
         * 
         * @return full class name
         */
        public String getDefaultPacketHeaderClass() {
            return defaultPacketHeaderClass;
        }
    
    }

    /**
     * Gets the full package name of the frame header processing class.
     * 
     * @return class name
     */
    public String getPacketHeaderClass();

    /**
     * Gets the frame format TypeName.
     * 
     * @return TypeName
     */
    public IPacketFormatDefinition.TypeName getType();

}