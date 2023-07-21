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
package jpl.gds.common.service;

import java.util.List;

import org.springframework.context.ApplicationContext;

import jpl.gds.shared.directive.DirectiveStatus;
import jpl.gds.shared.directive.IDirective;

/**
 * An interface to be implemented by service container objects that accept
 * directives.
 * 
 *
 * @since R8
 *
 */
public interface IServiceContainer {

	/**
	 * Initializes the container.
	 * 
	 * @return true if successful, false if not
	 */
	public boolean init();

	/**
	 * Shuts down the container and the services it contains.
	 * 
	 * @return true if successful, false if not
	 */
	public boolean shutdown();

	/**
	 * Gets the unique instance name of this container.
	 * 
	 * @return instance name
	 */
	public String getInstanceName();

	/**
	 * Gets the list of directives supported by this container.
	 * 
	 * @return list of IDirective objects
	 */
	public List<IDirective> getDirectives();

	/**
	 * Gets the ApplicationContext in use by this container.
	 * 
	 * @return ApplicationContext
	 */
	public ApplicationContext getContext();

	/**
	 * Asks the container to execute a directive.
	 * 
	 * @param toExecute
	 *            the directive to execute
	 * 
	 * @return the status of the execution
	 */
	public DirectiveStatus executeDirective(IDirective toExecute);

	/**
	 * Asks the container for its log file path.
	 * 
	 * @return log file name
	 */
	public String getLogFile();

}