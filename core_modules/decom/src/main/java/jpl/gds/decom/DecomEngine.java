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

import jpl.gds.decom.algorithm.IDecommutator;
import jpl.gds.decom.algorithm.ITransformer;
import jpl.gds.decom.algorithm.IValidator;
import jpl.gds.decom.exception.DecomAlgorithmNotFoundException;
import jpl.gds.decom.exception.DecomException;
import jpl.gds.decom.exception.MissingDecomVariableException;
import jpl.gds.dictionary.api.channel.ChannelType;
import jpl.gds.dictionary.api.decom.*;
import jpl.gds.dictionary.api.decom.types.*;
import jpl.gds.dictionary.api.decom.types.IFloatingPointDefinition.FloatEncoding;
import jpl.gds.dictionary.api.decom.types.IFloatingPointDefinition.Precision;
import jpl.gds.dictionary.api.decom.types.IMoveStatementDefinition.Direction;
import jpl.gds.dictionary.api.decom.types.IRepeatBlockDefinition.LengthType;
import jpl.gds.dictionary.api.decom.types.IStringDefinition.StringEncoding;
import jpl.gds.shared.algorithm.AlgorithmConfig;
import jpl.gds.shared.algorithm.AlgorithmManager;
import jpl.gds.shared.gdr.GDR;
import jpl.gds.shared.log.Loggers;
import jpl.gds.shared.log.TraceManager;
import jpl.gds.shared.log.Tracer;
import jpl.gds.shared.time.ISclk;
import jpl.gds.shared.time.ISclkExtractor;
import jpl.gds.shared.time.SclkExtractorManager;
import jpl.gds.shared.types.BitBuffer;
import org.springframework.context.ApplicationContext;

import java.nio.BufferUnderflowException;
import java.nio.InvalidMarkException;
import java.util.*;
import java.util.Map.Entry;

/**
 * This class drives generic decom, but does not interpret the data besides decoding raw bits.
 * It provides an event-driven API in the style of the listener design pattern; listeners can
 * register with a decom engine and receive callbacks each time a virtual decom node is reached
 * by the engine.  Those listeners may do whatever processing they like, although they should be aware
 * that the DecomEngine's performance will be affected by their actions.
 * 
 * This class is stateful and not thread safe.
 *
 */
public class DecomEngine {

	private int defaultWidth;
	private final List<IDecomListener> listeners= new ArrayList<>();
	private final StringBuilder errorBuilder = new StringBuilder(40);
	private BitBuffer buffer;
	private final IDecomDelegate decomDelegate;
	
	private final Stack<BitBuffer> bufferStack = new Stack<>();

	/**
	 * You can think of this like a call stack. The engine executes the next statement yielded
	 * by the iterator at the top of the stack. Any statements that contain other statements
	 * (e.g. arrays, repeat blocks, etc.) push an iterator from their list of children statements
	 * onto the stack, so the first child of such a statement executes it next.  
	 * When an iterator runs out of elements, it is popped and execution resumes at the new top of the stack,
	 * until the stack is empty which marks the end of decom.
	 */
	private final Stack<DecomStackFrame> executionStack = new Stack<>();

	private final IChannelDecomDefinitionProvider dict;

	private final SclkExtractorManager sclkExtractorManager;
	private final AlgorithmManager<ITransformer> transformerManager;
	private final AlgorithmManager<IValidator> validatorManager;
	private final AlgorithmManager<IDecommutator> decomManager;

    private final Tracer                          log;
	
    static class DoNothingDelegate implements IDecomDelegate {
		@Override
		public void decom(final IDecommutator algorithm, final BitBuffer data, final Map<String, Object> args) {
			algorithm.decom(data, args);
		}
	}

	/**
     * Create a new instance with the provided dictionary and delegate.
     * 
     * @param appContext
     *            The current application context
     * @param delegate
     *            the delegate object that will invoke decommutation algorithms
     *            on behalf the of the engine
     */
	public DecomEngine(final ApplicationContext appContext, final IDecomDelegate delegate) {

		this(delegate, appContext.getBean(AlgorithmConfig.class),
				appContext.getBean(IChannelDecomDefinitionProvider.class),
				appContext.getBean(SclkExtractorManager.class),
				TraceManager.getTracer(appContext, Loggers.TLM_EHA));
	}

