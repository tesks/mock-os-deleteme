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
package jpl.gds.product.impl.config;

import java.io.File;
import java.util.List;

import jpl.gds.context.api.IGeneralContextInformation;
import jpl.gds.product.api.config.IProductPropertiesProvider;
import jpl.gds.shared.config.GdsHierarchicalProperties;
import jpl.gds.shared.spring.context.flag.SseContextFlag;

/**
 * This object contains the GDS settings and parameters for the product builder.
 * Settings are obtained from the product.properties file.
 * 
 *
 * 05/18/17 - Updated reference properties - addedones that were being indirectly referenced,
 *          removed the ones that were not being used.
 * 07/05/17 - Changed name to ProductProperties,
 *          changed property file name to product.properties, changed
 *          PROPERTY_PREFIX to "product."
 * 3/23/18 - Added product checksum validation algorithm config
 * 
 */
public class ProductProperties extends GdsHierarchicalProperties implements IProductPropertiesProvider {

    /**
     * Name of the default properties file.
     */
    public static final String PROPERTY_FILE = "product.properties";
    
    /**
     * Base string for configuration properties. All other properties will be
     * based upon the base key and the mission.
     */
    private static final String PROPERTY_PREFIX = "product.";

    /**
     * String segment for validation property blocks.
     */
    private static final String VALIDATE_BLOCK_PREFIX = "validate.";

    /**
     * String segment for product assembly properties.
     */
    private static final String PRODUCT_ASSEMBLY_BLOCK =
            PROPERTY_PREFIX + "assembly.";
    
    /**
     * String segment for product channelization properties.
     */
    private static final String PRODUCT_CHANNELIZE_BLOCK =
            PROPERTY_PREFIX + "channelize.";
    
    /**
     * String segment for dpo properties
     */
    private static final String PRODUCT_DPO_BLOCK =
            PROPERTY_PREFIX + "dpo.";
    
    /**
     * String segment for dps queue properties
     */
    private static final String PRODUCT_DPS_BLOCK =
            PROPERTY_PREFIX + "dpsQueue.";
    
    /**
     * String segment for product process properties
     */
    private static final String PRODUCT_PROCESS_BLOCK =
            PROPERTY_PREFIX + "process.";
    
    /**
     * String segment for product storage properties
     */
    private static final String PRODUCT_STORAGE_BLOCK =
            PROPERTY_PREFIX + "storage.";
    
    /**
     * String segment for product vcid properties
     */
    private static final String PRODUCT_VCIDS_BLOCK =
            PROPERTY_PREFIX + "vcids.";    
    
    /**
     * String segment for product assembly force partial
     * creation block
     */
    private static final String FORCE_PARTIAL_BLOCK =
            PRODUCT_ASSEMBLY_BLOCK + "forcePartial.";
    
    /**
     * String segment for product channelize validation block
     */
    private static final String CHANNELIZE_VALIDATION_BLOCK =
            PRODUCT_CHANNELIZE_BLOCK + VALIDATE_BLOCK_PREFIX;
    
    /**
     * String segment for process filename block
     */
    private static final String PROCESS_FILENAME_BLOCK =
            PRODUCT_PROCESS_BLOCK + "filename.";
    /**
     * String segment for process filename DVT block
     */
    private static final String PROCESS_FILENAME_DVT_BLOCK =
            PROCESS_FILENAME_BLOCK + "dvt.";
    
    /**
     * String segment for product storage cache block
     */
    private static final String PRODUCT_STORAGE_CACHE_BLOCK =
            PRODUCT_STORAGE_BLOCK + "cache.";
    
    /**
     * String segment for product storage directory block
     */
    private static final String PRODUCT_STORAGE_DIRECTORY_BLOCK =
            PRODUCT_STORAGE_BLOCK + "directory.";
    
    
    /**
     * String segment for product storage product lock file
     * block
     */
    private static final String PRODUCT_STORAGE_LOCK_BLOCK =
            PRODUCT_STORAGE_BLOCK + "product." + "lock.";
    
