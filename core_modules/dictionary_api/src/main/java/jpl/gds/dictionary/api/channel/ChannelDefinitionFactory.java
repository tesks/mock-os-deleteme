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

import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.shared.reflect.ReflectionException;
import jpl.gds.shared.reflect.ReflectionToolkit;

/**
 * ChannelDefinitionFactory is used to create IChannelDefinition objects for use
 * in a IChannelDictionary implementation.
 * <br>
 * <br><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br><br>
 * 
 * An IChannelDefinition object is interface to the multi-mission definition of
 * a telemetry or ground- derived channel. IChannelDictionary implementations
 * must parse mission-specific channel definition files and create
 * ChannelDefinition objects for the definitions found therein. In order to
 * isolate the mission adaptation from changes in the multi-mission core,
 * IChannelDictionary implementations should never create ChannelDefinition
 * objects directly. Instead, this factory should be employed. In addition, all
 * ChannelDefinition objects implement the IChannelDefinition interface, and all
 * interaction with these objects in mission adaptations should use this
 * interface, rather than directly interacting with the objects themselves.
 * <p>
 * This class contains only static methods. Once the IChannelDefinition object is
 * returned by this factory, its additional members can be set through the
 * methods in the IChannelDefinition interface.
 *
 *  This factory will NO LONGER search the Channel Definition Table prior
 *  to creating a new definition, and return the existing definition.
 *  IT RETURNS A NEW DEFINITION EVERY TIME.
 *
 *  Use only reflection for object creation
 *
 *
 * @see IChannelDictionary
 * @see IChannelDefinition
 */
public final class ChannelDefinitionFactory
{    
    private static final Constructor<?> SIMPLE_DEFINITION_CONSTRUCTOR;
    private static final Constructor<?> SIMPLE_H_DEFINITION_CONSTRUCTOR;
    private static final Constructor<?> TYPED_DEFINITION_CONSTRUCTOR;
    private static final Constructor<?> TYPED_H_DEFINITION_CONSTRUCTOR;

    /* initialization of cached reflection objects. */
    static {
        try {
            Class<?> c = Class.forName(DictionaryProperties.PACKAGE + "channel.ChannelDefinition");
            SIMPLE_DEFINITION_CONSTRUCTOR = ReflectionToolkit.getConstructor(c, new Class<?> [] 
                    { String.class});
            TYPED_DEFINITION_CONSTRUCTOR = ReflectionToolkit.getConstructor(c, new Class<?> [] 
                    { ChannelType.class, String.class});
            /* Added init of header channel constructors */
            c = Class.forName(DictionaryProperties.PACKAGE + "channel.HeaderChannelDefinition");
            SIMPLE_H_DEFINITION_CONSTRUCTOR = ReflectionToolkit.getConstructor(c, new Class<?> [] 
                    { String.class});
            TYPED_H_DEFINITION_CONSTRUCTOR = ReflectionToolkit.getConstructor(c, new Class<?> [] 
                    { ChannelType.class, String.class});
        } catch (ClassNotFoundException | ReflectionException e) {
            throw new RuntimeException("Cannot locate channel definition class or its constructor", e);
        } 
    }

    /**
     * Private constructor to enforce static nature.
     * 
     */
    private ChannelDefinitionFactory() {}

