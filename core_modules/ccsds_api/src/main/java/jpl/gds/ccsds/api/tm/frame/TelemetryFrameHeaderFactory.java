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
package jpl.gds.ccsds.api.tm.frame;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import jpl.gds.common.config.mission.MissionProperties;
import jpl.gds.dictionary.api.config.IFrameFormatDefinition;
import jpl.gds.dictionary.api.frame.ITransferFrameDefinition;
import jpl.gds.shared.reflect.ReflectionException;
import jpl.gds.shared.reflect.ReflectionToolkit;

/**
 * FrameHeaderFactory is used to create mission-specific instances of classes
 * that implement IFrameHeader.
 * <p>
 * <b>MULTI-MISSION CORE ADAPTATION CLASS
 * <p>
 * This is a controlled class. It may not be updated without applicable change
 * requests being filed, and approval of project management. A new version tag
 * must be added below with each revision, and both ECR number and author must
 * be included with the version number.</b>
 * <p>
 * Instances of IFrameHeader are used by the telemetry processing system to
 * parse and access frame header information. An appropriate IFrameHeader
 * object must be used for each mission and transfer frame definition, 
 * since frame formats may differ.
 * IFrameHeader objects should only be created via the FrameHeaderFactory.
 * Direct creation of an IFrameHeader object is a violation of multi-mission
 * development standards.
 * <p>
 * This class contains only static methods. 
 * <p>
 *
 *
 * 
 * @see ITelemetryFrameHeader
 */
public class TelemetryFrameHeaderFactory {
    /* Removed need for adaptor property in the GDS Config.
     * Now must cache multiple constructors instead of just 1. 
     */     
     private static final Map<String, Constructor<?>> noArgConstructors = 
             new HashMap<String, Constructor<?>>();
     private static final Map<String, Constructor<?>> defArgConstructors = 
             new HashMap<String, Constructor<?>>();
     
    /**
     * Creates an ITelemetryFrameHeader object appropriate for the current mission, as
     * defined by the supplied FrameFormat object.
     * 
     * @param missionProps the current mission properties object
     * @param format FrameFormat object for the frame to create header for.
     * 
     * @return IFrameHeader object
     * 
     */
    public static ITelemetryFrameHeader create(MissionProperties missionProps, IFrameFormatDefinition format) {
       ITelemetryFrameHeader header = null;
       try {
           Constructor<?> tempConstructor = null;

           final String className = format.getFrameHeaderClass();
           if (className == null) {
               throw new IllegalStateException(
                       "The mission frame header class is not defined in the transfer frame format."
                               + " Cannot create frame header.");
           }
           tempConstructor = noArgConstructors.get(className);
           if (tempConstructor == null) {
               tempConstructor = ReflectionToolkit.getConstructor(className,
                       new Class<?>[] {});
           }
           header = (ITelemetryFrameHeader) ReflectionToolkit.createObject(
                   tempConstructor, new Object[] {});
           noArgConstructors.put(className, tempConstructor);

           header.setIdleVcids(missionProps.getIdleVcids());
           return header;
       } catch (final ReflectionException e) {
           e.printStackTrace();
           throw new IllegalStateException("Cannot create frame header: "
                   + e.toString());
       }
    }
    
    /**
     * Creates an IFrameHeader object appropriate for the current mission, as
     * defined by the supplied ITransferFrameDefinition object.
     * 
     * @param missionProps the current mission properties object
     * @param formatDef ITransferFrameDefinition object for the frame to create header for.
     * 
     * @return IFrameHeader object
     * 
     */
    public static ITelemetryFrameHeader create(MissionProperties missionProps, ITransferFrameDefinition formatDef) {
         ITelemetryFrameHeader header = null;
        try {
            Constructor<?> tempConstructor = null;

            final String className = formatDef.getFormat().getFrameHeaderClass();
            if (className == null) {
                throw new IllegalStateException(
                        "The mission frame header class is not defined in the transfer frame definition."
                                + " Cannot create frame header.");
            }
            tempConstructor = defArgConstructors.get(className);
            if (tempConstructor == null) {
                tempConstructor = ReflectionToolkit.getConstructor(className,
                        new Class<?>[] {ITransferFrameDefinition.class});
                if (tempConstructor == null) {
                   return create(missionProps, formatDef.getFormat());
                }
            }
            header = (ITelemetryFrameHeader) ReflectionToolkit.createObject(
                    tempConstructor, new Object[] {formatDef});
            defArgConstructors.put(className, tempConstructor);
            header.setIdleVcids(missionProps.getIdleVcids());

            return header;
        } catch (final ReflectionException e) {
            e.printStackTrace();
            throw new IllegalStateException("Cannot create frame header: "
                    + e.toString());
        }
     }

    
}
