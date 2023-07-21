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

import java.util.List;
import java.util.Map;

/**
 * An interface to be implemented by process control directives.
 * 
 *
 * @since R8
 */
public interface IDirective {

	/**
	 * Sets the owning instance ID of the service or process that accepts this
	 * directive.
	 * 
	 * @param instanceId
	 *            a unique ID string
	 */
	public void setOwnerId(String instanceId);

	/**
	 * Gets the owning instance ID of the service or process that accepts this
	 * directive.
	 * 
	 * @return instance ID of the directive owner
	 */
	public String getOwnerId();

	/**
	 * Gets the type of the directive.
	 * 
	 * @return IDirectiveType
	 */
	public IDirectiveType getType();

	/**
	 * Sets the type of the directive.
	 * 
	 * @param type
	 *            the IDirectiveType to set
	 */
	public void setType(IDirectiveType type);

	/**
	 * Sets the list of argument objects for arguments accepted with this
	 * directive.
	 * 
	 * @param args
	 *            list of IDirectiveArgument to set
	 */
	public void setArguments(List<IDirectiveArgument> args);

	/**
	 * Adds an argument object to the list of argument objects for arguments
	 * accepted with this directive.
	 * 
	 * @param arg
	 *            the IDirectiveArgument to add
	 */
	public void addArgument(IDirectiveArgument arg);

	/**
	 * Gets a map of the argument objects accepted with this directive.
	 * 
	 * @return Map of directive name to IArgumentDirective argument
	 */
	public Map<String, IDirectiveArgument> getArguments();

	/**
	 * Gets a single directive argument object.
	 * 
	 * @param argName
	 *            name of the argument
	 * 
	 * @return matching IDirectiveArgument object, or null if no match
	 */
	public IDirectiveArgument getArgument(String argName);

	/**
	 * Gets the value of an argument supplied with this directive.
	 * 
	 * @param argName
	 *            name of the argument
	 * 
	 * @return String value supplied when the directive was received, or null if
	 *         no match
	 */
	public Object getArgumentValue(String argName);

	/**
	 * Resets the message indicating the last error found when processing this
	 * directive to null.
	 */
	public void resetLastMessage();

	/**
	 * Gets the message indicating the last error found when processing this
	 * directive.
	 * 
	 * @return last message string set, or null if none was set
	 */
	public String getLastMessage();

	/**
	 * Sets the message indicating the last error found when processing this
	 * directive.
	 * 
	 * @param errorString
	 *            message string to set
	 */
	public void setLastMessage(String errorString);
}
