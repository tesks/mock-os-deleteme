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
package jpl.gds.dictionary.api.alarm;

import java.lang.reflect.Constructor;
import java.util.List;

import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.shared.reflect.ReflectionException;
import jpl.gds.shared.reflect.ReflectionToolkit;

/**
 * AlarmDefinitionFactory is used to create alarm definition and alarm control
 * objects for use in an IAlarmDictionary implementation.
 * <br>
 * <br><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br><br>
 * 
 * An IAlarmDefinition object is the multi-mission representation of a channel
 * alarms. IAlarmDictionary implementations must parse mission-specific alarm
 * definition files and create AlarmDefinition objects for the definitions found
 * therein. In order to isolate the mission adaptation from changes in the
 * multi-mission core, IAlarmDictionary implementations should never create
 * Alarm Definition objects directly. Instead, this factory should be employed.
 * In addition, all Alarm Definition objects implement the IAlarmDefinition
 * interface, and all interaction with alarm objects in mission adaptations
 * should use this interface, rather than directly interacting with the objects
 * themselves.
 * <p>
 * This class contains only static methods. There is one method per alarm type.
 * Once the IAlarmDefinition object is returned by this factory, its additional
 * members can be set through the methods in the IAlarmDefinition interface.
 *
 * use only reflection for object creation
 *
 * 
 * @see IAlarmDictionary
 * @see IAlarmDefinition
 */
public class AlarmDefinitionFactory {

    /*  Added constants for cached reflection objects. */
    private static final Constructor<?> SIMPLE_DEFINITION_CONSTRUCTOR;
    private static final Constructor<?> COMPOUND_DEFINITION_CONSTRUCTOR;
    private static final Constructor<?> COMBINATION_DEFINITION_CONSTRUCTOR;
    private static final Constructor<?> COMBINATION_GROUP_CONSTRUCTOR;
    private static final Constructor<?> COMBINATION_SOURCE_CONSTRUCTOR;
    private static final Constructor<?> COMBINATION_TARGET_CONSTRUCTOR;
    private static final Constructor<?> OFF_BY_ID_CONSTRUCTOR;
    private static final Constructor<?> OFF_BY_CHANNEL_CONSTRUCTOR;

    /* Added initialization of cached reflection objects. */
    static {
        try {
            Class<?> c = Class.forName(DictionaryProperties.PACKAGE + "alarm.AlarmDefinition");
            SIMPLE_DEFINITION_CONSTRUCTOR = ReflectionToolkit.getConstructor(c, new Class<?> [] 
                    { AlarmType.class, String.class, String.class, AlarmLevel.class});
            c = Class.forName(DictionaryProperties.PACKAGE + "alarm.CompoundAlarmDefinition");
            COMPOUND_DEFINITION_CONSTRUCTOR = ReflectionToolkit.getConstructor(c, new Class<?> [] 
                    { String.class, String.class });
            c = Class.forName(DictionaryProperties.PACKAGE + "alarm.CombinationAlarmDefinition");
            COMBINATION_DEFINITION_CONSTRUCTOR = ReflectionToolkit.getConstructor(c, new Class<?> [] 
                    { String.class, AlarmLevel.class });
            c = Class.forName(DictionaryProperties.PACKAGE + "alarm.CombinationGroupDefinition");
            COMBINATION_GROUP_CONSTRUCTOR = ReflectionToolkit.getConstructor(c, new Class<?> [] 
                    { ICombinationAlarmDefinition.class, AlarmCombinationType.class, String.class });
            c = Class.forName(DictionaryProperties.PACKAGE + "alarm.CombinationAlarmSourceProxyDefinition");
            COMBINATION_SOURCE_CONSTRUCTOR = ReflectionToolkit.getConstructor(c, new Class<?> [] 
                    { ICombinationAlarmDefinition.class, String.class, IAlarmDefinition.class });
            c = Class.forName(DictionaryProperties.PACKAGE + "alarm.CombinationAlarmTargetProxyDefinition");
            COMBINATION_TARGET_CONSTRUCTOR = ReflectionToolkit.getConstructor(c, new Class<?> [] 
                    { ICombinationAlarmDefinition.class, String.class, boolean.class });
            c = Class.forName(DictionaryProperties.PACKAGE + "alarm.AlarmOffControl");
            OFF_BY_ID_CONSTRUCTOR = ReflectionToolkit.getConstructor(c, new Class<?> [] 
                    { String.class});
            OFF_BY_CHANNEL_CONSTRUCTOR = ReflectionToolkit.getConstructor(c, new Class<?> [] 
                    { String.class, AlarmLevel.class });
        } catch (ClassNotFoundException | ReflectionException e) {
            throw new RuntimeException("Cannot locate alarm definition class or its constructor", e);
        } 
    }

