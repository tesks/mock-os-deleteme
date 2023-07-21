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

package jpl.gds.product;

import jpl.gds.db.api.sql.store.StoreIdentifier;
import jpl.gds.db.api.sql.store.ldi.IEvrLDIStore;
import jpl.gds.db.api.sql.store.ldi.IProductLDIStore;
import jpl.gds.db.api.sql.store.ldi.aggregate.IChannelAggregateLDIStore;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.eha.api.message.EhaMessageType;
import jpl.gds.evr.api.message.EvrMessageType;
import jpl.gds.product.api.file.IProductFilename;
import jpl.gds.product.api.message.ProductMessageType;
import jpl.gds.product.automation.hibernate.IAutomationLogger;
import jpl.gds.product.context.IPdppContextCache;
import jpl.gds.product.processors.IPostDownlinkProductProcessor;
import jpl.gds.product.processors.PostDownlinkProductProcessorOptions;
import jpl.gds.shared.message.IMessage;
import jpl.gds.shared.message.IMessagePublicationBus;
import jpl.gds.shared.message.MessageSubscriber;
import org.springframework.context.ApplicationContext;

/**
 * Class AbstractPostDownlinkProductProcessor
 *
 * MPCS-8180 - 07/20/2016 - Imported to and updated for AMPCS M20 adaptation
 */
public abstract class AbstractPostDownlinkProductProcessor implements IPostDownlinkProductProcessor {
	/** log on the AutomationLogger's logger instance */
//	protected static final AutomationLogger log = AutomationLogger.loggerInstance;

	/** the number of products processed successfully by this processor*/
	protected int										productsProcessedSuccessfully			= 0;
	/** the number of products that failed procesing by this processor */
	protected int										productsFailed							= 0;
	/** the total number of products processed (successful and failed) by this processor*/
	protected int										totalProductsProcessed					= 0;
	/** the number of products that failed in this processor due to dictionary mispatch*/
	protected int										productsFailedDueToDictionaryMismatch	= 0;

	protected final IPdppContextCache sessionCache;

	
	/**
	 *  MPCS-8550 11/10/16 - removed srcPfn, provided redundant information
	 *  11/21/2016 - Put this back in because it made it impossible for the PDPP tests to work.  I did not
	 *  realize the implications of the changes in 8550 until after they were done.
	 */
	protected IProductFilename srcPfn;
	
	protected final PostDownlinkProductProcessorOptions	options;

	/**
	 * This is the main application context, not the context that should be used for each individual
	 * product.  This is only being kept because the metadata needs to be loaded for a product in order to create
	 * the proper child session and then a new metadata needs to be created using the new context.  
	 */
	protected ApplicationContext mainContext;
    /** The PDPP service Tracer */
    protected IAutomationLogger log;
	
	/**
     * Create an AbstractPostDownlinkProductProcessor
     * 
     * @param options
     *            the options to be used to configure this processor
     * @param appContext
     *            The current application context
     * @throws DictionaryException
     *             dictionary exception
     */
	public AbstractPostDownlinkProductProcessor(final PostDownlinkProductProcessorOptions options, final ApplicationContext appContext) throws DictionaryException {
		super();
		this.mainContext = appContext;
		this.options = options;
        this.log = appContext.getBean(IAutomationLogger.class);

		/*
		 * Display Options to stdout if requested
		 */
		if ((null != options.getDisplayOptions()) && options.getDisplayOptions()) {
			System.out.println(options);
		}

		this.sessionCache = appContext.getBean(IPdppContextCache.class);

		/*
		 * Enable display-to-console option if requested
		 */
		if ((null != options.getDisplayToConsole()) && options.getDisplayToConsole()) {
			final MessageSubscriber extractionMessageSubscriber = new MessageSubscriber() {
				@Override
				public void handleMessage(final IMessage message) {
					System.out.println(message);
				}
			};
			final MessageSubscriber productMessageSubscriber = new MessageSubscriber() {
				@Override
				public void handleMessage(final IMessage message) {
					System.out.println(message.toXml().replace("><", ">\r\n<"));
				}
			};
			final IMessagePublicationBus messageContext = appContext.getBean(IMessagePublicationBus.class);
			if (null != options.getLdiStores()) {
				for (final StoreIdentifier si: options.getLdiStores()) {
					if (IEvrLDIStore.STORE_IDENTIFIER == si) {
                        messageContext.subscribe(EvrMessageType.Evr, extractionMessageSubscriber);
					}
					else if (IChannelAggregateLDIStore.STORE_IDENTIFIER == si) {
                        messageContext.subscribe(EhaMessageType.AlarmedEhaChannel, extractionMessageSubscriber);
					}
					else if (IProductLDIStore.STORE_IDENTIFIER == si) {
                        messageContext.subscribe(ProductMessageType.ProductAssembled, productMessageSubscriber);
                        messageContext.subscribe(ProductMessageType.PartialProduct, productMessageSubscriber);
					}
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see jpl.gds.product.processors.PostDownlinkProductProcessor#getTotalProductsProcessed()
	 */
	@Override
    public int getTotalProductsProcessed() {
		return totalProductsProcessed;
	}

	/* (non-Javadoc)
	 * @see jpl.gds.product.processors.PostDownlinkProductProcessor#getProductsExtractedSuccessfully()
	 */
	@Override
    public int getProductsSuccessfullyProcessed() {
		return productsProcessedSuccessfully;
	}

	/* (non-Javadoc)
	 * @see jpl.gds.product.processors.PostDownlinkProductProcessor#getProductsFailed()
	 */
	@Override
    public int getProductsFailedForOtherReasons() {
		return productsFailed;
	}

	@Override
    public int getProductsAbortedDueToDictionaryMismatch() {
		return productsFailedDueToDictionaryMismatch;
	}

	/**
	 * 11/16/2011 - MPCS-3029 Close message portal and stores if open.
	 *
	 * IMPORTANT NOTE: THIS IS NOT THREAD SAFE!!!
	 */
	@Override
    public void close() {
		log.info("PDPP: \"" + getClass().getSimpleName() + "\" shutting down...");
		if (null != sessionCache) {
			log.info("Shutting down session context cache...");
			
			// MPCS-10670 - 03/04/19
			// Need to reorder the shutdown sequence in order for 
			// the EHA Aggregation Service to flush its buffers
			// before the Message Portal and LDI Stores are shut down.
			// Prior sequence was shutting down the LDI Stores first 
			// and causing missing channel samples in the database.
			sessionCache.stopAllChildRequiredServices();
			sessionCache.stopAllChildMessagePortals();
			sessionCache.stopAllChildStores();
			log.info("Session context cache shut down.");
		}
	}
}
