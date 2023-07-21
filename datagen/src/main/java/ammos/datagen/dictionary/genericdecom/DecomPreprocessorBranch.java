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
 * This class is used by the decom map pre-processor to represent a branch, or
 * one processing path and its sub-paths, throough a decom map. Each branch has
 * a name, which consists of the switch variable name and value leading to the
 * branch, and a branch path, which is the concatenation of all branch IDs
 * leading to this branch and the current branch ID. The root branch is special,
 * having no variable or value leading to it. Its name is defined by the
 * constant ROOT_NAME its path by ROOT_PATH.
 * <p>
 * Each branch has attached variables, channels, and sub-branches. Therefore, if
 * one starts with a root branch, one effectively has a tree representing the
 * entire decom map.
 * 
 * MPCS-6944 - 12/10/14. Added class.
 * 
 */
public class DecomPreprocessorBranch implements IDecomPreprocessorElement {
	/**
	 * The branch component separator character, used to separate elements in
	 * the branch ID.
	 */
	public static final String BRANCH_COMPONENT_SEP = "-";

	/**
	 * The root branch name.
	 */
	public static final String ROOT_NAME = "ROOT";

	/**
	 * The root branch path.
	 */
	public static final String ROOT_PATH = BRANCH_PATH_SEP + ROOT_NAME;

	/**
	 * Tag for information lines that represent a branch.
	 */
	public static final String BRANCH_TAG = "BRANCH: ";

	private DecomPreprocessorBranch baseBranch;
	private String variableName;
	private String variableValue;
	private boolean rootBranch = false;
	private final List<DecomPreprocessorChannel> branchChannels = new LinkedList<DecomPreprocessorChannel>();
	private final List<IDecomPreprocessorVariable> branchVariables = new LinkedList<IDecomPreprocessorVariable>();
	private final List<DecomPreprocessorBranch> subBranches = new LinkedList<DecomPreprocessorBranch>();

	/**
	 * Constructor for a root branch only.
	 */
	public DecomPreprocessorBranch() {
		this.rootBranch = true;
	}

	/**
	 * Constructor for a non-root branch. This branch will be added as a
	 * sub-branch to the specified parent branch by the constructor.
	 * 
	 * @param base
	 *            the parent branch object
	 * @param varName
	 *            the variable used by the switch for which this branch is
	 *            created
	 * @param varValue
	 *            the variable value used by the switch case for which this
	 *            branch is created; may be OTHER
	 */
	public DecomPreprocessorBranch(final DecomPreprocessorBranch base,
			final String varName, final String varValue) {
		this.rootBranch = false;
		this.baseBranch = base;
		this.variableName = varName;
		this.variableValue = varValue;
		this.baseBranch.addSubBranch(this);
	}

	/**
	 * Gets the basic name of this branch. Elements in the name are delimited by
	 * BRANCH_COMPONENT_SEP.
	 * 
	 * @return branch ID
	 */
	@Override
	public String getName() {
		if (this.rootBranch) {
			return ROOT_NAME;
		} else {
			return this.variableName + BRANCH_COMPONENT_SEP
					+ this.variableValue;
		}
	}

	/**
	 * Gets the full path of this branch, ending with the branch name. Elements
	 * in the PATH are delimited by BRANCH_PATH_SEP.
	 * 
	 * @return the fill branch path
	 */
	@Override
	public String getPath() {
		if (this.rootBranch) {
			return ROOT_PATH;
		} else {
			return this.baseBranch.getPath() + BRANCH_PATH_SEP + this.getName();
		}
	}

	/**
	 * Gets the name of the switch variable used to select this decom branch.
	 * 
	 * @return decom variable name; will be null for root branches
	 */
	public String getBranchVariableName() {
		return this.variableName;
	}

	/**
	 * Gets the value the switch variable used to select this decom branch. This
	 * will be the string representation of an unsigned long or
	 * DecomSwitchVariable.OTHER_VALUE
	 * 
	 * @return decom variable value; will be null for root branches
	 */
	public String getVariableValue() {
		return this.variableValue;
	}

