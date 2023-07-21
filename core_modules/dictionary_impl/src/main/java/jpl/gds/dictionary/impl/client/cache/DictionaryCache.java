/*
 * Copyright 2006-2018. California Institute of Technology.
 * ALL RIGHTS RESERVED.
 * U.S. Government sponsorship acknowledged.
 *
 * This software is subject to U. S. export control laws and
 * regulations (22 C.F.R. 120-130 and 15 C.F.R. 730-774). To the
 * extent that the software  is subject to U.S. export control laws
 * and regulations, the recipient has the responsibility to obtain
 * export licenses or other export authority as may be required
 * before exporting such information to foreign countries or
 * providing access to foreign nationals.
 */
package jpl.gds.dictionary.impl.client.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheStats;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.ICacheableDictionary;
import jpl.gds.dictionary.api.alarm.IAlarmDictionary;
import jpl.gds.dictionary.api.alarm.IAlarmDictionaryFactory;
import jpl.gds.dictionary.api.apid.IApidDictionary;
import jpl.gds.dictionary.api.apid.IApidDictionaryFactory;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.channel.IChannelDictionary;
import jpl.gds.dictionary.api.channel.IChannelDictionaryFactory;
import jpl.gds.dictionary.api.client.cache.IDictionaryCache;
import jpl.gds.dictionary.api.command.ICommandDictionary;
import jpl.gds.dictionary.api.command.ICommandDictionaryFactory;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.config.DictionaryType;
import jpl.gds.dictionary.api.decom.IChannelDecomDictionary;
import jpl.gds.dictionary.api.decom.IChannelDecomDictionaryFactory;
import jpl.gds.dictionary.api.evr.IEvrDictionary;
import jpl.gds.dictionary.api.evr.IEvrDictionaryFactory;
import jpl.gds.dictionary.api.frame.ITransferFrameDictionary;
import jpl.gds.dictionary.api.frame.ITransferFrameDictionaryFactory;
import jpl.gds.dictionary.api.sequence.ISequenceDictionary;
import jpl.gds.dictionary.api.sequence.ISequenceDictionaryFactory;
import jpl.gds.dictionary.impl.alarm.AlarmDictionaryFactory;
import jpl.gds.dictionary.impl.apid.ApidDictionaryFactory;
import jpl.gds.dictionary.impl.channel.ChannelDictionaryFactory;
import jpl.gds.dictionary.impl.command.CommandDictionaryFactory;
import jpl.gds.dictionary.impl.decom.ChannelDecomDictionaryFactory;
import jpl.gds.dictionary.impl.evr.EvrDictionaryFactory;
import jpl.gds.dictionary.impl.frame.TransferFrameDictionaryFactory;
import jpl.gds.dictionary.impl.sequence.SequenceDictionaryFactory;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.spring.context.flag.SseContextFlag;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * A cache for dictionaries so that the application does not keep reloading the
 * same dictionary.
 * <p>
 * This is an application level bean so it still uses the singleton get instance
 * paradigm.
 *
 * Use a guava cache using soft references for values. This allows cached objects to
 * stay resident until the JVM needs more memory, then they are evicted using LRU.
 *
 *
 */
@Scope("singleton")
public class DictionaryCache implements IDictionaryCache {
    private static final long                         DEFEAULT_EXPIRY_HOURS = 12L;
    private static DictionaryCache                    instance;
    private final Tracer                              tracer;
    private final SseContextFlag                      sseFlag;
    private ApplicationContext appContext;

    /**
     * Thread safe cache, built with soft references to the values and expire time
     * after last read.
     */
    private Cache<DictionaryId, ICacheableDictionary> cache;

    private DictionaryCache(final ApplicationContext ctx) {
        this.appContext = ctx;
        final DictionaryProperties props = ctx.getBean(DictionaryProperties.class);
        final long expireTime = props.getCacheEvictionTimeout();
        this.tracer = TraceManager.getTracer(ctx, Loggers.DICTIONARY);
        this.cache = createCache(expireTime);
        this.sseFlag = ctx.getBean(SseContextFlag.class);

    }


    /**
     * Get the singleton instance. This is idempotent; it will not reconfigure an
     * existing instance.
     *
     * @param ctx
     *            the current application context
     * @return the dictionary cache instance
     */
    public static synchronized DictionaryCache getInstance(final ApplicationContext ctx) {
        if (instance == null) {
            instance = new DictionaryCache(ctx);
        }

        return instance;
    }

