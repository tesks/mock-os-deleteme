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
package jpl.gds.dictionary.impl.channel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import jpl.gds.dictionary.api.channel.BitRange;
import jpl.gds.dictionary.api.channel.DerivationType;
import jpl.gds.dictionary.api.channel.IBitUnpackChannelDerivation;

/**
 * BitUnpackDerivationDefinition is a specific type of channel derivation that
 * creates a child channel from a single parent channel by extracting bit fields
 * from the parent value. It is used to store the definition of a bit unpack
 * operation as loaded from a dictionary. It does not do the bit unpack
 * operation itself. The actual work is done through the BitUnpackDerivation
 * class at runtime.
 * 
 */
public class BitUnpackDerivationDefinition implements IBitUnpackChannelDerivation
{
    private final List<BitRange> _bitSequences     = new ArrayList<BitRange>();
    private final List<BitRange> _safeBitSequences =
            Collections.unmodifiableList(_bitSequences);

    private String _parent = null;
    private String _child  = null;

    /**
     * Creates an instance of BitUnpackDerivation.
     * 
     */
    BitUnpackDerivationDefinition()
    {
        super();
    }

    /**
     * Gets the parent channel for this derivation
     *
     * @return the parent channel ID
     */
    public String getParent()
    {
        return _parent;
    }


    public void addParent(final String parent)
    {
        _parent = parent;
    }
    
    /**
     * Gets the child channel for this derivation
     *
     * @return the child channel ID
     */
    public String getChild()
    {
        return _child;
    }


    public void addChild(final String child) {
        _child = child;
    }


    public String getId() {
        String result=this.getChild();
        if (result==null) {
           throw new IllegalStateException("ID/child cannot be null.");
        }
        return result;
    }

    /**
     * Adds a bit field range to this derivation, inserting the bit range into
     * the list in proper order based upon start bit.
     *
     * @param start the start bit of this range
     * @param len the number of bits in this range
     */
    public void addBitRange(final int start,
            final int len)
    {
        _bitSequences.add(new BitRange(start, len));

        Collections.sort(_bitSequences);
    }

   /**
    * Gets the derivation type when all that is known is that the object is of
    * a class that implements IChannelDerivation.  
    *
    * @return BIT_UNPACK in all cases
    *
    */

    public DerivationType getDerivationType() {

        return DerivationType.BIT_UNPACK;
    
    }

    /**
     * Gets the list of bit ranges that make up this derivation, ordered by
     * start bit.
     *
     * @return a list of BitUnpackDerivation.BitRange objects
     */
    public List<BitRange> getBitRanges()
    {
        return _safeBitSequences;
    }


  
    /**
     * {@inheritDoc}
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public final boolean equals(final Object o)
    {
        return (o == this);
    }

    /**
     * {@inheritDoc}
     * @see java.lang.Object#hashCode()
     */
    @Override
    public final int hashCode()
    {
        return super.hashCode();
    }

    /**
     * {@inheritDoc}
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        String firstPart = "Parent = " + _parent +
                ", Child = " + _child + "\n" +
                "Bit Ranges = ";
        StringBuilder secondPart = new StringBuilder();
        for (BitRange range: _bitSequences) {
            if (secondPart.length() != 0) {
                secondPart.append(", ");
            } 
            secondPart.append(range.getStartBit() + "-" + (range.getLength() + range.getStartBit() - 1));	
        }
        secondPart.append(" (LSB = Far Right Bit = 0)");
        return firstPart + secondPart.toString();
    }


    @Override
    public List<String> getParents() {

        List<String> result = new ArrayList<String>(1);
        if (this._parent != null) {
            result.add(this._parent);
        }
        return result;
    }


    @Override
    public List<String> getChildren() {

        List<String> result = new ArrayList<String>(1);
        if (this._child != null) {
            result.add(this._child);
        }
        return result;
    }
}
