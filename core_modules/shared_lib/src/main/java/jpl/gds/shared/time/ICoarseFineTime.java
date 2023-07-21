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
package jpl.gds.shared.time;

/**
 * Interface ICoarseFineTime
 */
public interface ICoarseFineTime extends Comparable<Object> {

    /**
     * Get coarse.
     *
     * @return Returns the secs.
     */
    long getCoarse();

    /**
     * Get fine.
     *
     * @return Returns the fine.
     */
    long getFine();

    /**
     * Get the upper limit of the secs value
     *
     * @return The maximum allowable value of the secs plus one
     */
    long getCoarseUpperLimit();

    /**
     * Get the upper limit of the fine value depending on the short/long format
     *
     * @return The maximum allowable value of the fine plus one
     */
    long getFineUpperLimit();

    /**
     * Get the floating point time of this SC CLOCK
     *
     * @return The floating point representation of this SC CLOCK
     */
    double getFloatingPointTime();

    /**
     * Increment this SC CLOCK time by the given amount and return
     * a new instance representing the resulting time
     *
     * @param sec The amount of SC CLOCK secs to increment
     * @param fin The amount of SC CLOCK fine to increment
     * 
     * @return a new incremented ISclk object
     */
    ISclk increment(long sec, long fin);

    /**
     * Decrement this SC CLOCK time by the given amount and return a new 
     * instance representing the resulting time.
     *
     * @param sec The amount of SC CLOCK secs to decrement
     * @param fin The amount of SC CLOCK fine to decrement
     * 
     * @return a new incremented ISclk object
     */
    ISclk decrement(long sec, long fin);

    /**
     * Get coarse as byte array.
     *
     * @return Byte array
     */
    byte[] getCoarseBytes();

    /**
     * Get fine as byte array.
     *
     * @return Byte array
     */
    byte[] getFineBytes();

    /**
     * Gets the byte length of the current SCLK
     * @return  the total byte length
     */
    int getByteLength();

    /**
     * Write the value of this SCLK into a byte array
     *
     * @param buff The byte array to write the value to
     * @param startingOffset The offset into the byte array where the value will start
     *
     * @return The number of bytes written
     */
    int toBytes(byte[] buff, int startingOffset);

    /**
     * Get the byte array (GDR) representation of this SCLK
     *
     * @return The byte array representation of this SCLK
     */
    byte[] getBytes();

    /**
     * This method packs the 6-byte representation of a SCLK
     * into a Java long.
     *
     * The GDR representation of a 6-byte SCLK in an 8-byte long
     * will look like this (assuming bit 63 is the MSB):
     *
     * bits 63-56 = 0x00
     * bits 55-48 = 0x00
     * bits 47-40 = sclk secs bits 31-24
     * bits 39-32 = sclk secs bits 23-16
     * bits 31-24 = sclk secs bits 15-8
     * bits 23-16 = sclk secs bits 7-0
     * bits 15-8 = sclk fine bits 15-8
     * bits 7-0 = sclk fine bits 7-0
     *
     * @return The GDR long representation of this SCLK
     */
    long getBinaryGdrLong();

    /**
     * Get dummy state.
     *
     * @return True if value is a dummy
     */
    boolean isDummy();

    /**
     * Gets the CoarseFineEncoding object in use by this object.
     * 
     * @return CoarseFineEncoding
     */
    CoarseFineEncoding getEncoding();
    

    /**
     * Get an exact representation of the clock. The value returned is the number of
     * fine ticks; this means that the fine tick modulus must be known to convert
     * this back into a ticks / fine ticks representation.
     * 
     * @return the exact representation as a long
     */
    public long getExact();

}