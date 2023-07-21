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
package jpl.gds.telem.common.feature;

import org.springframework.context.ApplicationContext;

import jpl.gds.common.notify.NotificationProperties;
import jpl.gds.common.service.telem.AbstractTelemetryFeatureManager;
import jpl.gds.evr.api.service.IEvrNotifierService;
import jpl.gds.evr.api.service.IEvrPublisherService;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;

/**
 * EvrFeatureManager handles everything required to initialize and shutdown the EVR
 * packet decom capability in the downlink process.
 *
 *
 */
public class EvrFeatureManager extends AbstractTelemetryFeatureManager {

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean init(final ApplicationContext springContext) {
        log = TraceManager.getTracer(springContext, Loggers.DOWNLINK);
        setValid(false);
        if (!this.isEnabled()) {
            return true;
        }
        
        setValid(true);
        
         /*
         * The command dictionary is no longer
         * required by the call to load the EVR XML. However, the command
         * dictionary is still needed to process EVRs, and we don't want it
         * loaded randomly in the middle of processing for performance
         * reasons. The following block forces the default command
         * dictionary to be loaded now. Do not remove it.
         */

//        if (GdsSystemProperties.applicationIsSse()) {
//        	springContext.getBean(SseDictionaryLoadingStrategy.class)
//        		.enableEvr();
//        } else {
//        	springContext.getBean(FlightDictionaryLoadingStrategy.class)
//        		.enableEvr()
//        		.enableCommand()
//        		.enableSequence();
//            try {
//            	// Load it and only keep the opcode to stem mappings.
//                springContext.getBean(ICommandUtilityDictionaryManager.class)
//                	.load(true);
//
//            }
//            catch (final Exception e) {
//                // OK not to have command dictionary here
//            }
//
//            try {
//                springContext.getBean(ISequenceUtilityDictionaryManager.class)
//                	.load();
//            }
//            catch (final Exception e) {
//                // OK not to have sequence dictionary here
//            }
//
//        }
//
//        try
//        {
//            final IEvrUtilityDictionaryManager evrDict = springContext.getBean(IEvrUtilityDictionaryManager.class);
//            evrDict.loadAll();
//        } 
//        catch (final Exception e)
//        {
//            e.printStackTrace();
//            log.fatal("Unable to load EVR definitions");
//            return false;
//        }
        
        // This should always already be done outside of this feature.
//        try
//        {
//        	springContext.getBean(IApidDefinitionProvider.class);
//
//        } 
//        catch (final Exception e)
//        {
//        	e.printStackTrace();
//            log.error("Unable to load APID definitions");
//            return false;
//        }

        // Start the EVR publisher, which will listen for packet messages and publish Evr messages
        // Also start other EVR features if configured to do so
        try {
            addService(springContext.getBean(IEvrPublisherService.class));
            final NotificationProperties nc = springContext.getBean(NotificationProperties.class);

            if (nc.isRealtimeEvrNotificationEnabled() ||
                    nc.isRecordedEvrNotificationEnabled())
            {
                try {
                    addService(springContext.getBean(IEvrNotifierService.class));
                } catch (final Exception e) {
                    e.printStackTrace();
                    log.error("No EVR notification will be done");
                }
            }

            setValid(startAllServices());
            log.debug("Evr Decom feature successfully initialized");

        } catch (final Exception e) {
            e.printStackTrace();
            log.error("Unable to start EVR adapter: " + e.toString());
            setValid(false);
            return false;
        }

        return isValid();
    }
}
