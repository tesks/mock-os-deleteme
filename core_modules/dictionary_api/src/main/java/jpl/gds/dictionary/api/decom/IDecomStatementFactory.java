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

import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.config.DictionaryProperties;
import jpl.gds.dictionary.api.decom.IOffsetStatementDefinition.OffsetType;
import jpl.gds.dictionary.api.decom.params.*;
import jpl.gds.dictionary.api.decom.types.*;
import jpl.gds.dictionary.api.decom.types.IMoveStatementDefinition.Direction;

import java.nio.ByteOrder;

/**
 * IDecomStatementDefinitionFactory is used to create IDecomStatement objects for
 * use in an IChannelDecomDictionary implementation.
 * <br>
 * <br><b>THIS IS A MULTI-MISSION CORE PUBLIC INTERFACE</b> <br>
 * <br>
 * <b>This is a controlled class. It may not be updated without applicable
 * change requests being filed, and approval of project management. A new
 * version tag must be added below with each revision, and both ECR number and
 * author must be included with the version number.</b> <br><br>
 * 
 * A Decom Statement Definition object is the multi-mission representation of a
 * processing statement within an IDecomMapDefinition. There are different
 * types of statements, each of which instructs the telemetry processor how to 
 * either navigate through the data block undergoing decommutation, or
 * or extract a channel sample from the data block. 
 * <p>
 * IChannelDecomDictionary implementations must parse mission-specific decom
 * dictionary files and create IDecomMapStatement objects for the statements
 * found therein. In order to isolate the mission adaptation from changes in the
 * multi-mission core, IChannelDecomDictionary implementations should use this
 * factory to create multi-mission IDecomStatement objects, and define a
 * mission-specific class that implements the interface. All interaction with
 * these objects in mission adaptations should use the IDecomStatement
 * interface, rather than directly interacting with the objects themselves.
 * <p>
 * 
 *
 */

public interface IDecomStatementFactory {
	
	/**
	 * Creates a boolean definition for use in an 
	 * @param params parameters used to initialize the object
	 * @return IBooleanDefinition object
	 */
	public IBooleanDefinition createBooleanDefinition(BooleanParams params);


	/**
	 * 
     * Creates a floating point definition for use in an IDecomMapDefinition.
	 * @param params parameters used to initialize the object
	 * @return IFloatingPointDefinition object
	 */
	public IFloatingPointDefinition createFloatingPointDefinition(FloatingPointParams params);
	
	/**
     * Creates an integer definition for use in an IDecomMapDefinition.
	 * @param params parameters used to initialize the object
	 * @return IIntegerDefinition object
	 */
	public IIntegerDefinition createIntegerDefinition(IntegerParams params);
	
	/**
	 * 
     * Creates a string definition for use in an IDecomMapDefinition.
	 * @param params parameters used to initialize the object
	 * @return IStringDefinition object
	 */
	public IStringDefinition createStringDefinition(StringParams params);
	
	/**
	 * 
     * Creates an enum definition for use in an IDecomMapDefinition.
	 * @param enumDataParams parameters used to initialize the object
	 * @return IEnumDefinition object
	 */
	public IEnumDataDefinition createEnumDataDefinition(EnumDataParams enumDataParams);

	/**
	 * 
     * Creates a opcode definition for use in an IDecomMapDefinition.
	 * @param opcodeParams parameters used to initialize the object
	 * @return {@linkplain IOpcodeDefinition} object
	 */
	public IOpcodeDefinition createOpcodeDefinition(IntegerParams opcodeParams);

	/**
	 * 
     * Creates a case block for use in an IDecomMapDefinition.
	 * @param openCase parameters used to initialize the object
	 * @return {@linkplain ICaseBlockDefinition} object
	 */
	public ICaseBlockDefinition createCaseBlock(CaseParams openCase);

	/**
     * Creates a move statement for use in an IDecomMapDefinition.
	 * @param offsetAmount the decom variable which stores the number of bits associated with the move
	 * @param offsetType the manner in which the offset was specified
	 * @param multiplier the multiplier to apply to the offsetAmount to convert to bits
	 * @return {@linkplain IMoveStatementDefinition} object
	 */
	public IMoveStatementDefinition createMoveStatement(String offsetAmount, Direction offsetType, int multiplier);

	/**
     * Creates a move statement for use in an IDecomMapDefinition.
	 * @param offsetAmount the number of bits associated with the move
	 * @param offsetType the manner in which the offset was specified
	 * @param multiplier the multiplier to apply to the offsetAmount to convert to bits
	 * @return {@linkplain IMoveStatementDefinition} object
	 */
	public IMoveStatementDefinition createMoveStatement(int offsetAmount, Direction offsetType, int multiplier);

	/**
	 * 
     * Creates a repeat block definition for use in an IDecomMapDefinition.
	 * @param params parameters used to initialize the object
	 * @return {@linkplain IRepeatBlockDefinition} object
	 */
	public IRepeatBlockDefinition createRepeatBlock(RepeatBlockParams params);

	/**
     * Creates a algorithm definition for use in an IDecomMapDefinition.
	 * @param algorithmParams parameters used to initialize the object

	 * @return {@linkplain IAlgorithmInvocation}
	 */
	public IAlgorithmInvocation createAlgoritmInvocation(AlgorithmParams algorithmParams);

	/**
	 * 
     * Creates a time definition for use in an IDecomMapDefinition.
	 * @param timeParams parameters used to initialize the object

	 * @return {@linkplain ITimeDefinition} object
	 */
	public ITimeDefinition createTimeDefinition(TimeParams timeParams);

