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

/**
 * This interface is to be implemented by decom map pre-processor classes that
 * represent elements in a decom pre-processor tree.
 * 
 *
 * MPCS-6944 - 12/10/14. Added interface.
 * 
 */
public interface IDecomPreprocessorElement {

	/**
	 * The branch separator character, used to separate sub-branch IDs in a
	 * decom branch path in an element information string.
	 */
	public static final String BRANCH_PATH_SEP = "/";

	/**
	 * Separator used between byte and bit offset in element information
	 * strings.
	 */
	public static final String OFFSET_SEP = ":";

	/**
	 * Tag that will precede the offset in the element information string.
	 **/
	public static final String OFFSET_TAG = "offset=";

	/**
	 * Tag that will precede the bit length in the element information string.
	 **/
	public static final String LENGTH_TAG = "length=";

	/**
	 * Gets the parent decom branch for this decom element. If this roots null,
	 * the element is a root element.
	 * 
	 * @return the parent DecomBranch object
	 */
	public abstract DecomPreprocessorBranch getParentBranch();

	/**
	 * Gets the basic name of the element, less branch path.
	 * 
	 * @return element identifier
	 */
	public abstract String getName();

	/**
	 * Gets the full decom path to this decom element as a string.
	 * 
	 * @return decom path
	 */
	public abstract String getPath();

	/**
	 * Gets the information string containing all the information in this
	 * element.
	 * 
	 * @return information string
	 */
	public abstract String getInfoString();

}