	/**
	 * Create a new instance with the provided dictionary and delegate. The engine
	 * will invoke custom decommutation algorithms itself, so the output of that decommutation
	 * algorithm will not be available to this engine's user.
	 * @param appContext the Spring application context
	 */
	public DecomEngine(final ApplicationContext appContext) {
		this(appContext, new DoNothingDelegate());

	}

	/**
	 * Creates a new instance
	 * @param decomDelegate the delegate object that will invoke decommutation algorithms
	 *      *            on behalf the of the engine
	 * @param algorithmConfig algorithm manager
	 * @param decomDefinitionProvider the channel decom definition provider
	 * @param sclkExtractorManager sclk extraction manager
	 * @param tracer logger
	 */
	DecomEngine(final IDecomDelegate decomDelegate, AlgorithmConfig algorithmConfig,
					   IChannelDecomDefinitionProvider decomDefinitionProvider,
					   SclkExtractorManager sclkExtractorManager,
					   Tracer tracer) {

		this.dict = decomDefinitionProvider;
		this.sclkExtractorManager = sclkExtractorManager;
		this.log = tracer;
		this.decomDelegate = decomDelegate;

		this.transformerManager= new AlgorithmManager<>(ITransformer.class, "transformers", algorithmConfig);
		this.validatorManager = new AlgorithmManager<>(IValidator.class, "validators", algorithmConfig);
		this.decomManager = new AlgorithmManager<>(IDecommutator.class, "decommutators", algorithmConfig);
	}

	/**
	 * Perform decommutation. Any listeners registered with the instance will receive callbacks
	 * for decom events that are hit. Unlike {@link #prepareForDecom(IDecomMapDefinition, byte[], int, int)},
	 * this immediately executes the decom map in its entirety.
	 * @param map the decom map describing the data
	 * @param data the data being decommed
	 * @param startingOffset the bit offset to start decommutation at in the data
	 * @param length the length, in bits, of the data to decom
	 * @throws DecomException if any errors occur in processing
	 */
	public void decom(final IDecomMapDefinition map, final byte[] data, final int startingOffset, final int length)
			throws DecomException {
		prepareForDecom(map, data, startingOffset, length);
		executeStatements();
	}
	
	/**
	 * Set the engine up for the next decommutation run, but do not begin execution.  This
	 * should be used if the caller wishes to have control over when statements get executed.
	 * @param map the initial map used to decom data
	 * @param data the byte array containing data being decommutated
	 * @param startingOffset the starting offset, in bits, of the first bit to be extracted by decommutation
	 * @param length the number of bits to be decommed from the data array
	 */
	public void prepareForDecom(final IDecomMapDefinition map, final byte[] data, final int startingOffset, final int length) {
		buffer = BitBuffer.wrap(data, startingOffset, length);
		errorBuilder.setLength(0);
		executionStack.clear();
		bufferStack.clear();
		executionStack.push(new DecomStackFrame(map.getStatementsToExecute(), map.getId(), 
				(f) -> {
					executionStack.pop();
					for (final IDecomListener l : listeners) {
						l.onMapEnd(map);
					}
				}
		));
		
	}

	/**
	 * Execute the next decom statement from the map. Use {@link #prepareForDecom(IDecomMapDefinition, byte[], int, int)}
	 * to set up the engine before calling this method.
	 * @throws DecomException if there are no statements to execute, or if an exception is thrown while executing the next statement
	 */
	public void step() throws DecomException {
		if (executionStack.empty()) {
			throw new DecomException("No more statements to execute");
		}
		if(executionStack.peek().hasNext()) {
			executeStatement(executionStack.peek().next());
		} else {
			executionStack.peek().exit();
		}
	}

	/**
	 * Get the bit offset of the next bit to be processed by the engine
	 * @return the offset in bits
	 */
	public int getOffset() {
		return buffer.position();
	}
	
