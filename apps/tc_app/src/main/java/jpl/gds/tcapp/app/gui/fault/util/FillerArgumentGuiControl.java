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
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.shared.util.BinOctHexUtility;
import jpl.gds.tc.api.command.args.ICommandArgument;
import jpl.gds.tc.api.command.args.IFillCommandArgument;

/**
 * This is an the command argument GUI control class for fill command arguments.
 * 
 * 03/22/19  Updated to extend AbstractCommandArgumentGuiControl,
 *          updated functionality to use the FlightCommand and index to access argument
 *          attributes instead of directly from the argument.
 */
public class FillerArgumentGuiControl extends AbstractCommandArgumentGuiControl {

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.tcapp.app.gui.fault.util.ICommandArgumentGuiControl#createArgumentValueControl(org.eclipse.swt.widgets.Composite,
     *      org.eclipse.swt.graphics.Font)
     */
    @Override
    public Control createArgumentValueControl(final Composite parentComposite,
            final Font font) {

        Control argControl = null;

        final Text textControl = new Text(parentComposite, SWT.BORDER);
        textControl.setFont(font);
        textControl.setText("0x"
                + BinOctHexUtility.toHexFromBin(getArgumentValue()));
        argControl = textControl;

        final FormData fd = SWTUtilities.getFormData(argControl, 1, 11);
        argControl.setLayoutData(fd);

        return (argControl);
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.tcapp.app.gui.fault.util.ICommandArgumentGuiControl#createSuffixControl(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public Control createSuffixControl(final Composite parent) {

        final Label suffix = new Label(parent, SWT.LEFT);
        suffix.setText("");
        return (suffix);
    }

    /**
     * {@inheritDoc}
     * 
     * @see jpl.gds.tcapp.app.gui.fault.util.ICommandArgumentGuiControl#createPrefixControl(org.eclipse.swt.widgets.Composite)
     */
    @Override
    public Label createPrefixControl(final Composite parent) {

        final Label prefix = new Label(parent, SWT.LEFT);
        prefix.setText("Fill (" + getDefinition().getBitLength() + " bits):");
        return (prefix);
    }
}
