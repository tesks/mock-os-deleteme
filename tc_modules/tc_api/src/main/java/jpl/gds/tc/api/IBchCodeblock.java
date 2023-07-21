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
package jpl.gds.tc.api;

/**
 * An interface to be implemented by classes that represent BCH (Bose–Chaudhuri–Hocquenghem) 
 * code blocks used in commanding.
 * 
 *
 * @since R8
 */
public interface IBchCodeblock {

    /**
     * Get the representation of this codeblock as a byte array. Both the EDAC
     * and data must be set already for this method to work.
     * 
     * @return A byte array representing this codeblock (data, edac & fill)
     * 
     */
    byte[] getBytes();

    /**
     * Set the contents of this BchCodeblock from a given byte array. This
     * method will read a portion out of the input byte array and then reverse
     * it into the data, edac and fill components.
     * 
     * @param codeblock
     *            The byte array to read the codeblock from
     * @param offset
     *            The starting offset in "codeblock" where data will start being
     *            read
     * 
     * @return The number of bytes read from "codeblock"
     */
    int parseFromBytes(byte[] codeblock, int offset);

    /**
     * Accessor for the EDAC of this codeblock (the calculated error correction
     * code + fill).
     * 
     * @return A copy of the EDAC member of this codeblock (contains EDAC +
     *         fill) as a byte array
     */
    byte[] getEdac();

    /**
     * Mutator for the EDAC of this codeblock (the calculated error correction
     * code + fill)
     * 
     * @param edac
     *            The EDAC + fill value to set on the codeblock
     */
    void setEdac(byte[] edac);

    /**
     * Accessor for the data portion of the codeblock.
     * 
     * @return Returns a copy of the data portion of the codeblock as a byte
     *         array.
     */
    byte[] getData();

    /**
     * Mutator for the data portion of the codeblock. If the input data is not
     * large enough to fit into a codeblock's full byte size, it will be padded
     * out to the full length of a codeblock using the configured CLTU fill byte
     * (usually 0x55).
     *
     * @param inData
     *            The byte array that will be used to initialize the data
     *            portion of this codeblock.
     */
    void setData(byte[] inData);

    IBchCodeblock copy();

}