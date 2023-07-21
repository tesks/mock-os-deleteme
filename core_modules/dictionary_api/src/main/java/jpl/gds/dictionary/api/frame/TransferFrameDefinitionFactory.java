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
package jpl.gds.dictionary.api.frame;

import java.lang.reflect.Constructor;

import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.config.IFrameFormatDefinition;
import jpl.gds.shared.reflect.ReflectionException;
import jpl.gds.shared.reflect.ReflectionToolkit;

/**
 * TransferFrameDefinitionFactory is used to create ITransferFrameDefinition
 * objects for use in a ITransferFrameDictionary implementation.
 * <br>
 * <br><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br><br>
 * 
 * An ITransferFrameDefinition object is the multi-mission representation of one
 * defined type of downlinked transfer frame CADU. It describes the format, ASM,
 * and encoding of the frame. ITransferFrameDefinition defines methods needed to
 * interact with Transfer Frame Definition objects as required by the Transfer
 * Frame Dictionary interface.
 * 
 * In order to isolate the mission adaptation from changes in the multi-mission
 * core, ITransferFrameDictionary implementations should with use this factory
 * to create a multi-mission Transfer Frame Definition object. All interaction
 * with these objects in mission adaptations should use the
 * ITransferFrameDefinition interface, rather than directly interacting with the
 * objects themselves.
 * <p>
 * This class contains only static methods.
 *
 * Use only reflection for object creation
 *
 *
 * @see ITransferFrameDictionary
 * @see ITransferFrameDefinition
 */
public final class TransferFrameDefinitionFactory {
    
    /*  Added constants for cached reflection objects. */
    private static final Constructor<?> SIMPLE_DEFINITION_CONSTRUCTOR;
    private static final Constructor<?> SIMPLE_FORMAT_CONSTRUCTOR;

    /*  Added initialization of cached reflection objects. */
    static {
        try {
            Class<?> c = Class.forName(DictionaryProperties.PACKAGE + "frame.TransferFrameDefinition");
            SIMPLE_DEFINITION_CONSTRUCTOR = ReflectionToolkit.getConstructor(c, new Class<?> [] {} );
            c = Class.forName(DictionaryProperties.PACKAGE + "frame.FrameFormatDefinition");
            SIMPLE_FORMAT_CONSTRUCTOR = ReflectionToolkit.getConstructor(c, new Class<?> [] {IFrameFormatDefinition.TypeName.class} );
        } catch (ClassNotFoundException | ReflectionException e) {
            throw new RuntimeException("Cannot locate frame definition class or its constructor", e);
        } 
    }
    
   /**
    * Private constructor to enforce static nature.
    */
    private TransferFrameDefinitionFactory() {}
    
    /**
     * Creates a multi-mission ITransferFrameDefinition object.
     * 
     * @return ITransferFrameDefinition
     */
    public static ITransferFrameDefinition createTransferFrame() {
        try {
            return (ITransferFrameDefinition) ReflectionToolkit.createObject(
                    SIMPLE_DEFINITION_CONSTRUCTOR, new Object[] {});
        } catch (final ReflectionException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Creates a multi-mission IFrameFormatDefinition object.
     * @param type the type of the transfer frame definition to create
     * 
     * @return IFrameFormatDefinition 
     * 
     */
    public static IFrameFormatDefinition createTransferFrameFormat(final IFrameFormatDefinition.TypeName type) {
        try {
            return (IFrameFormatDefinition) ReflectionToolkit.createObject(
                    SIMPLE_FORMAT_CONSTRUCTOR, new Object[] {type});
        } catch (final ReflectionException e) {
            e.printStackTrace();
            return null;
        }
    }
}