    /**
     * String segment for product storage lock file retry block
     */
    private static final String PRODUCT_STORAGE_LOCK_RETRY_BLOCK =
            PRODUCT_STORAGE_LOCK_BLOCK + "retry.";
    
    
    
    
    
    /**
     * Configuration property for whether or not to check for embedded EPDU in packets
     */
    private static final String CHECK_EMBEDDED_EPDU =
            PRODUCT_ASSEMBLY_BLOCK + "check." + "embeddedEpdu";
    
    
    /**
     * Configuration property for partial generation upon APID change.
     */
    private static final String FORCE_PARTIAL_ON_CHANGE_PROPERTY =
            FORCE_PARTIAL_BLOCK + "change";
    
    /**
     * Configuration property for unconditional partial generation upon END PDU.
     */
    private static final String FORCE_PARTIAL_ON_EPDU =
            FORCE_PARTIAL_BLOCK + "epdu";
    
    /**
     * Configuration property for the product aging timeout.
     */
    private static final String FORCE_PARTIAL_ON_TIMEOUT =
            FORCE_PARTIAL_BLOCK + "timeout";
    
    /**
     * Configuration property for product checksum validation.
     */
    private static final String VALIDATE_PRODUCT_CHECKSUM =
            PRODUCT_ASSEMBLY_BLOCK + VALIDATE_BLOCK_PREFIX + "checksum";
    

    /**
     * Configuration property for product checksum validation algorithm
     */
    public static final String  CHECKSUM_ALGORITHM = 
            PRODUCT_ASSEMBLY_BLOCK + VALIDATE_BLOCK_PREFIX + "checksum.algorithm";
    
    
    /**
     *
     * Property name for flag that indicates whether to validate FSW version on the product before channelizing DPOs.
     * <p>
     * true = chill_down will compare the current session's FSW Build ID against the DPO's FSW Build ID and only extract EVRs or
     * channelize data products in real time if the two FSW Build IDs match.
     * <p>
     * false = chill_down will extract EVRs and channelize data products in real time regardless of whether the current session's
     * FSW Build ID matches the DPO's FSW Build ID.
     */
    private static final String VALIDATE_FSW_VERSION =
            CHANNELIZE_VALIDATION_BLOCK + "fsw";
    
    /**
     *
     * Property name for flag that indicates whether to channelize DPOs if no MPDU with FSW Version has been received
     * <p>
     * true = chill_down will consider an absence of the DPO's FSW Build ID to indicate a MATCH between the current session's FSW
     * Build ID and the DPO's FSW Build ID This can happen if the MPDU is sent out of order and is not the first PDU received by
     * chill_down for a particular DPO.<br>
     * This will potentially result in some EHAs and EVRs channelized and extracted with an incorrect dictionary.
     * <p>
     * false = chill_down will consider an absence of the DPO's FSW Build ID to indicate a MISMATCH between the current session's
     * FSW Build ID and the DPO's FSW Build ID
     * <p>
     * This can happen if the MPDU is sent out of order and is not the first PDU received by chill_down for a particular DPO.<br>
     * <p>
     * This will potentially result in some EHAs and EVRs being lost due to presumed incorrect dictionary caused by the delayed
     * receipt of the MPDU.<br>
     */
    private static final String VALIDATE_MPDU =
            CHANNELIZE_VALIDATION_BLOCK + "mpdu";
    
    /**
     * Configuration property for performing product channelization.
     */
    private static final String PERFORM_CHANNELIZATION =
            PROPERTY_PREFIX + "channelize";
    
    
    
    /**
     * Configuration property for DPO processing.
     */
    private static final String PROCESS_DPOS =
            PRODUCT_DPO_BLOCK + "process";
    
    /**
     * Configuration property for VIEWER directory
     */
	public static final String VIEWER_DIR =
            PROPERTY_PREFIX + "viewer.dir";
	
    /**
     * Configuration property for DPO checksum validation.
     */
    private static final String VALIDATE_DPO_CHECKSUM =
            PRODUCT_DPO_BLOCK + VALIDATE_BLOCK_PREFIX + "checksum";
    
    
    
