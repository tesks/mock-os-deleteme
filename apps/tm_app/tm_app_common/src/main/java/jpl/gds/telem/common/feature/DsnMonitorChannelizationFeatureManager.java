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

import jpl.gds.common.service.telem.AbstractTelemetryFeatureManager;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.client.FlightDictionaryLoadingStrategy;
import jpl.gds.dictionary.api.client.channel.ChannelDictionaryClientUtility;
import jpl.gds.dictionary.api.client.channel.IChannelUtilityDictionaryManager;
import jpl.gds.eha.api.service.channel.IDsnMonitorDecomService;
import jpl.gds.shared.interfaces.IService;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.spring.context.flag.SseContextFlag;

/**
 * DsnMonitorChannelizationFeatureManager does what is required to initialize
 * and shutdown the channelization of DSN monitor SFDUs. The EhaFeatureManager
 * must also be enabled, as it loads the channel dictionaries and starts other
 * services needed for processing channels.
 * 
 */
public class DsnMonitorChannelizationFeatureManager extends
AbstractTelemetryFeatureManager {

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean init(final ApplicationContext springContext) {
        log = TraceManager.getTracer(springContext, Loggers.TLM_MONITOR);
        setValid(false);

        if (!isEnabled()) {
            return true;
        }

        setValid(true);
        
        /*
         * Regardless of configuration, there will
         * never be station monitor data or dictionaries for SSE/GSE. Disable
         * the service if SSE.
         */
        
        if (springContext.getBean(SseContextFlag.class).isApplicationSse()) {
            log.info("Station monitor channelization is being disabled for SSE/GSE processing");
            enable(false);
            return true;
        }
        
        IService monProcessor;
        try {
            try {
		        final IChannelUtilityDictionaryManager chanTable = springContext.getBean(ChannelDictionaryClientUtility.class);
		        springContext.getBean(FlightDictionaryLoadingStrategy.class).enableMonitor();
                chanTable.loadMonitor(true);
            } catch (final DictionaryException e) {
                log.warn("Station monitor channelization is enabled but the monitor dictionary could not be loaded:" + e.toString(), e);
            }
            monProcessor = springContext.getBean(IDsnMonitorDecomService.class);
            addService(monProcessor);
        } catch (final Exception e) {
            log.error("Station monitor channelization service could not be started");
            e.printStackTrace();
            setValid(false);
            return false;
        }

        setValid(startAllServices());

        if (isValid()) {
            log.debug("DSN Monitor channelization feature successfully initialized");
        }

        return isValid();
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.common.service.telem.AbstractTelemetryFeatureManager#shutdown()
     */
    @Override
    public void shutdown() {

        if (!isEnabled()) {
            return;
        }
        super.shutdown();

        log.debug("DSN Monitor channelization feature has shutdown");
    }
}
