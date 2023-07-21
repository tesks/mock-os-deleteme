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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import jpl.gds.dictionary.api.command.ICommandEnumerationValue;
import jpl.gds.shared.swt.SWTUtilities;
import jpl.gds.tc.api.command.args.ICommandArgument;
import jpl.gds.tc.api.command.args.IEnumeratedCommandArgument;

/**
 * This is an the command argument GUI control class for enumerated command
 * arguments.
 * 
 * 03/22/19 Updated to extend AbstractCommandArgumentGuiControl,
 *          updated functionality to use the FlightCommand and index to access argument
 *          attributes instead of directly from the argument.
 */
public class EnumeratedArgumentGuiControl extends AbstractCommandArgumentGuiControl {

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
        final String defaultValue = getDefinition().getDefaultValue() != null ? getDefinition()
                .getDefaultValue() : "";

                final Combo comboControl = new Combo(parentComposite, SWT.SIMPLE | SWT.BORDER);

                int controlLength = 0;
                /*
                 * Change to use the enumeration value
                 * interface type.
                 */
                final java.util.List<ICommandEnumerationValue> enumVals = getDefinition()
                        .getEnumeration().getEnumerationValues();
                for (final ICommandEnumerationValue enumVal : enumVals) {
                    final String value = enumVal.getDictionaryValue();
                    if (value.length() > controlLength) {
                        controlLength = value.length();
                    }
                    comboControl.add(enumVal.getDictionaryValue());
                }

                if (getArgumentValue() != null && !getArgumentValue().isEmpty()) {
                    comboControl.setText(getArgumentValue());
                } else if (getArgumentValue() == null && !enumVals.isEmpty() && !defaultValue.isEmpty()) {
                    comboControl.setText(defaultValue);
                } else if (getArgumentValue() == null && !enumVals.isEmpty()) {
                		// Set to defaultValue instead of first item
                    comboControl.setText(defaultValue);
                }

                argControl = comboControl;

                final FormData fd = SWTUtilities.getFormData(argControl, 1,
                        controlLength + 12);
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

        /*
         * 11/15/13 - I have never found and enum arg with units, but the
         * MSL command schema allows it, so I have preserved this code though it
         * seems a bit silly.
         */
        final String units = getDefinition().getUnits();
        if (units != null) {
            suffix.setText(units);
        }

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
        prefix.setText(getDisplayName() + " (" + getDefinition().getBitLength()
                + " bits):");
        return (prefix);
    }
}
