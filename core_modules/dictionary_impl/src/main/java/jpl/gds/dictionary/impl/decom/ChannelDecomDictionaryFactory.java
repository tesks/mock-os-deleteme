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
package jpl.gds.dictionary.impl.decom;

import java.util.Map;

import jpl.gds.dictionary.api.DictionaryClassContainer;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.config.DictionaryType;
import jpl.gds.dictionary.api.decom.IChannelDecomDictionary;
import jpl.gds.dictionary.api.decom.IChannelDecomDictionaryFactory;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.spring.context.flag.SseContextFlag;

/**
 * DecomMapDictionaryFactory is used to create mission-specific instances of
 * classes that implement IChannelDecomDictionary, and to maintain a single
 * global instance of the generic decom map definitions.
 * <p>
 * The Channel Decom dictionary is used by the telemetry processing system for
 * the extraction of channelized telemetry directly from packets utilizing a
 * decommutation map. It actually consists of two files: an APID map, defining
 * which decom map to use for each packet APID, and a set of decom map files.
 * Under most circumstances, this factory starts by loading the APID map file,
 * and then subsequently loads the referenced decom map files. Every mission may
 * have a different format for representing the decom dictionary. An appropriate
 * dictionary parser must be used in order to create the mission-specific
 * IChannelDecomDictionary object. IChannelDecomDictionary objects should only
 * be created via the ChannelDecomDictionaryFactory. Direct creation of an
 * IChannelDecomDictionary object is a violation of multi-mission development
 * standards.
 * <p>
 * This class is configured using the
 * DictionaryProperties and GdsSystemProperties objects. The system mission
 * must be set in the GdsSystemProperties object or configured on the Java
 * command line using the GdsSystemProperties.MISSION_PROPERTY System property.
 * In general, these DictionaryProperties fields are initialized to a set of
 * default values using a dictionary properties file, but are often adjusted by
 * the application using this factory.
 * 
 *
 */
public class ChannelDecomDictionaryFactory implements IChannelDecomDictionaryFactory {
    private final SseContextFlag sseFlag;
    private final Tracer         log;

    /**
     * Default factory test constructor
     */
    public ChannelDecomDictionaryFactory() {
        this(new SseContextFlag());
    }