	/**
	 * Main driver of decom.  Until the iterator stack is empty, keep getting the next
	 * element from the iterator on the stack.  Pop an iterator if it is out of elements.
	 * @throws DecomException 
	 */
	private void executeStatements() throws DecomException {
		while (!executionStack.empty()) {
			step();
		}
	}

	private void executeStatement(final IDecomStatement statement) throws DecomException {
		if (statement instanceof IDecomDataDefinition || statement instanceof IAlgorithmInvocation) {
			while (buffer.position() == buffer.limit()) {
				if (bufferStack.empty()) {
					throw new DecomException("Ran out of data during decommutation");
				} else {
					buffer = bufferStack.pop();
				}
			}
		}
		try {
			if (statement instanceof IWidthStatementDefinition) {
				handleStatement((IWidthStatementDefinition) statement);
			} else if (statement instanceof IOffsetStatementDefinition) {
				handleStatement((IOffsetStatementDefinition) statement);
			} else if (statement instanceof ISkipStatementDefinition) {
				handleStatement((ISkipStatementDefinition) statement);
			} else if (statement instanceof IChannelStatementDefinition) {
				handleStatement((IChannelStatementDefinition) statement);
			} else if (statement instanceof IVariableStatementDefinition) {
				handleStatement((IVariableStatementDefinition) statement);
			} else if (statement instanceof ISwitchStatementDefinition) {
				handleStatement((ISwitchStatementDefinition) statement);
			} else if (statement instanceof IIntegerDefinition) {
				handleStatement((IIntegerDefinition) statement);
			} else if (statement instanceof IBooleanDefinition) {
				handleStatement((IBooleanDefinition) statement);
			} else if (statement instanceof IBreakStatementDefinition) {
				handleStatement((IBreakStatementDefinition) statement);
			} else if (statement instanceof ICaseBlockDefinition) {
				handleStatement((ICaseBlockDefinition) statement);
			} else if (statement instanceof IDecomMapReference) {
				handleStatement((IDecomMapReference) statement);
			} else if (statement instanceof IDynamicArrayDefinition) {
				handleStatement((IDynamicArrayDefinition) statement);
			} else if (statement instanceof IEnumDataDefinition) {
				handleStatement((IEnumDataDefinition) statement);
			} else if (statement instanceof IFloatingPointDefinition) {
				handleStatement((IFloatingPointDefinition) statement);
			} else if (statement instanceof IFixedMoveStatementDefinition) {
				handleStatement((IFixedMoveStatementDefinition) statement);
			} else if (statement instanceof IVariableMoveStatementDefinition) {
				handleStatement((IVariableMoveStatementDefinition) statement);
			} else if (statement instanceof IOpcodeDefinition) {
				handleStatement((IOpcodeDefinition) statement);
			} else if (statement instanceof IRepeatBlockDefinition) {
				handleStatement((IRepeatBlockDefinition) statement);
			} else if (statement instanceof IStaticArrayDefinition) {
				handleStatement((IStaticArrayDefinition) statement);
			} else if (statement instanceof IStringDefinition) {
				handleStatement((IStringDefinition) statement);
			} else if (statement instanceof ITimeDefinition) {
				handleStatement((ITimeDefinition) statement);
			} else if (statement instanceof IAlgorithmInvocation) {
				handleStatement((IAlgorithmInvocation) statement);
			} else if (statement instanceof IByteOrderStatement) {
				buffer.order(((IByteOrderStatement) statement).getByteOrder());
			} else if (statement instanceof IGroundVariableDefinition) {
				handleStatement((IGroundVariableDefinition) statement);
			} else if (statement instanceof IEventRecordDefinition) {
				handleStatement((IEventRecordDefinition) statement);
			}
			else {
				this.errorBuilder.append("Encountered unexpected statement type: ");
				this.errorBuilder.append(statement.getClass().getName());
				throw new DecomException(errorBuilder.toString());
			}
		} catch (final BufferUnderflowException e) {
			throw new DecomException("Ran out of data during decommutation.");
		}
		// DecomException won't stop processing because of a bad definition
		catch (final InvalidMarkException e){
			throw new DecomException("Invalid data offset encountered during decommutation.");
		}
		catch (final IllegalArgumentException e){
			throw new DecomException("Illegal argument encountered during decommutation - " + e.getMessage());
		}
	}
	
