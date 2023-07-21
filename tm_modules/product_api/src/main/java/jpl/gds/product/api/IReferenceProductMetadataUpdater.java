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
 * An extension to the product metadata writer interface for the reference
 * product builder.
 * 
 *
 * @since R8
 */
public interface IReferenceProductMetadataUpdater extends IReferenceProductMetadataProvider, IProductMetadataUpdater {
    /**
     * Sets the source entity ID.
     * 
     * @param sourceEntityId the ID to set
     */
    void setSourceEntityId(int sourceEntityId);

    /**
     * Sets the CFDP Transaction ID, which is what makes the
     * product unique and links packets together.
     * 
     * @param cfdpTransactionId The ID to set.
     */
    void setCfdpTransactionId(long cfdpTransactionId);
}
