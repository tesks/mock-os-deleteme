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
package jpl.gds.shared.types;

import java.io.Serializable;
import java.util.List;


/**
 * Holder for array plus offset and length: a slice.
 *
 */
public class ByteArraySlice implements Serializable
{
    private static final long serialVersionUID = 0L;

    /** Byte array */
    public byte[] array;

    /** Byte offset */
    public int    offset;

    /** Byte length */
    public int    length;


    /**
     * Constructor.
     */
    public ByteArraySlice() { }


    /**
     * Constructor.
     *
     * @param buf Byte buffer
     * @param off Offset
     * @param len Length
     */
    public ByteArraySlice(byte[] buf, int off, int len) {
        array = buf;
        offset = off;
        length = len;
    }

    /**
     *  This constructor takes a list of ByteArraySlices
     *  merges them into one ByteArraySlice.  "slices" is an
     *  ArrayList.
     *
     * @param sliceList List of slices
     */
    public ByteArraySlice(List<ByteArraySlice> sliceList) { 
        if (sliceList == null) {
            return;
        }
          
        for (int i=0; i < sliceList.size(); i++)
            length += sliceList.get(i).length;

        array = new byte[length]; 
        offset = 0;

        // merge the data from each slice into one array

        int merge_pos = 0;
        for (int i=0; i < sliceList.size(); i++) {
            ByteArraySlice slice = sliceList.get(i);
            System.arraycopy(slice.array, slice.offset,
                             array, merge_pos, slice.length);
            merge_pos += slice.length;
        }
    }

    /**
     * Set new values.
     *
     * @param buf Byte buffer
     * @param off Offset
     * @param len Length
     */     
    public void update(byte[] buf, int off, int len) {
        array = buf;
        offset = off;
        length = len;
    }


    /**
     * Reset.
     */     
    public void reset() {
        array = null;
        offset = 0;
        length = 0;
    }

    /**
      * Create a duplicate copy of this ByteArraySlice
      *
      * @return Copy
      */
    public ByteArraySlice copy() {
        ByteArraySlice bas = new ByteArraySlice();
 
        byte[] dup = new byte[array.length]; 
        System.arraycopy(array, 0, dup, 0, array.length);

        bas.array = dup;
        bas.offset = offset;
        bas.length = length;
     
        return bas;  
    }
}
