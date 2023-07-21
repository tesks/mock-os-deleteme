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
package jpl.gds.shared.directive;

/**
 * An interface to be implemented by arguments to IDirective objects.
 * 
 *
 * @since R8
 *
 */
public interface IDirectiveArgument {

	/**
	 * Gets the name of this argument.
	 * 
	 * @return argument name
	 */
	public String getName();

	/**
	 * Gets the last value entered for this directive argument as an object.
	 * 
	 * @return argument value, or null if none assigned
	 */
	public Object getValue();

	/**
	 * Gets the last value entered for this directive argument as a string.
	 * 
	 * @return argument value, or null if none assigned
	 */
	public String getValueAsString();

}