    /**
     * Creates a ChannelDefinition of the specified definition type, for the
     * given channel data type and ID. The new channel will be given a default
     * DN/Raw formatter based upon its data type.
     * 
     * @param cid
     *            the ID of the new channel
     * @param chType
     *            the data type of the new channel; cannot be
     *            ChannelType.UNKNOWN or null
     * @param cdt
     *            the channel definition type of the new channel
     * 
     * @return IChannelDefinition
     * 
     * @throws IllegalArgumentException
     *             if an invalid ChannelType is supplied
     * 
     *
     */
    public static IChannelDefinition createChannel(final String cid,
            final ChannelType chType,
            final ChannelDefinitionType cdt) 
                    throws IllegalArgumentException {

        IChannelDefinition channel = null;
        String dn_format = null;

        /* Added check and constructor switch for H channels */
        Constructor<?> constructor = TYPED_DEFINITION_CONSTRUCTOR;
        if (cdt == ChannelDefinitionType.H) {
            constructor = TYPED_H_DEFINITION_CONSTRUCTOR;
        }
        try {
            channel = (IChannelDefinition) ReflectionToolkit.createObject(constructor, 
                    new Object[] {chType, cid});
        } catch (ReflectionException e) {
            e.printStackTrace();
            return null;
        }
        channel.setDefinitionType(cdt);

        switch(chType) {
        case SIGNED_INT:
        case STATUS:
        case BOOLEAN:
            dn_format = "%d";
            break;
        case UNSIGNED_INT:
        case TIME:
            dn_format = "%u";
            break;
        case DIGITAL:
            dn_format = "%x";
            break;
        case ASCII:
            dn_format = "%s";
            break;
        case FLOAT:
            dn_format = "%12.2g";
            break;
        default:
            throw new IllegalArgumentException("Illegal channel type " + chType.toString());
        }

        channel.setDnFormat(dn_format);

        return channel;
    }

    /**
     * Creates a ChannelDefinition for the given flight channel data type
     * and ID. The new channel will be given a default DN/Raw formatter based
     * upon its data type. 
     * <p>
     * This method should be used to create flight and flight-derived channel
     * definitions.
     * 
     * @param cid
     *            the ID of the new channel
     * @param chType
     *            the data type of the new channel; cannot be
     *            ChannelType.UNKNOWN or null
     * 
     * @return IChannelDefinition
     * 
     * @throws IllegalArgumentException
     *             if an invalid ChannelType is supplied
     * 
     */
    public static IChannelDefinition createFlightChannel(final String cid,
            final ChannelType chType) 
                    throws IllegalArgumentException
    {

        return createChannel(cid, chType, ChannelDefinitionType.FSW);
    }

    /**
     * Creates/gets a ChannelDefinition for the given SSE channel data type and
     * ID. The new channel will be given a default DN/Raw formatter based upon
     * its data type. 
     * <p>
     * This method should be used to create SSE/GSE and derived-from-SSE/GSE channel
     * definitions.
     * 
     * @param cid
     *            the ID of the new channel
     * @param chType
     *            the data type of the new channel; cannot be
     *            ChannelType.UNKNOWN or null
     * 
     * @return IChannelDefinition
     * 
     * @throws IllegalArgumentException
     *             if an invalid ChannelType is supplied
     * 
     */
    public static IChannelDefinition createSseChannel(final String cid,
            final ChannelType chType) 
                    throws IllegalArgumentException
    {

        return createChannel(cid, chType, ChannelDefinitionType.SSE);
    }


    /**
     * Creates a Monitor ChannelDefinition for the given channel data type
     * and ID. The new channel will be given a default DN/Raw formatter based
     * upon its data type. 
     * <p>
     * This method should be used only for ground-produced station monitor channels.
     * <p>
     * 
     * @param cid
     *            the ID of the new channel
     * @param chType
     *            the data type of the new channel; cannot be
     *            ChannelType.UNKNOWN or null
     * @return IChannelDefinition
     * 
     * @throws IllegalArgumentException
     *             if an invalid ChannelType is supplied
     */
    public static IChannelDefinition createMonitorChannel(final String cid, final ChannelType chType) throws IllegalArgumentException
    {
        return createChannel(cid, chType, ChannelDefinitionType.M);
    }


