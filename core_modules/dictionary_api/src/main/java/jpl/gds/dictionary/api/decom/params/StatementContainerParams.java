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
package jpl.gds.dictionary.api.decom.params;

import java.util.ArrayList;
import java.util.List;

import jpl.gds.dictionary.api.decom.IDecomStatement;

/**
 * Parameter builder class for creating IStatementContainer instances.
 * Mutable and reusable.
 * 
 * All values set for an instance of this class will be applied to the {@link IDecomStatement}
 * instance it will be used to create. For more information for each of the parameters,
 * see the interface this parameter object corresponds to.
 */
public class StatementContainerParams implements IDecomDefinitionParams {

	private List<IDecomStatement> statements = new ArrayList<>();

	/**
	 * Add a child statement to the container.
	 * @param statement the statement to add. Must not be null
	 * @throws IllegalArgumentException if the argument is null
	 */
	public void addStatement(IDecomStatement statement) {
		if (statement == null) {
			throw new IllegalArgumentException("Cannot add null statement to statement list");
		}
		statements.add(statement);
	}
	
	/**
	 * 
	 * @return the list of statements
	 */
	public List<IDecomStatement> getStatements() {
		return statements;
	}
	
	@Override
	public void reset() {
		statements = new ArrayList<>();
	}

}