    /**
     * Create the backing cache
     *
     * @param  expireTime
     * @return       the backing cache object
     */
    private Cache<DictionaryId, ICacheableDictionary> createCache(final long expireTime) {
        return CacheBuilder.newBuilder()
                           // soft values allows the cache to grow until memory pressure is reached, at
                           // which point
                           // entries will be evicted in an LRU manner
                           .softValues().expireAfterAccess(expireTime, TimeUnit.HOURS)
                           .removalListener((RemovalListener<DictionaryId, ICacheableDictionary>) notification -> {
                                          final DictionaryId did = notification.getKey();
                                          tracer.debug("Dictionary cache has evicted ", did.getType().getDictionaryName(), ".",
                                                       did.getVersion(), ": ", notification.getCause());
                           }).recordStats().build();
    }

    /**
     * Replace the backing cache
     *
     * @param cache
     */
    void setBackingCache(final Cache<DictionaryId, ICacheableDictionary> cache) {
        this.cache = cache;
    }

    private ICacheableDictionary getOrAdd(final String filePath, final DictionaryProperties dictConfig,
            final DictionaryType type, final boolean isSse) throws DictionaryException {
        final DictionaryId dictId = new DictionaryId(filePath, dictConfig, type, isSse);

        /**
         * Since we are using a weak map we must check that the value is not null after
         * retrieving it.
         */
        ICacheableDictionary dict = cache.getIfPresent(dictId);

        if (dict == null) {
            /**
             * There is no dictionary or the reference was removed, so create a new one and
             * add it.
             */
            dict = loadDictionary(dictConfig, dictId);

            /**
             * Not synchronizing this since we are using a concurrent map, so there should
             * be no issues. Worst case here is that it is already there due to another load
             * of the same dict, but the damage is done so whatever.
             */
            cache.put(dictId, dict);
        }

        return dict;
    }

    private ICacheableDictionary loadDictionary(final DictionaryProperties dictConfig, final DictionaryId dictId)
            throws DictionaryException {

        ICacheableDictionary dict = null;
        switch (dictId.getType()) {
        case APID:
            dict = loadApidDictionary(dictConfig, dictId);
            break;
        case EVR:
            dict = loadEvrDictionary(dictConfig, dictId);
            break;
        case COMMAND:
            dict = loadCommandDictionary(dictConfig, dictId);
            break;
        case CHANNEL:
        case HEADER:
        case MONITOR:
            dict = loadChannelDictionary(dictConfig, dictId);
            break;
        case FRAME:
            dict = loadTransferFrameDictionary(dictConfig, dictId);
            break;
        case SEQUENCE:
            dict = loadSequenceDictionary(dictConfig, dictId);
            break;
        case ALARM:
        case DECOM:
            throw new DictionaryException(
                    "Dictionary cannot be loaded in the general load method: " + dictId.getType());
        case PRODUCT:
        case MAPPER:
            // Not supported for caching because it is not necessary.
        default:
            throw new DictionaryException("Dictionary type not supported for caching yet");
        }

        return dict;
    }

    private ICacheableDictionary getOrAddAlarmOrDecomDictionary(final DictionaryProperties dictConfig,
            final Map<String, IChannelDefinition> channelIdMapping, final boolean isSse, final DictionaryType type,
            final String file) throws DictionaryException {
        final DictionaryId dictId = new DictionaryId(file, dictConfig, type, isSse);

        /**
         * Since we are using a weak map we must check that the value is not null after
         * retrieving it.
         */
        ICacheableDictionary dict = cache.getIfPresent(dictId);

        if (dict == null) {
            /**
             * There is no dictionary or the reference was removed, so create a new one and
             * add it.
             */
            switch (type) {
            case ALARM:
                dict = loadAlarmDictionary(dictId, dictConfig, channelIdMapping);
                break;
            case DECOM:
                dict = loadDecomDictionary(dictId, dictConfig, channelIdMapping);
                break;
            default:
                throw new DictionaryException("Invalid dictionary type for load method: " + type);
            }

            /**
             * Not synchronizing this since we are using a concurrent map, so there should
             * be no issues. Worst case here is that it is already there due to another load
             * of the same dict, but the damage is done so whatever.
             */
            cache.put(dictId, dict);
        }

        return dict;
    }