    /**
     * Configuration property for maximum DiskProductStorage queue size.
     */
    private static final String DPS_QUEUE_MSG_LIMIT =
            PRODUCT_DPS_BLOCK + "limit";
    
    
    /**
     * Configuration property for DiskProductStorage queue offer timeout duration value.
     */
    private static final String DPS_QUEUE_MSG_OFFER_TIMEOUT_MS =
            PRODUCT_DPS_BLOCK + "msgOffer." + "timeoutMs";
    
    /**
     * Configuration property for the queue percentage at which health should be considered YELLOW.
     */
    private static final String DPS_QUEUE_YELLOW_LEVEL =
            PRODUCT_DPS_BLOCK + "yellow";
    
    /**
     * Configuration property for the queue percentage at which health should be considered RED.
     */
    private static final String DPS_QUEUE_RED_LEVEL =
            PRODUCT_DPS_BLOCK + "red";
    
    /**
     * Configuration property for using the commanded product header when the product dictionary streaming flag is false
     */
    private static final String USE_COMMANDED_HEADER =
            PRODUCT_PROCESS_BLOCK + "commandedHeader." + "use";
    
    /**
     * Configuration property for locating the character preceeding the DVT in a product filename
     */
    private static final String FILENAME_DVT_MARKER =
            PROCESS_FILENAME_DVT_BLOCK + "marker";
    
    /**
     * Configuration property for loacting the character between the DVT segments in a product filename
     */
    private static final String FILENAME_DVT_SEPARATOR =
            PROCESS_FILENAME_DVT_BLOCK + "separator";
    
    /**
     * Configuration property for indicating the maximum offset a product part can have.
     */
    private static final String PRODUCT_PART_MAX_OFFSET =
            PRODUCT_PROCESS_BLOCK + "partOffset." + "max";
    
    /**
     * Configuration property for indicating if the upper 63 or lower 63 (of 64) bits will be used for the transaction sequence number
     */
    private static final String TRANS_SEQ_NUM_USE_UPPER_63_BITS =
            PRODUCT_PROCESS_BLOCK + "transactionNum." + "upperBits";
    
    /**
     * Configuration property for stating if the cache should be archived or not
     */
    private static final String CACHE_TO_ARCHIVE =
            PRODUCT_STORAGE_CACHE_BLOCK + "archive";
    
    /**
     * Configuration property for data products file objects cache limit.
     */
    private static final String CACHE_OBJECT_LIMIT =
            PRODUCT_STORAGE_CACHE_BLOCK + "limit";
    
    /**
     * Configuration property for the temp product parts storage directory for
     * OPS venues.
     */
    private static final String OPS_STORAGE_DIRECTORY =
            PRODUCT_STORAGE_DIRECTORY_BLOCK + "ops";
    
    /**
     * Configuration property for overriding the product storage directory and subdirectory
     */
    private static final String STORAGE_DIRECTORY_OVERRIDE =
            PRODUCT_STORAGE_DIRECTORY_BLOCK + "override";
    
    /**
     * Configuration property for the product storage sub-directory (the name of
     * the directory under the test directory)
     */
    private static final String STORAGE_SUBDIR =
            PRODUCT_STORAGE_DIRECTORY_BLOCK + "sub";
    
    /**
     * Configuration property for whether to use APID or product type in product directory names.
     */
    private static final String STORAGE_DIR_WITH_APID =
            PRODUCT_STORAGE_DIRECTORY_BLOCK + "withApid";
    
    
    /**
     * Configuration property for whether to push out partials upon shutdown.
     */
    private static final String STORE_PARTIALS_ON_SHUTDOWN =
            PRODUCT_STORAGE_BLOCK + "partial." + "onShutdown";
    
    /**
     * Configuration property for the number of maximum retries to get the product file lock.
     */
    private static final String PRODUCT_LOCK_RETRY_COUNT =
            PRODUCT_STORAGE_LOCK_RETRY_BLOCK + "count";
    
