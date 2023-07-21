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
package jpl.gds.dictionary.impl.channel;

import jpl.gds.dictionary.api.DictionaryClassContainer;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.channel.IChannelDictionary;
import jpl.gds.dictionary.api.channel.IChannelDictionaryFactory;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.config.DictionaryType;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.spring.context.flag.SseContextFlag;

/**
 * ChannelDictionaryFactory is used to create mission-specific instances of
 * classes that implement IChannelDictionary, and to maintain a single global
 * instance of the Channel dictionary parser.
 * 
 * The Channel dictionary is used by the telemetry processing system for the
 * extraction and creation of channelized telemetry values. It identifies the
 * type and characteristics of each supported telemetry channel for a given
 * project. Every mission may have a different format for representing the
 * channel dictionary. An appropriate dictionary parser must be used in order to
 * create the mission-specific IChannelDictionary object. IChannelDictionary
 * objects should only be created via the ChannelDictionaryFactory. Direct
 * creation of a IChannelDictionary object is a violation of multi-mission
 * development standards.
 * <p>
 * This class is configured using the
 * DictionaryProperties and GdsSystemProperties objects. The system mission
 * must be set in the GdsSystemProperties object or configured on the Java
 * command line using the GdsSystemProperties.MISSION_PROPERTY System property.
 * In general, these DictionaryProperty fields are initialized to a set of
 * default values using a dictionary properties file, but are often adjusted by
 * the application using this factory.
 * <p>
 * As of version 2.0, use of dictionary parsers through this class will NOT add
 * the parsed channel definitions to the core AMPCS Channel Definition Table. The
 * caller takes responsibility for that step.
 * 
 * @version 1.0 - Initial Implementation
 * @version 1.1 - Made ADAPTER_PROPERTY public
 * @version 2.0 - Removed ADAPTER_PROPERTY and
 *          SSE_ADAPTER_PROPERTY. This class must now be configured via the
 *          DictionaryConfiguration object. Changed method names. Added methods.
 * @version 2.1 - Uses new dictionary search methods, allowing this factory to
 *          find static instances using different methods based upon configured
 *          search path and file name. Fixed some synchronization.
 *
 * @see IChannelDictionary
 * @see jpl.gds.dictionary.api.config.DictionaryProperties
 * @see jpl.gds.shared.config.GdsSystemProperties
 */
public class ChannelDictionaryFactory implements IChannelDictionaryFactory {
    private final SseContextFlag sseFlag;
    private final Tracer         log;

    /**
     * Default factory test constructor
     */
    public ChannelDictionaryFactory() {
        this(new SseContextFlag());
    }

    /**
     * Default factory test constructor
     * 
     * @param sseFlag
     *            the sse context flag
     */
    public ChannelDictionaryFactory(final SseContextFlag sseFlag) {
        this(sseFlag, TraceManager.getTracer(Loggers.DICTIONARY));
    }

    /**
     * Factory constructor with SSE context flag
     * 
     * @param sseFlag
     *            the SSE context flag
     * @param log
     *            the Tracer to log with
     */
    public ChannelDictionaryFactory(final SseContextFlag sseFlag, final Tracer log) {
        this.sseFlag = sseFlag;
        this.log = log;
    }

    @Override
    public IChannelDictionary getNewInstance(final DictionaryProperties config, final String filename)
            throws DictionaryException {
        if (sseFlag.isApplicationSse()) {
            return getNewSseInstance(config, filename);
        } else {
            return getNewFlightInstance(config, filename);
        }
    }
    
    @Override
    public IChannelDictionary getNewFlightInstance(final DictionaryProperties config, final String filename)
            throws DictionaryException {

        // Need to create a new instance of the adapter class
        try {
            final Class<? extends IChannelDictionary> klazz = getDictionaryClass(config.getDictionaryClass(DictionaryType.CHANNEL));
            final IChannelDictionary localInstance = klazz.newInstance();
            /*
             * Use the supplied configuration when
             * parsing
             */
            localInstance.parse(filename, config, log);
            return localInstance;
        } catch (final InstantiationException | IllegalAccessException e) {
            throw new DictionaryException("Cannot create Channel dictionary adaptation: " + e.toString(), e);
        }
    }

    @Override
    public IChannelDictionary getNewInstance(final DictionaryProperties config)
            throws DictionaryException {
        if (sseFlag.isApplicationSse()) {
            return getNewSseInstance(config);
        }
        return getNewInstance(config, config.findFileForSystemMission(DictionaryType.CHANNEL));
    }


    @Override
    public IChannelDictionary getNewSseInstance(final DictionaryProperties config, final String filename)
            throws DictionaryException {

        // Need to create a new instance of the adapter class
        try {
            /*
             *  Use global dictionary configuration
             * instead of the property value in the GDS Configuration.
             */
            final Class<? extends IChannelDictionary> klazz = getDictionaryClass(config.getSseDictionaryClass(DictionaryType.CHANNEL));
            final IChannelDictionary localInstance = klazz.newInstance();
            /*
             * Use the supplied configuration when
             * parsing
             */
            localInstance.parse(filename, config, log);
            return localInstance;
        } catch (final InstantiationException | IllegalAccessException e) {
            throw new DictionaryException("Cannot create SSE Channel dictionary adaptation: " + e.toString(), e);
        }
    }


