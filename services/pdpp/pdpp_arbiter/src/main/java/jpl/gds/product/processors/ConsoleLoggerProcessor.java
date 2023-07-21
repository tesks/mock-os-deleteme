/*
 * Copyright 2006-2019. California Institute of Technology.
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

package jpl.gds.product.processors;

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.product.api.ProductException;
import jpl.gds.product.api.file.IProductMetadata;
import jpl.gds.product.api.file.IProductMetadataBuilder;
import jpl.gds.product.api.file.ProductFilenameException;
import jpl.gds.product.processors.descriptions.IPdppDescription;
import jpl.gds.product.processors.descriptions.LoggerPdppDescription;
import org.springframework.context.ApplicationContext;

/**
 * MPCS-11563 - multimssion PDPP
 * Simple dummy processor to use as a reference, corresponds to the ConsoleLoggerChecker.
 *
 * This processor only logs that it has received and "processed" the product it was given.
 * The intention is to provide a no-op way to run PDPP for the generic adaptations, or
 * for missions starting out with PDPP development. With this processor in place, a mission can run PDPP with
 * some configuration, but without needing to write its own processors and checkers first.
 *
 */
public class ConsoleLoggerProcessor extends AbstractReferencePostDownlinkProductProcessor implements IPostDownlinkProductProcessor {

    private LoggerPdppDescription description;
    private final Boolean isCreatesChildFiles = false;
    private final Boolean isProcessPartialProducts = false;

    /**
     * Constructor
     * @param options the PostdownlinkProcessorOptions used to configure this processor
     * @throws DictionaryException if an execption is encountered instantiating this processor
     */
    public ConsoleLoggerProcessor(final PostDownlinkProductProcessorOptions options, final ApplicationContext appContext) throws DictionaryException {
        super(options, appContext);
        description = new LoggerPdppDescription();
    }

    /**
     * Allows for product processing with just the filename, plus the process and product id, for logging.
     * For this processor, the only "processing" that happens is that a log is written.
     * @param filename
     * @param processId
     * @param productId
     * @throws ProductException
     */
    public IProductMetadata processProduct(String filename, final long processId, final long productId) throws ProductException {
        try {
            srcPfn = filenameBuilderFactory.createBuilder()
                    .addFullProductPath(filename)
                    .build();
        } catch (ProductFilenameException e) {
            e.printStackTrace();
        }

        final IProductMetadataBuilder metadataBuilder = mainContext.getBean(IProductMetadataBuilder.class);

        final IProductMetadata dummyMetadata = metadataBuilder.build();

        dummyMetadata.loadFile(srcPfn.getMetadataFilePath());

        dummyMetadata.setPartial(srcPfn.isPartial());

        IProductMetadata result = processProduct(dummyMetadata, processId, productId);;

        return result;
    }

    /**
     * Performs product processing with full product metadata, plus the process and product id, for logging.
     * For this processor, the only "processing" that happens is that a log is written.
     * @param metadata
     * @param processId
     * @param productId
     * @throws ProductException
     */
    @Override
    public IProductMetadata processProduct(final IProductMetadata metadata, final long processId, final long productId) throws ProductException {

        if(!isProcessPartialProducts && metadata.isPartial()) {
            log.info("skipping file " + metadata.getFilename() + " because it is a partial product.", processId, productId);
            return metadata;
        }

        log.info("\n*************  Logger processed something! *************", processId, productId);

        return run(metadata, description, processId, productId);
    }

    private IProductMetadata run(final IProductMetadata metadata, IPdppDescription description, final long processId, final long productId) throws ProductException {

        IProductMetadata result = null;
        String filename = metadata.getFilename();

        try{
            if(isCreatesChildFiles) {
                result = super.processProduct(metadata, description);
            }
            String message = "\nThe following file has been processed : " + filename;
            log.info(message, processId, productId);
            ++productsProcessedSuccessfully;
        } catch (Exception ex) {
            String error = "\nFailed to process product : " + filename;
            log.error(error, processId, productId);
            ++productsFailed;
        }

        ++totalProductsProcessed;

        return result;
    }


}