    private IAlarmDictionary loadAlarmDictionary(final DictionaryId dictId, final DictionaryProperties dictConfig,
            final Map<String, IChannelDefinition> channelIdMapping) throws DictionaryException {
        final IAlarmDictionaryFactory factory = appContext.getBean(IAlarmDictionaryFactory.class);
        if (dictId.isSse()) {
            return factory.getNewSseInstance(dictConfig, dictId.getFilePath(), channelIdMapping);
        } else {
            return factory.getNewFlightInstance(dictConfig, dictId.getFilePath(), channelIdMapping);
        }
    }

    private ICommandDictionary loadCommandDictionary(final DictionaryProperties dictConfig, final DictionaryId dictId)
            throws DictionaryException {
        final ICommandDictionaryFactory factory = appContext.getBean(ICommandDictionaryFactory.class);
        return factory.getNewInstance(dictConfig, dictId.getFilePath());
    }

    private IChannelDecomDictionary loadDecomDictionary(final DictionaryId dictId,
            final DictionaryProperties dictConfig, final Map<String, IChannelDefinition> channelIdMapping)
            throws DictionaryException {
        final IChannelDecomDictionaryFactory factory = appContext.getBean(IChannelDecomDictionaryFactory.class);

        if (dictId.isSse()) {
            return factory.getNewSseInstance(dictConfig, dictId.getFilePath(), channelIdMapping);
        } else {
            return factory.getNewFlightInstance(dictConfig, dictId.getFilePath(), channelIdMapping);
        }
    }

    private ITransferFrameDictionary loadTransferFrameDictionary(final DictionaryProperties dictConfig,
            final DictionaryId dictId) throws DictionaryException {
        final ITransferFrameDictionaryFactory factory = appContext.getBean(ITransferFrameDictionaryFactory.class);
        return factory.getNewInstance(dictConfig, dictId.getFilePath());
    }

    private ISequenceDictionary loadSequenceDictionary(final DictionaryProperties dictConfig, final DictionaryId dictId)
            throws DictionaryException {
        final ISequenceDictionaryFactory factory = appContext.getBean(ISequenceDictionaryFactory.class);
        return factory.getNewInstance(dictConfig, dictId.getFilePath());
    }

    /**
     * @param  dictConfig
     * @param  dictId
     * @return
     * @throws DictionaryException
     */
    private IApidDictionary loadApidDictionary(final DictionaryProperties dictConfig, final DictionaryId dictId)
            throws DictionaryException {
        final IApidDictionaryFactory factory = appContext.getBean(IApidDictionaryFactory.class);

        if (dictId.isSse()) {
            return factory.getNewSseInstance(dictConfig, dictId.getFilePath());
        } else {
            return factory.getNewFlightInstance(dictConfig, dictId.getFilePath());
        }
    }

    /**
     * @param  dictConfig
     * @param  dictId
     * @return
     * @throws DictionaryException
     */
    private IEvrDictionary loadEvrDictionary(final DictionaryProperties dictConfig, final DictionaryId dictId)
            throws DictionaryException {
        final IEvrDictionaryFactory factory = appContext.getBean(IEvrDictionaryFactory.class);

        if (dictId.isSse()) {
            return factory.getNewSseInstance(dictConfig, dictId.getFilePath());
        } else {
            return factory.getNewFlightInstance(dictConfig, dictId.getFilePath());
        }
    }

    private IChannelDictionary loadChannelDictionary(final DictionaryProperties dictConfig, final DictionaryId dictId)
            throws DictionaryException {
        final IChannelDictionaryFactory factory = appContext.getBean(IChannelDictionaryFactory.class);
        final boolean isSse = dictId.isSse();

        switch (dictId.getType()) {
        case HEADER:
                return isSse ? factory.getNewSseHeaderInstance(dictConfig, dictId.getFilePath())
                        : factory.getNewHeaderInstance(dictConfig, dictId.getFilePath());
        case CHANNEL:
                return isSse ? factory.getNewSseInstance(dictConfig, dictId.getFilePath())
                        : factory.getNewFlightInstance(dictConfig, dictId.getFilePath());
        case MONITOR:
                return factory.getNewMonitorInstance(dictConfig, dictId.getFilePath());
        default:
            throw new DictionaryException("Invalid dictionary type for channel dictionary: " + dictId.getType());
        }
    }

