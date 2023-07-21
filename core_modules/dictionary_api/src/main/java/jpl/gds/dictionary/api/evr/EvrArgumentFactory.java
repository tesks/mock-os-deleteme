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
package jpl.gds.dictionary.api.evr;

import java.lang.reflect.Constructor;

import jpl.gds.dictionary.api.EnumerationDefinition;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.shared.reflect.ReflectionException;
import jpl.gds.shared.reflect.ReflectionToolkit;

/**
 * EvrArgumentFactory is used to create IEvrArgumentDefinition objects for use
 * in an IEvrDictionary implementation. 
 * <br>
 * <br><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br><br>
 * 
 * An IEvrArgumentDefinition object is the multi-mission representation of an
 * EVR argument specification, which is used to interpret consumed EVR data and
 * to properly format them as required by the mission. IEvrDictionary
 * implementations must parse mission-specific EVR dictionary files and create
 * IEvrDefinition with attached IEvrArgumentDefinition objects for the
 * definitions found therein. In order to isolate the mission adaptation from
 * changes in the multi-mission core, IEvrDictionary implementations should use
 * this factory to create multi-mission IEvrArgumentDefinition objects, and
 * define a mission-specific class that implements the interface. All
 * interaction with these objects in mission adaptations should use the
 * IEvrArgumentDefinition interface, rather than directly interacting with the
 * objects themselves.
 * <p>
 * This class contains only static methods.
 *
 * Use only reflection for object creation
 * 
 *
 *
 * @see IEvrDictionary
 * @see IEvrDefinition
 */
public class EvrArgumentFactory {
    
    /* Added constants for cached reflection objects. */
    private static final Constructor<?> SIMPLE_DEFINITION_CONSTRUCTOR;
    private static final Constructor<?> COMPLEX_DEFINITION_CONSTRUCTOR;

    /* Added initialization of cached reflection objects. */
    static {
        try {
            Class<?> c = Class.forName(DictionaryProperties.PACKAGE + "evr.EvrArgumentEntry");
            SIMPLE_DEFINITION_CONSTRUCTOR = ReflectionToolkit.getConstructor(c, new Class<?> [] {} );
            COMPLEX_DEFINITION_CONSTRUCTOR = ReflectionToolkit.getConstructor(c, new Class<?> [] 
                    { int.class, String.class, EvrArgumentType.class, int.class, EnumerationDefinition.class, String.class});
        } catch (ClassNotFoundException | ReflectionException e) {
            throw new RuntimeException("Cannot locate channel definition class or its constructor", e);
        } 
    }
    
    /**
     * Create an empty multi-mission EVR argument definition object. 
     * 
     * @return new instance of an EVR argument definition      
     */
    public static IEvrArgumentDefinition createArgumentDefinition() {

        try {
            return (IEvrArgumentDefinition) ReflectionToolkit.createObject(
                    SIMPLE_DEFINITION_CONSTRUCTOR, new Object[] {});
        } catch (ReflectionException e) {
            e.printStackTrace();
            return null;
        }

    }    

    /**
     * Create a populated multi-mission EVR argument definition object.
     * 
     * @param number
     *            number of the argument; first argument is 0
     * @param name
     *            name of the argument
     * @param type
     *            data type of the argument
     * @param len
     *            length of the argument in bytes
     * @param enumValues
     *            table of enumeration values for the argument; may be null
     *            
     * @return new instance of an EVR argument definition
     * 
     */
    public static IEvrArgumentDefinition createArgumentDefinition(
            final int number, final String name, final EvrArgumentType type,
            int len, final EnumerationDefinition enumValues) {

        try {
            return (IEvrArgumentDefinition) ReflectionToolkit.createObject(
                    COMPLEX_DEFINITION_CONSTRUCTOR, 
                    new Object[] {number, name, type, len, enumValues,
                            enumValues == null ? null : enumValues.getName()});
        } catch (ReflectionException e) {
            e.printStackTrace();
            return null;
        }
       
    }

}
