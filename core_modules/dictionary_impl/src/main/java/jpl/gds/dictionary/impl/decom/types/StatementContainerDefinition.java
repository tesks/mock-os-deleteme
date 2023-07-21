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
package jpl.gds.dictionary.impl.decom.types;

import java.util.Collections;
import java.util.List;

import jpl.gds.dictionary.api.decom.IDecomStatement;
import jpl.gds.dictionary.api.decom.IStatementContainer;
import jpl.gds.dictionary.api.decom.params.StatementContainerParams;

/**
 * Base class for any statement container implementation classes.  Note that this may not be used
 * by all such classes at this time.
 *
 */
public abstract class StatementContainerDefinition implements IStatementContainer {
	
	private final List<IDecomStatement> instructions;

	/**
	 * Create a new instance initialized from the given parameter object.
	 * @param params
	 */
	public StatementContainerDefinition(StatementContainerParams params) {
		this.instructions = params.getStatements();
	}

	@Override
	public List<IDecomStatement> getStatementsToExecute() {
		return Collections.unmodifiableList(instructions);
	}
	
	@Override
	public void addStatement(IDecomStatement statement) {
		instructions.add(statement);
	}

}