    /**
     * {@inheritDoc}
     *
     * @see jpl.gds.dictionary.api.client.cache.IDictionaryCache#getFlightChannelDictionary(jpl.gds.dictionary.api.config.DictionaryProperties)
     */
    @Override
    public IChannelDictionary getFlightChannelDictionary(final DictionaryProperties dictConfig)
            throws DictionaryException {
        return getFlightChannelDictionary(dictConfig, dictConfig.findFileForSystemMission(DictionaryType.CHANNEL));
    }

    /**
     * {@inheritDoc}
     *
     * @see jpl.gds.dictionary.api.client.cache.IDictionaryCache#getFlightChannelDictionary(jpl.gds.dictionary.api.config.DictionaryProperties,
     *      java.lang.String)
     */
    @Override
    public IChannelDictionary getFlightChannelDictionary(final DictionaryProperties dictConfig, final String filePath)
            throws DictionaryException {
        return (IChannelDictionary) getOrAdd(filePath, dictConfig, DictionaryType.CHANNEL, false);
    }

    /**
     * {@inheritDoc}
     *
     * @see jpl.gds.dictionary.api.client.cache.IDictionaryCache#getSseChannelDictionary(jpl.gds.dictionary.api.config.DictionaryProperties)
     */
    @Override
    public IChannelDictionary getSseChannelDictionary(final DictionaryProperties dictConfig)
            throws DictionaryException {
        return getSseChannelDictionary(dictConfig, dictConfig.findSseFileForSystemMission(DictionaryType.CHANNEL));
    }

    @Override
    public IChannelDictionary getSseChannelDictionary(final DictionaryProperties dictConfig, final String filePath)
            throws DictionaryException {
        return (IChannelDictionary) getOrAdd(filePath, dictConfig, DictionaryType.CHANNEL, true);
    }

    /**
     * {@inheritDoc}
     *
     * @see jpl.gds.dictionary.api.client.cache.IDictionaryCache#getHeaderChannelDictionary(jpl.gds.dictionary.api.config.DictionaryProperties)
     */
    @Override
    public IChannelDictionary getHeaderChannelDictionary(final DictionaryProperties dictConfig)
            throws DictionaryException {
        return getHeaderChannelDictionary(dictConfig, dictConfig.findFileForSystemMission(DictionaryType.HEADER));
    }

    /**
     * {@inheritDoc}
     *
     * @see jpl.gds.dictionary.api.client.cache.IDictionaryCache#getHeaderChannelDictionary(jpl.gds.dictionary.api.config.DictionaryProperties)
     */
    @Override
    public IChannelDictionary getHeaderChannelDictionary(final DictionaryProperties dictConfig, final String filePath)
            throws DictionaryException {
        return (IChannelDictionary) getOrAdd(filePath, dictConfig, DictionaryType.HEADER, false);
    }

    /**
     * {@inheritDoc}
     *
     * @see jpl.gds.dictionary.api.client.cache.IDictionaryCache#getSseHeaderChannelDictionary(jpl.gds.dictionary.api.config.DictionaryProperties)
     */
    @Override
    public IChannelDictionary getSseHeaderChannelDictionary(final DictionaryProperties dictConfig)
            throws DictionaryException {
        return getSseHeaderChannelDictionary(dictConfig, dictConfig.findSseFileForSystemMission(DictionaryType.HEADER));
    }

    @Override
    public IChannelDictionary getSseHeaderChannelDictionary(final DictionaryProperties dictConfig,
            final String filePath) throws DictionaryException {
        return (IChannelDictionary) getOrAdd(filePath, dictConfig, DictionaryType.HEADER, true);
    }

    /**
     * {@inheritDoc}
     *
     * @see jpl.gds.dictionary.api.client.cache.IDictionaryCache#getMonitorDictionary(jpl.gds.dictionary.api.config.DictionaryProperties)
     */
    @Override
    public IChannelDictionary getMonitorDictionary(final DictionaryProperties dictConfig) throws DictionaryException {
        return getMonitorDictionary(dictConfig, dictConfig.findFileForSystemMission(DictionaryType.MONITOR));
    }

    @Override
    public IChannelDictionary getMonitorDictionary(final DictionaryProperties dictConfig, final String filePath)
            throws DictionaryException {
        return (IChannelDictionary) getOrAdd(filePath, dictConfig, DictionaryType.MONITOR, false);
    }