    /**
     * Default factory test constructor
     * 
     * @param sseFlag
     *            the sse context flag
     */
    public ChannelDecomDictionaryFactory(final SseContextFlag sseFlag) {
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
    public ChannelDecomDictionaryFactory(final SseContextFlag sseFlag, final Tracer log) {
        this.sseFlag = sseFlag;
        this.log = log;
    }

    @Override
    public IChannelDecomDictionary getEmptyInstance(final DictionaryProperties dictConfig) throws DictionaryException {
        if (dictConfig == null) {
            throw new IllegalArgumentException("dictionary config may not be null");
        }
        
        if (sseFlag.isApplicationSse()) {
            return getEmptySseInstance(dictConfig);
        }
        
        final Class<? extends IChannelDecomDictionary> klazz = getDictionaryClass(dictConfig.getDictionaryClass(DictionaryType.DECOM));
        try {
            final IChannelDecomDictionary dict = klazz.newInstance();
            dict.setDictionaryConfiguration(dictConfig);
            return dict;
        } catch (final InstantiationException | IllegalAccessException e) {
            throw new DictionaryException("Cannot create channel decom dictionary adaptation: " + e.toString(), e);
        }
    }
    
    @Override
    public IChannelDecomDictionary getEmptySseInstance(final DictionaryProperties dictConfig) throws DictionaryException {
   
        if (dictConfig == null) {
            throw new IllegalArgumentException("dictionary config may not be null");
        }
        final Class<? extends IChannelDecomDictionary> klazz = getDictionaryClass(dictConfig.getSseDictionaryClass(DictionaryType.DECOM));
        try {
        	final IChannelDecomDictionary dict = klazz.newInstance();
        	dict.setDictionaryConfiguration(dictConfig);
        	return dict;
        } catch (final InstantiationException | IllegalAccessException e) {
            throw new DictionaryException("Cannot create channel decom dictionary adaptation: " + e.toString(), e);
        }
    }

    
    @Override
    public IChannelDecomDictionary getNewInstance(final DictionaryProperties config, final String decomFile,
                                                  final Map<String, IChannelDefinition> chanMap)
            throws DictionaryException {

        if (chanMap == null) {
            throw new IllegalArgumentException("Channel map cannot be null");
        }

        if (config == null) {
            throw new IllegalArgumentException("Dictionary configuration cannot be null");
        }

        if (sseFlag.isApplicationSse()) {
            return getNewSseInstance(config, decomFile, chanMap);
        } else {
            return getNewFlightInstance(config, decomFile, chanMap);
        }
    }

    @Override
    public IChannelDecomDictionary getNewFlightInstance(final DictionaryProperties config,
                                                        final String decomFile,
                                                        final Map<String, IChannelDefinition> chanMap)
            throws DictionaryException {


        // Need to create a new instance of the adapter class
        try {
            final Class<? extends IChannelDecomDictionary> klazz = getDictionaryClass(config.getDictionaryClass(DictionaryType.DECOM));
            final IChannelDecomDictionary localInstance =  klazz.newInstance();
            localInstance.setChannelMap(chanMap);
            /*
             *  Use the supplied configuration when
             * parsing
             */
            if (decomFile != null) {
                localInstance.parse(decomFile, config, log);
            }
            return localInstance;
        } catch (final InstantiationException | IllegalAccessException e) {
            throw new DictionaryException("Cannot create Channel Decom dictionary adaptation: " + e.toString(), e);
        }

    }

    @Override
    public IChannelDecomDictionary getNewInstance(final DictionaryProperties config,
                                                  final Map<String, IChannelDefinition> chanMap)
            throws DictionaryException {
    	
        try {
            final String decomFile = config.findFileForSystemMission(DictionaryType.DECOM);
            return getNewInstance(config, decomFile, chanMap);
        } catch (final DictionaryException e) {
            return getNewInstance(config, null, chanMap);
        }

    }
    

    @Override
    public IChannelDecomDictionary getNewSseInstance(
            final DictionaryProperties config, final Map<String, IChannelDefinition> chanMap) 
                    throws DictionaryException {
        
        return getNewSseInstance(config, config.findSseFileForSystemMission(DictionaryType.DECOM), chanMap);
    }

    @Override
    public IChannelDecomDictionary getNewSseInstance(final DictionaryProperties config,
            final String decomFile, final Map<String, IChannelDefinition> chanMap)
            throws DictionaryException {
        if (chanMap == null) {
            throw new IllegalArgumentException("Channel map cannot be null");
        }

        if (config == null) {
            throw new IllegalArgumentException("Dictionary configuration cannot be null");
        }
        
        // Need to create a new instance of the adapter class
        try {
            final Class<? extends IChannelDecomDictionary> klazz = getDictionaryClass(config.getSseDictionaryClass(DictionaryType.DECOM));
            final IChannelDecomDictionary sseInstance = klazz.newInstance();
            sseInstance.setChannelMap(chanMap);
            /*  Use the supplied configuration when parsing */
            if (decomFile != null) {
                sseInstance.parse(decomFile, config, log);
            }
            return sseInstance;
        } catch (final InstantiationException | IllegalAccessException e) {
            throw new DictionaryException(
                    "Cannot create SSE Channel Decom dictionary adaptation: "
                            + e.toString(), e);
        }
    }
    
    @SuppressWarnings("unchecked")
    private Class<? extends IChannelDecomDictionary> getDictionaryClass(final DictionaryClassContainer typeContainer) throws DictionaryException {
        switch (typeContainer.getClassType()) {
            case CUSTOM:
                try {
                    return (Class<? extends IChannelDecomDictionary>) Class.forName(typeContainer.getCustomClassName());
                } catch (final ClassNotFoundException e) {
                    throw new DictionaryException("Cannot find DECOM dictionary class " +  typeContainer.getCustomClassName() + " " + e.toString(), e);
                }
            case MM_DECOM:
                return MultimissionGenericDecomDictionary.class;
            case OLD_MM_DECOM:
                return MultimissionChannelDecomDictionary.class;
            default:
                throw new DictionaryException("Unrecognized class type for DECOM dictionary: " + 
                        typeContainer.getClassType());
        }
    }
}
