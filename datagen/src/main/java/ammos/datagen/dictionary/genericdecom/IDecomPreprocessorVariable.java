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

import java.util.List;

/**
 * This interface is implemented by decom map preprocessor classes that
 * represent variables in a decom map.
 * 
 *
 * MPCS-6944 - 12/10/14. Added interface.
 */
public interface IDecomPreprocessorVariable extends IDecomPreprocessorElement {

	/**
	 * Value that will represent a variable value that is not in the list of
	 * potential values.
	 */
	public static final String OTHER_VALUE = "OTHER";

	/**
	 * Value that will separate elements in the variable information string.
	 */
	public static final String VARIABLE_COMPONENT_SEP = ",";

	/**
	 * Value that will separate values in the variable values list in the
	 * variable information string.
	 */
	public static final String VARIABLE_VALUE_SEP = ";";

	/**
	 * Tag that will precede list of variable values in the in the variable
	 * information string.
	 **/
	public static final String VALUES_TAG = "values=";

	/**
	 * Adds a potential value to the variable. Each potential value of the
	 * variable exercises one path through the decom map. Duplicate values
	 * should be filtered out by the implementing code.
	 * 
	 * @param val
	 *            an unsigned value for the variable
	 */
	public void addPotentialValue(long val);

	/**
	 * Gets the list of potential values for the variable.
	 * 
	 * @return non-modifiable list of unsigned integer values; may be empty but
	 *         never null
	 */
	public abstract List<Long> getPotentialValues();

	/**
	 * Sets the flag indicating whether the variable can take a value not in the
	 * potential values list. This is used to exercise default switch cases.
	 * 
	 * @param allow
	 *            true to allow other values, false if not
	 */
	public void setAllowOther(boolean allow);

	/**
	 * Gets the flag indicating whether a the variable can take a value not in
	 * the potential values list. This is used to exercise default switch cases.
	 * 
	 * @return true to allow other values, false if not
	 */
	public boolean isAllowOther();
}