	/**
	 * Indicates if this branch is a root branch.
	 * 
	 * @return true if a root branch (no parent branch) or false if not
	 */
	public boolean isRootBranch() {
		return this.rootBranch;
	}

	/**
	 * Gets the list of decom channels attached directly to this branch.
	 * 
	 * @return non-modifiable list of decom channel objects; may be empty but
	 *         never null
	 */
	public List<DecomPreprocessorChannel> getBranchChannels() {
		return Collections.unmodifiableList(this.branchChannels);
	}

	/**
	 * Gets the list of decom variables attached directly to this branch.
	 * 
	 * @return non-modifiable list of decom variable objects; may be empty but
	 *         never null
	 */
	public List<IDecomPreprocessorVariable> getBranchVariables() {
		return Collections.unmodifiableList(this.branchVariables);
	}

	/**
	 * Adds a channel entry to this branch as a direct child. This represents a
	 * channel sample to be extracted on this branch. Package protected because
	 * only the decom channel constructor should call this.
	 * 
	 * @param chan
	 *            the decom channel object to add
	 */
	void addChannel(final DecomPreprocessorChannel chan) {
		this.branchChannels.add(chan);
	}

	/**
	 * Adds a variable entry to this branch as a direct child. This represents a
	 * variable to be extracted on this branch. Package protected because only
	 * the decom variable constructor should call this.
	 * 
	 * 
	 * @param var
	 *            the decom variable object to add
	 */
	public void addVariable(final IDecomPreprocessorVariable var) {
		this.branchVariables.add(var);
	}

	/**
	 * Adds a sub-branch to this branch. Private protected because only the
	 * constructor should call this.
	 * 
	 * 
	 * @param branch
	 *            the sub-branch to add
	 */
	private void addSubBranch(final DecomPreprocessorBranch branch) {
		this.subBranches.add(branch);
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 * Returns null for root branches.
	 * 
	 * @see ammos.datagen.dictionary.genericdecom.IDecomPreprocessorElement#getParentBranch()
	 */
	@Override
	public DecomPreprocessorBranch getParentBranch() {
		return this.baseBranch;
	}

	/**
	 * Gets the list of sub-branches to this branch.
	 * 
	 * @return non-modifiable list of decom branches; may be empty but never
	 *         null
	 */
	public List<DecomPreprocessorBranch> getSubBranches() {
		return Collections.unmodifiableList(this.subBranches);
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see ammos.datagen.dictionary.genericdecom.IDecomPreprocessorElement#getInfoString()
	 */
	@Override
	public String getInfoString() {
		return getInfoString("");
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
	 * A to string method for the sub-tree starting with this branch, and which
	 * indents sub-branches. Recursively iterates all sub-branches, indenting
	 * appropriately.
	 * 
	 * @param indent
	 *            the indent string to append to output for this branch
	 * 
	 * @return the complete string representation of the branch and all
	 *         sub-branches
	 */
	private String getInfoString(final String indent) {
		final StringBuilder sb = new StringBuilder(indent + BRANCH_TAG
				+ this.getPath());

		final String newIndent = indent + "   ";

		if (!this.branchVariables.isEmpty()) {
			for (final IDecomPreprocessorVariable v : this.branchVariables) {
				sb.append("\n" + newIndent + v);
			}
		}
		if (!this.branchChannels.isEmpty()) {
			for (final IDecomPreprocessorElement c : this.branchChannels) {
				sb.append("\n" + newIndent + c);
			}
		}

		if (!this.subBranches.isEmpty()) {
			sb.append("\n");
			for (final DecomPreprocessorBranch b : this.subBranches) {
				sb.append("\n");
				sb.append(b.getInfoString(newIndent));
			}
		}

		return sb.toString() + "\n";
	}

	/**
	 * {@inheritDoc}
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(final Object o) {
		if (!(o instanceof DecomPreprocessorBranch)) {
			return false;
		}
		return this.getPath().equals(((DecomPreprocessorBranch) o).getPath());
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