    /**
     * {@inheritDoc}
     *
     * @see jpl.gds.dictionary.api.client.cache.IDictionaryCache#getApidDictionary(jpl.gds.dictionary.api.config.DictionaryProperties,
     *      boolean)
     */
    @Override
    public IApidDictionary getApidDictionary(final DictionaryProperties dictConfig, final boolean isSse)
            throws DictionaryException {
        return getApidDictionary(dictConfig, isSse, isSse ? dictConfig.findSseFileForSystemMission(DictionaryType.APID)
                : dictConfig.findFileForSystemMission(DictionaryType.APID));
    }

    @Override
    public IApidDictionary getApidDictionary(final DictionaryProperties dictConfig, final boolean isSse,
            final String filePath) throws DictionaryException {
        return (IApidDictionary) getOrAdd(filePath, dictConfig, DictionaryType.APID, isSse);
    }

    /**
     * {@inheritDoc}
     *
     * @see jpl.gds.dictionary.api.client.cache.IDictionaryCache#getEvrDictionary(jpl.gds.dictionary.api.config.DictionaryProperties,
     *      boolean)
     */
    @Override
    public IEvrDictionary getEvrDictionary(final DictionaryProperties dictConfig, final boolean isSse)
            throws DictionaryException {
        return getEvrDictionary(dictConfig, isSse, isSse ? dictConfig.findSseFileForSystemMission(DictionaryType.EVR)
                : dictConfig.findFileForSystemMission(DictionaryType.EVR));
    }

    @Override
    public IEvrDictionary getEvrDictionary(final DictionaryProperties dictConfig, final boolean isSse,
            final String filePath) throws DictionaryException {
        return (IEvrDictionary) getOrAdd(filePath, dictConfig, DictionaryType.EVR, isSse);
    }

    /**
     * {@inheritDoc}
     *
     * @see jpl.gds.dictionary.api.client.cache.IDictionaryCache#getAlarmDictionary(jpl.gds.dictionary.api.config.DictionaryProperties,
     *      java.util.Map, boolean)
     */
    @Override
    public IAlarmDictionary getAlarmDictionary(final DictionaryProperties dictConfig,
            final Map<String, IChannelDefinition> channelIdMapping, final boolean isSse) throws DictionaryException {
        return getAlarmDictionary(dictConfig, channelIdMapping,
                isSse ? dictConfig.findSseFileForSystemMission(DictionaryType.ALARM)
                        : dictConfig.findFileForSystemMission(DictionaryType.ALARM),
                isSse);
    }

    /**
     * {@inheritDoc}
     *
     * @see jpl.gds.dictionary.api.client.cache.IDictionaryCache#getAlarmDictionary(jpl.gds.dictionary.api.config.DictionaryProperties,
     *      java.util.Map, boolean)
     */
    @Override
    public IAlarmDictionary reloadAlarmDictionary(final DictionaryProperties dictConfig,
                                               final Map<String, IChannelDefinition> channelIdMapping, final boolean isSse) throws DictionaryException {

        return reloadAlarmDictionary(dictConfig, channelIdMapping,
                isSse ? dictConfig.findSseFileForSystemMission(DictionaryType.ALARM) : dictConfig
                        .findFileForSystemMission(DictionaryType.ALARM), isSse);
    }

    @Override
    public IAlarmDictionary getAlarmDictionary(final DictionaryProperties dictConfig,
            final Map<String, IChannelDefinition> channelIdMapping, final String alarmFile, final boolean isSse)
            throws DictionaryException {
        return (IAlarmDictionary) getOrAddAlarmOrDecomDictionary(dictConfig, channelIdMapping, isSse,
                DictionaryType.ALARM, alarmFile);
    }

    @Override
    public IAlarmDictionary reloadAlarmDictionary(DictionaryProperties dictConfig,
                                                  Map<String, IChannelDefinition> channelIdMapping, String alarmFile,
                                                  boolean isSse) throws DictionaryException {

        final DictionaryId dictId = new DictionaryId(alarmFile, dictConfig, DictionaryType.ALARM, isSse);

        /**
         * Since we are using a weak map we must check that the value is not null after
         * retrieving it.
         */
        cache.invalidate(dictId);
        return getAlarmDictionary(dictConfig, channelIdMapping, alarmFile, isSse);
    }

    /**
     * {@inheritDoc}
     *
     * @see jpl.gds.dictionary.api.client.cache.IDictionaryCache#getChannelDecomDictionary(jpl.gds.dictionary.api.config.DictionaryProperties,
     *      java.util.Map, boolean)
     */
    @Override
    public IChannelDecomDictionary getChannelDecomDictionary(final DictionaryProperties dictConfig,
            final Map<String, IChannelDefinition> channelIdMapping, final boolean isSse) throws DictionaryException {
        return getChannelDecomDictionary(dictConfig, channelIdMapping, isSse,
                isSse ? dictConfig.findSseFileForSystemMission(DictionaryType.DECOM)
                        : dictConfig.findFileForSystemMission(DictionaryType.DECOM));
    }