    /**
     * Configuration property for the interval between checks on the product file lock.
     */
    private static final String PRODUCT_LOCK_RETRY_INTERVAL =
            PRODUCT_STORAGE_LOCK_RETRY_BLOCK + "interval";
    
    /**
     * Configuration property for supported virtual channel IDs.
     */
    private static final String ALLOWED_VCIDS =
            PRODUCT_VCIDS_BLOCK + "allowed";
    
    private static final String              UNSPECIFIED_DIR                  = "[COMPUTED]";

    private final IGeneralContextInformation generalInfo;

    /**
     * Creates an instance and loads it with values from the
     * current configuration files. In this case the output
     * directory is set to UNSPECIFIED_DIR. This constructor should
     * be used only by configuration tools, and not at runtime
     * by the product builder.
     * 
     */
    public ProductProperties() {
        this(null, new SseContextFlag());
    }

    /**
     * Creates an instance of ProductConfig and loads it with values from the
     * current configuration files.
     * 
     * @param generalInfo
     *            The IGeneralContextInformation
     * @param sseFlag
     *            The SSE context flag
     */
    public ProductProperties(final IGeneralContextInformation generalInfo, final SseContextFlag sseFlag) {
        super(PROPERTY_FILE, sseFlag);
        this.generalInfo = generalInfo;
    }


    /**
     * Gets the list of project-specific virtual channels upon which product data may arrive.
     * 
     * @return the list of VCIDs; defaults to a one-element array of {0}.
     */
    @Override
	public int[] getSupportedVcids() {
    	int[] supportedVcids;
    	
    	final List<String> vcidListStr = getListProperty(ALLOWED_VCIDS, null, ",");
		if (vcidListStr == null || vcidListStr.isEmpty()) {
			supportedVcids = new int[1];
			supportedVcids[0] = 0;
		} else {
			supportedVcids = new int[vcidListStr.size()];
			for (int i = 0; i < vcidListStr.size(); i++) {
				supportedVcids[i] = Integer.parseInt(vcidListStr.get(i));
			}
		}
		
		return supportedVcids;
    }

    /**
     * Returns the product storage directory (where products are written or read
     * from) for test venues.
     * 
     * @return the path to the storage directory
     */
    @Override
	public String getStorageDir() {
        return (generalInfo != null ? generalInfo.getOutputDir() : UNSPECIFIED_DIR) + File.separator
                + getStorageSubdir();
    }
    
    /**
	 * Returns the override product storage directory (where products are written
	 * or read from) for test venues.
	 * 
	 * @return the path to the override storage directory
	 */
	@Override
    public String getOverrideProductDir() {
		if (getProperty(STORAGE_DIRECTORY_OVERRIDE) == null || getProperty(STORAGE_DIRECTORY_OVERRIDE).isEmpty()){
			return null;
		}
		else {
			return getProperty(STORAGE_DIRECTORY_OVERRIDE);
		}
	}

    /**
     * Returns the product storage directory (where products are written or read
     * from) for OPS venues.
     * 
     * @return the path to the storage directory used in OPS venues.
     */
    @Override
	public String getOpsStorageDir(){
        return getProperty(OPS_STORAGE_DIRECTORY);
    }

    /**
     * Gets the timeout in seconds indicating how long the builder should wait
     * between receipt of the last-received product part and generation of a
     * partial product, even if all parts have not been received.
     * 
     * @return the product aging timeout in seconds
     */
    @Override
	public int getAgingTimeout() {
        return getIntProperty(FORCE_PARTIAL_ON_TIMEOUT, 60);
    }

    /**
     * Gets the maximum number of product part messages that should be queued
     * by the product writer thread before blocking incoming messages.
     * 
	 * @return the dpsMsgQueueLimit the maximum number of messages to be queued
	 */
	@Override
	public int getDpsMsgQueueLimit() {
		return getIntProperty(DPS_QUEUE_MSG_LIMIT, 99999);
	}

