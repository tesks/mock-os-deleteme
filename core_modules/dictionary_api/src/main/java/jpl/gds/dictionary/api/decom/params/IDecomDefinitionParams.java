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

import jpl.gds.dictionary.api.decom.IDecomStatement;

/**
 * Interface for decom definition parameter objects.
 * These parameter objects exist to help construct {@link IDecomStatement}
 * instances that take large number of parameters, some of which need
 * not be specified.  In general, subclasses of this interface
 * should use reasonable default for optional parameters, and should
 * throw IllegalArgumentException if a parameter is given a value such
 * as a null, but no reasonable default exists.  If a reasonable default
 * exists, the parameter class should use that instead.
 * 
 *
 */
public interface IDecomDefinitionParams {

	/**
	 * Reset internal fields to reasonable defaults, if applicable.
	 * The reset method will not clear any collection objects; instead,
	 * the parameter object will create a new instance of that collection
	 * to use internally.
	 * Can be used instead of creating a new parameter object instance
	 * to reset.
	 */
	public void reset();

}
