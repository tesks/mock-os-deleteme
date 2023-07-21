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
package jpl.gds.evr.api.service.extractor;

/**
 * An interface to be implemented by data objects that represent raw EVR data
 * fields, i.e., EVR argument fields from a packet.
 * 
 * @since R8
 */
public interface IRawEvrData {

    /**
     * Returns the type of the data element as derived from the EVR definition
     * 
     * @return the Java class which is the expected type
     */
    public Class<?> getExpectedType();

    /**
     * Writes information about the data to the output string.
     * 
     * @return dump data information
     */
    public String getDumpDataInformation();

    /**
     * Replaces the original data with a string mnemonic this method is used to
     * replace opcodes with mnemonics for various reasons--ITAR, etc.
     * 
     * @param inputMnemonic
     *            mnemonic data to replace values with
     */
    public void replaceData(String inputMnemonic);

    /**
     * Creates an Object out of the raw data object based upon the the input
     * format string.
     * 
     * @param inputFormat
     *            the format which governs how the bits in the the returned
     *            object are to be filled. For example, if the input format is
     *            %d, a Long object is desired, and the original data is 4 bytes
     *            in length with the most significant bit( the sign bit ) set,
     *            the first four bytes of the Long object would be set to hex
     *            FFFF.
     * @return the converted object; it is null if the formatting failed.
     * 
     */
    public Object formatData(String inputFormat);

    /**
     * Returns the size of the original data in bytes.
     * 
     * @return data size
     */
    public int getSize();

    /**
     * Returns the original data array.
     * 
     * @return original data
     */
    public byte[] getByteArray();

    /**
     * This method sets the length of the EVR data element to 0, sets type to
     * String, and sets its state to "valid".
     */
    public void setDataToEmptyString();

}