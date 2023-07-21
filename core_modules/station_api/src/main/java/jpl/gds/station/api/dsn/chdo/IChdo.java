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
package jpl.gds.station.api.dsn.chdo;


/**
 * An interface to be implemented by Compressed Header Data Objects (CHDOs).
 * 
 *
 * @since R8
 */
public interface IChdo {

    /**
     * Gets the definition for this Chdo object.
     * @return ChdoDefinition object. Should never be null. 
     */
    public IChdoDefinition getDefinition();

    /**
     * Sets the definition for this Chdo object.
     *
     * @param definition The definition to set.
     */
    public void setDefinition(IChdoDefinition definition);

    /**
     * Gets the raw data bytes for this Chdo object, including header bytes.
     * @return byte array containing Chdo data
     */
    public byte[] getRawValue();

    /**
     * Sets the raw data bytes for this Chdo object.
     *
     * @param rawValue The data bytes to set, including header bytes.
     */
    public void setRawValue(byte[] rawValue);

    /**
     * Gets the raw data bytes for this Chdo object, without header bytes.
     * @return byte array containing headerless Chdo data
     */
    public byte[] getBytesWithoutChdoHeader();

    /**
     * Gets the data length of this Chdo, including header.
     * @return length of this Chdo
     */
    public int getLength();

    /**
     * Sets the data length of this Chdo, including header.
     *
     * @param length The length to set.
     */
    public void setLength(int length);

    /**
     * Return length of header (same as the offset of the Chdo within the raw
     * buffer).
     *
     * @return header length;
     */
    public int getHeaderLength();

}