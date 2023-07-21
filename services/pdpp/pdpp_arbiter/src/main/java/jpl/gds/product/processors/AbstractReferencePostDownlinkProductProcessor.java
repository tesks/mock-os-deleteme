/*
 * Copyright 2006-2020. California Institute of Technology.
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

import jpl.gds.context.api.IContextConfiguration;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.product.AbstractPostDownlinkProductProcessor;
import jpl.gds.product.IPdppFileWriter;
import jpl.gds.product.PdppApiBeans;
import jpl.gds.product.PdppFileWriter;
import jpl.gds.product.api.IProductMetadataProvider;
import jpl.gds.product.api.ProductException;
import jpl.gds.product.api.builder.AssemblyTrigger;
import jpl.gds.product.api.builder.IProductBuilderObjectFactory;
import jpl.gds.product.api.file.IProductFilenameBuilderFactory;
import jpl.gds.product.api.file.IProductMetadata;
import jpl.gds.product.api.file.ProductFilenameException;
import jpl.gds.product.api.message.IPartialProductMessage;
import jpl.gds.product.api.message.IProductAssembledMessage;
import jpl.gds.product.api.message.IProductMessageFactory;
import jpl.gds.product.context.IPdppContextContainer;
import jpl.gds.product.processors.descriptions.IPdppDescription;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessagePublicationBus;
import org.springframework.context.ApplicationContext;

/**
 * Class created as part of PDPP multimissionization. This abstract Processor contains tools that other processors
 * can reuse, such as sending a JMS message and creating a child product.
 *
 */
public abstract class AbstractReferencePostDownlinkProductProcessor extends AbstractPostDownlinkProductProcessor {

    protected final IProductFilenameBuilderFactory filenameBuilderFactory;
    protected final IProductMessageFactory messageFactory;

    public AbstractReferencePostDownlinkProductProcessor(final PostDownlinkProductProcessorOptions options, final ApplicationContext appContext) throws DictionaryException {
        super(options, appContext);

        filenameBuilderFactory = appContext.getBean(PdppApiBeans.PDPP_PRODUCT_FILENAME_BUILDER_FACTORY, IProductFilenameBuilderFactory.class);
        messageFactory = appContext.getBean(IProductMessageFactory.class);
    }

    /**
     * Takes an ReferenceProductMetadata and publishes a product message for the
     * associated product on the SessionBasedMessageContext
     *
     * @param md
     *            ReferenceProductMetadata for the product
     * @param options
     *            the current PostDownlinkProductProcessorOptions for this
     *            product
     * @throws PostDownlinkProductProcessingException
     *             An error occurs while publishing the message
     */
    protected void sendProductMessage(final IPdppContextContainer container, final IProductMetadata md, final PostDownlinkProductProcessorOptions options) throws PostDownlinkProductProcessingException {
        final IMessage msg;

        /*
         * Once again, get the default session config. This should be synced up when everything is figured out to make this cleaner.
         */
        final IContextConfiguration cc = container.getChildContext().getBean(IContextConfiguration.class);
        /*
         * Prepare a ProductAssembledMessage for transmission on JMS buss.
         */
        if (md.isPartial()) {
            /*
             * 03/20/2012 - MPCS-3401 - Need to create the proper message type depending on whether the product is
             * COMPLETE or PARTIAL.
             */
            final IPartialProductMessage pmsg = messageFactory.createPartialProductMessage(md.getFilenameWithPrefix(), null, AssemblyTrigger.DICTIONARY_CORRECTION, md);

            /*
             * Add appropriate data to message
             */
            pmsg.setEventTime(md.getProductCreationTime());
            pmsg.setContextHeader(cc.getMetadataHeader());
            msg = pmsg;
        }
        else {
            /*
             * 03/20/2012 - MPCS-3401 - Need to create the proper message type depending on whether the product is
             * COMPLETE or PARTIAL.
             */
            final IProductAssembledMessage cmsg = messageFactory.createProductAssembledMessage(md, md.getFilenameWithPrefix());

            /*
             * Add appropriate data to message
             */
            cmsg.setEventTime(md.getProductCreationTime());
            cmsg.setContextHeader(cc.getMetadataHeader());
            msg = cmsg;
        }

        /*
         * Fire off message.
         */
        container.getChildContext().getBean(IMessagePublicationBus.class).publish(msg);
    }

    /**
     * {@inheritDoc}
     * @see jpl.gds.product.processors.IPostDownlinkProductProcessor#processProduct(IProductMetadataProvider, IPdppDescription)
     */
    public IProductMetadata processProduct(final IProductMetadataProvider metadata, IPdppDescription suffix) throws ProductException {
        final IProductMetadata parentMetadata = (IProductMetadata)metadata;
        IProductMetadata childMetadata = null;

        try {
            final IPdppContextContainer container = getContainer(parentMetadata, suffix);

            childMetadata = getChildMetadata(container, parentMetadata);

            if (!options.getUseDatabase()) {
                container.getChildSessionConfig().getContextId().setNumber(0L);
            }

            /**
             * Start the store controller for the child context.
             */
            container.startChildDbStores();
            container.startChildMessagePortal();

            /**
             * This just means we are creating a child product
             */
            createChildProduct(container, childMetadata);
            
            sendProductMessage(container, childMetadata, options);

            return childMetadata;
        } catch (Exception ex){
            // do nothing
        }

        return childMetadata;
    }

    protected IProductMetadata getChildMetadata(IPdppContextContainer container, IProductMetadata parentMetadata) throws ProductFilenameException, ProductException {
        final String dataFile = srcPfn == null ? parentMetadata.getAbsoluteDataFile() : srcPfn.getDataFilePath();
        IProductMetadata childMetadata = (IProductMetadata) container.getChildContext().getBean(IProductBuilderObjectFactory.class).createMetadataUpdater();
        childMetadata.loadFile(filenameBuilderFactory.createBuilder()
                .addFullProductPath(dataFile)
                .build().getMetadataFilePath());
        return childMetadata;
    }

    protected IPdppContextContainer getContainer(IProductMetadata metadata, IPdppDescription suffix) throws PostDownlinkProductProcessingException {
        return this.sessionCache.getContextContainer(metadata,
                suffix,
                options,
                true);
    }

    protected void createChildProduct(IPdppContextContainer container, IProductMetadata childMetadata) throws ProductException, ProductFilenameException {
        /*
         * Morph the data product to the new session.
         */
        final IPdppFileWriter fileWriter = new PdppFileWriter(
                container.getParentSessionConfig(),
                container.getChildSessionConfig(),
                childMetadata,
                container.getChildContext());

        if (srcPfn == null) {
            srcPfn = filenameBuilderFactory.createBuilder().addFullProductPath(childMetadata.getAbsoluteDataFile()).build();
        }

        fileWriter.writeFile(srcPfn);
    }


}
