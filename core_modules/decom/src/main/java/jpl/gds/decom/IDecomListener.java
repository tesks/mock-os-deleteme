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
package jpl.gds.decom;

import jpl.gds.dictionary.api.decom.IChannelStatementDefinition;
import jpl.gds.dictionary.api.decom.IDecomMapDefinition;
import jpl.gds.dictionary.api.decom.ISwitchStatementDefinition;
import jpl.gds.dictionary.api.decom.IVariableStatementDefinition;
import jpl.gds.dictionary.api.decom.types.*;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.types.BitBuffer;

/**
 * Listener interface for generic decommutation. The {@link DecomEngine} itself provides
 * no functionality other than processing raw data; to perform operations on extracted data,
 * implement this interface and provide an instance to the engine.
 * 
 * This interface is for internal AMPCS usage only.
 * 
 * Each method has an empty default implementation, so implementation classes need not worry about events
 * they do not want to process.
 * 
 */
public interface IDecomListener {

    /**
     * Called upon extraction of enum data.
     * 
     * @param def
     *            the definition object used to extract the value
     * @param val
     *            the value of the enum data
     */
    public default void onEnum(final IEnumDataDefinition def, final int val) {
    }

    /**
     * Called upon extraction of an opcode.
     * 
     * @param def
     *            the definition object used to extract the value
     * @param val
     *            the value of the opcode data
     */
    public default void onOpcode(final IOpcodeDefinition def, final int val) {
    }

    /**
     * Called immediately prior to the extraction of the first data of an array
     * 
     * @param def
     *            the definition of the array
     */
    public default void onArrayStart(final IStaticArrayDefinition def) {
    }

    /**
     * Called immediately following to the extraction of the last data of an array
     * 
     * @param def
     *            the definition of the array
     */
    public default void onArrayEnd(final IStaticArrayDefinition def) {
    }

    /**
     * Called immediately prior to the extraction of the first data of an array
     * 
     * @param def
     *            the definition of the array
     */
    public default void onArrayStart(final IDynamicArrayDefinition def) {
    }

    /**
     * Called immediately following to the extraction of the last data of an array
     * 
     * @param def
     *            the definition of the array
     */
    public default void onArrayEnd(final IDynamicArrayDefinition def) {
    }

    /**
     * Called upon encountering a reference to a decom map.
     * 
     * @param ref
     *            the reference encountered
     */
    public default void onMapReference(final IDecomMapReference ref) {
    }

    /**
     * Called prior to executing the first statement of a map
     * 
     * @param def
     *            the definition of the map about to be executed
     */
    public default void onMapStart(final IDecomMapDefinition def) {
    }

    /**
     * Called immediately following the last statement of a map is executed
     * 
     * @param def
     *            the definition of the map that was executed
     */
    public default void onMapEnd(final IDecomMapDefinition def) {
    }

    /**
     * Called immediately prior to executing the first statement of a repeat block
     * 
     * @param def
     *            the definition of the repeat block about to be executed
     */
    public default void onRepeatBlockStart(final IRepeatBlockDefinition def) {
    }

    /**
     * Called immediately following the termination of a repeat block.
     * 
     * @param def
     *            the definition of the repeat block being terminated
     */
    public default void onRepeatBlockEnd(final IRepeatBlockDefinition def) {
    }

    /**
     * Called upon extraction of a time tag.
     * 
     * @param def
     *            the definition of the time tag
     * @param sclk
     *            the value of the extracted time tag
     */
    public default void onTime(final ITimeDefinition def, final ISclk sclk) {
    }

    /**
     * Called upon extraction of a boolean value
     * 
     * @param def
     *            the definition object used to extract the value
     * @param val
     *            the boolean resulting from evaluating the extracted value
     */
    public default void onBoolean(final IBooleanDefinition def, final boolean val) {
    }

    /**
     * Called upon extraction of a boolean value
     * 
     * @param def
     *            the definition object used to extract the value
     * @param val
     *            the value of the extracted integer value
     */
    public default void onInteger(final IIntegerDefinition def, final long val) {
    }

