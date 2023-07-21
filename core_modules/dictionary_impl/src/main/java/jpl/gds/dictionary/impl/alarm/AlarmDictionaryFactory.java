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
package jpl.gds.dictionary.impl.alarm;

import java.util.Map;

import jpl.gds.dictionary.api.DictionaryClassContainer;
import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.alarm.IAlarmDictionary;
import jpl.gds.dictionary.api.alarm.IAlarmDictionaryFactory;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.config.DictionaryType;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.spring.context.flag.SseContextFlag;

/**
 * AlarmDictionaryFactory is used to create mission-specific instances of
 * classes that implement IAlarmDictionary, and to maintain a single global
 * instance of the Alarm dictionary parser.
 * <p>
 * The Alarm dictionary is used by the telemetry processing system for range and
 * limit checking channelized telemetry values. It identifies the type and
 * characteristics of each alarm. Every mission may have a different format for
 * representing the alarm dictionary. An appropriate dictionary parser must be
 * used in order to create the mission-specific IAlarmDictionary object.
 * IAlarmDictionary objects should only be created via the
 * AlarmDictionaryFactory. Direct creation of a IAlarmDictionary object is a
 * violation of multi-mission development standards.
 * <p>
 * This class is configured using the
 * DictionaryProperties and GdsSystemProperties objects. The system mission
 * must be set in the GdsSystemProperties object or configured on the Java
 * command line using the GdsSystemProperties.MISSION_PROPERTY System property.
 * In general, these DictionaryProperty fields are initialized to a set of
 * default values using a dictionary properties file, but are often adjusted by
 * the application using this factory.
 * <p>
 * Use of dictionary parsers through this class will NOT add
 * the parsed alarm definitions to the core AMPCS AlarmTable. The caller takes
 * responsibility for that step.
 *
 * 
 * @see IAlarmDictionary
 * @see DictionaryProperties
 */
public class AlarmDictionaryFactory implements IAlarmDictionaryFactory  {
    final SseContextFlag sseFlag;
    final Tracer         log;

    /**
     * test factory constructor
     */
    public AlarmDictionaryFactory() {
        this(new SseContextFlag());
    }

    /**
     * Factory constructor with sse context flag
     * 
     * @param sseFlag
     *            the SSE context flag
     */
    public AlarmDictionaryFactory(final SseContextFlag sseFlag) {
        this(sseFlag, TraceManager.getTracer(Loggers.DICTIONARY));
    }

    /**
     * Factory constructor with sse context flag
     * 
     * @param sseFlag
     *            the SSE context flag
     * @param log
     *            Tracer to log with
     */
    public AlarmDictionaryFactory(final SseContextFlag sseFlag, final Tracer log) {
        this.sseFlag = sseFlag;
        this.log = log;
    }

	/**
     * {@inheritDoc}
     */
	@Override
    public IAlarmDictionary getNewInstance(
			final DictionaryProperties config,
			final Map<String, IChannelDefinition> channelIdMapping)
			throws DictionaryException {
		return getNewInstance(
		        config,
				config.findFileForSystemMission(DictionaryType.ALARM),
				channelIdMapping);
	}


    @Override
    public IAlarmDictionary getNewInstance(final DictionaryProperties config, final String filename,
                                           final Map<String, IChannelDefinition> channelIdMapping)
            throws DictionaryException {

        if (sseFlag.isApplicationSse()) {
            return getNewSseInstance(config, filename, channelIdMapping);
        } else {
            return getNewFlightInstance(config, filename, channelIdMapping);
        }
    }

    @Override
    public IAlarmDictionary getNewFlightInstance(final DictionaryProperties config, final String filename,
                                                 final Map<String, IChannelDefinition> channelIdMapping)
            throws DictionaryException {

        // Need to create a new instance of the adapter class
        try {
            final Class<? extends IAlarmDictionary> klazz = getDictionaryClass(config.getDictionaryClass(DictionaryType.ALARM));
            final IAlarmDictionary localInstance = klazz.newInstance();
            localInstance.setChannelMap(channelIdMapping);
            /*
             *  Use the supplied configuration when parsing
             */
            localInstance.parse(filename, config, log);
            return localInstance;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new DictionaryException("Cannot create alarm dictionary adaptation: " + e.toString(), e);
        }
    }

	/**
     * {@inheritDoc}
     */
	@Override
    public IAlarmDictionary getNewSseInstance(
			final DictionaryProperties config,
			final Map<String, IChannelDefinition> channelIdMapping)
			throws DictionaryException {
		return getNewSseInstance(
		        config,
				config.findFileForSystemMission(DictionaryType.ALARM),
				channelIdMapping);
	}


    @Override
    public IAlarmDictionary getNewSseInstance(final DictionaryProperties config, final String filename,
                                              final Map<String, IChannelDefinition> channelIdMapping)
            throws DictionaryException {
        // Need to create a new instance of the adapter class
        try {
            final Class<? extends IAlarmDictionary> klazz = getDictionaryClass(config.getSseDictionaryClass(DictionaryType.ALARM));
            final IAlarmDictionary localInstance = klazz.newInstance();
            localInstance.setChannelMap(channelIdMapping);
            /*
             * Use the supplied configuration when parsing
             */
            localInstance.parse(filename, config, log);
            return localInstance;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new DictionaryException("Cannot create SSE alarm dictionary adaptation: " + e.toString(), e);
        }
    }
    
    @SuppressWarnings("unchecked")
    protected Class<? extends IAlarmDictionary> getDictionaryClass(final DictionaryClassContainer typeContainer) throws DictionaryException {
        switch (typeContainer.getClassType()) {
            case CUSTOM:
                try {
                    return (Class<? extends IAlarmDictionary>) Class.forName(typeContainer.getCustomClassName());
                } catch (final ClassNotFoundException e) {
                    throw new DictionaryException("Cannot find ALARM dictionary class " +  typeContainer.getCustomClassName() + " " + e.toString(), e);
                }
            case MM_ALARM:
                return MultimissionAlarmDictionary.class;
            default:
                throw new DictionaryException("Unrecognized class type for ALARM dictionary: " + 
                        typeContainer.getClassType());
        }
    }


}
