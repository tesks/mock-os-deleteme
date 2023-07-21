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

/**
 * ProductStorageConstants contains constants related to temporary and permanent 
 * product storage on disk, primarily constant file names and extensions.
 * 
 */
public interface ProductStorageConstants {

	/**
	 *  Suffix for product data files.
	 */
	public static final String DATA_SUFFIX = ".dat";

	/**
	 *  Suffix for partial product data files.
	 */
	public static final String PARTIAL_DATA_SUFFIX = ".pdat";

	/**
	 *  Suffix for product metadata files, ".emd", stands for
	 *  "earth metadata".
	 */
	public static final String METADATA_SUFFIX = ".emd";

	/**
	 *  Suffix for product metadata files, ".emd", stands for
	 *  "earth metadata".
	 */
	public static final String PARTIAL_METADATA_SUFFIX = ".pemd";

	/**
	 * Name of the product transaction log.
	 */
	public static final String TRANS_LOG_FILE = "transaction_log.csv";

	/**
	 * Name of the temporary ACTIVE data file.
	 */
	public static final String TEMP_DATA_FILE = "temp_data.dat";

	/**
	 * metadata files for the DPOs are no longer used in the tx logs.  Removing
	 * the entry for the metadata log file.  Adding an entry for the temporary packet file.
	 */
	public static final String TEMP_PACKET_STORAGE_FILE = "temp_packet_data.pkt";

	/**
	 * Name of ACTIVE directory.
	 */
	public static final String ACTIVE_DIR = "PRODUCT_ACTIVE";
	
	/**
	 * Name of APID product version file
	 */
	public static final String APID_VERSION_FILE = "apid_versions.txt";
}
