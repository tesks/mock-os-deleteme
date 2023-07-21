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


/**
 * The PadCharacter class is...
 *
 */
public enum PadCharacterType
{
    /** Zero pad */
    ZERO('0'),

    /** Space pad */
    SPACE(' '),

    /** None (nul character) pad */
    NONE('\0');
    
    private char padChar;
    
    private PadCharacterType(char c) {
        this.padChar = c;
    }


    /**
     * Get pad as character.
     *
     * @return Pad character
     */    
    public char getPadCharacter() {
       return this.padChar;
    }
}
