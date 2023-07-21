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
package jpl.gds.ccsds.api.packet;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import jpl.gds.ccsds.api.packet.IPacketFormatDefinition.TypeName;
import jpl.gds.shared.reflect.ReflectionException;
import jpl.gds.shared.reflect.ReflectionToolkit;

/**
 * PacketHeaderFactory is used to create mission-specific instances of classes
 * that implement ISpacePacketHeader.
 * <p>
 * <b>MULTI-MISSION CORE ADAPTATION CLASS
 * <p>
 * This is a controlled class. It may not be updated without applicable change
 * requests being filed, and approval of project management. A new version tag
 * must be added below with each revision, and both ECR number and author must
 * be included with the version number.</b>
 * <p>
 * Instances of ISpacePacketHeader are used by the telemetry processing system to
 * parse and access packet header information. An appropriate ISpacePacketHeader
 * object must be used for each mission, since packet formats may differ.
 * ISpacePacketHeader objects should only be created via the PacketHeaderFactory.
 * Direct creation of an ISpacePacketHeader object is a violation of multi-mission
 * development standards.
 * <p>
 * This class contains only static methods. The class of the actual object
 * instantiated by this factory is controlled by the GDS configuration property
 * whose name is defined by the constant PACKET_TYPE_PROPERTY in the
 * implementation of this factory class.
 * <p>
 *
 * 
 * @see ISpacePacketHeader
 */
public class PacketHeaderFactory {
    
    /**
     * Map of cached Constructor objects by class name. Improves performance.
     * Not concurrent or synchronized because this class performs only atomic
     * operations on it.
     * 
     */
    private static Map<String, Constructor<?>> noArgConstructors = new HashMap<String, Constructor<?>>();

	/**
	 * Creates an ISpacePacketHeader object appropriate for the current mission,
	 * given a packet format definition. Note that this method utilizes caching
	 * of constructor objects. If the configuration should change, reset()
	 * should be invoked in order to clear the cached information.
	 * 
	 * @param packetFormat
	 *            the packet format definition
	 * @return new ISpacePacketHeader object 
	 *
	 * 
	 */
    public static ISpacePacketHeader create(final IPacketFormatDefinition packetFormat) {
        ISpacePacketHeader header = null;

        final String className = packetFormat.getPacketHeaderClass();

        if (className == null) {
            throw new RuntimeException(
                    "The mission packet header adaptation is not configured."
                            + " Cannot create packet header.");
        }

        try {

            /*
             * Check for cached constructor. If it
             * does not exist, create it and make the return object, and cache
             * both class object and constructor. If the constructor already
             * exists, use it to create the return object.
             */
            Constructor<?> noArgConstructor = noArgConstructors.get(className);
            if (noArgConstructor == null) {
                noArgConstructor = ReflectionToolkit.getConstructor(className,
                        new Class<?>[] {});
                header = (ISpacePacketHeader) ReflectionToolkit.createObject(
                        noArgConstructor, new Object[] {});
                noArgConstructors.put(className, noArgConstructor);
            } else {
                header = (ISpacePacketHeader) ReflectionToolkit.createObject(
                        noArgConstructor, new Object[] {});
            }
            return header;

        } catch (final ReflectionException e) {
            e.printStackTrace();
            throw new RuntimeException("Cannot create packet header (class=" +
            className + "): " + e.toString());

        }

    }
    
	/**
	 * Creates an ISpacePacketHeader object appropriate for the current mission,
	 * given a packet format type name. Note that this method utilizes caching
	 * of constructor objects. If the configuration should change, reset()
	 * should be invoked in order to clear the cached information.
	 * 
	 * @param type
	 *            the packet format type named
	 * @return new ISpacePacketHeader object 
	 * 
	 */
    public static ISpacePacketHeader create(final IPacketFormatDefinition.TypeName type) {
        ISpacePacketHeader header = null;
        
        if (type == TypeName.CUSTOM_CLASS) {
            throw new IllegalArgumentException("Cannot use this factory method for custom packet headers");
        }

        final String className = type.getDefaultPacketHeaderClass();

        try {

            /*
             * Check for cached constructor. If it
             * does not exist, create it and make the return object, and cache
             * both class object and constructor. If the constructor already
             * exists, use it to create the return object.
             */
            Constructor<?> noArgConstructor = noArgConstructors.get(className);
            if (noArgConstructor == null) {
                noArgConstructor = ReflectionToolkit.getConstructor(className,
                        new Class<?>[] {});
                header = (ISpacePacketHeader) ReflectionToolkit.createObject(
                        noArgConstructor, new Object[] {});
                noArgConstructors.put(className, noArgConstructor);
            } else {
                header = (ISpacePacketHeader) ReflectionToolkit.createObject(
                        noArgConstructor, new Object[] {});
            }
            return header;

        } catch (final ReflectionException e) {
            e.printStackTrace();
            throw new RuntimeException("Cannot create packet header (class=" +
            className + "): " + e.toString());

        }

    }


    /**
     * Resets all cached information so that the next call to create will reload
     * configuration.
     * 
     */
    public static void reset() {
        noArgConstructors.clear();
    }
}
