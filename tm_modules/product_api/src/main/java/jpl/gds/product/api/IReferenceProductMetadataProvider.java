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
 * An extension to the read-only product metadata interface for the reference
 * product builder.
 * 
 *
 * @since R8
 */
public interface IReferenceProductMetadataProvider extends IProductMetadataProvider {
  
    /**
     * Returns the CFDP Transaction ID for this product
     * 
     * @return the CFDP Transaction ID for this product
     */
    public long getCfdpTransactionId();
    
    /**
     * Returns the CFDP source entity ID for this product.
     * 
     * @return source entity ID
     */
    public int getSourceEntityId();
    
}