    @Override
    public IChannelDecomDictionary getChannelDecomDictionary(final DictionaryProperties dictConfig,
            final Map<String, IChannelDefinition> channelIdMapping, final boolean isSse, final String filePath)
            throws DictionaryException {
        return (IChannelDecomDictionary) getOrAddAlarmOrDecomDictionary(dictConfig, channelIdMapping, isSse,
                DictionaryType.DECOM, filePath);
    }

    /**
     * {@inheritDoc}
     *
     * @see jpl.gds.dictionary.api.client.cache.IDictionaryCache#getTransferFrameDictionary(jpl.gds.dictionary.api.config.DictionaryProperties)
     */
    @Override
    public ITransferFrameDictionary getTransferFrameDictionary(final DictionaryProperties dictConfig)
            throws DictionaryException {
        return getTransferFrameDictionary(dictConfig, dictConfig.findFileForSystemMission(DictionaryType.FRAME));
    }

    @Override
    public ITransferFrameDictionary getTransferFrameDictionary(final DictionaryProperties dictConfig,
            final String filePath) throws DictionaryException {
        return (ITransferFrameDictionary) getOrAdd(filePath, dictConfig, DictionaryType.FRAME, false);
    }

    /**
     * {@inheritDoc}
     *
     * @see jpl.gds.dictionary.api.client.cache.IDictionaryCache#getSequenceDictionary(jpl.gds.dictionary.api.config.DictionaryProperties)
     */
    @Override
    public ISequenceDictionary getSequenceDictionary(final DictionaryProperties dictConfig) throws DictionaryException {
        return getSequenceDictionary(dictConfig, dictConfig.findFileForSystemMission(DictionaryType.SEQUENCE));
    }

    @Override
    public ISequenceDictionary getSequenceDictionary(final DictionaryProperties dictConfig, final String filePath)
            throws DictionaryException {
        return (ISequenceDictionary) getOrAdd(filePath, dictConfig, DictionaryType.SEQUENCE, false);
    }

    /**
     * {@inheritDoc}
     *
     * @see jpl.gds.dictionary.api.client.cache.IDictionaryCache#getCommandDictionary(jpl.gds.dictionary.api.config.DictionaryProperties)
     */
    @Override
    public ICommandDictionary getCommandDictionary(final DictionaryProperties dictConfig) throws DictionaryException {
        return getCommandDictionary(dictConfig, dictConfig.findFileForSystemMission(DictionaryType.COMMAND));
    }

    @Override
    public ICommandDictionary getCommandDictionary(final DictionaryProperties dictConfig, final String filePath)
            throws DictionaryException {
        return (ICommandDictionary) getOrAdd(filePath, dictConfig, DictionaryType.COMMAND, false);
    }

    @Override
    public void clearCache() {
        this.cache.invalidateAll();
    }

    @Override
    public void printStats() {
        tracer.info(getFormattedStats());
    }

    /**
     * Return a statistics report for the cache.
     *
     * @return a statistics string
     */
    public String getFormattedStats() {
        final CacheStats stats = this.cache.stats();
        final StringBuilder sb = new StringBuilder("CacheStats{");

        sb.append("hitCount=");
        sb.append(stats.hitCount());
        sb.append(", missCount=");
        sb.append(stats.missCount());
        sb.append(", loadSuccessCount=");
        sb.append(stats.loadSuccessCount());
        sb.append(", loadExceptionCount=");
        sb.append(stats.loadExceptionCount());
        sb.append(", loadExceptionRate=");
        sb.append(stats.loadExceptionRate());
        sb.append(", averageLoadPenalty=");
        sb.append(stats.averageLoadPenalty());
        sb.append(", totalLoadTime=");
        sb.append(stats.totalLoadTime());
        sb.append(", evictionCount=");
        sb.append(stats.evictionCount());
        sb.append(", hitRate=");
        sb.append(stats.hitRate());
        sb.append(", missRate=");
        sb.append(stats.missRate());
        sb.append(", requestCount=");
        sb.append(stats.requestCount());

        sb.append("}");

        return sb.toString();
    }


}
