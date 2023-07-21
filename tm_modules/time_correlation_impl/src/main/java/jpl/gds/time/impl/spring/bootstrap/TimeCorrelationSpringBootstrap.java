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
package jpl.gds.time.impl.spring.bootstrap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.shared.message.MessageRegistry;
import jpl.gds.shared.message.RegisteredMessageConfiguration;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import jpl.gds.time.api.TimeCorrelationApiBeans;
import jpl.gds.time.api.config.TimeCorrelationProperties;
import jpl.gds.time.api.message.ITimeCorrelationMessageFactory;
import jpl.gds.time.api.message.TimeCorrelationMessageType;
import jpl.gds.time.api.service.ITimeCorrelationParser;
import jpl.gds.time.api.service.ITimeCorrelationService;
import jpl.gds.time.impl.message.FswTimeCorrelationMessage;
import jpl.gds.time.impl.message.SseTimeCorrelationMessage;
import jpl.gds.time.impl.message.TimeCorrelationMessageFactory;
import jpl.gds.time.impl.service.MultimissionTimeCorrelationParser;
import jpl.gds.time.impl.service.MultimissionTimeCorrelationService;
import jpl.gds.time.impl.service.SseTimeCorrelationService;

/**
 * Spring bootstrap configuration class for the time_correlation projects.
 * 
 * @since R8
 */
@Configuration
public class TimeCorrelationSpringBootstrap {
    
    @Autowired ApplicationContext appContext;

    /**
     * Constructor
     */
    public TimeCorrelationSpringBootstrap() {
        MessageRegistry.registerMessageType(new RegisteredMessageConfiguration(TimeCorrelationMessageType.FswTimeCorrelation, FswTimeCorrelationMessage.XmlParseHandler.class.getName(), null));
        MessageRegistry.registerMessageType(new RegisteredMessageConfiguration(TimeCorrelationMessageType.SseTimeCorrelation, SseTimeCorrelationMessage.XmlParseHandler.class.getName(), null));
    }
    
    /**
     * Creates or gets the singleton ITimeCorrelationParser bean.
     * 
     * @return ITimeCorrelationParser bean
     */
    @Bean(name = TimeCorrelationApiBeans.TIME_CORRELATION_PARSER)
    @Scope("singleton")
    @Lazy(value = true)
    public ITimeCorrelationParser createTimeCorrelationParser() {
        return new MultimissionTimeCorrelationParser(appContext);
    }
    
    /**
     * Creates or gets the singleton ITimeCorrelationParser bean.
     * 
     * @param mprops
     *            current MissionProperties object, autowired
     * @param sseFlag
     *            current SseContextFlag object, autowired
     * 
     * @return ITimeCorrelationParser bean
     */
    @Bean(name = TimeCorrelationApiBeans.TIME_CORRELATION_SERVICE)
    @Scope("singleton")
    @Lazy(value = true)
    public ITimeCorrelationService createTimeCorrelationService(final MissionProperties mprops,
                                                                final SseContextFlag sseFlag) {
        /**
         * Gets either the sse or flight time correlation service.
         */
        final boolean isSse = sseFlag.isApplicationSse();
        final boolean isJplSse = mprops.sseIsJplStyle();

        if (isSse && isJplSse) {
            return new SseTimeCorrelationService(appContext);
        } else {
            return new MultimissionTimeCorrelationService(appContext);
        }
    }
    
    /**
     * Creates or gets the singleton ITimeCorrelationMessageFactory bean.
     * 
     * @return ITimeCorrelationMessageFactory bean
     */
    @Bean(name = TimeCorrelationApiBeans.TIME_CORRELATION_MESSAGE_FACTORY)
    @Scope("singleton")
    @Lazy(value = true)
    public ITimeCorrelationMessageFactory createTimeCorrelationMessageFactory() {
        return new TimeCorrelationMessageFactory();
    }
    
    /**
     * Creates or gets the singleton TimeCorrelationProperties bean.
     * 
     * @param sseFlag
     *            The SSE context flag
     * 
     * @return TimeCorrelationProperties bean
     */
    @Bean(name = TimeCorrelationApiBeans.TIME_CORRELATION_PROPERTIES)
    @Scope("singleton")
    @Lazy(value = true)
    public TimeCorrelationProperties createTimeCorrelationProperties(final SseContextFlag sseFlag) {
        return new TimeCorrelationProperties(sseFlag);
    }
   
}