    /**
     * Creates a Header ChannelDefinition for the given channel data type
     * and ID. The new channel will be given a default DN/Raw formatter based
     * upon its data type.
     * <p>
     * This method should be used only for ground-produced telemetry header channels.
     * <p>
     * 
     * @param cid
     *            the ID of the new channel
     * @param chType
     *            the data type of the new channel; cannot be
     *            ChannelType.UNKNOWN or null
     * @return IHeaderChannelDefinition
     * 
     * @throws IllegalArgumentException
     *             if an invalid ChannelType is supplied
     *             
     */
    public static IHeaderChannelDefinition createHeaderChannel(final String cid, final ChannelType chType) throws IllegalArgumentException {
        return (IHeaderChannelDefinition) createChannel(cid, chType, ChannelDefinitionType.H);
    }


    /**
     * Creates/gets a ChannelDefinition for the given channel ID. The new
     * channel definition will be untyped, and the caller must set the channel
     * data type before the definition will be useful.
     *
     * @param cid
     *            the ID of the new channel
     * @param cdt
     *            the channel definition type of the new channel
     * 
     * @return IChannelDefinition
     * Method NO LONGER fetches existing
     *          definition from the Channel Definition Table. This method will
     *          return a NEW definition every time.
     */   
    public static IChannelDefinition createChannel(final String cid,
            final ChannelDefinitionType cdt)
    {
        try {
            /*  Added check and constructor switch for H channels */
            Constructor<?> constructor = SIMPLE_DEFINITION_CONSTRUCTOR;
            if (cdt == ChannelDefinitionType.H) {
                constructor = SIMPLE_H_DEFINITION_CONSTRUCTOR;
            }
            IChannelDefinition channel = (IChannelDefinition) ReflectionToolkit
                    .createObject(constructor,
                            new Object[] { cid });
            channel.setDefinitionType(cdt);
            return channel;
        } catch (ReflectionException e) {
            e.printStackTrace();
            return null;
        }
    }


    /**
     * Creates a flight ChannelDefinition for the given channel ID. The new
     * channel definition will be untyped, and the caller must set the channel
     * data type before the definition will be useful.
     * 
     * This method should be used to create flight, derived-from-flight channel
     * definitions.
     * <p>
     * 
     * @param cid
     *            the ID of the new channel
     * 
     * @return IChannelDefinition
     */
    public static IChannelDefinition createFlightChannel(final String cid)
    {
        return createChannel(cid, ChannelDefinitionType.FSW);
    }

    /**
     * Creates an SSE ChannelDefinition for the given channel ID. The new
     * channel definition will be untyped, and the caller must set the channel
     * data type before the definition will be useful.
     * 
     * This method should be used to create SSE/GSE and derived-from-SSE/GSE channel
     * definitions.
     * <p>
     * 
     * @param cid
     *            the ID of the new channel
     * 
     * @return IChannelDefinition
     */
    public static IChannelDefinition createSseChannel(final String cid)
    {
        return createChannel(cid, ChannelDefinitionType.SSE);
    }

    /**
     * Creates a Monitor ChannelDefinition for the given channel ID. The new channel
     * definition will be untyped, and the caller must set the channel data type
     * before the definition will be useful. If the definition of the channel 
     * already exists in the master channel definition table, then that definition 
     * object will be returned.
     *
     * <p>
     * This method should be used to create only ground generated station monitor channels.
     *<p>
     * @param cid the ID of the new channel
     * @return IChannelDefinition
     */
    public static IChannelDefinition createMonitorChannel(final String cid) {
        return createChannel(cid, ChannelDefinitionType.M);
    }

    /**
     * Creates a Header ChannelDefinition for the given channel ID. The new channel
     * definition will be untyped, and the caller must set the channel data type
     * before the definition will be useful. 
     * <p>
     * This method should be used to create only ground generated telemetry header channels.
     *<p>
     * @param cid the ID of the new channel
     * @return IHeaderChannelDefinition
     * 
     */
    public static IHeaderChannelDefinition createHeaderChannel(final String cid) {
        return (IHeaderChannelDefinition) createChannel(cid, ChannelDefinitionType.H);
    }

}
