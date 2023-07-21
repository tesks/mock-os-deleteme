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
package jpl.gds.dictionary.impl.frame;

import jpl.gds.dictionary.api.DictionaryClassContainer;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.config.DictionaryType;
import jpl.gds.dictionary.api.frame.ITransferFrameDictionary;
import jpl.gds.dictionary.api.frame.ITransferFrameDictionaryFactory;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.spring.context.flag.SseContextFlag;

/**
 * TransferFrameDictionaryFactory is used to create mission-specific instances
 * of classes that implement ITransferFrameDictionary, and to maintain a single
 * global instance of the telemetry transfer frame definitions.
 * <p>
 * The Transfer Frame dictionary is used by the telemetry processing system in
 * order to identify the types and format of telemetry frames in use by a
 * mission. It defines the Control and Display Unit (CADU) for each frame by
 * supplying its length, encoding type, ASM, and other information needed to
 * process the frame. An appropriate dictionary parser must be used in order to
 * create the mission-specific ITransferFrameDictionary object, which MUST
 * implement the ITransferFrameDictionary interface. ITransferFrameDictionary
 * objects should only be created via the TransferFrameDictionaryFactory. Direct
 * creation of an ITransferFrameDictionary object is a violation of
 * multi-mission development standards.
 * <p>
 * This class is configured using the
 * DictionaryProperties and GdsSystemProperties objects. The system mission
 * must be set in the GdsSystemProperties object or configured on the Java
 * command line using the GdsSystemProperties.MISSION_PROPERTY System property.
 * In general, these DictionaryConfiguration fields are initialized to a set of
 * default values using a dictionary properties file, but are often adjusted by
 * the application using this factory.
 *
 * 
 * @version 1.0 - Initial Implementation
 * @version 1.1 - Modified to use only reflection for
 *          object creation
 * @version 4.1 - Uses new dictionary search methods, allowing this factory to
 *          find static instances using different methods based upon configured
 *          search path and file name. Use adaptable class name from the config
 *          rather than hardcoded class name. Add method that takes a
 *          DictionaryConfiguration.
 * 
 *
 * @see ITransferFrameDictionary
 */
public final class TransferFrameDictionaryFactory implements ITransferFrameDictionaryFactory {
    private final SseContextFlag sseFlag;
    private final Tracer         log;

    /**
     * Default factory test constructor
     */
    public TransferFrameDictionaryFactory() {
        this(new SseContextFlag());
    }

    /**
     * Default factory test constructor
     */
    public TransferFrameDictionaryFactory(final SseContextFlag sseFlag) {
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
    public TransferFrameDictionaryFactory(final SseContextFlag sseFlag, final Tracer log) {
        this.sseFlag = sseFlag;
        this.log = log;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ITransferFrameDictionary getNewInstance(
            final DictionaryProperties config)
                    throws DictionaryException {
        return getNewInstance(config, config.findFileForSystemMission(DictionaryType.FRAME));
    }

    @Override
    public ITransferFrameDictionary getNewInstance(final DictionaryProperties config, final String filename)
            throws DictionaryException {
        ITransferFrameDictionary dict;
        // Need to create a new instance of the adapter class
        try {

            final Class<? extends ITransferFrameDictionary> klazz = getDictionaryClass(config.getDictionaryClass(DictionaryType.FRAME));
            dict = klazz.newInstance();

        } catch (final InstantiationException | IllegalAccessException e) {
            throw new DictionaryException(
                    "Cannot create Transfer frame dictionary adaptation: "
                            + e.toString(), e);
        }
        /* Use the supplied configuration when parsing */
        dict.parse(filename, config, log);

        return dict;
    }
    
    @SuppressWarnings({ "unchecked", "deprecation" })
    private Class<? extends ITransferFrameDictionary> getDictionaryClass(final DictionaryClassContainer typeContainer) throws DictionaryException {
        switch (typeContainer.getClassType()) {
            case CUSTOM:
                try {
                    return (Class<? extends ITransferFrameDictionary>) Class.forName(typeContainer.getCustomClassName());
                } catch (final ClassNotFoundException e) {
                    throw new DictionaryException("Cannot find FRAME dictionary class " +  typeContainer.getCustomClassName() + " " + e.toString(), e);
                }
            case MM_FRAME:
                return MultimissionTransferFrameDictionary.class;
            case OLD_MM_FRAME:
                return OldTransferFrameDictionary.class;
            default:
                throw new DictionaryException("Unrecognized class type for FRAME dictionary: " + 
                        typeContainer.getClassType());
        }
    }

}