	/**
	 * Gets the timeout, in milliseconds, that each attempt to post a new messages
	 * to the product writer thread will block if the writer's queue is full. When the
	 * timeout elapses, a warning message will be displayed, and the message
	 * offer will be re-attempted,
	 * 
	 * @return the product writer thread offer timeout, in milliseconds
	 */
	@Override
	public long getDpsMsgQueueOfferTimeout() {
		return getLongProperty(DPS_QUEUE_MSG_OFFER_TIMEOUT_MS, 2000L);
	}

	/**
	 * Gets the maximum number of open product file objects kept by the product builder for
	 * all virtual channels.  
	 *  
	 * @return the maximum size of the open file cache
	 */
	@Override
	public long getFileObjectsCacheLimit() {
		return getIntProperty(CACHE_OBJECT_LIMIT, 4);
	}

	/**
	 * Gets the flag indicating whether the product builder should force out remaining
     * partial products when shut down.
     *         
     * @return true if products will be forced out upon shutdown, false if not
     */
    @Override
	public boolean isForcePartialsOnShutdown() {
        return getBooleanProperty(STORE_PARTIALS_ON_SHUTDOWN, false);
    }
    
    /**
     * Gets the flag indicating whether the builder should force out 
     * partial products when there is a product change in the telemetry
     * stream (i.e., when product packets are interleaved).
     * 
     * @return true if partials are pushed out when interleaving is encountered,
     * false if not
     */
    @Override
	public boolean isForcePartialsOnChange() {
        return getBooleanProperty(FORCE_PARTIAL_ON_CHANGE_PROPERTY, false);
    }

	/**
	 * Gets the flag indicating whether the builder should force out partial
	 * products when an END PDU is received regardless of whether all parts of
	 * the product have been received or not.
	 * 
	 * @return true if partials are pushed out unconditionally when END PDU is
	 *         seen, false if not.
	 */
	@Override
	public boolean isForcePartialsOnEndPdu() {
    	return getBooleanProperty(FORCE_PARTIAL_ON_EPDU, false);
    }

    /**
     * Returns the process DPOs flag, indicating the builder should
     *         process data product objects
     * @return true if DPO processing should be enabled; false if not
     */
    @Override
	public boolean isProcessDpos() {
        return getBooleanProperty(PROCESS_DPOS, false);
    }
    
    @Override
    public String getDpoViewerDir(){
    	return getProperty(VIEWER_DIR);
    }

    /**
     * Gets the validate DPOs flag, indicating that the product builder
     * should validate DPO checksums.
     * 
     * @return true if DPO checksums should be validated, false if not
     * 
     */
    @Override
	public boolean isValidateDpos() {
        return getBooleanProperty(VALIDATE_DPO_CHECKSUM, false);
    }