	private void handleStatement(final IOpcodeDefinition statement) throws DecomException {
		final int val = (int) handleNumeric(statement, true);
		for (final IDecomListener l : this.listeners) {
			l.onOpcode(statement, val);
		}
	}
	
	private void handleStatement(final IGroundVariableDefinition def) {
		this.executionStack.peek().putVariable(def.getName(), def.getValue());
	}

	private void beforeData(final IDecomDataDefinition def) {
		if (def.offsetSpecified()) {
			this.buffer.mark().position(buffer.position() + def.getBitOffset());
		}
	}
	
	private void afterData(final IDecomDataDefinition def) {
		if (def.offsetSpecified()) {
			this.buffer.reset();
		}
	}

	private void handleStatement(final IFloatingPointDefinition statement) {
		beforeData(statement);
		if (statement.getPrecision() == Precision.SINGLE) {
			if (statement.getEncoding() == FloatEncoding.IEEE) {
				final float val = this.buffer.getFloat();
				for (final IDecomListener l : this.listeners) {
					l.onFloat(statement, val);
				}
			} else {
				final double val = this.buffer.getMILFloat();
				for (final IDecomListener l : listeners) {
					l.onDouble(statement, val);
				}
			}
		} else {
			if (statement.getEncoding() == FloatEncoding.IEEE) {
				final double val = this.buffer.getDouble();
				for (final IDecomListener l : this.listeners) {
					l.onDouble(statement, val);
				}
			} else {
				final double val = this.buffer.getMILDouble();
				for (final IDecomListener l : this.listeners) {
					l.onDouble(statement, val);
				}
			}
			
		}
		afterData(statement);
		
	}

	private long handleNumeric(final INumericDataDefinition def, final boolean isUnsigned) throws DecomException {
		if (def.getBitLength() > Long.SIZE) {
			throw new DecomException("Invalid bit length for integral field");
		}
		if (def.offsetSpecified()) {
			buffer.mark().position(buffer.position() + def.getBitOffset());
		}
		final long val;
		if (isUnsigned) {
			val = buffer.getUnsignedLong(def.getBitLength());
		} else {
			val = buffer.getLong(def.getBitLength());
		}
		if (def.offsetSpecified()) {
			buffer.reset();
		}
		if (def.shouldStore()) {
			executionStack.peek().putVariable(def.getName(), val);
		}
		return val;
	}

	private void handleStatement(final IEnumDataDefinition statement) throws DecomException {
		final int val = (int) handleNumeric(statement, false);
		for (final IDecomListener l : listeners) {
			l.onEnum(statement, val);
		}
	}

	private void handleStatement(final IIntegerDefinition statement) throws DecomException {
		final long val = handleNumeric(statement, statement.isUnsigned());
		for (final IDecomListener l : listeners) {
			l.onInteger(statement, val);
		}
	}

	private void handleStatement(final IBooleanDefinition statement) {
		final boolean val = buffer.getInt(statement.getBitLength()) != 0 ? true : false;
		for (final IDecomListener l : listeners) {
			l.onBoolean(statement, val);
		}
		
	}

	private Map<String, Object> populateAlgoArgs(final IAlgorithmInvocation statement) throws MissingDecomVariableException {
		final Map<String, Object> args = new HashMap<>(statement.getArgs().size());
		for (final Entry<String, String> entry : statement.getArgs().entrySet()) {
			final Object argValue = executionStack.peek().getValue(entry.getValue());
			args.put(entry.getKey(), argValue);
		}
		return args;
	}

