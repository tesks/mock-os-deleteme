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

import java.io.File;

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.product.api.IProductMetadataProvider;
import jpl.gds.product.api.IProductPartProvider;
import jpl.gds.product.api.ProductException;
import jpl.gds.product.api.file.IProductFilename;
import jpl.gds.product.api.file.ProductFilenameException;

/**
 * ProductMissionAdaptor is the interface implemented by all Product Mission Adaptations. 
 * These adaptors are responsible for delivering mission-specific functionality
 * related to data products to other components. Those components perform product
 * packet processing, product file generation to disk, and product decommutation.
 * 
 */
public interface IProductMissionAdaptor {

	/**
	 * Gets the base directory File object for product storage.
	 * @return a File object for the base directory
	 */
	public File getBaseDirectory();

	/**
	 * Sets the base directory File object for product storage.
	 * @param dir the File object to set
	 */
	public void setBaseDirectory(File dir);

	/**
	 * Stores a product part in the product storage directory.
	 * 6/23/2014 Adding storage metadata support.
	 * @param activeDirectory the directory under which part are to be
	 * written
	 * @param part the IProductPartProvider to store
	 * @return a AbstractProductStorageMetadata that contains the location into the temporary packet file within the local product disk cache where
	 * the packet data was written.
	 * @throws ProductStorageException if there is an issue storing the data
	 */
	public IProductStorageMetadata storePartData(File activeDirectory, IProductPartProvider part)
	throws ProductStorageException;

	/**
	 * Stores a "part received" event in the product transaction log. 
 	 * 6/23/2014 - Adding storage metadata support.
	 * @param activeDirectory the top level directory under parts are stored
	 * @param part the IProductPartProvider that was received
	 * @param storageMetadata the AbstractProductStorageMetadata object that contains the local offset into the temporary packet file
	 * within the product disk cache.  
	 * 
	 * @throws ProductStorageException if there is a problem writing the event to the transaction log.
	 */
	public void storePartReceivedEvent(File activeDirectory, IProductPartProvider part, IProductStorageMetadata storageMetadata)
	throws ProductStorageException;

	/**
	 * Stores a "assembly triggered" event in the product transaction log.
	 * @param id the product ID of the product to be assembled
	 * @param dirName the product directory name, under the active directory
	 * @param activeDirectory the top-level directory under which product temporary 
	 * files are to be written
	 * @param reason the reason for the assembly event
	 * @throws ProductStorageException if there is a problem writing the event
	 */
	public void storeAssemblyTriggeredEvent(String id, String dirName,
			File activeDirectory, AssemblyTrigger reason)
	throws ProductStorageException;

	/**
	 * Indicates whether receipt of the given part triggers assembly
	 * of a product. Note that this method will return true if either the
	 * new part should trigger assembly of the previous product (for which the
	 * last part was stored) or of the product associated with the current
	 * (specified as argument to this method) part.  To determine the first
	 * case, invoke this method before storage of the new part. For the
	 * second case, invoke this method after storage of the new part.
	 * 
	 * @param part the newly received product part
	 * @return true if assembly should take place
	 * @throws ProductException if there is a problem computing the assembly trigger
	 */
	public boolean isAssemblyTrigger(IProductPartProvider part)
	throws ProductException;

	/**
	 * Returns the reason that product assembly was triggered by the
	 * receipt of the given part, as one of the constants in the AssemblyTrigger
	 * class.  Logic for this method must be as follows:
	 * <br>FIRST, return AssemblyTrigger.PROD_CHANGE if the supplied part
	 * is a product type change from the last part stored by this class. This
	 * implies to the caller that the last product (not the current one) should
	 * be assembled at this point. Caller should invoke this method before
	 * storing the current part.
	 * <br>SECOND, return AssemblyTrigger.END_PART if the last stored
	 * part is the last part in the product. This implies that the current product
	 * should be assembled. Caller should invoke the this method after the
	 * current part has been stoired.
	 * <br>THIRD, return AssemblyTrigger.NO_TRIGGER if neither of the first or second 
	 * cases is true.
	 * @param part the productPart
	 * @return a reason as an AssemblyTrigger value
	 * @throws ProductException if there is a problem computing the assembly trigger
	 */
	public AssemblyTrigger getAssemblyTrigger(IProductPartProvider part)
	throws ProductException;

