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
package jpl.gds.dictionary.impl.sequence;

import jpl.gds.dictionary.api.DictionaryClassContainer;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.config.DictionaryType;
import jpl.gds.dictionary.api.sequence.ISequenceDictionary;
import jpl.gds.dictionary.api.sequence.ISequenceDictionaryFactory;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.spring.context.flag.SseContextFlag;

/**
 * SequenceDictionaryFactory is used to create mission-specific instances of
 * classes that implement ISequenceDictionary, and to maintain a single global
 * instance of the sequence category definitions.
 * <p>
 * The Sequence Dictionary is used in the command and telemetry processing in
 * order to translate numeric sequence IDs to more human-readable equivalents.
 * Primarily it tracks sequence categories, but also has some knowledge about
 * how to parse pure numeric sequence IDs to sequence category and sequence
 * number. Every mission may have a different format for representing the
 * Sequence dictionary. An appropriate dictionary parser must be used in order
 * to create the mission-specific ISequenceDictionary object, which MUST
 * implement this interface. ISequenceDictionary objects should only be created
 * via the SequenceDictionaryFactory. Direct creation of an ISequenceDictionary
 * object is a violation of multi-mission development standards.
 * <p>
 * This class is configured using the
 * DictionaryProperties and GdsSystemProperties objects. The system mission
 * must be set in the GdsSystemProperties object or configured on the Java
 * command line using the GdsSystemProperties.MISSION_PROPERTY System property.
 * In general, these DictionaryProperties fields are initialized to a set of
 * default values using a dictionary properties file, but may be adjusted by the
 * application.
 *
 * 
 * @see ISequenceDictionary
 */
public class SequenceDictionaryFactory implements ISequenceDictionaryFactory {
    private final SseContextFlag sseFlag;
    private final Tracer         log;

    /**
     * Default factory test constructor
     */
    public SequenceDictionaryFactory() {
        this(new SseContextFlag());
    }

    /**
     * Default factory test constructor
     * 
     * @param sseFlag
     *            the sse context flag
     */
    public SequenceDictionaryFactory(final SseContextFlag sseFlag) {
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
    public SequenceDictionaryFactory(final SseContextFlag sseFlag, final Tracer log) {
        this.sseFlag = sseFlag;
        this.log = log;
    }

    @Override
    public ISequenceDictionary getNewInstance(
            final DictionaryProperties config)
            throws DictionaryException {
        return getNewInstance(config, config.findFileForSystemMission(DictionaryType.SEQUENCE));
    }
    
    @Override
    public ISequenceDictionary getNewInstance(final DictionaryProperties config, final String filename)
            throws DictionaryException {

        ISequenceDictionary seqDict = null;
        try {

            final Class<? extends ISequenceDictionary> klazz = getDictionaryClass(config.getDictionaryClass(DictionaryType.SEQUENCE));
            seqDict = klazz.newInstance();

        } catch (InstantiationException | IllegalAccessException e) {
            throw new DictionaryException(
                    "Cannot create sequence dictionary adaptation: "
                            + e.toString(), e);
        }

        /* Use the supplied configuration when parsing */
        seqDict.parse(filename, config, log);

        return seqDict;
    }
    
    @SuppressWarnings("unchecked")
    protected Class<? extends ISequenceDictionary> getDictionaryClass(final DictionaryClassContainer typeContainer) throws DictionaryException {
        switch (typeContainer.getClassType()) {
            case CUSTOM:
                try {
                    return (Class<? extends ISequenceDictionary>) Class.forName(typeContainer.getCustomClassName());
                } catch (final ClassNotFoundException e) {
                    throw new DictionaryException("Cannot find SEQUENCE dictionary class " +  typeContainer.getCustomClassName() + " " + e.toString(), e);
                }
            case MM_SEQUENCE:
                return MultimissionSequenceDictionary.class;
            default:
                throw new DictionaryException("Unrecognized class type for SEQUENCE dictionary: " + 
                        typeContainer.getClassType());
        }
    }

}
