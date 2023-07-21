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
package jpl.gds.dictionary.impl.apid;

import jpl.gds.dictionary.api.DictionaryClassContainer;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.apid.IApidDictionary;
import jpl.gds.dictionary.api.apid.IApidDictionaryFactory;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.config.DictionaryType;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.spring.context.flag.SseContextFlag;

/**
 * ApidDictionaryFactory is used to create mission-specific instances of classes
 * that implement IApidDictionary, and to maintain a single global instance of
 * the APID definitions.
 * <p>
 * The APID (Application Process IDentifier) dictionary is used by the telemetry
 * processing system in order to identify the types and format of data in
 * telemetry packets. Every mission may have a different format for representing
 * the APID dictionary. An appropriate dictionary parser must be used in order
 * to create the mission-specific IApidDictionary object, which MUST implement
 * the IApidDictionary interface. IApidDictionary objects should only be created
 * via this ApidDictionaryFactory. Direct creation of an IApidDictionary object
 * is a violation of multi-mission development standards.
 * <p>
 * This class is configured using the
 * DictionaryProperties and GdsSystemProperties objects. The system mission
 * must be set in the GdsSystemProperties object or configured on the Java
 * command line using the GdsSystemProperties.MISSION_PROPERTY System property.
 * In general, these DictionaryProperty fields are initialized to a set of
 * default values using a dictionary properties file, but are often adjusted by
 * the application using this factory.
 * <p>
 *
 * 
 */
public class ApidDictionaryFactory implements IApidDictionaryFactory {
    private final SseContextFlag sseFlag;
    private final Tracer         log;

    /**
     * Default test constructor
     */
    public ApidDictionaryFactory() {
        this(new SseContextFlag());
    }

    /**
     * test constructor
     * 
     * @param sseFlag
     *            the SSE context flag
     */
    public ApidDictionaryFactory(final SseContextFlag sseFlag) {
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
    public ApidDictionaryFactory(final SseContextFlag sseFlag, final Tracer log) {
        this.sseFlag = sseFlag;
        this.log = log;
    }


	@Override
    public IApidDictionary getNewInstance(final DictionaryProperties dictConfig) throws DictionaryException {
        return getNewInstance(dictConfig, dictConfig.findFileForSystemMission(DictionaryType.APID));
	}

    @Override
    public IApidDictionary getNewInstance(final DictionaryProperties dictConfig, final String filePath)
            throws DictionaryException {
        if (sseFlag.isApplicationSse()) {
            return getNewSseInstance(dictConfig, filePath);
        } else {
            return getNewFlightInstance(dictConfig, filePath);
        }
    }

    @Override
    public IApidDictionary getNewFlightInstance(final DictionaryProperties dictConfig, final String filePath)
            throws DictionaryException {

        if (dictConfig == null) {
            throw new IllegalArgumentException("Dictionary configuration cannot be null");
        }

        if (filePath == null) {
            throw new IllegalArgumentException("File path cannot be null");
        }

        try {

            final Class<? extends IApidDictionary> klazz = getDictionaryClass(dictConfig.getDictionaryClass(DictionaryType.APID));
            final IApidDictionary newInstance = klazz.newInstance();

            /*
             * Use the supplied configuration when
             * parsing
             */
            newInstance.parse(filePath, dictConfig, log);

            return newInstance;

        } catch (InstantiationException | IllegalAccessException e) {
            throw new DictionaryException("Cannot create APID dictionary adaptation: " + e.toString(), e);
        }
	}


    @Override
    public IApidDictionary getNewSseInstance(final DictionaryProperties config, final String filename)
            throws DictionaryException {
        // Need to create a new instance of the adapter class
        try {
            final Class<? extends IApidDictionary> klazz = getDictionaryClass(config.getSseDictionaryClass(DictionaryType.APID));
            final IApidDictionary localInstance = klazz.newInstance();
            /*
             * Use the supplied configuration when
             * parsing
             */
            localInstance.parse(filename, config, log);
            return localInstance;
        } catch (final InstantiationException | IllegalAccessException e) {
            throw new DictionaryException("Cannot create SSE APID dictionary adaptation: " + e.toString(), e);
        }
    }

	@Override
    public IApidDictionary getSseNonStaticInstance(final DictionaryProperties dictConfig) throws DictionaryException {
        return getNewSseInstance(dictConfig, dictConfig.findSseFileForSystemMission(DictionaryType.APID));
	}
	
	@SuppressWarnings("unchecked")
    protected Class<? extends IApidDictionary> getDictionaryClass(final DictionaryClassContainer typeContainer) throws DictionaryException {
	    switch (typeContainer.getClassType()) {
            case CUSTOM:
                try {
                    return (Class<? extends IApidDictionary>) Class.forName(typeContainer.getCustomClassName());
                } catch (final ClassNotFoundException e) {
                    throw new DictionaryException("Cannot find APID dictionary class " +  typeContainer.getCustomClassName() + " " + e.toString(), e);
                }
            case MM_APID:
                return MultimissionApidDictionary.class;
            case SSE_APID:
                return SseApidDictionary.class;
            default:
                throw new DictionaryException("Unrecognized class type for APID dictionary: " + 
                        typeContainer.getClassType());
        }
	}

}
