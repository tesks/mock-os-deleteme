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

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

/**
 * This is an the command argument GUI control class for unsigned integer
 * command arguments.
 * 
 *
 * 03/22/19 Updated to extend AbstractCommandArgumentGuiControl,
 *          updated functionality to use the FlightCommand and index to access argument
 *          attributes instead of directly from the argument.
 */
public class UnsignedArgumentGuiControl extends AbstractNumericArgumentGuiControl {

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.tcapp.app.gui.fault.util.AbstractNumericArgumentGuiControl#createPrefixControl(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public Label createPrefixControl(final Composite parent) {

        final Label prefix = new Label(parent, SWT.LEFT);
        prefix.setText(getDisplayName() + " (U" + getDefinition().getBitLength() + "):");
        return (prefix);
    }

}