    @Override
    public IChannelDictionary getNewSseInstance(final DictionaryProperties config) throws DictionaryException {
        return getNewSseInstance(config, config.findSseFileForSystemMission(DictionaryType.CHANNEL));
    }

    @Override
    public IChannelDictionary getNewMonitorInstance(final DictionaryProperties config, final String filename)
            throws DictionaryException {
        // Need to create a new instance of the adapter class
        try {
            final Class<? extends IChannelDictionary> klazz = getDictionaryClass(config.getDictionaryClass(DictionaryType.MONITOR));
            final IChannelDictionary localInstance = klazz.newInstance();
            /*
             *  Use the supplied configuration when
             * parsing
             */
            localInstance.parse(filename, config, log);
            localInstance.setDictionaryType(DictionaryType.MONITOR);
            return localInstance;
        } catch (final InstantiationException | IllegalAccessException e) {
            throw new DictionaryException("Cannot create Monitor Channel dictionary adaptation: " + e.toString(), e);
        }
    }

    @Override
    public IChannelDictionary getNewMonitorInstance(final DictionaryProperties config) throws DictionaryException {
        return getNewMonitorInstance(config, config.findFileForSystemMission(DictionaryType.MONITOR));
    }

    @Override
    public IChannelDictionary getNewHeaderInstance(final DictionaryProperties config, final String filename)
            throws DictionaryException {
        if (sseFlag.isApplicationSse()) {
            return getNewSseHeaderInstance(config, filename);
        }
    
        // Need to create a new instance of the adapter class
        try {
            final Class<? extends IChannelDictionary> klazz = getDictionaryClass(config.getDictionaryClass(DictionaryType.HEADER));
            final IChannelDictionary localInstance = klazz.newInstance();
            /* Use the supplied configuration when parsing */
            localInstance.parse(filename, config, log);
            localInstance.setDictionaryType(DictionaryType.HEADER);
            return localInstance;
        } catch (final InstantiationException | IllegalAccessException e) {
            throw new DictionaryException(
                    "Cannot create Header Channel dictionary adaptation: "
                            + e.toString(), e);
        }
    }

    @Override
    public IChannelDictionary getNewHeaderInstance(final DictionaryProperties config) throws DictionaryException {
        if (sseFlag.isApplicationSse()) {
            return getNewSseHeaderInstance(config);
        }
        return getNewHeaderInstance(config, config.findFileForSystemMission(DictionaryType.HEADER));
    }


    @Override
    public IChannelDictionary getNewSseHeaderInstance(final DictionaryProperties config, final String filename)
            throws DictionaryException {
        // Need to create a new instance of the adapter class
        try {
            final Class<? extends IChannelDictionary> klazz = getDictionaryClass(config.getSseDictionaryClass(DictionaryType.HEADER));
            final IChannelDictionary localInstance = klazz.newInstance();
            /*
             * Use the supplied configuration when
             * parsing
             */
            localInstance.parse(filename, config, log);
            localInstance.setDictionaryType(DictionaryType.HEADER);
            return localInstance;
        } catch (final InstantiationException | IllegalAccessException e) {
            throw new DictionaryException("Cannot create SSE Header Channel dictionary adaptation: " + e.toString(), e);
        }
    }

    @Override
    public IChannelDictionary getNewSseHeaderInstance(final DictionaryProperties config) throws DictionaryException {
        return getNewSseHeaderInstance(config, config.findFileForSystemMission(DictionaryType.HEADER));
    }
    
    @SuppressWarnings("unchecked")
    protected Class<? extends IChannelDictionary> getDictionaryClass(final DictionaryClassContainer typeContainer) throws DictionaryException {
        switch (typeContainer.getClassType()) {
            case CUSTOM:
                try {
                    return (Class<? extends IChannelDictionary>) Class.forName(typeContainer.getCustomClassName());
                } catch (final ClassNotFoundException e) {
                    throw new DictionaryException("Cannot find CHANNEL dictionary class " +  typeContainer.getCustomClassName() + " " + e.toString(), e);
                }
            case MM_CHANNEL:
                return MultimissionChannelDictionary.class;
            case SSE_CHANNEL:
                 return SseChannelDictionary.class;
            case OLD_MM_HEADER_CHANNEL:
                return OldHeaderChannelDictionary.class;
            case OLD_MM_MONITOR_CHANNEL:
                return OldMonitorChannelDictionary.class;
            default:
                throw new DictionaryException("Unrecognized class type for APID dictionary: " + 
                        typeContainer.getClassType());
        }
    }
}
