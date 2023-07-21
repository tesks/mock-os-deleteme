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
package jpl.gds.dictionary.api.channel;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;

import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.shared.reflect.ReflectionException;
import jpl.gds.shared.reflect.ReflectionToolkit;


/**
 * ChannelDerivationFactory is used to create IChannelDerivation objects for use
 * in a IChannelDictionary implementation.
 * <br>
 * <br><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br><br>
 * 
 * An IChannelDerivation (Channel Derivation Definition) object is the
 * multi-mission representation of the method used to create a derived channel
 * (a channel created on the ground from the contents of other channels).
 * IChannelDictionary implementations must parse mission-specific channel
 * definition files and create IChannelDerivation objects for the derivation
 * definitions found therein. In order to isolate the mission adaptation from
 * changes in the multi-mission core, IChannelDictionary implementations should
 * never create IChannelDerivation objects directly. Instead, this factory
 * should be employed. In addition, all IChannelDerivation objects implement the
 * IChannelDerivation interface, and all interaction with these objects in
 * mission adaptations should use this interface, rather than directly
 * interacting with the objects themselves.
 * <p>
 * This class contains only static methods. There are currently two types of
 * derivations: bit unpack and algorithmic.
 *
 * Use only reflection for object creation
 *
 *
 * @see IChannelDictionary
 * @see IChannelDerivation
 */
public final class ChannelDerivationFactory {
    
    /* Added constants for cached reflection objects. */
    private static final Constructor<?> ALGO_DEFINITION_CONSTRUCTOR;
    private static final Constructor<?> UNPACK_DEFINITION_CONSTRUCTOR;

    /* Added initialization of cached reflection objects. */
    static {
        try {
            Class<?> c = Class.forName(DictionaryProperties.PACKAGE + "channel.AlgorithmicDerivationDefinition");
            ALGO_DEFINITION_CONSTRUCTOR = ReflectionToolkit.getConstructor(c, new Class<?> [] 
                    { String.class});
            c = Class.forName(DictionaryProperties.PACKAGE + "channel.BitUnpackDerivationDefinition");
            UNPACK_DEFINITION_CONSTRUCTOR = ReflectionToolkit.getConstructor(c, new Class<?> [] {} );
        } catch (ClassNotFoundException | ReflectionException e) {
            throw new RuntimeException("Cannot locate channel definition class or its constructor", e);
        } 
    }

    /**
     * Private constructor to enforce static nature.
     * 
     */
    private ChannelDerivationFactory() {}

    /**
     * Creates an empty Bit Unpack channel derivation definition.
     * 
     * @return IChannelDerivation
     * 
     */
    public static IBitUnpackChannelDerivation createBitUnpackDerivation() {

        try {
            return (IBitUnpackChannelDerivation) ReflectionToolkit.createObject(
                    UNPACK_DEFINITION_CONSTRUCTOR, new Object[] {});
        } catch (ReflectionException e) {
            e.printStackTrace();
            return null;
        }
    }
    
   /**
    * Creates a Bit Unpack channel derivation definition initialized with the given parameters.
    * 
    * @param parent the channel ID of the parent (source) channel
    * @param child the channel ID of the child (destination) channel
    * @param startBits the list of start bits, one per bit range to be extracted from the parent
    * @param bitLengths the list of bit field lengths, one per start bit entry
    * @return IChannelDerivation
    * 
    * @throws IllegalArgumentException if any argument is null, or if the number of items
    * in the startBits list does not equal the number in the bitLengths list
    */
   public static IBitUnpackChannelDerivation createBitUnpackDerivation(final String parent, final String child, 
           final List<Integer> startBits, final List<Integer> bitLengths) throws IllegalArgumentException {
       if (child == null || parent == null || startBits == null || bitLengths == null) {
           throw new IllegalArgumentException("all of the input arguments must be non-null");
       }
       if (startBits.size() != bitLengths.size()) {
           throw new IllegalArgumentException("startBits and bitLengths must have the same number of elements");
       }

       IBitUnpackChannelDerivation derivation = createBitUnpackDerivation();
       derivation.addChild(child);
       derivation.addParent(parent);
       int index = 0;
       for (Integer i: startBits) {
           derivation.addBitRange(i, bitLengths.get(index++));
       }
       return derivation;
   }
   
