/*
 * Copyright 2006-2019. California Institute of Technology.
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
package jpl.gds.dictionary.api.command;

import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.util.BinOctHexUtility;

/**
 * This class represents a signed command argument enumerated/lookup value. An
 * enumerated value represents a single potential value for an enumerated
 * command argument (a.k.a. a LookArgument). Enumerated arguments contain a list
 * of possible Lookup values and the value of an enumerated argument is a chosen
 * value from that enumeration.
 * <br>
 * <br><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br><br>
 * 
 * Unlike most enumerations in AMPCS, which use a numeric value and a symbolic
 * value for each entry in the table, command enumerations support three string
 * values: 1) dictionary value (what AMPCS normally thinks of as the symbolic
 * values, and generally uses for displays and other human-readable
 * representations, 2) a FSW value, which is also a symbolic name, and is used
 * in the flight software, and 3) a bit value, which is the numeric value.
 * <p>
 * This class is used to represent enumerated values whose bit value is signed.
 * For unsigned values, use UnsignedEnumeratedValue.
 * 
 * 
 *
 */
public class SignedEnumeratedValue implements ICommandEnumerationValue
{
    /**
     * The dictionary value of this lookup
     */
    protected String dictionaryValue;

    /**
     * The FSW value of this lookup
     */
    protected String fswValue;

    /**
     * The bit value of this lookup
     */
    protected String bitValue;

    /**
     * 
     * Creates an instance of SignedEnumeratedValue.
     */
    public SignedEnumeratedValue()
    {
        dictionaryValue = null;
        fswValue = null;
        bitValue = null;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.api.command.ICommandEnumerationValue#getBitValue()
     */
    @Override
    public String getBitValue()
    {
        return (bitValue);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.api.command.ICommandEnumerationValue#setBitValue(java.lang.String)
     */
    @Override
    public void setBitValue(final String bitValue) {

        if (bitValue == null) {
            throw new IllegalArgumentException("Null input bit value");
        }

        if(BinOctHexUtility.hasBinaryPrefix(bitValue) ||
                BinOctHexUtility.hasHexPrefix(bitValue))
        {
            this.bitValue = (Integer.valueOf(GDR.parse_int(bitValue))).toString();
            return;
        }

        this.bitValue = bitValue;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.api.command.ICommandEnumerationValue#getDictionaryValue()
     */
    @Override
    public String getDictionaryValue() {

        return (dictionaryValue);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.api.command.ICommandEnumerationValue#setDictionaryValue(java.lang.String)
     */
    @Override
    public void setDictionaryValue(final String dictionaryValue) {

        if (dictionaryValue == null) {
            throw new IllegalArgumentException("Null input dictionary value");
        }

        this.dictionaryValue = dictionaryValue;
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.api.command.ICommandEnumerationValue#getFswValue()
     */
    @Override
    public String getFswValue() {
        return (fswValue);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.dictionary.api.command.ICommandEnumerationValue#setFswValue(java.lang.String)
     */
    @Override
    public void setFswValue(final String fswValue) {

        if (fswValue == null) {
            throw new IllegalArgumentException("Null input FSW value");
        }

        this.fswValue = fswValue;
    }

    /**
     * {@inheritDoc}
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return(dictionaryValue);
    }


    /**
     * Make a copy of this enumerated value
     * 
     * @return A deep copy of this enumerated value
     */
    public SignedEnumeratedValue copy()
    {
        SignedEnumeratedValue val = new SignedEnumeratedValue();
        setSharedValues(val);
        return(val);
    }

    /**
     * Used by the copy operation to copy values from this enumerated value
     * onto the input enumerated value
     * 
     * @param val The enumerated value whose members should be set to the
     * same values as this class
     */
    protected void setSharedValues(final SignedEnumeratedValue val)
    {
        val.bitValue = bitValue;
        val.dictionaryValue = dictionaryValue;
        val.fswValue = fswValue;
    }

    /**
     * {@inheritDoc}
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj)
    {
        if(!(obj instanceof SignedEnumeratedValue))
        {
            return(false);
        }

        return(getDictionaryValue().equals(((SignedEnumeratedValue)obj).getDictionaryValue()));
    }

    /**
     * {@inheritDoc}
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode()
    {
        return(this.dictionaryValue.hashCode()); 
    }

    /**
     * {@inheritDoc}
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     * 
     */
    @Override
    public int compareTo(ICommandEnumerationValue o)
    {
        return(getDictionaryValue().compareTo(o.getDictionaryValue()));
    }
}