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

import jpl.gds.product.api.IProductMetadataProvider;
import jpl.gds.product.api.IProductMetadataUpdater;
import jpl.gds.product.api.IProductPartProvider;
import jpl.gds.product.api.IProductPartUpdater;
import jpl.gds.product.api.ProductException;
import jpl.gds.product.api.checksum.IProductDataChecksum;
import jpl.gds.product.api.checksum.ProductDataChecksumException;
import jpl.gds.shared.file.ISharedFileLock;
import jpl.gds.tm.service.api.packet.ITelemetryPacketMessage;

/**
 * An interface to be implemented by factories that create high-volume
 * product builder objects.
 * 
 *
 * @since R8
 */
public interface IProductBuilderObjectFactory {

	/**
	 * Due to circular dependencies this must be called after this object is created.
	 * @param txLogStorage
	 */
	void setTransactionLogStorage(ITransactionLogStorage txLogStorage);
	
	/**
	 * Creates a populated mission-specific product part object from the given
	 * packet.
	 * @param packet the IPacketMessage to build the part from
	 * @return a mission-specific Product Part object
	 * @throws ProductException if a part cannot be created for the given packet
	 * 
	 */
	IProductPartProvider createPart(ITelemetryPacketMessage packet) throws ProductException;

	/**
	 * Creates an empty (non-populated) mission-specific product part object.
	 * 
	 * @return a mission-specific Product Part object
	 * @throws ProductException if a part cannot be created
	 * 
	 */
	IProductPartProvider createPart() throws ProductException;

	/**
     * Creates a part provider and returns an updater for that new provider.
     * @return new part provider instance
     * @throws ProductException if there is an issue creating the part
     */
    public default IProductPartUpdater createPartUpdater() throws ProductException {
    	return convertToPartUpdater(createPart());
    	
    }

    /**
     * Converts the part provider to an updater.
     * 
     * @param part part provider to convert
     * @return converted part instance
     */
    public default IProductPartUpdater convertToPartUpdater(final IProductPartProvider part) {
    	return IProductPartUpdater.class.cast(part);
    }

    /**
	 * Creates an empty (non-populated) mission-specific product metadata object.
	 * @return a mission-specific Product Metadata object object
	 * 
	 */
	IProductMetadataProvider createProductMetadata();

	/**
	 * Gets a mission-specific product metadata object populated from the given
	 * product builder transaction.
	 * @param tx the IProductTransactionProvider to get data from
	 * @return the mission-specific Product Metadata object
	 */
	IProductMetadataProvider getProductMetadata(IProductTransactionProvider tx);

	/**
     * Converts the metadata provider to an updater.
     * 
     * @param md product metadata provider
     * @return converted metadata instance
     */
    public default IProductMetadataUpdater convertToMetadataUpdater(final IProductMetadataProvider md) {
    	return IProductMetadataUpdater.class.cast(md);
    }

    /**
     * Creates a new metadata provider and converts that to an updater.
     * 
     * @return metadata updater.
     */
    public default IProductMetadataUpdater createMetadataUpdater()  {
    	return convertToMetadataUpdater(createProductMetadata());
    }
    
    /**
     * Creates a product transaction provider.
     * @return product transaction
     */
    public IProductTransactionProvider createProductTransaction();

    /**
     * Creates a product transaction and casts to an updater.
     * @return product transaction updater
     */
    public default IProductTransactionUpdater createProductTransactionUpdater() {
        return convertTransactionToUpdater(createProductTransaction());
    }
    
    /**
     * Converts a product transaction provider to an updater.
     * @param provider provider to be converted
     * @return updater
     */
    public default IProductTransactionUpdater convertTransactionToUpdater(final IProductTransactionProvider provider) {
        return IProductTransactionUpdater.class.cast(provider);
        
    }

    /**
     * Creates a shared file lock.
     * 
     * @param fileName product file name
     * @return file lock instance
     */
    public ISharedFileLock createFileLock(String fileName);
    
    /**
     * Creates a shared file lock.
     * 
     * @param vcid product virtual channel ID
     * @param transactionId product transaction ID
     * @param dir File object for product directory
     * @return file lock instance
     */
    public ISharedFileLock createFileLock(int vcid, String transactionId, File dir);
    
    /**
     * Creates a product checksum calculator.
     * 
     * @return checksum calculator instance
     * @throws ProductDataChecksumException
     *             When error loading IProductDataChecksum configuration
     */
    public IProductDataChecksum createProductChecksumCalculator() throws ProductDataChecksumException;

}