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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import jpl.gds.decom.exception.MissingDecomVariableException;
import jpl.gds.dictionary.api.decom.IDecomMapId;
import jpl.gds.dictionary.api.decom.IDecomStatement;

/**
 * This class represents an execution context for generic decommutation of data.  It holds
 * its own variable table, an iterator over a list of {@link IDecomStatement} objects
 * that acts like an instruction pointer, and tracks some state such as whether a break statement
 * was hit and how many times the frame has been looped over.
 * 
 * Each stack frame belongs to some decom map and also stores its ID.  The frame may either represent
 * the top-level frame of a decom map itself, or it may represent a frame within that map, such as an 
 * array's frame.  In other words, there may be many stack frames corresponding to one decom map, 
 * though one decom map corresponds to exactly one frame.
 *  
 *
 */
public class DecomStackFrame implements Iterator<IDecomStatement> {
	
	private static final int INITIAL_VARIABLE_TABLE_SIZE = 16;
	private final Optional<DecomStackFrame> parentStackFrame;
	private final Map<String, Object> localVariableTable = new HashMap<>(INITIAL_VARIABLE_TABLE_SIZE);
	private Iterator<? extends IDecomStatement> statementItr;
	private final List<? extends IDecomStatement> cachedStatements;
	private final IDecomMapId mapId;
	private final Consumer<DecomStackFrame> finishAction;
	private boolean doBreak = false;
	private int iterations = 0;
	
	/**
	 * Create a new stack frame.
	 * @param statements the list of statements for this frame to iterate over
	 * @param mapId the ID of of the map this frame belongs to.
	 * @param finishAction the action to call upon exiting this frame the action to call upon exiting this frame.
	 */
	public DecomStackFrame(List<? extends IDecomStatement> statements, IDecomMapId mapId, Consumer<DecomStackFrame> finishAction) {
		this.mapId = mapId;
		this.parentStackFrame = Optional.empty();
		this.finishAction = finishAction;
		statementItr = statements.iterator();
		this.cachedStatements = statements;
	}
	
	/**
	 * Create a new stack frame.  This constructor caches the provided variable table,
	 * so it should be used when the frame is in scope of a previous frame and therefore
	 * should have access to its variables.
	 * @param statements the list of statements for this frame to iterate over
	 * @param mapId the ID of of the map this frame belongs to.
	 * @param finishAction the action to call upon exiting this frame the action to call upon exiting this frame.
	 * @param parentFrame the stack frame containing this stack frame, if it exists.
	 * 
	 */
	public DecomStackFrame(List<? extends IDecomStatement> statements, IDecomMapId mapId,
			Consumer<DecomStackFrame> finishAction, Optional<DecomStackFrame> parentFrame) {
		this.mapId = mapId;
		this.finishAction = finishAction;
		this.parentStackFrame = parentFrame;
		cachedStatements = statements;
		statementItr = statements.iterator();
	}

	@Override
	public boolean hasNext() {
		return statementItr.hasNext();
	}

	@Override
	public IDecomStatement next() { 
		return statementItr.next();
	}
	
	/**
	 * Store a variable with the given name and value.
	 * @param variable the name of the variable
	 * @param value the value of the variable
	 */
	public void putVariable(String variable, long value) {
		localVariableTable.put(variable, value);
	}

	/**
	 * Store a variable with the given name and value.
	 * @param variable the name of the variable
	 * @param value the value of the variable
	 */
	public void putVariable(String variable, String value) {
		this.localVariableTable.put(variable, value);
	}

	/**
	 * Get the value of a variable available to the frame. The frame first looks in a local
	 * table for the variable value, and then looks in the table it was passed when first created,
	 * if one exists. 
	 * @param variable the variable name to get a value for
	 * @return the value of the variable
	 * @throws MissingDecomVariableException if a value was not found for the variable
	 */
	public Object getValue(String variable) throws MissingDecomVariableException {
		Object value = localVariableTable.get(variable);
		if (value != null) {
			return value;
		}
		if (this.parentStackFrame.isPresent()) {
			return this.parentStackFrame.get().getValue(variable);
		}

		throw new MissingDecomVariableException(String.format("No value defined for variable %s", variable));
	}
	
	/**
	 * Get the map ID associated with this frame.
	 * @return the decom map id
	 */
	public IDecomMapId getMapId() {
		return mapId;
	}
	
	/**
	 * Get the local variable table of the frame.
	 * @return the variable table
	 */
	public Map<String, Object> getVariableTable() {
		return localVariableTable;
	}


	/**
	 * Loop the frame. This means a subsequent call to {@link #next()}
	 * will return the first {@link IDecomStatement} associated with the frame.
	 */
	public void loop() {
		statementItr = cachedStatements.iterator();
	}
	
	/**
	 * Get the number of times the statements in this frame has been executed in their entirety.
	 * @return the number of iterations for the frame
	 */
	public int getIterationCount() {
		return iterations;
	}

	/**
	 * Exit the frame and call the finishing action.
	 */
	public void doBreak() {
		doBreak = true;
		exit();
		
	}
	
	/**
	 * Exit the frame.  This increments the number of iterations held internally by this frame and
	 * triggers the finishing action passed to the frame's constructor.  The finishing action
	 * may call the {@link #loop()} method if the frame's statements need to be re-executed again.
	 */
	public void exit() {
		iterations++;
		this.finishAction.accept(this);
	}

	/**
	 * Determine whether the frame is marked for a break, and thus should potentially exit early.
	 * @return true if the frame was told to break
	 */
	public boolean hitBreak() {
		return doBreak;
	}
}
