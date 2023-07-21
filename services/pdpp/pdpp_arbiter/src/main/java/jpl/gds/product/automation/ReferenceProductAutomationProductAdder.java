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

package jpl.gds.product.automation;

import jpl.gds.product.api.IProductMetadataProvider;
import jpl.gds.product.api.file.IProductMetadata;
import jpl.gds.product.automation.disruptor.ProductMetadataEvent;
import jpl.gds.product.automation.hibernate.IAutomationLogger;
import jpl.gds.product.automation.hibernate.dao.ProductAutomationStatusDAO;
import org.hibernate.Transaction;
import org.springframework.context.ApplicationContext;

/**
 * MCSADAPT-180 - 12/3/2019 - This class was adapted from M20/MSL to allow for multimission PDPP.
 * Comments were brought over intact for their historical value.
 */
public class ReferenceProductAutomationProductAdder extends AbstractProductAutomationProductAdder implements IProductAutomationProductAdder {

    private static IAutomationLogger log;
    private int errorCount;
    private final int maxErrors;
    private final ProductAutomationStatusDAO instance;
    private final long fswBuildId;
    private final ApplicationContext appContext;

    /**
     * Public constructor
     */
    public ReferenceProductAutomationProductAdder(final ApplicationContext appContext, boolean isProductExtracted) {
        super(isProductExtracted);
        this.appContext = appContext;
        this.errorCount = 0;

        final int tmp = appContext.getBean(ProductAutomationProperties.class).getChillDownMaxErrors();
        maxErrors = tmp <= 0 ? 10 : tmp;

        instance = appContext.getBean(ProductAutomationStatusDAO.class);

        /**
         * Some adaptations rely on multiple dictionaries for one run, but other adaptations only need one.
         * For our generic/reference implementation, one dictionary is used, so we can set the fswBuildId to one value.
         */
        this.fswBuildId = 1L;

        this.log = appContext.getBean(IAutomationLogger.class);

    }


    @Override
    public void onEvent(final ProductMetadataEvent event, final long sequence, final boolean endOfBatch) throws Exception {
        /**
         * MPCS-6468 -  8/2014 - Real time extraction flag is no longer
         * used.
         *
         * MPCS-6716 -  10/2014 - The process of polling a single
         * product from the queue and adding it in its own hibernate session /
         * transaction is extremely inefficient. Also, if there is an error and
         * the transaction would need to be rolled back, the product was not put
         * back into the productQueue so it would be lost.
         */
        // Exit after either the current product was able to be processed or has failed too many times

        IProductMetadataProvider amd = null;
        amd = event.get();

        /*
         * MPCS-8568 01/04/17 - added check against max errors, perform no
         * action with the event if the adder has already thrown an error
         */
        if (amd == null || (errorCount >= maxErrors)) {
            return;
        }

        do {
            try {
                /**
                 * MPCS-6647 -  9/2014 - The metadata object from the
                 * event does not have everything we need populated. Use the
                 * session config to fill in the blanks.
                 *
                 * NOTE: This will never add a product as having an R0
                 * dictionary version, it will put the fsw version id and the
                 * session dictionary version. So PDPP will need to be able to
                 * deal with checking these things.
                 *
                 * MPCS-6758  - Was incorrectly using the default fsw
                 * version from the session config as the dictionary for the
                 * session.
                 *
                 * MPCS-6767 -  - Revert the optimizations that were not
                 * actually optimizations.
                 *
                 */
                final IProductMetadata md = (IProductMetadata) amd;
                final Transaction transaction = instance.startTransaction();

                // MPCS-11847 - switched from using mapper lookup for dictionary info to metadata on the product
                instance.addUncategorized(md.getFullPath(), // Path
                        null, // Parent path. Always null in chill_down
                        this.fswBuildId, // Build ID
                        md.getFswDictionaryVersion(), // Dictionary
                        md.getFswDictionaryDir(), // dictionary directory
                        //MPCS-11572 - Set session ID from metadata, not context
                        md.getSessionId(), // Session ID
                        md.getSessionHost(), // Session host
                        md.getApid(), // apid
                        md.getVcid(), // vcid
                        md.getSclkCoarse(), // sclk coarse
                        md.getSclkFine(), // sclk fine
                        md.getIsCompressed() ? 1 : 0,
                        isRealtimeExtactionOn ? 1 : 0
                );

                transaction.commit();
                errorCount = 0;

                /*
                 * MPCS-8295 11/28/16 - Catch any exception. Rollback, as
                 * before, but now directly retry adding the product metadata.
                 * This time isn't taken up with going back up and having the
                 * exception handler put it back in the ring buffer.
                 */
            } catch (final Exception e) {
                instance.rollback();
                log.error("Could not add product to hibernate database.  Rolling back transacton and closing session: ("
                        + e.getMessage() + ")");

                errorCount++;

            } finally {
                instance.closeSession();
            }

            // Check the error counts.
            if (errorCount >= maxErrors) {
                /*
                 * If errorCount drops to 0, the product was added and onEvent
                 * will terminate on its own. If it goes high enough, then this
                 * will throw an exception and the Disruptor will handle
                 * shutting down everything.
                 */
                throw new AutomationException("Automation product adder reached the maximum allowed consecutive "
                        + "errors adding products to the hibernate database.  Shutting down process.");
            }

        } while (errorCount != 0);
    }
}