	private void handleStatement(final IAlgorithmInvocation statement) throws DecomException {
		if (statement.getAlgorithmType() == AlgorithmType.DECOMMUTATOR) {
			final Optional<IDecommutator> decomAlgorithm = decomManager.getAlgorithm(statement.getAlgorithmId());
			if (decomAlgorithm.isPresent()) {
				decomDelegate.decom(decomAlgorithm.get(), buffer, populateAlgoArgs(statement));
			} else {
				throw new DecomAlgorithmNotFoundException(decomManager.getFailureCauseFor(statement.getAlgorithmId()));
			}
		} else if (statement.getAlgorithmType() == AlgorithmType.TRANSFORMER) {
			final Optional<ITransformer> transformerAlgorithm = transformerManager.getAlgorithm(statement.getAlgorithmId());
			if (transformerAlgorithm.isPresent()) {
				final BitBuffer transformedData = transformerAlgorithm.get().transform(buffer, populateAlgoArgs(statement));
				bufferStack.push(buffer);
				buffer = transformedData;
			} else {
				throw new DecomAlgorithmNotFoundException(transformerManager.getFailureCauseFor(statement.getAlgorithmId()));
			}
		} else if (statement.getAlgorithmType() == AlgorithmType.VALIDATOR) { 
			final Optional<IValidator> validatorAlgorithm = validatorManager.getAlgorithm(statement.getAlgorithmId());
			if (validatorAlgorithm.isPresent()) {
				buffer.mark();
				final boolean isValid = validatorAlgorithm.get().validate(buffer, populateAlgoArgs(statement));
				buffer.reset();
				if (!isValid) {
					throw new DecomException(String.format("ValidationAlgorithm %s failed", statement.getAlgorithmId()));
				}
			} else {
				throw new DecomAlgorithmNotFoundException(validatorManager.getFailureCauseFor(statement.getAlgorithmId()));
			}
		} else {
			throw new DecomException(String.format("Invalid algorithm type %s", statement.getAlgorithmType().toString()));
		}
	}

	private void handleStatement(final ITimeDefinition statement) throws DecomException {
		final Optional<ISclkExtractor> extractor = sclkExtractorManager.getAlgorithm(statement.getAlgorithmId());
		if (extractor.isPresent()) {
			final ISclk sclk = extractor.get().getValueFromBits(buffer, populateAlgoArgs(statement));
			for (final IDecomListener l : listeners) {
				l.onTime(statement, sclk);
			}
		} else {
			throw new DecomAlgorithmNotFoundException(sclkExtractorManager.getFailureCauseFor(statement.getAlgorithmId()));
		}
	}

	private void handleStatement(final IStringDefinition statement) {
		final String data;
		if (statement.getEncoding() == StringEncoding.ASCII) {
			data = buffer.getAsciiString(statement.getLength());
		} else if (statement.getEncoding() == StringEncoding.UTF8) {
			data = buffer.getUtf8String(statement.getLength());
		} else {
			throw new IllegalStateException("Decom encountered unknown string encoding: " + statement.getEncoding().toString());
		}
		if (statement.shouldStore()) {
			this.executionStack.peek().putVariable(statement.getName(), data);
		}
		for (final IDecomListener l : listeners) {
			l.onString(statement, data);
		}
	}

	private void handleStatement(final IStaticArrayDefinition statement) {
		final int size = statement.getLength();
		for (final IDecomListener l : listeners) {
			l.onArrayStart(statement);
		}
		final DecomStackFrame frame = new DecomStackFrame(statement.getDataDefinitions(),
				executionStack.peek().getMapId(), (f) -> {
					if (f.getIterationCount() < size) {
						f.loop();
					}
					else {
						for (final IDecomListener l : listeners) {
							l.onArrayEnd(statement);
						}
						executionStack.pop();
					} 
		});
		executionStack.push(frame);
		
	}