	/**
     * Creates a decom map reference for use in an IDecomMapDefinition.
	 * @param params parameters used to initialize the object
	 * @return {@linkplain IDecomMapReference} object
	 */
	public IDecomMapReference createDecomMapReference(DecomMapReferenceParams params);

    /**
     * Creates a channel statement for use in an IDecomMapDefinition.
     * 
     * @param def the IChannelDefinition object for the channel this statement refers to
     * @param width the bit width of the channel field in the data stream
     * @param offset the offset of the channel field, relative to the current position in the data stream
     * @return IChannelStatementDefinition object
     */
    public IChannelStatementDefinition createChannelStatement(final IChannelDefinition def, final int width, final int offset);

    /**
     * Creates a skip statement for use in an IDecomMapDefinition.
     * 
     * @param bitsToSkip the number of bits to skip in the data stream; must be positive
     * @return ISkipStatementDefinition object
     */
    public ISkipStatementDefinition createSkipStatement(final int bitsToSkip);

    /**
     * Creates a width statement for use in an IDecomMapDefinition.
     * 
     * @param width the width in bits
     * @return IWidthStatementDefinition object
     */
    public IWidthStatementDefinition createWidthStatement(final int width);

    /**
     * Creates an offset statement for use in an IDecomMapDefinition, for the case in
     * which the DATA offset is desired.
     * 
     * @return IOffsetStatementDefinition object
     */
    public IOffsetStatementDefinition createDataOffsetStatement();
    
    /**
     * Creates an offset statement for use in an IDecomMapDefinition, for the case in
     * which a numeric offset is specified.
     * 
     * @param offset the offset value in bits
     * @param offType the type of the offset
     * 
     * @return IOffsetStatementDefinition object
     */
    public IOffsetStatementDefinition createOffsetStatement(final int offset, final OffsetType offType);

    /**
     * Creates a variable statement for use in an IDecomMapDefinition, for the case in which
     * the variable merely refers to another variable.
     * 
     * @param varName the name of the variable
     * @param refVarName the name of the referenced variable 
     * @return IVariableStatementDefinition object
     */
    public IVariableStatementDefinition createVariableStatement(final String varName, final String refVarName);


    /**
     * Create a switch statement for use in an IDecomMapDefinition.
     * @param params the parameter object to initialize the switch statement with
     * @return ISwitchStatementDefinition object
     */
	public ISwitchStatementDefinition createSwitchStatement(SwitchParams params);

    /**
     * Creates a switch statement for use in an IDecomMapDefinition, for the case in which
     * the value of the switch variable is computed using a modulus.
     * 
     * @param varName the variable to be used as the conditional switch value
     * @param modulus the modulus value, as a positive integer
     * 
     * @return ISwitchStatementDefinition object
     */
    public ISwitchStatementDefinition createSwitchStatement(final String varName, final int modulus);


    /**
     * Creates a switch statement for use in an IDecomMapDefinition.
     * 
     * @param varName the variable to be used as the conditional switch value
     * 
     * @return ISwitchStatementDefinition object
     */
	public ISwitchStatementDefinition createSwitchStatement(String varName);

	
	/**
     * Creates a variable statement for use in an IDecomMapDefinition, for the case in which
     * the variable value is extracted from the data stream.
     * 
     * @param varName the name of the variable
     * @param width the bit width of the variable field in the data stream
     * @param offset the offset of the variable field, relative to the current position in the data stream
     * @return IVariableStatementDefinition object
     */
	public IVariableStatementDefinition createVariableStatement(String varName, int width, int offset);

	/**
	 * Creates an IStaticArrayDefinition for use in an IDecomMapDefinitio.
	 * @param staticArrayParams the parameters used to initialize the object
	 * @return IStaticArrayDefinition object
	 */
	public IStaticArrayDefinition createStaticArrayDefinition(StaticArrayParams staticArrayParams);

	/**
	 * Creates an IDynamicArrayDefinition for use in an IDecomMapDefinition.
	 * @param params parameters used to initialize the object
	 * @return IDynamicArrayDefinition object
	 */
	public IDynamicArrayDefinition createDynamicArrayDefinition(DynamicArrayParams params);

	/**
	 * Creates an IBreakStatementDefinition for use in an IDecomMapDefinition.
	 * @return IBreakStatementDefinition object
	 */
	public IBreakStatementDefinition createBreakStatement();

	/**
	 * Create a statement defining the byte order of subsequent data in a decom map
	 * @param order the byte ordering of the data
	 * @return IByteOrderStatement object
	 */
	public IByteOrderStatement createByteOrderStatement(ByteOrder order);
	
	/**
	 * Create a ground variable definition.
	 * @param varName the name of the variable, to be referenced in other decom statements
	 * @param value the numeric value of the variable
	 * @return IGroundVariableDefinition object
	 */
	public IGroundVariableDefinition createGroundVariableDefinition(String varName, long value);

	/**
	 * Get a new instance of and IDecomStatementFactory implementation class.
	 * @return the new IDecomStatementFactory object
	 */
	public static IDecomStatementFactory newInstance() {
		final IDecomStatementFactory factory;
		try {
			Class<?> defClass = Class.forName(DictionaryProperties.PACKAGE + "decom.DecomStatementFactory");
			factory = (IDecomStatementFactory) defClass.newInstance();
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new RuntimeException("Cannot instantiate IDecomStatementFactory concrete instance", e);
		}
		return factory;
	}

	/**
	 * Creates an {@link IEventRecordDefinition}
	 * @param decomDataParams Used to build the EVR definition
	 * @return The {@link IEventRecordDefinition}
	 */
	IEventRecordDefinition createEventRecordDefinition(DecomDataParams decomDataParams);
}
