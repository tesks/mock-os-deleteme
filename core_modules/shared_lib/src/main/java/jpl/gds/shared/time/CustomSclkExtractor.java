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

import java.util.Map;

import jpl.gds.shared.types.BitBuffer;

/**
 * An ISclkExtractor implementation based upon generic SCLK.
 * 
 *
 */
public class CustomSclkExtractor implements ISclkExtractor {
    
    public static final String FINE_LENGTH_PARAM = "fineLength";
    public static final String COARSE_LENGTH_PARAM = "coarseLength";
    public static final String FINE_LIMIT_PARAM = "fineLimit";
    
    private ISclkExtractor sclkExtractor;

    @Override
    public ISclk getValueFromBytes(final byte[] buff, final int startingOffset) {
    	if (sclkExtractor != null) {
    		return sclkExtractor.getValueFromBytes(buff, startingOffset);
    	} else {
    		throw new IllegalStateException("Attempting to extract SCLK in custom SCLK extractor, but extraction parameters are not set");
    	}
    }

    @Override
    public void setStaticArgs(final Map<String, Object> params) {
        if (params == null || params.isEmpty()) {
            throw new IllegalStateException("Attempting to extract SCLK in CustomSclkExtractor, but extraction parameters are not set");
        }
        final int coarseLength = (Integer)params.get(COARSE_LENGTH_PARAM);
        final int fineLength = (Integer)params.get(FINE_LENGTH_PARAM);
        final int fineLimit = (Integer)params.get(FINE_LIMIT_PARAM);
        sclkExtractor = new CoarseFineExtractor(new CoarseFineEncoding(coarseLength, fineLength, fineLimit));
    
    }

	@Override
	public boolean hasEnoughBytes(final byte[] buff, final int startingOffset) {
    	if (sclkExtractor != null) {
    		return sclkExtractor.hasEnoughBytes(buff, startingOffset);
    	} else {
    		throw new IllegalStateException("Custom SCLK extractor cannot determine if SCLK is present because extraction parameters are not set");
    	}
	}

	@Override
    public ISclk getValueFromBits(final BitBuffer buffer, final Map<String, Object> args) {
    	if (sclkExtractor != null) {
    		return sclkExtractor.getValueFromBits(buffer, args);
    	} else {
    		throw new IllegalStateException("Attempting to extract SCLK in custom SCLK extractor, but extraction parameters are not set");
    	}
	}

}
