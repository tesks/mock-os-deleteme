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
package jpl.gds.shared.util;

/**
 * Maintains offset to byte arrays.
 *
 */
public class ByteOffset
{
    private int offset = 0;


    /**
     * Constructor.
     */
    public ByteOffset()
    {
    }

    /**
     * Constructor.
     *
     * @param start Start offset
     */
    public ByteOffset(int start) {
        offset = start;
    }

    /**
     * Increment offset.
     *
     * @param n Increment
     *
     * @return Previous offset
     */
    public int inc(int n) {
        int before = offset;
        offset += n;
        return before;
    }

    /**
     * Get offset.
     *
     * @return Offset
     */
    public int getOffset() {
        return offset;
    }
}
