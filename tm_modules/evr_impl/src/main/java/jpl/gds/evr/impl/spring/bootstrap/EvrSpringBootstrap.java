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
package jpl.gds.evr.impl.spring.bootstrap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.evr.api.EvrApiBeans;
import jpl.gds.evr.api.IEvrFactory;
import jpl.gds.evr.api.config.EvrProperties;
import jpl.gds.evr.api.message.EvrMessageType;
import jpl.gds.evr.api.message.IEvrMessageFactory;
import jpl.gds.evr.api.service.IEvrNotifierService;
import jpl.gds.evr.api.service.IEvrPublisherService;
import jpl.gds.evr.api.service.extractor.EvrExtractorException;
import jpl.gds.evr.api.service.extractor.IEvrExtractor;
import jpl.gds.evr.api.service.extractor.IEvrExtractorUtility;
import jpl.gds.evr.api.service.extractor.IRawEvrDataFactory;
import jpl.gds.evr.impl.EvrFactory;
import jpl.gds.evr.impl.message.EvrMessage;
import jpl.gds.evr.impl.message.EvrMessageFactory;
import jpl.gds.evr.impl.service.EvrPublisherService;
import jpl.gds.evr.impl.service.extractor.EvrExtractorUtility;
import jpl.gds.evr.impl.service.extractor.JplMultimissionEvrExtractor;
import jpl.gds.evr.impl.service.extractor.JplSseEvrExtractor;
import jpl.gds.evr.impl.service.extractor.RawEvrDataFactory;
import jpl.gds.evr.impl.service.notify.EvrNotifierService;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.shared.message.RegisteredMessageConfiguration;
import jpl.gds.shared.spring.context.flag.SseContextFlag;

/**
 * Spring bootstrap configuration class for the EVR projects.
 * 
 *
 * @since R8
 */
@Configuration
public class EvrSpringBootstrap {
    
    @Autowired
    ApplicationContext appContext;

    /**
     * Constructor.
     */
    public EvrSpringBootstrap() {
        // Remove definition of XML parser
        MessageRegistry.registerMessageType(new RegisteredMessageConfiguration(EvrMessageType.Evr,
                null, EvrMessage.BinaryParseHandler.class.getName(), 
                new String[] {"EVR"}));
    }
	
	
	/**
     * Gets the singleton EvrProperties bean.
     * 
     * @param missionProps
     *            the current MissionProperties bean, autowired.
     * @param sseFlag
     *            The SSE context flag
     * @return the EvrProperties bean
     */
    @Bean(name = EvrApiBeans.EVR_PROPERTIES)
	@Scope("singleton")
	@Lazy(value = true)
    public EvrProperties getEvrProperties(final MissionProperties missionProps, final SseContextFlag sseFlag) {
        return new EvrProperties(missionProps, sseFlag);
	}
	
	/**
	 * Gets the singleton IEvrFactory bean.
	 * 
	 * @return the IEvrFactory bean
	 */
    @Bean(name = EvrApiBeans.EVR_FACTORY)
    @Scope("singleton")
    @Lazy(value = true)
    public IEvrFactory getEvrFactory() {
        return new EvrFactory();
    }
    
	/**
	 * Gets the singleton IEvrMessageFactory bean.
	 * 
	 * @return the IEvrMessageFactory bean
	 */
    @Bean(name = EvrApiBeans.EVR_MESSAGE_FACTORY)
    @Scope("singleton")
    @Lazy(value = true)
    public IEvrMessageFactory getEvrMessageFactory(final MissionProperties missionProperties) {
        return new EvrMessageFactory(missionProperties);
    }

    /**
     * Gets the singleton IEvrPublisherService bean.
     * 
     * @return the IEvrPublisherService bean
     */
    @Bean(name = EvrApiBeans.EVR_PUBLISHER_SERVICE)
    @Scope("singleton")
    @Lazy(value = true)
    public IEvrPublisherService getEvrPublisherService() {
        return new EvrPublisherService(appContext);
    }
    
    /**
     * Gets the singleton IEvrNotifierService bean.
     * 
     * @return the IEvrNotifierService bean
     */
    @Bean(name = EvrApiBeans.EVR_NOTIFIER_SERVICE)
    @Scope("singleton")
    @Lazy(value = true)
    public IEvrNotifierService getEvrNotifierService() {
        return new EvrNotifierService(appContext);
    }
    
    /**
     * Gets the singleton IEvrExtractor bean.
     * 
     * @param mprops
     *            current MissionProperties bean, autowired
     * @param sseFlag
     *            current SseContextFlag bean, autowired
     * 
     * @return the IEvrExtractorFactory bean
     * @throws EvrExtractorException
     *             if there is a problem creating the extractor
     */
    @Bean(name = EvrApiBeans.EVR_EXTRACTOR)
    @Scope("singleton")
    @Lazy(value = true)
    public IEvrExtractor getEvrExtractor(final MissionProperties mprops, final SseContextFlag sseFlag)
            throws EvrExtractorException {
        /**
         * Gets either the sse or flight EVR extractor.
         */
        final boolean isSse = sseFlag.isApplicationSse();
        final boolean isJplSse = mprops.sseIsJplStyle();

        if (isSse && isJplSse) {
            return new JplSseEvrExtractor(appContext);
        } else {
            return new JplMultimissionEvrExtractor(appContext);
        }
    }
    
    /**
     * Gets the singleton IRawEvrDataFactory bean.
     * 
     * @return the IRawEvrDataFactory bean
     */
    @Bean(name = EvrApiBeans.EVR_RAW_DATA_FACTORY)
    @Scope("singleton")
    @Lazy(value = true)
    public IRawEvrDataFactory getEvrRawDataFactory() {
        return new RawEvrDataFactory();
    }
    
    /**
     * Gets the singleton IEvrExtractorUtility bean.
     * 
     * @return the IEvrExtractorFactory bean
     * @throws EvrExtractorException
     *             if there is a problem creating the utility
     */
    @Bean(name = EvrApiBeans.EVR_EXTRACTOR_UTILITY)
    @Scope("singleton")
    @Lazy(value = true)
    public IEvrExtractorUtility getEvrExtractorUtility()
            throws EvrExtractorException {
        return new EvrExtractorUtility(appContext);
    }
    
}