   /**
    * Creates an empty Algorithmic channel derivation definition.
    * 
    * @param id
    *            A unique ID for this derivation in the dictionary
    * @return IChannelDerivation
    * 
    * @throws IllegalArgumentException
    *             if any argument other than parameters is null or there is an
    *             error creating the algorithm definition
    */
   public static IAlgorithmicChannelDerivation createAlgorithmicDerivation(String id) {
       try {
           return (IAlgorithmicChannelDerivation) ReflectionToolkit.createObject(
                   ALGO_DEFINITION_CONSTRUCTOR, new Object[] {id});
       } catch (ReflectionException e) {
           if (e.getCause() instanceof DerivationDefinitionException) {
               throw new IllegalArgumentException(
                       "Algorithm definition is in error for ID: " + id);
           }
           e.printStackTrace();
           return null;
       }
   }

    /**
     * Creates an Algorithmic channel derivation definition with parameters 
     * supplied as a map.
     * 
     * @param derivationId
     *            A unique ID for this derivation in the dictionary
     * @param algorithmName
     *            the Java class name of the algorithm that performs the
     *            derivation
     * @param parents
     *            the list of parent (source) channel IDs
     * @param children
     *            the list of child (destination) channel IDs
     * @param parameters
     *            optional Map of algorithm parameters in name,value pairs; may
     *            be null
     * @param triggerId
     *            optional trigger channel for the derivation; may be null
     * @return IChannelDerivation
     * 
     * @throws IllegalArgumentException
     *             if any argument other than parameters is null or there is an
     *             error creating the algorithm definition
     */
    public static IAlgorithmicChannelDerivation createAlgorithmicDerivation(
            final String derivationId, final String algorithmName,
            final List<String> parents, final List<String> children,
            final Map<String, String> parameters, String triggerId)
            throws IllegalArgumentException {

        if (derivationId == null || children == null || parents == null
                || algorithmName == null) {
            throw new IllegalArgumentException(
                    "all of the input arguments must be non-null");
        }
            
            IAlgorithmicChannelDerivation derivation = createAlgorithmicDerivation(derivationId);
            derivation.addChildren(children);
            derivation.addParents(parents);
            derivation.setAlgorithmName(algorithmName);
            derivation.setTriggerId(triggerId);
            if (parameters != null) {
                for (Map.Entry<String, String> entry : parameters.entrySet()) {
                    derivation.putParameter(entry.getKey(), entry.getValue());
                }
            }
            return derivation;
       
    }

    /**
     * Creates an Algorithmic channel derivation definition with parameters
     * supplied as lists.
     * 
     * @param derivationId
     *            A unique ID for this derivation in the dictionary
     * @param algorithmName
     *            the Java class name of the algorithm that performs the
     *            derivation
     * @param parents
     *            the list of parent (source) channel IDs
     * @param children
     *            the list of child (destination) channel IDs
     * @param parameters
     *            optional List of algorithm parameters; may be null
     * @param triggerId
     *            optional trigger channel for the derivation; may be null
     * @return IChannelDerivation
     * 
     * @throws IllegalArgumentException
     *             if any argument other than parameters is null or there is an
     *             error creating the algorithm definition
     */
    public static IAlgorithmicChannelDerivation createAlgorithmicDerivation(
            final String derivationId, final String algorithmName,
            final List<String> parents, final List<String> children,
            final List<String> parameters, String triggerId)
            throws IllegalArgumentException {

        if (derivationId == null || children == null || parents == null
                || algorithmName == null) {
            throw new IllegalArgumentException(
                    "all of the input arguments must be non-null");
        }
            IAlgorithmicChannelDerivation derivation = createAlgorithmicDerivation(derivationId);
            derivation.addChildren(children);
            derivation.addParents(parents);
            derivation.setAlgorithmName(algorithmName);
            derivation.setTriggerId(triggerId);
            if (parameters != null) {
                for (String parameter : parameters) {
                    derivation.addParameter(parameter);
                }
            }
            return derivation;
      
    }
}
