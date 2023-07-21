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
package jpl.gds.dictionary.impl.command;

import jpl.gds.dictionary.api.DictionaryClassContainer;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.command.ICommandDictionary;
import jpl.gds.dictionary.api.command.ICommandDictionaryFactory;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.config.DictionaryType;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.spring.context.flag.SseContextFlag;

/**
 * CommandDictionaryFactory is used to create mission-specific instances of
 * classes that implement ICommandDictionary, and to maintain a single global
 * instance of the command definitions.
 * <p>
 * The Command dictionary is used by uplink applications to formulate and
 * validate spacecraft or SSE commands. A particular command's arguments,
 * format, and requirements are defined in the project's command dictionary.
 * Every mission may have a different format for representing the command
 * dictionary. An appropriate dictionary parser must be used in order to create
 * the mission-specific ICommandDictionary object, which MUST implement the
 * ICommandDictionary interface. ICommandDictionary objects should only be
 * created via the CommandDictionaryFactory. Direct creation of an
 * ICommandDictionary object in anything other than unit tests is a violation of
 * multi-mission development standards.
 * <p>
 * This class is configured using the
 * DictionaryProperties and GdsSystemProperties objects. The system mission
 * must be set in the GdsSystemProperties object or configured on the Java
 * command line using the GdsSystemProperties.MISSION_PROPERTY System property.
 * In general, these DictionaryProperties fields are initialized to a set of
 * default values using a dictionary properties file, but are often adjusted by
 * the application using this factory.
 * 
 * @version 1.0 - Initial Implementation
 * @version 2.0 - Use DictionaryConfiguration instead of GdsConfiguration.
 *          Remove ADAPTOR_PROPERTY
 * @version 3.0 - Renamed and added methods to follow the standard dictionary
 *          factory pattern
 * @version 3.1 - Uses new dictionary search methods, allowing this factory to
 *          find static instances using different methods based upon configured
 *          search path and file name.

 * 
 * @see ICommandDictionary
 * @see DictionaryProperties
 */
public class CommandDictionaryFactory implements ICommandDictionaryFactory {
    private final SseContextFlag sseFlag;
    private final Tracer         log;

    /**
     * Default factory constructor
     */
    public CommandDictionaryFactory() {
        this(new SseContextFlag());
    }

    /**
     * Factory constructor with SSE context flag
     * 
     * @param sseFlag
     *            the SSE context flag
     */
    public CommandDictionaryFactory(final SseContextFlag sseFlag) {
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
    public CommandDictionaryFactory(final SseContextFlag sseFlag, final Tracer log) {
        this.sseFlag = sseFlag;
        this.log = log;
    }

    @Override
    public ICommandDictionary getNewInstance(final DictionaryProperties config) throws DictionaryException {
        return getNewInstance(config, config.findFileForSystemMission(DictionaryType.COMMAND));
    }

    @Override
    public ICommandDictionary getNewInstance(final DictionaryProperties config, final String filename)
            throws DictionaryException {
        // Need to create a new instance of the adapter class
        try {

            final Class<? extends ICommandDictionary> klazz = getDictionaryClass(config.getDictionaryClass(DictionaryType.COMMAND));
            final ICommandDictionary localInstance = klazz.newInstance();
            /*
             * Use the supplied configuration when
             * parsing
             */
            localInstance.parse(filename, config, log);
            return localInstance;
        } catch (final InstantiationException | IllegalAccessException e) {
            throw new DictionaryException("Cannot create Command dictionary adaptation: " + e.toString(), e);
        }
    }
    
    @SuppressWarnings("unchecked")
    protected Class<? extends ICommandDictionary> getDictionaryClass(final DictionaryClassContainer typeContainer) throws DictionaryException {
        switch (typeContainer.getClassType()) {
            case CUSTOM:
                try {
                    return (Class<? extends ICommandDictionary>) Class.forName(typeContainer.getCustomClassName());
                } catch (final ClassNotFoundException e) {
                    throw new DictionaryException("Cannot find COMMAND dictionary class " +  typeContainer.getCustomClassName() + " " + e.toString(), e);
                }
            case MM_COMMAND:
                return MultimissionCommandDictionary.class;
            default:
                throw new DictionaryException("Unrecognized class type for COMMAND dictionary: " + 
                        typeContainer.getClassType());
        }
    }

}