    /**
     * Gets the validate products flag, indicating that the product builder
     *         should validate product checksums.
     * @return true if product checksums should be validated; false if not
     */
    @Override
	public boolean isValidateProducts() {
        return getBooleanProperty(VALIDATE_PRODUCT_CHECKSUM, false);
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String getChecksumAlgorithm() {
        return getProperty(CHECKSUM_ALGORITHM, "cfdp");
    }

    /**
     * Retrieves the flag indicating whether to channelize product fields.
     * @return true to perform channelization; false if not
     */
    @Override
	public boolean isDoChannels() {
        return getBooleanProperty(PERFORM_CHANNELIZATION, false);
    }
    
    /**
     * Gets the configured name of the product sub-directory.
     * 
     * @return subdirectory name, no slashes
     */
    @Override
	public String getStorageSubdir() {
    	return getProperty(STORAGE_SUBDIR, "products");
    }
    
    /**
     * Gets the product lock retry interval.
     * 
     * @return product lock retry interval, in milliseconds
     */
    @Override
	public int getProductLockRetryInterval() {
    	return getIntProperty(PRODUCT_LOCK_RETRY_INTERVAL, 500);
    }
    
    /**
     * Gets the number of retry attempts for obtaining the product lock.
     * 
     * @return number of product lock retries
     */
    @Override
	public int getProductLockRetryCount() {
    	return getIntProperty(PRODUCT_LOCK_RETRY_COUNT, 5);
    }
    
    /**
     * Gets the flag indicating whether product output sub-directory names use
     * numeric APID or product type string.
     * 
     * @return true if directory names should use APID, false if they should use product
     * type name
     */
    @Override
	public boolean productDirNameUsesApid() {
    	return getBooleanProperty(STORAGE_DIR_WITH_APID, true);
    }


    /**
     * If true, check for embedded EPDU in packets.
     * 
     * @return True to require check
     *
     */
    @Override
	public boolean checkEmbeddedEpdu()
    {
        return getBooleanProperty(CHECK_EMBEDDED_EPDU, false);
    }

    /**
     * Gets the percentage of total DP message queue length at which the
     * performance status of the queue should be considered YELLOW.
     * 
     * @return percentage value (1 - 100)
     * 
     */
    @Override
	public int getQueueYellowPercentage() {
        return Math.max(Math.min(getIntProperty(DPS_QUEUE_YELLOW_LEVEL, 80), 100), 0);
    }

    /**
     * Gets the percentage of total DP message queue length at which the
     * performance status of the queue should be considered RED.
     * 
     * @return percentage value (1 - 100)
     * 
     */
    @Override
	public int getQueueRedPercentage() {
        return Math.max(Math.min(getIntProperty(DPS_QUEUE_RED_LEVEL, 95), 100), 0);
    }


	/**
	 * @return the checkFswDpoVersion
	 */
	@Override
	public boolean isCheckFswVersion() {
		return getBooleanProperty(VALIDATE_FSW_VERSION, true);
	}

	/**
	 * @return the assumeVersionMismatchIfNoMPDU
	 */
	@Override
	public boolean isAssumeVersionMismatchIfNoMPDU() {
		return getBooleanProperty(VALIDATE_MPDU, true);
	}
	
	/**
	 * Get the String that precedes the DVT in a product filename
	 * 
	 * @return the string prefixing the DVT in a product filename
	 */
	@Override
    public String getFileDvtMarker(){
		return getProperty(FILENAME_DVT_MARKER, "_");
	}
	
	/**
	 * Get the String that separates the DVT coarse and fine values in a product
	 * filename
	 * 
	 * @return the string separating the DVT coarse and fine values in a product
	 *         filename
	 */
	@Override
    public String getFileDvtSeparator(){
		return getProperty(FILENAME_DVT_SEPARATOR, "-");
	}

	/**
	 * Gets the maximum allowed value for a product part's declared offset in bytes
	 * 
	 * @return the maximum byte offset a product part can have
	 */
	@Override
    public long getMaximumPartOffset() {
		return getLongProperty(PRODUCT_PART_MAX_OFFSET, 4194303);
	}
	
	/**
	 * Gets the boolean stating if the cache should be archived or not
	 * 
	 * @return the boolean representing if the cache should be archived
	 */
	@Override
    public boolean isCacheArchived(){
		return getBooleanProperty(CACHE_TO_ARCHIVE, false);
	}
	
	/**
	 * Get if the commanded product header should be used when the product is not streaming according to the product dictionary
	 * 
	 * @return if the commanded product header can be used
	 */
	@Override
    public boolean isCommandedHeaderUsed(){
		return getBooleanProperty(USE_COMMANDED_HEADER, true);
	}
	
	/**
	 * The transaction sequence number can be up to 8 bytes / 64 bits. 
	 * Gets if only the upper 63 bits should be used as the transaction sequence number.
	 * 
	 * @return boolean if only the upper 63 bits should be used as the transaction sequence number
	 */
	@Override
    public boolean isUpperTSNBitsUsed(){
		return getBooleanProperty(TRANS_SEQ_NUM_USE_UPPER_63_BITS, false);
	}

	@Override
    public String getPropertyPrefix() {
	    return PROPERTY_PREFIX;
	}
}
