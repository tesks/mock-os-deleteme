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
package jpl.gds.context.api;

import jpl.gds.shared.metadata.context.ContextConfigurationType;

/**
 * An interface to be implemented by context configuration factories.
 */
public interface IContextConfigurationFactory {

	/**
	 * Create a new context configuration.
	 * 
	 * @param type
	 *            type of the context configuration to instantiate
	 * @param ephemeral
	 * 			  boolean to indicate if the context config being created is ephemeral, not to be reused.
	 * @return newly instantiated context configuration
	 */
	public ISimpleContextConfiguration createContextConfiguration(ContextConfigurationType type, boolean ephemeral);

}
