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
package jpl.gds.product.api.builder;

import jpl.gds.product.api.IProductMetadataProvider;
import jpl.gds.shared.template.Templatable;

/**
 * A read-only interface to be implemented by product transaction classes.
 * 
 *
 * @since R8
 */
public interface IProductTransactionProvider extends Templatable {

    /** Invalid grouping flags **/
    int RECORD_INVALID = 0;
    /** Grouping flag: part starts a new record **/
    int RECORD_START = 1;
    /** Grouping flag: part is a continuation of a record **/
    int RECORD_CONTINUED = 2;
    /** Grouping flag: part ends a record **/
    int RECORD_END = 3;
    /** Grouping flag: part not in any record **/
    int RECORD_NOT = 4;

    /**
     * Retrieves the receivedMetadata flag, indicating whether a metadata packet has been received for
     * the product.
     * @return true if metadata has been received
     */
    boolean isReceivedMetadata();

    /**
     * Retrieves the product metadata object.
     * @return a mission-specific product metadata object
     */
    IProductMetadataProvider getMetadata();

    /**
     * Gets the unique transaction ID for this product builder transaction.
     * @return the transaction ID
     */
    String getId();

    /**
     * Gets product filename, not including the directory path.
     * @return the filename of the product being built
     */
    String getFilename();

    /**
     * Indicates whether the product being built still has data gaps.
     * @return true if the product has gaps, or if neither the total file size nor total 
     * number of parts is known
     */
    boolean hasGaps();

    /**
     * Gets the number of missing parts in the product being assembled. This measure is only
     * reliable if the total number of parts is known.  If it is not known, the number of parts
     * missing up to the last known part number is returned.
     * @return the number of parts not yet received
     */
    int getMissingParts();

    /**
     * Gets the highest received part number.
     * @return part number
     */
    int getLastPartNumber();

    /**
     * Gets the lowest received part number.
     * @return part number
     */
    int getFirstPartNumber();

    /**
     * Gets the number of received parts.
     * 
     *
     * @return the number of received parts
     */
    int getReceivedPartCount();

    /**
     * Gets the current number of records in the product being assembled.
     * This number will change as the product is assembled.
     * @return the current number of records
     */
    int getRecordCount();

    /**
     * Gets the offset of a record in the product being assembled.
     * @param n the record number; record numbers start at 1
     * @return the byte offset of the given record
     */
    long getRecordOffset(int n);

    /**
     * Gets the active (temporary product builder) directory path for this transaction.
     * 
     * @return directory path
     */
    String getActiveDir();

    /**
     * Indicates if the end PDU has been received.
     * 
     * Need a simple way to tell if the EPDU has been received or not.  Created a method to check
     * if the fileSize has been set or is equal to -1, meaning there is no EPDU found.
     * 
     * @return true if EPDU received
     */
    boolean isReceivedEnd();

    /**
     * Convenience method to check if the metadata and the EPDU have been received, in addition to all
     * data parts. 
     * 
     * @TODO This may not be needed.
     * 
     * @return true if all parts received
     */
	boolean allPartsReceived();

	/**
	 * Gets the metadata of a product part from the transaction.
	 * @param number the part number; part numbers start at 1
	 * @return the part metadata, or null if the part has not
	 * been received
	 */
	IProductStorageMetadata getStorageMetadataForPart(int number);
}