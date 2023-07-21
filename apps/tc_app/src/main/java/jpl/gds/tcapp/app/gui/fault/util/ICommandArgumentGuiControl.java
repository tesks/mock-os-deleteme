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
package jpl.gds.tcapp.app.gui.fault.util;

import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import jpl.gds.tc.api.command.IFlightCommand;

/**
 * This interface is implemented by all command argument GUI control classes,
 * which are used to create GUI controls for different types of command arguments.
 * 
 * 03/22/19 Changed constructor to take FlightCommand
 *          and the argument index, added second constructor that takes
 *          FlightCommand, repeat argument index, and argument subIndex.
 */
public interface ICommandArgumentGuiControl {

    /**
     * Sets the command argument associated with this GUI control object.
     * 
     * @param cmd the FlightCommand containing the argument to be controlled
     * @param argIndex the index to the ICommandArgument to be controlled
     */
    public void setArgument(final IFlightCommand cmd, final int argIndex);
    
    /**
     * Sets the single command argument within a repeated argument to be associated
     * with this GUI control object
     * @param cmd the FlightCommand containing the argument to be controlled
     * @param argIndex the index to the IRepeatCommandArgument containing the
     *            argument to be controlled
     * @param argSubIndex the index to the ICommandArgument to be controlled
     */
    public void setArgument(final IFlightCommand cmd, final int argIndex, final int argSubIndex);

    /**
     * Create an SWT label identifying this particular command argument
     * (generally consisting of name and length). This Label is used in the
     * command builder to identify this argument for the user.
     * 
     * Displayed to the left of the "Argument Value Control" in the chill_up 
     * command builder.
     * 
     * @param parent
     *            The SWT Composite that will contain this Label.
     * 
     * @return An SWT Label containing text that identifies this argument.
     */
    public abstract Label createPrefixControl(final Composite parent);

    /**
     * Create an SWT Control for setting the value of this particular argument
     * type in the command builder. For instance, a numeric argument might need
     * a Text field, but an enumerated argument probably needs a Combo box.
     * 
     * Displayed to the right of the "Prefix Control" and to the left of the
     * "Suffix Control" in the chill_up command builder.
     * 
     * @param parentComposite
     *            The composite that will contain this SWT Control.
     * @param font
     *            The font that the Control should use to display its contents.
     * 
     * @return An SWT Control created for setting the value of this particular
     *         argument.
     */
    public abstract Control createArgumentValueControl(
            final Composite parentComposite, final Font font);

    /**
     * Create an SWT Control used as a trailer after the argument value control
     * in the command builder. Generally this Control is a Label containing
     * range or other validation information for the argument, but in some
     * special cases (e.g. repeat argument) it can be a button used to launch
     * another window.
     * 
     * Displayed to the right of the "Argument Value Control" in the chill_up
     * command builder.
     * 
     * @param parent
     *            The SWT Composite that will contain this Control.
     * 
     * @return An SWT Control containing extra information about this argument.
     */
    public abstract Control createSuffixControl(final Composite parent);
}
