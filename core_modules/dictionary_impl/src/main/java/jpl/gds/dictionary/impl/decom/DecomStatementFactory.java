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
package jpl.gds.dictionary.impl.decom;

import java.nio.ByteOrder;

import jpl.gds.dictionary.api.channel.IChannelDefinition;
import jpl.gds.dictionary.api.decom.IChannelStatementDefinition;
import jpl.gds.dictionary.api.decom.IDecomStatementFactory;
import jpl.gds.dictionary.api.decom.IOffsetStatementDefinition;
import jpl.gds.dictionary.api.decom.ISkipStatementDefinition;
import jpl.gds.dictionary.api.decom.ISwitchStatementDefinition;
import jpl.gds.dictionary.api.decom.IVariableStatementDefinition;
import jpl.gds.dictionary.api.decom.IWidthStatementDefinition;
import jpl.gds.dictionary.api.decom.IOffsetStatementDefinition.OffsetType;
import jpl.gds.dictionary.api.decom.params.*;
import jpl.gds.dictionary.api.decom.types.*;
import jpl.gds.dictionary.api.decom.types.IMoveStatementDefinition.Direction;
import jpl.gds.dictionary.impl.decom.types.*;

/**
 * Implementation factory for decom statements.
 *
 */
public class DecomStatementFactory implements IDecomStatementFactory {

	@Override
	public IBooleanDefinition createBooleanDefinition(BooleanParams params) {
		return new BooleanDefinition(params);
	}

	@Override
	public IFloatingPointDefinition createFloatingPointDefinition(FloatingPointParams params) {
		return new FloatingPointDefinition(params);
	}

	@Override
	public IIntegerDefinition createIntegerDefinition(IntegerParams params) {
		return new IntegerDefinition(params);
	}

	@Override
	public IStringDefinition createStringDefinition(StringParams params) {
		return new StringDefinition(params);
	}

	@Override
	public IDynamicArrayDefinition createDynamicArrayDefinition(DynamicArrayParams params) {
		return new DynamicArrayDefinition(params);
	}

	@Override
	public IRepeatBlockDefinition createRepeatBlock(RepeatBlockParams params) {
		return new RepeatBlockDefinition(params);
	}

	@Override
	public IMoveStatementDefinition createMoveStatement(int numBits, Direction offsetType, int multiplier) {
		return new FixedMoveStatementDefinition(numBits, offsetType, multiplier);
	}

	@Override
	public IEnumDataDefinition createEnumDataDefinition(EnumDataParams enumDataParams) {
		return new EnumDataDefinition(enumDataParams);
	}

	@Override
	public IOpcodeDefinition createOpcodeDefinition(IntegerParams opcodeParams) {
		return new OpcodeDefinition(opcodeParams);
	}

	@Override
	public ICaseBlockDefinition createCaseBlock(CaseParams openCase) {
		return new CaseBlockDefinition(openCase);
	}

	@Override
	public IAlgorithmInvocation createAlgoritmInvocation(AlgorithmParams algorithmParams) {
		return new AlgorithmInvocation(algorithmParams);
	}

	@Override
	public ITimeDefinition createTimeDefinition(TimeParams timeParams) {
		return new TimeDefinition(timeParams);
	}

	@Override
	public IStaticArrayDefinition createStaticArrayDefinition(StaticArrayParams staticArrayParams) {
		return new StaticArrayDefinition(staticArrayParams);
	}

	@Override
	public IDecomMapReference createDecomMapReference(DecomMapReferenceParams params) {
		return new DecomMapReference(params);
	}

	@Override
	public IChannelStatementDefinition createChannelStatement(IChannelDefinition def, int width, int offset) {
		return new ChannelStatementDefinition(def, width, offset);
	}

	@Override
	public ISkipStatementDefinition createSkipStatement(int bitsToSkip) {
		return new SkipStatementDefinition(bitsToSkip);
	}

	@Override
	public IWidthStatementDefinition createWidthStatement(int width) {
		return new WidthStatementDefinition(width);
	}

	@Override
	public IOffsetStatementDefinition createDataOffsetStatement() {
		return new OffsetStatementDefinition();
	}

	@Override
	public IOffsetStatementDefinition createOffsetStatement(int offset, OffsetType offType) {
		return new OffsetStatementDefinition(offset, offType);
	}

	@Override
	public IVariableStatementDefinition createVariableStatement(String varName, String refVarName) {
		return new VariableStatementDefinition(varName, refVarName);
	}

	@Override
	public ISwitchStatementDefinition createSwitchStatement(SwitchParams params) {
		return new SwitchStatementDefinition(params);
	}

	@Override
	public ISwitchStatementDefinition createSwitchStatement(String varName, int modulus) {
		return new SwitchStatementDefinition(varName, modulus);
	}

	@Override
	public ISwitchStatementDefinition createSwitchStatement(String varName) {
		return new SwitchStatementDefinition(varName);
	}

	@Override
	public IVariableStatementDefinition createVariableStatement(String name, int width, int offset) {
		return new VariableStatementDefinition(name, width, offset);
	}

	@Override
	public IBreakStatementDefinition createBreakStatement() {
		return new BreakStatementDefinition();
	}

	@Override
	public IByteOrderStatement createByteOrderStatement(ByteOrder order) {
		return new ByteOrderStatement(order);
	}

	@Override
	public IMoveStatementDefinition createMoveStatement(String offsetVariable, Direction offsetType, int multiplier) {
		return new VariableMoveStatementDefinition(offsetVariable, offsetType, multiplier);
	}

	@Override
	public IGroundVariableDefinition createGroundVariableDefinition(String varName, long value) {
		return new GroundVariableDefinition(varName, value);
	}

	@Override
	public IEventRecordDefinition createEventRecordDefinition(DecomDataParams decomDataParams) {
		return new EventRecordDefinition(decomDataParams);
	}

}
