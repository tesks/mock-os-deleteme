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

import jpl.gds.dictionary.api.decom.IVariableStatementDefinition;

/**
 * 
 * This class represents a variable statement in a generic decom map used for
 * packet decommutation.
 * 
 */
public class VariableStatementDefinition implements IVariableStatementDefinition {

	private String name;
	private String referenceVariableName;
	private final int width;
	private int offset;

	/**
	 * Constructor for reference variables.
	 * 
	 * @param name the name of the variable; cannot be null
	 * @param refVarName the name of the reference variable; cannot be null
	 */
	/*package */ VariableStatementDefinition(final String name, final String refVarName) {
		
		if (name == null) {
			throw new IllegalArgumentException("Variable name cannot be null");
		}
		
		if (refVarName == null) {
			throw new IllegalArgumentException("Variable statement has null reference variable name");
		}
		
		this.name = name;
		referenceVariableName = refVarName;
		width = -1;
		offset = -1;
	}
	
	/**
     * Constructor for extraction variables.
     * 
     * @param name the name of the variable; cannot be null
     * @param w the width of the variable field in bits
     * @param o the offset of the variable field in bits
     */
    /*package */ VariableStatementDefinition(final String name, final int w, final int o) {
		
		if (name == null) {
			throw new IllegalArgumentException("Variable name cannot be null");
		}
		
		if (w < 1) {
			throw new IllegalArgumentException("Variable statement must specify width greater than 0 (got "+ w + ")");
		}
		
		this.name = name;
		width = w;
		offset = o;
	}


	/**
	 * @{inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.decom.adaptation.IVariableStatementDefinition#isExtractionVariable()
	 */
	@Override
    public boolean isExtractionVariable() {
		
		return referenceVariableName == null ? true : false;
	}
	
	/**
	 * @{inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.decom.adaptation.IVariableStatementDefinition#isOffsetSpecified()
	 */
	@Override
    public boolean isOffsetSpecified() {
		
		if (offset < 0) {
			return false;
		}
		
		return true;
	}

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.decom.adaptation.IVariableStatementDefinition#getVariableName()
	 */
	@Override
    public String getVariableName() {
		return name;
	}
	
    /**
     * @{inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.decom.adaptation.IVariableStatementDefinition#setVariableName(java.lang.String)
     */
    @Override
    public void setVariableName(final String var) {
        name = var;
    }
    
	/**
	 * @{inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.decom.adaptation.IVariableStatementDefinition#getReferenceVariableName()
	 */
	@Override
    public String getReferenceVariableName() {
		return referenceVariableName;
	}
	
    /**
     * @{inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.decom.adaptation.IVariableStatementDefinition#setReferenceVariableName(java.lang.String)
     */
    @Override
    public void setReferenceVariableName(final String refVarName) {
        referenceVariableName = refVarName;
    }

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.decom.adaptation.IVariableStatementDefinition#getWidthToExtract()
	 */
	@Override
    public int getWidthToExtract() {
		return width;
	}
	
    /**
     * @{inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.decom.adaptation.IVariableStatementDefinition#setWidthToExtract(int)
     */
    @Override
    public void setWidthToExtract(final int w) {
        if (w < 1) {
            throw new IllegalArgumentException("Variable statement must specify width greater than 0 (got "+ w + ")");
        } else {
            offset = w;
        }
    }

	/**
	 * @{inheritDoc}
	 * @see jpl.gds.dictionary.impl.impl.api.decom.adaptation.IVariableStatementDefinition#getOffsetToExtract()
	 */
	@Override
    public int getOffsetToExtract() {
		return offset;
	}
	
    /**
     * @{inheritDoc}
     * @see jpl.gds.dictionary.impl.impl.api.decom.adaptation.IVariableStatementDefinition#setOffsetToExtract(int)
     */
    @Override
    public void setOffsetToExtract(final int o) {
        offset = o;
    }

	
}
