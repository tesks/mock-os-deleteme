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
package jpl.gds.dictionary.api.channel;

/**
 * BitRange represents one bit range (start bit and length) used by an
 * IBitUnpackDerivaiton implementation. 
 * <br>
 * <br><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br><br>
 * 
 * IBitUnpackChannelDerivation defines methods needed to interact with Bit
 * Unpack Channel Derivation objects as required by the IChannelDictionary
 * interface. BitRange is a utility class used in that interface.
 *
 *
 * @see IBitUnpackChannelDerivation
 */
public class BitRange implements Comparable<BitRange> {
    private final int startBit;
    private final int length;

    /**
     * Creates an instance of BitRange. Note that
     * bits are numbered with LSB=0 on the right.
     *
     * @param start
     *            the start bit
     * @param len
     *            the number of bits
     */
    public BitRange(final int start, final int len) {
        super();

        startBit = start;
        length = len;
    }

    /**
     * Gets the start bit number. Note that
     * bits are numbered with LSB=0 on the right.
     * MSB is on the left.
     * 
     * @return the starting bit number.
     */
    public int getStartBit() {
        return startBit;
    }

    /**
     * Gets the bit field length.
     * 
     * @return the length.
     */
    public int getLength() {
        return length;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object other) {
        if (other == this) {
            return true;
        }

        if (!(other instanceof BitRange)) {
            return false;
        }

        final BitRange o = (BitRange) other;

        return ((startBit == o.startBit) && (length == o.length));
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return (startBit + length);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(final BitRange other) {
        if (other == null) {
            throw new IllegalArgumentException();
        }

        if (startBit < other.startBit) {
            return -1;
        }

        if (startBit > other.startBit) {
            return 1;
        }

        if (length < other.length) {
            return -1;
        }

        if (length > other.length) {
            return 1;
        }

        return 0;
    }
}
