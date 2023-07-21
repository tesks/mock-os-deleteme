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
package jpl.gds.product.automation.hibernate.checkers;

import org.springframework.context.ApplicationContext;

/**
 * Abstract checker class to figure out if a given product requires processing for the actionName (PDPP mnemonic) given in the constructor. 
 * 
 *
 * MPCS-8180 - 07/20/2016 - Imported to and updated for AMPCS M20 adaptation
 */
public abstract class AbstractAutomationChecker implements IProductAutomationChecker {

	private final String actionName;
	protected final ApplicationContext appContext;

	/**
	 * This is a required constructor.  This will set the actionName.
	 * 
	 * @param actionName the action this checker will be working for
	 */
	public AbstractAutomationChecker(String actionName, ApplicationContext appContext) {
		this.actionName = actionName;
		this.appContext = appContext;
	}

	/**
	 * @return actionName that was set in the constructor.  This should be the mnemonic that maps to the specific 
	 * PDPP class in the PDPP database.
	 */
	public String getActionName() {
		return actionName;
	}
}
