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
package jpl.gds.dictionary.api.command;

import java.lang.reflect.Constructor;

import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.shared.reflect.ReflectionException;
import jpl.gds.shared.reflect.ReflectionToolkit;
import jpl.gds.shared.sys.SystemUtilities;

/**
 * CommandArgumentDefinitionFactory is used to create ICommandAtgumentDefinition
 * objects for use in an ICommandDictionary implementation.
 * <br>
 * <br><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br><br><br>
 * 
 * An ICommandArgumentDefinition object is the multi-mission representation of a
 * command argument specification, which is used in the construction of a
 * command argument, in order to understand its required format valid ranges of
 * values. ICommandDictionary implementations must parse mission-specific
 * command dictionary files and create ICommandArgumentDefinition objects for
 * the argument definitions found therein. In order to isolate the mission
 * adaptation from changes in the multi-mission core, ICommandDictionary
 * implementations should use this factory to create multi-mission
 * ICommandArgumentDefinition objects, and define a mission-specific class that
 * implements the interface. All interaction with these objects in mission
 * adaptations should use the ICommandArgumentDefinition interface, rather than
 * directly interacting with the objects themselves.
 * <p>
 * This class contains only static methods.
 *
 * Use only reflection for object creation
 *
 *
 * @see ICommandDictionary
 * @see ICommandArgumentDefinition
 * @see IRepeatCommandArgumentDefinition
 */
public final class CommandArgumentDefinitionFactory {
    
    /* Added constants for cached reflection objects. */
    private static final Constructor<?> SIMPLE_DEFINITION_CONSTRUCTOR;
    private static final Constructor<?> REPEAT_DEFINITION_CONSTRUCTOR;

    /* Added initialization of cached reflection objects. */
    static {
        try {
            Class<?> c = Class.forName(DictionaryProperties.PACKAGE + "command.CommandArgumentDefinition");
            SIMPLE_DEFINITION_CONSTRUCTOR = ReflectionToolkit.getConstructor(c, new Class<?> [] 
                    { CommandArgumentType.class});
            c = Class.forName(DictionaryProperties.PACKAGE + "command.RepeatCommandArgumentDefinition");
            REPEAT_DEFINITION_CONSTRUCTOR = ReflectionToolkit.getConstructor(c, new Class<?> [] {});
        } catch (ClassNotFoundException | ReflectionException e) {
            throw new RuntimeException("Cannot locate command argument definition class or its constructor", e);
        } 
    }
    

    /**
     * Private constructor to enforce static nature.
     */
    private CommandArgumentDefinitionFactory() {

        SystemUtilities.doNothing();
    }

    /**
     * Creates an ICommandArgumentDefinition for the given argument type.
     * 
     * @param type
     *            the data type of the command argument
     * 
     * @return new ICommandArgumentDefinition object, initialized to the
     *         supplied argument type. If type is REPEAT, then an
     *         IRepeatCommandArgumentDefinition object is returned instead.
     */
    public static ICommandArgumentDefinition create(CommandArgumentType type) {

        try {
        if (type.equals(CommandArgumentType.REPEAT)) {
                return (ICommandArgumentDefinition) ReflectionToolkit.createObject(
                        REPEAT_DEFINITION_CONSTRUCTOR, new Object[] {});
        }
        return (ICommandArgumentDefinition) ReflectionToolkit.createObject(
                SIMPLE_DEFINITION_CONSTRUCTOR, new Object[] {type});
        } catch (ReflectionException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Creates an IRepeatCommandArgumentDefinition for a repeating command
     * argument.
     * 
     * 
     * @return new IRepeatCommandArgumentDefinition object
     */
    public static IRepeatCommandArgumentDefinition createRepeat() {

        try {
            return (IRepeatCommandArgumentDefinition) ReflectionToolkit.createObject(
                    REPEAT_DEFINITION_CONSTRUCTOR, new Object[] {});
        } catch (ReflectionException e) {
            e.printStackTrace();
            return null;
        }
    }
}