	private void handleStatement(final IRepeatBlockDefinition statement) {
		final int startOffset = buffer.position();
		for (final IDecomListener l : listeners) {
			l.onRepeatBlockStart(statement);
		}
		final DecomStackFrame frame = new DecomStackFrame(statement.getStatementsToExecute(), executionStack.peek().getMapId(),
				(f) -> {
					if (statement.getLengthType() != LengthType.ABSENT && buffer.position() >= startOffset + statement.getLength()) {
						for (final IDecomListener l : listeners) {
							l.onRepeatBlockEnd(statement);
						}
						executionStack.pop();
					} else if (f.hitBreak() || buffer.position() == buffer.limit()) {
						// Must have hit break statement
						if (statement.getLengthType() == LengthType.ABSOLUTE) {
							buffer.position(statement.getLength() + startOffset);
						}
						for (final IDecomListener l : listeners) {
							l.onRepeatBlockEnd(statement);
						}
						executionStack.pop();
					} else {
						f.loop();
					}
				});
		executionStack.push(frame);
		
	}

	private void handleStatement(final IFixedMoveStatementDefinition statement) throws DecomException {
		final int originalOffset = buffer.position();
		final int offset = statement.getOffset() * statement.offsetMultiplier();
		final int newPosition;
		if (statement.getDirection() == Direction.FORWARD) {
			newPosition =	buffer.position() + offset; 
		} else if (statement.getDirection() == Direction.BACKWARD ) {
			newPosition = buffer.position() - offset;
		} else {
			newPosition = offset;
		}

		if (newPosition > buffer.limit() || newPosition < 0) {
			final String errorTemplate = "Execution of move statement would result in illegal decom offset: offsetValue=%d multiplier=%d startingOffset=%d newOffset=%d";
			throw new DecomException(String.format(errorTemplate,
					 statement.getOffset(), statement.offsetMultiplier(), buffer.position(), newPosition));
		}
		buffer.position(newPosition);
		
		for (final IDecomListener l : listeners) {
			l.onMove(statement, originalOffset, buffer.position());
		}
		
	}

	private void handleStatement(final IVariableMoveStatementDefinition statement) throws MissingDecomVariableException, DecomException {
		final int originalOffset = buffer.position();
		final int variableValue = ((Long)executionStack.peek().getValue(statement.getOffsetVariable())).intValue();
		final int offset = variableValue * statement.offsetMultiplier();
		final int newPosition;
		if (statement.getDirection() == Direction.FORWARD) {
			newPosition =	buffer.position() + offset; 
		} else if (statement.getDirection() == Direction.BACKWARD ) {
			newPosition = buffer.position() - offset;
		} else {
			newPosition = offset;
		}

		if (newPosition > buffer.limit() || newPosition < 0) {
			final String errorTemplate = "Execution of move statement would result in illegal decom offset: variableName=%s variableValue=%d multiplier=%d startingOffset=%d newOffset=%d";
			throw new DecomException(String.format(errorTemplate,
					statement.getOffsetVariable(), variableValue, statement.offsetMultiplier(), buffer.position(), newPosition));
		}
		buffer.position(newPosition);

		for (final IDecomListener l : listeners) {
			l.onMove(statement, originalOffset, buffer.position());
		}
		
	}

	private void handleStatement(final IDynamicArrayDefinition statement) throws MissingDecomVariableException {
		final long size = (long) executionStack.peek().getValue(statement.getLengthVariableName());
		for (final IDecomListener l : listeners) {
			l.onArrayStart(statement);
		}

		final DecomStackFrame frame = new DecomStackFrame(statement.getDataDefinitions(),
				executionStack.peek().getMapId(), (f) -> {
					if (f.getIterationCount() < size) {
						f.loop();
					} else {
						for (final IDecomListener l : listeners) {
							l.onArrayEnd(statement);
						}
						executionStack.pop();
					}
				});
		executionStack.push(frame);
		
	}
	

	private void handleStatement(final IDecomMapReference statement) throws DecomException {
		final IDecomMapId referenceId = executionStack.peek().getMapId().resolveReference(statement.getMapId());
		final IDecomMapDefinition referencedMap =  dict.getDecomMapById(referenceId);
		// If referenceId is bad, no referencedMap can be retrieved, preventing decom
		if(referencedMap == null){
			throw new DecomException("Unable to retrieve decom map for ID " + referenceId.getFullId());
		}
		for (final IDecomListener l : listeners) {
			l.onMapReference(statement);
			l.onMapStart(referencedMap);
		}
		executionStack.push(new DecomStackFrame(referencedMap.getStatementsToExecute(), referenceId,
				(f) -> {
					for (final IDecomListener l : listeners) {
						l.onMapEnd(referencedMap);
					}
					executionStack.pop();
				})
		);
	}

