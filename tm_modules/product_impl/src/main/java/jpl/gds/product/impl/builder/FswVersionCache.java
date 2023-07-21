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

package jpl.gds.product.impl.builder;

import java.util.HashMap;

/**
 * FswVersionCache is a singleton class used by MslProductPart to
 * cache the FSW version ID as products are received. The FSW version
 * ID is specified in the Metadata PDU, and this cache allows
 * subsequent Data PDUs to be associated with their FSW version ID.
 * 
 * One of the reasons for having a separate class for this purpose is
 * to facilitate synchronization.
 * 
 * If the cache carries a value < 0, it's considered uninitialized.
 *
 *
 * 5/2015 - This cache is not used anymore so no point in holding onto it.  Deprecating it for now.
 */
@Deprecated
public class FswVersionCache {
    private final HashMap<String, Long> versions = new HashMap<String, Long>();
    
    private static FswVersionCache instance;
    
    /**
     * Private constructor: creates an instance of FswVersionCache.
     */
    private FswVersionCache() {
    }
    
    /**
     * Gets the single instance of this class.
     * @return instance of FswVersionCache
     */
    public static FswVersionCache getInstance() {
        if (instance == null) {
            instance = new FswVersionCache();
        }
        return instance;
    }
    
    /**
     * Set the FSW version ID cache to new value for a product.
     * 
     * @param vcid the virtual channel ID on which the product was received
     * @param transactionId the product transaction identifier
     * @param newVersion the FSW version ID to set
     */
    public void setVersion(final int vcid, final String transactionId, final long newVersion) {
    	final String key = vcid + "/" + transactionId;
    	versions.put(key, newVersion);
    }
    
    /**
     * Get the cached FSW version ID value for a product.
     * 
     * @param vcid the virtual channel ID on which the product was received
     * @param transactionId the product transaction identifier     
     *  
     * @return FSW version ID
     */
    public long getVersion(final int vcid, final String transactionId) {
    	final String key = vcid + "/" + transactionId;
    	final Long version = versions.get(key);
    	if (version == null) {
    		return -1;
    	} else {
    		return version.longValue();
        }
    }
    
    /**
     * Clears the cached FSW version ID value for a product. 
     * 
     * @param vcid the virtual channel ID on which the product was received
     * @param transactionId the product transaction identifier     
     */
    public void resetCache(final int vcid, final String transactionId) {
    	final String key = vcid + "/" + transactionId;
    	versions.remove(key);
    }
    
}

