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
package jpl.gds.dictionary.api.decom;

import java.lang.reflect.Constructor;
import java.util.Map;

import jpl.gds.dictionary.api.DictionaryException;
import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.shared.reflect.ReflectionException;
import jpl.gds.shared.reflect.ReflectionToolkit;

/**
 * DecomMapDefinitionFactory is used to create IDecomMapDefinition objects for
 * use in an IChannelDecomDictionary implementation.
 * <br>
 * <br><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br><br>
 * 
 * A Decom Map Definition object is the multi-mission representation of a
 * generic map format for decommutating packets. It assumes that all extracted
 * fields are channels. It is possible to change the allocation of the channels
 * based on a value in the packet. It is also possible that the length of the
 * data in the extracted field does not exactly match the length specified in
 * the channel dictionary (e.g. one may wish to extract 2 bits and put it into a
 * 4-byte integer channel). The channel dictionary sets the type and length of
 * the result. The decom map sets the length of the field to extract. The length
 * of the field extracted by the decom must be less than or equal to the length
 * of the destination channel.
 * <p>
 * IChannelDecomDictionary implementations must parse mission-specific decom
 * dictionary files and create IDecomMapDefinition objects for the definitions
 * found therein. In order to isolate the mission adaptation from changes in the
 * multi-mission core, IChannelDecomDictionary implementations should use this
 * factory to create multi-mission IDecomMapDefinition objects, and define a
 * mission-specific class that implements the interface. All interaction with
 * these objects in mission adaptations should use the IDecomMapDefinition
 * interface, rather than directly interacting with the objects themselves.
 * <p>
 * 
 *
 *
 */
public final class DecomMapDefinitionFactory {
   
    private static final Constructor<?> DEFINITION_CONSTRUCTOR;
    static {
        try {
            final Class<?> defClass = Class.forName(DictionaryProperties.PACKAGE + "decom.DecomMapDefinition");
            DEFINITION_CONSTRUCTOR = ReflectionToolkit.getConstructor(defClass, new Class<?>[]{});
        } catch (ClassNotFoundException | ReflectionException e) {
            throw new RuntimeException("Cannot locate decom map definition class or its constructor", e);
        }
    }
    
    /**
     * Private constructor to enforce static nature.
     */
    private DecomMapDefinitionFactory() {}

    /**
     * Returns an empty IDecomMapDefinition object.
     * 
     * @return IDecomMapDefinition, empty of all decom statements
     */
    public static IDecomMapDefinition createEmptyDecomMap() {
        try {
            return (IDecomMapDefinition)ReflectionToolkit.createObject(DEFINITION_CONSTRUCTOR, new Class<?>[]{});
        } catch (final ReflectionException e) {
            e.printStackTrace();
            return null;
        }
    }
    

    /**
     * Creates an IDecomMapDefinition object from a decom map file.
     * 
     * @param decomFile decom map file to parse
     * @param chanMap channel definition map of channel ID to IChannelDefinition, which must
     * contain the channels referenced in the map
     * 
     * @return IDecomMapDefinition containing parser decom statements
     * @throws DictionaryException if there is an error parsing the map file
     */ 
    public static IDecomMapDefinition createDecomMapFromFile(final String decomFile, final Map<String, IChannelDefinition> chanMap) throws DictionaryException {
        if (decomFile == null) {
            throw new IllegalArgumentException("Decom file path cannot be null");
        }

        if (chanMap == null) {
            throw new IllegalArgumentException("Channel map cannot be null");
        }

        /* Create empty map and populate it, rather than creating an object
         * just to create another object!
         */
        final IDecomMapDefinition instance = createEmptyDecomMap();
        instance.parseDecomFile(decomFile, chanMap);
        return instance;
    }

}