	private void handleStatement(final ICaseBlockDefinition statement) {
		// Currently no processing or callbacks performed
	}

	private void handleStatement(final IBreakStatementDefinition statement) {
		for (final IDecomListener l : listeners) {
			l.onBreak(statement);
		}
		executionStack.peek().doBreak();
	}

	

	private void handleStatement(final IVariableStatementDefinition statement) throws DecomException {
		final String name = statement.getVariableName();

		if (statement.isExtractionVariable()) {
			final int offset = statement.isOffsetSpecified() ? statement
					.getOffsetToExtract() : buffer.position();
					final int width = statement.getWidthToExtract();

					try {

						if (offset + width > buffer.limit()) {
							// out-of-bounds decom
							this.errorBuilder.append("Attempted to decom variable ")
							.append(name)
							.append(" at bit offset ")
							.append(offset)
							.append(", bit width ")
							.append(width)
							.append(" but packet data length is ")
							.append(buffer.limit())
							.append(" bits.");
							throw new DecomException(this.errorBuilder.toString());

						} else {
							buffer.mark();
							buffer.position(offset);
							final long value = buffer.getUnsignedLong(width);
							buffer.reset();
							for (final IDecomListener l : listeners) {
								l.onVariable(statement, value);
							}
							executionStack.peek().putVariable(name, value);
						}

					} catch (final Exception e) {
						this.errorBuilder.append("Exception extracting variable ");
						this.errorBuilder.append(name);
						this.errorBuilder.append(". ");
						this.errorBuilder.append(e.getMessage());
						throw new DecomException(this.errorBuilder.toString(), e);
					}

		} else {
			final long value = (long) executionStack.peek().getValue(statement.getReferenceVariableName());
			executionStack.peek().putVariable(name, value);
			for (final IDecomListener l : listeners) {
					l.onVariable(statement, value);
			}
		}

	}

	private void handleStatement(final IChannelStatementDefinition statement) throws DecomException {
		final boolean offsetOverridden = statement.offsetSpecified();
		final int offset = offsetOverridden ? statement.getOffset() : buffer.position();
		final boolean widthOverridden = statement.widthSpecified();
		final int width = widthOverridden ? statement.getWidth() : this.defaultWidth;

		if (offset + width > buffer.limit()) {
			// out-of-bounds decom
			this.errorBuilder.append("Attempted to decom ")
			.append(statement.getChannelId())
			.append(" at bit offset ")
			.append(offset)
			.append(", bit width ")
			.append(width)
			.append(" but packet data length is ")
			.append(buffer.limit())
			.append(" bits.");
			throw new DecomException(errorBuilder.toString());

		}
		try {

			final ChannelType chanType = statement.getChannelType();
			if (statement.offsetSpecified()) {
				buffer.mark();
				buffer.position(offset);
			}
			switch (chanType) {
			case STATUS:
				if (statement.getWidth() > Integer.SIZE) {
					throw new IllegalArgumentException("Attempted to decom STATUS channel of length " + statement.getWidth() + "bits");
				}
			case SIGNED_INT: {
				// Decom even signed data as unsigned
				// if length is not divisible by 8 or the offset is not byte-aligned.
				// This emulates pre-R7.4 behavior for channel statements.
				long val;
				if (width % 8 == 0 && offset % 8 == 0) {
					val = buffer.getLong(width);
				} else {
					val = buffer.getUnsignedLong(width);
				}
				for (final IDecomListener l : listeners) {
					l.onChannel(statement, val);
				}
				break;
			}
			case UNSIGNED_INT:
			case DIGITAL:
			case BOOLEAN:
			case TIME: {
				final long val =  buffer.getUnsignedLong(width);
				for (final IDecomListener l : listeners) {
					l.onChannel(statement, val);
				}
				break;
			}

			case FLOAT: {
				if (width == Float.SIZE) {
					final float val = buffer.getFloat();
					for (final IDecomListener l : listeners) {
						l.onChannel(statement, val);
					}
				} else {
					final double val = buffer.getDouble();
					for (final IDecomListener l : listeners) {
						l.onChannel(statement, val);
					}
				}
				break;
			}
			case ASCII:
				final String val = GDR.stringValue(buffer.get(width / Byte.SIZE), 0, width / Byte.SIZE);
				for (final IDecomListener l : listeners) {
					l.onChannel(statement, val);
				}
				break;
			default:
				break;

			}
			if (statement.offsetSpecified()) {
				buffer.reset();
			}
		} catch (final Exception e) {
			this.errorBuilder.append("Exception decommutating channel ");
			this.errorBuilder.append(statement.getChannelId());
			this.errorBuilder.append(". ");
			throw new DecomException(errorBuilder.toString(), e);

		} /**finally {
			buffer.position(offsetOverridden ? buffer.position()
					: buffer.position() + width);

		} */


	}

