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
package jpl.gds.product.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Writer;

import jpl.gds.product.api.*;
import org.springframework.context.ApplicationContext;

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.apid.IApidDefinitionProvider;
import jpl.gds.product.api.builder.AssemblyTrigger;
import jpl.gds.product.api.builder.IProductBuilderObjectFactory;
import jpl.gds.product.api.builder.IProductMissionAdaptor;
import jpl.gds.product.api.builder.IProductTransactionProvider;
import jpl.gds.product.api.builder.IReceivedPartsTracker;
import jpl.gds.product.api.builder.ITransactionLogStorage;
import jpl.gds.product.api.builder.ProductStorageConstants;
import jpl.gds.product.api.builder.ProductStorageException;
import jpl.gds.product.api.checksum.IProductDataChecksum;
import jpl.gds.product.api.checksum.ProductDataChecksumException;
import jpl.gds.product.api.config.IProductPropertiesProvider;
import jpl.gds.product.api.file.IProductFilename;
import jpl.gds.product.api.file.IProductFilenameBuilderFactory;
import jpl.gds.product.api.file.ProductFilenameException;
import jpl.gds.product.api.message.IProductMessageFactory;
import jpl.gds.shared.file.FileUtility;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.message.IMessagePublicationBus;

/**
 * AbstractProductAdapter is a general base class that can be used for
 * developing mission-specific Product Builder Mission Adaptors. It contains methods
 * common to all adaptation implementations.
 *
 *
 */
public abstract class AbstractProductAdapter implements IProductMissionAdaptor {

	/**
	 * Flag indicating whether product checksums should be verified.
	 */
	protected final boolean validateProductChecksum;
	/**
	 * Trace logger instance shared with subclasses.
	 */
    protected Tracer log;


	/**
	 * Transaction log storage object shared with subclasses.
	 */
	// TODO Only one instance? One per VCID sounds a lot safer.
	protected ITransactionLogStorage transactionLogStorage;
	
	/**
	 * Last written product parts record, indexed by product builder VCID.
	 */
	// TODO This probably really should be some sort of static member of Product Builder
	protected IProductPartProvider[] lastParts;
	
	/**
	 * File object for the root data product output directory.
	 */
	private File baseDirectory;
	
	/** The current application context */
	protected ApplicationContext appContext;
	/** The internal publication bus */
	protected IMessagePublicationBus bus;
	/** APID dictionary instance */
	protected IApidDefinitionProvider apidReference;
	
	/** Factory for creating high-volume product builder objects */
	protected final IProductBuilderObjectFactory productInstanceFactory;
	/** Recived parts tracker instance */
	protected final IReceivedPartsTracker tracker;	
	/** Product configuration properties */
	protected final IProductPropertiesProvider productConfig;
	/** Product filename builder factory */
	protected final IProductFilenameBuilderFactory filenameBuilderFactory;
	/** Product message factory */
    protected IProductMessageFactory messageFactory;

