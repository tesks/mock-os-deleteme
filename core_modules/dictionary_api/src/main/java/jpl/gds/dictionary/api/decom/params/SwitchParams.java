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

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import jpl.gds.dictionary.api.decom.IDecomStatement;
import jpl.gds.dictionary.api.decom.IDecomStatementFactory;
import jpl.gds.dictionary.api.decom.ISwitchStatementDefinition;
import jpl.gds.dictionary.api.decom.types.ICaseBlockDefinition;

/**
 * Parameter builder class for creating {@link ISwitchStatementDefinition} instances.
 * Mutable and reusable.
 * 
 * All values set for an instance of this class will be applied to the {@link IDecomStatement}
 * instance it will be used to create. For more information for each of the parameters,
 * see the interface this parameter object corresponds to.
 */
public class SwitchParams implements IDecomDefinitionParams {

	private String variableName = "";
	
	private Map<Long, ICaseBlockDefinition> cases = new HashMap<>();
	
	// Present between a call to start a case and the next call to end a case
	private Optional<CaseParams> openCase = Optional.empty();

	private ICaseBlockDefinition defaultCase;
	
	private int modulus;

	/**
	 * @see ISwitchStatementDefinition#getVariableToSwitchOn()
	 * @return the name of the variable to switch on
	 */
	public String getVariableName() {
		return variableName;
	}

	/**
	 * Set the name of the variable to switch on
	 * @param variableName
	 */
	public void setVariableName(String variableName) {
		this.variableName = variableName;
	}

	/**
	 * @see ISwitchStatementDefinition#getModulus()
	 * @return the modulus to use with the switch variable in order to determine
	 * 		   the case to execute
	 */
	public int getModulus() {
		return modulus;
	}

	/**
	 * Set the modulus for the switch variable
	 * @param modulus
	 */
	public void setModulus(int modulus) {
		this.modulus = modulus;
	}
	
	/**
	 * Get the constituent cases within the switch statement
	 * @return a mapping of values to case blocks. Default case not included
	 */
	public Map<Long, ICaseBlockDefinition> getCases() {
		return cases;
	}
	
	/**
	 * Get the default case block.
	 * @return the default case
	 */
	public ICaseBlockDefinition getDefaultCase() {
		return defaultCase;
	}

	/**
	 * Start a pending case. The CaseParams may be in the process of having statements
	 * added to it.  Call {@link #endCase()} once the statements have all been added
	 * to the CaseParams.
	 * @param caseVal the value associated with the new case. Must be non-null.
	 * @param params the case block having statements being added to it.
	 */
	public void startCase(long caseVal, CaseParams params) {
		if (openCase.isPresent()) {
			throw new IllegalStateException(
					String.format("Case with value=%d was started before the last case ended.", caseVal));
		}
		openCase = Optional.of(params);
		openCase.get().setValue(caseVal);
	}

	/**
	 * Like {@link #startCase(long, CaseParams)} but should be used for
	 * the default case.
	 * @param params the case block having statements being added to it.
	 * 		  Must be non-null.
	 */
	public void startDefaultCase(CaseParams params) {
		if (openCase.isPresent()) {
			throw new IllegalStateException("Default case was started before the last case ended.");
		}
		openCase = Optional.of(params);
		openCase.get().setIsDefault();
	}

	/**
	 * Close the last opened case.  Creates the actual {@link ICaseBlockDefinition} object for that case.
	 * @throws IllegalStateException if called but no case has been started since the last close call
	 * 		   or since the creation of the parameter object
	 */
	public void endCase() {
		if (openCase.isPresent()) { 
			ICaseBlockDefinition def = IDecomStatementFactory.newInstance().createCaseBlock(openCase.get());
			if (def.isDefault()) {
				defaultCase = def;
			} else {
				cases.put(def.getValue(), def);
			}
			openCase = Optional.empty();
		} else {
			throw new IllegalStateException("Attempted to end a case statement, but none have been started.");
		}
	}

	@Override
	public void reset() {
		cases = new HashMap<>();
	}
}
