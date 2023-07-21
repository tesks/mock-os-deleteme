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
package ammos.datagen.config;

/**
 * This interface is to be implemented by all Mission Configuration classes. It
 * exists to provide common configuration methods that apply to all Mission
 * Configuration classes, and to provide constants for all common mission
 * configuration property names.
 * 
 */
public interface IMissionConfiguration extends IXmlConfiguration {
    /**
     * Configuration property name for the SCLK Coarse Length, in bits (integer)
     */
    public static final String SCLK_COARSE_LEN = "SclkCoarseLengthBits";
    /**
     * Configuration property name for the SCLK Fine Length, in bits (integer)
     */
    public static final String SCLK_FINE_LEN = "SclkFineLengthBits";
    /**
     * Configuration property name for use of fractional vs non-fractional SCLK
     * format (boolean)
     */
    public static final String USE_FRACTIONAL_SCLK = "UseFractionalSclk";
    /**
     * Configuration property name SCLK coarse/fine separator (String)
     */
    public static final String SCLK_SEPARATOR_CHAR = "SclkSeparatorChar";
    /**
     * Configuration property name for the maximum packet length, in bytes
     * (integer)
     */
    public static final String PACKET_MAX_LEN = "MaxPacketLength";
    /**
     * Configuration property name for the fill packet APID (integer)
     */
    public static final String FILL_PACKET_APID = "FillPacketApid";
    /* MPCS-7663 - 9/10/15. Added packet header class property name */
    /**
     * Configuration property name for packet header class (string).
     */
    public static final String PACKET_HEADER_CLASS = "PacketHeaderClass";
}