    /**
     * Called upon extraction of a String value
     * 
     * @param def
     *            the definition object used to extract the value
     * @param val
     *            the value of the extracted String
     */
    public default void onString(final IStringDefinition def, final String val) {
    }

    /**
     * Called immediately prior to execution of the first statement of a switch block
     * 
     * @param def
     *            the definition of the switch block being started
     * @param variableValue
     *            the value of the variable switched on
     */
    public default void onSwitchStart(final ISwitchStatementDefinition def, final long variableValue) {
    }

    /**
     * Called immediately following the exit of a switch block.
     * 
     * @param def
     *            the definition of the switch block being exited
     * @param variableValue
     *            the value of the variable switched on
     */
    public default void onSwitchEnd(final ISwitchStatementDefinition def, final long variableValue) {
    }

    /**
     * Called upon extraction of a channel data number value
     * 
     * @param def
     *            the definition object used to extract the value
     * @param val
     *            the value of the extracted channel data number
     */
    public default void onChannel(final IChannelStatementDefinition def, final long val) {
    }

    /**
     * Called upon extraction of a channel data number value
     * 
     * @param def
     *            the definition object used to extract the value
     * @param val
     *            the value of the extracted channel data number
     */
    public default void onChannel(final IChannelStatementDefinition def, final float val) {
    }

    /**
     * Called upon extraction of a channel data number value
     * 
     * @param def
     *            the definition object used to extract the value
     * @param val
     *            the value of the extracted channel data number
     */
    public default void onChannel(final IChannelStatementDefinition def, final double val) {
    }

    /**
     * Called upon extraction of a channel data number value
     * 
     * @param def
     *            the definition object used to extract the value
     * @param val
     *            the value of the extracted channel data number
     */
    public default void onChannel(final IChannelStatementDefinition def, final String val) {
    };

    /**
     * Called upon extraction of a double value
     * 
     * @param def
     *            the definition object used to extract the value
     * @param val
     *            the value of the extracted double
     */
    public default void onDouble(final IFloatingPointDefinition def, final double val) {
    }

    /**
     * Called upon extraction of a float value
     * 
     * @param def
     *            the definition object used to extract the value
     * @param val
     *            the value of the extracted float
     */
    public default void onFloat(final IFloatingPointDefinition def, final float val) {
    }

    /**
     * Called upon encountering a break statement
     * 
     * @param def
     *            the definition object representing the break statement
     */
    public default void onBreak(final IBreakStatementDefinition def) {
    }

    /**
     * Called upon extraction of a variable value.
     * @param def the definition object used to extract the value
     * @param value the value of the extracted variable
     */
    default void onVariable(final IVariableStatementDefinition def, String value) { }

    /**
     * Called upon extraction of a variable value.
     * 
     * @param def
     *            the definition object used to extract the value
     * @param val
     *            the value of the extracted variable
     */
    public default void onVariable(final IVariableStatementDefinition def, final long val) { }

    /**
     * Called when executing a move statement.
     * 
     * @param def
     *            the definition of the executed move statement
     * @param offsetBefore
     *            the bit offset of the engine before executing the move statement
     * @param offsetAfter
     *            the bit offset of the engine after executing the move statement
     */
    public default void onMove(final IVariableMoveStatementDefinition def, final int offsetBefore,
                               final int offsetAfter) { }

    /**
     * Called when executing a move statement.
     * 
     * @param def
     *            the definition of the executed move statement
     * @param offsetBefore
     *            the bit offset of the engine before executing the move statement
     * @param offsetAfter
     *            the bit offset of the engine after executing the move statment
     */
    public default void onMove(final IFixedMoveStatementDefinition def, final int offsetBefore, final int offsetAfter) { }

    /**
     * Called when executing an event_record statement.
     * @param def the definition of the executed move statement
     * @param buffer the current bit buffer
     */
    default void onEvr(IEventRecordDefinition def, BitBuffer buffer) { }
}
