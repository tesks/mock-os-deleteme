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
package jpl.gds.product.api.config;

import java.util.Properties;

/**
 * This interface class contains the getter method declarations for
 * ProductConfig properties, the name of the property file, and the property
 * names for each property contained within it.
 * 
 *
 * 05/18/17 - Changed from GDS property keys to
 *          properties and inherited blocks. Updated contained getter methods
 *          due to addition and removal of properties.
 *
 */
public interface IProductPropertiesProvider {

	/**
	 * Sub-directory of the product output directory in which to place
	 * unidentified partial data products, i.e., products for which not enough metadata
	 * has been received to identify their output location.
	 */
	public static final String UNDEFINED_PARTIAL_DIR = "UNIDENTIFIED_PARTIALS";
		

	/**
	 * Gets the list of project-specific virtual channels upon which product data may arrive.
	 * 
	 * @return the list of VCIDs; defaults to a one-element array of {0}.
	 */
	int[] getSupportedVcids();

	/**
	 * Returns the product storage directory (where products are written or read
	 * from) for test venues.
	 * 
	 * @return the path to the storage directory
	 */
	String getStorageDir();
	
	/**
	 * Returns the override product storage directory (where products are written
	 * or read from) for test venues.
	 * 
	 * @return the path to the override storage directory
	 */
	String getOverrideProductDir();

	/**
	 * Returns the product storage directory (where products are written or read
	 * from) for OPS venues.
	 * 
	 * @return the path to the storage directory used in OPS venues.
	 */
	String getOpsStorageDir();

	/**
	 * Gets the timeout in seconds indicating how long the builder should wait
	 * between receipt of the last-received product part and generation of a
	 * partial product, even if all parts have not been received.
	 * 
	 * @return the product aging timeout in seconds
	 */
	int getAgingTimeout();

	/**
	 * Gets the maximum number of product part messages that should be queued
	 * by the product writer thread before blocking incoming messages.
	 * 
	 * @return the dpsMsgQueueLimit the maximum number of messages to be queued
	 */
	int getDpsMsgQueueLimit();

	/**
	 * Gets the timeout, in milliseconds, that each attempt to post a new messages
	 * to the product writer thread will block if the writer's queue is full. When the
	 * timeout elapses, a warning message will be displayed, and the message
	 * offer will be re-attempted,
	 * 
	 * @return the product writer thread offer timeout, in milliseconds
	 */
	long getDpsMsgQueueOfferTimeout();

	/**
	 * Gets the maximum number of open product file objects kept by the product builder for
	 * all virtual channels.  
	 *  
	 * @return the maximum size of the open file cache
	 */
	long getFileObjectsCacheLimit();

	/**
	 * Gets the flag indicating whether the product builder should force out remaining
	 * partial products when shut down.
	 *         
	 * @return true if products will be forced out upon shutdown, false if not
	 */
	boolean isForcePartialsOnShutdown();

	/**
	 * Gets the flag indicating whether the builder should force out 
	 * partial products when there is a product change in the telemetry
	 * stream (i.e., when product packets are interleaved).
	 * 
	 * @return true if partials are pushed out when interleaving is encountered,
	 * false if not
	 */
	boolean isForcePartialsOnChange();

	/**
	 * Gets the flag indicating whether the builder should force out partial
	 * products when an END PDU is received regardless of whether all parts of
	 * the product have been received or not.
	 * 
	 * @return true if partials are pushed out unconditionally when END PDU is
	 *         seen, false if not.
	 */
	boolean isForcePartialsOnEndPdu();

	/**
	 * Returns the process DPOs flag, indicating the builder should
	 *         process data product objects
	 * @return true if DPO processing should be enabled; false if not
	 */
	boolean isProcessDpos();

	/**
	 * Gets the validate DPOs flag, indicating that the product builder
	 * should validate DPO checksums.
	 * 
	 * @return true if DPO checksums should be validated, false if not
	 * 
	 */
	boolean isValidateDpos();
	
	/**
	 * Gets the directory of the DPO viewer.
	 * 
	 * @return the path to the DPO viewer directory
	 */
	String getDpoViewerDir();

	/**
	 * Gets the validate products flag, indicating that the product builder
	 *         should validate product checksums.
	 * @return true if product checksums should be validated; false if not
	 */
	boolean isValidateProducts();

	/**
	 * Retrieves the flag indicating whether to channelize product fields.
	 * @return true to perform channelization; false if not
	 */
	boolean isDoChannels();

	/**
	 * Gets the configured name of the product sub-directory.
	 * 
	 * @return subdirectory name, no slashes
	 */
	String getStorageSubdir();

	/**
	 * Gets the product lock retry interval.
	 * 
	 * @return product lock retry interval, in milliseconds
	 */
	int getProductLockRetryInterval();

	/**
	 * Gets the number of retry attempts for obtaining the product lock.
	 * 
	 * @return number of product lock retries
	 */
	int getProductLockRetryCount();

	/**
	 * Gets the flag indicating whether product output sub-directory names use
	 * numeric APID or product type string.
	 * 
	 * @return true if directory names should use APID, false if they should use product
	 * type name
	 */
	boolean productDirNameUsesApid();

	/**
	 * If true, check for embedded EPDU in packets.
	 * 
	 * @return True to require check
	 *
	 */
	boolean checkEmbeddedEpdu();

	/**
	 * Gets the percentage of total DP message queue length at which the
	 * performance status of the queue should be considered YELLOW.
	 * 
	 * @return percentage value (1 - 100)
	 * 
	 */
	int getQueueYellowPercentage();

	/**
	 * Gets the percentage of total DP message queue length at which the
	 * performance status of the queue should be considered RED.
	 * 
	 * @return percentage value (1 - 100)
	 * 
	 */
	int getQueueRedPercentage();

	/**
	 * @return the checkFswDpoVersion
	 */
	boolean isCheckFswVersion();

	/**
	 * @return the assumeVersionMismatchIfNoMPDU
	 */
	boolean isAssumeVersionMismatchIfNoMPDU();
	
	/**
	 * Gets the boolean stating if the cache should be archived or not
	 * 
	 * @return the boolean representing if the cache should be archived
	 */
	public boolean isCacheArchived();

	/**
	 * Get the String that precedes the DVT in a product filename
	 *
	 * @return the string prefixing the DVT in a product filename
	 */
	String getFileDvtMarker();

	/**
	 * Get the String that separates the DVT coarse and fine values in a product
	 * filename
	 *
	 * @return the string separating the DVT coarse and fine values in a product
	 *         filename
	 */
	String getFileDvtSeparator();

	/**
	 * Gets the maximum allowed value for a product part's declared offset in bytes
	 * 
	 * @return the maximum byte offset a product part can have
	 */
	long getMaximumPartOffset();
	
	/**
	 * Get if the commanded product header should be used when the product is not streaming according to the product dictionary
	 * 
	 * @return if the commanded product header can be used
	 */
	public boolean isCommandedHeaderUsed();
	
	/**
	 * The transaction sequence number can be up to 8 bytes / 64 bits. 
	 * Gets if only the upper 63 bits should be used as the transaction sequence number.
	 * 
	 * @return boolean if only the upper 63 bits should be used as the transaction sequence number
	 */
	public boolean isUpperTSNBitsUsed();
	
	/**
	 * Returns the object containing the map of all properties.
	 * 
	 * @return properties map
	 */
	public Properties asProperties();

    /**
     * Gets the configured product checksum algorithm.
     * 
     * @return checksum algorithm implementation
     */
    public String getChecksumAlgorithm();
}