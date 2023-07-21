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
/**
 * 
 */
package jpl.gds.dictionary.impl.decom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import jpl.gds.dictionary.api.decom.IDecomStatement;
import jpl.gds.dictionary.api.decom.ISwitchStatementDefinition;
import jpl.gds.dictionary.api.decom.params.SwitchParams;
import jpl.gds.dictionary.api.decom.types.ICaseBlockDefinition;

/**
 * This class represents a switch statement in a decom map, with multiple
 * attached switch cases, selected by a single switch variable.
 * <p>
 * TODO: 1/15/16 This class is not designed like most of the other
 * dictionary definition objects, which are largely just containers for
 * dictionary attributes. This one is used to facilitate parsing by tracking
 * current parser state. This fact requires us to expose interface methods
 * needed by our specific parser that may not really be the interfaces we
 * would want customers of the dictionary API to use. It would be better if the
 * parser tracked its own state by caching cases and statements until parsed,
 * and then just set them into this object. If an opportunity arises owing 
 * to other refactoring tasks, this class should be re-worked.
 * 
 *
 */
public class SwitchStatementDefinition extends Statement implements ISwitchStatementDefinition {

	private final static int INITIAL_STATEMENTS_CACHE_SIZE = 16;

	private String variableName;
	private int modulus;
	private final Map<Long, List<IDecomStatement>> caseMap;
	private List<IDecomStatement> defaultStatements;
	private final List<IDecomStatement> currentStatementsCache;
	private final List<Long> currentCasesCache;

	/**
	 * Package protected constructor.
	 * 
	 * @param vn the switch variable name
	 */
	/*package */ SwitchStatementDefinition (final String vn) {

		if (vn == null) {
			throw new IllegalArgumentException("Switch statement's variable name cannot be null");
		}

		variableName = vn;
		modulus = -1;

		/* Make this a sorted map for more predictable results with Java 8 */
		caseMap = new TreeMap<Long,List<IDecomStatement>>();
		currentStatementsCache = new ArrayList<IDecomStatement>(INITIAL_STATEMENTS_CACHE_SIZE);

		currentCasesCache = new ArrayList<Long>();
	}
	
	SwitchStatementDefinition(SwitchParams params) {
		if ( params.getDefaultCase() != null){
			defaultStatements = params.getDefaultCase().getStatementsToExecute();
		}
		else{
			defaultStatements = Collections.unmodifiableList(new ArrayList<>());
		}
		
		variableName = params.getVariableName();
		currentCasesCache = new ArrayList<Long>();
		currentStatementsCache = new ArrayList<IDecomStatement>(INITIAL_STATEMENTS_CACHE_SIZE);
		/*  Make this a sorted map for more predictable results with Java 8 */
		caseMap = new TreeMap<Long,List<IDecomStatement>>();
		for (ICaseBlockDefinition caseBlock : params.getCases().values()) {
			caseMap.put(caseBlock.getValue(), caseBlock.getStatementsToExecute());
		}

	}

	/**
	 * Package protected constructor with modulus. If specified, the modulo of
	 * the variable value will be calculated before the case(s) in the switch
	 * are executed.
	 * 
	 * @param vn
	 *            the switch variable name
	 * @param mod
	 *            the switch variable modulus
	 */
	/*package */ SwitchStatementDefinition (final String vn, final int mod) {
		this(vn);

		if (mod < 1) {
			throw new IllegalArgumentException("Modulus in Switch statement cannot be less than 1 (got " + mod + ")");
		}

		modulus = mod;
	}
	
	/**
	 * Note that this method adds statements to the pending statement list. The list must be finalized
	 * with a call to either endPendingCases() or endDefaultCase().
	 * <p><br>
	 * 
	 * @{inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.decom.adaptation.IStatementContainer#addStatement(jpl.gds.dictionary.impl.impl.api.decom.adaptation.IDecomStatement)
	 */
	@Override
    public void addStatement(final IDecomStatement stmt) {
		currentStatementsCache.add(stmt);
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.decom.adaptation.ISwitchStatementDefinition#startPendingCase(long)
	 */
	@Override
    public void startPendingCase(final long c) {
	    currentStatementsCache.clear();
		currentCasesCache.add(c);
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.decom.adaptation.ISwitchStatementDefinition#hasCases()
	 */
	@Override
    public boolean hasCases() {
		return currentCasesCache.size() > 0; 
	}
	
	/**
	 * @{inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.decom.adaptation.ISwitchStatementDefinition#endPendingCases()
	 */
	@Override
    public void endPendingCases() {

		if (currentCasesCache.size() < 1) {
			throw new IllegalArgumentException("Cannot map empty cases");
		}

		final List<IDecomStatement> statementsToMap = new ArrayList<IDecomStatement>(currentStatementsCache);

		for (final Long l : currentCasesCache) {

			if (caseMap.containsKey(l.longValue())) {
				throw new IllegalStateException("Case " + l.longValue() + " has already been mapped");
			}

			caseMap.put(l.longValue(), statementsToMap);
		}

		currentStatementsCache.clear();
		currentCasesCache.clear();
	}

    /**
     * @{inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.decom.adaptation.ISwitchStatementDefinition#startDefaultCase()
     */
    @Override
    public void startDefaultCase() {
        currentStatementsCache.clear();
        currentCasesCache.clear();      
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.decom.adaptation.ISwitchStatementDefinition#endDefaultCase()
     */
    @Override
    public void endDefaultCase() {
        defaultStatements = new ArrayList<IDecomStatement>(currentStatementsCache);
        currentStatementsCache.clear();
    }

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.decom.adaptation.ISwitchStatementDefinition#getVariableToSwitchOn()
	 */
	@Override
    public String getVariableToSwitchOn() {
		return variableName;
	}
	
    /**
     * @{inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.decom.adaptation.ISwitchStatementDefinition#setVariableToSwitchOn(java.lang.String)
     */
    @Override
    public void setVariableToSwitchOn(final String varName) {
        variableName = varName;
    }

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.decom.adaptation.ISwitchStatementDefinition#getStatementsToExecute(long)
	 */
	@Override
    public List<IDecomStatement> getStatementsToExecute(long value) {

		if (modulus > 0) {
			value = value % modulus;
		}

		final List<IDecomStatement> s = caseMap.get(value);

		if (s == null) {
			return defaultStatements;
		} else {
			return s;
		}

	}
	
	/**
	 * @{inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.decom.adaptation.ISwitchStatementDefinition#getCaseValues()
	 */
	@Override
    public List<Long> getCaseValues() {
		return new LinkedList<Long>(caseMap.keySet());
	}


	/**
	 * @{inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.decom.adaptation.ISwitchStatementDefinition#getDefaultStatements()
	 */
	@Override
    public List<IDecomStatement> getDefaultStatements() {

		if (defaultStatements != null) {
			return Collections.unmodifiableList(defaultStatements);
		} else {
			return null;
		}
	}

    /**
     * @{inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.decom.adaptation.ISwitchStatementDefinition#setModulus(int)
     */
    @Override
    public void setModulus(final int modulus) {
        this.modulus = modulus;
        
    }

    /**
     * @{inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.decom.adaptation.ISwitchStatementDefinition#getModulus()
     */
    @Override
    public int getModulus() {
        return modulus;
    }

	@Override
	public List<IDecomStatement> getStatementsToExecute() {
		return defaultStatements;
	}
	

}