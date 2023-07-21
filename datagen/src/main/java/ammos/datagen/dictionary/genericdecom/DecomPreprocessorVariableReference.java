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
package ammos.datagen.dictionary.genericdecom;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * This class is used by the decom map pre-processor to represent a switch
 * variable that is actually a reference to another variable, which may also be
 * a reference.
 * 
 *
 * MPCS-6944 - 12/10/14. Added class.
 * 
 */
public class DecomPreprocessorVariableReference implements
		IDecomPreprocessorVariable {

	/**
	 * Tag for information lines that represent a variable reference.
	 */
	public static final String VARIABLE_REF_TAG = "VARIABLE_REFERENCE: ";

	/**
	 * Tag that will precede the reference variable name in the variable
	 * information string.
	 */
	public static final String REFERENCE_NAME_TAG = "reference=";

	private final DecomPreprocessorBranch branch;
	private final String variableName;
	private final IDecomPreprocessorVariable reference;
	private final List<Long> potentialValues = new LinkedList<Long>();
	private boolean allowOther = false;

	/**
	 * Constructor. Automatically adds the variable to the specified parent
	 * branch.
	 * 
	 * @param baseBranch
	 *            the parent branch for the variable
	 * @param varName
	 *            the name of the variable
	 * @param refersTo
	 *            the DecomSwitchVariable this variable refers to
	 */
	public DecomPreprocessorVariableReference(
			final DecomPreprocessorBranch baseBranch, final String varName,
			final IDecomPreprocessorVariable refersTo) {
		this.branch = baseBranch;
		this.variableName = varName;
		this.reference = refersTo;
		baseBranch.addVariable(this);
	}

	/**
	 * Gets the variable object this one is a reference to.
	 * 
	 * @return DecomSwitchVariable object, which may be another reference
	 */
	public IDecomPreprocessorVariable getReferencedVariable() {
		return this.reference;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.dictionary.genericdecom.IDecomPreprocessorElement#getParentBranch()
	 */
	@Override
	public DecomPreprocessorBranch getParentBranch() {
		return this.branch;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.dictionary.genericdecom.IDecomPreprocessorVariable#getPotentialValues()
	 */
	@Override
	public List<Long> getPotentialValues() {
		return Collections.unmodifiableList(this.potentialValues);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Value is also automatically added to the referenced variable.
	 * 
	 * @see ammos.datagen.dictionary.genericdecom.IDecomPreprocessorVariable#addPotentialValue(long)
	 */
	@Override
	public void addPotentialValue(final long val) {
		if (!this.potentialValues.contains(val)) {
			this.reference.addPotentialValue(val);
			this.potentialValues.add(val);
		}
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.dictionary.genericdecom.IDecomPreprocessorVariable#setAllowOther(boolean)
	 */
	@Override
	public void setAllowOther(final boolean allow) {
		this.allowOther = allow;
		if (allow) {
			this.reference.setAllowOther(allow);
		}

	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.dictionary.genericdecom.IDecomPreprocessorVariable#isAllowOther()
	 */
	@Override
	public boolean isAllowOther() {
		return this.allowOther;
	}

	@Override
	public String getPath() {
		return this.branch.getPath() + DecomPreprocessorBranch.BRANCH_PATH_SEP
				+ this.variableName;
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Return value corresponds to the variable name.
	 * 
	 * @see ammos.datagen.dictionary.genericdecom.IDecomPreprocessorElement#getName()
	 */
	@Override
	public String getName() {
		return this.variableName;
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.dictionary.genericdecom.IDecomPreprocessorElement#getInfoString()
	 */
	@Override
	public String getInfoString() {
		final StringBuilder sb = new StringBuilder(VARIABLE_REF_TAG
				+ this.getPath() + BRANCH_PATH_SEP);
		sb.append("name=" + this.getName());
		sb.append(VARIABLE_COMPONENT_SEP + REFERENCE_NAME_TAG
				+ this.reference.getName());
		sb.append(VARIABLE_COMPONENT_SEP + VALUES_TAG);
		int i = 0;
		for (final Long v : this.potentialValues) {
			sb.append(v);
			if (i < this.potentialValues.size() - 1) {
				sb.append(VARIABLE_VALUE_SEP);
			}
			i++;
		}
		if (this.isAllowOther()) {
			sb.append(VARIABLE_VALUE_SEP + OTHER_VALUE);
		}

		return sb.toString();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return getInfoString();
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof DecomPreprocessorVariableReference)) {
			return false;
		}
		return this.getPath().equals(
				((DecomPreprocessorVariableReference) o).getPath());
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return this.getPath().hashCode();
	}

}