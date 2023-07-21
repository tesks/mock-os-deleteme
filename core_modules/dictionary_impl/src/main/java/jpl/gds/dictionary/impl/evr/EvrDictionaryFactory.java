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
package jpl.gds.dictionary.impl.evr;

import jpl.gds.dictionary.api.DictionaryClassContainer;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.config.DictionaryType;
import jpl.gds.dictionary.api.evr.IEvrDefinition;
import jpl.gds.dictionary.api.evr.IEvrDictionary;
import jpl.gds.dictionary.api.evr.IEvrDictionaryFactory;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.spring.context.flag.SseContextFlag;

/**
 * EvrDictionaryFactory is used to create mission-specific instances of classes
 * that implement IEvrDictionary, and to maintain a single global instance of
 * the Evr dictionary parser.
 * <p>
 * The Evr dictionary is used by the telemetry processing system for the
 * extraction and creation of EVR packets. It identifies the type and
 * characteristics of each supported EVR for a given project. Every mission may
 * have a different format for representing the EVR dictionary. An appropriate
 * dictionary parser must be used in order to create the mission-specific
 * IEvrDictionary object. IEvrDictionary objects should only be created via the
 * EvrDictionaryFactory. Direct creation of an IEvrDictionary object is a
 * violation of multi-mission development standards.
 * <p>
 * This class contains is configured using the
 * DictionaryProperties and GdsSystemProperties objects. The system mission
 * must be set in the GdsSystemProperties object or configured on the Java
 * command line using the GdsSystemProperties.MISSION_PROPERTY System property.
 * In general, these DictionaryProperties fields are initialized to a set of
 * default values using a dictionary properties file, but are often adjusted by
 * the application using this factory.
 * 
 * 
 * @version 1.0 - Initial Implementation
 * @version 2.0 - Use DictionaryConfiguration instead of GdsConfiguration.
 *          Remove ADAPTOR_PROPERTYs
 * @version 3.0 - Remove arguments to supply command definition table when
 *          creating an Evr dictionary. Removed deprecated methods.
 * @version 4.0 - Rename methods to match the standard dictionary factory
 *          pattern.
 * @version 4.1 - Uses new dictionary search methods, allowing this factory to
 *          find static instances using different methods based upon configured
 *          search path and file name.
 *
 * @see IEvrDictionary
 * @see DictionaryProperties
 */
public class EvrDictionaryFactory implements IEvrDictionaryFactory {
    
    private final SseContextFlag sseFlag;
    private final Tracer         log;

    /**
     * Default factory constructor
     */
    public EvrDictionaryFactory() {
        this(new SseContextFlag());
    }

    /**
     * test consntructor
     * 
     * @param sseFlag
     *            the SSE context flag
     */
    public EvrDictionaryFactory(final SseContextFlag sseFlag) {
        this(sseFlag, TraceManager.getTracer(Loggers.DICTIONARY));
    }

    /**
     * Factory constructor with SSE context flag
     * 
     * @param sseFlag
     *            the SSE context flag
     * @param log
     *            Tracer to log with
     */
    public EvrDictionaryFactory(final SseContextFlag sseFlag, final Tracer log) {
        this.sseFlag = sseFlag;
        this.log = log;
    }

    @Override
    public IEvrDictionary getNewInstance(final DictionaryProperties config, final String filename)
            throws DictionaryException {
        if (sseFlag.isApplicationSse()) {
            return getNewSseInstance(config, filename);
        } else {
            return getNewFlightInstance(config, filename);
        }
    }

    @Override
    public IEvrDictionary getNewFlightInstance(final DictionaryProperties config, final String filename)
            throws DictionaryException {
        // Need to create a new instance of the adapter class
        try {

            final Class<? extends IEvrDictionary> klazz= getDictionaryClass(config.getDictionaryClass(DictionaryType.EVR));
            final IEvrDictionary localInstance = klazz.newInstance();
            /*
             *  Use the supplied configuration when
             * parsing
             */
            localInstance.parse(filename, config, log);
            return localInstance;

        } catch (final InstantiationException | IllegalAccessException e) {
            throw new DictionaryException("Cannot create EVR dictionary adaptation: " + e.toString(), e);
        }
    }


    @Override
    public IEvrDictionary getNewInstance(final DictionaryProperties config) throws DictionaryException {
        if (sseFlag.isApplicationSse()) {
            return getNewSseInstance(config);
        }
        return getNewInstance(config, config.findFileForSystemMission(DictionaryType.EVR));
    }

    @Override
    public IEvrDictionary getNewSseInstance(final DictionaryProperties config, final String filename)
            throws DictionaryException {
        // Need to create a new instance of the adapter class
        try {

            final Class<? extends IEvrDictionary> klazz = getDictionaryClass(config.getSseDictionaryClass(DictionaryType.EVR));
            final IEvrDictionary localInstance = klazz.newInstance();
            
            /*  Use the supplied configuration when parsing */
            localInstance.parse(filename, config, log);
            return localInstance;

        } catch (final InstantiationException | IllegalAccessException e) {
            /*
             * Throw DictionaryException rather than
             * GdsConfigurationException.
             */
            throw new DictionaryException(
                    "Cannot create SSE EVR dictionary adaptation: "
                            + e.toString(), e);
        }
    }
    

    @Override
    public IEvrDictionary getNewSseInstance(final DictionaryProperties config) throws DictionaryException {
        return getNewSseInstance(config, config.findSseFileForSystemMission(DictionaryType.EVR));
    }
    
    @Override
    public IEvrDefinition getMultimissionEvrDefinition() {
        return new MultimissionEvrDefinition();
    }
    
    @Override
    public IEvrDefinition getJplSseEvrDefinition() {
        return new SseEvrDefinition();
    }
    
    @SuppressWarnings("unchecked")
    protected Class<? extends IEvrDictionary> getDictionaryClass(final DictionaryClassContainer typeContainer) throws DictionaryException {
        switch (typeContainer.getClassType()) {
            case CUSTOM:
                try {
                    return (Class<? extends IEvrDictionary>) Class.forName(typeContainer.getCustomClassName());
                } catch (final ClassNotFoundException e) {
                    throw new DictionaryException("Cannot find EVR dictionary class " +  typeContainer.getCustomClassName() + " " + e.toString(), e);
                }
            case MM_EVR:
                return MultimissionEvrDictionary.class;
            case SSE_EVR:
                return SseEvrDictionary.class;
            default:
                throw new DictionaryException("Unrecognized class type for EVR dictionary: " + 
                        typeContainer.getClassType());
        }
    }


}
