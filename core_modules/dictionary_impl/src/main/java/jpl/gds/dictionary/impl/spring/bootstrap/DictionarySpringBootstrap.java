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
package jpl.gds.dictionary.impl.spring.bootstrap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;

import jpl.gds.dictionary.api.alarm.IAlarmDictionaryFactory;
import jpl.gds.dictionary.api.apid.IApidDictionaryFactory;
import jpl.gds.dictionary.api.channel.IChannelDictionaryFactory;
import jpl.gds.dictionary.api.client.FlightDictionaryLoadingStrategy;
import jpl.gds.dictionary.api.client.SseDictionaryLoadingStrategy;
import jpl.gds.dictionary.api.client.cache.IDictionaryCache;
import jpl.gds.dictionary.api.command.ICommandDictionaryFactory;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.decom.IChannelDecomDictionaryFactory;
import jpl.gds.dictionary.api.evr.IEvrDictionaryFactory;
import jpl.gds.dictionary.api.frame.ITransferFrameDictionaryFactory;
import jpl.gds.dictionary.api.sequence.ISequenceDictionaryFactory;
import jpl.gds.dictionary.impl.alarm.AlarmDictionaryFactory;
import jpl.gds.dictionary.impl.apid.ApidDictionaryFactory;
import jpl.gds.dictionary.impl.channel.ChannelDictionaryFactory;
import jpl.gds.dictionary.impl.client.cache.DictionaryCache;
import jpl.gds.dictionary.impl.command.CommandDictionaryFactory;
import jpl.gds.dictionary.impl.decom.ChannelDecomDictionaryFactory;
import jpl.gds.dictionary.impl.evr.EvrDictionaryFactory;
import jpl.gds.dictionary.impl.frame.TransferFrameDictionaryFactory;
import jpl.gds.dictionary.impl.sequence.SequenceDictionaryFactory;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.spring.context.flag.SseContextFlag;

/**
 * Spring bootstrap configuration class for the dictionary modules.
 * 
 *
 * @since  R8
 *
 */
@Configuration
public class DictionarySpringBootstrap {
    /** SSE dictionary loading strategy name */
    public static final String SSE_DICTIONARY_LOAD_STRATEGY    = "SSE_DICTIONARY_LOAD_STRATEGY";
    /** Flight dictionary loading strategy name */
    public static final String FLIGHT_DICTIONARY_LOAD_STRATEGY = "FLIGH_DICTIONARY_LOAD_STRATEG";
    /** EVR dictionary factory bean name */
    public static final String EVR_DICTIONARY_FACTORY          = "EVR_DICTIONARY_FACTORY";
    /** Alarm dictionary factory bean name */
    public static final String ALARM_DICTIONARY_FACTORY        = "ALARM_DICTIONARY_FACTORY";
    /** Apid dictionary factory bean name */
    public static final String APID_DICTIONARY_FACTORY         = "APID_DICTIONARY_FACTORY";
    /** Channel dictionary factory bean name */
    public static final String CHANNEL_DICTIONARY_FACTORY      = "CHANNEL_DICTIONARY_FACTORY";
    /** Command dictionary factory bean name */
    public static final String COMMAND_DICTIONARY_FACTORY      = "COMMAND_DICTIONARY_FACTORY";
    /** Decom dictionary factory bean name */
    public static final String DECOM_DICTIONARY_FACTORY        = "DECOM_DICTIONARY_FACTORY";
    /** Frame dictionary factory bean name */
    public static final String FRAME_DICTIONARY_FACTORY        = "FRAME_DICTIONARY_FACTORY";
    /** Sequence dictionary factory bean name */
    public static final String SEQUENCE_DICTIONARY_FACTORY     = "SEQUENCE_DICTIONARY_FACTORY";
    /** EVR dictionary factory bean name */

    /**
     * Bean name for DictionaryProperties bean.
     */
    public static final String DICTIONARY_PROPERTIES           = "DICTIONARY_PROPERTIES";
    /**
     * Bean name for Dictionary Cache
     */
    public static final String DICTIONARY_CACHE                = "DICTIONARY_CACHE";

    /**
     * @param  appContext The current application Context
     * @return            IDictionaryCache instance
     */
    @Bean(name = DICTIONARY_CACHE)
    @Scope("singleton")
    @Lazy(value = true)
    public IDictionaryCache getDictionaryCache(final ApplicationContext appContext) {
        return DictionaryCache.getInstance(appContext);
    }

    /**
     * Gets the singleton DictionaryConfiguration bean.
     * 
     * @param sseFlag
     *            The SSE context flag
     * 
     * @return DictionaryConfiguration bean
     */
    @Bean(name = DICTIONARY_PROPERTIES)
    @Scope("singleton")
    @Lazy(value = true)
    public DictionaryProperties getDictionaryProperties(final SseContextFlag sseFlag) {
        return new DictionaryProperties(true, sseFlag);
    }

    /**
     * Gets the singleton SSE dictionary loading strategy bean.
     * 
     * @return loading strategy bean
     */
    @Bean(name = SSE_DICTIONARY_LOAD_STRATEGY)
    @Scope("singleton")
    @Lazy(value = true)
    public SseDictionaryLoadingStrategy getSseDictionaryLoadingStrategy() {
        return new SseDictionaryLoadingStrategy();
    }

