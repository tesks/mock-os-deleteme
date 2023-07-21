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
package jpl.gds.product.api;

/**
 * ProductStatusType is an enumeration of ground status values for data products.
 *
 *
 */
public enum ProductStatusType {
	/**
	 * Status of the product is uninitialized or unknown.
	 */
    UNKNOWN,
    /**
     * Product has been partially received or lacks metadata.
     */
    PARTIAL,
    /**
     * Product has been completely received and has all metadata.
     */
    COMPLETE,
    /**
     * Product has been completely received and has all metadata.
     * No checksum was available from the flight data.
     */
    COMPLETE_NO_CHECKSUM,
    /**
     * Product has been completely received and has all metadata.
     * Checksum matches that from the flight data.
     */
    COMPLETE_CHECKSUM_PASS,
    /**
     * Product has been completely received and has all metadata.
     * Checksum does not match that in the flight data.
     * 
     * @deprecated left here for compatibility with old MSL products
     */
    @Deprecated
    COMPLETE_CHECKSUM_FAIL,
    
    /**
     * Product has been completely received and has all metadata.
     * Checksum does not match that in the flight data.
     */
    PARTIAL_CHECKSUM_FAIL;
    
    /**
     * Indicates if the current value for status indicates a complete data product.
     * @return true if a complete status, false if not
     */
    public boolean isCompleteStatus() {
    	return this == COMPLETE || this == COMPLETE_NO_CHECKSUM || this == COMPLETE_CHECKSUM_FAIL 
    	|| this == COMPLETE_CHECKSUM_PASS;
    }
    
    /**
     * Indicates if the current value for status indicates a partial data product.
     * @return true if a partial status, false if not
     */
    public boolean isPartialStatus() {
    	return this == PARTIAL || this == PARTIAL_CHECKSUM_FAIL || this == UNKNOWN;
    }
}
