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

import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.dictionary.api.apid.IApidDefinition;
import jpl.gds.product.api.IProductMetadataUpdater;
import jpl.gds.product.api.IProductPartProvider;
import jpl.gds.product.api.builder.AssemblyTrigger;
import jpl.gds.product.api.builder.IProductTransactionProvider;
import jpl.gds.product.api.builder.ProductStorageConstants;
import jpl.gds.product.api.builder.ProductStorageException;
import jpl.gds.product.api.file.IProductFilename;
import jpl.gds.product.api.file.ProductFilenameException;
import jpl.gds.product.api.message.IPartialProductMessage;
import jpl.gds.product.api.message.IProductAssembledMessage;
import jpl.gds.shared.file.ISharedFileLock;
import jpl.gds.shared.message.IMessage;
import org.springframework.context.ApplicationContext;

import java.io.File;
import java.io.IOException;

/**
 * Class ReferenceDiskProductStorage
 */
public class ReferenceDiskProductStorage extends AbstractDiskProductStorage {

	/**
	 * Constructor.
	 * 
	 * @param appContext the current application context
	 * @param vcid the virtual channel ID
	 */
	public ReferenceDiskProductStorage(final ApplicationContext appContext, final int vcid) {
		super(appContext, vcid);
	}

    /*
	 * @see jpl.gds.product.builder.AbstractDiskProductStorage#assembleProduct(jpl.gds.product.AbstractProductPart, jpl.gds.product.builder.AssemblyTrigger, jpl.gds.product.builder.ProductFileLock)
	 */
	@Override
    protected void assembleProduct(final IProductPartProvider part,
            final AssemblyTrigger reason, ISharedFileLock fileLock) throws ProductStorageException {

        //flag used when lock is released.  Only want to release lock if it was created in this method.
        boolean lockCreatedHere = false;

        try {
            if(fileLock == null) {
                lockCreatedHere = true;
                fileLock = this.productInstanceFactory.createFileLock(part.getVcid(), part.getTransactionId(), getProductDirectory(part));
                final String infoMessage = "product ID: " + part.getTransactionId() + ", lock file path: " + fileLock.getLockFilePath();
                final boolean obtained = obtainLock(fileLock, infoMessage);
                if(!obtained) {
                    throw new ProductStorageException("Cannot obtain product file lock in DiskProductStorage");
                }
            }

            final String id = part.getTransactionId();
            final String filename = part.getFilename();
            /*
             *
             * Changed the product active subdirectory everywhere to part
             * transaction ID. There were inconsistencies: sometimes it was
             * transaction ID, and sometimes it was product directory +
             * filename. It must be consistent.
             */
            final String dirName = part.getTransactionId();

            final File dir = new File(activeDirectory, dirName);
            if (!dir.exists()) {
                /*
                 *
                 * This means that a complete product was written, but an aging timeout
                 * message had already been queued for it. Should not happen now that
                 * aging timeout messages are purged after a complete product is written,
                 * so I am making this an error.
                 *
                 *
                 * This error is still occurring and is breaking our smoke tests. This is an obscure bug that
                 * needs to be addressed, but there is insufficient time to correct this for AMPCS R6.1.0.
                 * The improvement made is an improvement from the previous release, so
                 * I have chosen make this a debug message again and defer the analysis for why it is still
                 * occurring.
                 */
                log.debug("Not storing product " + id + " because product temporary directory is missing -- timeout was bogus?");
                return;
            }

            missionAdaptation.storeAssemblyTriggeredEvent(filename, dirName, activeDirectory, reason);

            final File sourceDir = new File(activeDirectory, dirName);

            final File sourceLog = new File(sourceDir,
                    ProductStorageConstants.TRANS_LOG_FILE);
            final IProductTransactionProvider tx = missionAdaptation.loadTransactionLog(dirName, sourceLog);


            /* Go through APID definition to get name. */
            final IApidDefinition apidDef = apidDefs.getApidDefinition(part.getApid());

            final IProductMetadataUpdater md = productInstanceFactory.convertToMetadataUpdater(tx.getMetadata());
            md.setProductType(apidDef.getName());

            final File destDir = getProductDirectory(part);

            if (!destDir.exists() && !destDir.mkdirs()) {
                throw new ProductStorageException("Can't create directory "
                        + destDir + " for " + filename);
            }

            final IProductFilename pfn = missionAdaptation.computeProductStatus(tx, destDir);

            // 4/8/19 - If not already set, set from context
            // Starting with R8.1, we differentiate between sessions and contexts, and a parent context can be set
            // in IContextKey if we have both. But legacy apps like chill_down use session and context interchangeably
            // 7/1/2020 - store fsw dictionary information, too
            IContextConfiguration contextConfig = appContext.getBean(IContextConfiguration.class);
            if(md.getSessionId() == 0) {
                md.setSessionId(contextConfig.getContextId().getNumber());
                md.setSessionHost(contextConfig.getContextId().getHost());
                md.setSessionHostId(contextConfig.getContextId().getContextKey().getHostId());
            }
            if(md.getFswDictionaryDir() == null) {
                md.setFswDictionaryDir(contextConfig.getDictionaryConfig().getFswDictionaryDir());
            }
            if(md.getFswDictionaryVersion() == null) {
                md.setFswDictionaryVersion(contextConfig.getDictionaryConfig().getFswVersion());
            }

            final File sourceData = new File(sourceDir,
                    ProductStorageConstants.TEMP_DATA_FILE);

            md.setProductType(missionAdaptation.getProductType(md));
            md.setFullPath(pfn.getReservedDataFile().getAbsolutePath());
            //throws ProductStorageException which will propagate up
            missionAdaptation.storeDataFile(md, tx, sourceData, pfn);


            log.debug("In assemble product: For product " + tx.getId() +
                    " AssemblyTrigger is " + reason + " and ground status is " + md.getGroundStatus());

            /**
             * The data file has been created, build the EMD, send the product message and clear the cache if necessary.
             */
            IMessage msg;
            boolean cleanCache = true;

            switch(md.getGroundStatus()) {
            case PARTIAL:
            case UNKNOWN:
                // If it is partial we do not want to clean out the cache.  If it
                // is checksum fail, we do.
                cleanCache = false;
            case PARTIAL_CHECKSUM_FAIL:
                final IPartialProductMessage partialMessage = messageFactory.createPartialProductMessage(
                tx.getId(),
                activeDirectory + File.separator
                        + md.getDirectoryName()
                        + ProductStorageConstants.TRANS_LOG_FILE,
                        reason, 
                        md);

                msg = partialMessage;
                break;
            case COMPLETE_NO_CHECKSUM:
            case COMPLETE_CHECKSUM_PASS:
                final IProductAssembledMessage completeMessage = messageFactory.createProductAssembledMessage(md, tx.getId());
                msg = completeMessage;
                break;
            default:
                // Not sure why this would happen.
                throw new ProductStorageException("Product ground status unrecognized for transaction + " + tx.getId() + " : " + md.getGroundStatus());
            }
            missionAdaptation.storeTransactionLog(tx, pfn);
            messageContext.publish(msg);

            if (cleanCache) {
                cleanupAfterProduct(sourceDir, tx, md);
            } else {
                // Store the cache for a true partial.
                synchronized (tracker) {
                    tracker.store(md.getVcid(), tx.getId());
                }
            }

            /*
             * Removed old commented out implementation logic
             */
        } catch (final ProductFilenameException e) {
            throw new ProductStorageException("Failed to assemble product.", e);
        }
        finally{
            try {
                if(fileLock != null && lockCreatedHere) {
                    fileLock.release();
                }
            } catch (final IOException e) {
                e.printStackTrace();
            }
        }
    }
}