	/**
	 * Stores a product data file in its final location.
	 * @param md product metadata object with final product status included
	 * @param tx the IProductTransactionProvider used by the builder to assemble the
	 * product
	 * @param sourceFile the temporary data file created by the builder
	 * @param pfn the IProductFilename object for the data product
	 * @return the IProductFilename object for the final data file
	 * @throws ProductStorageException if there is an issue storing the data
	 */
	public IProductFilename storeDataFile(IProductMetadataProvider md, IProductTransactionProvider tx, File sourceFile, IProductFilename pfn)
	throws ProductStorageException;

	/**
	 * Loads the product transaction with the given ID from the given
	 * transaction file.
	 * @param id the transaction ID
	 * @param sourceFile the transaction log file
	 * @return a populated mission-specific IProductTransactionProvider object
	 * @throws ProductStorageException if there is an issue loading the log
	 */
	public IProductTransactionProvider loadTransactionLog(String id, File sourceFile)
	throws ProductStorageException;

	/**
	 * Stores the transaction log for a product.  Writes the transaction log to the metadata (p)emd file.
	 * @param tx the IProductTransactionProvider to store
	 * @param pfn the product filename
	 * @return the product filename
	 * @throws ProductStorageException if there is an error storing the metadata file
	 */
	public IProductFilename storeTransactionLog(IProductTransactionProvider tx, IProductFilename pfn)
	throws ProductStorageException;


	/**
	 * Gets the product type description associated with the given
	 * product metadata object.
	 * @param md the mission-specific product metadata object
	 * @return the product type name/description
	 */
	public String getProductType(IProductMetadataProvider md);

	/**
	 * Gets the product type description associated with the given
	 * product part.
	 * @param part the mission-specific IProductPartProvider object.
	 * @return the product type name/description
	 */
	public String getProductType(IProductPartProvider part);

	/**
	 * Gets the last product part processed by the mission adaptation for the given VCID. This allows
	 * the previous product to be assembled when the next part constitutes a
	 * change in product type. 
	 * 
	 * @param vcid the virtual channel ID of the last part to return
	 * @return the last IProductPartProvider stored by this object
	 */
	public IProductPartProvider getLastPart(int vcid);

	/**
	 * Clears the last part record for the given virtual channel ID.
	 * @param vcid the VCID to clear the last part for
	 */
	public abstract void clearLastPart(int vcid);
	
	/**
	 * Computes the delivery status of the given product. The ground status and partial status flags in
	 * the metadata object will be set by this method. A checksum may or may not be calculated depending
	 * on the mission.
	 * @param tx the product transaction
	 * @param destDir File object for the destination directory for this data product
	 * @return the product filename
	 * @throws ProductFilenameException the IProductFilename object for the product
	 */
	public IProductFilename computeProductStatus (IProductTransactionProvider tx, File destDir) throws ProductFilenameException;


    /**
     * This must be called immediately after the constructor by the application
     * running the product builder. This is a way to separate out any code that
     * could cause side effects to other apps that need to use the product
     * adaptor. May load dictionaries.
     * 
     * @throws DictionaryException if there is an error loading dictionaries
     */
	public default void init() throws DictionaryException {
		// No-op
		return;
	}
	
	/**
	 * Gets a filename from the transaction map.
	 * @param transactionId the product builder transaction ID
	 * @return the product filename, or null if no mapping found
	 */
	public String getFromFilenameMap(long transactionId);
	
	/**
	 * For product adaptors that keep filename maps only.  Adds a filename to the transaction map.
	 * 
	 * @param transactionId product transaction ID
	 * @param filename product filename
	 */
	public default void addToFilenameMap(final long transactionId, final String filename) {
	    throw new UnsupportedOperationException("Add to filename map is not supported by the current product adaptor");
	}


}