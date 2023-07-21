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
package jpl.gds.watcher.responder.handlers;

import java.io.File;

import org.springframework.context.ApplicationContext;

import jpl.gds.dictionary.api.client.FlightDictionaryLoadingStrategy;
import jpl.gds.product.api.IProductMetadataProvider;
import jpl.gds.product.api.ProductException;
import jpl.gds.product.api.config.RecordedProductProperties;
import jpl.gds.product.api.decom.receng.IRecordedEngProductDecom;
import jpl.gds.shared.cli.app.ApplicationConfiguration;
import jpl.gds.shared.email.EmailCenter;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.metadata.context.IContextKey;
import jpl.gds.shared.types.ByteStream;
import jpl.gds.watcher.responder.app.ProductHandlerApp.ProductHandler;
import jpl.gds.watcher.responder.app.ProductHandlerApp.ProductHandlerException;

/**
 * RecordedEngineeringproductHandler is the ProductHandler implementation used by
 * the ProductMessageHandler to process newly received SMAP recorded engineering
 * products containing EHA and EVR data to be extracted and recorded to the current
 * session.
 */
public class RecordedEngineeringProductHandler extends Object implements ProductHandler
{
	
    private static final String NAME = "RecordedEngineeringProductHandler";

    private final Tracer                      log;

    /** Configured flag for global LAD insertion */
    private static final boolean INSERT_TO_GLOBAL_LAD;
    
    /** Configured flag for message service publishing */
    private final boolean publishToJms;
    
    /** Configured flag for database insertion */
    private final boolean insertToDatabase;
    
    private final Long _sessionId;

    private final IRecordedEngProductDecom _decom;
    
    private final ApplicationContext springContext;
    

	//private final IService _evrNotifier;
	// AlarmNotifierService has been removed from the LadKeeper, so keep instance here.
    // private final IService _alarmNotifier;

    static
    {
        /**
         * Eng watcher would not pass data to the global lad. For now set this to false and
         * leave all the other logic in place.
         */
        INSERT_TO_GLOBAL_LAD = false;
    }


    /**
     * Constructor.
     *
     * @throws ProductException On error in creating product decom
     */
    public RecordedEngineeringProductHandler(final ApplicationContext springContext) throws ProductException
    {
        super();

        this.springContext = springContext;
        try {
			this.springContext.getBean(FlightDictionaryLoadingStrategy.class)
			.enableApid()
			.enableChannel()
			.enableDecom()
			.enableEvr()
			.enableCommand()
            .enableMonitor()
			.loadAllEnabled(springContext, false, true);
		} catch (final Exception e) {
			throw new ProductException("", e);
		}
        // Get properties from the context
        final RecordedProductProperties rpc = springContext.getBean(RecordedProductProperties.class);
        
        publishToJms     = rpc.recordedProductProcessingPublishToMessageService();
        insertToDatabase = rpc.recordedProductProcessingInsertToDatabase();

        log = TraceManager.getTracer(springContext, Loggers.RECORDED_ENG);
        _sessionId = springContext.getBean(IContextKey.class).getNumber();

        _decom = springContext.getBean(IRecordedEngProductDecom.class,
        		                                 insertToDatabase,
                                                 INSERT_TO_GLOBAL_LAD,
                                                 publishToJms,
                                                 null,
                                                 null);
        
     }


	/**
	 * Wrapper method for processDataProduct.
	 * 
	 * @param metadata
	 *            the product metadata from the message
	 * @param bytestream
	 *            the bytestream of product data
	 * 
	 * @throws ProductHandlerException
	 *             On error in product handling
	 */
    @Override
    public void handle(final IProductMetadataProvider metadata, final ByteStream bytestream) throws ProductHandlerException
    {
        final String fullPath = metadata.getFullPath();
        final String partPath = new File(fullPath).getName();

        if (metadata.isPartial())
        {
            log.info(NAME +
                     " received data product " +
                     partPath                  +
                     " is partial, skipping");

            return;
        }

        // Compare session number in product metadata to that in the current session configuration
        // and discard products that do not match.

        final Long productSession = metadata.getSessionId();

        if ((_sessionId != null) && ! _sessionId.equals(productSession))
        {
            log.debug(NAME +
                     " received data product "              +
                     partPath                               +
                     " does not match configured session: " +
                     productSession                         +
                     " instead of expected "                +
                     _sessionId);
            return;
        }

        log.info(NAME + " received data product " + partPath);

        // Decom the recorded engineering

        try
        {
            _decom.execute(fullPath);
        }
        catch (final ProductException pe)
        {
            log.warn(pe.getMessage() +
                     ". Will not channelize or extract EVRs from " +
                     fullPath                         +
                     ".");
        }
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void shutdown()
    {

        _decom.shutdown();
        
        /** Close transport connections */
        EmailCenter.closeAll();

    }


    /**
     * Returns the custom command-line usage text for the application
     * using this handler.
     *
     * @return the usage text String
     */
    @Override
    public String getUsageText()
    {
        return "Usage: "                                                                                     +
               ApplicationConfiguration.getApplicationName("chill_recorded_eng_watcher")                       +
               " --venueType <venueType> [--testbedName <testbedName>\n"                                     +
               "                            --testKey <number> --downlinkStreamId <streamId>\n"              +
               "                            --testUser <user> --testHost <hostname>\n"                       +
               "                            --sessionConfig <filename> --jmsHost <hostname> --jmsPort <port>\n" +
               "                            --jmsSubtopic <subtopic> --exitWithSession --printLog]\n";
    }

}
