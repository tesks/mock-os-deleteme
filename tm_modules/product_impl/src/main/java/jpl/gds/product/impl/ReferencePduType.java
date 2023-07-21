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
package jpl.gds.product.impl;

import java.util.HashMap;
import java.util.Map;

import jpl.gds.product.api.IPduType;

/**
 * PduType is an enumeration of product builder PDU types.
 *
 */
public enum ReferencePduType implements IPduType {
    /*
     * Uncompressed Versions.
     */
    /** Metadata PDU */
    METADATA((byte)0), 
    /** File data PDU */
    DATA((byte)1), 
    /** End PDU */
    END((byte)2), 
    /** Combination data/end PDU */
    DATA_END((byte)3);
    
    @SuppressWarnings("serial")
    private static final Map<Byte, IPduType> pduTypeMap = new HashMap<Byte, IPduType>() {{
        put(METADATA.value, METADATA);
        put(DATA.value, DATA);
        put(END.value, END);
        put(DATA_END.value, DATA_END);
    }};
    
    /**
     * Static factory for PDU type
     * @param pduType A byte indicating PDU type
     * @return an IPduType enum
     */
    public static IPduType valueOf(final byte pduType) {
        final IPduType retVal = pduTypeMap.get(pduType);
        if (null == retVal) {
            throw new IllegalArgumentException("Illegal value specified for IPduType: 0x" + Integer.toString((pduType & 0x00ff), 16).toUpperCase());
        }
        return retVal;
    }
    
    private final byte      value;
    
    private final byte      pduType;
    

    private ReferencePduType(final byte value) {
        this.value = value;
        this.pduType = (byte)(value & 0x007f);
    }
    
    /**
     * Gets the PDU type
     * @return the PDU type (integer)
     */
    public int getPduType() {
        return pduType;
    }

    /**
     * {@inheritDoc} 
     * @see jpl.gds.product.api.IPduType#isData()
     */
    @Override
    public boolean isData() {
        return DATA.pduType == this.pduType;
    }
    
    /**
     * {@inheritDoc} 
     * @see jpl.gds.product.api.IPduType#isMetadata()
     */
    @Override
    public boolean isMetadata() {
        return METADATA.pduType == this.pduType;
    }
    
    /**
     * {@inheritDoc} 
     * @see jpl.gds.product.api.IPduType#isEnd()
     */
    @Override
    public boolean isEnd() {
        return END.pduType == this.pduType;
    }
    
    /**
     * {@inheritDoc} 
     * @see jpl.gds.product.api.IPduType#isEndOfData()
     */
    @Override
    public boolean isEndOfData() {
        return DATA_END.pduType == this.pduType;
    }
}