	/**
	 *
	 * Constructs an instance of AbstractProductAdapter. This loads the APID dictionary
	 * for the current mission.
	 * @param context the current application context
	 *
	 * @throws DictionaryException if the APID dictionary cannot be loaded
	 */
	public AbstractProductAdapter(final ApplicationContext context) throws DictionaryException {
		appContext = context;
        log = TraceManager.getTracer(context, Loggers.TLM_PRODUCT);
		bus = appContext.getBean(IMessagePublicationBus.class);
		tracker = appContext.getBean(IReceivedPartsTracker.class);
		
		productConfig = appContext.getBean(IProductPropertiesProvider.class);
		this.validateProductChecksum = productConfig.isValidateProducts();

		final int[] vcids = productConfig.getSupportedVcids();
		int highest = 0;
		for (int i = 0; i < vcids.length; i++) {
			if (vcids[i] > highest) {
				highest = vcids[i];
			}
		}
		lastParts = new IProductPartProvider[highest + 1];
		
		apidReference = context.getBean(IApidDefinitionProvider.class);		
		transactionLogStorage = context.getBean(ITransactionLogStorage.class);
		productInstanceFactory = context.getBean(IProductBuilderObjectFactory.class);
		// need to specify the reference factory by name, since we added a PDPP factory as well
		filenameBuilderFactory = context.getBean(ProductApiBeans.PRODUCT_FILENAME_BUILDER_FACTORY, IProductFilenameBuilderFactory.class);
		messageFactory = context.getBean(IProductMessageFactory.class);
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.product.api.builder.IProductMissionAdaptor#setBaseDirectory(java.io.File)
	 */
	@Override
	public void setBaseDirectory(final File baseDirectory) {
		this.baseDirectory = baseDirectory;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.product.api.builder.IProductMissionAdaptor#getBaseDirectory()
	 */
	@Override
	public File getBaseDirectory() {
		return baseDirectory;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.product.api.builder.IProductMissionAdaptor#storeAssemblyTriggeredEvent(java.lang.String, java.lang.String, java.io.File, jpl.gds.product.api.builder.AssemblyTrigger)
	 */
	@Override
	public void storeAssemblyTriggeredEvent(final String id, final String dirName,
			final File activeDirectory, final AssemblyTrigger reason) throws ProductStorageException {
		final File dir = new File(activeDirectory, dirName);
		if (!dir.exists()) {
			throw new ProductStorageException("Can't assemble product " + id
					+ ": no directory found at " + dir);
		}
		final File file = new File(dir, ProductStorageConstants.TRANS_LOG_FILE);
		if (!file.exists()) {
			throw new ProductStorageException("Can't assemble product " + id
					+ ": no events file found at " + file);
		}
		FileWriter writer = null;
		try {
			writer = new FileWriter(file, true);
			transactionLogStorage.writeAssemblyTriggeredEvent(writer, reason);
		} catch (final IOException e) {
			throw new ProductStorageException("Can't write to file " + file);
		} finally {
			close(writer);
		}
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.product.api.builder.IProductMissionAdaptor#loadTransactionLog(java.lang.String, java.io.File)
	 */
	@Override
	public IProductTransactionProvider loadTransactionLog(final String id, final File sourceFile)
	throws ProductStorageException {
		LineNumberReader reader = null;
		try {
			reader = new LineNumberReader(new FileReader(sourceFile));
		} catch (final FileNotFoundException e) {
			throw new ProductStorageException("No transaction log to store for"
					+ sourceFile);
		}
		final IProductTransactionProvider tx = transactionLogStorage.loadTransactionLog(id, reader);
		productInstanceFactory.convertTransactionToUpdater(tx).setActiveDir(sourceFile.getParent());

		return tx;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IProductFilename storeTransactionLog(final IProductTransactionProvider tx, final IProductFilename reservedPfn)
	throws ProductStorageException {
		File destFile = null;
		FileWriter writer = null;
		try {
		    destFile = reservedPfn.getReservedMetadataFile();
			writer = new FileWriter(destFile);
		} catch (final IOException e) {
			throw new ProductStorageException("Can't write transaction log"
					+ " for transaction " + destFile);
		}
		transactionLogStorage.storeTransactionToMetadataFile(writer, tx);
		close(writer);
		return reservedPfn;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IProductFilename storeDataFile(final IProductMetadataProvider md, final IProductTransactionProvider tx, final File sourceFile,
			final IProductFilename pfn) throws ProductStorageException {
	    
	    try {
            if (!sourceFile.exists()) {
                final FileOutputStream fos = new FileOutputStream(sourceFile);
                fos.close();
            }

            /**
             * Use the Files.createLink to create hard links instead of using the
             * copyFile, which will use memory buffers to create a new file.  This is more efficient and is being implemented
             * to fix out of memory issues.
             *
             */

            final File dest = pfn.getReservedDataFile();

            /**
             * Hards links have issues with NFS and can not be used.  Go back to the old
             * way of doing this which was a file copy.
             */
            FileUtility.copyFile(sourceFile, dest);

            /*
             * removed code that deleted the complete sourcefile. This gets done in DiskProductStorage.
             */
        }

        catch (final IOException e) {
            e.printStackTrace();
            throw new ProductStorageException("Can't copy product " + tx.getId() + " to destination directory " + pfn, e);
        }
        return pfn;
	}

	/**
	 * Closes a file Writer and catches the IOException.
	 *
	 * @param writer
	 *            the writer to close
	 */
	protected void close(final Writer writer) {
		if (writer != null) {
			try {
				writer.close();
			} catch (final IOException e) {
				// ok - nothing we can do anyway
			}
		}
	}

	/**
	 * Constructs the next partial product data file name.
	 *
	 * @param version the partial file version identifier
	 * @param destDir the destination directory for the data file
	 * @param filename the base file name for the data file
	 * @return a File object for the complete file path to the data file
	 *
	 * @throws RuntimeException if a file with the resulting path and name already exists
	 */
	protected File getNextPartialDataFile(final String version, final File destDir, final String filename)
	throws RuntimeException {
		final File destFile = new File(destDir, filename + "_Partial-" + version
					+ ProductStorageConstants.PARTIAL_DATA_SUFFIX);
		if (destFile.exists()) {
			throw new RuntimeException("Partial product data file already exists: " + destFile.getPath());
		}
		return destFile;
	}

	/**
	 * Gets the next complete data file version identifier. This method must actually
	 * query the file system to find the next version for the product data file. This means
	 * that as long as the same root product output directory is used, data product file
	 * version numbers will increment across MPCS sessions.
	 *
	 * @param destDir the destination directory for the data file
	 * @param filename the base file name for the data file
	 * @return the complete file version identifier to use for the next complete data product
	 */
	protected String getNextCompleteFileVersion(final File destDir, final String filename) {
		File destFile = null;
		final boolean found = false;
		int counter = 1;
		while (!found) {
			destFile = new File(destDir, filename + "-" + counter
					+ ProductStorageConstants.DATA_SUFFIX);
			if (!destFile.exists()) {
				break;
			}
			++counter;
		}
		return String.valueOf(counter);
	}

	/**
	 * Gets the next partial data file version identifier. This method must
	 * actually query the file system to find the next version for the product
	 * data file. This means that as long as the same root product output
	 * directory is used, data product file version numbers will increment
	 * across MPCS sessions. <br>
	 * Partials are numbered within completes. Therefore, the partial version
	 * identifier returned by this method will include the input complete
	 * version.
	 *
	 * @param completeVersion
	 *            the next complete product version
	 * @param destDir the destination directory for the data file
	 * @param filename the base file name for the data file
	 * @return the partial file version identifier for the next partial data product
	 */
	protected String getNextPartialFileVersion(final String completeVersion, final File destDir, final String filename) {
		File destFile = null;
		final boolean found = false;
		int counter = 1;
		while (!found) {
			destFile = new File(destDir, filename + "_Partial-" + completeVersion + "." + counter
					+ ProductStorageConstants.PARTIAL_DATA_SUFFIX);
			if (!destFile.exists()) {
				break;
			}
			++counter;
		}
		return completeVersion + "." + counter;
	}


	/**
	 * Constructs the next complete product data file name.
	 *
	 * @param completeVersion the complete file version identifier
	 * @param destDir
	 *            the destination directory for the data file
	 * @param filename
	 *            the base file name for the data file
	 * @return a File object for the complete file path to the data file
	 *
	 * @throws RuntimeException if a file already exists with the resulting path and filename
	 *
	 * TODO Use ProductStorageException rather than RuntimeException
	 */
	protected File getNextCompleteDataFile(final String completeVersion, final File destDir, final String filename)
	throws RuntimeException {
		final File destFile = new File(destDir, filename + "-" + completeVersion
					+ ProductStorageConstants.DATA_SUFFIX);
		if (destFile.exists()) {
		    throw new RuntimeException("Complete product data file already exists: " + destFile.getPath());
		}
		return destFile;
	}

	/**
	 * Constructs the next partial product metadata file name.
	 *
	 * @param version the partial file version identifier
	 * @param destDir
	 *            the destination directory
	 * @param filename
	 *            the base file name for the metadata file
	 * @return a File object for the complete file path to the metadata file
	 *
	 * @throws RuntimeException if a file already exists with the resulting path and filename
	 *
	 * TODO Use ProductStorageException rather than RuntimeException
	 */
	protected File getNextPartialMetadataFile(final String version, final File destDir, final String filename)
	throws RuntimeException {
		final File destFile = new File(destDir, filename + "_Partial-" + version
				+ ProductStorageConstants.PARTIAL_METADATA_SUFFIX);
		if (destFile.exists()) {
			throw new RuntimeException("Partial product metadata file already exists: " + destFile.getPath());
		}
		return destFile;
	}

	/**
	 * Constructs the next complete product metadata file name.
	 *
	 * @param version the complete file version identifier
	 * @param destDir
	 *            the destination directory
	 * @param filename
	 *            the base file name for the metadata file
	 * @return a File object for the complete file path to the metadata file
	 *
	 * @throws RuntimeException if a file already exists with the resulting path and filename
	 *
	 * TODO Use ProductStorageException rather than RuntimeException
	 */
	protected File getNextCompleteMetadataFile(final String version, final File destDir, final String filename)
	throws RuntimeException {
		final File destFile = new File(destDir, filename + "-" + version
					+ ProductStorageConstants.METADATA_SUFFIX);
		if (destFile.exists()) {
			throw new RuntimeException("Complete product metadata file already exists: " + destFile.getPath());
		}
		return destFile;
	}

	/**
	 * Sets the last product part that was created by the product builder on a
	 * given virtual channel. This is used to determine when to push out partial
	 * products.
	 *
	 * @param part
	 *            the AbstractProductPart to set
	 */
	protected void setLastPart(final IProductPartProvider part) {
		lastParts[part.getVcid()] = part;
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.product.api.builder.IProductMissionAdaptor#getLastPart(int)
	 */
	@Override
	public IProductPartProvider getLastPart(final int vcid) {
		return lastParts[vcid];
	}

	/**
	 * {@inheritDoc}
	 * @see jpl.gds.product.api.builder.IProductMissionAdaptor#clearLastPart(int)
	 */
	@Override
	public void clearLastPart(final int vcid) {
		lastParts[vcid] = null;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public IProductFilename computeProductStatus(final IProductTransactionProvider tx, final File destDir) throws ProductFilenameException  {

		final IProductMetadataUpdater md = productInstanceFactory.convertToMetadataUpdater(productInstanceFactory.getProductMetadata(tx));
        final File productFile = new File(tx.getActiveDir() + File.separator + ProductStorageConstants.TEMP_DATA_FILE);
        long computedChecksum = 0;
        long providedChecksum = 0;

        /**
         * Calling the method that checks if there are gaps or we have not yet received
         * the M or E pdus.
         */
        boolean isPartial = tx.hasGaps() || !tx.isReceivedMetadata();
        ProductStatusType statusType;

        /**
         * Added setting of Actual File Size to metadata
         */
        if (productFile.exists()) {
            md.setActualFileSize(productFile.length());
        }
        if (isPartial) {
            statusType = ProductStatusType.PARTIAL;
        } else if (!validateProductChecksum) {
            statusType = ProductStatusType.COMPLETE_NO_CHECKSUM;
        } else {

            if (!productFile.exists()) {
                log.error(String.format("Product file %s does not exist and the checksum can not be computed.", productFile.getAbsolutePath()));
                statusType = ProductStatusType.UNKNOWN;
            } else {
                providedChecksum = md.getChecksum();

                try {
                    final IProductDataChecksum checker = appContext.getBean(IProductDataChecksum.class);
                    computedChecksum = checker.computeChecksum(productFile);
                    md.setActualChecksum(computedChecksum);
                } catch (final ProductDataChecksumException e) {
                    log.error("Failed to calculate checksum for transaction " + tx.getId() + " : " + e.getMessage());
                }

                if (providedChecksum == computedChecksum) {
                    statusType = ProductStatusType.COMPLETE_CHECKSUM_PASS;
                } else {
                    log.error("Checksum validation failed for data product " +
                            tx.getId() + ": received=" + providedChecksum + ", computed="+ computedChecksum);
                    statusType = ProductStatusType.PARTIAL_CHECKSUM_FAIL;
                    /**
                     * Forgot to include setting the isPartial flag if we have a checksum fail which
                     * caused this product to look like a complete and to get created twice.
                     */
                    isPartial = true;
                }
            }
        }

        md.setGroundStatus(statusType);
        md.setPartial(isPartial);

        /**
         *  Use the new builder pattern to create the product file name objects.
         */
        final IProductFilename pfn = filenameBuilderFactory.createBuilder()
                .addIsPartial(isPartial)
                .addProductPath(destDir.getAbsolutePath())
                .addProductName(tx.getFilename())
                .build()
                .reserve();

        md.setProductVersion(pfn.getProductVersionString(false));

		return pfn;
	}


}
