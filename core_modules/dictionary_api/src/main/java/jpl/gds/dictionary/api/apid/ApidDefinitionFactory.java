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
package jpl.gds.dictionary.api.apid;

import java.lang.reflect.Constructor;

import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.shared.reflect.ReflectionException;
import jpl.gds.shared.reflect.ReflectionToolkit;

/**
 * ApidDefinitionFactory is used to create IApidDefinition objects for use in a
 * IChannelDictionary implementation.
 * <p>
 * <p><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <p>
 * <p>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <p><p>
 * 
 * An Apid Definition object is the multi-mission representation of an
 * application process identifier, which is used to label packets such that they
 * can be routed to the proper processing components in the ground system.
 * ApidDictionary implementations must parse mission-specific Apid definition
 * files and create Apid Definition objects for the definitions found therein.
 * In order to isolate the mission adaptation from changes in the multi-mission
 * core, ApidDictionary implementations should with use this factory to create a
 * multi-mission Apid Definition object, or define a mission-specific class that
 * implements IApidDefinition. All interaction with these objects in mission
 * adaptations should use the IApidDefinition interface, rather than directly
 * interacting with the objects themselves.
 * <p>
 * This class contains only static methods.
 *
 * 
 * @see IApidDictionary
 * @see IApidDefinition
 */
public final class ApidDefinitionFactory {
    
    private static final Constructor<?> DEFINITION_CONSTRUCTOR;

    static {
        try {
            Class<?> defClass = Class.forName(DictionaryProperties.PACKAGE + "apid.ApidDefinition");
            DEFINITION_CONSTRUCTOR = ReflectionToolkit.getConstructor(defClass, new Class<?>[]{});
        } catch (ClassNotFoundException | ReflectionException e) {
            throw new RuntimeException("Cannot locate apid definition class or its constructor", e);
        }
    }

    /**
     * Private constructor to enforce static nature.
     */
    private ApidDefinitionFactory() {}

    /**
     * Creates a multi-mission IApidDefinition object.
     * 
     * @return IApidDefinition
     */
    public static IApidDefinition createApid() {
        try {
            return (IApidDefinition)ReflectionToolkit.createObject(DEFINITION_CONSTRUCTOR, new Class<?>[]{});
        } catch (ReflectionException e) {
            e.printStackTrace();
            return null;
        }
    }
}
