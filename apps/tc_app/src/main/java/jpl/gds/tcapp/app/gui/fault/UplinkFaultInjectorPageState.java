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
package jpl.gds.tcapp.app.gui.fault;

import org.eclipse.swt.widgets.Composite;
import org.springframework.context.ApplicationContext;

/**
 * This interface is implemented by classes/enums who describe the flow
 * of a particular type of fault injection.  The idea is that there are many
 * types of fault injection flows:
 * 
 * - HW/FSW commands
 * - File Loads
 * - SCMFs
 * - Raw Data Files
 * 
 * Currently only the fault injection flow for commands is implemented as an
 * implementer of this interface, but it can serve as a model for how to implement
 * the other flows.
 *
 *
 */
public interface UplinkFaultInjectorPageState
{
	/**
	 * Get the first page in the flow of this type of fault injection.
	 * 
	 * @return The instance of the first page in this flow.
	 */
	public abstract UplinkFaultInjectorPageState getFirstPage();

	/**
	 * Get the last page in the flow of this type of fault injection.
	 * 
	 * @return The instance of the last page in this flow.
	 */
	public abstract UplinkFaultInjectorPageState getLastPage();

	/**
	 * Check if this is the first page in the flow.
	 * 
	 * @return True if this is the first page in the flow, false otherwise.
	 */
	public abstract boolean isFirstPage();

	/**
	 * Check if this is the last page in the flow.
	 * 
	 * @return True if this is the last page in the flow, false otherwise.
	 */
	public abstract boolean isLastPage();

	/**
	 * Get the previous page state from this state.
	 * 
	 * @return The instance of the enum representing the previous state to this one
	 * or null if there isn't one.
	 */
	public abstract UplinkFaultInjectorPageState backState();

	/**
	 * Get the next page state from this state.
	 * 
	 * @return The instance of the enum representing the next state to this one
	 * or null if there isn't one.
	 */
	public abstract UplinkFaultInjectorPageState nextState();

	/**
	 * Return a composite that represents the current state in the fault
	 * injector.  The input composite will be its parent.
	 * 
	 * @param contentComposite The SWT Composite that will be the parent of the returned composite
	 * 
	 * @return An SWT composite that displays the current input fields for this particular state of the fault
	 * injector.  The input component will be its parent.
	 * 
	 * @throws FaultInjectorException If there's a problem generating the new composite.
	 */
	public abstract FaultInjectorGuiComponent getComponentForState(ApplicationContext appContext, Composite contentComposite) throws FaultInjectorException;
	
	/**
	 * Get the number of this page in the fault injection flow.
	 * 
	 * @return The current page number (which step) in the fault injection flow.
	 */
	public abstract int getCurrentPageNumber();
	
	/**
	 * Get the last page number in the fault injection flow (the total number of pages).
	 * 
	 * @return The numeric value of the last page number in the flow (a.k.a. the total # of pages).
	 * 
	 */
	public abstract int getLastPageNumber();
}