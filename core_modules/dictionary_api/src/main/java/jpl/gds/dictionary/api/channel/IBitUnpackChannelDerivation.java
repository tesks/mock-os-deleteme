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

import java.util.List;

/**
 * The IBitUnpackChannelDerivation interface is to be implemented by all channel
 * derivation definition classes that create a child channel by extracting bits
 * from a parent channel.
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
 * interface. It is primarily used by channel file parser implementations in
 * conjunction with the ChannelDerivationFactory, which is used to create actual
 * Channel Derivation objects in the parsers. IChannelDictionary objects should
 * interact with Channel Derivation objects only through the Factory and the
 * IChannelDerivation interface and its extensions. Interaction with the actual
 * Channel Derivation implementation classes in an IChannelDictionary
 * implementation is contrary to multi-mission development standards.
 * 
 *
 * @see IChannelDictionary
 * @see ChannelDerivationFactory
 */
public interface IBitUnpackChannelDerivation extends IChannelDerivation {

    /**
     * Adds a bit field range to this derivation, inserting the bit range into
     * the list in proper order based upon start bit. Note that bits are
     * numbered with LSB=0 on the right.
     *
     * @param start
     *            the start bit of this range
     * @param len
     *            the number of bits in this range
     */
    public void addBitRange(final int start, final int len);

    /**
     * Gets the list of bit ranges that make up this derivation, ordered by
     * start bit.
     *
     * @return a list of BitRange objects
     */
    public List<BitRange> getBitRanges();

    /**
     * Gets the parent channel for this derivation.
     *
     * @return the parent channel ID
     */
    public String getParent();

    /**
     * Gets the child channel for this derivation.
     *
     * @return the child channel ID
     */
    public String getChild();

}