    /**
     * Gets the flight dictionary loading strategy bean.
     * 
     * @return loading strategy bean
     */
    @Bean(name = FLIGHT_DICTIONARY_LOAD_STRATEGY)
    @Scope("singleton")
    @Lazy(value = true)
    public FlightDictionaryLoadingStrategy getFlightDictionaryLoadingStrategy() {
        return new FlightDictionaryLoadingStrategy();
    }

    /**
     * Gets the singleton EVR dictionary factory bean.
     * 
     * @param sseFlag
     *            The SSE context flag
     * @param appContext
     *            the application context
     * 
     * @return factory bean
     */
    @Bean(name = EVR_DICTIONARY_FACTORY)
    @Scope("singleton")
    @Lazy(value = true)
    public IEvrDictionaryFactory getEvrDictionaryFactory(final SseContextFlag sseFlag,
                                                         final ApplicationContext appContext) {
        return new EvrDictionaryFactory(sseFlag, TraceManager.getTracer(appContext, Loggers.DICTIONARY));
    }

    /**
     * Gets the singleton alarm dictionary factory bean.
     * 
     * @param sseFlag
     *            The SSE context flag
     * @param appContext
     *            The application context
     * 
     * @return factory bean
     */
    @Bean(name = ALARM_DICTIONARY_FACTORY)
    @Scope("singleton")
    @Lazy(value = true)
    public IAlarmDictionaryFactory getAlarmDictionaryFactory(final SseContextFlag sseFlag,
                                                             final ApplicationContext appContext) {
        return new AlarmDictionaryFactory(sseFlag, TraceManager.getTracer(appContext, Loggers.DICTIONARY));
    }

    /**
     * Gets the singleton APID dictionary factory bean.
     * 
     * @param sseFlag
     *            the current SSE Context flag
     * @param appContext
     *            the application context
     * 
     * @return factory bean
     */
    @Bean(name = APID_DICTIONARY_FACTORY)
    @Scope("singleton")
    @Lazy(value = true)
    public IApidDictionaryFactory getApidDictionaryFactory(final SseContextFlag sseFlag,
                                                           final ApplicationContext appContext) {
        return new ApidDictionaryFactory(sseFlag, TraceManager.getTracer(appContext, Loggers.DICTIONARY));
    }

    /**
     * Gets the singleton channel dictionary factory bean.
     * 
     * @param sseFlag
     *            the current SSE Context flag
     * @param appContext
     *            the application context
     * @return factory bean
     */
    @Bean(name = CHANNEL_DICTIONARY_FACTORY)
    @Scope("singleton")
    @Lazy(value = true)
    public IChannelDictionaryFactory getChannelDictionaryFactory(final SseContextFlag sseFlag,
                                                                 final ApplicationContext appContext) {
        return new ChannelDictionaryFactory(sseFlag, TraceManager.getTracer(appContext, Loggers.DICTIONARY));
    }

    /**
     * Gets the singleton command dictionary factory bean.
     * 
     * @param sseFlag
     *            the current SSE Context flag
     * @param appContext
     *            the application context
     * @return factory bean
     */
    @Bean(name = COMMAND_DICTIONARY_FACTORY)
    @Scope("singleton")
    @Lazy(value = true)
    public ICommandDictionaryFactory getCommandDictionaryFactory(final SseContextFlag sseFlag,
                                                                 final ApplicationContext appContext) {
        return new CommandDictionaryFactory(sseFlag, TraceManager.getTracer(appContext, Loggers.DICTIONARY));
    }

    /**
     * Gets the singleton decom dictionary factory bean.
     * 
     * @param sseFlag
     *            the current SSE Context flag
     * @param appContext
     *            the application context
     * @return factory bean
     */
    @Bean(name = DECOM_DICTIONARY_FACTORY)
    @Scope("singleton")
    @Lazy(value = true)
    public IChannelDecomDictionaryFactory getDecomDictionaryFactory(final SseContextFlag sseFlag,
                                                                    final ApplicationContext appContext) {
        return new ChannelDecomDictionaryFactory(sseFlag, TraceManager.getTracer(appContext, Loggers.DICTIONARY));
    }

    /**
     * Gets the singleton frame dictionary factory bean.
     * 
     * @param sseFlag
     *            the current SSE Context flag
     * @param appContext
     *            the application context
     * @return factory bean
     */
    @Bean(name = FRAME_DICTIONARY_FACTORY)
    @Scope("singleton")
    @Lazy(value = true)
    public ITransferFrameDictionaryFactory getFrameDictionaryFactory(final SseContextFlag sseFlag,
                                                                     final ApplicationContext appContext) {
        return new TransferFrameDictionaryFactory(sseFlag, TraceManager.getTracer(appContext, Loggers.DICTIONARY));
    }

    /**
     * Gets the singleton sequence dictionary factory bean.
     * 
     * @param sseFlag
     *            the current SSE Context flag
     * @param appContext
     *            the application context
     * @return factory bean
     */
    @Bean(name = SEQUENCE_DICTIONARY_FACTORY)
    @Scope("singleton")
    @Lazy(value = true)
    public ISequenceDictionaryFactory getSequenceDictionaryFactory(final SseContextFlag sseFlag,
                                                                   final ApplicationContext appContext) {
        return new SequenceDictionaryFactory(sseFlag, TraceManager.getTracer(appContext, Loggers.DICTIONARY));
    }
}
