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
 * An interface to be implemented by classes that represent the definition
 * of a CHDO SFDU data field.
 * 
 *
 * @since R8
 */
public interface IChdoFieldDefinition {

    /**
     * Retrieves the length in bits of the CHDO field.
     * 
     * @return bit length
     */
    public int getBitLength();

    /**
     * Retrieves the length in bytes of the CHDO field.
     * 
     * @return byte length
     */
    public int getByteLength();

    /**
     * Returns true if field has a bit-length of 1. This indicates a flag,
     * and a flag is probably an overlap over a field of flags. We want to
     * process flags last.
     * 
     * @return True if flag field
     */
    public boolean getFlag();

    /**
     * Sets the length in bits of the CHDO field.
     *
     * @param bitLength The bitLength to set.
     */
    public void setBitLength(int bitLength);

    /**
     * Retrieves the offset in bits of the CHDO field. This offset
     * is relative to the byte offset.
     * 
     * @return bit offset
     */
    public int getBitOffset();

    /**
     * Sets the offset in bits of the CHDO field. This offset
     * is relative to the byte offset.
     *
     * @param bitOffset The offset to set.
     */
    public void setBitOffset(int bitOffset);

    /**
     * Retrieves the offset in bytes of the CHDO field.
     * 
     * @return byte offset
     */
    public int getByteOffset();

    /**
     * Sets the the offset in bytes of the CHDO field.
     *
     * @param byteOffset The byteOffset to set.
     */
    public void setByteOffset(int byteOffset);

    /**
     * Retrieves the field identifier (name).
     * 
     * @return the field ID
     */
    public String getFieldId();

    /**
     * Sets the field identifier (name).
     *
     * @param fieldId The ID to set.
     */
    public void setFieldId(String fieldId);

    /**
     * Retrieves the format of this CHDO field.
     * 
     * @return the format
     */
    public ChdoFieldFormatEnum getFieldFormat();

    /**
     * Sets the format of this CHDO field.
     *
     * @param fieldFormat The format to set.
     */
    public void setFieldFormat(ChdoFieldFormatEnum fieldFormat);

    /**
     * Get minimum value, or null if none set.
     *
     * @return Minimum value or null
     */
    public Long getMinValue();

    /**
     * Set minimum value, or null if there is none.
     *
     * @param min Minimum value or null
     */
    public void setMinValue(Long min);

    /**
     * Get maximum value, or null if none set.
     *
     * @return Maximum value or null
     */
    public Long getMaxValue();

    /**
     * Set maximum value, or null if there is none.
     *
     * @param max Maximum value or null
     */
    public void setMaxValue(Long max);

    /**
     * Get default value, or null if none set.
     *
     * @return Default value or null
     */
    public Long getDefaultValue();

    /**
     * Set default value, or null if there is none.
     *
     * @param value Value or null
     */
    public void setDefaultValue(Long value);

    /**
     * Get fixed value, or null if none set.
     *
     * @return Fixed value or null
     */
    public Long getFixedValue();

    /**
     * Set fixed value, or null if there is none.
     *
     * @param value Value or null
     */
    public void setFixedValue(Long value);

}