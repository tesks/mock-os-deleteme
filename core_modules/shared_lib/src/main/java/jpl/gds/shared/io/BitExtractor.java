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
package jpl.gds.shared.io;

import jpl.gds.shared.gdr.GDR;

/**
 * Utility wrapper for GDR.cbits_to_u16 that keeps track of its place in the input stream.
 * 
 * The initial simple version assumes the buffer contains word-aligned (16-bit words) data.
 *
 */
public class BitExtractor {
    private static final int WORD_BYTES = 2;
    private static final int BYTE_BITS = 8;
    private final byte[] buff;
    private int byte_offset=0;
    private int bit_offset=0;
    
    /**
     * Constructor.
     * 
     * @param buff byte array to extract from
     * @param start starting byte offset in the array
     */
    public BitExtractor(byte[] buff, int start) {
        this.buff=buff;
        this.byte_offset=start;
    }
    
    /**
     * Extract size bits from current word offset in input buffer into an int.
     * The internal bit offset is incremented by size so that next call will start
     * where this extraction leaves off. We prefer to increment byte offset in units
     * of 2 because the GDR.cbits_to_u16 will always try to read the indexed byte 
     * plus one following.
     * 
     * size must be no more than 16 bits.
     * @param size number of bits to extract
     * @return extracted field of given size as int
     */
    public int getInt(int size) {
        assert(size <= BYTE_BITS*WORD_BYTES);
        while (bit_offset > 7) {
            bit_offset -= BYTE_BITS;
            byte_offset++;
        }
        
        int rslt=GDR.cbits_to_u16(buff, byte_offset, bit_offset, size);
        bit_offset+=size;
        
        return rslt;
    }
    
    /**
     * Extract size bits from current byte offset. This method will always read one byte,
     * so if the bit pattern happens to span two bytes you'll get an incomplete value.
     * 
     * @param size of the value to get, in bits. Size must be no more than 8 bits.
     * @return byte value extracted
     */
    public int getByte(int size) {
        assert(size <= BYTE_BITS);
        while (bit_offset > 7) {
            bit_offset -= BYTE_BITS;
            byte_offset++;
        }
        int rslt=GDR.cbits_to_u8(buff, byte_offset, bit_offset, size);
        bit_offset+=size;
        
        return rslt;
    }
    
    /**
     * This method combines the benefits of both the getByte and getInt methods. If size is more than
     * one byte's worth, or if the value will span bytes, this method will call the appropriate method
     * to extract one or two bytes from the input stream as needed.
     * This method should be able to read sizes up to 20 bits, which can span up to four bytes (two words).
     * 
     * @param size the number of bits to extract
     * @return the next size bits from the stream
     */
    public int getBits(int size) {
        assert(size <= BYTE_BITS*WORD_BYTES);
        if (size > 16) {
            throw new IllegalArgumentException("size > 16");
        }
        while (bit_offset > 7) {
            bit_offset -= BYTE_BITS;
            byte_offset++;
        }
        int rslt=0;
        if (bit_offset+size > 8) {
            if (bit_offset+size > 16) {
                throw new IllegalArgumentException(String.format("size + bit offset > 16: %d",size+bit_offset));
            } else {
                rslt= GDR.cbits_to_u16(buff, byte_offset, bit_offset, size);
            }
        } else {
            rslt= GDR.cbits_to_u8(buff, byte_offset, bit_offset, size);
        }
        bit_offset+=size;
        return rslt;
    }
    
    /**
     * Version of getBits to get values longer than 16 bits, or that wrap across more than two words.
     * If this works I'll have getBits call this, or just integrate into getBits.
     * 
     * @param size bit size of the value to extract
     * @return extracted integer value
     */
    public int getLongbits(int size) {
        assert (size <= 24);
        if (size > 24) {
            throw new IllegalArgumentException("size > 24");
        }
        while (bit_offset > 7) {
            bit_offset -= BYTE_BITS;
            byte_offset++;
        }
        //int extent=bit_offset+size;
        int msbsize = 8-bit_offset;
        int lsbsize = size - msbsize;
        // read MSB part
        int msb = getBits(msbsize);
        int lsb = getBits(lsbsize);
        return msb << lsbsize | lsb;
    }
    
    /**
     * Move offset to next 16-bit word
     */
    public void nextWord() {
        byte_offset+=WORD_BYTES;
        bit_offset=0;
    }
    
    /**
     * Move to next byte
     */
    public void nextByte() {
        byte_offset++;
        bit_offset=0;
    }
    
    /**
     * Skips over the specified number of bits.
     * 
     * @param size number of bits to skip
     */
    public void skipBits(int size) {
        bit_offset += size;
    }
}