	private void handleStatement(final ISkipStatementDefinition statement) {
		buffer.position(buffer.position() + statement.getNumberOfBitsToSkip());
	}

	/**
	 * Subscribe a listener to the decom engine.
	 * @param listener the listener that will receive callbacks from the engine
	 */
	public void addListener(final IDecomListener listener) {
		listeners.add(listener);
	}

	private void handleStatement(final IWidthStatementDefinition statement) {
		this.defaultWidth = statement.getWidth();
	}

	private void handleStatement(final IOffsetStatementDefinition statement) throws DecomException {
		if (statement.isDataOffset()) {
			buffer.rewind();
			return;
		}
		final int newPosition;
		switch (statement.getOffsetType()) {
		case ABSOLUTE:
			newPosition = statement.getOffset();
			break;

		case PLUS:
			newPosition = buffer.position() + statement.getOffset();
			break;

		case MINUS:
			newPosition = buffer.position() - statement.getOffset();
			break;

		default:
			this.errorBuilder.append("Encountered unexpected type in offset statement: ")
			.append(statement.getOffsetType());
			throw new DecomException(errorBuilder.toString());

		}
		if (newPosition > buffer.limit() || newPosition < 0) {
			final String errorTemplate = "Execution of move statement would result in illegal decom offset: offsetValue=%d startingOffset=%d newOffset=%d";
			throw new DecomException(String.format(errorTemplate,
					statement.getOffset(), buffer.position(), newPosition));
		}
		buffer.position(newPosition);

	}

	private void handleStatement(final ISwitchStatementDefinition statement) throws DecomException {
		final String variableName = statement.getVariableToSwitchOn();
		final long variableValue = (long) executionStack.peek().getValue(variableName);
		final List<IDecomStatement> statementsUnderCase = statement.getStatementsToExecute(variableValue);

		if (statementsUnderCase == null) {
			this.errorBuilder.append("Switch statement case miss (no default)")
			.append(" varName=")
			.append(variableName)
			.append(" varValue=")
			.append(variableValue);
			throw new DecomException(errorBuilder.toString());
		} else {
			this.listeners.forEach(l -> l.onSwitchStart(statement, variableValue));
			executionStack.push(new DecomStackFrame(statementsUnderCase, executionStack.peek().getMapId(),
					(f) -> {
						this.listeners.forEach(l -> l.onSwitchEnd(statement, variableValue));
						executionStack.pop();
						if (f.hitBreak()) {
							if (!executionStack.empty()) {
								executionStack.peek().doBreak();
							}
						}
					},
					 Optional.ofNullable(this.executionStack.peek())));
		}
	}

	private void handleStatement(IEventRecordDefinition statement) {

		if(statement.offsetSpecified()) {
			this.buffer.mark();
			this.buffer.position(statement.getBitOffset());
		}
		this.listeners.forEach(l -> l.onEvr(statement, buffer));
		if(statement.offsetSpecified()) {
			this.buffer.reset();
		}
	}

}