    /**
     * Creates a simple alarm definition by reflection.
     * 
     * @param type
     *            the AlarmType
     * @param alarmId
     *            the alarm ID string
     * @param channelId
     *            the channel ID of the channel the alarm applies to
     * @param level
     *            the Alarmlevel
     * @return new IAlarmDefinition object
     * 
     */
    private static IAlarmDefinition createSimpleDefinition(
            final AlarmType type, final String alarmId, final String channelId,
            final AlarmLevel level) {
        try {
            return (IAlarmDefinition) ReflectionToolkit.createObject(
                    SIMPLE_DEFINITION_CONSTRUCTOR, new Object[] { type,
                            alarmId, channelId, level });
        } catch (ReflectionException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Creates a new high alarm definition object.
     * 
     * @param alarmId
     *            a unique identifier for the alarm. If null, will be computed.
     * @param channelId
     *            the ID of the channel to which the alarm applies
     * @param level
     *            level of the alarm; if null, will be set to AlarmLevel.NONE.
     * @param highLimit
     *            the high value limit for the new alarm
     * 
     * @return IAlarmDefinition object
     */
    public static IAlarmDefinition createHighAlarm(final String alarmId,
            final String channelId, final AlarmLevel level, final double highLimit) {
        IAlarmDefinition alarm = createSimpleDefinition(AlarmType.HIGH_VALUE_COMPARE, alarmId, channelId, level);
        if (alarm != null) {
            alarm.setUpperLimit(highLimit);
        }
        return alarm;
    }

    /**
     * Creates a new low alarm definition object.
     * 
     * @param alarmId
     *            a unique identifier for the alarm. If null, will be computed.
     * @param channelId
     *            the ID of the channel to which the alarm applies
     * @param level
     *            level of the alarm; if null, will be set to AlarmLevel.NONE.
     * @param lowLimit
     *            the low value limit for the new alarm
     * 
     * @return IAlarmDefinition object
     */
    public static IAlarmDefinition createLowAlarm(final String alarmId,
            final String channelId, final AlarmLevel level, final double lowLimit) {
        IAlarmDefinition alarm = createSimpleDefinition(AlarmType.LOW_VALUE_COMPARE, alarmId, channelId, level);
        if (alarm != null) {
            alarm.setLowerLimit(lowLimit);
        }
        return alarm;
    }

    /**
     * Creates a new inclusive range alarm definition object.
     * 
     * @param alarmId
     *            a unique identifier for the alarm. If null, will be computed.
     * @param channelId
     *            the ID of the channel to which the alarm applies
     * @param level
     *            level of the alarm; if null, will be set to AlarmLevel.NONE.
     * @param lowLimit
     *            the low value limit for the new alarm
     * @param highLimit
     *            the high value limit for the new alarm
     * 
     * @return IAlarmDefinition object
     */
    public static IAlarmDefinition createInclusiveRangeAlarm(final String alarmId,
            final String channelId, final AlarmLevel level, final double lowLimit,
            final double highLimit) {
        IAlarmDefinition alarm = createSimpleDefinition(AlarmType.INCLUSIVE_COMPARE, alarmId, channelId, level);
        if (alarm != null) {
            alarm.setLowerLimit(lowLimit);
            alarm.setUpperLimit(highLimit);
        }
        return alarm;
    }

    /**
     * Creates a new exclusive range alarm definition object.
     * 
     * @param alarmId
     *            a unique identifier for the alarm. If null, will be computed.
     * @param channelId
     *            the ID of the channel to which the alarm applies
     * @param level
     *            level of the alarm; if null, will be set to AlarmLevel.NONE.
     * @param lowLimit
     *            the low value limit for the new alarm
     * @param highLimit
     *            the high value limit for the new alarm
     * 
     * @return IAlarmDefinition object
     */
    public static IAlarmDefinition createExclusiveRangeAlarm(final String alarmId,
            final String channelId, final AlarmLevel level, final double lowLimit,
            final double highLimit) {
        IAlarmDefinition alarm = createSimpleDefinition(AlarmType.EXCLUSIVE_COMPARE, alarmId, channelId, level);
        if (alarm != null) {
            alarm.setLowerLimit(lowLimit);
            alarm.setUpperLimit(highLimit);
        }
        return alarm;
    }

    /**
     * Creates a new mask alarm definition object.
     * 
     * @param alarmId
     *            a unique identifier for the alarm. If null, will be computed.
     * @param channelId
     *            the ID of the channel to which the alarm applies
     * @param level
     *            level of the alarm; if null, will be set to AlarmLevel.NONE.
     * @param mask
     *            the mask for the new alarm
     * 
     * @return IAlarmDefinition object
     */
    public static IAlarmDefinition createMaskAlarm(final String alarmId,
            final String channelId, final AlarmLevel level, final long mask) {
        IAlarmDefinition alarm = createSimpleDefinition(AlarmType.MASK_COMPARE, alarmId, channelId, level);
        if (alarm != null) {
            alarm.setValueMask(mask);
        }
        return alarm;
    }

    /**
     * Creates a new digital alarm definition object.
     * 
     * @param alarmId
     *            a unique identifier for the alarm. If null, will be computed.
     * @param channelId
     *            the ID of the channel to which the alarm applies
     * @param level
     *            level of the alarm; if null, will be set to AlarmLevel.NONE.
     * @param valueMask
     *            the value mask for the new alarm
     * @param validMask
     *            the valid mask for the new alarm
     * 
     * @return IAlarmDefinition object
     */
    public static IAlarmDefinition createDigitalAlarm(final String alarmId,
            final String channelId, final AlarmLevel level, final long valueMask,
            final long validMask) {
        IAlarmDefinition alarm = createSimpleDefinition(AlarmType.DIGITAL_COMPARE, alarmId, channelId, level);
        if (alarm != null) {
            alarm.setValueMask(valueMask);
            alarm.setDigitalValidMask(validMask);
        }
        return alarm;
    }

    /**
     * Creates a new change alarm definition object.
     * 
     * @param alarmId
     *            a unique identifier for the alarm. If null, will be computed.
     * @param channelId
     *            the ID of the channel to which the alarm applies
     * @param level
     *            level of the alarm; if null, will be set to AlarmLevel.NONE.
     * 
     * @return IAlarmDefinition object
     */
    public static IAlarmDefinition createChangeAlarm(final String alarmId,
            final String channelId, final AlarmLevel level) {
        IAlarmDefinition alarm = createSimpleDefinition(AlarmType.VALUE_CHANGE, alarmId, channelId, level);
        return alarm;
    }

    /**
     * Creates a new delta alarm definition object.
     * 
     * @param alarmId
     *            a unique identifier for the alarm. If null, will be computed.
     * @param channelId
     *            the ID of the channel to which the alarm applies
     * @param level
     *            level of the alarm; if null, will be set to AlarmLevel.NONE.
     * @param deltaLimit
     *            the delta value for the new alarm
     * 
     * @return IAlarmDefinition object
     */
    public static IAlarmDefinition createDeltaAlarm(final String alarmId,
            final String channelId, final AlarmLevel level, final double deltaLimit) {
        IAlarmDefinition alarm = createSimpleDefinition(AlarmType.VALUE_DELTA, alarmId, channelId, level);
        if (alarm != null) {
            alarm.setDeltaLimit(deltaLimit);
        }
        return alarm;
    }

    /**
     * Creates a new state alarm definition object.
     * 
     * @param alarmId
     *            a unique identifier for the alarm. If null, will be computed.
     * @param channelId
     *            the ID of the channel to which the alarm applies
     * @param level
     *            level of the alarm; if null, will be set to AlarmLevel.NONE.
     * @param alarmStates
     *            the state values for the new alarm
     * 
     * @return IAlarmDefinition object
     */
    public static IAlarmDefinition createStateAlarm(final String alarmId,
            final String channelId, final AlarmLevel level, final List<Long> alarmStates) {
        IAlarmDefinition alarm = createSimpleDefinition(AlarmType.STATE_COMPARE, alarmId, channelId, level);
        if (alarm != null) {
            alarm.addAlarmStates(alarmStates);
        }
        return alarm;
    }

    /**
     * Creates a new compound alarm definition object.
     * 
     * @param alarmId
     *            a unique identifier for the alarm. If null, will be computed.
     * @param channelId
     *            the ID of the channel to which the alarm applies
     * 
     * @return ICompoundAlarmDefinition object
     */
    public static ICompoundAlarmDefinition createCompoundAlarm(final String alarmId,
            final String channelId) {
        try {
            return (ICompoundAlarmDefinition) ReflectionToolkit.createObject(COMPOUND_DEFINITION_CONSTRUCTOR, 
                    new Object[] {alarmId, channelId});
        } catch (ReflectionException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Create a combination alarm definition object.
     * 
     * @param alarmId
     *            the unique alarm ID for the combination
     * @param level
     *            the target level of the alarm
     * @return ICombinationAlarmDefinition object
     */
    public static ICombinationAlarmDefinition createCombinationAlarm(
            final String alarmId, final AlarmLevel level) {
        try {
            return (ICombinationAlarmDefinition) ReflectionToolkit.createObject(COMBINATION_DEFINITION_CONSTRUCTOR, 
                    new Object[] {alarmId, level});
        } catch (ReflectionException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Creates a new combination alarm group definition. Combination groups
     * combine a set of source alarms into a single alarm using a boolean
     * operator.
     * 
     * @param parent
     *            the parent combination alarm definition for this group
     * @param operator
     *            the combination operator
     * @param groupId
     *            the unique ID for this group
     * @return ICombinationGroup object
     */
    public static ICombinationGroup createCombinationAlarmGroup(
            final ICombinationAlarmDefinition parent,
            final AlarmCombinationType operator, final String groupId) {
        try {
            return (ICombinationGroup) ReflectionToolkit.createObject(COMBINATION_GROUP_CONSTRUCTOR, new Object[] {
                    parent, operator, groupId});
        } catch (ReflectionException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Creates a combination alarm source proxy definition. Combination source
     * proxies are alarm definitions that are attached to a combination alarm
     * definition in order to define a source channel alarm that is used to
     * compute the combination. They are basically a wrapper around a standard
     * IAlarmDefinition.
     * 
     * @param parent
     *            the parent combination alarm definition for this source proxy
     * @param actualAlarm
     *            the IAlarmDefinition on the source channel to wrap in the
     *            proxy
     * @return ICombinationSource object
     */
    public static ICombinationSource createCombinationSourceAlarm(
            final ICombinationAlarmDefinition parent,
            final IAlarmDefinition actualAlarm) {
        try {
            return (ICombinationSource) ReflectionToolkit.createObject(COMBINATION_SOURCE_CONSTRUCTOR, new Object[] {
                    parent, actualAlarm.getChannelId(), actualAlarm});
        } catch (ReflectionException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Creates a combination alarm target proxy definition. Combination target
     * proxies are alarm definitions that are attached to a combination alarm
     * definition in order to define a target channel and which field in the
     * channel to alarm.
     * 
     * @param parent
     *            the parent combination alarm definition for this target proxy
     * @param channelId
     *            the ID of the target channel
     * @param isOnDn
     *            true to alarm the DN of the target, false to alarm the EU
     * @return ICombinationTarget object
     * 
     */
    public static ICombinationTarget createCombinationTargetAlarm(
            final ICombinationAlarmDefinition parent, final String channelId,
            final boolean isOnDn) {
        try {
            return (ICombinationTarget) ReflectionToolkit.createObject(COMBINATION_TARGET_CONSTRUCTOR, new Object[] {
                    parent, channelId, isOnDn});
        } catch (ReflectionException e) {
            e.printStackTrace();
            return null;
        }

    }

    /**
     * Creates an off control for a specific alarm ID.
     * 
     * @param alarmId
     *            the ID of the alarm to turn off
     * @return IAlarmOffControl object
     */
    public static IAlarmOffControl createOffControlForAlarm(final String alarmId) {
        try {
            return (IAlarmOffControl) ReflectionToolkit.createObject(OFF_BY_ID_CONSTRUCTOR, new Object[] {
                    alarmId});
        } catch (ReflectionException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Creates an off control for a specific channel ID, and optionally, an
     * alarm level.
     * 
     * @param channelId
     *            the ID of the channel for which to turn off alarms
     * @param level
     *            the level of alarms to turn off for the channel
     * 
     * @return IAlarmOffControl object
     */
    public static IAlarmOffControl createOffControlForChannel(
            final String channelId, final AlarmLevel level) {
        try {
            return (IAlarmOffControl) ReflectionToolkit.createObject(OFF_BY_CHANNEL_CONSTRUCTOR, new Object[] {
                    channelId, level});
        } catch (ReflectionException e) {
            e.printStackTrace();
            return null;
        }
    }

}
