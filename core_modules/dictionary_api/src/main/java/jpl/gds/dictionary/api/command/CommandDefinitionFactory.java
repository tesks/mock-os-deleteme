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

/**
 * CommandDefinitionFactory is used to create ICommandDefinition objects for use
 * in an ICommandDictionary implementation. <br>
 * <br>
 * <b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br><br>
 * 
 * An ICommandDefinition object is the multi-mission representation of a command
 * specification, which is primarily used to construct a command (e.g. to direct
 * the flight software) into its required format and valid parameter ranges.
 * ICommandDictionary implementations must parse mission-specific command
 * dictionary files and create ICommandDefinition objects for the definitions
 * found therein. In order to isolate the mission adaptation from changes in the
 * multi-mission core, ICommandDictionary implementations should use this
 * factory to create multi-mission ICommandDefinition objects, and define a
 * mission-specific class that implements the interface. All interaction with
 * these objects in mission adaptations should use the ICommandDefinition
 * interface, rather than directly interacting with the objects themselves.
 * <p>
 * This class contains only static methods.
 *
 *  use only reflection for object creation
 *
 *
 * @see ICommandDictionary
 * @see ICommandDefinition
 */
public class CommandDefinitionFactory {
    
    /*  Added constants for cached reflection objects. */
    private static final Constructor<?> SIMPLE_DEFINITION_CONSTRUCTOR;
    
    /*  Added initialization of cached reflection objects. */
    static {
        try {
            Class<?> c = Class.forName(DictionaryProperties.PACKAGE + "command.CommandDefinition");
            SIMPLE_DEFINITION_CONSTRUCTOR = ReflectionToolkit.getConstructor(c, new Class<?> [] 
                    { CommandDefinitionType.class});
        } catch (ClassNotFoundException | ReflectionException e) {
            throw new RuntimeException("Cannot locate command argument definition class or its constructor", e);
        } 
    }
    

    /**
     * Create a multi-mission flight software command object.
     *  
     * @return	new instance of a flight software command
     */
    public static ICommandDefinition createFlightSoftwareCommand() {
        return createSimpleDefinition(CommandDefinitionType.FLIGHT);
    }

    /**
     * Create a multi-mission hardware command object.
     * 
     * @return	new instance of a hardware command
     */
    public static ICommandDefinition createHardwareCommand() {
        ICommandDefinition cmd = createSimpleDefinition(CommandDefinitionType.HARDWARE);
        if (cmd != null) {
            cmd.setCategory(ICommandDefinition.MODULE, "n/a");
        }
        return cmd;
    }

    /**
     * Create a multi-mission sequence directive object.
     * 
     * @return	new instance of a sequence directive
     */
    public static ICommandDefinition createSequenceDirective() {
        return createSimpleDefinition(CommandDefinitionType.SEQUENCE_DIRECTIVE);
    }

    /**
     * Create a multi-mission SSE command object.
     *  
     * @return  new instance of an SSE command
     */
    public static ICommandDefinition createSseCommand() {
        return createSimpleDefinition(CommandDefinitionType.SSE);
    }
    
    /**
     * Creates a command definition object using reflection.
     * 
     * @param type
     *            the CommandDefinitionType of the command
     * @return new ICommandDefinition object
     */
    private static ICommandDefinition createSimpleDefinition(
            CommandDefinitionType type) {
        try {
            return (ICommandDefinition) ReflectionToolkit.createObject(
                    SIMPLE_DEFINITION_CONSTRUCTOR, new Object[] { type });
        } catch (ReflectionException e) {
            e.printStackTrace();
            return null;
        }
    }

}
