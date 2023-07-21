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

import jpl.gds.shared.reflect.ReflectionException;
import jpl.gds.shared.reflect.ReflectionToolkit;

/**
 * A factory for creating packet format definition objects.
 * 
 * @since R8
 *
 */
public class PacketFormatFactory {

    
    private static final String IMPL_PACKAGE = "jpl.gds.ccsds.impl.packet.";
    
    
    private static final Constructor<?> STANDARD_FORMAT_CONSTRUCTOR;
    private static final Constructor<?> CUSTOM_FORMAT_CONSTRUCTOR;
    
    static {
        try {
            final Class<?> c = Class.forName(IMPL_PACKAGE + "PacketFormatDefinition");
            STANDARD_FORMAT_CONSTRUCTOR = ReflectionToolkit.getConstructor(c, new Class<?> [] 
                    { IPacketFormatDefinition.TypeName.class });
            CUSTOM_FORMAT_CONSTRUCTOR = ReflectionToolkit.getConstructor(c, new Class<?> [] 
                    {  IPacketFormatDefinition.TypeName.class, String.class });
        } catch (ClassNotFoundException | ReflectionException e) {
            throw new RuntimeException("Cannot locate packet format class or its constructor", e);
        } 
    }

    /**
     * Constructor. Private to enforce static nature.
     */
    private PacketFormatFactory() {
    	// do nothing
    }
    
    /**
     * Creates a packet format definition given a packet format type.
     * 
     * @param type packet format type name
     * 
     * @return new IPacketFormatDefinition instance
     */
    public static final IPacketFormatDefinition create(IPacketFormatDefinition.TypeName type) {
        try {
            return (IPacketFormatDefinition) ReflectionToolkit.createObject(STANDARD_FORMAT_CONSTRUCTOR, new Object[] { type });
        } catch (final ReflectionException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Creates a packet format definition given a packet format type and custom class name.
     * 
     * @param type packet format type name
     * @param className custom packet format class name
     * 
     * @return new IPacketFormatDefinition instance
     */
    public static final IPacketFormatDefinition create(IPacketFormatDefinition.TypeName type, String className) {
        try {
            return (IPacketFormatDefinition) ReflectionToolkit.createObject(CUSTOM_FORMAT_CONSTRUCTOR, new Object[] { type, className });
        } catch (final ReflectionException e) {
            e.printStackTrace();
            return null;
        }
    }

}
