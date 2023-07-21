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

import org.eclipse.swt.graphics.Font;

/**
 * The FaultInjectorGuiComponent is the interface implemented by every Composite
 * that shows up in the middle panel of the fault injector GUI (e.g. CommandBuilderComposite,
 * FrameEditorComposite, etc.).  This interface describe all of the methods that all of these
 * components need to properly transition back and forth (based on user clicks of the back and
 * next buttons in the fault injector shell) while maintaining proper data state and display state
 * between them.
 *
 *
 */
public interface FaultInjectorGuiComponent
{
	/**
	 * Get the title to be displayed in the top panel of the fault injector shell
	 *
	 * @return The title of this GUI page
	 */
	public abstract String getTitle();

	/**
	 * Get the description to be displayed in the top panel of the fault injector shell
	 *
	 * @return The description of this GUI page
	 */
	public abstract String getDescription();

	/**
	 * Set the state of the current component
	 * (this is called when the user clicks the back or next button)
	 *
	 * @param state The current state of the fault injection process
	 */
	public abstract void setFromState(FaultInjectionState state);

	/**
	 * Update the component's current state with whatever changes have been made
	 * by the user (this is called when the user clicks the back or next button)
	 * 
	 * @throws FaultInjectorException If the state can't be updated properly.
	 */
	public abstract void updateState() throws FaultInjectorException;

	/**
	 * Get the current state of this component (used in conjunction with setFromState
	 * when the user clicks the back or next button). This is generally called AFTER
	 * updateState() has been called.
	 *
	 * @return The state of the current component
	 */
	public abstract FaultInjectionState getCurrentState();

	/**
	 * Update all the display contents of the component with the current state
	 * that it has (used when the user clicks the back or next button).  This
	 * is generally called AFTER setFromState has been called.
	 *
	 * @throws FaultInjectorException Any possible thing that went wrong during the display update
	 * that would put this component into a state where it cannot be displayed (the
	 * transition triggered by the back or next button cannot occur).
	 */
	public abstract void updateDisplay() throws FaultInjectorException;

	/**
	 * Destroy the current component
	 */
	public abstract void destroy();
	
	/**
	 * Get the particular font that should be used for this GUI component.
	 * 
	 * @return The SWT Font that should be used in this window.
	 */
	public Font getTextFieldFont();
}
